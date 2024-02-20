package pl.jms.auth.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import pl.jms.auth.Main;
import pl.jms.auth.interfaces.Prefix;

import static net.md_5.bungee.api.ChatColor.COLOR_CHAR;

public class Util implements Prefix {

    public static ChatColor setHEX(final String text) {
        return ChatColor.of(text);
    }
    
    public static String fixColors(final String text) {
        return translateHexColorCodes("&#", "", ChatColor.translateAlternateColorCodes('&', text.replace("{PREFIX}", ULTIMATEBUNGEEAUTH$PREFIXHEX)));
        //return ChatColor.translateAlternateColorCodes('&', translateHexColorCodes("&#", "", text));
    }

    public static String translateHexColorCodes(String startTag, String endTag, String message)
    {
        final Pattern hexPattern = Pattern.compile(startTag + "([A-Fa-f0-9]{6})" + endTag);
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find())
        {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5)
            );
        }
        return matcher.appendTail(buffer).toString();
    }
    
    public static boolean checkPremium(final String name) {
        try {
            final URL url = new URL("http://redinginer.pl/api/haspaid.php?nick=" + name);
            final BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            final String line = in.readLine();
            return line.startsWith("true");
        }
        catch (MalformedURLException ex) {}
        catch (IOException ex2) {}
        return false;
    }
    
    public static boolean hasPaid(final String nick) {
        try {
            final URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + nick);
            final HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36");
            conn.connect();
            return conn.getResponseCode() == 200;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean checkVersion(){

        try {
            final URL url = new URL("");
            final BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            final String line = in.readLine();
            return line.startsWith("true");
        }
        catch (IOException ignored) {}
        return false;

    }
    
    @SuppressWarnings("deprecation")
	public static boolean sendMessage(final CommandSender player, final String text) {
    	player.sendMessage(fixColors(text));
    	return true;
    }
    
    public static void sendTitlePremium(ProxiedPlayer p, int fadeIn, int stay, int fadeOut){
    	Title t = ProxyServer.getInstance().createTitle();
    	t.fadeIn((fadeIn*20));
    	t.stay((stay*20));
    	t.fadeOut((fadeOut*20));
    	t.title(new TextComponent(Util.fixColors(Main.configuration.getString("titles.loginPrefix"))));
    	t.subTitle(new TextComponent(Util.fixColors(Main.configuration.getString("titles.premiumLogin"))));
    	t.send(p);
    }

    public static void sendTitleNopremium(ProxiedPlayer p, int fadeIn, int stay, int fadeOut){
        Title t = ProxyServer.getInstance().createTitle();
        t.fadeIn((fadeIn*20));
        t.stay((stay*20));
        t.fadeOut((fadeOut*20));
        t.title(new TextComponent(Util.fixColors(Main.configuration.getString("titles.loginPrefix"))));
        t.subTitle(new TextComponent(Util.fixColors(Main.configuration.getString("titles.nonpremiumLogin"))));
        t.send(p);
    }
    
    public static void sendTitleSession(ProxiedPlayer p){
    	Title t = ProxyServer.getInstance().createTitle();
    	t.fadeIn(40);
    	t.stay(120);
    	t.fadeOut(40);
        t.title(new TextComponent(Util.fixColors(Main.configuration.getString("titles.loginPrefix"))));
        t.subTitle(new TextComponent(Util.fixColors(Main.configuration.getString("titles.lastSession"))));
    	t.send(p);
    }

}
