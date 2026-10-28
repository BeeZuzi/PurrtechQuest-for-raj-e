package eu.purrtech.purrtechQuest.gui;

import eu.purrtech.purrtechQuest.config.CategoriesConfig;
import eu.purrtech.purrtechQuest.config.MessagesConfig;
import eu.purrtech.purrtechQuest.util.DisplayText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the player-facing category menu ({@link QuestCategoryGui}) and the editor's picker
 * ({@link CategoryPickerGui}) share: the title, a category's item, and where the bottom-row buttons sit.
 */
final class CategoryMenu {

    private CategoryMenu() {
    }

    /** Bottom row, left to right: back at {@code +3}, close at {@code +4}, the screen's own action at {@code +5}. */
    static int bottomRow(int size) {
        return size - 9;
    }

    static Set<Integer> bottomControls(int size) {
        int row = bottomRow(size);
        return Set.of(row + 3, row + 4, row + 5);
    }

    /** The configured title (MiniMessage), or the lang text when the config leaves it blank. */
    static Component title(CategoriesConfig.Menu menu, MessagesConfig messages, Player player) {
        if (menu.title().isBlank()) {
            return messages.render("quest.gui-category-title", player, Map.of());
        }
        return MiniMessage.miniMessage().deserialize(menu.title());
    }

    /**
     * {@code slot} if it's inside the menu and not one of {@code blocked}, else {@code fallback} — so a
     * misplaced guide in the config never ends up on top of a button or outside the inventory.
     */
    static int validSlot(int slot, int size, Set<Integer> blocked, int fallback) {
        return slot >= 0 && slot < size && !blocked.contains(slot) ? slot : fallback;
    }

    /** An unnamed item from config settings; an unknown material falls back to a book. */
    static ItemStack baseStack(String materialName, Integer customModelData, boolean glow, String texture) {
        ItemStack base;
        if (texture != null && !texture.isBlank()) {
            base = GuiItems.textureHead(texture, customModelData);
        } else {
            Material material = Material.matchMaterial(materialName);
            base = new ItemStack(material == null || material.isAir() ? Material.BOOK : material);
            ItemMeta meta = base.getItemMeta();
            if (customModelData != null) {
                meta.setCustomModelData(customModelData);
            }
            base.setItemMeta(meta);
        }
        if (glow) {
            ItemMeta meta = base.getItemMeta();
            meta.setEnchantmentGlintOverride(true);
            base.setItemMeta(meta);
        }
        return base;
    }

    /** The category as configured; {@code hint} lines are appended to its lore. */
    static ItemStack icon(CategoriesConfig.Category category, List<Component> hint) {
        ItemStack base = baseStack(category.material(), category.customModelData(), category.glow(), category.texture());
        List<Component> lore = new ArrayList<>();
        for (String line : category.lore()) {
            lore.add(DisplayText.component(line, NamedTextColor.GRAY));
        }
        if (!hint.isEmpty()) {
            if (!lore.isEmpty()) {
                lore.add(Component.empty());
            }
            lore.addAll(hint);
        }
        return GuiItems.icon(base, DisplayText.component(category.name(), NamedTextColor.WHITE), lore);
    }
}
