package pl.jms.auth.velocity;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import com.google.inject.Inject;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import pl.jms.auth.core.auth.AuthService;
import pl.jms.auth.core.config.AuthConfig;
import pl.jms.auth.core.integrations.WebhookNotifier;
import pl.jms.auth.core.storage.SqlDataSourceFactory;
import pl.jms.auth.core.storage.SqlUserRepository;
import pl.jms.auth.core.storage.UserRepository;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.Arrays;
import java.sql.SQLException;
import java.time.Duration;

@Plugin(id = "ultimatebungeeauth", name = "UltimateBungeeAuth", version = "2.0.0", authors = {"UltimateBungeeAuth"})
public final class VelocityAuthPlugin {

    private final ProxyServer server;
    private final Path dataDirectory;
    private final Logger logger;
    private final PluginContainer container;

    private AuthService authService;
    private WebhookNotifier webhooks;
    private DataSource dataSource;
    private AuthConfig authConfig;

    @Inject
    public VelocityAuthPlugin(ProxyServer server, @com.velocitypowered.api.plugin.annotation.DataDirectory Path dataDirectory,
                              Logger logger, PluginContainer container) {
        this.server = server;
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.container = container;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        try {
            authConfig = VelocityConfigLoader.load(dataDirectory, VelocityAuthPlugin.class);
        } catch (Exception e) {
            logger.error("Config load failed", e);
            return;
        }
        dataSource = SqlDataSourceFactory.create(authConfig.database());
        webhooks = new WebhookNotifier(authConfig.integrations());
        UserRepository repository = new SqlUserRepository(dataSource, authConfig.database().table());
        authService = new AuthService(authConfig, repository, webhooks);
        try {
            authService.migrateAndLoad(dataSource);
        } catch (SQLException e) {
            logger.error("Database failed", e);
            return;
        }
        server.getEventManager().register(container, new VelocityPlatformListener(this, authService, server));

        CommandManager cmd = server.getCommandManager();
        register(cmd, cmd.metaBuilder("login").aliases("l").plugin(this.container).build(), new VelocityCommands.VLoginCommand(authService));
        register(cmd, cmd.metaBuilder("register").aliases("reg").plugin(this.container).build(), new VelocityCommands.VRegisterCommand(authService, server));
        register(cmd, cmd.metaBuilder("unregister").plugin(this.container).build(), new VelocityCommands.VUnregisterCommand(authService));
        register(cmd, cmd.metaBuilder("changepassword").plugin(this.container).build(), new VelocityCommands.VChangePasswordCommand(authService));
        register(cmd, cmd.metaBuilder("premium").plugin(this.container).build(), new VelocityCommands.VPremiumCommand(authService));
        String[] titleAliases = Arrays.stream(authConfig.titlesCommandAliases().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        CommandMeta.Builder titleMeta = cmd.metaBuilder("titles").plugin(this.container);
        if (titleAliases.length > 0) {
            titleMeta = titleMeta.aliases(titleAliases);
        }
        register(cmd, titleMeta.build(), new VelocityCommands.VTitlesCommand(authService));
        register(cmd, cmd.metaBuilder("ultimatebungeeauth").aliases("uba", "ultimateba", "bungeeauth").plugin(this.container).build(), new VelocityCommands.VAdminCommand(authService, server));

        long tick = Math.max(1, authConfig.queueTickSeconds());
        server.getScheduler()
                .buildTask(this.container, new VelocityQueueRunnable(server, authService))
                .repeat(Duration.ofSeconds(tick))
                .schedule();

        logger.info("UltimateBungeeAuth enabled on Velocity.");
    }

    private void register(CommandManager cmd, CommandMeta meta, com.velocitypowered.api.command.SimpleCommand command) {
        cmd.register(meta, command);
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (webhooks != null) {
            webhooks.close();
        }
        if (dataSource instanceof HikariDataSource h) {
            h.close();
        }
    }

    public AuthService auth() {
        return authService;
    }
}
