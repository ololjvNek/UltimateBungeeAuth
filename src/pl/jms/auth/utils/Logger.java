package pl.jms.auth.utils;


import pl.jms.auth.Main;

import java.util.logging.Level;


public final class Logger
{
    public static void info(final String... logs) {
        for (final String s : logs) {
            log(Level.INFO, s);
        }
    }

    public static void warning(final String... logs) {
        for (final String s : logs) {
            log(Level.WARNING, s);
        }
    }

    public static void severe(final String... logs) {
        for (final String s : logs) {
            log(Level.SEVERE, s);
        }
    }

    public static void log(final Level level, final String log) {
        Main.getInstance().getLogger().log(level, log);
    }

    public static void exception(final Throwable cause) {
        cause.printStackTrace();
    }
}