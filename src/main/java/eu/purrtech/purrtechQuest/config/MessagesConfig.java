package eu.purrtech.purrtechQuest.config;

import eu.purrtech.purrtechQuest.util.LegacyColors;
import eu.purrtech.purrtechQuest.util.TinyFont;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Loads {@code lang/<locale>.yml} files (extracted from the jar on first run, then editable by admins)
 * and resolves message keys per player locale, falling back to the configured default locale.
 */
public final class MessagesConfig {

    private static final String[] BUNDLED_LOCALES = {"cs", "en"};

    /**
     * Keys whose bundled default changed *shape* (not just wording) after they'd already shipped — the
     * objective label feature swapped {@code %type%}/{@code %target%} for {@code %label%} in these, but the
     * keys themselves already existed, so {@link #mergeMissingKeys} never touches them (it only fills in
     * keys missing entirely). Without this, an on-disk file from before that change keeps the old template
     * forever, and since the code no longer supplies {@code %type%}/{@code %target%}, those tokens render
     * as literal unmatched text instead of being substituted. An on-disk value only gets upgraded if it
     * still matches the exact old default below — any admin customization, even a small tweak, is left
     * alone, same as everywhere else in this file.
     * <p>
     * The {@code guide-*} entries under {@code "en"} are the same situation from a different cause: the
     * guide item's English text was later swapped for the same small-caps Czech text {@code cs.yml} uses
     * (the guide is meant to always read in Czech regardless of a player's client locale), but any server
     * that had already loaded the original English keys keeps them on disk forever without this — a
     * player whose client locale is English would otherwise keep seeing "Quests completed" instead of the
     * intended "ꜱᴘʟɴěɴýᴄʜ Qᴜᴇꜱᴛů".
     */
    private static final Map<String, Map<String, String>> STALE_DEFAULTS = Map.of(
            "en", Map.ofEntries(
                    Map.entry("quest.info-objective", "<gray>- %type% %target%: %progress%/%amount%</gray>"),
                    Map.entry("quest.info-objective-choice", "<yellow>- (choice) %type% %target%: %progress%/%amount%</yellow>"),
                    Map.entry("quest.info-objective-locked", "<dark_gray>- %type% %target% (unavailable, you completed a different choice)</dark_gray>"),
                    Map.entry("quest.tracking-line", "<yellow>» %quest%: %target% %progress%/%amount%</yellow>"),
                    Map.entry("quest.gui-lore-objective-line", "<gray> - %type% %target%: %progress%/%amount%</gray>"),
                    Map.entry("quest.gui-lore-objective-choice-line", "<yellow> - (choice) %label%: %progress%/%amount%</yellow>"),
                    Map.entry("quest.gui-lore-objective-locked", "<dark_gray> - %label% (unavailable, a different choice was completed)</dark_gray>"),
                    Map.entry("quest.guide-name", "<#F69B45><b>Guide</b></#F69B45>"),
                    Map.entry("quest.guide-intro", "<gray><i>Guides you through the server step by step</i></gray>"),
                    Map.entry("quest.guide-completed", "<#F69B45><b>●</b></#F69B45> <white>Quests completed: <#F8AF69>%done%</#F8AF69><dark_gray>/</dark_gray><#F69B45>%total%</#F69B45></white>"),
                    Map.entry("quest.guide-current-quest-header", "<#F69B45><b>Current quest:</b></#F69B45>"),
                    Map.entry("quest.guide-no-active-quest", "<#F69B45><b>●</b></#F69B45>  <gray>Žádný aktivní quest</gray>"),
                    Map.entry("quest.guide-progress-header", "<#F69B45><b>●</b></#F69B45>  <white>Quest progress:</white>"),
                    Map.entry("quest.gui-lore-guide-hint", "<dark_gray>Klikni pro zobrazení všech questů.</dark_gray>"),
                    Map.entry("quest.gui-category-title", "Quest categories"),
                    Map.entry("quest.status-available", "available"),
                    Map.entry("quest.status-in-progress", "in progress"),
                    Map.entry("quest.status-completed", "completed, go turn it in"),
                    Map.entry("quest.status-turned-in", "turned in"),
                    Map.entry("quest.gui-log-title", "Quests"),
                    Map.entry("quest.gui-button-accept", "<green>Accept quest</green>"),
                    Map.entry("quest.gui-button-abandon", "<red>Abandon quest</red>"),
                    Map.entry("quest.gui-button-turnin", "<gold>Turn in quest</gold>"),
                    Map.entry("quest.gui-button-track", "<yellow>Track quest</yellow>"),
                    Map.entry("quest.gui-button-untrack", "<yellow>Stop tracking</yellow>"),
                    Map.entry("quest.gui-button-back", "<gray>« Back</gray>"),
                    Map.entry("quest.gui-button-close", "<gray>Close</gray>"),
                    Map.entry("quest.gui-button-next-page", "<gray>Next page »</gray>"),
                    Map.entry("quest.gui-button-prev-page", "<gray>« Previous page</gray>"),
                    Map.entry("quest.gui-lore-objectives-header", "<gray>Objectives:</gray>"),
                    Map.entry("quest.gui-lore-rewards-header", "<gray>Rewards:</gray>"),
                    Map.entry("quest.gui-lore-reward-money", "<gray> - %amount% coins</gray>"),
                    Map.entry("quest.gui-lore-reward-experience", "<gray> - %amount% experience</gray>"),
                    Map.entry("quest.gui-lore-reward-bonus", "<gray> - bonus</gray>"),
                    Map.entry("quest.gui-lore-reward-section-base", "<gold>Base rewards:</gold>"),
                    Map.entry("quest.gui-lore-reward-section-tier", "<light_purple>Bonus (%tier%):</light_purple>"),
                    Map.entry("quest.gui-lore-cooldown", "<gray>Available again in %time%</gray>"),
                    Map.entry("quest.gui-lore-status", "<gray>Status: %status%</gray>"),
                    Map.entry("quest.gui-button-back-hint", "<gray>Returns you to the previous screen.</gray>"),
                    Map.entry("quest.gui-button-prev-page-hint", "<gray>Shows the previous page.</gray>"),
                    Map.entry("quest.gui-button-next-page-hint", "<gray>Shows the next page.</gray>"),
                    Map.entry("quest.gui-lore-quest-hint", "<gray>Click for the quest's details.</gray>"),
                    Map.entry("quest.gui-button-close-hint", "<gray>Closes this menu.</gray>"),
                    Map.entry("quest.gui-button-accept-hint", "<gray>Accepts this quest and starts tracking its progress.</gray>"),
                    Map.entry("quest.gui-button-abandon-hint", "<gray>Abandons the quest in progress and wipes its progress.</gray>"),
                    Map.entry("quest.gui-button-track-hint", "<gray>Shows this quest's progress on the action bar.</gray>"),
                    Map.entry("quest.gui-button-untrack-hint", "<gray>Stops showing this quest's progress on the action bar.</gray>"),
                    Map.entry("quest.gui-button-turnin-hint", "<gray>Turns in the completed quest and collects the rewards.</gray>")),
            "cs", Map.ofEntries(
                    Map.entry("quest.info-objective", "<gray>- %type% %target%: %progress%/%amount%</gray>"),
                    Map.entry("quest.info-objective-choice", "<yellow>- (volba) %type% %target%: %progress%/%amount%</yellow>"),
                    Map.entry("quest.info-objective-locked", "<dark_gray>- %type% %target% (nedostupné, splnil jsi jinou volbu)</dark_gray>"),
                    Map.entry("quest.gui-category-title", "Kategorie questů"),
                    Map.entry("quest.tracking-line", "<yellow>» %quest%: %target% %progress%/%amount%</yellow>"),
                    Map.entry("quest.gui-lore-objective-line", "<gray> - %type% %target%: %progress%/%amount%</gray>"),
                    Map.entry("quest.gui-lore-objective-choice-line", "<yellow> - (volba) %label%: %progress%/%amount%</yellow>"),
                    Map.entry("quest.gui-lore-objective-locked", "<dark_gray> - %label% (nedostupné, splněna jiná volba)</dark_gray>"),
                    Map.entry("quest.status-available", "dostupný"),
                    Map.entry("quest.status-in-progress", "rozpracovaný"),
                    Map.entry("quest.status-completed", "splněný, jdi odevzdat"),
                    Map.entry("quest.status-turned-in", "odevzdaný"),
                    Map.entry("quest.gui-log-title", "Questy"),
                    Map.entry("quest.gui-button-accept", "<green>Přijmout quest</green>"),
                    Map.entry("quest.gui-button-abandon", "<red>Zrušit quest</red>"),
                    Map.entry("quest.gui-button-turnin", "<gold>Odevzdat quest</gold>"),
                    Map.entry("quest.gui-button-track", "<yellow>Sledovat quest</yellow>"),
                    Map.entry("quest.gui-button-untrack", "<yellow>Přestat sledovat</yellow>"),
                    Map.entry("quest.gui-button-back", "<gray>« Zpět</gray>"),
                    Map.entry("quest.gui-button-close", "<gray>Zavřít</gray>"),
                    Map.entry("quest.gui-button-next-page", "<gray>Další strana »</gray>"),
                    Map.entry("quest.gui-button-prev-page", "<gray>« Předchozí strana</gray>"),
                    Map.entry("quest.gui-lore-objectives-header", "<gray>Cíle:</gray>"),
                    Map.entry("quest.gui-lore-rewards-header", "<gray>Odměny:</gray>"),
                    Map.entry("quest.gui-lore-reward-money", "<gray> - %amount% mincí</gray>"),
                    Map.entry("quest.gui-lore-reward-experience", "<gray> - %amount% zkušeností</gray>"),
                    Map.entry("quest.gui-lore-reward-bonus", "<gray> - bonus</gray>"),
                    Map.entry("quest.gui-lore-reward-section-base", "<gold>Základní odměny:</gold>"),
                    Map.entry("quest.gui-lore-reward-section-tier", "<light_purple>Bonus (%tier%):</light_purple>"),
                    Map.entry("quest.gui-lore-cooldown", "<gray>Znovu dostupný za %time%</gray>"),
                    Map.entry("quest.gui-lore-status", "<gray>Stav: %status%</gray>"),
                    Map.entry("quest.gui-button-back-hint", "<gray>Vrátí tě na předchozí obrazovku.</gray>"),
                    Map.entry("quest.gui-button-prev-page-hint", "<gray>Zobrazí předchozí stránku.</gray>"),
                    Map.entry("quest.gui-button-next-page-hint", "<gray>Zobrazí další stránku.</gray>"),
                    Map.entry("quest.gui-lore-quest-hint", "<gray>Klikni pro detail questu.</gray>"),
                    Map.entry("quest.gui-lore-guide-hint", "<dark_gray>Klikni pro zobrazení všech questů.</dark_gray>"),
                    Map.entry("quest.guide-no-active-quest", "<#F69B45><b>●</b></#F69B45>  <gray>Žádný aktivní quest</gray>"),
                    Map.entry("quest.gui-button-close-hint", "<gray>Zavře toto menu.</gray>"),
                    Map.entry("quest.gui-button-accept-hint", "<gray>Přijme tento quest a začne sledovat jeho postup.</gray>"),
                    Map.entry("quest.gui-button-abandon-hint", "<gray>Zruší rozpracovaný quest a vymaže postup.</gray>"),
                    Map.entry("quest.gui-button-track-hint", "<gray>Zapne zobrazení postupu questu na action baru.</gray>"),
                    Map.entry("quest.gui-button-untrack-hint", "<gray>Vypne zobrazení postupu questu na action baru.</gray>"),
                    Map.entry("quest.gui-button-turnin-hint", "<gray>Odevzdá splněný quest a vyzvedne odměny.</gray>")));

