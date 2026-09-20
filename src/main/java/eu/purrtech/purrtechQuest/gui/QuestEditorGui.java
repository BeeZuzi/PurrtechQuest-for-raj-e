package eu.purrtech.purrtechQuest.gui;

import eu.purrtech.purrtechQuest.config.MessagesConfig;
import eu.purrtech.purrtechQuest.model.Quest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Editor hub for one {@link QuestDraft}: name/description/category via chat prompts, links to the
 * objectives/rewards/prerequisites sub-screens, boolean toggles, and Save/Cancel. Nothing is written to
 * disk until Save — closing the inventory or disconnecting mid-edit just discards the draft.
 */
public final class QuestEditorGui extends Gui {

    private static final int NAME_SLOT = 1;
    private static final int DESCRIPTION_SLOT = 3;
    private static final int CATEGORY_SLOT = 5;
    private static final int SORT_ORDER_SLOT = 7;
    private static final int OBJECTIVES_SLOT = 10;
    private static final int REWARDS_SLOT = 13;
    private static final int PREREQUISITES_SLOT = 16;
    private static final int REPEATABLE_SLOT = 19;
    private static final int COOLDOWN_SLOT = 21;
    private static final int AUTOSTART_SLOT = 23;
    private static final int AUTOTURNIN_SLOT = 25;
    private static final int NPC_SLOT = 27;
    private static final int PERMISSION_SLOT = 29;
    private static final int SAVE_SLOT = 31;
    private static final int CANCEL_SLOT = 33;

    private final QuestEditorContext context;
    private final Player player;
    private final QuestDraft draft;

    public QuestEditorGui(QuestEditorContext context, Player player, QuestDraft draft) {
        super(Component.text(draft.id(), NamedTextColor.GOLD), 36);
        this.context = context;
        this.player = player;
        this.draft = draft;
        render();
    }

