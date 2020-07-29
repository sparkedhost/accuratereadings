package me.bettd.accuratereadings;

import com.stanjg.ptero4j.controllers.TestController;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.logging.Level;

public class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        String panelUrl = getConfig().getString("panelUrl");
        String apiKey = getConfig().getString("apiKey");
        String serverId = getConfig().getString("serverId");
        getLogger().log(Level.INFO, "AccurateReadings is loading...");
        getCommand("perf").setExecutor(new PerformanceCmd(this));
        if(getConfig().getBoolean("enableRestartCmd")) {
            getCommand("restart").setExecutor(new RestartCmd(this));
        }
        getLogger().log(Level.INFO, "Loaded all the commands. Connecting to the panel...");
        getLogger().log(Level.INFO, "Using panel link: " + panelUrl);
        if(apiKey.equalsIgnoreCase("CHANGETHIS")) {
            getLogger().log(Level.WARNING, "You need to change the API key in your config.yml before using this plugin. Read how to get the API key on the GitHub page. SERVER WILL SHUT DOWN!");
            Bukkit.getServer().shutdown();
        }
        if(serverId.isEmpty()) {
            getLogger().log(Level.INFO, "The plugin needs a server ID on its config.yml in order for the plugin to work. The plugin will now disable itself.");
            getServer().getPluginManager().disablePlugin(this);
        } else {
            getLogger().log(Level.INFO, "PLUGIN LOADED.");
        }
        getLogger().log(Level.INFO, "Connection established!");
        getLogger().log(Level.INFO, "We've tested the connection to the panel and it has succeeded! This does not mean that the API key has access to the server though, so if you encounter any issue, please make sure the server specified in the config is owned by the account used to create the API key, or has subuser access to this server.");

    }

    public void onDisable() {
        getLogger().log(Level.INFO, "Plugin is disabling.");
    }
}
