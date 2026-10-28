package eu.purrtech.purrtechQuest.gui;

import eu.purrtech.purrtechQuest.config.CategoriesConfig;
import eu.purrtech.purrtechQuest.config.CategoriesConfig.Category;
import eu.purrtech.purrtechQuest.config.GuideConfig;
import eu.purrtech.purrtechQuest.config.MessagesConfig;
import eu.purrtech.purrtechQuest.player.PlayerQuestDataCache;
import eu.purrtech.purrtechQuest.service.QuestService;
import eu.purrtech.purrtechQuest.service.QuestTrackingService;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@code /quest gui}'s landing page: the categories from {@code categories:} in {@code config.yml}, each at its
 * configured slot, plus (on the main menu only) the "guide" item ({@code guide:} in the config) — turned-in/total
 * across the guide's quests and a breakdown of the one in progress; clicking it opens {@link QuestLogGui} with
 * just the guide's quests ({@link GuideConfig#quests}), so a quest in another category never shows up there. Clicking a category opens its subcategories (a menu of the same kind),
 * or its quest list when it has none. A submenu's bottom row also has a button for the category's own quests.
 */
public final class QuestCategoryGui extends Gui {

    private final QuestService questService;
    private final PlayerQuestDataCache playerCache;
    private final QuestTrackingService trackingService;
    private final MessagesConfig messages;
    private final Player player;
    private final CategoriesConfig categories;
    private final Category parent;
    private final Runnable onBack;
    private final int size;

    public QuestCategoryGui(QuestService questService, PlayerQuestDataCache playerCache, QuestTrackingService trackingService,
                             MessagesConfig messages, Player player) {
        this(questService, playerCache, trackingService, messages, player, CategoriesConfig.load(), null, null);
    }

    private QuestCategoryGui(QuestService questService, PlayerQuestDataCache playerCache,
                              QuestTrackingService trackingService, MessagesConfig messages, Player player,
                              CategoriesConfig categories, Category parent, Runnable onBack) {
        super(CategoryMenu.title(categories.menuFor(parent), messages, player), categories.menuFor(parent).rows() * 9);
        this.questService = questService;
        this.playerCache = playerCache;
        this.trackingService = trackingService;
        this.messages = messages;
        this.player = player;
        this.categories = categories;
        this.parent = parent;
        this.onBack = onBack;
        this.size = categories.menuFor(parent).rows() * 9;
        render();
    }

    private void render() {
        clear();
        int row = CategoryMenu.bottomRow(size);

        Set<Integer> reserved = new HashSet<>(CategoryMenu.bottomControls(size));
        // A negative slot in the config hides the guide altogether.
        int configured = GuideConfig.load().mainSlot();
        int guideSlot = configured < 0 ? -1 : CategoryMenu.validSlot(configured, size, reserved, 3);
        if (parent == null && guideSlot >= 0) {
            reserved.add(guideSlot);
        }
        List<Category> items = parent == null ? categories.roots() : parent.children();
        CategoriesConfig.layout(items, size, reserved).forEach((slot, category) -> {
            boolean hasChildren = !category.children().isEmpty();
            var hint = messages.render(hasChildren ? "quest.category-hint-sub" : "quest.category-hint-open", player, Map.of());
            setItem(slot, CategoryMenu.icon(category, List.of(hint)), event -> open(category));
        });

        if (parent == null) {
            if (guideSlot >= 0) {
                setItem(guideSlot, GuideIcon.build(questService, playerCache, messages, player, true), event ->
                        new QuestLogGui(questService, playerCache, trackingService, messages, player,
                                GuideConfig.load().quests(categories, questService.allQuests()), this::reopen)
                                .open(player));
            }
        } else {
            setItem(row + 3, GuiItems.icon(Material.IRON_DOOR, messages.render("quest.gui-nav-back", player, Map.of()),
                            List.of(messages.render("quest.gui-nav-back-hint", player, Map.of()))),
                    event -> onBack.run());
            setItem(row + 5, GuiItems.icon(Material.BOOKSHELF,
                            messages.render("quest.category-open-own", player, Map.of()),
                            List.of(messages.render("quest.category-open-own-hint", player, Map.of()))),
                    event -> openQuests(parent));
        }
        setItem(row + 4, GuiItems.closeButton(messages.render("quest.gui-nav-close", player, Map.of()),
                        List.of(messages.render("quest.gui-nav-close-hint", player, Map.of()))),
                event -> player.closeInventory());
    }

    private void open(Category category) {
        if (category.children().isEmpty()) {
            openQuests(category);
            return;
        }
        new QuestCategoryGui(questService, playerCache, trackingService, messages, player, categories, category,
                this::reopen).open(player);
    }

    private void openQuests(Category category) {
        new QuestLogGui(questService, playerCache, trackingService, messages, player,
                categories.quests(category, questService.allQuests()), category.name(), this::reopen).open(player);
    }

    /** Re-reads the config, so a reload done while a player had a submenu open still takes effect on return. */
    private void reopen() {
        CategoriesConfig fresh = CategoriesConfig.load();
        Category freshParent = parent == null ? null : fresh.find(parent.id());
        new QuestCategoryGui(questService, playerCache, trackingService, messages, player, fresh, freshParent,
                freshParent == null ? null : onBack).open(player);
    }
}
