package com.sparkedhost.accuratereadings.config;

import com.sparkedhost.accuratereadings.Main;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Settings {
    private final Main plugin = Main.getInstance();

    /*
     * Settings:
     * PTERODACTYL-SPECIFIC
     */

    public String pterodactyl_panelUrl;
    public String pterodactyl_apiKey;
    public String pterodactyl_serverId;
    public boolean pterodactyl_useWebsocket;
    public long pterodactyl_updateFrequency;
    public boolean pterodactyl_autoStopOnEmpty;

    /*
     * Settings:
     * RESTART COMMAND
     */

    public boolean restart_enabled;
    public boolean restart_announce;


    /*
     * Settings:
     * /PERF COMMAND CUSTOMIZATION
     */

    public String output_postCommand;
    public boolean output_normalizeCpu;


    /*
     * Settings:
     * COMMAND COOLDOWN
     */

    public boolean cooldown_enabled;
    public int cooldown_time;


    /*
     * Settings:
     * TASKS
     */

    @Getter
    TaskSettings taskSettings = new TaskSettings();


    /*
     * Settings:
     * MESSAGES
     */

    public String messages_statsMessage;
    public String messages_noPerms;
    public String messages_restartBroadcast;

    public void loadValues() {

        /*
         * Settings:
         * PTERODACTYL-SPECIFIC
         */

        pterodactyl_panelUrl = plugin.getConfig().getString("pterodactyl.panel-url");
        pterodactyl_apiKey = plugin.getConfig().getString("pterodactyl.api-key");

        pterodactyl_serverId = plugin.getConfig().getString("pterodactyl.server-id");

        if (pterodactyl_serverId == null || pterodactyl_serverId.isEmpty()) {
            plugin.log(Level.INFO, "No server ID specified, we're going to try to determine the ID " +
                    "from the machine's hostname.");

            try {
                String determinedID = determineID();
                if (determinedID != null) {
                    pterodactyl_serverId = determinedID;
                    plugin.log(Level.INFO, "Server ID found: " + determinedID);
                } else {
                    plugin.log(Level.SEVERE, "Unable to determine server ID, read above for errors.");
                }
            } catch (IOException exception) {
                plugin.log(Level.SEVERE, "An IOException occurred.");
                exception.printStackTrace();
            }
        }

        pterodactyl_useWebsocket = plugin.getConfig().getBoolean("pterodactyl.use-websocket");
        pterodactyl_updateFrequency = plugin.getConfig().getLong("pterodactyl.update-frequency");
        pterodactyl_autoStopOnEmpty = plugin.getConfig().getBoolean("pterodactyl.auto-stop-on-empty");

        /*
         * Settings:
         * RESTART COMMAND
         */

        restart_enabled = plugin.getConfig().getBoolean("restart.enabled");
        restart_announce = plugin.getConfig().getBoolean("restart.announce");


        /*
         * Settings:
         * /PERF COMMAND CUSTOMIZATION
         */

        output_postCommand = plugin.getConfig().getString("output-customization.post-command");
        output_normalizeCpu = plugin.getConfig().getBoolean("output-customization.normalize-cpu");


        /*
         * Settings:
         * COMMAND COOLDOWN
         */

        cooldown_enabled = plugin.getConfig().getBoolean("cooldown.enabled");
        cooldown_time = plugin.getConfig().getInt("cooldown.time");


        /*
         * Settings:
         * TASKS
         */

        getTaskSettings().load();

        /*
         * Settings:
         * MESSAGES
         */

        messages_statsMessage = plugin.getConfig().getString("messages.stats-message");

        // If 'stats-message' is a space (" ") or completely blank, replace with default value in memory and log warning
        if (messages_statsMessage.equals("") || messages_statsMessage.equals(" ")) {
            messages_statsMessage = String.join("\n",
                    "&8&m        &r &f&lSTATS&r &8&m        &r",
                    "&r &r",
                    "&6&l- CPU: &e{CURRENTCPU}% &7(limit: {MAXCPU}%)",
                    "&6&l- RAM: &e{CURRENTRAM} &7(limit: {MAXRAM})",
                    "&6&l- Disk: &e{CURRENTDISK} &7(limit: {MAXDISK})",
                    "&6&l- Players: &e{PLAYERCOUNT}&7/&f{PLAYERLIMIT}",
                    "&6&l- Uptime: &e{UPTIME}",
                    "&r &r");
            plugin.log(Level.WARNING, "'stats-message' is empty, replacing with default value (only on runtime). Please fix this in the config file!");
        }

        messages_noPerms = plugin.getConfig().getString("messages.no-permission");
        messages_restartBroadcast = plugin.getConfig().getString("messages.restart-broadcast");
    }

    private boolean isNotLinux() {
        return !System.getProperty("os.name").toLowerCase().contains("linux");
    }

    private String determineID() throws IOException {
        final Path path = Paths.get("/etc/hostname");

        if (isNotLinux() || Files.notExists(path)) {
            plugin.log(Level.SEVERE, "System is not running a Linux kernel or /etc/hostname does not exist.");
            return null;
        }

        try {
            final String hostname = Files.readAllLines(path).get(0);
            final String serverID = hostname.substring(0, 8);

            final Pattern pattern = Pattern.compile("([0-9a-f]{8})");
            final Matcher validator = pattern.matcher(serverID);

            if (!validator.matches()) {
                plugin.log(Level.SEVERE, "Hostname does not look like a valid server ID. Got '" + hostname + "'.");
                return null;
            }

            return serverID;
        } catch (Exception exception) {
            plugin.log(Level.SEVERE, "Unable to get a valid hostname from the /etc/hostname file.");
            exception.printStackTrace();
            return null;
        }
    }
}
