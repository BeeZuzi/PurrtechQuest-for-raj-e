package eu.purrtech.purrtechQuest.gui;

import eu.purrtech.purrtechQuest.model.Quest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Toggle-list of every other quest as a prerequisite. Excludes the draft's own id (a quest can't require
 * itself) — cross-quest cycles (A requires B requires A) aren't checked here; {@code QuestService}'s
 * prerequisite check would just make both permanently unacceptable, which is a config mistake rather than
 * something worth building cycle-detection for in a first version.
 */
public final class QuestEditorPrerequisitesGui extends Gui {

    private static final int PAGE_SIZE = 45;
    private static final int BACK_SLOT = 45;
    private static final int PREV_SLOT = 46;
    private static final int NEXT_SLOT = 53;

    private final QuestEditorContext context;
    private final Player player;
    private final QuestDraft draft;
    private final List<Quest> candidates;
    private int page;

    public QuestEditorPrerequisitesGui(QuestEditorContext context, Player player, QuestDraft draft) {
        super(context.messages().render("quest.editor-button-prerequisites", player,
                Map.of("%count%", String.valueOf(draft.requiredQuests().size()))), 54);
        this.context = context;
        this.player = player;
        this.draft = draft;
        this.candidates = context.questService().allQuests().stream()
                .filter(quest -> !quest.id().equals(draft.id()))
                .sorted(Comparator.comparing(Quest::id))
                .toList();
        render();
    }

    private void render() {
        clear();
        var messages = context.messages();
        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, candidates.size());

        for (int i = from; i < to; i++) {
            Quest quest = candidates.get(i);
            boolean selected = draft.requiredQuests().contains(quest.id());
            Material material = selected ? Material.LIME_DYE : Material.GRAY_DYE;
            Component name = Component.text(quest.displayName(), selected ? NamedTextColor.GREEN : NamedTextColor.GRAY);
            List<Component> lore = List.of(Component.text(quest.id(), NamedTextColor.DARK_GRAY),
                    messages.render("quest.editor-prerequisite-toggle-hint", player, Map.of()));
            setItem(i - from, GuiItems.icon(material, name, lore), event -> {
                if (selected) {
                    draft.requiredQuests().remove(quest.id());
                } else {
                    draft.requiredQuests().add(quest.id());
                }
                render();
            });
        }

        for (int slot = 45; slot < 54; slot++) {
            setItem(slot, GuiItems.filler(), null);
        }
        setItem(BACK_SLOT, GuiItems.icon(Material.ARROW, messages.render("quest.gui-button-back", player, Map.of()),
                        List.of(messages.render("quest.gui-button-back-hint", player, Map.of()))),
                event -> new QuestEditorGui(context, player, draft).open(player));
        if (page > 0) {
            setItem(PREV_SLOT, GuiItems.icon(Material.ARROW,
                    messages.render("quest.gui-button-prev-page", player, Map.of()),
                    List.of(messages.render("quest.gui-button-prev-page-hint", player, Map.of()))),
                    event -> {
                        page--;
                        render();
                    });
        }
        if (to < candidates.size()) {
            setItem(NEXT_SLOT, GuiItems.icon(Material.ARROW,
                    messages.render("quest.gui-button-next-page", player, Map.of()),
                    List.of(messages.render("quest.gui-button-next-page-hint", player, Map.of()))),
                    event -> {
                        page++;
                        render();
                    });
        }
    }
}
