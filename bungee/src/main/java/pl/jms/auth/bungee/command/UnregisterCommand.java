package pl.jms.auth.bungee.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import pl.jms.auth.bungee.util.BungeeText;
import pl.jms.auth.core.auth.AuthService;
import pl.jms.auth.core.model.Account;

public final class UnregisterCommand extends Command {

    private final AuthService auth;

    public UnregisterCommand(AuthService auth) {
        super("unregister", null);
        this.auth = auth;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            return;
        }
        Account account = auth.account(player.getUniqueId()).orElse(null);
        if (account == null || !account.registered()) {
            player.sendMessage(BungeeText.colorize(auth.config().messages().unregisterNotRegistered()));
            return;
        }
        if (args.length != 1) {
            player.sendMessage(BungeeText.colorize("&8>> &7Usage: &c/unregister <password>"));
            return;
        }
        if (!auth.verifyPasswordReadOnly(account, args[0])) {
            player.sendMessage(BungeeText.colorize(auth.config().messages().unregisterWrongPassword()));
            return;
        }
        auth.applyUnregister(account);
        player.sendMessage(BungeeText.colorize(auth.config().messages().unregisterSuccess()));
    }
}
