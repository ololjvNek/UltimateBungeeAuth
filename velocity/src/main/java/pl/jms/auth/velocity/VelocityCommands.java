package pl.jms.auth.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import com.velocitypowered.api.proxy.ProxyServer;
import pl.jms.auth.core.auth.AuthService;
import pl.jms.auth.core.auth.LoginAttemptResult;
import pl.jms.auth.core.auth.RegisterAttemptResult;
import pl.jms.auth.core.model.Account;

public final class VelocityCommands {

    private VelocityCommands() {
    }

    public static final class VLoginCommand implements SimpleCommand {
        private final AuthService auth;

        public VLoginCommand(AuthService auth) {
            this.auth = auth;
        }

        @Override
        public void execute(SimpleCommand.Invocation invocation) {
            if (!(invocation.source() instanceof Player player)) {
                return;
            }
            Account account = auth.account(player.getUniqueId()).orElse(null);
            if (account == null) {
                return;
            }
            if (!account.registered()) {
                player.sendMessage(VelocityText.parse(auth.config().messages().notRegistered()));
                return;
            }
            if (auth.isLoggedIn(player.getUniqueId())) {
                player.sendMessage(VelocityText.parse(auth.config().messages().loggedIn()));
                return;
            }
            String[] args = invocation.arguments();
            if (args.length != 1) {
                player.sendMessage(VelocityText.parse("&8>> &7Usage: &c/login <password>"));
                return;
            }
            String ip = VelocityPlatformListener.ipOf(player);
            LoginAttemptResult r = auth.tryLogin(account, args[0], ip, "velocity");
            switch (r) {
                case SUCCESS -> player.sendMessage(VelocityText.parse(auth.config().messages().loginSuccess()));
                case WRONG_PASSWORD -> player.sendMessage(VelocityText.parse(auth.config().messages().wrongPassword()));
                case RATE_LIMITED -> player.sendMessage(VelocityText.parse(auth.config().messages().rateLimited()));
                case LOCKED_OUT -> player.sendMessage(VelocityText.parse(auth.config().messages().lockedOut()));
                default -> player.sendMessage(VelocityText.parse(auth.config().messages().notRegistered()));
            }
        }
    }

    public static final class VRegisterCommand implements SimpleCommand {
        private final AuthService auth;
        private final ProxyServer server;

        public VRegisterCommand(AuthService auth, ProxyServer server) {
            this.auth = auth;
            this.server = server;
        }

        @Override
        public void execute(SimpleCommand.Invocation invocation) {
            if (!(invocation.source() instanceof Player player)) {
                return;
            }
            Account account = auth.account(player.getUniqueId()).orElse(null);
            if (account == null) {
                return;
            }
            if (account.registered()) {
                player.sendMessage(VelocityText.parse(auth.config().messages().alreadyRegistered()));
                return;
            }
            String[] args = invocation.arguments();
            if (args.length != 2) {
                player.sendMessage(VelocityText.parse("&8>> &7Usage: &c/register <password> <password>"));
                return;
            }
            String ip = VelocityPlatformListener.ipOf(player);
            RegisterAttemptResult r = auth.tryRegister(account, args[0], args[1], ip, "velocity");
            switch (r) {
                case SUCCESS -> {
                    player.sendMessage(VelocityText.parse(auth.config().messages().registerSuccess()));
                    auth.pickLobbyServer(n -> server.getServer(n).isPresent())
                            .flatMap(server::getServer)
                            .ifPresent(lobby -> player.createConnectionRequest(lobby).connect());
                }
                case PASSWORDS_MISMATCH -> player.sendMessage(VelocityText.parse(auth.config().messages().passwordsMismatch()));
                case RULE_VIOLATION_LENGTH -> player.sendMessage(VelocityText.parse(auth.config().messages().passwordLength()));
                case RULE_VIOLATION_DIGITS -> player.sendMessage(VelocityText.parse(auth.config().messages().passwordNumbers()));
                case RULE_VIOLATION_SPECIAL -> player.sendMessage(VelocityText.parse(auth.config().messages().passwordSpecial()));
                case RATE_LIMITED -> player.sendMessage(VelocityText.parse(auth.config().messages().rateLimited()));
                case LOCKED_OUT -> player.sendMessage(VelocityText.parse(auth.config().messages().lockedOut()));
                default -> {
                }
            }
        }
    }

