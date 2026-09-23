package eu.purrtech.purrtechQuest.util;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisplayTextTest {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @Test
    void aColoredNameKeepsItsOwnColorInsteadOfTheFallback() {
        var component = DisplayText.component("&e$10,000", NamedTextColor.DARK_GRAY);
        assertEquals("$10,000", PLAIN.serialize(component));
        assertEquals(NamedTextColor.YELLOW, component.color());
    }

    @Test
    void aPlainNameGetsTheFallbackColor() {
        assertEquals(NamedTextColor.DARK_GRAY, DisplayText.component("5 rajčecoinů", NamedTextColor.DARK_GRAY).color());
    }

    @Test
    void hexColorsWorkToo() {
        TextColor gold = TextColor.color(0xFCD303);
        assertEquals(gold, DisplayText.component("&#FCD303Odměna", NamedTextColor.WHITE).color());
    }

    @Test
    void aMalformedValueStillRendersAsPlainText() {
        assertEquals("<ɴᴏᴘᴇ", PLAIN.serialize(DisplayText.component("<nope", NamedTextColor.WHITE)));
    }

    @Test
    void theTextIsShownInTheTinyFont() {
        assertEquals("ᴢʟᴀᴛᴏ", PLAIN.serialize(DisplayText.component("Zlato", NamedTextColor.WHITE)));
    }
}
