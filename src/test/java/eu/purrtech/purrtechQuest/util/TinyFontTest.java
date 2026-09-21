package eu.purrtech.purrtechQuest.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TinyFontTest {

    @Test
    void matchesTheLingoJamSmallCapsAlphabet() {
        assertEquals("ᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ", TinyFont.convert("abcdefghijklmnopqrstuvwxyz"));
        assertEquals("ᴋʟɪᴋɴɪ ᴘʀᴏ ᴢᴏʙʀᴀᴢᴇɴí ǫᴜᴇsᴛů", TinyFont.convert("Klikni pro zobrazení questů"));
    }

    @Test
    void lowercasesFirstAndLeavesDigitsAndPunctuationAlone() {
        assertEquals("ǫᴜᴇsᴛ 1: $10,000!", TinyFont.convert("QUEST 1: $10,000!"));
    }

    @Test
    void isIdempotent() {
        String once = TinyFont.convert("Sběr železa");
        assertEquals(once, TinyFont.convert(once));
    }

    @Test
    void keepsMiniMessageTagsPlaceholdersAndColorCodesWorking() {
        assertEquals("<red>ᴄᴇʟᴋᴇᴍ</red> %amount%", TinyFont.convert("<red>celkem</red> %amount%"));
        assertEquals("&eᴢʟᴀᴛᴏ &#FFD700ᴢʟᴀᴛᴏ", TinyFont.convert("&ezlato &#FFD700zlato"));
    }

    @Test
    void convertsTextInAParsedComponentAndKeepsItsColor() {
        Component converted = TinyFont.convert(Component.text("Zlato", NamedTextColor.YELLOW));
        assertEquals("ᴢʟᴀᴛᴏ", PlainTextComponentSerializer.plainText().serialize(converted));
        assertEquals(NamedTextColor.YELLOW, converted.color());
    }

    @Test
    void nullAndEmptyPassThrough() {
        assertEquals(null, TinyFont.convert((String) null));
        assertEquals("", TinyFont.convert(""));
    }
}