    /**
     * Same idea as {@link #STALE_DEFAULTS}, for keys whose default changed again after an earlier change had
     * already shipped: a server that picked up the in-between wording (not the original one recorded above)
     * would otherwise keep it forever. Each key lists every in-between value that should be upgraded to the
     * current bundled default.
     */
    private static final Map<String, Map<String, List<String>>> INTERMEDIATE_DEFAULTS = Map.of(
            "cs", Map.ofEntries(
                    Map.entry("quest.status-turned-in", List.of("ᴏᴅᴇᴠᴢᴅᴀɴý")),
                    Map.entry("quest.status-in-progress", List.of("ʀᴏᴢᴘʀᴀᴄᴏᴠᴀɴý")),
                    Map.entry("quest.gui-lore-quest-hint", List.of("<gray>ᴋʟɪᴋɴɪ ᴘʀᴏ ᴅᴇᴛᴀɪʟ ǫᴜᴇsᴛᴜ.</gray>")),
                    Map.entry("quest.editor-reward-line-name", List.of("<aqua>Název: <white>%name%</white></aqua>")),
                    Map.entry("quest.editor-prompt-reward-name", List.of("<yellow>Napiš název odměny — hráči ho uvidí v menu s questy (např. '50 mincí'):</yellow>"))),
            "en", Map.ofEntries(
                    Map.entry("quest.status-turned-in", List.of("ᴏᴅᴇᴠᴢᴅᴀɴý")),
                    Map.entry("quest.status-in-progress", List.of("ʀᴏᴢᴘʀᴀᴄᴏᴠᴀɴý")),
                    Map.entry("quest.gui-lore-quest-hint", List.of("<gray>ᴋʟɪᴋɴɪ ᴘʀᴏ ᴅᴇᴛᴀɪʟ ǫᴜᴇsᴛᴜ.</gray>")),
                    Map.entry("quest.editor-reward-line-name", List.of("<aqua>Name: <white>%name%</white></aqua>")),
                    Map.entry("quest.editor-prompt-reward-name", List.of("<yellow>Type a name for this reward — players see it in the quest menu (e.g. '50 coins'):</yellow>"))));

