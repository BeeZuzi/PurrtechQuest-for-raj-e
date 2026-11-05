package eu.purrtech.purrtechQuest.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PermissionMessagesTest {

    private static YamlConfiguration config(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return config;
    }

    @Test
    void findsTheMessageForAPermissionWithDotsInItsName() throws Exception {
        YamlConfiguration config = config("""
                permission-messages:
                  - permission: "skyblock.premium"
                    message: "<red>Uzavřeno."
                  - permission: "other.node"
                    message: "x"
                """);

        assertEquals("<red>Uzavřeno.", PermissionMessages.forPermission(config, "skyblock.premium"));
        assertEquals("x", PermissionMessages.forPermission(config, "other.node"));
    }

    @Test
    void unknownBlankOrMissingPermissionHasNoMessage() throws Exception {
        YamlConfiguration config = config("""
                permission-messages:
                  - permission: "skyblock.premium"
                    message: ""
                """);

        assertNull(PermissionMessages.forPermission(config, "skyblock.premium"));
        assertNull(PermissionMessages.forPermission(config, "nope"));
        assertNull(PermissionMessages.forPermission(config, null));
        assertNull(PermissionMessages.forPermission(config("a: 1\n"), "skyblock.premium"));
    }
}
