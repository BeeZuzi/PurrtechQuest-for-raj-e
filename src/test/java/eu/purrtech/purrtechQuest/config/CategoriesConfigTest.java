package eu.purrtech.purrtechQuest.config;

import eu.purrtech.purrtechQuest.config.CategoriesConfig.Category;
import eu.purrtech.purrtechQuest.model.ObjectiveType;
import eu.purrtech.purrtechQuest.model.Quest;
import eu.purrtech.purrtechQuest.model.QuestObjective;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoriesConfigTest {

    private final List<String> warnings = new ArrayList<>();

    private CategoriesConfig parse(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return CategoriesConfig.parse(config.getConfigurationSection("categories"), warnings::add);
    }

    @Test
    void missingSectionGivesOneDefaultCategory() {
        CategoriesConfig categories = CategoriesConfig.parse(null, warnings::add);

        assertEquals("default", categories.defaultId());
        assertEquals(List.of("default"), categories.roots().stream().map(Category::id).toList());
        assertEquals(3, categories.menuFor(null).rows());
    }

    @Test
    void emptyListStaysEmptyAndKeepsTheConfiguredDefaultId() throws Exception {
        CategoriesConfig categories = parse("""
                categories:
                  default: zacatek
                  list:
                """);

        assertTrue(categories.roots().isEmpty());
        assertEquals("zacatek", categories.defaultId());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void parsesItemSettingsChildrenAndMenu() throws Exception {
        CategoriesConfig categories = parse("""
                categories:
                  default: mining
                  menu: {title: "<red>Hi", rows: 4}
                  list:
                    mining:
                      slot: 12
                      material: IRON_PICKAXE
                      name: "<gold>Mining"
                      lore: ["a", "b"]
                      custom-model-data: 7
                      glow: true
                      menu: {rows: 5}
                      children:
                        ores: {slot: 3, material: IRON_ORE}
                """);

        Category mining = categories.find("mining");
        assertEquals(12, mining.slot());
        assertEquals(7, mining.customModelData());
        assertTrue(mining.glow());
        assertEquals(List.of("a", "b"), mining.lore());
        assertEquals("ores", mining.children().get(0).id());
        assertEquals(Set.of("mining", "ores"), mining.ids());
        assertNull(categories.find("ores").customModelData());
        assertEquals("mining", categories.defaultId());
        // Submenu overrides rows but inherits the main menu's title.
        assertEquals(5, categories.menuFor(mining).rows());
        assertEquals("<red>Hi", categories.menuFor(mining).title());
        assertEquals(4, categories.menuFor(null).rows());
    }

    @Test
    void rowsAreClampedAndUnknownDefaultFallsBackToFirst() throws Exception {
        CategoriesConfig categories = parse("""
                categories:
                  default: nope
                  menu: {rows: 99}
                  list:
                    a: {}
                """);

        assertEquals(6, categories.menuFor(null).rows());
        assertEquals("a", categories.defaultId());
        assertEquals(1, warnings.size());
    }

    @Test
    void duplicateIdIsIgnoredWithAWarning() throws Exception {
        CategoriesConfig categories = parse("""
                categories:
                  default: a
                  list:
                    a:
                      children:
                        a: {}
                """);

        assertTrue(categories.find("a").children().isEmpty());
        assertEquals(1, warnings.size());
    }

    @Test
    void layoutFallsBackToFreeSlotsForBadOrTakenSlots() {
        Category first = category("first", 4);
        Category clash = category("clash", 4);
        Category outOfRange = category("far", 99);
        Category unset = category("unset", -1);

        Map<Integer, Category> layout = CategoriesConfig.layout(
                List.of(first, clash, outOfRange, unset), 9, Set.of(0));

        assertEquals(first, layout.get(4));
        // Slot 0 is reserved, so the three displaced items take 1, 2, 3.
        assertEquals(clash, layout.get(1));
        assertEquals(outOfRange, layout.get(2));
        assertEquals(unset, layout.get(3));
    }

    @Test
    void questsIncludeDescendantsAndUndefinedCategoriesGoToDefault() throws Exception {
        CategoriesConfig categories = parse("""
                categories:
                  default: general
                  list:
                    general: {}
                    mining:
                      children:
                        ores: {}
                """);
        List<Quest> all = List.of(quest("q1", "general"), quest("q2", "mining"), quest("q3", "ores"),
                quest("q4", "old-removed-category"));

        assertEquals(List.of("q2", "q3"), ids(categories.quests(categories.find("mining"), all)));
        assertEquals(List.of("q3"), ids(categories.quests(categories.find("ores"), all)));
        assertEquals(List.of("q1", "q4"), ids(categories.quests(categories.find("general"), all)));
    }

    private static List<String> ids(List<Quest> quests) {
        return quests.stream().map(Quest::id).toList();
    }

    private static Category category(String id, int slot) {
        return new Category(id, slot, "BOOK", id, List.of(), null, false, null,
                new CategoriesConfig.Menu("", 0), List.of());
    }

    private static Quest quest(String id, String category) {
        return new Quest(id, id, "", category,
                List.of(new QuestObjective(ObjectiveType.BREAK_BLOCK, "STONE", 1)),
                List.of(), List.of(), List.of(), false, 0, false, false, null);
    }
}
