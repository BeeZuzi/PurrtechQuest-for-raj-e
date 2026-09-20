package eu.purrtech.purrtechQuest.gui;

import eu.purrtech.purrtechQuest.config.MessagesConfig;
import eu.purrtech.purrtechQuest.model.PlayerQuestData;
import eu.purrtech.purrtechQuest.model.Quest;
import eu.purrtech.purrtechQuest.model.QuestProgress;
import eu.purrtech.purrtechQuest.model.QuestStatus;
import eu.purrtech.purrtechQuest.player.PlayerQuestDataCache;
import eu.purrtech.purrtechQuest.service.QuestService;
import eu.purrtech.purrtechQuest.service.QuestStatusText;
import eu.purrtech.purrtechQuest.service.QuestTrackingService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A list of quests — every known quest in one category ({@link QuestCategoryGui}, the normal way in), or a
 * curated list an NPC gives ({@code npc.QuestGiverInteraction}, which has no category concept and passes
 * {@code onBack = null} for that reason, same as before categories existed). Sorted by
 * {@link Quest#DISPLAY_ORDER} (admin-assigned number first, alphabetical fallback/tie-break) regardless of
 * whatever order the caller's list happened to be in. 45 to a page, opening {@link QuestDetailGui} on
 * click. Completed ({@code TURNED_IN}) and not-yet-completed quests are listed together rather than split
 * behind a tab — the different book material per {@link QuestStatus}
 * ({@link QuestIcons#materialFor(QuestStatus)}) is what tells them apart at a glance.
 */
public final class QuestLogGui extends Gui {

    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int BACK_SLOT = 47;
    private static final int NEXT_SLOT = 53;

    private final QuestService questService;
    private final PlayerQuestDataCache playerCache;
    private final QuestTrackingService trackingService;
    private final MessagesConfig messages;
    private final Player player;
    private final List<Quest> quests;
    private final Runnable onBack;
    private int page;

    /** {@code onBack} may be {@code null} — no back button is shown then (the NPC-linked, category-less flow). */
    public QuestLogGui(QuestService questService, PlayerQuestDataCache playerCache, QuestTrackingService trackingService,
                        MessagesConfig messages, Player player, List<Quest> quests, Runnable onBack) {
        super(messages.render("quest.gui-log-title", player, Map.of()), 54);
        this.questService = questService;
        this.playerCache = playerCache;
        this.trackingService = trackingService;
        this.messages = messages;
        this.player = player;
        this.quests = quests.stream().sorted(Quest.DISPLAY_ORDER).toList();
        this.onBack = onBack;
        this.page = 0;
        render();
    }

    private void render() {
        clear();
        PlayerQuestData data = playerCache.get(player.getUniqueId());
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, quests.size());

        for (int i = from; i < to; i++) {
            Quest quest = quests.get(i);
            QuestProgress progress = data == null ? null : data.progress(quest.id());
            setItem(i - from, questIcon(quest, progress), event ->
                    new QuestDetailGui(questService, playerCache, trackingService, messages, player, quest.id(),
                            this::reopen).open(player));
        }

        for (int slot = 45; slot < 54; slot++) {
            setItem(slot, GuiItems.filler(), null);
        }
        if (page > 0) {
            setItem(PREV_SLOT, GuiItems.icon(Material.ARROW,
                    messages.render("quest.gui-button-prev-page", player, Map.of()),
                    List.of(messages.render("quest.gui-button-prev-page-hint", player, Map.of()))),
                    event -> {
                        page--;
                        render();
                    });
        }
        if (to < quests.size()) {
            setItem(NEXT_SLOT, GuiItems.icon(Material.ARROW,
                    messages.render("quest.gui-button-next-page", player, Map.of()),
                    List.of(messages.render("quest.gui-button-next-page-hint", player, Map.of()))),
                    event -> {
                        page++;
                        render();
                    });
        }
        if (onBack != null) {
            setItem(BACK_SLOT, GuiItems.icon(Material.ARROW, messages.render("quest.gui-button-back", player, Map.of()),
                            List.of(messages.render("quest.gui-button-back-hint", player, Map.of()))),
                    event -> onBack.run());
        }
    }

    private ItemStack questIcon(Quest quest, QuestProgress progress) {
        QuestStatus status = progress == null ? QuestStatus.NOT_ACCEPTED : progress.status();
        Material material = QuestIcons.materialFor(status);
        String statusText = messages.get(QuestStatusText.key(progress), player.locale().getLanguage());

        Component name = QuestIcons.displayNameComponent(quest.displayName(), NamedTextColor.GOLD);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(statusText, NamedTextColor.GRAY));
        List<Component> rewardLines = quest.rewards().stream().map(r -> QuestIcons.rewardLine(r, messages, player)).toList();
        if (!rewardLines.isEmpty()) {
            lore.add(Component.empty());
            lore.add(messages.render("quest.gui-lore-rewards-header", player, Map.of()));
            lore.addAll(rewardLines);
        }
        lore.add(Component.empty());
        lore.add(messages.render("quest.gui-lore-quest-hint", player, Map.of()));
        return GuiItems.icon(material, name, lore);
    }

    private void reopen() {
        new QuestLogGui(questService, playerCache, trackingService, messages, player, quests, onBack).open(player);
    }
}
