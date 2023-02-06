package ru.yandex.qe.mail.meetings.ws.sync;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.qe.mail.meetings.services.abc.dto.AbcService;
import ru.yandex.qe.mail.meetings.services.calendar.CalendarUpdate;
import ru.yandex.qe.mail.meetings.services.calendar.dto.WebEventData;
import ru.yandex.qe.mail.meetings.services.staff.StaffClient;
import ru.yandex.qe.mail.meetings.services.staff.dto.Person;
import ru.yandex.qe.mail.meetings.services.staff.dto.StaffGroup;
import ru.yandex.qe.mail.meetings.synchronizer.MeetingSynchronizer;
import ru.yandex.qe.mail.meetings.synchronizer.dto.IdWithType;
import ru.yandex.qe.mail.meetings.synchronizer.dto.SourceType;
import ru.yandex.qe.mail.meetings.synchronizer.dto.SyncErrors;
import ru.yandex.qe.mail.meetings.synchronizer.dto.SyncEvent;
import ru.yandex.qe.mail.meetings.ws.handlers.HandlerResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.qe.mail.meetings.ws.sync.Constants.ABC_SERVICE;
import static ru.yandex.qe.mail.meetings.ws.sync.Constants.EVENT_ID_BY_RUBTSOVDMV_CAN_INVITE;
import static ru.yandex.qe.mail.meetings.ws.sync.Constants.EVENT_ID_BY_RUBTSOVDMV_CAN_NOT_EDIT;
import static ru.yandex.qe.mail.meetings.ws.sync.Constants.EVENT_ID_BY_RUBTSOVDMV_CAN_NOT_INVITE;
import static ru.yandex.qe.mail.meetings.ws.sync.Constants.RUBTSOVDMV;
import static ru.yandex.qe.mail.meetings.ws.sync.Constants.STAFF_GROUP;

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MockConfiguration.class})
public class SyncServiceTest {
    @Inject
    private MeetingSynchronizer synchronizer;

    @Inject
    private EmbeddedPostgres ps;

    @Inject
    private CalendarUpdate calendarUpdate;

    @Inject
    private StaffClient staffClient;

    @Before
    public void setup() throws SQLException {
        reset(calendarUpdate);
        try (var conn = ps.getPostgresDatabase().getConnection()) {
            var statement = conn.createStatement();
            statement.execute("DELETE FROM synchronized_events");
        } catch (Exception e) {
            System.out.print(e);
            fail();
        }
    }

    @Test
    public void canAssignOneEvent() throws InterruptedException {
        var person = staffClient.getByLogin("login");

        var result = synchronizer.assign(EVENT_ID_BY_RUBTSOVDMV_CAN_INVITE, RUBTSOVDMV, Set.of(ABC_SERVICE), Set.of(STAFF_GROUP), Set.of(person));
        assertTrue(result.isOk());

        var mapping = synchronizer.getAllbyOwner(RUBTSOVDMV);
        assertEquals(1, mapping.size());
        assertTrue(mapping.containsKey(se(EVENT_ID_BY_RUBTSOVDMV_CAN_INVITE)));

        var entities = mapping.get(se(EVENT_ID_BY_RUBTSOVDMV_CAN_INVITE));
        assertEquals(3, entities.size());
        assertTrue(entities.contains(idwt(ABC_SERVICE)));
        assertTrue(entities.contains(idwt(STAFF_GROUP)));
        assertTrue(entities.contains(idwt(person)));


        Thread.sleep(250);
        verify(calendarUpdate, times(1)).updateEvent(eq(EVENT_ID_BY_RUBTSOVDMV_CAN_INVITE), anyInt(), any(), any(), any(), any(), Matchers.argThat(new BaseMatcher<>() {
            @Override
            public void describeTo(Description description) {
            }

            @Override
            public boolean matches(Object item) {
                WebEventData webEventData = (WebEventData) item;
                var attendees = new HashSet<>(webEventData.getAttendees());
                var expectedLogins = Set.of(
                        "abc-login@yandex-team.ru",
                        "rubtsovdmv@yandex-team.ru",
                        "login@yandex-team.ru",
                        "conf_rr_4_20@yandex-team.ru",
                        "conf_spb_4_8@yandex-team.ru",
                        "staff-login@yandex-team.ru"
                );
                return attendees.equals(expectedLogins);
            }
        }));
    }

