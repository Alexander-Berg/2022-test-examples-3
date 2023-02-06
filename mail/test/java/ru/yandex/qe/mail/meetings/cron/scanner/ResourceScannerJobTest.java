package ru.yandex.qe.mail.meetings.cron.scanner;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobExecutionContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.qe.mail.meetings.api.resource.CalendarActions;
import ru.yandex.qe.mail.meetings.api.resource.dto.ActionType;
import ru.yandex.qe.mail.meetings.api.resource.dto.CalendarAction;
import ru.yandex.qe.mail.meetings.api.resource.dto.Status;
import ru.yandex.qe.mail.meetings.cron.actions.MockConfiguration;
import ru.yandex.qe.mail.meetings.cron.declines.DeclinedEventDismissedAttendeeMessageBuilderTest;
import ru.yandex.qe.mail.meetings.mocks.CommonMockConfiguration;
import ru.yandex.qe.mail.meetings.rooms.dao.SentEmailsDao;
import ru.yandex.qe.mail.meetings.security.tokens.UserAuthentication;
import ru.yandex.qe.mail.meetings.utils.DateRange;
import ru.yandex.qe.mail.meetings.utils.DateRangeTest;
import ru.yandex.qe.mail.meetings.ws.CalendarActionsImpl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author Sergey Galyamichev
 */
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MockConfiguration.class})
public class ResourceScannerJobTest {
    private static final Date ACTION_READY_DATE = DateRange.fromUTC(DateRangeTest.toDate("2019-10-30T03:30:00"));
    private static final Date ACTION_STALE_DATE = DateRange.fromUTC(DateRangeTest.toDate("2019-10-30T04:30:00"));
    private static final String FILE = "/request-outdated.html";

    private final static int EVENT_ID = 39931788;
    private static final String BENUA = "2";

    @Inject
    private ResourceScannerJob resourceScannerJob;
    @Inject
    private CalendarActions calendarActions;
    @Inject
    private SentEmailsDao sentEmailsDao;
    @Inject
    private EmbeddedPostgres ps;
    @Inject
    private CommonMockConfiguration.SendAnswer sendAnswer;

    @Before
    public void setUp() throws Exception {
        ps.getPostgresDatabase().getConnection().createStatement()
                .execute("delete from actions;");
    }

    @Test
    public void testCancelActions() {
        SecurityContextHolder.getContext().setAuthentication(new UserAuthentication("2", "l2", "ticket"));
        String actionId = calendarActions.findResource(EVENT_ID, BENUA, 120, false, null, "l2", null);
        checkActionStatus(actionId, Status.ACCEPTED);
        assertEquals(CalendarActionsImpl.PERFORMED_TEXT, calendarActions.cancelAction(actionId));
        List<CalendarAction> actions = sentEmailsDao.getActions(ActionType.ADD_RESOURCE, Status.ACCEPTED);
        assertEquals(0, actions.size());
    }

    @Test
    public void testScanJobActions() {
        String actionId = calendarActions.findResource(EVENT_ID, BENUA, 120, false, null, "l2", null);
        checkActionStatus(actionId, Status.ACCEPTED);
        JobExecutionContext context = mockJobExecutionContext(ACTION_READY_DATE);
        resourceScannerJob.execute(context);
        List<CalendarAction> actions = sentEmailsDao.getActions(ActionType.ADD_RESOURCE, Status.ACCEPTED);
        assertEquals(0, actions.size());
    }

    @Test
    public void testStaleActions() throws Exception {
        String actionId = calendarActions.findResource(EVENT_ID, "1", 120, false, null, "l2", null);
        checkActionStatus(actionId, Status.ACCEPTED);
        JobExecutionContext context = mockJobExecutionContext(ACTION_READY_DATE, ACTION_STALE_DATE);
        resourceScannerJob.execute(context);
        checkActionStatus(actionId, Status.ACCEPTED);
        resourceScannerJob.execute(context);
        checkActionStatus(actionId, Status.OUTDATED);
        DeclinedEventDismissedAttendeeMessageBuilderTest.assertEmailEquals(FILE, sendAnswer.message);
    }

    public JobExecutionContext mockJobExecutionContext(Date date, Date... dates) {
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getFireTime()).thenReturn(date, dates);
        return context;
    }

    private void checkActionStatus(String actionId, Status status) {
        List<CalendarAction> actions = sentEmailsDao.getActions(ActionType.ADD_RESOURCE, status);
        assertEquals(1, actions.size());
        assertEquals(actionId, actions.get(0).getActionId());
    }
}
