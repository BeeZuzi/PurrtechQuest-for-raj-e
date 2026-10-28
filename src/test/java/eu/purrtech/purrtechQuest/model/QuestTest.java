package eu.purrtech.purrtechQuest.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers {@link Quest#DISPLAY_ORDER}: quests with an explicit {@code sortOrder} come first (lowest number
 * first), quests with none set come after as their own group, and either group breaks ties alphabetically
 * by display name.
 */
class QuestTest {

    @Test
    void explicitlyNumberedQuestsSortBeforeUnnumberedOnesByTheirNumber() {
        Quest first = quest("z_quest", "Z Quest", 1);
        Quest second = quest("a_quest", "A Quest", 2);
        Quest unnumbered = quest("m_quest", "M Quest", null);

        List<Quest> sorted = List.of(unnumbered, second, first).stream().sorted(Quest.DISPLAY_ORDER).toList();

        assertEquals(List.of(first, second, unnumbered), sorted);
    }

    @Test
    void sameNumberBreaksTiesAlphabeticallyByDisplayName() {
        Quest zebra = quest("zebra_id", "Zebra", 5);
        Quest apple = quest("apple_id", "Apple", 5);

        List<Quest> sorted = List.of(zebra, apple).stream().sorted(Quest.DISPLAY_ORDER).toList();

        assertEquals(List.of(apple, zebra), sorted);
    }

    @Test
    void noNumberAtAllFallsBackToPlainAlphabeticalOrder() {
        Quest zebra = quest("zebra_id", "Zebra", null);
        Quest apple = quest("apple_id", "Apple", null);

        List<Quest> sorted = List.of(zebra, apple).stream().sorted(Quest.DISPLAY_ORDER).toList();

        assertEquals(List.of(apple, zebra), sorted);
    }

    @Test
    void descriptionSplitsOnPipeAndTrimsAroundIt() {
        assertEquals(List.of("Vytěž železo", "a odnes ho kováři"),
                Quest.descriptionLines("Vytěž železo | a odnes ho kováři"));
    }

    @Test
    void doublePipeKeepsABlankLine() {
        assertEquals(List.of("a", "", "b"), Quest.descriptionLines("a||b"));
    }

    @Test
    void blankDescriptionHasNoLines() {
        assertEquals(List.of(), Quest.descriptionLines(""));
        assertEquals(List.of(), Quest.descriptionLines(null));
    }

    @Test
    void countingPlaceholderCheckTakesItsNumberAsAmount() {
        assertEquals(50, placeholderObjective(">=", "50").amount());
        assertEquals(50, placeholderObjective(">", "49").amount());
        assertEquals(50, placeholderObjective(">=", "49.5").amount());
    }

    @Test
    void nonCountingPlaceholderCheckStaysSatisfiedOrNot() {
        assertEquals(1, placeholderObjective("=", "50").amount());
        assertEquals(1, placeholderObjective("<", "50").amount());
        assertEquals(1, placeholderObjective(">=", "vip").amount());
        assertEquals(1, placeholderObjective(">=", "1").amount());
    }

    private static QuestObjective placeholderObjective(String operator, String value) {
        return new QuestObjective(ObjectiveType.PLACEHOLDER_CHECK, "%fish_caught%", 1,
                java.util.Map.of("operator", operator, "value", value));
    }

    private static Quest quest(String id, String displayName, Integer sortOrder) {
        return new Quest(id, displayName, "", "default",
                List.of(new QuestObjective(ObjectiveType.BREAK_BLOCK, "STONE", 1)),
                List.of(), List.of(), List.of(), false, 0, false, false, null, null, sortOrder);
    }
}
