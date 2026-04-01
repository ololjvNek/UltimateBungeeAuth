package pl.jms.auth.bungee.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import pl.jms.auth.bungee.util.BungeeText;
import pl.jms.auth.core.auth.AuthService;
import pl.jms.auth.core.model.Account;

public final class AdminCommand extends Command {

    private final AuthService auth;

    public AdminCommand(AuthService auth) {
        super("ultimatebungeeauth", null, "uba", "ultimateba", "bungeeauth");
        this.auth = auth;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            return;
        }
        if (!player.hasPermission(auth.config().security().permissionAdmin())) {
            player.sendMessage(BungeeText.colorize("&cNo permission."));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(BungeeText.colorize("&9/uba unregister &7<player>"));
            player.sendMessage(BungeeText.colorize("&9/uba changepassword &7<player> <password>"));
            player.sendMessage(BungeeText.colorize("&9/uba changestatus &7<player> premium|nonpremium"));
            player.sendMessage(BungeeText.colorize("&9/uba delete &7<player>"));
            return;
        }
        switch (args[0].toLowerCase()) {
            case "unregister" -> handleUnregister(player, args[1]);
            case "changepassword" -> {
                if (args.length < 3) {
                    return;
                }
                handleChangePassword(player, args[1], args[2]);
            }
            case "changestatus" -> {
                if (args.length < 3) {
                    return;
                }
                handleChangeStatus(player, args[1], args[2]);
            }
            case "delete" -> handleDelete(player, args[1]);
            default -> {
            }
        }
    }

    private Account resolveTarget(String name, ProxiedPlayer onlineHint) {
        ProxiedPlayer o = ProxyServer.getInstance().getPlayer(name);
        if (o != null) {
            return auth.account(o.getUniqueId()).orElse(null);
        }
        return auth.accountByName(name).orElse(null);
    }

    private void handleUnregister(ProxiedPlayer admin, String targetName) {
        Account target = resolveTarget(targetName, admin);
        if (target == null) {
            admin.sendMessage(BungeeText.colorize("&cPlayer not found."));
            return;
        }
        if (!target.registered()) {
            admin.sendMessage(BungeeText.colorize("&cNot registered."));
            return;
        }
        auth.applyUnregister(target);
        admin.sendMessage(BungeeText.colorize("&aUnregistered &6" + target.name()));
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(target.name());
        if (p != null) {
            p.disconnect(new TextComponent(""));
        }
    }

    private void handleChangePassword(ProxiedPlayer admin, String targetName, String newPassword) {
        Account target = resolveTarget(targetName, admin);
        if (target == null) {
            admin.sendMessage(BungeeText.colorize("&cPlayer not found."));
            return;
        }
        var violations = new pl.jms.auth.core.security.PasswordRulesEngine(auth.config().passwordRules()).validate(newPassword);
        if (!violations.isEmpty()) {
            admin.sendMessage(BungeeText.colorize("&cPassword does not meet rules."));
            return;
        }
        auth.applyAdminPassword(target, newPassword);
        admin.sendMessage(BungeeText.colorize("&aPassword updated for &6" + target.name()));
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(target.name());
        if (p != null) {
            p.disconnect(new TextComponent(""));
        }
    }

    private void handleChangeStatus(ProxiedPlayer admin, String targetName, String status) {
        Account target = resolveTarget(targetName, admin);
        if (target == null) {
            admin.sendMessage(BungeeText.colorize("&cPlayer not found."));
            return;
        }
        if ("premium".equalsIgnoreCase(status)) {
            target.setPremium(true);
        } else {
            target.setPremium(false);
        }
        auth.persist(target);
        admin.sendMessage(BungeeText.colorize("&aStatus updated for &6" + target.name()));
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(target.name());
        if (p != null) {
            p.disconnect(new TextComponent(""));
        }
    }

    private void handleDelete(ProxiedPlayer admin, String targetName) {
        Account target = resolveTarget(targetName, admin);
        if (target == null) {
            admin.sendMessage(BungeeText.colorize("&cPlayer not found."));
            return;
        }
        auth.deleteAccount(target);
        admin.sendMessage(BungeeText.colorize("&aDeleted account &6" + targetName));
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(targetName);
        if (p != null) {
            p.disconnect(new TextComponent(""));
        }
    }
}
