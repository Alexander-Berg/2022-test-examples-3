package ru.yandex.calendar.frontend.ews.imp;

import java.util.List;

import com.microsoft.schemas.exchange.services._2006.types.AttendeeType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.EmailAddressType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfAttendeesType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.util.idlent.YandexUser;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class EwsImporterGetEmailTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private EwsImporter ewsImporter;

    @Test
    public void parseStrangeExchangeEmails() {
        YandexUser user = TestManager.createYashunsky();
        TestUserInfo userInfo = testManager.prepareYandexUser(user);

        String email = user.getEmail().get().toString();
        String name = user.getName().get();

        ListF<String> exchangeStrangeFormats = Cf.list(
                "/O=YANDEX/OU=First Administrative Group/cn=Recipients/cn=" + userInfo.getLogin().toString(),
                "/O=YANDEX/OU=(FYDIBOHF23SPDLT)/CN=RECIPIENTS/CN=" + user.getLogin().toString().toUpperCase(),
                "attendee mailbox has empty email: EmailAddressType@2c73315e[name=" + email + "; (" + name
                        + "),emailAddress=<null>,routingType=<null>,mailboxType=<null>,itemId=<null>]",
                "'" + email + "'"
        );

        for (String exchangeFormat : exchangeStrangeFormats) {
            EmailAddressType emailAddress = new EmailAddressType();
            emailAddress.setEmailAddress(exchangeFormat);

            Option<Email> result = ewsImporter.getEmailOSmart(emailAddress);

            Assert.some(result, "could not extract email from: " + exchangeFormat);
            Assert.equals(userInfo.getEmail(), result.get(), "emails do not match: " + exchangeFormat);
        }
    }

    @Test
    public void fixEmails() {
        EmailAddressType emailAddressType = new EmailAddressType();
        emailAddressType.setEmailAddress("'fixable@yandex-team.ru'");
        AttendeeType attendeeType = new AttendeeType();
        attendeeType.setMailbox(emailAddressType);

        NonEmptyArrayOfAttendeesType attendees = new NonEmptyArrayOfAttendeesType() {
            public List<AttendeeType> getAttendee() {
                return Cf.list(attendeeType);
            }
        };

        CalendarItemType calItem = new CalendarItemType();
        calItem.setRequiredAttendees(attendees);
        Assert.hasSize(0, ExchangeEventDataConverter.getAttendeeEmails(calItem));

        ewsImporter.fixEmailsIfPossible(calItem);
        SetF<Email> expected = Cf.set(new Email("fixable@yandex-team.ru"));
        SetF<Email> actual = ExchangeEventDataConverter.getAttendeeEmails(calItem).unique();
        Assert.equals(expected, actual);
    }
}