    /**
     * Placeholders that stand for text an admin typed (quest and tier names, descriptions, objective labels):
     * shown in the tiny font, and their {@code &} color codes work. Everything else (numbers, ids, the
     * editor's {@code %value%}) is inserted exactly as given.
     */
    private static final Set<String> ADMIN_TEXT_PLACEHOLDERS =
            Set.of("%quest%", "%description%", "%label%", "%name%", "%tier%");

    private final Map<String, YamlConfiguration> byLocale = new HashMap<>();
    private final String defaultLocale;

    private MessagesConfig(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public static MessagesConfig load(JavaPlugin plugin, String defaultLocale) {
        MessagesConfig messages = new MessagesConfig(defaultLocale);
        for (String locale : BUNDLED_LOCALES) {
            messages.loadLocale(plugin, locale);
        }
        return messages;
    }

    private void loadLocale(JavaPlugin plugin, String locale) {
        String resourcePath = "lang/" + locale + ".yml";
        File file = new File(plugin.getDataFolder(), resourcePath);
        boolean isNewFile = !file.exists();
        if (isNewFile) {
            file.getParentFile().mkdirs();
            try (InputStream in = plugin.getResource(resourcePath)) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Could not extract default language file " + resourcePath + ": " + e.getMessage());
            }
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!isNewFile) {
            mergeMissingKeys(plugin, resourcePath, file, config);
            migrateStaleDefaults(plugin, resourcePath, locale, file, config);
        }
        byLocale.put(locale, config);
    }

