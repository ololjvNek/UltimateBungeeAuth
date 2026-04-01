package pl.jms.auth.bungee.listener;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import pl.jms.auth.core.auth.AuthService;
import pl.jms.auth.core.model.Account;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class PluginChannelListener implements Listener {

    private final AuthService auth;

    public PluginChannelListener(AuthService auth) {
        this.auth = auth;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equalsIgnoreCase(auth.config().security().pluginRequestChannel())) {
            return;
        }
        if (!(event.getReceiver() instanceof ProxiedPlayer player)) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String sub = in.readUTF();
            if (!"get".equalsIgnoreCase(sub)) {
                return;
            }
            String payload = in.readUTF();
            if (!"auth".equalsIgnoreCase(payload)) {
                return;
            }
            Account account = auth.account(player.getUniqueId()).orElse(null);
            boolean logged = account != null && auth.isLoggedIn(player.getUniqueId());
            ServerResponder.send(auth, player, sub, logged);
        } catch (IOException ignored) {
        }
    }

    private static final class ServerResponder {
        private static void send(AuthService auth, ProxiedPlayer player, String requestSub, boolean logged) {
            if (player.getServer() == null || player.getServer().getInfo() == null) {
                return;
            }
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(stream)) {
                out.writeUTF(requestSub);
                out.writeUTF(logged ? "true" : "false");
            } catch (IOException ignored) {
                return;
            }
            player.getServer().getInfo().sendData(
                    auth.config().security().pluginResponseChannel(),
                    stream.toByteArray()
            );
        }
    }
}
