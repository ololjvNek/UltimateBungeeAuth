package pl.jms.auth.core.queue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AuthQueueService {

    private final Deque<UUID> order = new ArrayDeque<>();
    private final ConcurrentMap<UUID, Boolean> enqueued = new ConcurrentHashMap<>();

    public void join(UUID playerId) {
        if (enqueued.putIfAbsent(playerId, true) == null) {
            synchronized (order) {
                order.addLast(playerId);
            }
        }
    }

    public void leave(UUID playerId) {
        if (enqueued.remove(playerId) != null) {
            synchronized (order) {
                order.remove(playerId);
            }
        }
    }

    public int position(UUID playerId) {
        synchronized (order) {
            int i = 1;
            for (UUID id : order) {
                if (id.equals(playerId)) {
                    return i;
                }
                i++;
            }
        }
        return 0;
    }

    public boolean isHead(UUID playerId) {
        synchronized (order) {
            return !order.isEmpty() && order.peekFirst().equals(playerId);
        }
    }

    public void clearStale(Iterable<UUID> stillPresent) {
        ConcurrentMap<UUID, Boolean> present = new ConcurrentHashMap<>();
        for (UUID u : stillPresent) {
            present.put(u, true);
        }
        synchronized (order) {
            Iterator<UUID> it = order.iterator();
            while (it.hasNext()) {
                UUID u = it.next();
                if (!present.containsKey(u)) {
                    it.remove();
                    enqueued.remove(u);
                }
            }
        }
    }
}
