package com.sparkedhost.accuratereadings.events;

import com.mattmalec.pterodactyl4j.client.managers.WebSocketManager;
import com.mattmalec.pterodactyl4j.client.ws.events.AuthSuccessEvent;
import com.mattmalec.pterodactyl4j.client.ws.events.StatsUpdateEvent;
import com.mattmalec.pterodactyl4j.client.ws.events.connection.FailureEvent;
import com.mattmalec.pterodactyl4j.client.ws.hooks.ClientSocketListenerAdapter;
import com.sparkedhost.accuratereadings.Main;
import com.sparkedhost.accuratereadings.managers.PterodactylManager;
import com.sparkedhost.accuratereadings.managers.ResourceUsageManager;
import org.bukkit.Bukkit;

import java.util.logging.Level;

public class WebSocketEvents extends ClientSocketListenerAdapter {
    PterodactylManager pteroManager = Main.getInstance().pteroAPI;
    ResourceUsageManager resourceUsageManager = pteroManager.getResourceUsageManager();
    @Override
    public void onAuthSuccess(AuthSuccessEvent e) {
        Main.getInstance().log(Level.INFO, "Successfully established a websocket connection.");
        e.getWebSocketManager().request(WebSocketManager.RequestAction.STATS);
        resourceUsageManager.setWebSocketManager(e.getWebSocketManager());
    }

    @Override
    public void onStatsUpdate(StatsUpdateEvent e) {
        pteroManager.setCpuUsage((long) e.getCPU());
        pteroManager.setMemoryUsage(e.getMemory());
        pteroManager.setDiskUsage(e.getDisk());
        pteroManager.setUptime(e.getUptimeFormatted());
    }

    @Override
    public void onFailure(FailureEvent e) {
        Main.getInstance().log(Level.WARNING, "An error occurred with the websocket connection, reconnecting...");

        resourceUsageManager.stopListener();
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> resourceUsageManager.startListener(), 60L);
    }
}