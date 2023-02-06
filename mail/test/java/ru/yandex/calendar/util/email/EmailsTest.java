package ru.yandex.calendar.util.email;

import org.junit.Test;

import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.commune.mail.MailAddress;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public class EmailsTest extends CalendarTestBase {

    @Test
    public void email() {
        String unicode = "test@релиз.админкапдд.рф";
        String punycode = "test@xn--e1aeclx.xn--80aalbavookw.xn--p1ai";

        Assert.isTrue(Emails.isEmail(unicode));
        Assert.isTrue(Emails.isEmail(punycode));

        Assert.equals(punycode, Emails.punycode(unicode).getEmail());
        Assert.equals(punycode, Emails.punycode(punycode).getEmail());

        Assert.equals(unicode, Emails.getUnicoded(Emails.punycode(unicode)));
        Assert.equals(unicode, Emails.getUnicoded(Emails.punycode(punycode)));
    }

    @Test
    public void mailto() {
        String mailto = "mailto:test@%D1%80%D0%B5%D0%BB%D0%B8%D0%B7.%D1%80%D1%84";
        Email email = Emails.punycode("test@релиз.рф");

        Assert.equals(email, Emails.punycodeMailto(mailto));
        Assert.equals(mailto, Emails.getUnicodedMailto(Emails.punycodeMailto(mailto)));
    }

    @Test
    public void validate() {
        Assert.isTrue(Emails.isValid(new MailAddress(new Email("mnogo@nas.ru"))));
        Assert.isFalse(Emails.isValid(new MailAddress(new Email("много@nas.ru"))));

        Assert.isTrue(Emails.isValid(new MailAddress(Emails.punycode("mnogo@нас.ру"))));
    }
}
