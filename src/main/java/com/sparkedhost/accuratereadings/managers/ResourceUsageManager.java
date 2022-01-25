package com.sparkedhost.accuratereadings.managers;

import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import com.mattmalec.pterodactyl4j.client.entities.Utilization;
import com.mattmalec.pterodactyl4j.client.managers.WebSocketBuilder;
import com.mattmalec.pterodactyl4j.client.managers.WebSocketManager;
import com.mattmalec.pterodactyl4j.client.ws.events.AuthSuccessEvent;
import com.mattmalec.pterodactyl4j.client.ws.events.StatsUpdateEvent;
import com.mattmalec.pterodactyl4j.client.ws.hooks.ClientSocketListenerAdapter;
import com.sparkedhost.accuratereadings.Main;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

public class ResourceUsageManager extends ClientSocketListenerAdapter {
    PterodactylManager manager = Main.getInstance().pteroAPI;
    private final ResourceUsageManager resManagerThread = new ResourceUsageManager();

    @Getter
    private BukkitTask fallbackTimer;

    @Getter
    @Setter
    private boolean isRunning = false;

    /**
     * Starts resource usage listener.
     */
    public void initializeListener() {
        setRunning(true);

        // If use-websocket is set to true in the config, use that to gather resource usage stats.
        if (Main.getInstance().getSettings().pterodactyl_useWebsocket) {
            manager.getApi().retrieveServerByIdentifier(manager.getServerId()).map(ClientServer::getWebSocketBuilder)
                    .map(builder -> builder.addEventListeners(resManagerThread)).executeAsync(WebSocketBuilder::build);
            return;
        }

        // Standard API polling as fallback, every 200 server ticks (or 10 seconds on 20 TPS)
        fallbackTimer = Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
            Utilization usage = manager.getServer().retrieveUtilization().execute();

            manager.setCpuUsage((long) usage.getCPU());
            manager.setMemoryUsage(usage.getMemory());
            manager.setDiskUsage(usage.getDisk());
        }, 0L, 200L);
    }

    /**
     * Stops resource usage listener.
     */
    public void stopListener() {
        setRunning(false);

        if (getFallbackTimer() == null) {
            manager.getApi().retrieveServerByIdentifier(manager.getServerId()).map(ClientServer::getWebSocketBuilder)
                    .map(builder -> builder.removeEventListeners(resManagerThread)).executeAsync();
            return;
        }

        getFallbackTimer().cancel();
    }

    /*
     * The following event listeners are only registered when using a websocket connection to poll resource usage
     * statistics, and remain unused when using the standard API polling method.
     */

    @Override
    public void onAuthSuccess(AuthSuccessEvent e) {
        Main.getInstance().log(Level.INFO, "Successfully established a websocket connection.");
        e.getWebSocketManager().request(WebSocketManager.RequestAction.STATS);
    }

    @Override
    public void onStatsUpdate(StatsUpdateEvent e) {
        manager.setCpuUsage((long) e.getCPU());
        manager.setMemoryUsage(e.getMemory());
        manager.setDiskUsage(e.getDisk());
    }
}
