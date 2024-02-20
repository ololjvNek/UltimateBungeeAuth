package pl.jms.auth.cmds;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import pl.jms.auth.managers.UserManager;
import pl.jms.auth.utils.User;
import pl.jms.auth.utils.Util;

public class UnregisterCommand extends Command{

	public UnregisterCommand() {
		super("unregister");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		ProxiedPlayer p = (ProxiedPlayer) sender;
		User u = UserManager.getUser(p);
		if(args.length == 0){
			Util.sendMessage(p, "&8>> &7Correct usage: &c/unregister <password>");
			return;
		}
		if(u.isRegistered()){
			if(args[0].equals(u.getPassword())){
				u.setLogged(false);
				u.setPassword("null");
				u.setRegistered(false);
				Util.sendMessage(p, "&8>> &7You've been &2unregistered&7!");
			}else{
				Util.sendMessage(p, "&8>> &7Incorrect password!");
			}
		}else{
			Util.sendMessage(p, "&8>> &7You're not &2registered!");
		}
	}
	
	

}
