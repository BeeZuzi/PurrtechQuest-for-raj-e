package eu.purrtech.purrtechQuest.gui;

import eu.purrtech.purrtechQuest.config.CategoriesConfig;
import eu.purrtech.purrtechQuest.config.GuideConfig;
import eu.purrtech.purrtechQuest.config.MessagesConfig;
import eu.purrtech.purrtechQuest.util.DisplayText;
import net.kyori.adventure.text.format.NamedTextColor;
import eu.purrtech.purrtechQuest.model.ChoiceGroups;
import eu.purrtech.purrtechQuest.model.PlayerQuestData;
import eu.purrtech.purrtechQuest.model.Quest;
import eu.purrtech.purrtechQuest.model.QuestObjective;
import eu.purrtech.purrtechQuest.model.QuestProgress;
import eu.purrtech.purrtechQuest.player.PlayerQuestDataCache;
import eu.purrtech.purrtechQuest.service.QuestService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The "guide" item shown in the quest menus: turned-in/total across the quests it covers, then a breakdown of
 * whichever of them the player currently has in progress (first match, same as
 * {@link QuestService#currentActiveQuest}) — its name, description, and live objective progress.
 * <p>
 * On the landing page ({@link QuestCategoryGui}, where clicking it opens the quest list) it covers the server
 * guide's own quests ({@link GuideConfig#quests}). Inside a category's quest list ({@link QuestLogGui}, where it
 * is display-only) it is not the guide at all: it is that category's own item (its name and look from
 * {@code categories:}) with the category's completed count and the quest in progress there.
 */
final class GuideIcon {

    private GuideIcon() {
    }

    /** The server guide: covers the quests {@code guide:} in the config names. {@code clickable} adds the "click to see every quest" hint line. */
    static ItemStack build(QuestService questService, PlayerQuestDataCache playerCache, MessagesConfig messages,
                           Player player, boolean clickable) {
        GuideConfig guide = GuideConfig.load();
        List<Quest> covered = guide.quests(CategoriesConfig.load(), questService.allQuests());
        return render(questService, playerCache, messages, player, guide, covered, null, clickable);
    }

    /**
     * The item at the top of a quest list: for an open category, that category's own item (its configured
     * name, or its id if it has none, and its look) with no guide intro line, covering exactly the quests the
     * list shows. A list that isn't a category (an NPC's) has none to show and keeps the plain guide item.
     */
    static ItemStack buildForCategory(QuestService questService, PlayerQuestDataCache playerCache,
                                      MessagesConfig messages, Player player, Collection<Quest> listed,
                                      CategoriesConfig.Category category) {
        return render(questService, playerCache, messages, player, GuideConfig.load(), listed, category, false);
    }

    private static ItemStack render(QuestService questService, PlayerQuestDataCache playerCache,
                                    MessagesConfig messages, Player player, GuideConfig guide,
                                    Collection<Quest> covered, CategoriesConfig.Category category,
                                    boolean clickable) {
        Component name;
        if (category != null) {
            String categoryName = category.name() == null || category.name().isBlank() ? category.id() : category.name();
            // Only the words of the configured name: the guide's own color and boldness (lang guide-category-name)
            // replace whatever colors the config gave it, so the item reads as one piece with its lore below.
            String plain = PlainTextComponentSerializer.plainText()
                    .serialize(DisplayText.component(categoryName, NamedTextColor.WHITE));
            name = messages.render("quest.guide-category-name", player,
                    Map.of("%name%", MiniMessage.miniMessage().escapeTags(plain)));
        } else if (guide.name() == null) {
            name = messages.render("quest.guide-name", player, Map.of());
        } else {
            name = DisplayText.component(guide.name(), NamedTextColor.WHITE);
        }
        List<Component> lore = new ArrayList<>();
        if (category == null) {
            lore.add(messages.render("quest.guide-intro", player, Map.of()));
        }

        QuestService.OverallSummary overall = questService.overallSummary(player, covered);
        lore.add(messages.render("quest.guide-completed", player, Map.of(
                "%done%", String.valueOf(overall.turnedIn()), "%total%", String.valueOf(overall.total()))));
        lore.add(Component.empty());
        lore.add(messages.render("quest.guide-current-quest-header", player, Map.of()));

        Quest active = questService.currentActiveQuest(player, covered).orElse(null);
        if (active == null) {
            lore.add(messages.render("quest.guide-no-active-quest", player, Map.of()));
        } else {
            lore.add(messages.render("quest.guide-current-quest-name", player, Map.of("%quest%", active.displayName())));
            for (String line : active.descriptionLines()) {
                lore.add(messages.render("quest.guide-current-quest-description", player,
                        Map.of("%description%", line)));
            }
            lore.add(Component.empty());
            lore.add(messages.render("quest.guide-progress-header", player, Map.of()));
            lore.addAll(objectiveProgressLines(playerCache, messages, player, active));
        }
        if (clickable) {
            lore.add(Component.empty());
            lore.add(messages.render("quest.gui-lore-guide-hint", player, Map.of()));
        }

        ItemStack base = category == null
                ? CategoryMenu.baseStack(guide.material(), guide.customModelData(), guide.glow(), guide.texture())
                : CategoryMenu.baseStack(category.material(), category.customModelData(), category.glow(),
                        category.texture());
        return GuiItems.icon(base, name, lore);
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
                    "%label%", messages.objectiveLabel(objective, player),
                    "%progress%", String.valueOf(current),
                    "%amount%", String.valueOf(objective.amount()))));
        }
        return lines;
    }
}
