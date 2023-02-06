package ru.yandex.qe.mail.meetings.cron.actions;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobExecutionContext;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.qe.mail.meetings.api.resource.dto.ActionType;
import ru.yandex.qe.mail.meetings.api.resource.dto.CalendarAction;
import ru.yandex.qe.mail.meetings.api.resource.dto.Status;
import ru.yandex.qe.mail.meetings.config.NotificationConfiguration;
import ru.yandex.qe.mail.meetings.cron.dismissed.DismissedAttendeesJob;
import ru.yandex.qe.mail.meetings.mocks.CommonMockConfiguration;
import ru.yandex.qe.mail.meetings.rooms.dao.SentEmailsDao;
import ru.yandex.qe.mail.meetings.services.calendar.CalendarUpdate;
import ru.yandex.qe.mail.meetings.services.calendar.CalendarWeb;
import ru.yandex.qe.mail.meetings.services.calendar.dto.WebEventData;
import ru.yandex.qe.mail.meetings.services.calendar.dto.faults.CalendarException;
import ru.yandex.qe.mail.meetings.utils.DateRangeTest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Sergey Galyamichev
 */
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MockConfiguration.class})
public class EventWithDismissedUserTest {
    private static final Date FETCH_DATE = DateRangeTest.toDate("2019-10-28T12:30:00");
    private static final Date CHECK_DATE = DateRangeTest.toDate("2019-10-30T12:32:00");
    private static final Date EXECUTION_TIME = DateRangeTest.toDate("2019-11-05T12:32:00");

    private static final int FORBIDDEN_FRUIT = 38912574;
    private static final int PERPETUUM_MOBILE = 50000000;

    @Inject
    private DismissedAttendeesJob dismissedAttendeesJob;
    @Inject
    private DoActionsJob doActionsJob;
    @Inject
    private JavaMailSender mailSender;
    @Inject
    private NotificationConfiguration notConfig;
    @Inject
    private SentEmailsDao sentEmailsDao;
    @Inject
    private CalendarWeb calendarWeb;
    @Inject
    private CalendarUpdate calendarUpdate;
    @Inject
    public CommonMockConfiguration.SendAnswer sendAnswer;

    @Before
    public void setUp() {
        sendAnswer.clear();
        reset(calendarUpdate);
    }

    @Test
    public void testUserRemoved() {
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getFireTime()).thenReturn(FETCH_DATE, FETCH_DATE, CHECK_DATE, EXECUTION_TIME);
        dismissedAttendeesJob.execute(context);
        assertEquals(3, sendAnswer.invocations);
        assertEquals(2, sentEmailsDao.getActions(ActionType.REMOVE_PARTICIPANTS_SERIES, Status.PENDING).size());
        assertEquals(2, sentEmailsDao.getActions(ActionType.REMOVE_SERIES, Status.PENDING).size());
        dismissedAttendeesJob.execute(context);
        assertEquals(3, sendAnswer.invocations);
        doActionsJob.execute(context);
        List<CalendarAction> pendingActions = sentEmailsDao.getExpiredPendingActions(EXECUTION_TIME);
        assertEquals(4, pendingActions.size());
        Map<ActionType, Long> statuses = pendingActions.stream()
                .collect(Collectors.groupingBy(CalendarAction::getType, Collectors.counting()));
        assertEquals(2L, (long) statuses.get(ActionType.REMOVE_PARTICIPANTS_SERIES));
        assertEquals(2L, (long) statuses.get(ActionType.REMOVE_SERIES));
        doActionsJob.execute(context);
        verify(calendarUpdate, times(1)).updateEvent(eq(35453290), anyInt(), eq("2019-10-29T10:00:00"), any(), any(), any(), any());
        verify(calendarUpdate, times(1)).updateEvent(eq(34761317), anyInt(), eq("2019-11-01T07:00:00"), any(), any(), any(), any());
        verify(calendarUpdate, times(1)).deleteEvent(eq(39136382), anyInt(), eq(null), any(), any());
        verify(calendarUpdate, times(1)).deleteEvent(eq(37730976), anyInt(), eq(null), any(), any());
        assertEquals(0, sentEmailsDao.getExpiredPendingActions(EXECUTION_TIME).size());
    }

    @Test
    public void testUserRemovedAfterUpdate() {
        JobExecutionContext context = mock(JobExecutionContext.class);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(EXECUTION_TIME);
        calendar.add(Calendar.DAY_OF_MONTH, notConfig.getDismissed().getDaysToAction());
        when(context.getFireTime()).thenReturn(FETCH_DATE, calendar.getTime());
        dismissedAttendeesJob.execute(context);
        verify(mailSender, times(3)).send(any(MimeMessagePreparator.class));
        List<CalendarAction> pendingActions = sentEmailsDao.getActions(Status.PENDING);
        assertEquals(4, pendingActions.size());
        for (CalendarAction action : pendingActions) {
            WebEventData event = WebEventData.fromEvent(calendarWeb.getEvent(action.getEventId()));
            try {
                calendarUpdate.updateEvent(action.getEventId(), action.getSequence(), null, true, false, null, event);
            } catch (Exception ignore) {
                //TODO check exception
            }
        }
        doActionsJob.execute(context);
//        verify(calendarUpdate, times(0)).deleteEvent(anyInt(), anyInt(), eq(null));
        assertEquals(0, sentEmailsDao.getExpiredPendingActions(calendar.getTime()).size());
    }

    @Test(expected = CalendarException.class)
    public void getForbiddenFruit() {
        calendarWeb.getEvent(FORBIDDEN_FRUIT);
    }

    @Test(expected = CalendarException.class)
    public void getPerpetuumMobile() {
        calendarWeb.getEvent(PERPETUUM_MOBILE);
    }

}
