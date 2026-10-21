package eu.purrtech.purrtechQuest.gui;

import eu.purrtech.purrtechQuest.config.GuideConfig;
import eu.purrtech.purrtechQuest.config.MessagesConfig;
import eu.purrtech.purrtechQuest.model.PlayerQuestData;
import eu.purrtech.purrtechQuest.model.Quest;
import eu.purrtech.purrtechQuest.model.QuestProgress;
import eu.purrtech.purrtechQuest.model.QuestStatus;
import eu.purrtech.purrtechQuest.player.PlayerQuestDataCache;
import eu.purrtech.purrtechQuest.service.QuestService;
import eu.purrtech.purrtechQuest.service.QuestTrackingService;
import eu.purrtech.purrtechQuest.util.SlotRanges;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A list of quests — every known quest in one category ({@link QuestCategoryGui}, the normal way in), or a
 * curated list an NPC gives ({@code npc.QuestGiverInteraction}, which has no category concept and passes
 * {@code onBack = null} for that reason, same as before categories existed). Sorted by
 * {@link Quest#DISPLAY_ORDER} (admin-assigned number first, alphabetical fallback/tie-break) regardless of
 * whatever order the caller's list happened to be in. The quests sit in the slots set by
 * {@code quest-list.slots} in config.yml (default {@value #DEFAULT_SLOTS}, the area the menu's custom
 * background leaves for them), one page's worth at a time, opening {@link QuestDetailGui} on click. The guide ({@link GuideIcon}) sits in the top-centre slot
 * {@value #GUIDE_SLOT}, display-only. Completed ({@code TURNED_IN}) and not-yet-completed quests are listed together rather than split
 * behind a tab — the different book material per {@link QuestStatus}
 * ({@link QuestIcons#materialFor(QuestStatus)}) is what tells them apart at a glance.
 */
public final class QuestLogGui extends Gui {

    private static final int SIZE = 54;
    private static final int GUIDE_SLOT = 4;
    private static final int PREV_SLOT = 45;
    private static final int BACK_SLOT = 48;
    private static final int CLOSE_SLOT = 50;
    private static final int NEXT_SLOT = 53;
    private static final String DEFAULT_SLOTS = "9-35";

    private final QuestService questService;
    private final PlayerQuestDataCache playerCache;
    private final QuestTrackingService trackingService;
    private final MessagesConfig messages;
    private final Player player;
    private final List<Quest> quests;
    private final Runnable onBack;
    private final List<Integer> questSlots;
    private int page;

    /** {@code onBack} may be {@code null} — no back button is shown then (the NPC-linked, category-less flow). */
    public QuestLogGui(QuestService questService, PlayerQuestDataCache playerCache, QuestTrackingService trackingService,
                        MessagesConfig messages, Player player, List<Quest> quests, Runnable onBack) {
        super(messages.render("quest.gui-log-title", player, Map.of()), SIZE);
        this.questService = questService;
        this.playerCache = playerCache;
        this.trackingService = trackingService;
        this.messages = messages;
        this.player = player;
        this.quests = quests.stream().sorted(Quest.DISPLAY_ORDER).toList();
        this.onBack = onBack;
        this.questSlots = questSlots();
        this.page = 0;
        render();
    }

    private void render() {
        clear();
        PlayerQuestData data = playerCache.get(player.getUniqueId());
        int pageSize = questSlots.size();
        int from = page * pageSize;
        int to = Math.min(from + pageSize, quests.size());

        for (int i = from; i < to; i++) {
            Quest quest = quests.get(i);
            QuestProgress progress = data == null ? null : data.progress(quest.id());
            setItem(questSlots.get(i - from), questIcon(quest, progress, i + 1), event ->
                    new QuestDetailGui(questService, playerCache, trackingService, messages, player, quest.id(),
                            this::reopen).open(player));
        }

        int configured = GuideConfig.load().listSlot();
        if (configured >= 0) {
            int guideSlot = CategoryMenu.validSlot(configured, 54, blockedSlots(), GUIDE_SLOT);
            setItem(guideSlot, GuideIcon.build(questService, playerCache, messages, player, false), null);
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
            setItem(BACK_SLOT, GuiItems.icon(Material.IRON_DOOR, messages.render("quest.gui-nav-back", player, Map.of()),
                            List.of(messages.render("quest.gui-nav-back-hint", player, Map.of()))),
                    event -> onBack.run());
        }
        setItem(CLOSE_SLOT, GuiItems.closeButton(messages.render("quest.gui-nav-close", player, Map.of()),
                        List.of(messages.render("quest.gui-nav-close-hint", player, Map.of()))),
                event -> player.closeInventory());
    }

    /**
     * Read on every open, so a {@code /questadmin reload} applies right away. The guide and navigation slots
     * are never handed out to quests; a value that leaves no usable slot falls back to the default.
     */
    private static List<Integer> questSlots() {
        Set<Integer> reserved = Set.of(GUIDE_SLOT, PREV_SLOT, BACK_SLOT, CLOSE_SLOT, NEXT_SLOT);
        String spec = JavaPlugin.getProvidingPlugin(QuestLogGui.class).getConfig()
                .getString("quest-list.slots", DEFAULT_SLOTS);
        List<Integer> slots = SlotRanges.parse(spec, SIZE, reserved);
        return slots.isEmpty() ? SlotRanges.parse(DEFAULT_SLOTS, SIZE, reserved) : slots;
    }

    /**
     * The quest's book in the list. Layout: bold accent-colored name (the quest's own {@code display-name}
     * styling wins when it has any), its number in the list, status, rewards (one line per reward, by the
     * reward's own name) and the click hint.
     */
    private ItemStack questIcon(Quest quest, QuestProgress progress, int number) {
        QuestStatus status = progress == null ? QuestStatus.NOT_ACCEPTED : progress.status();
        Material material = QuestIcons.materialFor(status);
        String statusText = messages.get(questService.statusKey(player, quest), player.locale().getLanguage());

        Component name = QuestIcons.displayNameComponent(quest.displayName(), QuestIcons.ACCENT)
                .decorationIfAbsent(TextDecoration.BOLD, TextDecoration.State.TRUE);

        List<Component> lore = new ArrayList<>();
        lore.add(messages.render("quest.gui-lore-quest-number", player, Map.of("%number%", String.valueOf(number))));
        lore.add(Component.empty());
        lore.add(messages.render("quest.gui-lore-quest-status", player, Map.of("%status%", statusText)));
        if (!quest.rewards().isEmpty()) {
            lore.add(Component.empty());
            lore.add(messages.render("quest.gui-lore-quest-reward-header", player, Map.of()));
            for (var reward : quest.rewards()) {
                lore.add(QuestIcons.questListRewardLine(reward, messages, player));
            }
        }
        lore.add(Component.empty());
        lore.add(messages.render("quest.gui-lore-quest-hint", player, Map.of()));
        return GuiItems.icon(material, name, lore);
    }

    /** Where the guide can't go: on a quest slot or a navigation button. */
    private static Set<Integer> blockedSlots() {
        Set<Integer> blocked = new HashSet<>(Set.of(PREV_SLOT, BACK_SLOT, CLOSE_SLOT, NEXT_SLOT));
        for (int slot = FIRST_SLOT; slot <= LAST_SLOT; slot++) {
            blocked.add(slot);
        }
        return blocked;
    }

    private void reopen() {
        new QuestLogGui(questService, playerCache, trackingService, messages, player, quests, onBack).open(player);
    }
}
