package pl.jms.auth.bungee.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import pl.jms.auth.bungee.util.BungeeText;
import pl.jms.auth.core.auth.AuthService;
import pl.jms.auth.core.model.Account;

public final class ChangePasswordCommand extends Command {

    private final AuthService auth;

    public ChangePasswordCommand(AuthService auth) {
        super("changepassword", null);
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
        if (args.length != 2) {
            player.sendMessage(BungeeText.colorize("&8>> &7Usage: &c/changepassword <old> <new>"));
            return;
        }
        if (!account.registered() || !auth.isLoggedIn(player.getUniqueId())) {
            return;
        }
        if (!auth.verifyPasswordReadOnly(account, args[0])) {
            player.sendMessage(BungeeText.colorize("&8>> &cIncorrect old password."));
            return;
        }
        if (args[0].equals(args[1])) {
            player.sendMessage(BungeeText.colorize("&8>> &cNew password must differ."));
            return;
        }
        var violations = new pl.jms.auth.core.security.PasswordRulesEngine(auth.config().passwordRules()).validate(args[1]);
        if (!violations.isEmpty()) {
            player.sendMessage(BungeeText.colorize(auth.config().messages().passwordLength()));
            return;
        }
        auth.applyPasswordChange(account, args[1]);
        player.sendMessage(BungeeText.colorize("&8>> &aPassword updated."));
    }
}