    private SyncEvent se(int id) {
        return new SyncEvent("someone", id);
    }

    @Test
    public void canAssignTwice() {
        HandlerResult<SyncErrors> result;
        result = synchronizer.assign(EVENT_ID_BY_RUBTSOVDMV_CAN_INVITE, RUBTSOVDMV, Set.of(ABC_SERVICE), Set.of(STAFF_GROUP), Collections.emptySet());
        assertTrue(result.isOk());

        result = synchronizer.assign(EVENT_ID_BY_RUBTSOVDMV_CAN_INVITE, RUBTSOVDMV, Set.of(ABC_SERVICE), Collections.emptySet(), Collections.emptySet());
        assertTrue(result.isOk());

        var mapping = synchronizer.getAllbyOwner(RUBTSOVDMV);
        assertEquals(1, mapping.size());
        assertEquals(1, mapping.get(se(EVENT_ID_BY_RUBTSOVDMV_CAN_INVITE)).size());
    }

    @Test
    public void attendeesCanInviteThenAssign() {
        // stassiak is attendee and participantsCanInvite == true
        var result = synchronizer.assign(EVENT_ID_BY_RUBTSOVDMV_CAN_NOT_EDIT, "stassiak", Set.of(ABC_SERVICE), Set.of(STAFF_GROUP), Collections.emptySet());
        assertTrue(result.isOk());
    }

    @Test
    public void attendeesCanNotInviteThenError() {
        // stassiak is attendee and participantsCanInvite != true
        var result = synchronizer.assign(EVENT_ID_BY_RUBTSOVDMV_CAN_NOT_INVITE, "stassiak", Set.of(ABC_SERVICE), Set.of(STAFF_GROUP), Collections.emptySet());
        assertFalse(result.isOk());
        assertEquals(SyncErrors.PERMISSION_DENIED, result.error());
    }

    @Test
    public void strangePersonCanNotAssigne() {
        // xxx is NOT attendee and participantsCanInvite == true
        var result = synchronizer.assign(EVENT_ID_BY_RUBTSOVDMV_CAN_NOT_EDIT, "xxx", Set.of(ABC_SERVICE), Set.of(STAFF_GROUP), Collections.emptySet());
        assertFalse(result.isOk());
        assertEquals(SyncErrors.PERMISSION_DENIED, result.error());
    }

    @Test
    public void syncCanBeCanceled() {
        synchronizer.assign(EVENT_ID_BY_RUBTSOVDMV_CAN_INVITE, RUBTSOVDMV, Set.of(ABC_SERVICE), Set.of(STAFF_GROUP), Collections.emptySet());
        assertEquals(1, synchronizer.getAllbyOwner(RUBTSOVDMV).size());

        synchronizer.unassign(EVENT_ID_BY_RUBTSOVDMV_CAN_INVITE);
        assertTrue(synchronizer.getAllbyOwner(RUBTSOVDMV).isEmpty());
    }

    @Test
    public void canCancelSingleEntity() {
        synchronizer.assign(EVENT_ID_BY_RUBTSOVDMV_CAN_INVITE, RUBTSOVDMV, Set.of(ABC_SERVICE), Set.of(STAFF_GROUP), Collections.emptySet());
        synchronizer.unassign(EVENT_ID_BY_RUBTSOVDMV_CAN_INVITE, idwt(ABC_SERVICE));
        var syncSet = synchronizer.getAllbyOwner(RUBTSOVDMV).get(se(EVENT_ID_BY_RUBTSOVDMV_CAN_INVITE));
        assertEquals(1, syncSet.size());
        assertTrue(syncSet.contains(idwt(STAFF_GROUP)));
    }

    private IdWithType idwt(AbcService abcService) {
        return new IdWithType("" + abcService.id(), SourceType.ABC);
    }

    private IdWithType idwt(StaffGroup staffGroup) {
        return new IdWithType("" + staffGroup.id(), SourceType.STAFF);
    }

    private IdWithType idwt(Person person) {
        return new IdWithType(person.getLogin(), SourceType.PERSON);
    }

}
