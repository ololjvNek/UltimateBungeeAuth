package pl.jms.auth.bungee.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import pl.jms.auth.bungee.util.BungeeText;
import pl.jms.auth.core.auth.AuthService;
import pl.jms.auth.core.model.Account;

import java.util.Arrays;

public final class TitlesCommand extends Command {

    private final AuthService auth;

    public TitlesCommand(AuthService auth) {
        super("titles", null, aliasesArray(auth));
        this.auth = auth;
    }

    private static String[] aliasesArray(AuthService auth) {
        String raw = auth.config().titlesCommandAliases();
        if (raw == null || raw.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
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
        account.setTitlesEnabled(!account.titlesEnabled());
        auth.persist(account);
        String msg = account.titlesEnabled()
                ? auth.config().messages().titlesOn()
                : auth.config().messages().titlesOff();
        player.sendMessage(BungeeText.colorize(msg));
    }
}
