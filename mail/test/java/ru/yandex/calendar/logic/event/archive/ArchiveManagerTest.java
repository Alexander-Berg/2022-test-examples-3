package ru.yandex.calendar.logic.event.archive;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;

/**
 * @author Daniel Brylev
 */
public class ArchiveManagerTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private ArchiveManager archiveManager;

    @Test
    public void archive() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(127);
        Resource resource = testManager.cleanAndCreateThreeLittlePigs();

        long eventId = testManager.createDefaultEvent(organizer.getUid(), "Survived").getId();
        testManager.addUserParticipantToEvent(eventId, organizer.getUid(), Decision.YES, true);
        testManager.addResourceParticipantToEvent(eventId, resource);

        archiveManager.storeDeletedEventsWithDependingItems(Cf.list(eventId), ActionInfo.webTest());
    }
}
