package pl.jms.auth.core.storage;

import pl.jms.auth.core.model.Account;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqlUserRepository implements UserRepository {

    private final DataSource dataSource;
    private final String table;

    public SqlUserRepository(DataSource dataSource, String table) {
        this.dataSource = dataSource;
        this.table = table;
    }

    @Override
    public void upsert(Account account) {
        String sql = "INSERT INTO `" + table + "` "
                + "(`uuid`,`name`,`name_lower`,`password`,`password_hash`,`premium`,`registered`,`titlesEnabled`,`last_ip`,`last_login`) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE "
                + "`name`=VALUES(`name`),`name_lower`=VALUES(`name_lower`),`password`=VALUES(`password`),"
                + "`password_hash`=VALUES(`password_hash`),`premium`=VALUES(`premium`),`registered`=VALUES(`registered`),"
                + "`titlesEnabled`=VALUES(`titlesEnabled`),`last_ip`=VALUES(`last_ip`),`last_login`=VALUES(`last_login`)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            fillStatement(ps, account);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void update(Account account) {
        String sql = "UPDATE `" + table + "` SET "
                + "`name`=?,`name_lower`=?,`password`=?,`password_hash`=?,`premium`=?,`registered`=?,`titlesEnabled`=?,`last_ip`=?,`last_login`=? "
                + "WHERE `uuid`=?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, account.name());
            ps.setString(i++, account.nameLower());
            ps.setString(i++, nullToEmpty(account.passwordLegacy()));
            setStringOrNull(ps, i++, account.passwordHash());
            ps.setInt(i++, account.premium() ? 1 : 0);
            ps.setInt(i++, account.registered() ? 1 : 0);
            ps.setInt(i++, account.titlesEnabled() ? 1 : 0);
            setStringOrNull(ps, i++, account.lastIp());
            if (account.lastLoginEpochMs() != null) {
                ps.setLong(i++, account.lastLoginEpochMs());
            } else {
                ps.setNull(i++, java.sql.Types.BIGINT);
            }
            ps.setString(i, account.uuid().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void delete(UUID uuid) {
        String sql = "DELETE FROM `" + table + "` WHERE `uuid`=?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void changeUuid(UUID oldUuid, UUID newUuid) {
        String sql = "UPDATE `" + table + "` SET `uuid`=? WHERE `uuid`=?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newUuid.toString());
            ps.setString(2, oldUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Optional<Account> findByUuid(UUID uuid) {
        String sql = "SELECT * FROM `" + table + "` WHERE `uuid`=? LIMIT 1";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(read(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Optional<Account> findByNameLower(String nameLower) {
        String sql = "SELECT * FROM `" + table + "` WHERE `name_lower`=? LIMIT 1";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nameLower.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(read(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Collection<Account> loadAll() {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT * FROM `" + table + "`";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(read(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private void fillStatement(PreparedStatement ps, Account account) throws SQLException {
        int i = 1;
        ps.setString(i++, account.uuid().toString());
        ps.setString(i++, account.name());
        ps.setString(i++, account.nameLower());
        ps.setString(i++, nullToEmpty(account.passwordLegacy()));
        setStringOrNull(ps, i++, account.passwordHash());
        ps.setInt(i++, account.premium() ? 1 : 0);
        ps.setInt(i++, account.registered() ? 1 : 0);
        ps.setInt(i++, account.titlesEnabled() ? 1 : 0);
        setStringOrNull(ps, i++, account.lastIp());
        if (account.lastLoginEpochMs() != null) {
            ps.setLong(i++, account.lastLoginEpochMs());
        } else {
            ps.setNull(i++, java.sql.Types.BIGINT);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static void setStringOrNull(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    private static Account read(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid").trim());
        String name = rs.getString("name");
        String nameLower = rs.getString("name_lower");
        if (nameLower == null || nameLower.isEmpty()) {
            nameLower = name.toLowerCase();
        }
        String legacy = rs.getString("password");
        if (legacy != null && legacy.isEmpty()) {
            legacy = null;
        }
        String hash = rs.getString("password_hash");
        if (hash != null && hash.isEmpty()) {
            hash = null;
        }
        boolean premium = rs.getInt("premium") != 0;
        boolean reg = rs.getInt("registered") != 0;
        boolean titles = rs.getInt("titlesEnabled") != 0;
        String lip = rs.getString("last_ip");
        long lastLogin = rs.getLong("last_login");
        boolean wasNull = rs.wasNull();
        return new Account(uuid, name, nameLower, legacy, hash, premium, reg, titles, lip, wasNull ? null : lastLogin);
    }
}
