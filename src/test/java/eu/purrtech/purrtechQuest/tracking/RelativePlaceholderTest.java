package eu.purrtech.purrtechQuest.tracking;

import eu.purrtech.purrtechQuest.model.ObjectiveType;
import eu.purrtech.purrtechQuest.model.QuestObjective;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Relative PLACEHOLDER_CHECK: progress = rise since the quest was accepted, capped at the amount. */
class RelativePlaceholderTest {

    @Test
    void playerAlreadyAt1000StartsAtZeroAndNeeds1010ForTen() {
        assertEquals(0, PlaceholderMatcher.relativeProgress(1000, 1000, 10));
        assertEquals(9, PlaceholderMatcher.relativeProgress(1009, 1000, 10));
        assertEquals(10, PlaceholderMatcher.relativeProgress(1010, 1000, 10));
    }

    @Test
    void progressIsCappedAndNeverNegative() {
        assertEquals(10, PlaceholderMatcher.relativeProgress(5000, 1000, 10));
        assertEquals(0, PlaceholderMatcher.relativeProgress(3, 1000, 10));
    }

    @Test
    void relativeObjectiveKeepsItsAmountInsteadOfDerivingItFromTheValue() {
        QuestObjective relative = new QuestObjective(ObjectiveType.PLACEHOLDER_CHECK, "%statistic_animals_bred%", 500,
                Map.of("mode", "relative"));
        QuestObjective absolute = new QuestObjective(ObjectiveType.PLACEHOLDER_CHECK, "%statistic_animals_bred%", 1,
                Map.of("operator", ">=", "value", "500"));

        assertTrue(relative.relativePlaceholder());
        assertEquals(500, relative.amount());
        assertFalse(absolute.relativePlaceholder());
        assertEquals(500, absolute.amount());
    }
}
