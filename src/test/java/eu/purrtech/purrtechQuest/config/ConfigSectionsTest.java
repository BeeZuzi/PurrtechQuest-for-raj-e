package eu.purrtech.purrtechQuest.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSectionsTest {

    private static final String BUNDLED = """
            # first
            first: 1

            # How many quests.
            # Second comment line.
            limit:
              default: 3
              ranks:
                hrac: 3

            # The language.
            default-locale: cs
            """;

    @Test
    void appendsTheMissingSectionWithItsCommentsAndLeavesTheRestUntouched(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("config.yml");
        String original = "# my own comment\nfirst: 5\ndefault-locale: en\n";
        Files.writeString(file, original, StandardCharsets.UTF_8);

        assertTrue(ConfigSections.appendIfMissing(file, BUNDLED, "limit"));

        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(after.startsWith(original));
        assertEquals(original + "\n# How many quests.\n# Second comment line.\nlimit:\n  default: 3\n  ranks:\n    hrac: 3\n", after);
    }

    @Test
    void doesNothingWhenTheSectionIsAlreadyThere(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("config.yml");
        String original = "limit:\n  default: 9\n";
        Files.writeString(file, original, StandardCharsets.UTF_8);

        assertFalse(ConfigSections.appendIfMissing(file, BUNDLED, "limit"));
        assertEquals(original, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void appendingTwiceAddsItOnce(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("config.yml");
        Files.writeString(file, "first: 1", StandardCharsets.UTF_8);

        assertTrue(ConfigSections.appendIfMissing(file, BUNDLED, "limit"));
        assertFalse(ConfigSections.appendIfMissing(file, BUNDLED, "limit"));
    }

    @Test
    void unknownSectionInTheBundledTextIsIgnored(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("config.yml");
        Files.writeString(file, "first: 1\n", StandardCharsets.UTF_8);

        assertFalse(ConfigSections.appendIfMissing(file, BUNDLED, "nope"));
    }

    @Test
    void extractsTheRealActiveQuestLimitSectionFromTheBundledConfig(@TempDir Path dir) throws IOException {
        String bundled;
        try (var in = ConfigSectionsTest.class.getClassLoader().getResourceAsStream("config.yml")) {
            bundled = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Path file = dir.resolve("config.yml");
        Files.writeString(file, "default-locale: cs\n", StandardCharsets.UTF_8);

        assertTrue(ConfigSections.appendIfMissing(file, bundled, "active-quest-limit"));

        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(after.contains("# How many quests a player can have in progress at once, by rank."));
        assertTrue(after.contains("active-quest-limit:\n  default: 3\n  permission: \"group.%rank%\"\n  ranks:\n"
                + "    hrac: 3\n    rajce: 4\n    ninja: 5\n    king: 6\n    sky: 6\n"));
        assertFalse(after.contains("# The language"), "must not drag in neighbouring sections");
        assertEquals(after.indexOf("sky: 6") + "sky: 6\n".length(), after.length());
    }
}
