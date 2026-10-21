package eu.purrtech.purrtechQuest.config;

import eu.purrtech.purrtechQuest.model.Quest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;

/**
 * The {@code guide:} section of {@code config.yml}: how the "guide" item (progress + current quest) looks, where it
 * sits in the main menu and in the quest list, and which quests it covers. Its lore is live data and stays in
 * {@code lang/<locale>.yml}; only {@code name} can be overridden here ({@code null} = use the lang text).
 * {@code category} is the id of the category whose quests (and its subcategories') the guide counts and
 * highlights; empty means every quest. Read fresh on every use, so {@code /questadmin reload} applies at once.
 */
public record GuideConfig(int mainSlot, int listSlot, String material, String name, Integer customModelData,
                          boolean glow, String texture, String category) {

    public static GuideConfig load() {
        return parse(JavaPlugin.getProvidingPlugin(GuideConfig.class).getConfig().getConfigurationSection("guide"));
    }

    /** {@code section} may be null (config from before this section existed): everything takes its default. */
    public static GuideConfig parse(ConfigurationSection section) {
        ConfigurationSection s = section == null ? new MemoryConfiguration() : section;
        String name = s.getString("name", "");
        return new GuideConfig(s.getInt("main-slot", 3), s.getInt("list-slot", 4), s.getString("material", "SPYGLASS"),
                name.isBlank() ? null : name, s.isInt("custom-model-data") ? s.getInt("custom-model-data") : null,
                s.getBoolean("glow", false), s.getString("texture"), s.getString("category", "").trim());
    }

    /**
     * The quests the guide covers. A {@code category} that isn't defined under {@code categories:} counts as
     * unset (every quest) rather than an empty guide — a typo shouldn't blank it out.
     */
    public List<Quest> quests(CategoriesConfig categories, Collection<Quest> all) {
        CategoriesConfig.Category category = this.category.isEmpty() ? null : categories.find(this.category);
        return category == null ? List.copyOf(all) : categories.quests(category, all);
    }
}
