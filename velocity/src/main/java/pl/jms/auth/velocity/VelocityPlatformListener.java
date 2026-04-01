package pl.jms.auth.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import pl.jms.auth.core.auth.AccountJoinResult;
import pl.jms.auth.core.auth.AuthService;
import pl.jms.auth.core.model.Account;

import java.net.InetSocketAddress;

public final class VelocityPlatformListener {

    private final AuthService auth;
    private final ProxyServer server;

    public VelocityPlatformListener(VelocityAuthPlugin ignored, AuthService auth, ProxyServer server) {
        this.auth = auth;
        this.server = server;
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        for (Player o : server.getAllPlayers()) {
            if (!o.getUniqueId().equals(player.getUniqueId()) && o.getUsername().equalsIgnoreCase(player.getUsername())) {
                event.setResult(LoginEvent.ComponentResult.denied(VelocityText.parse(auth.config().messages().playerInServer())));
                return;
            }
        }
        AccountJoinResult result = auth.resolveJoin(player.getUniqueId(), player.getUsername());
        if (result.nameClaimedByOtherUuid()) {
            Account holder = result.accountHolderForName();
            String msg = auth.config().messages().wrongNickname().replace("{CORRECT_NICK}", holder.name());
            event.setResult(LoginEvent.ComponentResult.denied(VelocityText.parse(msg)));
            return;
        }
        Account account = result.activeAccount();
        auth.queue().join(player.getUniqueId());

        if (account.premium()) {
            if (!isLikelyOnlineModeUuid(player.getUniqueId())) {
                event.setResult(LoginEvent.ComponentResult.denied(VelocityText.parse("&cPremium accounts require an online-mode UUID. Reconnect using a Mojang session.")));
                return;
            }
            auth.setLoggedIn(account.uuid(), true);
            return;
        }

        auth.setLoggedIn(account.uuid(), false);
        if (!account.registered()) {
            player.sendMessage(VelocityText.parse(auth.config().messages().registerPrompt()));
            return;
        }
        if (!account.nameMatchesConnection(player.getUsername())) {
            String msg = auth.config().messages().wrongNickname().replace("{CORRECT_NICK}", account.name());
            event.setResult(LoginEvent.ComponentResult.denied(VelocityText.parse(msg)));
            return;
        }
        player.sendMessage(VelocityText.parse(auth.config().messages().login()));
    }

    private static boolean isLikelyOnlineModeUuid(java.util.UUID uuid) {
        return uuid.version() == 4;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        auth.onQuit(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        Account account = auth.account(player.getUniqueId()).orElse(null);
        if (account == null) {
            return;
        }
        if (!account.nameMatchesConnection(player.getUsername())) {
            String msg = auth.config().messages().wrongNickname().replace("{CORRECT_NICK}", account.name());
            player.disconnect(VelocityText.parse(msg));
            return;
        }
        String raw = event.getMessage();
        if (!account.registered()) {
            if (auth.config().security().commandAllowed(raw)) {
                return;
            }
            event.setResult(PlayerChatEvent.ChatResult.denied());
            player.sendMessage(VelocityText.parse(auth.config().messages().registerPrompt()));
            return;
        }
        if (!auth.isLoggedIn(player.getUniqueId())) {
            if (auth.config().security().commandAllowed(raw)) {
                return;
            }
            event.setResult(PlayerChatEvent.ChatResult.denied());
            player.sendMessage(VelocityText.parse(auth.config().messages().login()));
        }
    }

    public static String ipOf(Player player) {
        InetSocketAddress isa = player.getRemoteAddress();
        if (isa == null) {
            return "";
        }
        return isa.getAddress().getHostAddress();
    }
}
