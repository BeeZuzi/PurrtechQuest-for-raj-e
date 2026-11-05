package eu.purrtech.purrtechQuest.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public final class PluginConfig {

    private final StorageType storageType;
    private final String sqliteFileName;
    private final MySqlSettings mysql;
    private final String defaultLocale;
    private final String questsDirectoryName;
    private final TrackingDisplay trackingDisplay;
    private final boolean debug;

    private PluginConfig(StorageType storageType, String sqliteFileName, MySqlSettings mysql, String defaultLocale,
                          String questsDirectoryName, TrackingDisplay trackingDisplay, boolean debug) {
        this.storageType = storageType;
        this.sqliteFileName = sqliteFileName;
        this.mysql = mysql;
        this.defaultLocale = defaultLocale;
        this.questsDirectoryName = questsDirectoryName;
        this.trackingDisplay = trackingDisplay;
        this.debug = debug;
    }

    /** Sections added to config.yml after its first release; written into an older file that lacks them. */
    private static final List<String> LATER_SECTIONS = List.of("active-quest-limit", "permission-messages");

    public static PluginConfig load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        addMissingSections(plugin);
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        StorageType storageType = StorageType.valueOf(
                config.getString("storage.type", "SQLITE").toUpperCase());
        String sqliteFileName = config.getString("storage.sqlite-file", "data.db");
        MySqlSettings mysql = new MySqlSettings(
                config.getString("storage.mysql.host", "localhost"),
                config.getInt("storage.mysql.port", 3306),
                config.getString("storage.mysql.database", "purrtechquest"),
                config.getString("storage.mysql.username", "purrtechquest"),
                config.getString("storage.mysql.password", ""),
                Math.max(1, config.getInt("storage.mysql.pool-size", 10)),
                config.getBoolean("storage.mysql.use-ssl", false));
        String defaultLocale = config.getString("default-locale", "cs").toLowerCase();
        String questsDirectoryName = config.getString("quests-directory", "quests");
        TrackingDisplay trackingDisplay = TrackingDisplay.valueOf(
                config.getString("tracking.display", "ACTION_BAR").toUpperCase());
        boolean debug = config.getBoolean("debug", false);

        return new PluginConfig(storageType, sqliteFileName, mysql, defaultLocale, questsDirectoryName,
                trackingDisplay, debug);
    }

    /**
     * An existing config.yml is never replaced by the bundled one, so an option added later wouldn't show up in
     * it. Writes the bundled text of each such section (with its comments) at the end of the server's file, so
     * the admin sees it there and can edit it; if that fails, the section's built-in defaults still apply.
     */
    private static void addMissingSections(JavaPlugin plugin) {
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in == null) {
                return;
            }
            String bundled = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Path file = plugin.getDataFolder().toPath().resolve("config.yml");
            for (String section : LATER_SECTIONS) {
                if (ConfigSections.appendIfMissing(file, bundled, section)) {
                    plugin.getLogger().info("Added the new \"" + section + "\" section to config.yml.");
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not add new sections to config.yml: " + e.getMessage());
        }
    }

    public StorageType storageType() {
        return storageType;
    }

    public String sqliteFileName() {
        return sqliteFileName;
    }

    public MySqlSettings mysql() {
        return mysql;
    }

    public String defaultLocale() {
        return defaultLocale;
    }

    public String questsDirectoryName() {
        return questsDirectoryName;
    }

    public TrackingDisplay trackingDisplay() {
        return trackingDisplay;
    }

    public boolean debug() {
        return debug;
    }
}
