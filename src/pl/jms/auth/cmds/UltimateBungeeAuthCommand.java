package pl.jms.auth.cmds;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import pl.jms.auth.interfaces.Prefix;
import pl.jms.auth.managers.UserManager;
import pl.jms.auth.utils.User;
import pl.jms.auth.utils.Util;

public class UltimateBungeeAuthCommand extends Command implements Prefix {


    public UltimateBungeeAuthCommand() {super("ultimatebungeeauth", "", "uba", "ultimateba", "bungeeauth", "auth");}

    @Override
    public void execute(CommandSender commandSender, String[] args) {

        if(commandSender instanceof ProxiedPlayer){

            final ProxiedPlayer player = (ProxiedPlayer) commandSender;

            if(args.length < 2){

                Util.sendMessage(commandSender, """
                        """ + (player.getPendingConnection().getVersion() >= 735 ? ULTIMATEBUNGEEAUTH$PREFIXHEX : ULTIMATEBUNGEEAUTH$PREFIXNOHEX) + """
                        
                        &9/uba unregister <player> &a- Unregistering specified player
                        &9/uba changepassword <player> <password> &a- Change player's password
                        &9/uba changestatus <player> <premium/nonpremium> &a- Changing player's status
                        
                        """);
                return;
            }

            switch (args[0]){
                case "unregister" -> {
                    final ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(args[1]);
                    final User userTarget;
                    if(proxiedPlayer == null){

                        userTarget = UserManager.getUser(args[1]);

                    }else{

                        userTarget = UserManager.getUser(proxiedPlayer);

                    }
                    if(userTarget == null){
                        Util.sendMessage(player, "&8>> &cCan't find player in database");
                        return;
                    }
                    if(userTarget.isRegistered()){
                        userTarget.setLogged(false);
                        userTarget.setRegistered(false);
                        userTarget.setPassword("");
                        userTarget.update();
                        Util.sendMessage(player, "&8>> &aSuccessfully unregistered player &6" + userTarget.getName());
                        if(proxiedPlayer != null){
                            proxiedPlayer.disconnect(new TextComponent(""));
                        }
                    }
                }
                case "changepassword" -> {
                    final ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(args[1]);
                    final User userTarget;
                    if(proxiedPlayer == null){

                        userTarget = UserManager.getUser(args[1]);

                    }else{

                        userTarget = UserManager.getUser(proxiedPlayer);

                    }
                    if(userTarget == null) {
                        Util.sendMessage(player, "&8>> &cCan't find player in database");
                        return;
                    }

                    if(userTarget.isRegistered()){
                        userTarget.setPassword(args[2]);
                        userTarget.update();
                        Util.sendMessage(player, "&8>> &aSuccessfully changed password for player &6" + userTarget.getName());
                        if(proxiedPlayer != null){
                            proxiedPlayer.disconnect(new TextComponent(""));
                        }
                    }
                }
                case "changestatus" -> {
                    final ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(args[1]);
                    final User userTarget;
                    if(proxiedPlayer == null){

                        userTarget = UserManager.getUser(args[1]);

                    }else{

                        userTarget = UserManager.getUser(proxiedPlayer);

                    }
                    if(userTarget == null) {
                        Util.sendMessage(player, "&8>> &cCan't find player in database");
                        return;
                    }

                    if(args[2].equals("premium")){
                        userTarget.setPremium(true);
                    }else{
                        userTarget.setPremium(false);
                    }
                    userTarget.update();
                    Util.sendMessage(player, "&8>> &aSuccessfully changed status for player &6" + userTarget.getName());
                    if(proxiedPlayer != null){
                        proxiedPlayer.disconnect(new TextComponent(""));
                    }
                }
            }

        }else{

            if(args.length < 2){



            }

        }

    }
}
