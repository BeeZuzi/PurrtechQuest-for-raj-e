package eu.purrtech.purrtechQuest.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoreWrapTest {

    private static List<String> plain(List<Component> lines) {
        return lines.stream().map(c -> PlainTextComponentSerializer.plainText().serialize(c)).toList();
    }

    @Test
    void breaksBetweenWordsNeverInsideOne() {
        List<Component> out = LoreWrap.wrapLine(Component.text("Pokacej 100 dubovych klad a vyrob prkna", NamedTextColor.GRAY), 20);
        assertEquals(List.of("Pokacej 100 dubovych", "klad a vyrob prkna"), plain(out));
        assertEquals(NamedTextColor.GRAY, out.get(1).children().get(0).color());
    }

    @Test
    void shortLineAndBlankLineUntouched() {
        assertEquals(List.of("abc"), plain(LoreWrap.wrapLine(Component.text("abc"), 20)));
        assertEquals(List.of(""), plain(LoreWrap.wrapLine(Component.empty(), 20)));
    }

    @Test
    void bulletIndentIsRepeatedAndLongWordStaysWhole() {
        assertEquals(List.of(" - aaa bbb", "   ccc", "   dddddddddddd"),
                plain(LoreWrap.wrapLine(Component.text(" - aaa bbb ccc dddddddddddd"), 10)));
    }
}
