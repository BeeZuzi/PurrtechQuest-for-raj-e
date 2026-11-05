package eu.purrtech.purrtechQuest.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategoryMenuControlSlotTest {

    @Test
    void aConfiguredSlotInsideTheMenuIsUsed() {
        assertEquals(31, CategoryMenu.controlSlot(31, 36, 22));
        assertEquals(0, CategoryMenu.controlSlot(0, 36, 22));
    }

    @Test
    void unsetOrOutOfRangeFallsBackToTheDefault() {
        assertEquals(22, CategoryMenu.controlSlot(-1, 27, 22));
        assertEquals(22, CategoryMenu.controlSlot(27, 27, 22));
        assertEquals(22, CategoryMenu.controlSlot(99, 27, 22));
    }
}
