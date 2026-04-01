package pl.jms.auth.core.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AuthConfig {

    public final Database database;
    public final Servers servers;
    public final PasswordRules passwordRules;
    public final Titles titles;
    public final Security security;
    public final Integrations integrations;
    public final Messages messages;
    public final String titlesCommandAliases;
    public final int queueTickSeconds;

    public AuthConfig(
            Database database,
            Servers servers,
            PasswordRules passwordRules,
            Titles titles,
            Security security,
            Integrations integrations,
            Messages messages,
            String titlesCommandAliases,
            int queueTickSeconds
    ) {
        this.database = database;
        this.servers = servers;
        this.passwordRules = passwordRules;
        this.titles = titles;
        this.security = security;
        this.integrations = integrations;
        this.messages = messages;
        this.titlesCommandAliases = titlesCommandAliases;
        this.queueTickSeconds = queueTickSeconds;
    }

    public Database database() {
        return database;
    }

    public Servers servers() {
        return servers;
    }

    public PasswordRules passwordRules() {
        return passwordRules;
    }

    public Titles titles() {
        return titles;
    }

    public Security security() {
        return security;
    }

    public Integrations integrations() {
        return integrations;
    }

    public Messages messages() {
        return messages;
    }

    public String titlesCommandAliases() {
        return titlesCommandAliases;
    }

    public int queueTickSeconds() {
        return queueTickSeconds;
    }

    @SuppressWarnings("unchecked")
    public static AuthConfig fromYamlRoot(Map<String, Object> root) {
        Map<String, Object> db = nested(root, "database");
        Map<String, Object> srv = nested(root, "servers");
        Map<String, Object> pr = nested(root, "passwordRules");
        Map<String, Object> titlesMap = nested(root, "titles");
        Map<String, Object> titlesSettings = nested(titlesMap, "settings");
        Map<String, Object> sec = nested(root, "security");
        Map<String, Object> integ = nested(root, "integrations");
        Map<String, Object> msg = nested(root, "messages");
        Map<String, Object> queueMap = nested(root, "queue");

        Database database = new Database(
                string(db, "host", "localhost"),
                intOf(db, "port", 3306),
                string(db, "user", "root"),
                string(db, "password", ""),
                string(db, "name", "minecraft"),
                string(db, "table", "authusers")
        );

        Servers servers = parseServers(srv);

        PasswordRules passwordRules = new PasswordRules(
                intOf(pr, "minLength", 4),
                intOf(pr, "minUniqueDigitCount", 3),
                intOf(pr, "minSpecialCharacters", 0)
        );

        Titles titles = new Titles(
                intOf(titlesSettings, "fadeInSeconds", 1),
                intOf(titlesSettings, "staySeconds", 3),
                intOf(titlesSettings, "fadeOutSeconds", 1),
                string(titlesMap, "loginPrefix", "&2&lLOGIN"),
                string(titlesMap, "premiumLogin", "&7Premium account"),
                string(titlesMap, "nonPremiumLogin", "&7Offline account"),
                string(titlesMap, "lastSession", "&7Last session")
        );

        List<String> whitelist = stringList(sec, "allowedCommandsBeforeLogin", defaultWhitelist());
        List<String> ipBypass = stringList(sec, "throttleBypassIps", List.of());

        Security security = new Security(
                string(sec, "permissionAdmin", "ultimatebungeeauth.admin"),
                string(sec, "permissionQueueBypass", "ultimatebungeeauth.queue.bypass"),
                intOf(sec, "maxPasswordAttempts", 5),
                intOf(sec, "rateLimitWindowSeconds", 60),
                intOf(sec, "maxAuthenticationsPerWindow", 20),
                intOf(sec, "lockoutSeconds", 300),
                whitelist,
                ipBypass,
                string(sec, "pluginRequestChannel", "BungeeCord"),
                string(sec, "pluginResponseChannel", "Return")
        );

        Map<String, Object> hooks = nested(integ, "webhooks");
        Integrations integrations = new Integrations(
                string(nested(hooks, "firstJoin"), "url", ""),
                string(nested(hooks, "firstJoin"), "format", "First join: {PLAYER} at {TIME}"),
                string(nested(hooks, "loginSuccess"), "url", ""),
                string(nested(hooks, "loginSuccess"), "format", "{PLAYER} logged in at {TIME}")
        );

        Messages messages = new Messages(
                string(msg, "login", ""),
                string(msg, "loginSuccess", ""),
                string(msg, "wrongPassword", ""),
                string(msg, "loggedIn", ""),
                string(msg, "notRegistered", ""),
                string(msg, "registerSuccess", ""),
                string(msg, "registerPrompt", ""),
                string(msg, "passwordsMismatch", ""),
                string(msg, "alreadyRegistered", ""),
                string(msg, "playerInServer", ""),
                string(msg, "wrongNickname", ""),
                string(msg, "priorityBrought", ""),
                string(msg, "inQueue", ""),
                string(msg, "titlesOn", ""),
                string(msg, "titlesOff", ""),
                string(msg, "passwordLength", ""),
                string(msg, "passwordNumbers", ""),
                string(msg, "passwordSpecial", ""),
                string(msg, "tooManyAttempts", ""),
                string(msg, "lockedOut", ""),
                string(msg, "rateLimited", ""),
                string(msg, "unregisterSuccess", ""),
                string(msg, "unregisterNotRegistered", ""),
                string(msg, "unregisterWrongPassword", "")
        );

        return new AuthConfig(
                database,
                servers,
                passwordRules,
                titles,
                security,
                integrations,
                messages,
                string(root, "titlesCommandAliases", "joinTitles"),
                intOf(queueMap, "tickSeconds", 5)
        );
    }

    private static Servers parseServers(Map<String, Object> srv) {
        List<String> authRaw = stringList(srv, "authServers", List.of());
        List<String> auth;
        if (authRaw.isEmpty()) {
            String leg = string(srv, "authServer", "auth").trim();
            auth = leg.isEmpty() ? List.of("auth") : List.of(leg);
        } else {
            auth = authRaw.stream().map(String::trim).filter(x -> !x.isEmpty()).toList();
            if (auth.isEmpty()) {
                String leg = string(srv, "authServer", "auth").trim();
                auth = leg.isEmpty() ? List.of("auth") : List.of(leg);
            }
        }
        List<String> lobbyRaw = stringList(srv, "lobbyServers", List.of());
        List<String> lobby;
        if (lobbyRaw.isEmpty()) {
            String leg = string(srv, "lobbyServer", "lobby").trim();
            lobby = leg.isEmpty() ? List.of("lobby") : List.of(leg);
        } else {
            lobby = lobbyRaw.stream().map(String::trim).filter(x -> !x.isEmpty()).toList();
            if (lobby.isEmpty()) {
                String leg = string(srv, "lobbyServer", "lobby").trim();
                lobby = leg.isEmpty() ? List.of("lobby") : List.of(leg);
            }
        }
        ServerPickMode mode = parsePickMode(string(srv, "lobbyPickMode", "FIRST_AVAILABLE"));
        return new Servers(auth, lobby, mode);
    }

    private static ServerPickMode parsePickMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return ServerPickMode.FIRST_AVAILABLE;
        }
        String n = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return ServerPickMode.valueOf(n);
        } catch (IllegalArgumentException e) {
            return ServerPickMode.FIRST_AVAILABLE;
        }
    }

    private static List<String> defaultWhitelist() {
        return List.of("/login", "/l", "/register", "/reg", "/unregister");
    }

    private static List<String> stringList(Map<String, Object> map, String key, List<String> fallback) {
        Object v = map.get(key);
        if (v instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o != null) {
                    out.add(o.toString());
                }
            }
            return Collections.unmodifiableList(out);
        }
        if (v instanceof String s && !s.isEmpty()) {
            return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(z -> !z.isEmpty())
                    .toList();
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> nested(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Map) {
            return (Map<String, Object>) v;
        }
        return Map.of();
    }

    private static String string(Map<String, Object> map, String key, String def) {
        Object v = map.get(key);
        return v == null ? def : v.toString();
    }

    private static int intOf(Map<String, Object> map, String key, int def) {
        Object v = map.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v != null) {
            try {
                return Integer.parseInt(v.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    public record Database(String host, int port, String user, String password, String name, String table) {
        public String jdbcUrl() {
            return "jdbc:mysql://" + host + ":" + port + "/" + name
                    + "?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=UTC";
        }
    }

    public enum ServerPickMode {
        FIRST_AVAILABLE,
        RANDOM,
        ROUND_ROBIN
    }

    public record Servers(List<String> authServers, List<String> lobbyServers, ServerPickMode lobbyPickMode) {
        public Servers {
            authServers = List.copyOf(authServers);
            lobbyServers = List.copyOf(lobbyServers);
        }

        public boolean isAuthServerName(String serverName) {
            if (serverName == null) {
                return false;
            }
            for (String s : authServers) {
                if (s.equalsIgnoreCase(serverName)) {
                    return true;
                }
            }
            return false;
        }
    }

    public record PasswordRules(int minLength, int minUniqueDigitCount, int minSpecialCharacters) {
    }

    public record Titles(
            int fadeInSeconds,
            int staySeconds,
            int fadeOutSeconds,
            String loginPrefix,
            String premiumLogin,
            String nonPremiumLogin,
            String lastSession
    ) {
    }

    public record Security(
            String permissionAdmin,
            String permissionQueueBypass,
            int maxPasswordAttempts,
            int rateLimitWindowSeconds,
            int maxAuthenticationsPerWindow,
            int lockoutSeconds,
            List<String> allowedCommandsBeforeLogin,
            List<String> throttleBypassIps,
            String pluginRequestChannel,
            String pluginResponseChannel
    ) {
        public boolean commandAllowed(String rawMessage) {
            if (rawMessage == null || rawMessage.isEmpty()) {
                return false;
            }
            String t = rawMessage.trim();
            String lower = t.toLowerCase(Locale.ROOT);
            for (String allowed : allowedCommandsBeforeLogin) {
                String a = allowed.trim().toLowerCase(Locale.ROOT);
                if (lower.equals(a) || lower.startsWith(a + " ")) {
                    return true;
                }
            }
            return false;
        }

        public boolean throttleBypassIp(String ip) {
            if (ip == null || throttleBypassIps.isEmpty()) {
                return false;
            }
            return throttleBypassIps.contains(ip);
        }
    }

    public record Integrations(String firstJoinWebhookUrl, String firstJoinFormat,
                               String loginWebhookUrl, String loginFormat) {
        public boolean firstJoinEnabled() {
            return firstJoinWebhookUrl != null && !firstJoinWebhookUrl.isBlank();
        }

        public boolean loginEnabled() {
            return loginWebhookUrl != null && !loginWebhookUrl.isBlank();
        }
    }

    public record Messages(
            String login,
            String loginSuccess,
            String wrongPassword,
            String loggedIn,
            String notRegistered,
            String registerSuccess,
            String registerPrompt,
            String passwordsMismatch,
            String alreadyRegistered,
            String playerInServer,
            String wrongNickname,
            String priorityBrought,
            String inQueue,
            String titlesOn,
            String titlesOff,
            String passwordLength,
            String passwordNumbers,
            String passwordSpecial,
            String tooManyAttempts,
            String lockedOut,
            String rateLimited,
            String unregisterSuccess,
            String unregisterNotRegistered,
            String unregisterWrongPassword
    ) {
    }
}
