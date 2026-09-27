package eu.purrtech.purrtechQuest.integration;

import eu.purrtech.purrtechQuest.config.MessagesConfig;
import eu.purrtech.purrtechQuest.model.PlayerQuestData;
import eu.purrtech.purrtechQuest.model.Quest;
import eu.purrtech.purrtechQuest.model.QuestObjective;
import eu.purrtech.purrtechQuest.model.QuestProgress;
import eu.purrtech.purrtechQuest.model.QuestStatus;
import eu.purrtech.purrtechQuest.player.PlayerQuestDataCache;
import eu.purrtech.purrtechQuest.service.QuestService;
import eu.purrtech.purrtechQuest.util.TinyFont;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import eu.purrtech.purrtechQuest.service.QuestStatusText;
import eu.purrtech.purrtechQuest.service.QuestTrackingService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * {@code %purrtechquest_...%} placeholders. Only instantiated (and its PlaceholderAPI-typed fields
 * touched) when PlaceholderAPI is confirmed present — see {@code PurrtechQuest.onEnable}. Only supports
 * online players: a player's quest progress lives in {@link PlayerQuestDataCache}, which only holds data
 * for the current session, and there's no way to serve an offline player's data without a blocking DB read
 * on whatever thread PlaceholderAPI calls this from.
 * <p>
 * Placeholders:
 * <ul>
 *   <li>{@code %purrtechquest_active_count%} — quests currently in progress</li>
 *   <li>{@code %purrtechquest_completed_count%} — distinct quests turned in at least once</li>
 *   <li>{@code %purrtechquest_completed_total%} — total turn-ins, counting every repeat of a repeatable quest</li>
 *   <li>{@code %purrtechquest_total_count%} — quests defined on the server</li>
 *   <li>{@code %purrtechquest_status_<questId>%} — that quest's status, localized to the player's client</li>
 *   <li>{@code %purrtechquest_tracked_name%} — display name of the player's tracked quest (empty if none)</li>
 *   <li>{@code %purrtechquest_tracked_progress%} — {@code current/amount} for its first incomplete objective</li>
 * </ul>
 */
public final class PurrtechQuestPlaceholderExpansion extends PlaceholderExpansion {

    private final QuestService questService;
    private final PlayerQuestDataCache playerCache;
    private final QuestTrackingService trackingService;
    private final MessagesConfig messages;

    public PurrtechQuestPlaceholderExpansion(QuestService questService, PlayerQuestDataCache playerCache,
                                              QuestTrackingService trackingService, MessagesConfig messages) {
        this.questService = questService;
        this.playerCache = playerCache;
        this.trackingService = trackingService;
        this.messages = messages;
    }

    @Override
    public String getIdentifier() {
        return "purrtechquest";
    }

    @Override
    public String getAuthor() {
        return "PurrtechQuest";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return "";
        }
        Player player = offlinePlayer.getPlayer();
        if (player == null) {
            return "";
        }
        PlayerQuestData data = playerCache.get(player.getUniqueId());

        if (params.equalsIgnoreCase("active_count")) {
            return String.valueOf(countByStatus(data, QuestStatus.IN_PROGRESS));
        }
        if (params.equalsIgnoreCase("completed_count")) {
            return String.valueOf(countCompletedQuests(data));
        }
        if (params.equalsIgnoreCase("completed_total")) {
            return String.valueOf(totalCompletions(data));
        }
        if (params.equalsIgnoreCase("total_count")) {
            return String.valueOf(questService.allQuests().size());
        }
        if (params.equalsIgnoreCase("tracked_name")) {
            return trackingService.trackedQuestId(player)
                    .flatMap(questService::quest)
                    .map(Quest::displayName)
                    .map(TinyFont::convert)
                    .orElse("");
        }
        if (params.equalsIgnoreCase("tracked_progress")) {
            return trackedProgressText(player, data);
        }
        if (params.toLowerCase(Locale.ROOT).startsWith("status_")) {
            String questId = params.substring("status_".length());
            return statusText(data, questId, player);
        }
        return null;
    }

    private static int countByStatus(PlayerQuestData data, QuestStatus status) {
        if (data == null) {
            return 0;
        }
        int count = 0;
        for (QuestProgress progress : data.states().values()) {
            if (progress.status() == status) {
                count++;
            }
        }
        return count;
    }

    /**
     * A repeatable quest that's been re-accepted is back to IN_PROGRESS but keeps its timesCompleted, so
     * status alone would drop it from the count; TURNED_IN still counts for rows that predate the counter.
     */
    private static int countCompletedQuests(PlayerQuestData data) {
        if (data == null) {
            return 0;
        }
        int count = 0;
        for (QuestProgress progress : data.states().values()) {
            if (progress.timesCompleted() > 0 || progress.status() == QuestStatus.TURNED_IN) {
                count++;
            }
        }
        return count;
    }

    private static int totalCompletions(PlayerQuestData data) {
        if (data == null) {
            return 0;
        }
        int total = 0;
        for (QuestProgress progress : data.states().values()) {
            // Same fallback as countCompletedQuests: a TURNED_IN row is at least one completion.
            total += Math.max(progress.timesCompleted(), progress.status() == QuestStatus.TURNED_IN ? 1 : 0);
        }
        return total;
    }

    private String statusText(PlayerQuestData data, String questId, Player player) {
        Quest quest = questService.quest(questId).orElse(null);
        QuestProgress progress = data == null ? null : data.progress(questId);
        String key = quest == null ? QuestStatusText.key(progress) : questService.statusKey(player, quest);
        // Status texts carry MiniMessage color tags for the GUI; a placeholder consumer wants plain text.
        return PlainTextComponentSerializer.plainText().serialize(
                MiniMessage.miniMessage().deserialize(messages.get(key, player.locale().getLanguage())));
    }

    private String trackedProgressText(Player player, PlayerQuestData data) {
        Optional<String> questId = trackingService.trackedQuestId(player);
        if (questId.isEmpty() || data == null) {
            return "";
        }
        Quest quest = questService.quest(questId.get()).orElse(null);
        QuestProgress progress = data.progress(questId.get());
        if (quest == null || progress == null) {
            return "";
        }
        List<QuestObjective> objectives = quest.objectives();
        for (int i = 0; i < objectives.size(); i++) {
            if (progress.objectiveProgress(i) < objectives.get(i).amount()) {
                return progress.objectiveProgress(i) + "/" + objectives.get(i).amount();
            }
        }
        return "done";
    }
}
