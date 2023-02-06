package ru.yandex.qe.mail.meetings.cron.dismissed;

import java.util.Date;
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

import ru.yandex.qe.mail.meetings.api.resource.dto.CalendarAction;
import ru.yandex.qe.mail.meetings.config.NotificationConfiguration;
import ru.yandex.qe.mail.meetings.cron.declines.DeclinedEventDismissedAttendeeMessageBuilderTest;
import ru.yandex.qe.mail.meetings.services.calendar.dto.Event;
import ru.yandex.qe.mail.meetings.services.calendar.dto.User;
import ru.yandex.qe.mail.meetings.services.staff.StaffClient;
import ru.yandex.qe.mail.meetings.utils.DateRange;
import ru.yandex.qe.mail.meetings.utils.DateRangeTest;

import static org.junit.Assert.assertEquals;

/**
 * @author Sergey Galyamichev
 */
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MockConfiguration.class})
public class DismissedAttendeeMessageBuilderTest {
    private static final String FILE = "/dismissed-users.html";
    @Inject
    private DismissedAttendeeMessageBuilder dismissedAttendeeMessageBuilder;
    @Inject
    private DismissedAttendeesEventsProvider dismissedAttendeesEventsProvider;
    @Inject
    private StaffClient client;
    @Inject
    private NotificationConfiguration notConfig;

    @Test
    public void testRemoveSeriesAndParticipants() throws Exception {
        MimeMessage mimeMessage = new SMTPMessage((Session) null);
        Date date = new Date(DateRangeTest.AUG_26_2019_MON_12_00_MSK);
        DateRange range = DateRange.lastBusinessWeek(date);
        User user = MockConfiguration.toUser(client.getByUid("2"));
        Map<Event, List<User>> events = dismissedAttendeesEventsProvider.findDismissed(range.getFrom(), range.getTo()).get(user);
        List<CalendarAction> actions = DismissedAttendeesEventsProvider.buildActions(date, user, notConfig.getDismissed().getDaysToAction(), events);
        dismissedAttendeeMessageBuilder.prepareMessage(mimeMessage, range, user, actions);
        assertEquals("l2@ Бывшие сотрудники в Ваших встречах", mimeMessage.getSubject());
        DeclinedEventDismissedAttendeeMessageBuilderTest.assertEmailEquals(FILE, mimeMessage);
    }
}
