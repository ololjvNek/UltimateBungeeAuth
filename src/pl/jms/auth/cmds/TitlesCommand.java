package pl.jms.auth.cmds;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import pl.jms.auth.Main;
import pl.jms.auth.managers.UserManager;
import pl.jms.auth.utils.User;
import pl.jms.auth.utils.Util;

public class TitlesCommand extends Command {
    public TitlesCommand() {
        super("titles", "", Main.configuration.getString("settings.titlesCommand"));
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        final ProxiedPlayer player = (ProxiedPlayer) commandSender;
        final User u = UserManager.getUser(player);
        assert u != null;
        u.setTitlesEnabled(!u.isTitlesEnabled());
        u.update();
        Util.sendMessage(player, (u.isTitlesEnabled() ? Main.configuration.getString("messages.titlesOn") : Main.configuration.getString("messages.titlesOff")));
    }
}
