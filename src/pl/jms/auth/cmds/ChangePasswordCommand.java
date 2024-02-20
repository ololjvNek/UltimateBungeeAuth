package pl.jms.auth.cmds;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import pl.jms.auth.managers.UserManager;
import pl.jms.auth.utils.User;
import pl.jms.auth.utils.Util;

public class ChangePasswordCommand extends Command {
    public ChangePasswordCommand() {
        super("changepassword", "");
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {

        final ProxiedPlayer proxiedPlayer = (ProxiedPlayer) commandSender;
        final User u = UserManager.getUser(proxiedPlayer);

        if(args.length < 2){
            Util.sendMessage(proxiedPlayer, "&8>> &7Correct usage: /changepassword <old password> <new password>");
            return;
        }

        if(u == null) return;

        if(u.isRegistered() && u.isLogged()){
            final String oldPassword = args[0];
            final String newPassword = args[1];

            if(u.getPassword().equals(oldPassword)){
                if(newPassword.equals(oldPassword)){
                    Util.sendMessage(proxiedPlayer, "&8>> &cYou can't change password to old one");
                    return;
                }

                u.setPassword(newPassword);
                Util.sendMessage(proxiedPlayer, "&8>> &aSuccessfully changed your password!");
            }else{
                Util.sendMessage(proxiedPlayer, "&8>> &cPassword is incorrect");
            }
        }

    }
}
