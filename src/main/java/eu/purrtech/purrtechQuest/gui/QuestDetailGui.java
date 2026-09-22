package eu.purrtech.purrtechQuest.gui;

import eu.purrtech.purrtechQuest.config.MessagesConfig;
import eu.purrtech.purrtechQuest.model.ChoiceGroups;
import eu.purrtech.purrtechQuest.model.PlayerQuestData;
import eu.purrtech.purrtechQuest.model.Quest;
import eu.purrtech.purrtechQuest.model.QuestObjective;
import eu.purrtech.purrtechQuest.model.QuestProgress;
import eu.purrtech.purrtechQuest.model.QuestRewardTier;
import eu.purrtech.purrtechQuest.model.QuestStatus;
import eu.purrtech.purrtechQuest.player.PlayerQuestDataCache;
import eu.purrtech.purrtechQuest.service.QuestFeedback;
import eu.purrtech.purrtechQuest.service.QuestService;
import eu.purrtech.purrtechQuest.service.QuestTrackingService;
import eu.purrtech.purrtechQuest.util.DurationFormat;
import eu.purrtech.purrtechQuest.util.TinyFont;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A single quest's detail view, laid out as a ring around the middle row: the quest's book (name,
 * description, status, objectives) at the top-centre, rewards on the left, the primary action (accept,
 * abandon, turn in, or re-accept once its cooldown ends) in the centre, tracking on the right, and back/close
 * at the bottom. The primary action always lives in {@value #PRIMARY_ACTION_SLOT} regardless of which one it
 * currently is, since only one of them ever applies to a given status.
 */
public final class QuestDetailGui extends Gui {

    private static final int SIZE = 45;
    private static final int INFO_SLOT = 4;
    private static final int REWARDS_SLOT = 20;
    private static final int PRIMARY_ACTION_SLOT = 22;
    private static final int TRACK_SLOT = 24;
    private static final int BACK_SLOT = 39;
    private static final int CLOSE_SLOT = 41;

    private final QuestService questService;
    private final PlayerQuestDataCache playerCache;
    private final QuestTrackingService trackingService;
    private final MessagesConfig messages;
    private final Player player;
    private final String questId;
    private final Runnable onBack;

    public QuestDetailGui(QuestService questService, PlayerQuestDataCache playerCache,
                           QuestTrackingService trackingService, MessagesConfig messages,
                           Player player, String questId, Runnable onBack) {
        super(messages.render("quest.gui-detail-title", player,
                        Map.of("%quest%", questService.quest(questId).map(Quest::displayName).orElse(questId))),
                SIZE);
        this.questService = questService;
        this.playerCache = playerCache;
        this.trackingService = trackingService;
        this.messages = messages;
        this.player = player;
        this.questId = questId;
        this.onBack = onBack;
        render();
    }

    private void render() {
        clear();
        Quest quest = questService.quest(questId).orElse(null);
        if (quest == null) {
            setItem(BACK_SLOT, backButtonIcon(), event -> onBack.run());
            return;
        }

        PlayerQuestData data = playerCache.get(player.getUniqueId());
        QuestProgress progress = data == null ? null : data.progress(questId);
        QuestStatus status = progress == null ? QuestStatus.NOT_ACCEPTED : progress.status();

        setItem(INFO_SLOT, infoIcon(quest, progress, status), null);
        setItem(REWARDS_SLOT, rewardsIcon(quest), null);
        setItem(TRACK_SLOT, trackIcon(status), event -> onTrackClick(status));
        setItem(BACK_SLOT, backButtonIcon(), event -> onBack.run());
        setItem(CLOSE_SLOT, GuiItems.closeButton(messages.render("quest.gui-nav-close", player, Map.of()),
                        List.of(messages.render("quest.gui-nav-close-hint", player, Map.of()))),
                event -> player.closeInventory());

        renderActions(quest, progress, status);
    }

    private void renderActions(Quest quest, QuestProgress progress, QuestStatus status) {
        switch (status) {
            case NOT_ACCEPTED -> setItem(PRIMARY_ACTION_SLOT,
                    GuiItems.icon(Material.LIME_DYE, messages.render("quest.gui-button-accept", player, Map.of()),
                            List.of(messages.render("quest.gui-button-accept-hint", player, Map.of()))),
                    event -> {
                        QuestService.AcceptResult result = questService.acceptQuest(player, questId);
                        QuestFeedback.report(player, result, questId, questService, messages);
                        render();
                    });
            case IN_PROGRESS -> setItem(PRIMARY_ACTION_SLOT,
                    GuiItems.icon(Material.RED_DYE, messages.render("quest.gui-button-abandon", player, Map.of()),
                            List.of(messages.render("quest.gui-button-abandon-hint", player, Map.of()))),
                    event -> {
                        QuestService.AbandonResult result = questService.abandonQuest(player, questId);
                        QuestFeedback.report(player, result, messages);
                        render();
                    });
            case COMPLETED -> setItem(PRIMARY_ACTION_SLOT,
                    GuiItems.icon(Material.GOLD_INGOT, messages.render("quest.gui-button-turnin", player, Map.of()),
                            List.of(messages.render("quest.gui-button-turnin-hint", player, Map.of()))),
                    event -> {
                        QuestService.TurnInResult result = questService.turnIn(player, questId);
                        QuestFeedback.report(player, result, questId, messages);
                        render();
                    });
            case TURNED_IN -> renderReacceptOrCooldown(quest);
        }
    }

    private void renderReacceptOrCooldown(Quest quest) {
        if (!quest.repeatable()) {
            return;
        }
        Instant readyAt = questService.cooldownReadyAt(player, questId).orElse(null);
        if (readyAt == null || !Instant.now().isBefore(readyAt)) {
            setItem(PRIMARY_ACTION_SLOT,
                    GuiItems.icon(Material.LIME_DYE, messages.render("quest.gui-button-accept", player, Map.of()),
                            List.of(messages.render("quest.gui-button-accept-hint", player, Map.of()))),
                    event -> {
                        QuestService.AcceptResult result = questService.acceptQuest(player, questId);
                        QuestFeedback.report(player, result, questId, questService, messages);
                        render();
                    });
        } else {
            String time = DurationFormat.humanReadable(Duration.between(Instant.now(), readyAt));
            setItem(PRIMARY_ACTION_SLOT,
                    GuiItems.icon(Material.CLOCK, messages.render("quest.gui-button-accept", player, Map.of()),
                            List.of(messages.render("quest.gui-lore-cooldown", player, Map.of("%time%", time)))),
                    null);
        }
    }

    /**
     * The compass sits in every status, not just {@code IN_PROGRESS} — {@link #onTrackClick} is what actually
     * enforces that only a rozpracovaný (in-progress) quest can be tracked, with a chat message explaining why
     * a click did nothing for any other status.
     */
    private ItemStack trackIcon(QuestStatus status) {
        if (status != QuestStatus.IN_PROGRESS) {
            return GuiItems.icon(Material.COMPASS, messages.render("quest.gui-button-track", player, Map.of()),
                    List.of(messages.render("quest.gui-button-track-unavailable-hint", player, Map.of())));
        }
        boolean tracking = trackingService.isTracking(player, questId);
        String trackKey = tracking ? "quest.gui-button-untrack" : "quest.gui-button-track";
        String trackHintKey = tracking ? "quest.gui-button-untrack-hint" : "quest.gui-button-track-hint";
        return GuiItems.icon(Material.COMPASS, messages.render(trackKey, player, Map.of()),
                List.of(messages.render(trackHintKey, player, Map.of())));
    }

    private void onTrackClick(QuestStatus status) {
        if (status != QuestStatus.IN_PROGRESS) {
            player.sendMessage(messages.render("quest.track-requires-in-progress", player, Map.of()));
            return;
        }
        if (trackingService.isTracking(player, questId)) {
            trackingService.untrack(player);
        } else {
            trackingService.track(player, questId);
        }
        render();
    }

    private ItemStack backButtonIcon() {
        return GuiItems.icon(Material.IRON_DOOR, messages.render("quest.gui-nav-back", player, Map.of()),
                List.of(messages.render("quest.gui-nav-back-hint", player, Map.of())));
    }

    private ItemStack infoIcon(Quest quest, QuestProgress progress, QuestStatus status) {
        Material material = QuestIcons.materialFor(status);
        Component name = QuestIcons.displayNameComponent(quest.displayName(), NamedTextColor.GOLD);

        List<Component> lore = new ArrayList<>();
        if (!quest.description().isBlank()) {
            lore.add(Component.text(TinyFont.convert(quest.description()), NamedTextColor.GRAY));
            lore.add(Component.empty());
        }
        String statusText = messages.get(questService.statusKey(player, quest), player.locale().getLanguage());
        lore.add(messages.render("quest.gui-lore-status", player, Map.of("%status%", statusText)));
        lore.add(Component.empty());
        lore.add(messages.render("quest.gui-lore-objectives-header", player, Map.of()));

        List<QuestObjective> objectives = quest.objectives();
        Set<String> satisfiedGroups = progress == null ? Set.of() : ChoiceGroups.satisfiedGroups(quest, progress);
        for (int i = 0; i < objectives.size(); i++) {
            QuestObjective objective = objectives.get(i);
            int current = progress == null ? 0 : progress.objectiveProgress(i);
            boolean satisfied = current >= objective.amount();
            if (objective.choiceGroup() != null && !satisfied && satisfiedGroups.contains(objective.choiceGroup())) {
                lore.add(messages.render("quest.gui-lore-objective-locked", player, Map.of(
                        "%label%", objective.label())));
                continue;
            }
            Map<String, String> placeholders = Map.of(
                    "%label%", objective.label(),
                    "%progress%", String.valueOf(current),
                    "%amount%", String.valueOf(objective.amount()));
            String lineKey = objective.choiceGroup() != null
                    ? "quest.gui-lore-objective-choice-line"
                    : "quest.gui-lore-objective-line";
            lore.add(messages.render(lineKey, player, placeholders));
        }

        return GuiItems.icon(material, name, lore);
    }

    /**
     * Base rewards are always listed under their own header; each rank tier the player currently holds the
     * permission for gets its own header (using that tier's admin-set {@code displayName}, not the raw
     * permission node) plus its actual reward lines — a tier the player doesn't qualify for isn't shown at
     * all, so this never spoils rewards they can't get.
     */
    private ItemStack rewardsIcon(Quest quest) {
        Component name = messages.render("quest.gui-lore-rewards-header", player, Map.of());
        List<Component> lore = new ArrayList<>();

        List<Component> baseLines = quest.rewards().stream().map(r -> QuestIcons.rewardLine(r, messages, player)).toList();
        if (!baseLines.isEmpty()) {
            lore.add(messages.render("quest.gui-lore-reward-section-base", player, Map.of()));
            lore.addAll(baseLines);
        }

        for (QuestRewardTier tier : quest.rewardTiers()) {
            if (!player.hasPermission(tier.permission())) {
                continue;
            }
            List<Component> tierLines = tier.rewards().stream().map(r -> QuestIcons.rewardLine(r, messages, player)).toList();
            if (tierLines.isEmpty()) {
                continue;
            }
            if (!lore.isEmpty()) {
                lore.add(Component.empty());
            }
            lore.add(messages.render("quest.gui-lore-reward-section-tier", player, Map.of("%tier%", tier.displayName())));
            lore.addAll(tierLines);
        }

        if (lore.isEmpty()) {
            lore.add(messages.render("quest.gui-lore-reward-bonus", player, Map.of()));
        }
        return GuiItems.icon(Material.CHEST, name, lore);
    }
}
