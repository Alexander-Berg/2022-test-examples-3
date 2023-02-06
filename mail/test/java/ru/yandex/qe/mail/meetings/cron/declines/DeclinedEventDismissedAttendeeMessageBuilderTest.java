package ru.yandex.qe.mail.meetings.cron.declines;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import com.sun.mail.smtp.SMTPMessage;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.qe.mail.meetings.blamer.DeclineEvent;
import ru.yandex.qe.mail.meetings.blamer.DeclineEvents;
import ru.yandex.qe.mail.meetings.utils.DateRange;
import ru.yandex.qe.mail.meetings.utils.DateRangeTest;

import static org.junit.Assert.assertEquals;
import static ru.yandex.qe.mail.meetings.utils.StringUtils.trim;

/**
 * @author Sergey Galyamichev
 */
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MockConfiguration.class})
public class DeclinedEventDismissedAttendeeMessageBuilderTest {
    private static final String USER_EMAIL = "man@yandex-team.ru";
    private static final String FILE = "/declined-events.html";

    @Inject
    private DeclinedEventMessageBuilder declinedEventMessageBuilder;
    @Inject
    private DeclinedEventProvider declinedEventProvider;

    @Test
    public void prepareMessage() throws Exception {
        MimeMessage mimeMessage = new SMTPMessage((Session) null);
        DeclineEvents user = new DeclineEvents();
        user.setEmail(USER_EMAIL);
        user.setName(MockConfiguration.USER_NAME);
        user.setCount(MockConfiguration.getEvents().size());
        DateRange range = DateRange.lastBusinessWeek(new Date(DateRangeTest.AUG_26_2019_MON_12_00_MSK));
        List<DeclineEvent> events = declinedEventProvider.getEventsList(user, range);
        declinedEventMessageBuilder.prepareMessage(mimeMessage, user, range, events);
        assertEquals("man@ Напоминание о работе с переговорками", mimeMessage.getSubject());
        assertEmailEquals(FILE, mimeMessage);
    }

    public static void assertEmailEquals(String file, MimeMessage mimeMessage) throws MessagingException, IOException {
        mimeMessage.saveChanges();
        String expected = trim(IOUtils.readLines(
                DeclinedEventDismissedAttendeeMessageBuilderTest.class.getResourceAsStream(file), StandardCharsets.UTF_8));
        String actual = trim(Arrays.asList(getText(mimeMessage).split("\n")));
        assertEquals(expected, actual);
    }

    public static String getText(Part part) throws MessagingException, IOException {
        if (part.isMimeType("text/*")) {
            return (String) part.getContent();
        } else if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String text = getText(mp.getBodyPart(i));
                if (text != null)
                    return text;
            }
        }
        return "";
    }
}
