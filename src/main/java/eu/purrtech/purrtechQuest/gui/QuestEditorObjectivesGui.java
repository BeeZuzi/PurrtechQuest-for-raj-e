package eu.purrtech.purrtechQuest.gui;

import eu.purrtech.purrtechQuest.model.QuestObjective;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lists a draft's objectives (left-click: opens {@link QuestEditorObjectiveEditGui} to edit every field in
 * place, right-click: remove).
 */
public final class QuestEditorObjectivesGui extends Gui {

    private static final int BACK_SLOT = 18;
    private static final int ADD_SLOT = 22;

    private final QuestEditorContext context;
    private final Player player;
    private final QuestDraft draft;

    public QuestEditorObjectivesGui(QuestEditorContext context, Player player, QuestDraft draft) {
        super(context.messages().render("quest.editor-button-objectives", player,
                Map.of("%count%", String.valueOf(draft.objectives().size()))), 27);
        this.context = context;
        this.player = player;
        this.draft = draft;
        render();
    }

    private void render() {
        clear();
        var messages = context.messages();
        List<QuestObjective> objectives = draft.objectives();
        for (int i = 0; i < objectives.size() && i < 18; i++) {
            QuestObjective objective = objectives.get(i);
            Component name = Component.text(objective.label(), NamedTextColor.GOLD);
            List<Component> lore = new ArrayList<>(List.of(
                    messages.render("quest.editor-objective-line", player, Map.of(
                            "%type%", objective.type().name(),
                            "%target%", objective.target(),
                            "%amount%", String.valueOf(objective.amount())))));
            if (objective.choiceGroup() != null) {
                lore.add(messages.render("quest.editor-objective-choice-group-line", player,
                        Map.of("%group%", objective.choiceGroup())));
            }
            lore.add(messages.render("quest.editor-objective-edit-hint", player, Map.of()));
            lore.add(messages.render("quest.editor-remove-hint", player, Map.of()));
            int index = i;
            setItem(i, GuiItems.icon(QuestIcons.materialFor(objective.type()), name, lore), event -> {
                if (event.isRightClick()) {
                    objectives.remove(index);
                    render();
                } else {
                    new QuestEditorObjectiveEditGui(context, player, draft, index).open(player);
                }
            });
        }

        setItem(BACK_SLOT, GuiItems.icon(Material.ARROW, messages.render("quest.gui-button-back", player, Map.of()),
                        List.of(messages.render("quest.gui-button-back-hint", player, Map.of()))),
                event -> new QuestEditorGui(context, player, draft).open(player));
        setItem(ADD_SLOT, GuiItems.icon(Material.LIME_DYE, messages.render("quest.editor-button-add", player, Map.of()),
                        List.of(messages.render("quest.editor-objectives-add-hint", player, Map.of()))),
                event -> new QuestEditorObjectiveTypeGui(context, player, draft).open(player));
    }
}
