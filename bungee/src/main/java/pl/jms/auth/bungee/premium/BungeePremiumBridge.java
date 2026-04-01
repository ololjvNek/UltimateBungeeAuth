package pl.jms.auth.bungee.premium;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public final class BungeePremiumBridge {

    private BungeePremiumBridge() {
    }

    public static void requestOnlineMode(Plugin plugin, PreLoginEvent event, PendingConnection connection) {
        event.registerIntent(plugin);
        ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            connection.setOnlineMode(true);
            event.completeIntent(plugin);
        }, 50L, TimeUnit.MILLISECONDS);
    }
}
