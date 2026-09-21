package eu.purrtech.purrtechQuest.config;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
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

    @Test
    void adminTypedPlaceholdersAreShownInTheTinyFontAndOthersAreLeftAlone(@TempDir Path tempDir) {
        Path dataFolder = tempDir.resolve("plugin-data");
        MessagesConfig messages = MessagesConfig.load(mockPlugin(dataFolder), "cs");

        var plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText();
        // quest.info-header is "<gold>=== %quest% ===</gold>"; %quest% is admin text, so it gets converted.
        assertEquals("=== sʙěʀ žᴇʟᴇᴢᴀ ===",
                plain.serialize(messages.render("quest.info-header", "cs", Map.of("%quest%", "Sběr železa"))));
        // quest.gui-lore-cooldown's %time% is not admin text and must come through exactly as given.
        assertTrue(plain.serialize(messages.render("quest.gui-lore-cooldown", "cs", Map.of("%time%", "2h 5m")))
                .contains("2h 5m"));
    }

    @Test
    void statusesGetTheirColorEvenWhenTheOnDiskFileHasAnEarlierUncoloredWording(@TempDir Path tempDir) throws IOException {
        Path dataFolder = tempDir.resolve("plugin-data");
        writeLangFile(dataFolder, "cs", """
                quest:
                  status-turned-in: "ᴏᴅᴇᴠᴢᴅᴀɴý"
                  status-in-progress: "ʀᴏᴢᴘʀᴀᴄᴏᴠᴀɴý"
                """);

        MessagesConfig messages = MessagesConfig.load(mockPlugin(dataFolder), "cs");

        assertEquals("<#00D420>ᴏᴅᴇᴠᴢᴅáɴᴏ</#00D420>", messages.get("quest.status-turned-in", "cs"));
        assertEquals("<#FF7200>ʀᴏᴢᴘʀᴀᴄᴏᴠáɴᴏ</#FF7200>", messages.get("quest.status-in-progress", "cs"));
    }

    @Test
    void theStatusWordRendersInItsOwnColor(@TempDir Path tempDir) {
        MessagesConfig messages = MessagesConfig.load(mockPlugin(tempDir.resolve("plugin-data")), "cs");
        assertEquals(0x00D420, colorOfWord(messages, "quest.status-turned-in", "ᴏᴅᴇᴠᴢᴅáɴᴏ"));
        assertEquals(0xFF7200, colorOfWord(messages, "quest.status-in-progress", "ʀᴏᴢᴘʀᴀᴄᴏᴠáɴᴏ"));
        assertEquals(0xDD1717, colorOfWord(messages, "quest.status-locked", "ᴜᴢᴀᴍčᴇɴᴏ"));
    }

    /** Renders the status inside the quest book's status line and returns the RGB the status word ends up with. */
    private static int colorOfWord(MessagesConfig messages, String statusKey, String word) {
        var line = messages.render("quest.gui-lore-quest-status", "cs", Map.of("%status%", messages.get(statusKey, "cs")));
        var color = findColor(line, word, null);
        return color == null ? -1 : color.value();
    }

    private static net.kyori.adventure.text.format.TextColor findColor(
            net.kyori.adventure.text.Component node, String word, net.kyori.adventure.text.format.TextColor inherited) {
        var effective = node.color() != null ? node.color() : inherited;
        if (node instanceof net.kyori.adventure.text.TextComponent text && text.content().equals(word)) {
            return effective;
        }
        for (var child : node.children()) {
            var found = findColor(child, word, effective);
            if (found != null) {
                return found;
            }
        }
        return null;
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
