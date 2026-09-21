package eu.purrtech.purrtechQuest.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestIconsTest {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @Test
    void ampersandColorCodesColorTheName() {
        var component = QuestIcons.displayNameComponent("&e$10,000", NamedTextColor.DARK_GRAY);
        assertEquals("$10,000", PLAIN.serialize(component));  // digits/symbols have no tiny form
        assertEquals(NamedTextColor.YELLOW, colorOfText(component, "$10,000", null));
    }

    @Test
    void aNameWithoutColorGetsTheFallbackColor() {
        assertEquals(NamedTextColor.DARK_GRAY,
                QuestIcons.displayNameComponent("5 rajčecoinů", NamedTextColor.DARK_GRAY).color());
    }

    @Test
    void theNameIsShownInTheTinyFont() {
        assertEquals("ᴢʟᴀᴛᴏ", PLAIN.serialize(QuestIcons.displayNameComponent("&eZlato", NamedTextColor.DARK_GRAY)));
    }

    @Test
    void aMalformedNameStillRendersAsPlainText() {
        var component = QuestIcons.displayNameComponent("<nope", NamedTextColor.DARK_GRAY);
        assertEquals("<ɴᴏᴘᴇ", PLAIN.serialize(component));
    }

    /** The color a text leaf actually renders with: its own, else the nearest ancestor's. */
    private static TextColor colorOfText(Component node, String text, TextColor inherited) {
        TextColor effective = node.color() != null ? node.color() : inherited;
        if (node instanceof TextComponent t && t.content().equals(text)) {
            return effective;
        }
        for (Component child : node.children()) {
            TextColor found = colorOfText(child, text, effective);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
