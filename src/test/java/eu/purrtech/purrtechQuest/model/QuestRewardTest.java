package eu.purrtech.purrtechQuest.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QuestRewardTest {

    @Test
    void aRewardWithoutANameHasNone() {
        assertNull(new QuestReward.Money(50.0).name());
        assertNull(new QuestReward.Command("say hi").name());
    }

    @Test
    void aBlankNameCountsAsNoName() {
        assertNull(new QuestReward.Experience(5, "   ").name());
        assertNull(new QuestReward.Item("DIAMOND", 1, null, "").name());
    }

    @Test
    void theNameIsTrimmed() {
        assertEquals("50 mincí", new QuestReward.Money(50.0, "  50 mincí ").name());
    }

    @Test
    void withNameReplacesOnlyTheNameOfEveryRewardType() {
        assertEquals(new QuestReward.Money(50.0, "nový"), new QuestReward.Money(50.0, "starý").withName("nový"));
        assertEquals(new QuestReward.Item("DIAMOND", 3, null, "nový"),
                new QuestReward.Item("DIAMOND", 3, null).withName("nový"));
        assertEquals(new QuestReward.Command("say hi", "nový"), new QuestReward.Command("say hi").withName("nový"));
        assertEquals(new QuestReward.Experience(7, "nový"), new QuestReward.Experience(7, "x").withName("nový"));
        assertEquals(new QuestReward.Permission("a.b", 60L, "nový"),
                new QuestReward.Permission("a.b", 60L).withName("nový"));
    }

    @Test
    void withNameCanClearAName() {
        assertNull(new QuestReward.Money(50.0, "x").withName(null).name());
    }
}
