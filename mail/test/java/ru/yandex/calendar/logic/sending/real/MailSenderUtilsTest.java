package ru.yandex.calendar.logic.sending.real;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.commune.mail.MailAddress;
import ru.yandex.commune.mail.MailMessage;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public class MailSenderUtilsTest extends CalendarTestBase {

    @Test
    public void inacceptableAddress() {
        Assert.isFalse(MailSenderUtils.isInacceptableAddress(new MailAddress(new Email("address@yandex-team.ru"), "Addressed")));
        Assert.isFalse(MailSenderUtils.isInacceptableAddress(new MailAddress(new Email("address@yandex-team.ru"), null)));
        Assert.isTrue(MailSenderUtils.isInacceptableAddress(new MailAddress(new Email("address@localhost"), null)));
    }

    @Test
    public void parseRecipientEmailsSafe() {
        MailMessage message = MailMessage.empty()
                .withTo("Danil Brylev <dbrylev@yandex-team.ru>, Vladimir Yashunskiy <yashunsky@yandex-team.ru>")
                .withHeader(MailHeaders.CC, "Windows Administrators <winadmin@yandex-team.ru>, xxx@yandex-team.ru");

        Assert.equals(
                Cf.list("dbrylev", "yashunsky", "winadmin", "xxx").map(l -> new Email(l + "@yandex-team.ru")),
                MailSenderUtils.parseRecipientEmailsSafe(message));
    }
}
