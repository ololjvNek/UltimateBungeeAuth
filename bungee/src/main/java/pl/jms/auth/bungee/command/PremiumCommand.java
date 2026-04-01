package pl.jms.auth.bungee.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import pl.jms.auth.bungee.util.BungeeText;
import pl.jms.auth.core.auth.AuthService;
import pl.jms.auth.core.model.Account;

public final class PremiumCommand extends Command {

    private final AuthService auth;

    public PremiumCommand(AuthService auth) {
        super("premium", null);
        this.auth = auth;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            return;
        }
        Account account = auth.account(player.getUniqueId()).orElse(null);
        if (account == null) {
            return;
        }
        if (args.length < 1) {
            player.sendMessage(BungeeText.colorize("&8>> &7Confirm with &a/premium confirm &7if you own a Mojang/Microsoft account."));
            return;
        }
        if ("confirm".equalsIgnoreCase(args[0])) {
            auth.applyPremiumConfirmed(account);
            player.disconnect(new TextComponent(BungeeText.colorize("&8>> &aPremium flag saved. Reconnect with an online session.")));
        }
    }
}
