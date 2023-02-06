package ru.yandex.qe.mail.meetings.cron.absence;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import com.sun.mail.smtp.SMTPMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.qe.mail.meetings.api.resource.dto.ActionType;
import ru.yandex.qe.mail.meetings.api.resource.dto.CalendarAction;
import ru.yandex.qe.mail.meetings.cron.actions.MockConfiguration;
import ru.yandex.qe.mail.meetings.cron.declines.DeclinedEventDismissedAttendeeMessageBuilderTest;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Event;
import ru.yandex.qe.mail.meetings.services.gaps.dto.Gap;
import ru.yandex.qe.mail.meetings.services.staff.StaffClient;
import ru.yandex.qe.mail.meetings.services.staff.dto.Person;
import ru.yandex.qe.mail.meetings.utils.DateRange;
import ru.yandex.qe.mail.meetings.utils.DateRangeTest;

import static org.junit.Assert.assertEquals;

/**
 * @author Sergey Galyamichev
 */
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MockConfiguration.class})
public class GapEventMessageBuilderTest {
    private static final String FILE = "/user_absence.html";
    @Inject
    private GapEventMessageBuilder gapEventMessageBuilder;
    @Inject
    private StaffClient staffClient;

    @Test
    public void testRemoveSeriesAndParticipants() throws Exception {
        MimeMessage mimeMessage = new SMTPMessage((Session) null);
        Date date = new Date(DateRangeTest.AUG_26_2019_MON_12_00_MSK);
        DateRange range = DateRange.nextBusinessWeek(date);
        Gap gap1 = new Gap(1, "trip", range.getFrom(), range.getTo(), "comment", false, true);
        Gap gap2 = new Gap(2, "vacation", range.getFrom(), range.getTo(), "comment", false, true);
        Map<ActionType, List<CalendarAction>> actions = new HashMap<>();
        Event event = new Event(1, null, date, date);
        event.setName("test");
        actions.put(ActionType.NOTIFICATION, Collections.singletonList(getCalendarAction(event,
                ActionType.NOTIFICATION)));
        actions.put(ActionType.REMOVE_EVENT, Collections.singletonList(getCalendarAction(event,
                ActionType.REMOVE_EVENT)));
        actions.put(ActionType.DECLINE_EVENT, Collections.singletonList(getCalendarAction(event,
                ActionType.DECLINE_EVENT)));
        Person person = staffClient.getByLogin("l2");
        gapEventMessageBuilder.prepareMessage(mimeMessage, person, List.of(gap1, gap2), actions);
        assertEquals("l2@ У Вас запланировано отсутствие", mimeMessage.getSubject());
        DeclinedEventDismissedAttendeeMessageBuilderTest.assertEmailEquals(FILE, mimeMessage);
    }

    private CalendarAction getCalendarAction(Event event, ActionType type) {
        CalendarAction action = GapActionsBuilder.buildAction(new Date(), "l4@yandex-team.ru", 5, event);
        action.setActionId("actionId");
        action.setGroupId(type + "-groupId");
        action.setType(type);
        return action;
    }

    @Test
    public void testRemoveSeriesAndParticipants2() throws Exception {
        MimeMessage mimeMessage = new SMTPMessage((Session) null);
        Date date = new Date(DateRangeTest.AUG_26_2019_MON_12_00_MSK);
        DateRange range = DateRange.nextBusinessWeek(date);
        Gap gap1 = new Gap(1, "trip", range.getFrom(), range.getTo(), "comment", false, true);
        Gap gap2 = new Gap(2, "vacation", range.getFrom(), range.getTo(), "comment", false, true);
        Map<ActionType, List<CalendarAction>> actions = new HashMap<>();
        Event event = new Event(1, null, date, date);
        event.setName("test");
        actions.put(ActionType.NOTIFICATION, Collections.singletonList(getCalendarAction(event,
                ActionType.NOTIFICATION)));
        actions.put(ActionType.REMOVE_EVENT, Collections.singletonList(getCalendarAction(event,
                ActionType.REMOVE_EVENT)));
        Person person = staffClient.getByLogin("l2");
        gapEventMessageBuilder.prepareMessage(mimeMessage, person, List.of(gap1, gap2), actions);
        assertEquals("l2@ У Вас запланировано отсутствие", mimeMessage.getSubject());
    }
}
