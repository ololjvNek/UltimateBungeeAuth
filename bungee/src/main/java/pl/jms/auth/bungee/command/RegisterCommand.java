package pl.jms.auth.bungee.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import pl.jms.auth.bungee.listener.AuthListener;
import pl.jms.auth.bungee.util.BungeeText;
import pl.jms.auth.core.auth.AuthService;
import pl.jms.auth.core.auth.RegisterAttemptResult;
import pl.jms.auth.core.model.Account;

public final class RegisterCommand extends Command {

    private final AuthService auth;

    public RegisterCommand(AuthService auth) {
        super("register", null, "reg");
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
        if (account.registered()) {
            player.sendMessage(BungeeText.colorize(auth.config().messages().alreadyRegistered()));
            return;
        }
        if (args.length != 2) {
            player.sendMessage(BungeeText.colorize("&8>> &7Usage: &c/register <password> <password>"));
            return;
        }
        String ip = AuthListener.ipOf(player);
        RegisterAttemptResult r = auth.tryRegister(account, args[0], args[1], ip, "bungee");
        switch (r) {
            case SUCCESS -> {
                player.sendMessage(BungeeText.colorize(auth.config().messages().registerSuccess()));
                auth.pickLobbyServer(n -> ProxyServer.getInstance().getServerInfo(n) != null)
                        .map(ProxyServer.getInstance()::getServerInfo)
                        .ifPresent(player::connect);
            }
            case PASSWORDS_MISMATCH -> player.sendMessage(BungeeText.colorize(auth.config().messages().passwordsMismatch()));
            case RULE_VIOLATION_LENGTH -> player.sendMessage(BungeeText.colorize(auth.config().messages().passwordLength()));
            case RULE_VIOLATION_DIGITS -> player.sendMessage(BungeeText.colorize(auth.config().messages().passwordNumbers()));
            case RULE_VIOLATION_SPECIAL -> player.sendMessage(BungeeText.colorize(auth.config().messages().passwordSpecial()));
            case RATE_LIMITED -> player.sendMessage(BungeeText.colorize(auth.config().messages().rateLimited()));
            case LOCKED_OUT -> player.sendMessage(BungeeText.colorize(auth.config().messages().lockedOut()));
            default -> {
            }
        }
    }
}
