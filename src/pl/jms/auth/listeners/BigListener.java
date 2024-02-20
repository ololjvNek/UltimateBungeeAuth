package pl.jms.auth.listeners;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
//import pl.blazingpack.bpauth.BlazingPackAuthEvent;
//import pl.blazingpack.bpauth.BlazingPackHandshakeAuthEvent;
import pl.jms.auth.Main;
import pl.jms.auth.managers.QueueManager;
import pl.jms.auth.managers.UserManager;
import pl.jms.auth.utils.User;
import pl.jms.auth.utils.Util;

public class BigListener implements Listener{

	public BigListener(Main plugin){
		ProxyServer.getInstance().getPluginManager().registerListener(plugin, this);
	}
	
	
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGHEST)
	public void PreLogin(PreLoginEvent e){
		ProxiedPlayer p = ProxyServer.getInstance().getPlayer(e.getConnection().getName());
		if(p != null){
			e.setCancelled(true);
			e.setCancelReason(Util.fixColors(Main.configuration.getString("messages.playerinserver")));
			return;
		}
		ProxiedPlayer p2 = BungeeCord.getInstance().getPlayer(e.getConnection().getName());
		if(p2 != null){
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
		User u = UserManager.getUser(e.getConnection().getName());
		if (u == null) { 
			UserManager.createUser(e.getConnection().getName());
			if(Util.hasPaid(e.getConnection().getName()) == true){
				UserManager.getUser(e.getConnection().getName()).setPremium(true);
				e.getConnection().setOnlineMode(true);
				User u2 = UserManager.getUser(e.getConnection().getName());
				u2.setLogged(true);
				u2.setRegistered(true);
			}
		}else {
			if (!UserManager.getUser(e.getConnection().getName()).getName().equals(e.getConnection().getName())) {
				e.setCancelled(true);
				e.setCancelReason(Util.fixColors(Main.configuration.getString("messages.wrongnick").replace("{CORRECT_NICK}", UserManager.getUser(e.getConnection().getName()).getName())));
			}
			if(u.isPremium()){
				e.getConnection().setOnlineMode(true);
				u.setRegistered(true);
				u.setLogged(true);
				return;
			}
			u.setLogged(false);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onLogin(PostLoginEvent e){
		ProxiedPlayer p = e.getPlayer();
		UserManager.createUser(p);
		User u = UserManager.getUser(p);
		QueueManager.createQueue(p);
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
