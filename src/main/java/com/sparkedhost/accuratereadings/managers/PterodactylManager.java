package com.sparkedhost.accuratereadings.managers;

import com.sparkedhost.pterodactyl4j.PteroBuilder;
import com.sparkedhost.pterodactyl4j.client.entities.Account;
import com.sparkedhost.pterodactyl4j.client.entities.ClientServer;
import com.sparkedhost.pterodactyl4j.client.entities.PteroClient;
import com.sparkedhost.pterodactyl4j.entities.Limit;
import com.sparkedhost.pterodactyl4j.exceptions.LoginException;
import com.sparkedhost.pterodactyl4j.exceptions.NotFoundException;
import com.sparkedhost.accuratereadings.Main;
import lombok.Getter;
import lombok.Setter;

import java.util.logging.Level;

public class PterodactylManager {
    String panelURL;
    String apiKey;

    @Getter
    @Setter
    Account account;

    @Getter
    @Setter
    ClientServer server;

    @Getter
    String serverId;

    @Getter
    private PteroClient api;

    @Getter
    private ResourceUsageManager resourceUsageManager;

    private final Main plugin = Main.getInstance();

    private final String userAgent = String.format("%s v%s; +https://sparked.host/arua", plugin.getName(), plugin.getDescription().getVersion());

    /**
     * Gets everything ready: initializes PteroClient object, validates credentials and server access, and starts the
     * resource usage monitor.
     */

    public void initializeClient() {
        // Initialize values and PteroClient API object
        initializeAPI();
        try {
            account = login();
            server = retrieveServer();

            plugin.log(Level.INFO, "Connection established successfully! The API key specified belongs to " + getAccount().getFirstName() + ", and is able to access the server '" + server.getName() + "'. You're good to go!");

            setLimits();

            resourceUsageManager = new ResourceUsageManager();
            getResourceUsageManager().startListener();

            // Stores whether the account used to access this server owns it or not
            setServerOwner(server.isServerOwner());
        } catch (LoginException | NotFoundException e) {
            e.printStackTrace();
            plugin.disableItself();
        }
    }

    /**
     * Initializes values from config file, and PteroClient object.
     */

    private void initializeAPI() {
        panelURL = plugin.getSettings().pterodactyl_panelUrl;
        apiKey = plugin.getSettings().pterodactyl_apiKey;
        serverId = plugin.getSettings().pterodactyl_serverId;
        api = PteroBuilder.createClient(panelURL, apiKey, userAgent);
    }

    /**
     * Get account from PteroClient
     * @return Account object
     */

    private Account login() {
        try {
            return api.retrieveAccount().execute();
        } catch (LoginException e) {
            throw new LoginException("The API key provided is invalid.");
        }
    }

    /**
     * Retrieve server from PteroClient, by server ID
     * @return Server object
     */

    private ClientServer retrieveServer() {
        try {
            return api.retrieveServerByIdentifier(serverId).execute();
        } catch (NotFoundException e) {
            throw new NotFoundException("This server doesn't exist, or the account '" + getAccount().getEmail() + "' is unable to access it.");
        }
    }

    /**
     * Store resource limits.
     */

    private void setLimits() {
        Limit limits = getServer().getLimits();
        setCpuLimit(limits.getCPULong());
        setMemoryLimit(limits.getMemoryLong());
        setDiskLimit(limits.getDiskLong());
    }

    /**
     * Resets the resource utilization and limits variables.
     */

    protected void resetVariables() {
        setCpuUsage(0);
        setMemoryUsage(0);
        setDiskUsage(0);
        setUptime("(resource usage manager not running)");
    }

    /*
     * And here lies:
     * The almighty list of Getters and Setters
     */

    @Getter
    @Setter
    boolean isServerOwner;

    @Getter
    @Setter
    long memoryUsage = 0;

    @Getter
    @Setter
    long memoryLimit = 0;

    @Getter
    @Setter
    long diskUsage = 0;

    @Getter
    @Setter
    long diskLimit = 0;

    @Getter
    @Setter
    long cpuUsage = 0;

    @Getter
    @Setter
    long cpuLimit = 0;

    @Getter
    @Setter
    String uptime;
}
