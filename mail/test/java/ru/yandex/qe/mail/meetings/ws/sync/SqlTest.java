package ru.yandex.qe.mail.meetings.ws.sync;

import java.sql.SQLException;
import java.util.Set;
import java.util.stream.IntStream;

import javax.inject.Inject;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.qe.mail.meetings.synchronizer.dao.SyncDao;
import ru.yandex.qe.mail.meetings.synchronizer.dto.SyncEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static ru.yandex.qe.mail.meetings.ws.sync.Constants.ABC_IDWT;
import static ru.yandex.qe.mail.meetings.ws.sync.Constants.EVENT_ID;
import static ru.yandex.qe.mail.meetings.ws.sync.Constants.OWNER;
import static ru.yandex.qe.mail.meetings.ws.sync.Constants.PERSON2_IDWT;
import static ru.yandex.qe.mail.meetings.ws.sync.Constants.PERSON_IDWT;
import static ru.yandex.qe.mail.meetings.ws.sync.Constants.STAFF_IDWT;
import static ru.yandex.qe.mail.meetings.ws.sync.Constants.SYNC_EVENT;

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MockConfiguration.class})
public class SqlTest {
    @Inject
    private SyncDao syncDao;

    @Inject
    private EmbeddedPostgres ps;

    @Before
    public void setup() throws SQLException {
        try (var conn = ps.getPostgresDatabase().getConnection()) {
            var statement = conn.createStatement();
            statement.execute("DELETE FROM synchronized_events");
        } catch (Exception e) {
            System.out.print(e);
            fail();
        }
    }

    @Test
    public void canAssignEvent() {
        assignEvent();

        var assigned = syncDao.getAssignedIds(EVENT_ID);

        assertEquals(3, assigned.size());
        assertTrue(assigned.contains(PERSON_IDWT));
        assertTrue(assigned.contains(ABC_IDWT));
        assertTrue(assigned.contains(STAFF_IDWT));
    }

    @Test
    public void canAssignAndUnassignEvent() {
        assignEvent();
        syncDao.unassign(EVENT_ID);
        var assigned = syncDao.getAssignedIds(EVENT_ID);
        assertTrue(assigned.isEmpty());
    }

    @Test
    public void canUnassignOneEntity() {
        assignEvent();
        syncDao.unassign(EVENT_ID, PERSON_IDWT);
        var assigned = syncDao.getAssignedIds(EVENT_ID);
        assertEquals(2, assigned.size());
        assertFalse(assigned.contains(PERSON_IDWT));
        assertTrue(assigned.contains(ABC_IDWT));
        assertTrue(assigned.contains(STAFF_IDWT));
    }

    @Test(expected = DuplicateKeyException.class)
    public void canNotAssignTwice() {
        assignEvent();
        syncDao.assign(SYNC_EVENT, Set.of(PERSON2_IDWT));
    }

    @Test
    public void canGetEventsByOwner() {
        assignEvent(1, "p1");
        assignEvent(2, "p1");
        assignEvent(3, "p3");
        assignEvent(4, "p3");
        assignEvent(5, "p4");
        var assignMap = syncDao.getAllByOwner("p1");
        assertEquals(2, assignMap.size());
        assertEquals(3, assignMap.get(new SyncEvent("", 2)).size());
    }

    @Test
    public void canGetAllEvents() {
        IntStream.range(0, 100).forEach(i -> {
            assignEvent(i, "owner-" + i);
        });
        var all = syncDao.getAllEvents();
        assertEquals(100, all.size());
    }

    @Test
    public void unassignedEventDoesntHaveLinkedIds() {
        var assigned = syncDao.getAssignedIds(2123);
        assertTrue(assigned.isEmpty());
    }

    @Test
    public void emptyEventListIsEmpty() {
        var isEmpty = syncDao.getAllEvents().isEmpty();
        assertTrue(isEmpty);
    }

    @Test
    public void canStopSyncAnyEvent() {
        var count = syncDao.unassign(123);
        assertEquals(0, count);
    }

    private void assignEvent() {
        assignEvent(EVENT_ID, OWNER);
    }
    private void assignEvent(int eventId, String owner) {
        syncDao.assign(new SyncEvent(owner, eventId), Set.of(
                PERSON_IDWT,
                STAFF_IDWT,
                ABC_IDWT
        ));
    }

}
