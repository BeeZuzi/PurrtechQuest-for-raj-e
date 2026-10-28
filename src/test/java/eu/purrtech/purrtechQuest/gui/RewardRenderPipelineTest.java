package eu.purrtech.purrtechQuest.gui;

import eu.purrtech.purrtechQuest.util.DisplayText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simulates exactly what QuestLogGui.questIcon() -> QuestIcons.questListRewardLine() builds for one reward
 * line, then applies the same final italic-false pass GuiItems.icon() does to every lore line, to check
 * whether the reward's own color really does survive the full pipeline players actually see.
 */
class RewardRenderPipelineTest {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @Test
    void namedRewardLineKeepsItsColorAfterGuiItemsForcesNonItalic() {
        // Mirrors QuestIcons.questListRewardLine's dark-gray-dash prefix + appended named-reward component.
        Component prefix = Component.text(" - ", NamedTextColor.DARK_GRAY);
        Component name = DisplayText.component("&#FFF78B$500", NamedTextColor.DARK_GRAY);
        Component line = prefix.append(name);

        // Mirrors GuiItems.icon()'s lore.stream().map(l -> l.decoration(ITALIC, false)) pass.
        Component afterGuiItems = line.decoration(TextDecoration.ITALIC, false);

        assertEquals("- $500", colorScopedText(afterGuiItems));
        TextColor color = findColor(afterGuiItems, "$500", null);
        assertEquals(TextColor.color(0xFFF78B), color, "reward's own color should survive to the final lore line");

        assertTrue(isItalicFalse(afterGuiItems, "$500"),
                "the non-italic pass on the parent should still reach the appended child");
    }

    private static String colorScopedText(Component root) {
        StringBuilder sb = new StringBuilder();
        collect(root, sb);
        return sb.toString().trim();
    }

    private static void collect(Component node, StringBuilder sb) {
        if (node instanceof TextComponent t) {
            sb.append(t.content());
        }
        node.children().forEach(c -> collect(c, sb));
    }

    private static TextColor findColor(Component node, String text, TextColor inherited) {
        TextColor effective = node.color() != null ? node.color() : inherited;
        if (node instanceof TextComponent t && t.content().equals(text)) {
            return effective;
        }
        for (Component child : node.children()) {
            TextColor found = findColor(child, text, effective);
            if (found != null) return found;
        }
        return null;
    }

    private static Boolean isItalicFalse(Component node, String text) {
        TextDecoration.State state = node.decoration(TextDecoration.ITALIC);
        if (node instanceof TextComponent t && t.content().equals(text)) {
            return state != TextDecoration.State.TRUE;
        }
        for (Component child : node.children()) {
            Boolean found = isItalicFalse(child, text);
            if (found != null) return found;
        }
        return null;
    }
}
