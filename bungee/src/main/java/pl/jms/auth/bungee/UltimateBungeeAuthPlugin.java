package pl.jms.auth.bungee;

import com.zaxxer.hikari.HikariDataSource;
import net.md_5.bungee.api.plugin.Plugin;
import pl.jms.auth.bungee.command.AdminCommand;
import pl.jms.auth.bungee.command.ChangePasswordCommand;
import pl.jms.auth.bungee.command.LoginCommand;
import pl.jms.auth.bungee.command.PremiumCommand;
import pl.jms.auth.bungee.command.RegisterCommand;
import pl.jms.auth.bungee.command.TitlesCommand;
import pl.jms.auth.bungee.command.UnregisterCommand;
import pl.jms.auth.bungee.listener.AuthListener;
import pl.jms.auth.bungee.listener.PluginChannelListener;
import pl.jms.auth.bungee.task.QueueTickRunnable;
import pl.jms.auth.bungee.util.BungeeConfigLoader;
import pl.jms.auth.core.auth.AuthService;
import pl.jms.auth.core.config.AuthConfig;
import pl.jms.auth.core.integrations.WebhookNotifier;
import pl.jms.auth.core.storage.SqlDataSourceFactory;
import pl.jms.auth.core.storage.SqlUserRepository;
import pl.jms.auth.core.storage.UserRepository;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public final class UltimateBungeeAuthPlugin extends Plugin {

    private AuthService authService;
    private WebhookNotifier webhooks;
    private DataSource dataSource;
    private AuthConfig authConfig;

    @Override
    public void onEnable() {
        try {
            authConfig = BungeeConfigLoader.load(getDataFolder(), this);
        } catch (Exception e) {
            getLogger().severe("Failed to load config: " + e.getMessage());
            return;
        }
        dataSource = SqlDataSourceFactory.create(authConfig.database());
        webhooks = new WebhookNotifier(authConfig.integrations());
        UserRepository repository = new SqlUserRepository(dataSource, authConfig.database().table());
        authService = new AuthService(authConfig, repository, webhooks);
        try {
            authService.migrateAndLoad(dataSource);
        } catch (SQLException e) {
            getLogger().severe("Database error: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        getProxy().getPluginManager().registerListener(this, new AuthListener(this, authService));
        getProxy().getPluginManager().registerListener(this, new PluginChannelListener(authService));
        getProxy().registerChannel(authConfig.security().pluginResponseChannel());

        getProxy().getPluginManager().registerCommand(this, new LoginCommand(authService));
        getProxy().getPluginManager().registerCommand(this, new RegisterCommand(authService));
        getProxy().getPluginManager().registerCommand(this, new UnregisterCommand(authService));
        getProxy().getPluginManager().registerCommand(this, new ChangePasswordCommand(authService));
        getProxy().getPluginManager().registerCommand(this, new PremiumCommand(authService));
        getProxy().getPluginManager().registerCommand(this, new TitlesCommand(authService));
        getProxy().getPluginManager().registerCommand(this, new AdminCommand(authService));

        long tick = Math.max(1, loadedConfigTickSeconds());
        getProxy().getScheduler().schedule(this, new QueueTickRunnable(authService), tick, tick, TimeUnit.SECONDS);

        getLogger().info("UltimateBungeeAuth enabled");
    }

    private int loadedConfigTickSeconds() {
        return authConfig.queueTickSeconds();
    }

    @Override
    public void onDisable() {
        if (webhooks != null) {
            webhooks.close();
        }
        if (dataSource instanceof HikariDataSource hikari) {
            hikari.close();
        }
    }

    public AuthService auth() {
        return authService;
    }
}
