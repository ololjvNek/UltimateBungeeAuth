package pl.jms.auth.core.auth;

import pl.jms.auth.core.config.AuthConfig;
import pl.jms.auth.core.integrations.WebhookNotifier;
import pl.jms.auth.core.model.Account;
import pl.jms.auth.core.queue.AuthQueueService;
import pl.jms.auth.core.security.LoginThrottle;
import pl.jms.auth.core.security.PasswordHasher;
import pl.jms.auth.core.security.PasswordRulesEngine;
import pl.jms.auth.core.storage.SqlSchema;
import pl.jms.auth.core.storage.UserRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public final class AuthService {

    private final AuthConfig config;
    private final UserRepository repository;
    private final PasswordHasher passwordHasher;
    private final PasswordRulesEngine rulesEngine;
    private final LoginThrottle throttle;
    private final WebhookNotifier webhooks;
    private final AuthQueueService queueService;

    private final Map<UUID, Account> byUuid = new ConcurrentHashMap<>();
    private final Map<String, Account> byLowerName = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> loggedIn = new ConcurrentHashMap<>();
    private final AtomicInteger lobbyRoundRobin = new AtomicInteger(0);

    public AuthService(
            AuthConfig config,
            UserRepository repository,
            WebhookNotifier webhooks
    ) {
        this.config = config;
        this.repository = repository;
        this.passwordHasher = new PasswordHasher();
        this.rulesEngine = new PasswordRulesEngine(config.passwordRules());
        this.throttle = new LoginThrottle(config.security());
        this.webhooks = webhooks;
        this.queueService = new AuthQueueService();
    }

    public AuthQueueService queue() {
        return queueService;
    }

    public AuthConfig config() {
        return config;
    }

    public void migrateAndLoad(DataSource dataSource) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            new SqlSchema().ensureMigrated(c, config.database().table());
        }
        for (Account a : repository.loadAll()) {
            byUuid.put(a.uuid(), a);
            byLowerName.put(a.nameLower(), a);
        }
    }

    public AccountJoinResult resolveJoin(UUID connectionUuid, String connectionName) {
        Account byUuidHit = byUuid.get(connectionUuid);
        if (byUuidHit != null) {
            if (!byUuidHit.name().equalsIgnoreCase(connectionName)) {
                String oldLower = byUuidHit.nameLower();
                byUuidHit.setName(connectionName);
                repository.update(byUuidHit);
                byLowerName.remove(oldLower);
                byLowerName.put(byUuidHit.nameLower(), byUuidHit);
            }
            return AccountJoinResult.ok(byUuidHit);
        }
        Account sameName = byLowerName.get(connectionName.toLowerCase());
        if (sameName != null && !sameName.uuid().equals(connectionUuid)) {
            if (sameName.premium()) {
                UUID previous = sameName.uuid();
                byUuid.remove(previous);
                repository.changeUuid(previous, connectionUuid);
                sameName.reassignUuid(connectionUuid);
                byUuid.put(connectionUuid, sameName);
                return AccountJoinResult.ok(sameName);
            }
            return AccountJoinResult.conflict(sameName);
        }
        Account created = Account.newPending(connectionUuid, connectionName);
        repository.upsert(created);
        byUuid.put(created.uuid(), created);
        byLowerName.put(created.nameLower(), created);
        return AccountJoinResult.ok(created);
    }

    public Optional<Account> account(UUID uuid) {
        return Optional.ofNullable(byUuid.get(uuid));
    }

    public Optional<Account> accountByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byLowerName.get(name.toLowerCase()));
    }

    public boolean isLoggedIn(UUID uuid) {
        return Boolean.TRUE.equals(loggedIn.get(uuid));
    }

    public void setLoggedIn(UUID uuid, boolean value) {
        if (value) {
            loggedIn.put(uuid, true);
        } else {
            loggedIn.remove(uuid);
        }
    }

    public void onQuit(UUID uuid) {
        loggedIn.remove(uuid);
        queueService.leave(uuid);
    }

    public LoginAttemptResult tryLogin(Account account, String password, String ip, String serverLabel) {
        if (!account.registered()) {
            return LoginAttemptResult.NOT_REGISTERED;
        }
        LoginThrottle.ThrottleState ts = throttle.allowNewAttempt(ip);
        if (ts == LoginThrottle.ThrottleState.LOCKED) {
            return LoginAttemptResult.LOCKED_OUT;
        }
        if (ts == LoginThrottle.ThrottleState.RATE_LIMITED) {
            return LoginAttemptResult.RATE_LIMITED;
        }
        throttle.recordAttempt(ip);
        boolean verified = verifyAndMigratePassword(account, password);
        if (!verified) {
            if (throttle.onWrongPassword(ip) == LoginThrottle.WrongPasswordOutcome.LOCKED) {
                return LoginAttemptResult.LOCKED_OUT;
            }
            return LoginAttemptResult.WRONG_PASSWORD;
        }
        throttle.clearFailures(ip);
        account.setLastIp(ip);
        account.setLastLoginEpochMs(System.currentTimeMillis());
        repository.update(account);
        loggedIn.put(account.uuid(), true);
        webhooks.loginSuccess(account.name(), serverLabel);
        return LoginAttemptResult.SUCCESS;
    }

    private boolean verifyAndMigratePassword(Account account, String password) {
        String hash = account.passwordHash();
        if (hash != null && !hash.isEmpty()) {
            return passwordHasher.verify(password, hash);
        }
        String legacy = account.passwordLegacy();
        if (legacy != null && !legacy.isEmpty() && password != null && legacy.equals(password)) {
            account.setPasswordHash(passwordHasher.hash(password));
            account.setPasswordLegacy("");
            repository.update(account);
            return true;
        }
        return false;
    }

    public boolean verifyPasswordReadOnly(Account account, String password) {
        String hash = account.passwordHash();
        if (hash != null && !hash.isEmpty()) {
            return passwordHasher.verify(password, hash);
        }
        String legacy = account.passwordLegacy();
        return legacy != null && !legacy.isEmpty() && password != null && legacy.equals(password);
    }

    public RegisterAttemptResult tryRegister(Account account, String pass1, String pass2, String ip, String serverLabel) {
        if (account.registered()) {
            return RegisterAttemptResult.ALREADY_REGISTERED;
        }
        LoginThrottle.ThrottleState ts = throttle.allowNewAttempt(ip);
        if (ts == LoginThrottle.ThrottleState.LOCKED) {
            return RegisterAttemptResult.LOCKED_OUT;
        }
        if (ts == LoginThrottle.ThrottleState.RATE_LIMITED) {
            return RegisterAttemptResult.RATE_LIMITED;
        }
        if (pass1 == null || pass2 == null || !pass1.equals(pass2)) {
            return RegisterAttemptResult.PASSWORDS_MISMATCH;
        }
        List<String> violations = rulesEngine.validate(pass1);
        if (!violations.isEmpty()) {
            if (violations.contains("length")) {
                return RegisterAttemptResult.RULE_VIOLATION_LENGTH;
            }
            if (violations.contains("digits")) {
                return RegisterAttemptResult.RULE_VIOLATION_DIGITS;
            }
            return RegisterAttemptResult.RULE_VIOLATION_SPECIAL;
        }
        throttle.recordAttempt(ip);
        account.setPasswordHash(passwordHasher.hash(pass1));
        account.setPasswordLegacy("");
        account.setRegistered(true);
        account.setPremium(false);
        account.setLastIp(ip);
        account.setLastLoginEpochMs(System.currentTimeMillis());
        repository.update(account);
        loggedIn.put(account.uuid(), true);
        throttle.clearFailures(ip);
        webhooks.firstJoin(account.name(), serverLabel);
        return RegisterAttemptResult.SUCCESS;
    }

    public void applyPremiumConfirmed(Account account) {
        account.setPremium(true);
        repository.update(account);
    }

    public void applyUnregister(Account account) {
        account.setRegistered(false);
        account.setPasswordHash(null);
        account.setPasswordLegacy("");
        account.setPremium(false);
        repository.update(account);
        loggedIn.remove(account.uuid());
    }

    public void applyPasswordChange(Account account, String newPlain) {
        account.setPasswordHash(passwordHasher.hash(newPlain));
        account.setPasswordLegacy("");
        repository.update(account);
    }

    public void applyAdminPassword(Account account, String newPlain) {
        applyPasswordChange(account, newPlain);
    }

    public void deleteAccount(Account account) {
        repository.delete(account.uuid());
        byUuid.remove(account.uuid());
        byLowerName.remove(account.nameLower());
        loggedIn.remove(account.uuid());
    }

    public void persist(Account account) {
        repository.update(account);
    }

    public Optional<String> pickLobbyServer(Predicate<String> existsOnProxy) {
        List<String> list = config.servers().lobbyServers();
        if (list.isEmpty()) {
            return Optional.empty();
        }
        return switch (config.servers().lobbyPickMode()) {
            case FIRST_AVAILABLE -> firstExisting(list, existsOnProxy);
            case RANDOM -> {
                List<String> copy = new ArrayList<>(list);
                Collections.shuffle(copy, ThreadLocalRandom.current());
                yield firstExisting(copy, existsOnProxy);
            }
            case ROUND_ROBIN -> {
                int n = list.size();
                int start = Math.floorMod(lobbyRoundRobin.getAndIncrement(), n);
                Optional<String> found = Optional.empty();
                for (int i = 0; i < n; i++) {
                    String s = list.get((start + i) % n);
                    if (existsOnProxy.test(s)) {
                        found = Optional.of(s);
                        break;
                    }
                }
                yield found;
            }
        };
    }

    private static Optional<String> firstExisting(List<String> names, Predicate<String> existsOnProxy) {
        for (String s : names) {
            if (existsOnProxy.test(s)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }
}
