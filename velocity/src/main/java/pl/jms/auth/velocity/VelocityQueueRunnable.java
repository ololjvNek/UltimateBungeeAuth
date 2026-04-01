package pl.jms.auth.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import pl.jms.auth.core.auth.AuthService;
import pl.jms.auth.core.config.AuthConfig;
import pl.jms.auth.core.model.Account;
import pl.jms.auth.core.queue.AuthQueueService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class VelocityQueueRunnable implements Runnable {

    private final ProxyServer server;
    private final AuthService auth;

    public VelocityQueueRunnable(ProxyServer server, AuthService auth) {
        this.server = server;
        this.auth = auth;
    }

    @Override
    public void run() {
        AuthConfig cfg = auth.config();
        boolean anyAuth = cfg.servers().authServers().stream()
                .anyMatch(n -> server.getServer(n).isPresent());
        if (!anyAuth) {
            return;
        }

        List<UUID> onAuth = new ArrayList<>();
        Set<Player> playersOnAuth = new LinkedHashSet<>();
        for (Player p : server.getAllPlayers()) {
            p.getCurrentServer().ifPresent(rs -> {
                if (cfg.servers().isAuthServerName(rs.getServerInfo().getName())) {
                    onAuth.add(p.getUniqueId());
                    playersOnAuth.add(p);
                }
            });
        }
        auth.queue().clearStale(onAuth);

        AuthQueueService queue = auth.queue();
        AuthConfig.Titles titles = cfg.titles();

        for (Player player : playersOnAuth) {
            Account account = auth.account(player.getUniqueId()).orElse(null);
            if (account == null) {
                continue;
            }
            if (!auth.isLoggedIn(player.getUniqueId()) || !account.registered()) {
                continue;
            }

            RegisteredServer lobby = auth.pickLobbyServer(n -> server.getServer(n).isPresent())
                    .flatMap(server::getServer)
                    .orElse(null);
            if (lobby == null) {
                continue;
            }

            boolean bypass = player.hasPermission(cfg.security().permissionQueueBypass());
            if (bypass) {
                queue.leave(player.getUniqueId());
                player.sendActionBar(VelocityText.parse(cfg.messages().priorityBrought()));
                player.createConnectionRequest(lobby).connect();
                maybeTitle(player, account, titles);
                continue;
            }
            queue.join(player.getUniqueId());
            int pos = queue.position(player.getUniqueId());
            if (pos <= 1 || queue.isHead(player.getUniqueId())) {
                queue.leave(player.getUniqueId());
                maybeTitle(player, account, titles);
                player.createConnectionRequest(lobby).connect();
            } else {
                String bar = cfg.messages().inQueue().replace("{QUEUE}", String.valueOf(pos));
                player.sendActionBar(VelocityText.parse(bar));
            }
        }
    }

    private void maybeTitle(Player player, Account account, AuthConfig.Titles t) {
        if (!account.titlesEnabled()) {
            return;
        }
        net.kyori.adventure.title.Title title = net.kyori.adventure.title.Title.title(
                VelocityText.parse(t.loginPrefix()),
                VelocityText.parse(account.premium() ? t.premiumLogin() : t.nonPremiumLogin()),
                net.kyori.adventure.title.Title.Times.times(
                        Duration.ofSeconds(t.fadeInSeconds()),
                        Duration.ofSeconds(t.staySeconds()),
                        Duration.ofSeconds(t.fadeOutSeconds())
                )
        );
        player.showTitle(title);
    }
}
