package pl.jms.auth.utils;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import pl.jms.auth.Main;

public class User {
	
	private String name;
	private String password;
	private boolean premium, registered, logged, titlesEnabled;
	private String lastip;
	private PendingConnection connection;
	
	public User(ProxiedPlayer p){
		this.name = p.getName();
		this.password = null;
		this.premium = false;
		this.registered = false;
		this.logged = false;
		this.connection = null;
		this.lastip = null;
		insert();
	}
	
	public User(String p){
		this.name = p;
		this.password = null;
		this.premium = false;
		this.registered = false;
		this.logged = false;
		this.connection = null;
		this.lastip = null;
		insert();
	}
	
	public User(ResultSet rs) throws SQLException{
		this.name = rs.getString("name");
		this.password = rs.getString("password");
		this.premium = rs.getBoolean("premium");
		this.registered = rs.getBoolean("registered");
		this.logged = false;
		this.lastip = null;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getPassword(){
		return this.password;
	}

	public void setName(String s){
		this.name = s;
	}
	
	public void setConnection(PendingConnection con){
		this.connection = con;
	}

	public String getLastIP() {return this.lastip;}

	public boolean isTitlesEnabled(){
		return this.titlesEnabled;
	}

	public void setTitlesEnabled(boolean b){
		this.titlesEnabled = b;
	}

	public void setLastIP(String ip){
		this.lastip = ip;
	}

	public PendingConnection getConnection(){
		return this.connection;
	}
	
	public void setPassword(String pass){
		this.password = pass;
		update();
	}
	
	public void setPremium(boolean status){
		this.premium = status;
		update();
	}
	
	public boolean isPremium(){
		return this.premium;
	}
	
	public void setRegistered(boolean register){
		this.registered = register;
		update();
	}
	
	public boolean isRegistered(){
		return this.registered;
	}
	
	public void setLogged(boolean logged){
		this.logged = logged;
		update();
	}
	
	public boolean isLogged(){
		return this.logged;
	}
	
	public void update(){
		int pre = this.premium ? 1 : 0;
		int reg = this.registered ? 1 : 0;
		int tE = this.titlesEnabled ? 1 : 0;
		Main.store.update(true, "UPDATE `authusers` SET `password` = '" + this.password + "', `registered` = '" + reg + "', `premium` = '" + pre + "', `titlesEnabled` = '" + tE + "'  WHERE `name` ='" + this.name + "';");
	}
	
	public void insert(){
		int pre = this.premium ? 1 : 0;
		int reg = this.registered ? 1 : 0;
		int tE = this.titlesEnabled ? 1 : 0;
		Main.store.update(true, "INSERT INTO `authusers`(`id`, `name`, `password`, `premium`, `registered`, `titlesEnabled`) VALUES (NULL, '" + this.name + "', '" + this.password + "', '" + pre + "', '" + reg + "', '" + tE + "');");
	}

}
