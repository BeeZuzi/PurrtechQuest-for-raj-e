package eu.purrtech.purrtechQuest.config;

import eu.purrtech.purrtechQuest.model.ObjectiveType;
import eu.purrtech.purrtechQuest.model.Quest;
import eu.purrtech.purrtechQuest.model.QuestObjective;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideConfigTest {

    @Test
    void missingSectionKeepsTheOriginalSpyglassOnSlotsThreeAndFour() {
        GuideConfig guide = GuideConfig.parse(null);

        assertEquals("SPYGLASS", guide.material());
        assertEquals(3, guide.mainSlot());
        assertEquals(4, guide.listSlot());
        assertNull(guide.name());
        assertNull(guide.customModelData());
    }

    @Test
    void parsesOverrides() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                guide:
                  main-slot: 5
                  list-slot: 2
                  material: COMPASS
                  name: "<gold>Průvodce"
                  custom-model-data: 12
                  glow: true
                """);

        GuideConfig guide = GuideConfig.parse(config.getConfigurationSection("guide"));

        assertEquals(5, guide.mainSlot());
        assertEquals(2, guide.listSlot());
        assertEquals("COMPASS", guide.material());
        assertEquals("<gold>Průvodce", guide.name());
        assertEquals(12, guide.customModelData());
        assertTrue(guide.glow());
    }

    @Test
    void guideCoversOnlyItsCategoryEvenWhenTheCategoryIsNotDefined() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                categories:
                  default: general
                  list:
                    general: {}
                    zacatek: {}
                """);
        CategoriesConfig categories = CategoriesConfig.parse(yaml.getConfigurationSection("categories"), w -> { });
        List<Quest> all = List.of(quest("a", "zacatek"), quest("b", "general"), quest("c", "removed"));

        assertEquals(List.of("a"), ids(guide("zacatek").quests(categories, all)));
        assertEquals(List.of("a", "b", "c"), ids(guide("").quests(categories, all)));
        assertEquals(List.of("c"), ids(guide("removed").quests(categories, all)));
        assertEquals(List.of(), ids(guide("typo").quests(categories, all)));
    }

    @Test
    void missingSectionFollowsZacatekEvenWithoutACategoriesSection() {
        CategoriesConfig categories = CategoriesConfig.parse(null, w -> { });
        List<Quest> all = List.of(quest("a", "zacatek"), quest("b", "default"), quest("c", "mining"));

        assertEquals(List.of("a"), ids(GuideConfig.parse(null).quests(categories, all)));
    }

    private static GuideConfig guide(String category) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("guide:\n  category: \"" + category + "\"\n");
        return GuideConfig.parse(yaml.getConfigurationSection("guide"));
    }

    private static List<String> ids(List<Quest> quests) {
        return quests.stream().map(Quest::id).toList();
    }

    private static Quest quest(String id, String category) {
        return new Quest(id, id, "", category,
                List.of(new QuestObjective(ObjectiveType.BREAK_BLOCK, "STONE", 1)),
                List.of(), List.of(), List.of(), false, 0, false, false, null);
    }

    @Test
    void blankNameMeansUseTheLangText() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("guide:\n  name: \"\"\n");

        assertNull(GuideConfig.parse(config.getConfigurationSection("guide")).name());
    }
}
