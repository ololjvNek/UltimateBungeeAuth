package pl.jms.auth.core.storage;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class SqlSchema {

    public void ensureMigrated(Connection connection, String table) throws SQLException {
        if (!tableExists(connection, table)) {
            createFresh(connection, table);
            return;
        }
        Set<String> cols = existingColumns(connection, table);
        if (!cols.contains("name_lower")) {
            execute(connection, "ALTER TABLE `" + table + "` ADD COLUMN `name_lower` VARCHAR(16) NULL");
        }
        if (!cols.contains("password_hash")) {
            execute(connection, "ALTER TABLE `" + table + "` ADD COLUMN `password_hash` VARCHAR(255) NULL");
        }
        if (!cols.contains("last_ip")) {
            execute(connection, "ALTER TABLE `" + table + "` ADD COLUMN `last_ip` VARCHAR(45) NULL");
        }
        if (!cols.contains("last_login")) {
            execute(connection, "ALTER TABLE `" + table + "` ADD COLUMN `last_login` BIGINT NULL");
        }
        execute(connection, "UPDATE `" + table + "` SET `name_lower` = LOWER(`name`) WHERE `name_lower` IS NULL OR `name_lower` = ''");
        try {
            execute(connection, "ALTER TABLE `" + table + "` ADD UNIQUE KEY `uk_uuid` (`uuid`)");
        } catch (SQLException ignored) {
        }
        execute(connection, "ALTER TABLE `" + table + "` MODIFY COLUMN `password` VARCHAR(255) NOT NULL DEFAULT ''");
        try {
            execute(connection, "ALTER TABLE `" + table + "` MODIFY COLUMN `name` VARCHAR(16) NOT NULL");
        } catch (SQLException ignored) {
        }
        try {
            execute(connection, "ALTER TABLE `" + table + "` MODIFY COLUMN `uuid` VARCHAR(36) NOT NULL");
        } catch (SQLException ignored) {
        }
    }

    private void createFresh(Connection connection, String table) throws SQLException {
        String sql = "CREATE TABLE `" + table + "` ("
                + "`id` INT NOT NULL AUTO_INCREMENT,"
                + "`uuid` VARCHAR(36) NOT NULL,"
                + "`name` VARCHAR(16) NOT NULL,"
                + "`name_lower` VARCHAR(16) NOT NULL,"
                + "`password` VARCHAR(255) NOT NULL DEFAULT '',"
                + "`password_hash` VARCHAR(255) NULL,"
                + "`premium` TINYINT(1) NOT NULL DEFAULT 0,"
                + "`registered` TINYINT(1) NOT NULL DEFAULT 0,"
                + "`titlesEnabled` TINYINT(1) NOT NULL DEFAULT 1,"
                + "`last_ip` VARCHAR(45) NULL,"
                + "`last_login` BIGINT NULL,"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `uk_uuid` (`uuid`),"
                + "KEY `idx_name_lower` (`name_lower`)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        execute(connection, sql);
    }

    private static boolean tableExists(Connection c, String table) throws SQLException {
        DatabaseMetaData meta = c.getMetaData();
        try (ResultSet rs = meta.getTables(c.getCatalog(), null, table, null)) {
            return rs.next();
        }
    }

    private static Set<String> existingColumns(Connection c, String table) throws SQLException {
        Set<String> set = new HashSet<>();
        DatabaseMetaData meta = c.getMetaData();
        try (ResultSet rs = meta.getColumns(c.getCatalog(), null, table, null)) {
            while (rs.next()) {
                set.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return set;
    }

    private static void execute(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }
}
