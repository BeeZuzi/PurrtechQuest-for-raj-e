package eu.purrtech.purrtechQuest.gui;

import eu.purrtech.purrtechQuest.config.MessagesConfig;
import eu.purrtech.purrtechQuest.model.ChoiceGroups;
import eu.purrtech.purrtechQuest.model.PlayerQuestData;
import eu.purrtech.purrtechQuest.model.Quest;
import eu.purrtech.purrtechQuest.model.QuestObjective;
import eu.purrtech.purrtechQuest.model.QuestProgress;
import eu.purrtech.purrtechQuest.player.PlayerQuestDataCache;
import eu.purrtech.purrtechQuest.service.QuestService;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The "guide" item shown in the quest menus: overall turned-in/total across every quest, then a breakdown of
 * whichever quest the player currently has in progress (first match, same as
 * {@link QuestService#currentActiveQuest}) — its name, description, and live objective progress. Shared by
 * the landing page ({@link QuestCategoryGui}, where clicking it opens the quest list) and the quest list
 * itself ({@link QuestLogGui}, where it is display-only).
 */
final class GuideIcon {

    private GuideIcon() {
    }

    /** {@code clickable} adds the "click to see every quest" hint line; leave it off where clicking does nothing. */
    static ItemStack build(QuestService questService, PlayerQuestDataCache playerCache, MessagesConfig messages,
                           Player player, boolean clickable) {
        Component name = messages.render("quest.guide-name", player, Map.of());
        List<Component> lore = new ArrayList<>();
        lore.add(messages.render("quest.guide-intro", player, Map.of()));

        QuestService.OverallSummary overall = questService.overallSummary(player);
        lore.add(messages.render("quest.guide-completed", player, Map.of(
                "%done%", String.valueOf(overall.turnedIn()), "%total%", String.valueOf(overall.total()))));
        lore.add(Component.empty());
        lore.add(messages.render("quest.guide-current-quest-header", player, Map.of()));

        Quest active = questService.currentActiveQuest(player).orElse(null);
        if (active == null) {
            lore.add(messages.render("quest.guide-no-active-quest", player, Map.of()));
        } else {
            lore.add(messages.render("quest.guide-current-quest-name", player, Map.of("%quest%", active.displayName())));
            if (!active.description().isBlank()) {
                lore.add(messages.render("quest.guide-current-quest-description", player,
                        Map.of("%description%", active.description())));
            }
            lore.add(Component.empty());
            lore.add(messages.render("quest.guide-progress-header", player, Map.of()));
            lore.addAll(objectiveProgressLines(playerCache, messages, player, active));
        }
        if (clickable) {
            lore.add(Component.empty());
            lore.add(messages.render("quest.gui-lore-guide-hint", player, Map.of()));
        }

        return GuiItems.icon(Material.SPYGLASS, name, lore);
    }

    private static List<Component> objectiveProgressLines(PlayerQuestDataCache playerCache, MessagesConfig messages,
                                                          Player player, Quest quest) {
        PlayerQuestData data = playerCache.get(player.getUniqueId());
        QuestProgress progress = data == null ? null : data.progress(quest.id());
        List<Component> lines = new ArrayList<>();
        if (progress == null) {
            return lines;
        }
        List<QuestObjective> objectives = quest.objectives();
        Set<String> satisfiedGroups = ChoiceGroups.satisfiedGroups(quest, progress);
        for (int i = 0; i < objectives.size(); i++) {
            QuestObjective objective = objectives.get(i);
            int current = progress.objectiveProgress(i);
            boolean satisfied = current >= objective.amount();
            if (objective.choiceGroup() != null && !satisfied && satisfiedGroups.contains(objective.choiceGroup())) {
                continue;
            }
            lines.add(messages.render("quest.guide-progress-line", player, Map.of(
                    "%label%", objective.label(),
                    "%progress%", String.valueOf(current),
                    "%amount%", String.valueOf(objective.amount()))));
        }
        return lines;
    }
}
