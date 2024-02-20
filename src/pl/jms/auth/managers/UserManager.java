package pl.jms.auth.managers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import pl.jms.auth.Main;
import pl.jms.auth.utils.User;

public class UserManager {
	
	public static ConcurrentHashMap<String, User> users;
	
	static{
		users = new ConcurrentHashMap<String, User>();
	}
	
	public static void createUser(ProxiedPlayer p){
		if(getUser(p) == null){
			User mu = new User(p);
			users.put(p.getName(), mu);
		}
	}
	
	public static void createUser(String p){
		if(getUser(p) == null){
			User mu = new User(p);
			users.put(p, mu);
		}
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
                UserManager.users.put(u.getName(), u);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
	}
	
    public static ConcurrentHashMap<String, User> getUsers() {
    	return UserManager.users;
    }

}
