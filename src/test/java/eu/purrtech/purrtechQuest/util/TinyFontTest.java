package eu.purrtech.purrtechQuest.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TinyFontTest {

    @AfterEach
    void switchBackOn() {
        TinyFont.enabledWhen(() -> true);
    }

    @Test
    void whenSwitchedOffAdminTextIsLeftExactlyAsTyped() {
        TinyFont.enabledWhen(() -> false);
        assertEquals("Sběr Železa", TinyFont.convert("Sběr Železa"));
        Component text = Component.text("Sběr Železa", NamedTextColor.YELLOW);
        assertEquals(text, TinyFont.convert(text));
    }

    @Test
    void untinyTurnsTinyLettersBackIntoOrdinaryLowercaseOnes() {
        assertEquals("abcdefghijklmnopqrstuvwxyz", TinyFont.untiny("ᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ"));
        assertEquals("klikni pro zobrazení questů", TinyFont.untiny("ᴋʟɪᴋɴɪ ᴘʀᴏ ᴢᴏʙʀᴀᴢᴇɴí ǫᴜᴇsᴛů"));
        assertEquals("s", TinyFont.untiny("ꜱ"));
    }

    @Test
    void untinyThenConvertIsAStableRoundTrip() {
        String tiny = TinyFont.convert("Odevzdáno");
        assertEquals(tiny, TinyFont.convert(TinyFont.untiny(tiny)));
    }

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
