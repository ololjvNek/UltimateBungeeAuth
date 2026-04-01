package pl.jms.auth.bungee.task;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import pl.jms.auth.bungee.util.BungeeText;
import pl.jms.auth.core.auth.AuthService;
import pl.jms.auth.core.config.AuthConfig;
import pl.jms.auth.core.model.Account;
import pl.jms.auth.core.queue.AuthQueueService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class QueueTickRunnable implements Runnable {

    private final AuthService auth;

    public QueueTickRunnable(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public void run() {
        AuthConfig cfg = auth.config();
        boolean anyAuth = cfg.servers().authServers().stream()
                .anyMatch(n -> ProxyServer.getInstance().getServerInfo(n) != null);
        if (!anyAuth) {
            return;
        }

        List<UUID> onAuthIds = new ArrayList<>();
        Set<ProxiedPlayer> playersOnAuth = new LinkedHashSet<>();
        for (String authName : cfg.servers().authServers()) {
            ServerInfo si = ProxyServer.getInstance().getServerInfo(authName);
            if (si != null) {
                for (ProxiedPlayer p : si.getPlayers()) {
                    onAuthIds.add(p.getUniqueId());
                    playersOnAuth.add(p);
                }
            }
        }
        auth.queue().clearStale(onAuthIds);

        AuthConfig.Titles titles = cfg.titles();
        AuthQueueService queue = auth.queue();

        for (ProxiedPlayer player : playersOnAuth) {
            Account account = auth.account(player.getUniqueId()).orElse(null);
            if (account == null) {
                continue;
            }
            if (!auth.isLoggedIn(player.getUniqueId()) || !account.registered()) {
                continue;
            }

            ServerInfo lobby = auth.pickLobbyServer(n -> ProxyServer.getInstance().getServerInfo(n) != null)
                    .map(ProxyServer.getInstance()::getServerInfo)
                    .orElse(null);
            if (lobby == null) {
                continue;
            }

            boolean bypass = player.hasPermission(cfg.security().permissionQueueBypass());

            if (bypass) {
                queue.leave(player.getUniqueId());
                maybeTitle(player, account, titles);
                player.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(BungeeText.colorize(cfg.messages().priorityBrought())));
                player.connect(lobby);
                continue;
            }

            queue.join(player.getUniqueId());
            int pos = queue.position(player.getUniqueId());
            if (pos <= 1 || queue.isHead(player.getUniqueId())) {
                queue.leave(player.getUniqueId());
                maybeTitle(player, account, titles);
                player.connect(lobby);
            } else {
                String bar = cfg.messages().inQueue().replace("{QUEUE}", String.valueOf(pos));
                player.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(BungeeText.colorize(bar)));
            }
        }
    }

    private void maybeTitle(ProxiedPlayer player, Account account, AuthConfig.Titles t) {
        if (!account.titlesEnabled()) {
            return;
        }
        net.md_5.bungee.api.Title title = ProxyServer.getInstance().createTitle()
                .title(new TextComponent(BungeeText.colorize(t.loginPrefix())))
                .subTitle(new TextComponent(BungeeText.colorize(account.premium() ? t.premiumLogin() : t.nonPremiumLogin())))
                .fadeIn(t.fadeInSeconds() * 20)
                .stay(t.staySeconds() * 20)
                .fadeOut(t.fadeOutSeconds() * 20);
        title.send(player);
    }
}
