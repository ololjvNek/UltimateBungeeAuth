package pl.jms.auth.utils;

import net.md_5.bungee.api.connection.ProxiedPlayer;


public class Queue {

    private ProxiedPlayer player;
    private int position;

    public Queue(ProxiedPlayer player, int i){
        this.player = player;
        this.position = i;
    }

    public ProxiedPlayer getPlayer(){
        return this.player;
    }

    public int getPosition(){
        return this.position;
    }

    public void removeOne(){
        this.position -= 1;
    }
}
