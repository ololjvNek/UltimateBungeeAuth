package pl.jms.auth.core.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import pl.jms.auth.core.config.AuthConfig;

import javax.sql.DataSource;

public final class SqlDataSourceFactory {

    public static DataSource create(AuthConfig.Database database) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(database.jdbcUrl());
        config.setUsername(database.user());
        config.setPassword(database.password());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("UltimateBungeeAuth");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setConnectionTimeout(30_000);
        config.setValidationTimeout(5_000);
        return new HikariDataSource(config);
    }
}
