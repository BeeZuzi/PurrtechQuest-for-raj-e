package eu.purrtech.purrtechQuest.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlotRangesTest {

    @Test
    void parsesSingleRange() {
        assertEquals(List.of(9, 10, 11, 12), SlotRanges.parse("9-12", 54, Set.of()));
    }

    @Test
    void parsesListOfRangesAndSingles() {
        assertEquals(List.of(10, 11, 19, 20, 28), SlotRanges.parse("10-11, 19-20, 28", 54, Set.of()));
    }

    @Test
    void skipsReservedOutOfRangeDuplicatesAndGarbage() {
        assertEquals(List.of(3, 5, 53), SlotRanges.parse("3-5, abc, 5, 53-60, -1", 54, Set.of(4)));
    }
}
