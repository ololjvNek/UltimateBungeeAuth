package pl.jms.auth.core.security;

import pl.jms.auth.core.config.AuthConfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LoginThrottle {

    private final AuthConfig.Security security;
    private final Map<String, List<Long>> ipHits = new ConcurrentHashMap<>();
    private final Map<String, Integer> ipFailures = new ConcurrentHashMap<>();
    private final Map<String, Long> ipLockedUntil = new ConcurrentHashMap<>();

    public LoginThrottle(AuthConfig.Security security) {
        this.security = security;
    }

    public ThrottleState allowNewAttempt(String ip) {
        if (ip == null) {
            ip = "";
        }
        if (security.throttleBypassIp(ip)) {
            return ThrottleState.OK;
        }
        long now = System.currentTimeMillis();
        Long until = ipLockedUntil.get(ip);
        if (until != null && now < until) {
            return ThrottleState.LOCKED;
        }
        pruneHits(ip, now);
        List<Long> hits = ipHits.get(ip);
        int windowMs = Math.max(1, security.rateLimitWindowSeconds()) * 1000;
        int cap = security.maxAuthenticationsPerWindow();
        if (hits != null && hits.size() >= cap) {
            return ThrottleState.RATE_LIMITED;
        }
        return ThrottleState.OK;
    }

    public void recordAttempt(String ip) {
        if (ip == null) {
            ip = "";
        }
        if (security.throttleBypassIp(ip)) {
            return;
        }
        long now = System.currentTimeMillis();
        ipHits.computeIfAbsent(ip, k -> new ArrayList<>()).add(now);
    }

    public WrongPasswordOutcome onWrongPassword(String ip) {
        if (ip == null) {
            ip = "";
        }
        if (security.throttleBypassIp(ip)) {
            return WrongPasswordOutcome.CONTINUE;
        }
        int n = ipFailures.merge(ip, 1, Integer::sum);
        if (n >= security.maxPasswordAttempts()) {
            long until = System.currentTimeMillis() + (long) security.lockoutSeconds() * 1000L;
            ipLockedUntil.put(ip, until);
            ipFailures.remove(ip);
            return WrongPasswordOutcome.LOCKED;
        }
        return WrongPasswordOutcome.CONTINUE;
    }

    public void clearFailures(String ip) {
        if (ip == null) {
            return;
        }
        ipFailures.remove(ip);
    }

    private void pruneHits(String ip, long now) {
        List<Long> hits = ipHits.get(ip);
        if (hits == null) {
            return;
        }
        int windowMs = Math.max(1, security.rateLimitWindowSeconds()) * 1000;
        Iterator<Long> it = hits.iterator();
        while (it.hasNext()) {
            if (now - it.next() > windowMs) {
                it.remove();
            }
        }
    }

    public enum ThrottleState {
        OK, RATE_LIMITED, LOCKED
    }

    public enum WrongPasswordOutcome {
        CONTINUE, LOCKED
    }

    public long lockedRemainingMs(String ip) {
        Long until = ipLockedUntil.get(ip);
        if (until == null) {
            return 0;
        }
        long rem = until - System.currentTimeMillis();
        return Math.max(0, rem);
    }
}
