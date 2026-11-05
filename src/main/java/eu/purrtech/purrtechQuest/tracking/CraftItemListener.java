package eu.purrtech.purrtechQuest.tracking;

import eu.purrtech.purrtechQuest.model.ObjectiveType;
import eu.purrtech.purrtechQuest.service.QuestService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * CRAFT_ITEM tracks taking a crafted item out of the crafting table's result slot — separate from
 * COLLECT_ITEM (picking up a dropped item) since "craft this" and "find/loot this" are different
 * objectives worth telling apart. Reports {@link ItemMatcher#resolveTarget} like COLLECT_ITEM, so custom
 * ItemsAdder/Oraxen crafted items are addressable by their own id.
 * <p>
 * A normal click counts the recipe's yield once. A shift-click crafts as many as the ingredients and the free
 * inventory room allow, but {@link CraftItemEvent} doesn't say how many that was — so the player's inventory is
 * counted before the click and again one tick later, and the difference is what was really crafted. That also
 * stays right when the inventory fills up halfway through.
 */
public final class CraftItemListener implements Listener {

    private final JavaPlugin plugin;
    private final QuestService questService;

    public CraftItemListener(JavaPlugin plugin, QuestService questService) {
        this.plugin = plugin;
        this.questService = questService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        ItemStack result = current != null && !current.getType().isAir() ? current.clone() : event.getRecipe().getResult();
        if (result.getType().isAir()) {
            return;
        }
        String target = ItemMatcher.resolveTarget(result);

        if (!event.isShiftClick()) {
            questService.updateProgress(player, ObjectiveType.CRAFT_ITEM, target, result.getAmount());
            return;
        }

        int before = countSimilar(player, result);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            int crafted = countSimilar(player, result) - before;
            if (crafted > 0) {
                questService.updateProgress(player, ObjectiveType.CRAFT_ITEM, target, crafted);
            }
        });
    }

    /** How many items like {@code like} the player carries in their main inventory and hotbar. */
    private static int countSimilar(Player player, ItemStack like) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.isSimilar(like)) {
                total += stack.getAmount();
            }
        }
        return total;
    }
}
