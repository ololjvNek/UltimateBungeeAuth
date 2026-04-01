package pl.jms.auth.bungee.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import pl.jms.auth.bungee.listener.AuthListener;
import pl.jms.auth.bungee.util.BungeeText;
import pl.jms.auth.core.auth.AuthService;
import pl.jms.auth.core.auth.LoginAttemptResult;
import pl.jms.auth.core.model.Account;

public final class LoginCommand extends Command {

    private final AuthService auth;

    public LoginCommand(AuthService auth) {
        super("login", null, "l");
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
        if (!account.registered()) {
            player.sendMessage(BungeeText.colorize(auth.config().messages().notRegistered()));
            return;
        }
        if (auth.isLoggedIn(player.getUniqueId())) {
            player.sendMessage(BungeeText.colorize(auth.config().messages().loggedIn()));
            return;
        }
        if (args.length != 1) {
            player.sendMessage(BungeeText.colorize("&8>> &7Usage: &c/login <password>"));
            return;
        }
        String ip = AuthListener.ipOf(player);
        LoginAttemptResult r = auth.tryLogin(account, args[0], ip, "bungee");
        switch (r) {
            case SUCCESS -> player.sendMessage(BungeeText.colorize(auth.config().messages().loginSuccess()));
            case WRONG_PASSWORD -> player.sendMessage(BungeeText.colorize(auth.config().messages().wrongPassword()));
            case RATE_LIMITED -> player.sendMessage(BungeeText.colorize(auth.config().messages().rateLimited()));
            case LOCKED_OUT -> player.sendMessage(BungeeText.colorize(auth.config().messages().lockedOut()));
            default -> player.sendMessage(BungeeText.colorize(auth.config().messages().notRegistered()));
        }
    }
}
