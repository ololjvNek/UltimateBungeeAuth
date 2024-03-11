package pl.jms.auth.managers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import pl.jms.auth.Main;
import pl.jms.auth.utils.User;

public class UserManager {
	
	public static ConcurrentHashMap<UUID, User> users = new ConcurrentHashMap<>();
	
	public static void createUser(ProxiedPlayer p){
		if(getUser(p) == null){
			User mu = new User(p);
			users.put(p.getUniqueId(), mu);
		}
	}
	
	public static void createUser(String p){
		if(getUser(p) == null){
			User mu = new User(p);
			users.put(ProxyServer.getInstance().getPlayer(p).getUniqueId(), mu);
		}
	}

	public static User getUser(final UUID uuid){
		return users.get(uuid);
	}
	public static User getUser(String p){
		for(User u : users.values()){
			if(p.equalsIgnoreCase(u.getName())){
				return u;
			}
		}
		return null;
	}
	
	public static User getUser(ProxiedPlayer p){
		for(User u : users.values()){
			if(p.getName().equalsIgnoreCase(u.getName())){
				return u;
			}
		}
		return null;
	}
	
	public static void loadUsers(){
        try {
            final ResultSet rs = Main.store.query("SELECT * FROM `authusers`");
            while (rs.next()) {
            	User u = new User(rs);
                users.put(u.getUUID(), u);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
	}
	
    public static ConcurrentHashMap<UUID, User> getUsers() {
    	return UserManager.users;
    }

}
