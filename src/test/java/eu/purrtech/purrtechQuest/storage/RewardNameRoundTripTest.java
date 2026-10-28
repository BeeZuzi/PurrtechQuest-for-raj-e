package eu.purrtech.purrtechQuest.storage;

import eu.purrtech.purrtechQuest.model.ObjectiveType;
import eu.purrtech.purrtechQuest.model.Quest;
import eu.purrtech.purrtechQuest.model.QuestObjective;
import eu.purrtech.purrtechQuest.model.QuestReward;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RewardNameRoundTripTest {

    @Test
    void aRewardNameStartingWithAHashSurvivesSaveAndReload(@TempDir Path tempDir) {
        QuestDefinitionRepository repository = new YamlQuestDefinitionRepository(tempDir, Logger.getAnonymousLogger());
        Quest quest = new Quest("t", "Test", "", "default",
                List.of(new QuestObjective(ObjectiveType.COLLECT_ITEM, "IRON", 1)),
                List.of(new QuestReward.Money(500.0, "&#FFF78B$500")), List.of(), List.of(),
                false, 0, false, false, null);

        repository.save(quest);
        Quest loaded = repository.loadAll().get(0);

        System.out.println("RAW SAVED FILE:\n" + java.nio.file.Files.exists(tempDir.resolve("t.yml")));
        try {
            System.out.println(java.nio.file.Files.readString(tempDir.resolve("t.yml")));
        } catch (Exception e) {
            System.out.println("read failed: " + e);
        }

        assertEquals("&#FFF78B$500", loaded.rewards().get(0).name());
    }
}
