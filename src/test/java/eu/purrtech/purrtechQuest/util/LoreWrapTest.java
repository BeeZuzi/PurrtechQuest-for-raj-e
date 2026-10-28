package eu.purrtech.purrtechQuest.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoreWrapTest {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @BeforeEach
    void width20() {
        LoreWrap.widthWhen(() -> 20);
    }

    @AfterEach
    void defaultWidth() {
        LoreWrap.widthWhen(() -> LoreWrap.DEFAULT_WIDTH);
    }

    private static List<String> plain(List<Component> lines) {
        return lines.stream().map(PLAIN::serialize).toList();
    }

    @Test
    void aLineThatFitsIsReturnedUntouched() {
        Component line = Component.text("krátký text", NamedTextColor.GRAY);
        List<Component> wrapped = LoreWrap.wrap(line);
        assertEquals(1, wrapped.size());
        assertSame(line, wrapped.get(0));
    }

    @Test
    void aLongLineBreaksAtSpacesAndNeverExceedsTheWidth() {
        List<String> lines = plain(LoreWrap.wrap(Component.text(
                "Vytěž 10 kusů železné rudy pro kováře ve vesnici a přines mu je zpátky")));
        assertTrue(lines.size() > 1);
        for (String line : lines) {
            assertTrue(line.length() <= 20, "too wide: '" + line + "'");
            assertFalse(line.startsWith(" ") || line.endsWith(" "), "stray space: '" + line + "'");
        }
        assertEquals("Vytěž 10 kusů železné rudy pro kováře ve vesnici a přines mu je zpátky",
                String.join(" ", lines));
    }

    @Test
    void theContinuationIndentCountsTowardsTheWidth() {
        List<String> lines = plain(LoreWrap.wrap(
                Component.text(" - zabij sto zombíků a deset kostlivců v jeskyni"), "   "));
        assertTrue(lines.size() > 1);
        assertTrue(lines.get(0).length() <= 20);
        for (String line : lines.subList(1, lines.size())) {
            assertTrue(line.startsWith("   "), "missing indent: '" + line + "'");
            assertTrue(line.length() <= 20, "too wide: '" + line + "'");
        }
    }

    @Test
    void everyPartKeepsItsOwnColorOnWhicheverLineItLandsOn() {
        Component line = Component.text()
                .append(Component.text("šedý úvod který je dost dlouhý ", NamedTextColor.GRAY))
                .append(Component.text("žlutá odměna která pokračuje dál", TextColor.color(0xFFF78B)))
                .build();
        List<Component> wrapped = LoreWrap.wrap(line);
        assertTrue(wrapped.size() >= 3);
        assertEquals(NamedTextColor.GRAY, colorOf(wrapped.get(0), "šedý"));
        assertEquals(TextColor.color(0xFFF78B), colorOf(wrapped.get(wrapped.size() - 1), "dál"));
    }

    @Test
    void aColorOnTheParentReachesTheWrappedChildren() {
        Component line = Component.text("barva zděděná od rodiče", NamedTextColor.RED)
                .append(Component.text(" a její dlouhé pokračování bez vlastní barvy"));
        List<Component> wrapped = LoreWrap.wrap(line);
        assertEquals(NamedTextColor.RED, colorOf(wrapped.get(wrapped.size() - 1), "barvy"));
        assertEquals(NamedTextColor.RED, colorOf(wrapped.get(wrapped.size() - 2), "pokračování"));
    }

    @Test
    void aSingleWordLongerThanALineIsSplitInTheMiddle() {
        List<String> lines = plain(LoreWrap.wrap(Component.text("a" + "b".repeat(45) + "c")));
        assertEquals(List.of("a" + "b".repeat(19), "b".repeat(20), "b".repeat(6) + "c"), lines);
    }

    @Test
    void aNewlineForcesABreak() {
        assertEquals(List.of("první", "druhý"), plain(LoreWrap.wrap(Component.text("první\ndruhý"))));
    }

    @Test
    void widthZeroTurnsWrappingOff() {
        LoreWrap.widthWhen(() -> 0);
        Component line = Component.text("x".repeat(500));
        assertEquals(1, LoreWrap.wrap(line).size());
    }

    private static TextColor colorOf(Component node, String word) {
        return find(node, word, null);
    }

    private static TextColor find(Component node, String word, TextColor inherited) {
        TextColor effective = node.color() != null ? node.color() : inherited;
        if (node instanceof TextComponent t && t.content().contains(word)) {
            return effective;
        }
        for (Component child : node.children()) {
            TextColor found = find(child, word, effective);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
