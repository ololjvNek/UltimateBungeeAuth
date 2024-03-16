package pl.jms.auth.listeners;

import com.google.common.base.Throwables;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
//import pl.blazingpack.bpauth.BlazingPackAuthEvent;
//import pl.blazingpack.bpauth.BlazingPackHandshakeAuthEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.jms.auth.Main;
import pl.jms.auth.managers.QueueManager;
import pl.jms.auth.managers.UserManager;
import pl.jms.auth.tasks.AsyncPremiumTask;
import pl.jms.auth.utils.User;
import pl.jms.auth.utils.Util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BigListener implements Listener{

	public BigListener(Main plugin){
		ProxyServer.getInstance().getPluginManager().registerListener(plugin, this);
	}
	private static final String UUID_FIELD_NAME = "uniqueId";
	protected static final MethodHandle UNIQUE_ID_SETTER;

	static {
		MethodHandle setHandle = null;
		try {
			MethodHandles.Lookup lookup = MethodHandles.lookup();

			Class.forName("net.md_5.bungee.connection.InitialHandler");

			Field uuidField = InitialHandler.class.getDeclaredField(UUID_FIELD_NAME);
			uuidField.setAccessible(true);
			setHandle = lookup.unreflectSetter(uuidField);
		} catch (ReflectiveOperationException reflectiveOperationException) {
			Logger logger = LoggerFactory.getLogger(BigListener.class);
			logger.error(
					"Cannot find Bungee initial handler; Disabling premium UUID and skin won't work.",
					reflectiveOperationException
			);
		}

		UNIQUE_ID_SETTER = setHandle;
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGHEST)
	public void PreLogin(PreLoginEvent e){
		ProxyServer.getInstance().getLogger().info("PRELOGIN " + e.getConnection().toString());

		ProxiedPlayer p = ProxyServer.getInstance().getPlayer(e.getConnection().getUniqueId());
		if(p != null){
			e.setCancelled(true);
			e.setCancelReason(Util.fixColors(Main.configuration.getString("messages.playerinserver")));
			return;
		}
		for(ProxiedPlayer player : BungeeCord.getInstance().getPlayers()){
			if(player.getName().equalsIgnoreCase(e.getConnection().getName())){
				e.setCancelled(true);
				e.setCancelReason(Util.fixColors(Main.configuration.getString("messages.playerinserver")));
				return;
			}
		}
		final User user = UserManager.getUser(e.getConnection().getName());
		if(user != null && user.isPremium()){
			final AsyncPremiumTask asyncPremiumTask = new AsyncPremiumTask(e, e.getConnection());
			final ScheduledTask scheduledTask = ProxyServer.getInstance().getScheduler().schedule(Main.getInstance(), asyncPremiumTask, 1, 2, TimeUnit.SECONDS);
			asyncPremiumTask.setScheduledTask(scheduledTask);
			e.registerIntent(Main.getInstance());
			return;
		}
		if (e.getConnection() != null && e.getConnection().getUniqueId() != null) {
			User u = UserManager.getUser(e.getConnection().getUniqueId());
			if(u == null ) UserManager.createUser(e.getConnection().getName());
			u = UserManager.getUser(e.getConnection().getUniqueId());
			/*if(Util.hasPaid(e.getConnection().getName()) == true){
				UserManager.getUser(e.getConnection().getName()).setPremium(true);
				User u2 = UserManager.getUser(e.getConnection().getName());
				u2.setLogged(true);
				u2.setRegistered(true);
			}*/
		}else {
			if (!UserManager.getUser(e.getConnection().getName()).getName().equals(e.getConnection().getName())) {
				e.setCancelled(true);
				e.setCancelReason(Util.fixColors(Main.configuration.getString("messages.wrongnick").replace("{CORRECT_NICK}", UserManager.getUser(e.getConnection().getName()).getName())));
			}
			/*if(u.isPremium()){
				e.getConnection().setOnlineMode(true);
				u.setRegistered(true);
				u.setLogged(true);
				return;
			}
			u.setLogged(false);*/
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onLogin(PostLoginEvent e){
		ProxiedPlayer p = e.getPlayer();
		UserManager.createUser(p);
		User u = UserManager.getUser(p);
		QueueManager.createQueue(p);
		if(u == null) u = UserManager.createUser(p);
		u.setPlayerConnection(p.getPendingConnection());
		if(u.isPremium()){
			u.setLogged(true);
			u.setRegistered(true);
			return;
		}
		u.setLogged(false);
		if(!u.isRegistered()){
			Util.sendMessage(p, Main.configuration.getString("messages.register"));
			return;
		}
		if (!UserManager.getUser(p).getName().equals(p.getName())) {
			p.disconnect(new TextComponent(Util.fixColors(Main.configuration.getString("messages.wrongnick").replace("{CORRECT_NICK}", UserManager.getUser(p).getName()))));
		}
		Util.sendMessage(p, Main.configuration.getString("messages.login"));
	}

	@EventHandler
	public void onDisconnect(PlayerDisconnectEvent e){
		if(QueueManager.getQueue(e.getPlayer()) != null){
			QueueManager.kolejki.remove(e.getPlayer());
		}
		User u = UserManager.getUser(e.getPlayer());
		assert u != null;
		u.setLogged(false);
		u.update();
	}
	
	@EventHandler
	public void onChat(ChatEvent e){
		ProxiedPlayer p = (ProxiedPlayer) e.getSender();
		User u = UserManager.getUser(p);
		if(!u.isRegistered()){
			if(e.getMessage().startsWith("/")){
				if (!UserManager.getUser(p).getName().equals(p.getName())) {
					p.disconnect(new TextComponent(Util.fixColors(Main.configuration.getString("messages.wrongnick").replace("{CORRECT_NICK}", UserManager.getUser(p).getName()))));
				}
				return;
			}
			e.setCancelled(true);
			Util.sendMessage(p, Main.configuration.getString("messages.register"));
			return;
		}
		if(!u.isLogged()){
			if(e.getMessage().startsWith("/")){
				if (!UserManager.getUser(p).getName().equals(p.getName())) {
					p.disconnect(new TextComponent(Util.fixColors(Main.configuration.getString("messages.wrongnick").replace("{CORRECT_NICK}", UserManager.getUser(p.getName()).getName()))));
				}
				return;
			}
			e.setCancelled(true);
			Util.sendMessage(p, Main.configuration.getString("messages.login"));
		}
	}

}
