package eu.purrtech.purrtechQuest.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActiveQuestLimitsTest {

    private static ActiveQuestLimits parse(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return ActiveQuestLimits.parse(config.getConfigurationSection("active-quest-limit"));
    }

    private static final String RANKS = """
            active-quest-limit:
              default: 3
              permission: "group.%rank%"
              ranks:
                hrac: 3
                rajce: 4
                ninja: 5
                king: 6
                sky: 6
            """;

    @Test
    void missingSectionMeansUnlimited() {
        assertEquals(ActiveQuestLimits.UNLIMITED, ActiveQuestLimits.parse(null).limitFor(p -> false));
    }

    @Test
    void playerWithNoRankGetsTheDefault() throws Exception {
        assertEquals(3, parse(RANKS).limitFor(p -> false));
    }

    @Test
    void highestHeldRankWins() throws Exception {
        ActiveQuestLimits limits = parse(RANKS);

        assertEquals(4, limits.limitFor(Set.of("group.rajce")::contains));
        assertEquals(6, limits.limitFor(Set.of("group.hrac", "group.rajce", "group.ninja", "group.king")::contains));
        assertEquals(5, limits.limitFor(Set.of("group.ninja", "group.hrac")::contains));
    }

    @Test
    void permissionTemplateIsConfigurable() throws Exception {
        ActiveQuestLimits limits = parse("""
                active-quest-limit:
                  permission: "purrtechquest.rank.%rank%"
                  ranks:
                    vip: 8
                """);

        assertEquals(3, limits.limitFor(Set.of("group.vip")::contains));
        assertEquals(8, limits.limitFor(Set.of("purrtechquest.rank.vip")::contains));
    }

    @Test
    void negativeLimitMeansUnlimited() throws Exception {
        ActiveQuestLimits limits = parse("""
                active-quest-limit:
                  default: 2
                  ranks:
                    admin: -1
                """);

        assertEquals(2, limits.limitFor(p -> false));
        assertEquals(ActiveQuestLimits.UNLIMITED, limits.limitFor(Set.of("group.admin")::contains));
    }
}
