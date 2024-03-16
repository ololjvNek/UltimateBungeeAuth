package pl.jms.auth.tasks;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.scheduler.BungeeTask;
import pl.jms.auth.Main;

public class AsyncPremiumTask implements Runnable {

    private PreLoginEvent preLoginEvent;
    private PendingConnection pendingConnection;
    private ScheduledTask scheduledTask;
    public AsyncPremiumTask(final PreLoginEvent preLoginEvent, final PendingConnection pendingConnection){
        this.preLoginEvent = preLoginEvent;
        this.pendingConnection = pendingConnection;
    }

    public void setScheduledTask(ScheduledTask scheduledTask){
        this.scheduledTask = scheduledTask;
    }
    @Override
    public void run() {

        BungeeCord.getInstance().getLogger().info("Premium account checking: " + pendingConnection.toString());

        pendingConnection.setOnlineMode(true);

        if(pendingConnection.isOnlineMode()){
            preLoginEvent.completeIntent(Main.getInstance());
            ProxyServer.getInstance().getScheduler().cancel(scheduledTask);
            BungeeCord.getInstance().getLogger().info("Completed checking");
        }
    }
}