    public static final class VUnregisterCommand implements SimpleCommand {
        private final AuthService auth;

        public VUnregisterCommand(AuthService auth) {
            this.auth = auth;
        }

        @Override
        public void execute(SimpleCommand.Invocation invocation) {
            if (!(invocation.source() instanceof Player player)) {
                return;
            }
            Account account = auth.account(player.getUniqueId()).orElse(null);
            if (account == null || !account.registered()) {
                player.sendMessage(VelocityText.parse(auth.config().messages().unregisterNotRegistered()));
                return;
            }
            String[] args = invocation.arguments();
            if (args.length != 1) {
                player.sendMessage(VelocityText.parse("&8>> &7Usage: &c/unregister <password>"));
                return;
            }
            if (!auth.verifyPasswordReadOnly(account, args[0])) {
                player.sendMessage(VelocityText.parse(auth.config().messages().unregisterWrongPassword()));
                return;
            }
            auth.applyUnregister(account);
            player.sendMessage(VelocityText.parse(auth.config().messages().unregisterSuccess()));
        }
    }

    public static final class VChangePasswordCommand implements SimpleCommand {
        private final AuthService auth;

        public VChangePasswordCommand(AuthService auth) {
            this.auth = auth;
        }

        @Override
        public void execute(SimpleCommand.Invocation invocation) {
            if (!(invocation.source() instanceof Player player)) {
                return;
            }
            Account account = auth.account(player.getUniqueId()).orElse(null);
            if (account == null) {
                return;
            }
            String[] args = invocation.arguments();
            if (args.length != 2) {
                player.sendMessage(VelocityText.parse("&8>> &7Usage: &c/changepassword <old> <new>"));
                return;
            }
            if (!account.registered() || !auth.isLoggedIn(player.getUniqueId())) {
                return;
            }
            if (!auth.verifyPasswordReadOnly(account, args[0])) {
                player.sendMessage(VelocityText.parse("&8>> &cIncorrect old password."));
                return;
            }
            if (args[0].equals(args[1])) {
                player.sendMessage(VelocityText.parse("&8>> &cNew password must differ."));
                return;
            }
            var violations = new pl.jms.auth.core.security.PasswordRulesEngine(auth.config().passwordRules()).validate(args[1]);
            if (!violations.isEmpty()) {
                player.sendMessage(VelocityText.parse(auth.config().messages().passwordLength()));
                return;
            }
            auth.applyPasswordChange(account, args[1]);
            player.sendMessage(VelocityText.parse("&8>> &aPassword updated."));
        }
    }

    public static final class VPremiumCommand implements SimpleCommand {
        private final AuthService auth;

        public VPremiumCommand(AuthService auth) {
            this.auth = auth;
        }

        @Override
        public void execute(SimpleCommand.Invocation invocation) {
            if (!(invocation.source() instanceof Player player)) {
                return;
            }
            Account account = auth.account(player.getUniqueId()).orElse(null);
            if (account == null) {
                return;
            }
            String[] args = invocation.arguments();
            if (args.length < 1) {
                player.sendMessage(VelocityText.parse("&8>> &7Confirm with &a/premium confirm"));
                return;
            }
            if ("confirm".equalsIgnoreCase(args[0])) {
                auth.applyPremiumConfirmed(account);
                player.disconnect(VelocityText.parse("&8>> Premium saved. Reconnect with online mode."));
            }
        }
    }

    public static final class VTitlesCommand implements SimpleCommand {
        private final AuthService auth;

        public VTitlesCommand(AuthService auth) {
            this.auth = auth;
        }

