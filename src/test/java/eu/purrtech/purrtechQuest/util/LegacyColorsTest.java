package eu.purrtech.purrtechQuest.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyColorsTest {

    @Test
    void convertsColorCodesAndResetsBeforeEachColor() {
        assertEquals("<reset><yellow>$10,000", LegacyColors.toMiniMessage("&e$10,000"));
        assertEquals("<bold><reset><red>x", LegacyColors.toMiniMessage("&l&cx"));
        assertEquals("<reset><red><bold>x", LegacyColors.toMiniMessage("&c&lx"));
    }

    @Test
    void convertsHexAndIsCaseInsensitive() {
        assertEquals("<reset><#FFD700>gold", LegacyColors.toMiniMessage("&#FFD700gold"));
        assertEquals("<reset><green>ok", LegacyColors.toMiniMessage("&Aok"));
    }

    @Test
    void leavesAnAmpersandThatIsNotACodeAlone() {
        assertEquals("Kováři & spol.", LegacyColors.toMiniMessage("Kováři & spol."));
        assertEquals("plain", LegacyColors.toMiniMessage("plain"));
    }
}
