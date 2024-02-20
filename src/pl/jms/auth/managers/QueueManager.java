package pl.jms.auth.managers;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import pl.jms.auth.utils.Queue;

import java.util.concurrent.ConcurrentHashMap;

public class QueueManager {

    public static ConcurrentHashMap<ProxiedPlayer, Queue> kolejki = new ConcurrentHashMap<>();

    public static Queue createQueue(ProxiedPlayer p){
        Queue k = new Queue(p, kolejki.size()+1);
        kolejki.put(k.getPlayer(), k);
        return k;
    }

    public static Queue getQueue(ProxiedPlayer p){
        return kolejki.get(p);
    }
}
