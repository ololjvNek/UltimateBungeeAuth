package pl.jms.auth.cmds;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import pl.jms.auth.managers.UserManager;
import pl.jms.auth.utils.User;
import pl.jms.auth.utils.Util;

public class PremiumCommand extends Command {

    public PremiumCommand() {
        super("premium");
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {

        final ProxiedPlayer proxiedPlayer = (ProxiedPlayer) commandSender;
        final User u = UserManager.getUser(proxiedPlayer);

        if(args.length < 1){
            Util.sendMessage(proxiedPlayer, "&8>> &aPlease confirm you want to change your status to &6PREMIUM\n&8>> &7If you don't have a &6PREMIUM &7account just ignore it\n&8>> &7To confirm please type &a/premium confirm");
            return;
        }

        if(args[0].equals("confirm")){
            u.setPremium(true);
            proxiedPlayer.disconnect(new TextComponent(Util.fixColors("&8>> &aSuccessfully changed your status to &6PREMIUM. &aPlease rejoin")));
        }

    }
}
