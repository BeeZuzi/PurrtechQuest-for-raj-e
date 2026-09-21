package eu.purrtech.purrtechQuest.gui;

import eu.purrtech.purrtechQuest.config.MessagesConfig;
import eu.purrtech.purrtechQuest.player.PlayerQuestDataCache;
import eu.purrtech.purrtechQuest.service.QuestService;
import eu.purrtech.purrtechQuest.service.QuestTrackingService;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * {@code /quest gui}'s landing page: a compact 3-row menu holding just the "guide" item at slot
 * {@value #GUIDE_SLOT} — overall turned-in/total across every quest, then a breakdown of whichever quest
 * the player currently has in progress (name, description, live objective progress) — plus a close button.
 * Clicking the guide opens {@link QuestLogGui} with every quest.
 */
public final class QuestCategoryGui extends Gui {

    private static final int SIZE = 27;
    private static final int GUIDE_SLOT = 3;
    private static final int CLOSE_SLOT = 22;

    private final QuestService questService;
    private final PlayerQuestDataCache playerCache;
    private final QuestTrackingService trackingService;
    private final MessagesConfig messages;
    private final Player player;

    public QuestCategoryGui(QuestService questService, PlayerQuestDataCache playerCache, QuestTrackingService trackingService,
                             MessagesConfig messages, Player player) {
        super(messages.render("quest.gui-category-title", player, Map.of()), SIZE);
        this.questService = questService;
        this.playerCache = playerCache;
        this.trackingService = trackingService;
        this.messages = messages;
        this.player = player;
        render();
    }

    private void render() {
        clear();
        setItem(GUIDE_SLOT, GuideIcon.build(questService, playerCache, messages, player, true), event ->
                new QuestLogGui(questService, playerCache, trackingService, messages, player,
                        List.copyOf(questService.allQuests()), this::reopen).open(player));
        setItem(CLOSE_SLOT, GuiItems.icon(Material.BARRIER, messages.render("quest.gui-button-close", player, Map.of()),
                        List.of(messages.render("quest.gui-button-close-hint", player, Map.of()))),
                event -> player.closeInventory());
    }

    private void reopen() {
        new QuestCategoryGui(questService, playerCache, trackingService, messages, player).open(player);
    }
}