    /**
     * A file already on disk (from a previous plugin version) never picks up keys that got added to the
     * bundled resource later — {@link YamlConfiguration#getString} would just fall back to returning the
     * raw key as literal text, which is exactly as confusing in-game as it sounds. This fills in only the
     * keys missing from the on-disk file, leaving every key an admin already edited untouched, then
     * persists the result so this only has to run once per new key.
     */
    private void mergeMissingKeys(JavaPlugin plugin, String resourcePath, File file, YamlConfiguration config) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) {
                return;
            }
            YamlConfiguration bundled = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            boolean changed = false;
            for (String key : bundled.getKeys(true)) {
                if (bundled.isConfigurationSection(key) || config.isSet(key)) {
                    continue;
                }
                config.set(key, bundled.get(key));
                changed = true;
            }
            if (changed) {
                config.save(file);
                plugin.getLogger().info("Added new translation keys to " + resourcePath);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not merge new translation keys into " + resourcePath + ": " + e.getMessage());
        }
    }

    /** See {@link #STALE_DEFAULTS}. Runs after {@link #mergeMissingKeys} on every locale that has known stale keys. */
    private void migrateStaleDefaults(JavaPlugin plugin, String resourcePath, String locale, File file, YamlConfiguration config) {
        Map<String, String> staleDefaults = STALE_DEFAULTS.getOrDefault(locale, Map.of());
        Map<String, List<String>> intermediateDefaults = INTERMEDIATE_DEFAULTS.getOrDefault(locale, Map.of());
        if (staleDefaults.isEmpty() && intermediateDefaults.isEmpty()) {
            return;
        }
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) {
                return;
            }
            YamlConfiguration bundled = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            boolean changed = false;
            for (Map.Entry<String, String> entry : staleDefaults.entrySet()) {
                String key = entry.getKey();
                String oldDefault = entry.getValue();
                if (bundled.isSet(key) && oldDefault.equals(config.getString(key))) {
                    config.set(key, bundled.get(key));
                    changed = true;
                }
            }
            for (Map.Entry<String, List<String>> entry : intermediateDefaults.entrySet()) {
                String key = entry.getKey();
                if (bundled.isSet(key) && entry.getValue().contains(config.getString(key))) {
                    config.set(key, bundled.get(key));
                    changed = true;
                }
            }
            if (changed) {
                config.save(file);
                plugin.getLogger().info("Upgraded outdated translation defaults in " + resourcePath);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not upgrade outdated translation defaults in " + resourcePath + ": " + e.getMessage());
        }
    }

    public String get(String key, String locale) {
        String normalized = locale == null ? defaultLocale : locale.toLowerCase(Locale.ROOT);
        YamlConfiguration config = byLocale.getOrDefault(normalized, byLocale.get(defaultLocale));
        if (config == null) {
            return key;
        }
        return config.getString(key, key);
    }

    /**
     * Resolves a message for the given player's client locale, substitutes placeholders, and parses the
     * result as MiniMessage. This is the one place message text turns into something you can
     * {@code player.sendMessage(...)} — callers never touch MiniMessage directly.
     */
    public Component render(String key, Player player, Map<String, String> placeholders) {
        return render(key, player.locale().getLanguage(), placeholders);
    }

    /**
     * Same as {@link #render(String, Player, Map)} but for callers with no player at all (console senders)
     * — uses the configured default locale.
     */
    public Component render(String key, Map<String, String> placeholders) {
        return render(key, (String) null, placeholders);
    }

    /**
     * Same as {@link #render(String, Player, Map)} but for callers with no player locale to key off of
     * (console senders) — uses the configured default locale.
     */
    public Component render(String key, String locale, Map<String, String> placeholders) {
        String raw = get(key, locale);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String value = entry.getValue();
            if (ADMIN_TEXT_PLACEHOLDERS.contains(entry.getKey())) {
                value = TinyFont.convert(LegacyColors.toMiniMessage(value));
            }
            raw = raw.replace(entry.getKey(), value);
        }
        return MiniMessage.miniMessage().deserialize(raw);
    }
}
