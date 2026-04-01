package pl.jms.auth.bungee.listener;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import pl.jms.auth.bungee.UltimateBungeeAuthPlugin;
import pl.jms.auth.bungee.premium.BungeePremiumBridge;
import pl.jms.auth.bungee.util.BungeeText;
import pl.jms.auth.core.auth.AccountJoinResult;
import pl.jms.auth.core.auth.AuthService;
import pl.jms.auth.core.config.AuthConfig;
import pl.jms.auth.core.model.Account;

import java.net.InetSocketAddress;

public final class AuthListener implements Listener {

    private final UltimateBungeeAuthPlugin plugin;
    private final AuthService auth;

    public AuthListener(UltimateBungeeAuthPlugin plugin, AuthService auth) {
        this.plugin = plugin;
        this.auth = auth;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(PreLoginEvent event) {
        if (ProxyServer.getInstance().getPlayer(event.getConnection().getUniqueId()) != null) {
            event.setCancelled(true);
            event.setCancelReason(BungeeText.colorize(auth.config().messages().playerInServer()));
            return;
        }
        String joinName = event.getConnection().getName();
        for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
            if (p.getName().equalsIgnoreCase(joinName)) {
                event.setCancelled(true);
                event.setCancelReason(BungeeText.colorize(auth.config().messages().playerInServer()));
                return;
            }
        }
        auth.accountByName(joinName).filter(Account::premium).ifPresent(a ->
                BungeePremiumBridge.requestOnlineMode(plugin, event, event.getConnection())
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        AccountJoinResult result = auth.resolveJoin(player.getUniqueId(), player.getName());
        if (result.nameClaimedByOtherUuid()) {
            Account holder = result.accountHolderForName();
            String msg = auth.config().messages().wrongNickname()
                    .replace("{CORRECT_NICK}", holder.name());
            player.disconnect(new TextComponent(BungeeText.colorize(msg)));
            return;
        }
        Account account = result.activeAccount();
        auth.queue().join(player.getUniqueId());

        if (account.premium()) {
            auth.setLoggedIn(account.uuid(), true);
            maybeSendTitles(player, account, true);
            return;
        }

        auth.setLoggedIn(account.uuid(), false);

        if (!account.registered()) {
            player.sendMessage(BungeeText.colorize(auth.config().messages().registerPrompt()));
            return;
        }

        if (!account.nameMatchesConnection(player.getName())) {
            String msg = auth.config().messages().wrongNickname()
                    .replace("{CORRECT_NICK}", account.name());
            player.disconnect(new TextComponent(BungeeText.colorize(msg)));
            return;
        }

        player.sendMessage(BungeeText.colorize(auth.config().messages().login()));
    }

    private void maybeSendTitles(ProxiedPlayer player, Account account, boolean premiumFlow) {
        AuthConfig.Titles t = auth.config().titles();
        if (!account.titlesEnabled()) {
            return;
        }
        net.md_5.bungee.api.Title title = ProxyServer.getInstance().createTitle()
                .title(new TextComponent(BungeeText.colorize(t.loginPrefix())))
                .subTitle(new TextComponent(BungeeText.colorize(premiumFlow ? t.premiumLogin() : t.nonPremiumLogin())))
                .fadeIn(t.fadeInSeconds() * 20)
                .stay(t.staySeconds() * 20)
                .fadeOut(t.fadeOutSeconds() * 20);
        title.send(player);
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        auth.onQuit(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer player)) {
            return;
        }
        Account account = auth.account(player.getUniqueId()).orElse(null);
        if (account == null) {
            return;
        }
        if (!account.nameMatchesConnection(player.getName())) {
            String msg = auth.config().messages().wrongNickname()
                    .replace("{CORRECT_NICK}", account.name());
            player.disconnect(new TextComponent(BungeeText.colorize(msg)));
            return;
        }
        String msgTxt = event.getMessage();
        if (!account.registered()) {
            if (auth.config().security().commandAllowed(msgTxt)) {
                return;
            }
            event.setCancelled(true);
            player.sendMessage(BungeeText.colorize(auth.config().messages().registerPrompt()));
            return;
        }
        if (!auth.isLoggedIn(player.getUniqueId())) {
            if (auth.config().security().commandAllowed(msgTxt)) {
                return;
            }
            event.setCancelled(true);
            player.sendMessage(BungeeText.colorize(auth.config().messages().login()));
        }
    }

    public static String ipOf(ProxiedPlayer player) {
        if (player.getSocketAddress() instanceof InetSocketAddress isa) {
            return isa.getAddress().getHostAddress();
        }
        return "";
    }
}
