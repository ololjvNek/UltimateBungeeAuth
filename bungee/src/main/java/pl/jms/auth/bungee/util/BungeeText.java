package pl.jms.auth.bungee.util;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BungeeText {

    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})(?!>)");

    private BungeeText() {
    }

    public static String colorize(String text) {
        if (text == null) {
            return "";
        }
        Matcher m = HEX.matcher(text);
        StringBuffer buf = new StringBuffer();
        while (m.find()) {
            String g = m.group(1);
            String rep = String.valueOf(ChatColor.COLOR_CHAR) + 'x'
                    + ChatColor.COLOR_CHAR + g.charAt(0) + ChatColor.COLOR_CHAR + g.charAt(1)
                    + ChatColor.COLOR_CHAR + g.charAt(2) + ChatColor.COLOR_CHAR + g.charAt(3)
                    + ChatColor.COLOR_CHAR + g.charAt(4) + ChatColor.COLOR_CHAR + g.charAt(5);
            m.appendReplacement(buf, Matcher.quoteReplacement(rep));
        }
        m.appendTail(buf);
        return ChatColor.translateAlternateColorCodes('&', buf.toString());
    }
}
