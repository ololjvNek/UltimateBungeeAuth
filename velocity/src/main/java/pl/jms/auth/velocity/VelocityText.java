package pl.jms.auth.velocity;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VelocityText {

    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})(?!>)");
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();

    private VelocityText() {
    }

    public static Component parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        Matcher m = HEX.matcher(raw);
        StringBuffer buf = new StringBuffer();
        while (m.find()) {
            String g = m.group(1);
            String rep = "§x§" + g.charAt(0) + g.charAt(1) + "§" + g.charAt(2) + g.charAt(3)
                    + "§" + g.charAt(4) + "§" + g.charAt(5);
            m.appendReplacement(buf, Matcher.quoteReplacement(rep));
        }
        m.appendTail(buf);
        return SECTION.deserialize(ampersandToSection(buf.toString()));
    }

    private static String ampersandToSection(String text) {
        char[] chars = text.toCharArray();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '&' && i + 1 < chars.length) {
                b.append('§').append(chars[i + 1]);
                i++;
            } else {
                b.append(chars[i]);
            }
        }
        return b.toString();
    }
}