    private void render() {
        clear();
        var messages = context.messages();
        var chatInput = context.chatInput();

        for (int slot = 0; slot < 36; slot++) {
            setItem(slot, GuiItems.filler(), null);
        }

        setItem(NAME_SLOT, GuiItems.icon(Material.NAME_TAG,
                        messages.render("quest.editor-button-name", player, Map.of("%value%", draft.displayName())),
                        List.of(messages.render("quest.editor-button-name-hint", player, Map.of()))),
                event -> chatInput.prompt(player, "quest.editor-prompt-name", value -> {
                    draft.displayName(value);
                    reopen();
                }, this::reopen));

        setItem(DESCRIPTION_SLOT, GuiItems.icon(Material.WRITTEN_BOOK,
                        messages.render("quest.editor-button-description", player, Map.of()),
                        List.of(Component.text(draft.description(), NamedTextColor.GRAY),
                                messages.render("quest.editor-button-description-hint", player, Map.of()))),
                event -> chatInput.prompt(player, "quest.editor-prompt-description", value -> {
                    draft.description(value);
                    reopen();
                }, this::reopen));

        setItem(CATEGORY_SLOT, GuiItems.icon(Material.ITEM_FRAME,
                        messages.render("quest.editor-button-category", player, Map.of("%value%", draft.category())),
                        List.of(messages.render("quest.editor-button-category-hint", player, Map.of()))),
                event -> chatInput.prompt(player, "quest.editor-prompt-category", value -> {
                    draft.category(value);
                    reopen();
                }, this::reopen));

        setItem(SORT_ORDER_SLOT, GuiItems.icon(Material.HOPPER,
                        messages.render("quest.editor-button-sort-order", player, Map.of("%value%", sortOrderValue())),
                        List.of(messages.render("quest.editor-button-sort-order-hint", player, Map.of()))),
                event -> chatInput.prompt(player, "quest.editor-prompt-sort-order", value -> {
                    String typed = value.trim();
                    if (typed.isEmpty() || typed.equalsIgnoreCase("none")) {
                        draft.sortOrder(null);
                        reopen();
                        return;
                    }
                    try {
                        draft.sortOrder(Integer.parseInt(typed));
                    } catch (NumberFormatException e) {
                        player.sendMessage(messages.render("quest.editor-invalid-number", player, Map.of()));
                    }
                    reopen();
                }, this::reopen));

        setItem(OBJECTIVES_SLOT, GuiItems.icon(Material.TARGET,
                        messages.render("quest.editor-button-objectives", player,
                                Map.of("%count%", String.valueOf(draft.objectives().size()))),
                        List.of(messages.render("quest.editor-button-objectives-hint", player, Map.of()))),
                event -> new QuestEditorObjectivesGui(context, player, draft).open(player));

        setItem(REWARDS_SLOT, GuiItems.icon(Material.GOLD_INGOT,
                        messages.render("quest.editor-button-rewards", player,
                                Map.of("%count%", String.valueOf(draft.totalRewardCount()))),
                        List.of(messages.render("quest.editor-button-rewards-hint", player, Map.of()))),
                event -> new QuestEditorRewardTiersGui(context, player, draft, this::reopen).open(player));

        setItem(PREREQUISITES_SLOT, GuiItems.icon(Material.BOOK,
                        messages.render("quest.editor-button-prerequisites", player,
                                Map.of("%count%", String.valueOf(draft.requiredQuests().size()))),
                        List.of(messages.render("quest.editor-button-prerequisites-hint", player, Map.of()))),
                event -> new QuestEditorPrerequisitesGui(context, player, draft).open(player));

        setItem(REPEATABLE_SLOT, boolToggle("quest.editor-button-repeatable", "quest.editor-button-repeatable-hint", draft.repeatable()),
                event -> {
                    draft.repeatable(!draft.repeatable());
                    reopen();
                });

        setItem(COOLDOWN_SLOT, GuiItems.icon(Material.CLOCK,
                        messages.render("quest.editor-button-cooldown", player,
                                Map.of("%value%", String.valueOf(draft.cooldownSeconds()))),
                        List.of(messages.render("quest.editor-button-cooldown-hint", player, Map.of()))),
                event -> chatInput.promptInt(player, "quest.editor-prompt-cooldown", value -> {
                    draft.cooldownSeconds(Math.max(0, value));
                    reopen();
                }, this::reopen));

        setItem(AUTOSTART_SLOT, boolToggle("quest.editor-button-autostart", "quest.editor-button-autostart-hint", draft.autoStart()),
                event -> {
                    draft.autoStart(!draft.autoStart());
                    reopen();
                });

        setItem(AUTOTURNIN_SLOT, boolToggle("quest.editor-button-autoturnin", "quest.editor-button-autoturnin-hint", draft.autoTurnIn()),
                event -> {
                    draft.autoTurnIn(!draft.autoTurnIn());
                    reopen();
                });

        setItem(NPC_SLOT, GuiItems.icon(Material.VILLAGER_SPAWN_EGG,
                        messages.render("quest.editor-button-npc", player, Map.of("%value%", npcValue())),
                        List.of(messages.render("quest.editor-npc-hint", player, Map.of()))),
                event -> player.sendMessage(messages.render("quest.editor-npc-hint", player, Map.of())));

        setItem(PERMISSION_SLOT, GuiItems.icon(Material.TRIPWIRE_HOOK,
                        messages.render("quest.editor-button-permission", player, Map.of("%value%", permissionValue())),
                        List.of(messages.render("quest.editor-button-permission-hint", player, Map.of()))),
                event -> chatInput.prompt(player, "quest.editor-prompt-permission", value -> {
                    String typed = value.trim();
                    draft.requiredPermission((typed.isEmpty() || typed.equalsIgnoreCase("none")) ? null : typed);
                    reopen();
                }, this::reopen));

        setItem(SAVE_SLOT, GuiItems.icon(Material.EMERALD_BLOCK, messages.render("quest.editor-button-save", player, Map.of()),
                        List.of(messages.render("quest.editor-button-save-hint", player, Map.of()))),
                event -> save());

        setItem(CANCEL_SLOT, GuiItems.icon(Material.BARRIER, messages.render("quest.editor-button-cancel", player, Map.of()),
                        List.of(messages.render("quest.editor-button-cancel-hint", player, Map.of()))),
                event -> player.closeInventory());
    }

    private String npcValue() {
        var giver = draft.questGiver();
        if (giver == null) {
            return context.messages().get("quest.npc-none", player.locale().getLanguage());
        }
        return giver.provider().name() + " #" + giver.npcId();
    }

    private String permissionValue() {
        return draft.requiredPermission() == null
                ? context.messages().get("quest.bool-no", player.locale().getLanguage())
                : draft.requiredPermission();
    }

    private String sortOrderValue() {
        return draft.sortOrder() == null
                ? context.messages().get("quest.bool-no", player.locale().getLanguage())
                : String.valueOf(draft.sortOrder());
    }

    private org.bukkit.inventory.ItemStack boolToggle(String labelKey, String hintKey, boolean value) {
        var messages = context.messages();
        String valueText = messages.get(value ? "quest.bool-yes" : "quest.bool-no", player.locale().getLanguage());
        Material material = value ? Material.LIME_DYE : Material.GRAY_DYE;
        return GuiItems.icon(material, messages.render(labelKey, player, Map.of("%value%", valueText)),
                List.of(messages.render(hintKey, player, Map.of())));
    }

    private void save() {
        MessagesConfig messages = context.messages();
        Quest quest;
        try {
            quest = draft.toQuest();
        } catch (IllegalArgumentException e) {
            player.sendMessage(messages.render("quest.editor-save-failed", player, Map.of("%reason%", String.valueOf(e.getMessage()))));
            return;
        }
        context.questRepository().save(quest);
        context.questService().reload();
        context.trackerRegistrationManager().refresh();
        context.questPermissionRegistrar().refresh();
        player.sendMessage(messages.render("quest.editor-saved", player, Map.of("%quest%", quest.displayName())));
        player.closeInventory();
    }

    private void reopen() {
        new QuestEditorGui(context, player, draft).open(player);
    }
}
