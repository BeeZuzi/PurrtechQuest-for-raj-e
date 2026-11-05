package eu.purrtech.purrtechQuest.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;

/**
 * The {@code permission-messages:} list of {@code config.yml}: what a player is told when they try to accept a
 * quest that needs a {@code required-permission} they don't have, per permission node (a list of
 * {@code permission}/{@code message} pairs rather than a map, because a node like {@code skyblock.premium}
 * contains dots, which YAML paths read as nesting). A node with no entry gets the generic "no permission" text.
 */
public final class PermissionMessages {

    private PermissionMessages() {
    }

    /** The configured message (MiniMessage) for {@code permission}, or {@code null} when none is set. */
    public static String forPermission(FileConfiguration config, String permission) {
        return forPermission(config.getMapList("permission-messages"), permission);
    }

    static String forPermission(List<Map<?, ?>> entries, String permission) {
        if (permission == null) {
            return null;
        }
        for (Map<?, ?> entry : entries) {
            Object node = entry.get("permission");
            Object message = entry.get("message");
            if (node != null && message != null && permission.equalsIgnoreCase(String.valueOf(node).trim())
                    && !String.valueOf(message).isBlank()) {
                return String.valueOf(message);
            }
        }
        return null;
    }
}
