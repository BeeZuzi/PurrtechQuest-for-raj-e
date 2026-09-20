package eu.purrtech.purrtechQuest.gui;

import eu.purrtech.purrtechQuest.config.MessagesConfig;
import eu.purrtech.purrtechQuest.model.ObjectiveType;
import eu.purrtech.purrtechQuest.model.QuestReward;
import eu.purrtech.purrtechQuest.model.QuestStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;

final class QuestIcons {

    private QuestIcons() {
    }

    /**
     * A quest's raw {@code display-name}, deserialized as MiniMessage so an admin's own styling (custom hex,
     * this server's tiny-caps font, ...) comes through unchanged instead of being forced into one fixed
     * color; {@code colorIfAbsent} keeps a plain display-name with no styling of its own looking exactly as
     * before ({@code fallbackColor}). Falls back to plain text on a malformed value (e.g. a stray
     * {@code <} typed into the name in the quest editor) so a bad display-name degrades to ugly rather than
     * making the quest's icon impossible to render at all.
     */
    static Component displayNameComponent(String rawDisplayName, TextColor fallbackColor) {
        try {
            return MiniMessage.miniMessage().deserialize(rawDisplayName).colorIfAbsent(fallbackColor);
        } catch (ParsingException e) {
            return Component.text(rawDisplayName, fallbackColor);
        }
    }

    /** Shared by {@link QuestDetailGui} (full reward list) and {@link QuestLogGui} (compact book lore). */
    static Component rewardLine(QuestReward reward, MessagesConfig messages, Player player) {
        return switch (reward) {
            case QuestReward.Money money ->
                    messages.render("quest.gui-lore-reward-money", player, Map.of("%amount%", String.valueOf(money.amount())));
            case QuestReward.Item item ->
                    messages.render("quest.gui-lore-reward-item", player,
                            Map.of("%amount%", String.valueOf(item.amount()), "%material%", item.material()));
            case QuestReward.Experience experience ->
                    messages.render("quest.gui-lore-reward-experience", player, Map.of("%amount%", String.valueOf(experience.amount())));
            case QuestReward.Command ignored -> messages.render("quest.gui-lore-reward-bonus", player, Map.of());
            case QuestReward.Permission ignored -> messages.render("quest.gui-lore-reward-bonus", player, Map.of());
        };
    }

    static Material materialFor(QuestStatus status) {
        return switch (status) {
            case NOT_ACCEPTED -> Material.BOOK;
            case IN_PROGRESS -> Material.WRITABLE_BOOK;
            case COMPLETED -> Material.ENCHANTED_BOOK;
            case TURNED_IN -> Material.KNOWLEDGE_BOOK;
        };
    }

    static Material materialFor(ObjectiveType type) {
        return switch (type) {
            case KILL_ENTITY -> Material.IRON_SWORD;
            case BREAK_BLOCK -> Material.IRON_PICKAXE;
            case PLACE_BLOCK -> Material.BRICKS;
            case COLLECT_ITEM -> Material.CHEST;
            case CRAFT_ITEM -> Material.CRAFTING_TABLE;
            case FISH -> Material.FISHING_ROD;
            case REACH_LOCATION -> Material.COMPASS;
            case INTERACT_BLOCK -> Material.LEVER;
            case TALK_TO_NPC -> Material.VILLAGER_SPAWN_EGG;
            case SPEND_MONEY -> Material.EMERALD;
            case PLACEHOLDER_CHECK -> Material.COMPARATOR;
            case DEFEAT_BOSS -> Material.WITHER_SKELETON_SKULL;
            case EARN_MONEY -> Material.GOLD_INGOT;
            case CUSTOM -> Material.NETHER_STAR;
        };
    }
}
