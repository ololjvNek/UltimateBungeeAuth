package pl.jms.auth;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.w3c.dom.Text;
import pl.jms.auth.cmds.*;
import pl.jms.auth.database.mysql.Store;
import pl.jms.auth.database.mysql.modes.StoreMySQL;
import pl.jms.auth.listeners.BigListener;
import pl.jms.auth.listeners.ChannelListener;
import pl.jms.auth.managers.QueueManager;
import pl.jms.auth.managers.UserManager;
import pl.jms.auth.utils.Queue;
import pl.jms.auth.utils.User;
import pl.jms.auth.utils.Util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

public class Main extends Plugin{
	
	public static Main instance;
	
	public static Main getInstance(){
		return instance;
	}

	public static Store store;

	public static Configuration configuration;

	public void onEnable(){
		if (!getDataFolder().exists())
			getDataFolder().mkdir();

		File file = new File(getDataFolder(), "config.yml");

		if (!file.exists()) {
			try (InputStream in = getResourceAsStream("config.yml")) {
				Files.copy(in, file.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		instance = this;
		store = new StoreMySQL(configuration.getString("database.host"), configuration.getInt("database.port"), configuration.getString("database.user"), configuration.getString("database.password"), configuration.getString("database.name"), "");
		boolean conn = store.connect();
		if(conn){
			store.update(true, "CREATE TABLE IF NOT EXISTS `authusers` (`id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT, `name` varchar(32) NOT NULL, `password` varchar(32) NOT NULL, `premium` tinyint(1) NOT NULL, `registered` tinyint(1) NOT NULL, `titlesEnabled` tinyint(1) NOT NULL);");
		}
		UserManager.loadUsers();
		ProxyServer.getInstance().getPluginManager().registerCommand(this, new UltimateBungeeAuthCommand());
		ProxyServer.getInstance().getPluginManager().registerCommand(this, new LoginCommand());
		ProxyServer.getInstance().getPluginManager().registerCommand(this, new RegisterCommand());
		ProxyServer.getInstance().getPluginManager().registerCommand(this, new UnregisterCommand());
		ProxyServer.getInstance().getPluginManager().registerCommand(this, new ChangePasswordCommand());
		ProxyServer.getInstance().getPluginManager().registerCommand(this, new PremiumCommand());
		ProxyServer.getInstance().getPluginManager().registerCommand(this, new TitlesCommand());
		new BigListener(this);
        BungeeCord.getInstance().getPluginManager().registerListener(this, new ChannelListener());
        BungeeCord.getInstance().registerChannel("Return");
		getLogger().info("UltimateBungeeAuth Loaded!");



		final ServerInfo serverInfoMain = ProxyServer.getInstance().getServerInfo(configuration.getString("settings.broughtServerName"));

		final int fadeIn = configuration.getInt("titles.settings.fadeIn");
		final int stayIn = configuration.getInt("titles.settings.stayIn");
		final int fadeOut = configuration.getInt("titles.settings.fadeOut");

		BungeeCord.getInstance().getScheduler().schedule(this, () -> {

			if(ProxyServer.getInstance().getServerInfo(configuration.getString("settings.authServer")) == null){
				ProxyServer.getInstance().getLogger().info("[UltimateBungeeAuth] Auth server is not configured!");
				return;
			}

			for(final ProxiedPlayer proxiedPlayer : ProxyServer.getInstance().getServerInfo(configuration.getString("settings.authServer")).getPlayers()){
				final User u = UserManager.getUser(proxiedPlayer);
				if(u != null){
					if(u.isLogged() && u.isRegistered()){
						Queue queue = QueueManager.getQueue(proxiedPlayer);

						if(proxiedPlayer.hasPermission("uba.queue.bypass")){

							if(serverInfoMain == null){
								ProxyServer.getInstance().getLogger().info("[UltimateBungeeAuth] Main server is not responding. Is it configured?");
								return;
							}else{
								proxiedPlayer.connect(serverInfoMain);
							}

							if(u.isPremium()){
								if(u.isTitlesEnabled()) Util.sendTitlePremium(proxiedPlayer, fadeIn, stayIn, fadeOut);
							}else{
								if(u.isTitlesEnabled()) Util.sendTitleNopremium(proxiedPlayer, fadeIn, stayIn, fadeOut);
							}

							if(queue != null){
								QueueManager.kolejki.remove(proxiedPlayer);
							}

							proxiedPlayer.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Util.fixColors(configuration.getString("messages.prioritybrought"))));

							continue;
						}

						if(queue == null){
							queue = QueueManager.createQueue(proxiedPlayer);
						}

						if(queue.getPosition() <= 1){
							if(serverInfoMain == null){
								ProxyServer.getInstance().getLogger().info("[UltimateBungeeAuth] Main server is not responding. Is it configured?");
								return;
							}else{
								proxiedPlayer.connect(serverInfoMain);
							}

							if(u.isPremium()){
								if(u.isTitlesEnabled()) Util.sendTitlePremium(proxiedPlayer, fadeIn, stayIn, fadeOut);
							}else{
								if(u.isTitlesEnabled()) Util.sendTitleNopremium(proxiedPlayer, fadeIn, stayIn, fadeOut);
							}

							QueueManager.kolejki.remove(proxiedPlayer);
						}else{
							proxiedPlayer.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Util.fixColors(configuration.getString("messages.inqueue").replace("{QUEUE}", String.valueOf(queue.getPosition())))));
						}


					}else{
						if(!u.isLogged() && u.isRegistered()){
							Util.sendMessage(proxiedPlayer, configuration.getString("messages.login"));
						}else if(!u.isLogged() && !u.isRegistered()){
							Util.sendMessage(proxiedPlayer, configuration.getString("messages.register"));
						}
					}
				}

				for(Queue k : QueueManager.kolejki.values()){
					if(k.getPosition() != 1){
						k.removeOne();
					}
				}
			}

        }, 5000, 5000, TimeUnit.MILLISECONDS);

	}

}
