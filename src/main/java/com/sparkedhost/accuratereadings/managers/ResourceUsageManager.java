package com.sparkedhost.accuratereadings.managers;

import com.sparkedhost.pterodactyl4j.client.entities.ClientServer;
import com.sparkedhost.pterodactyl4j.client.entities.Utilization;
import com.sparkedhost.pterodactyl4j.client.managers.WebSocketBuilder;
import com.sparkedhost.pterodactyl4j.client.managers.WebSocketManager;
import com.sparkedhost.pterodactyl4j.client.ws.events.AuthSuccessEvent;
import com.sparkedhost.pterodactyl4j.client.ws.events.StatsUpdateEvent;
import com.sparkedhost.pterodactyl4j.client.ws.events.connection.DisconnectedEvent;
import com.sparkedhost.pterodactyl4j.client.ws.events.token.TokenEvent;
import com.sparkedhost.pterodactyl4j.client.ws.hooks.ClientSocketListenerAdapter;
import com.sparkedhost.accuratereadings.Main;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

public class ResourceUsageManager extends ClientSocketListenerAdapter {
    PterodactylManager pteroManager = Main.getInstance().pteroAPI;
    private ResourceUsageManager resManagerThread;

    @Getter
    private BukkitTask fallbackTimer;

    @Getter
    @Setter
    private boolean isRunning = false;

    /**
     * Starts resource usage listener.
     */

    public void startListener() {
        setRunning(true);
        Main.getInstance().log(Level.INFO, "Resource usage monitor has been started.");

        // If use-websocket is set to true in the config, use that to gather resource usage stats.
        if (Main.getInstance().getSettings().pterodactyl_useWebsocket) {
            resManagerThread = new ResourceUsageManager();
            pteroManager.getApi().retrieveServerByIdentifier(pteroManager.getServerId()).map(ClientServer::getWebSocketBuilder)
                    .map(builder -> builder.addEventListeners(resManagerThread)).executeAsync(WebSocketBuilder::build);
            return;
        }

        // Standard API polling as fallback, every X seconds (specified in the config)
        fallbackTimer = Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
            Utilization usage = pteroManager.getServer().retrieveUtilization().execute();

            pteroManager.setCpuUsage((long) usage.getCPU());
            pteroManager.setMemoryUsage(usage.getMemory());
            pteroManager.setDiskUsage(usage.getDisk());
            pteroManager.setUptime(usage.getUptimeFormatted());
        }, 0L, (Main.getInstance().getSettings().pterodactyl_updateFrequency) * 20L);
    }

    /**
     * Stops resource usage listener.
     */
    public void stopListener() {
        setRunning(false);
        Main.getInstance().log(Level.INFO, "Resource usage monitor has been stopped.");

        if (getFallbackTimer() == null) {
            pteroManager.getApi().retrieveServerByIdentifier(pteroManager.getServerId()).map(ClientServer::getWebSocketBuilder)
                    .map(builder -> builder.removeEventListeners(resManagerThread)).executeAsync();
            return;
        }

        getFallbackTimer().cancel();
        pteroManager.resetVariables();
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
        pteroManager.setCpuUsage((long) e.getCPU());
        pteroManager.setMemoryUsage(e.getMemory());
        pteroManager.setDiskUsage(e.getDisk());
        pteroManager.setUptime(e.getUptimeFormatted());
    }

    @Override
    public void onTokenUpdate(TokenEvent e) {
        Main.getInstance().log(Level.INFO, "The authentication token has been updated.");
    }

    @Override
    public void onDisconnected(DisconnectedEvent e) {
        Main.getInstance().log(Level.WARNING, "Websocket connection lost, restarting resource usage manager...");
        stopListener();
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), this::startListener, 60L);
    }
}
