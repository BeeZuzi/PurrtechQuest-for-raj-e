package eu.purrtech.purrtechQuest.config;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the two self-healing paths a real admin's on-disk {@code lang/*.yml} can need after an update:
 * a brand new key ({@link eu.purrtech.purrtechQuest.config.MessagesConfig} calls this
 * {@code mergeMissingKeys}) and an existing key whose bundled default changed shape, not just wording
 * ({@code migrateStaleDefaults}) — the scenario that motivated this test, where objectives gained a
 * {@code %label%} placeholder and the old {@code %type% %target%} template on disk started rendering those
 * tokens literally instead of being replaced.
 */
class MessagesConfigTest {

    @Test
    void upgradesAStaleObjectiveTemplateToTheCurrentDefault(@TempDir Path tempDir) throws IOException {
        Path dataFolder = tempDir.resolve("plugin-data");
        writeLangFile(dataFolder, "cs", """
                quest:
                  info-objective: "<gray>- %type% %target%: %progress%/%amount%</gray>"
                """);

        MessagesConfig messages = MessagesConfig.load(mockPlugin(dataFolder), "cs");

        // Scoped to this one key rather than the whole file — editor-objective-line (the admin editor's
        // technical objective listing) legitimately still uses %type% %target% by design and would also
        // get filled in here by mergeMissingKeys, since this test's on-disk file starts with only one key.
        String upgradedValue = messages.get("quest.info-objective", "cs");
        assertTrue(upgradedValue.contains("%label%"), "stale %type%/%target% template should have been upgraded to %label%");
        assertTrue(!upgradedValue.contains("%type% %target%"), "old template should no longer be present");
    }

    @Test
    void upgradesTheStaleEnglishGuideTextToTheIntendedCzechText(@TempDir Path tempDir) throws IOException {
        Path dataFolder = tempDir.resolve("plugin-data");
        writeLangFile(dataFolder, "en", """
                quest:
                  guide-completed: "<#F69B45><b>●</b></#F69B45> <white>Quests completed: <#F8AF69>%done%</#F8AF69><dark_gray>/</dark_gray><#F69B45>%total%</#F69B45></white>"
                """);

        MessagesConfig messages = MessagesConfig.load(mockPlugin(dataFolder), "cs");

        // The guide is meant to always read in Czech, even for a player whose client locale is English —
        // an on-disk en.yml from before that decision keeps the old English wording forever without the
        // stale-defaults migration, since "guide-completed" isn't a missing key, just an outdated value.
        String upgradedValue = messages.get("quest.guide-completed", "en");
        assertTrue(upgradedValue.contains("ꜱᴘʟɴěɴýᴄʜ"), "stale English guide text should have been upgraded to the Czech default");
        assertTrue(!upgradedValue.contains("Quests completed"), "old English wording should no longer be present");
    }

    @Test
    void leavesAnAdminCustomizedValueUntouched(@TempDir Path tempDir) throws IOException {
        Path dataFolder = tempDir.resolve("plugin-data");
        String customLine = "<gray>>> %type% %target% custom wording <<</gray>";
        writeLangFile(dataFolder, "cs", """
                quest:
                  info-objective: "%s"
                """.formatted(customLine));

        MessagesConfig.load(mockPlugin(dataFolder), "cs");

        String stillOnDisk = Files.readString(dataFolder.resolve("lang/cs.yml"), StandardCharsets.UTF_8);
        assertTrue(stillOnDisk.contains(customLine), "an admin's custom value must not be overwritten");
    }

    @Test
    void newlyExtractedFileMatchesBundledDefaultVerbatim(@TempDir Path tempDir) {
        Path dataFolder = tempDir.resolve("plugin-data");
        MessagesConfig messages = MessagesConfig.load(mockPlugin(dataFolder), "cs");
        assertEquals("<gray>ᴄíʟᴇ:</gray>", messages.get("quest.gui-lore-objectives-header", "cs"));
    }

    private static void writeLangFile(Path dataFolder, String locale, String content) throws IOException {
        Path file = dataFolder.resolve("lang/" + locale + ".yml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static JavaPlugin mockPlugin(Path dataFolder) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getResource(anyString()))
                .thenAnswer(invocation -> MessagesConfigTest.class.getClassLoader()
                        .getResourceAsStream(invocation.getArgument(0)));
        return plugin;
    }
}
