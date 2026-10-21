package eu.purrtech.purrtechQuest.config;

import eu.purrtech.purrtechQuest.model.Quest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The {@code categories:} section of {@code config.yml}: which categories exist, how each one looks as an item
 * in the category menu (and in the editor's category picker), and the menu's own title/size. Categories nest —
 * a category with {@code children} opens a submenu of subcategories instead of its quest list.
 * <p>
 * Read fresh from the live config on every menu open ({@link #load()}), so {@code /questadmin reload} applies at
 * once. A config from before this section existed gets {@link #defaultCategory()} so nothing breaks.
 */
public final class CategoriesConfig {

    private static final int MIN_ROWS = 2;
    private static final int MAX_ROWS = 6;
    private static final int DEFAULT_ROWS = 3;

    /** {@code title} may be blank (fall back to the lang text); {@code rows} is 0 in a category's own menu when not overridden. */
    public record Menu(String title, int rows) {
    }

    /** One category item. {@code slot} is -1 when unset. {@code customModelData} / {@code texture} may be null. */
    public record Category(String id, int slot, String material, String name, List<String> lore,
                           Integer customModelData, boolean glow, String texture, Menu menu,
                           List<Category> children) {

        /** This category's id plus every descendant's. */
        public Set<String> ids() {
            Set<String> ids = new HashSet<>();
            ids.add(id);
            for (Category child : children) {
                ids.addAll(child.ids());
            }
            return ids;
        }
    }

    private final String defaultId;
    private final Menu rootMenu;
    private final List<Category> roots;
    private final Map<String, Category> byId;

    private CategoriesConfig(String defaultId, Menu rootMenu, List<Category> roots, Map<String, Category> byId) {
        this.defaultId = defaultId;
        this.rootMenu = rootMenu;
        this.roots = roots;
        this.byId = byId;
    }

    public static CategoriesConfig load() {
        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(CategoriesConfig.class);
        return parse(plugin.getConfig().getConfigurationSection("categories"), plugin.getLogger()::warning);
    }

    /** {@code section} may be null (no {@code categories:} in the config). */
    public static CategoriesConfig parse(ConfigurationSection section, Consumer<String> warn) {
        Map<String, Category> byId = new LinkedHashMap<>();
        List<Category> roots = new ArrayList<>();
        ConfigurationSection list = section == null ? null : section.getConfigurationSection("list");
        if (list != null) {
            parseInto(list, roots, byId, warn);
        }
        // Only a config from before this section existed gets the built-in category; an empty list under an
        // existing section is the admin's choice (no categories yet) and stays empty.
        if (section == null) {
            Category fallback = defaultCategory();
            roots.add(fallback);
            byId.put(fallback.id(), fallback);
        }

        int rows = section == null ? DEFAULT_ROWS : clampRows(section.getInt("menu.rows", DEFAULT_ROWS));
        Menu rootMenu = new Menu(section == null ? "" : section.getString("menu.title", ""), rows);

        String defaultId = section == null ? "default" : section.getString("default", "default");
        if (!byId.containsKey(defaultId) && !roots.isEmpty()) {
            String first = roots.get(0).id();
            warn.accept("categories.default '" + defaultId + "' is not a defined category, using '" + first + "'");
            defaultId = first;
        }
        return new CategoriesConfig(defaultId, rootMenu, List.copyOf(roots), Map.copyOf(byId));
    }

    private static void parseInto(ConfigurationSection list, List<Category> target, Map<String, Category> byId,
                                  Consumer<String> warn) {
        for (String id : list.getKeys(false)) {
            ConfigurationSection s = list.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            if (byId.containsKey(id)) {
                warn.accept("Category id '" + id + "' is defined twice, ignoring the second one");
                continue;
            }
            // Reserve the id before the children so a child can't reuse it; replaced by the full record below.
            byId.put(id, null);
            List<Category> children = new ArrayList<>();
            ConfigurationSection childSection = s.getConfigurationSection("children");
            if (childSection != null) {
                parseInto(childSection, children, byId, warn);
            }
            Menu menu = new Menu(s.getString("menu.title", ""), s.getInt("menu.rows", 0));
            Category category = new Category(id, s.getInt("slot", -1), s.getString("material", "BOOK"),
                    s.getString("name", id), s.getStringList("lore"),
                    s.isInt("custom-model-data") ? s.getInt("custom-model-data") : null,
                    s.getBoolean("glow", false), s.getString("texture"), menu, List.copyOf(children));
            byId.put(id, category);
            target.add(category);
        }
    }

    /** What a server with no {@code categories:} section gets: one category, which is also the default. */
    public static Category defaultCategory() {
        return new Category("default", 13, "BOOK", "<gold>Obecné questy</gold>", List.of(), null, false, null,
                new Menu("", 0), List.of());
    }

    private static int clampRows(int rows) {
        return Math.max(MIN_ROWS, Math.min(MAX_ROWS, rows));
    }

    /** Id new quests created in the editor start in; always a defined category. */
    public String defaultId() {
        return defaultId;
    }

    /** Top-level categories, in config order. */
    public List<Category> roots() {
        return roots;
    }

    public Category find(String id) {
        return byId.get(id);
    }

    /** The menu listing {@code parent}'s children, or the main menu when {@code parent} is null. */
    public Menu menuFor(Category parent) {
        if (parent == null) {
            return rootMenu;
        }
        int rows = parent.menu().rows() > 0 ? clampRows(parent.menu().rows()) : rootMenu.rows();
        String title = parent.menu().title().isBlank() ? rootMenu.title() : parent.menu().title();
        return new Menu(title, rows);
    }

    /**
     * Quests shown for a category: its own plus every descendant's. Quests whose category isn't defined in the
     * config at all (an old quest file) are listed under the default category so they never become unreachable.
     */
    public List<Quest> quests(Category category, Collection<Quest> all) {
        Set<String> ids = category.ids();
        boolean includesDefault = ids.contains(defaultId);
        return all.stream()
                .filter(quest -> ids.contains(quest.category())
                        || (includesDefault && !byId.containsKey(quest.category())))
                .toList();
    }

    /**
     * Assigns each item its configured slot; an item whose slot is unset, out of range, reserved or already
     * taken goes to the first free slot instead, so a typo in the config never hides a category.
     */
    public static Map<Integer, Category> layout(List<Category> items, int size, Set<Integer> reserved) {
        Map<Integer, Category> placed = new LinkedHashMap<>();
        List<Category> unplaced = new ArrayList<>();
        for (Category item : items) {
            int slot = item.slot();
            if (slot >= 0 && slot < size && !reserved.contains(slot) && !placed.containsKey(slot)) {
                placed.put(slot, item);
            } else {
                unplaced.add(item);
            }
        }
        int next = 0;
        for (Category item : unplaced) {
            while (next < size && (reserved.contains(next) || placed.containsKey(next))) {
                next++;
            }
            if (next >= size) {
                break;
            }
            placed.put(next, item);
        }
        return placed;
    }
}
