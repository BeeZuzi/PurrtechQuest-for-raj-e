package eu.purrtech.purrtechQuest.gui;

import eu.purrtech.purrtechQuest.config.CategoriesConfig;
import eu.purrtech.purrtechQuest.config.CategoriesConfig.Category;
import eu.purrtech.purrtechQuest.config.MessagesConfig;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The editor's category chooser: the same menu players see ({@link QuestCategoryGui}, same slots and
 * title), but clicking a category picks it for the quest. A category with subcategories opens them instead;
 * its own button in the submenu's bottom row picks the parent itself.
 */
public final class CategoryPickerGui extends Gui {

    private final MessagesConfig messages;
    private final Player player;
    private final CategoriesConfig categories;
    private final Category parent;
    private final Consumer<String> onPick;
    private final Runnable onBack;
    private final int size;

    /** {@code onBack} runs from the back button of the top-level menu (the editor reopens itself there). */
    public CategoryPickerGui(MessagesConfig messages, Player player, Consumer<String> onPick, Runnable onBack) {
        this(messages, player, CategoriesConfig.load(), null, onPick, onBack);
    }

    private CategoryPickerGui(MessagesConfig messages, Player player, CategoriesConfig categories, Category parent,
                               Consumer<String> onPick, Runnable onBack) {
        super(CategoryMenu.title(categories.menuFor(parent), messages, player), categories.menuFor(parent).rows() * 9);
        this.messages = messages;
        this.player = player;
        this.categories = categories;
        this.parent = parent;
        this.onPick = onPick;
        this.onBack = onBack;
        this.size = categories.menuFor(parent).rows() * 9;
        render();
    }

    private void render() {
        clear();
        int row = CategoryMenu.bottomRow(size);
        List<Category> items = parent == null ? categories.roots() : parent.children();
        CategoriesConfig.layout(items, size, CategoryMenu.bottomControls(size)).forEach((slot, category) -> {
            boolean hasChildren = !category.children().isEmpty();
            var hint = messages.render(hasChildren ? "quest.category-hint-sub" : "quest.category-hint-pick", player, Map.of());
            setItem(slot, CategoryMenu.icon(category, List.of(hint)), event -> {
                if (hasChildren) {
                    new CategoryPickerGui(messages, player, categories, category, onPick, this::reopen).open(player);
                } else {
                    onPick.accept(category.id());
                }
            });
        });

        setItem(row + 4, GuiItems.icon(Material.IRON_DOOR, messages.render("quest.gui-nav-back", player, Map.of()),
                        List.of(messages.render("quest.gui-nav-back-hint", player, Map.of()))),
                event -> onBack.run());
        if (parent != null) {
            setItem(row + 5, GuiItems.icon(Material.LIME_DYE,
                            messages.render("quest.category-pick-this", player, Map.of()),
                            List.of(messages.render("quest.category-pick-this-hint", player, Map.of()))),
                    event -> onPick.accept(parent.id()));
        }
    }

    private void reopen() {
        new CategoryPickerGui(messages, player, categories, parent, onPick, onBack).open(player);
    }
}
