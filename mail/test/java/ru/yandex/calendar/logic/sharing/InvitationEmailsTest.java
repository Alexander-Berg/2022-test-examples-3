package ru.yandex.calendar.logic.sharing;

import org.junit.Test;

import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.commune.mail.MailMessage;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.io.ClassPathResourceInputStreamSource;
import ru.yandex.misc.io.file.File2;

/**
 * @author Stepan Koltsov
 */
public class InvitationEmailsTest {

    @Test
    public void createInvitationCancelMessage() {
        IcsCalendar ics = IcsCalendar.parse(new ClassPathResourceInputStreamSource(InvitationEmailsTest.class, "InvitationEmailsTest-cancel.ics"));
        MailMessage message = InvitationEmails.createDummyIcsMessage(
                new Email("nga@yandex-team.ru"), new Email("isafarov@yandex-team.ru"),
                "привет от календаря", "жаль, не получилось лучше",
                ics);
        File2 dir = File2.valueOf("./tmp");
        dir.mkdirs();
        dir.child(InvitationEmailsTest.class.getSimpleName() + ".eml").write(message.serializeToBytes());
    }

} //~
