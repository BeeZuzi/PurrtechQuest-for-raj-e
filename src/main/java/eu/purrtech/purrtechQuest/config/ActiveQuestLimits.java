package eu.purrtech.purrtechQuest.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The {@code active-quest-limit:} section of {@code config.yml}: how many quests a player may have in progress
 * at once, depending on their rank. A rank is held when the player has the permission {@code permission} with
 * {@code %rank%} replaced by the rank's id ({@code group.%rank%} matches a LuckPerms group, inherited ones
 * included); the highest limit among the ranks held applies, and {@code defaultLimit} when they hold none.
 * A negative limit means unlimited. A config with no such section at all (a server's existing config.yml from
 * before this option existed is never rewritten by the plugin) gets {@link #standard()}, the same values the
 * bundled config.yml ships, so dropping in a new jar is enough for the limits to apply.
 */
public record ActiveQuestLimits(int defaultLimit, String permission, Map<String, Integer> ranks) {

    public static final int UNLIMITED = Integer.MAX_VALUE;
    private static final String DEFAULT_PERMISSION = "group.%rank%";

    public ActiveQuestLimits {
        ranks = Map.copyOf(ranks);
    }

    public static ActiveQuestLimits unlimited() {
        return new ActiveQuestLimits(UNLIMITED, DEFAULT_PERMISSION, Map.of());
    }

    /** The limits bundled config.yml ships; used when the server's config.yml has no such section. */
    public static ActiveQuestLimits standard() {
        Map<String, Integer> ranks = new LinkedHashMap<>();
        ranks.put("hrac", 3);
        ranks.put("rajce", 4);
        ranks.put("ninja", 5);
        ranks.put("king", 6);
        ranks.put("sky", 6);
        return new ActiveQuestLimits(3, DEFAULT_PERMISSION, ranks);
    }

    /** {@code section} may be null (config from before this option existed): {@link #standard()}. */
    public static ActiveQuestLimits parse(ConfigurationSection section) {
        if (section == null) {
            return standard();
        }
        Map<String, Integer> ranks = new LinkedHashMap<>();
        ConfigurationSection rankSection = section.getConfigurationSection("ranks");
        if (rankSection != null) {
            for (String rank : rankSection.getKeys(false)) {
                ranks.put(rank, normalize(rankSection.getInt(rank)));
            }
        }
        String permission = section.getString("permission", DEFAULT_PERMISSION);
        return new ActiveQuestLimits(normalize(section.getInt("default", 3)),
                permission == null || permission.isBlank() ? DEFAULT_PERMISSION : permission, ranks);
    }

    private static int normalize(int limit) {
        return limit < 0 ? UNLIMITED : limit;
    }

    /** The limit for a player, given a way to ask whether they hold a permission node. */
    public int limitFor(Predicate<String> hasPermission) {
        int limit = defaultLimit;
        for (Map.Entry<String, Integer> rank : ranks.entrySet()) {
            if (rank.getValue() > limit && hasPermission.test(permission.replace("%rank%", rank.getKey()))) {
                limit = rank.getValue();
            }
        }
        return limit;
    }
}