        @Override
        public void execute(SimpleCommand.Invocation invocation) {
            if (!(invocation.source() instanceof Player player)) {
                return;
            }
            Account account = auth.account(player.getUniqueId()).orElse(null);
            if (account == null) {
                return;
            }
            account.setTitlesEnabled(!account.titlesEnabled());
            auth.persist(account);
            String msg = account.titlesEnabled() ? auth.config().messages().titlesOn() : auth.config().messages().titlesOff();
            player.sendMessage(VelocityText.parse(msg));
        }
    }

    public static final class VAdminCommand implements SimpleCommand {
        private final AuthService auth;
        private final ProxyServer server;

        public VAdminCommand(AuthService auth, ProxyServer server) {
            this.auth = auth;
            this.server = server;
        }

        @Override
        public void execute(SimpleCommand.Invocation invocation) {
            if (!(invocation.source() instanceof Player player)) {
                return;
            }
            if (!player.hasPermission(auth.config().security().permissionAdmin())) {
                player.sendMessage(VelocityText.parse("&cNo permission."));
                return;
            }
            String[] args = invocation.arguments();
            if (args.length < 2) {
                player.sendMessage(VelocityText.parse("&9/uba unregister &7<player>"));
                return;
            }
            switch (args[0].toLowerCase()) {
                case "unregister" -> adminUnregister(player, args[1]);
                case "changepassword" -> {
                    if (args.length >= 3) {
                        adminPassword(player, args[1], args[2]);
                    }
                }
                case "changestatus" -> {
                    if (args.length >= 3) {
                        adminStatus(player, args[1], args[2]);
                    }
                }
                case "delete" -> adminDelete(player, args[1]);
                default -> {
                }
            }
        }

        private Account resolve(String name) {
            for (Player p : server.getAllPlayers()) {
                if (p.getUsername().equalsIgnoreCase(name)) {
                    return auth.account(p.getUniqueId()).orElse(null);
                }
            }
            return auth.accountByName(name).orElse(null);
        }

        private void adminUnregister(Player admin, String targetName) {
            Account target = resolve(targetName);
            if (target == null) {
                admin.sendMessage(VelocityText.parse("&cPlayer not found."));
                return;
            }
            auth.applyUnregister(target);
            admin.sendMessage(VelocityText.parse("&aUnregistered &6" + target.name()));
            server.getPlayer(target.name()).ifPresent(p -> p.disconnect(Component.empty()));
        }

        private void adminPassword(Player admin, String targetName, String newPw) {
            Account target = resolve(targetName);
            if (target == null) {
                admin.sendMessage(VelocityText.parse("&cPlayer not found."));
                return;
            }
            var violations = new pl.jms.auth.core.security.PasswordRulesEngine(auth.config().passwordRules()).validate(newPw);
            if (!violations.isEmpty()) {
                admin.sendMessage(VelocityText.parse("&cInvalid password."));
                return;
            }
            auth.applyAdminPassword(target, newPw);
            admin.sendMessage(VelocityText.parse("&aPassword updated."));
            server.getPlayer(target.name()).ifPresent(p -> p.disconnect(Component.empty()));
        }

        private void adminStatus(Player admin, String targetName, String status) {
            Account target = resolve(targetName);
            if (target == null) {
                admin.sendMessage(VelocityText.parse("&cPlayer not found."));
                return;
            }
            target.setPremium("premium".equalsIgnoreCase(status));
            auth.persist(target);
            admin.sendMessage(VelocityText.parse("&aStatus updated."));
            server.getPlayer(target.name()).ifPresent(p -> p.disconnect(Component.empty()));
        }

        private void adminDelete(Player admin, String targetName) {
            Account target = resolve(targetName);
            if (target == null) {
                admin.sendMessage(VelocityText.parse("&cPlayer not found."));
                return;
            }
            auth.deleteAccount(target);
            admin.sendMessage(VelocityText.parse("&aDeleted."));
            server.getPlayer(targetName).ifPresent(p -> p.disconnect(Component.empty()));
        }
    }
}
