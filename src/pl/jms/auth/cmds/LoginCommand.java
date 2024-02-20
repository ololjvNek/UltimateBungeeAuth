package pl.jms.auth.cmds;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import pl.jms.auth.Main;
import pl.jms.auth.managers.UserManager;
import pl.jms.auth.utils.User;
import pl.jms.auth.utils.Util;

public class LoginCommand extends Command{

	public LoginCommand() {
		super("login", "", "l");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		ProxiedPlayer p = (ProxiedPlayer) sender;
		User u = UserManager.getUser(p);
		if(u.isRegistered()){
			if(!u.isLogged()){
				if(args.length == 1){
					if(args[0].equals(u.getPassword())){
						Util.sendMessage(p, Main.configuration.getString("messages.loginsuccess"));
						u.setLogged(true);
					}else{
						p.disconnect(new TextComponent(Util.fixColors(Main.configuration.getString("messages.wrongpassword"))));
					}
				}else{
					Util.sendMessage(p, "&8>> &7Correct usage: &c/login <password>");
				}
			}else{
				Util.sendMessage(p, Main.configuration.getString("messages.loggedin"));
			}
		}else{
			Util.sendMessage(p, Main.configuration.getString("messages.notregistered"));
		}
	}

}
