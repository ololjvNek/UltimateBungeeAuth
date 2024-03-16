package pl.jms.auth.cmds;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import pl.jms.auth.Main;
import pl.jms.auth.managers.UserManager;
import pl.jms.auth.utils.User;
import pl.jms.auth.utils.Util;

import java.util.List;

public class RegisterCommand extends Command{

	public RegisterCommand() {
		super("register", "", "reg");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		ProxiedPlayer p = (ProxiedPlayer) sender;
		User u = UserManager.getUser(p);
		if(!u.isRegistered()){
			if(args.length == 2){

				if(args[0].length() < Main.configuration.getInt("settings.passwordMinLength")){
					Util.sendMessage(p, Main.configuration.getString("messages.passwordlength"));
					return;
				}

				final List<Integer> numbersInString = Util.getNumbersInText(args[0]);

				boolean isDuplicate = false;

				int lastNumber = -1;
				for(final int i : numbersInString){
					if(lastNumber != -1 && lastNumber == i){
						isDuplicate = true;
					}
					lastNumber = i;
				}

				if(isDuplicate || numbersInString.size() < Main.configuration.getInt("settings.passwordMinNumbers")){
					Util.sendMessage(p, Main.configuration.getString("messages.passwordnumbers"));
					return;
				}

				if(args[0].equals(args[1])){
					Util.sendMessage(p, Main.configuration.getString("messages.registersuccess"));
					u.setRegistered(true);
					u.setPassword(args[0]);
					u.setLogged(true);
					u.setPremium(false);
					ServerInfo si = ProxyServer.getInstance().getServerInfo("main");
					p.connect(si);
				}else{
					Util.sendMessage(p, Main.configuration.getString("messages.passwordsincorrect"));
				}
			}else{
				Util.sendMessage(p, "&8>> &7Correct usage: &c/register <password> <password>");
			}
		}else{
			Util.sendMessage(p, Main.configuration.getString("messages.registered"));
		}
	}

}
