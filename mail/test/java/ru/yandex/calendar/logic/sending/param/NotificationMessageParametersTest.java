package ru.yandex.calendar.logic.sending.param;

import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.user.Language;
import ru.yandex.calendar.logic.user.NameI18n;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.commune.mail.MailAddress;
import ru.yandex.inside.passport.PassportSid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class NotificationMessageParametersTest extends CalendarTestBase {

    @Test
    public void formatSmsWithTime() {
        formatSmsCommon(true, 0, "Сегодня в ");
    }

    @Test
    public void formatSms() {
        formatSmsCommon(false, 0, "Сегодня");
    }

    @Test
    public void formatSmsTomorrow() {
        formatSmsCommon(false, -1, "Завтра");
    }

    @Test
    public void formatSmsOnDayAfterTomorrow() {
        formatSmsCommon(false, -2, "05.07.10");
    }

    private void formatSmsCommon(boolean withTime, int daysAhead, String expectedWhen) {
        String sms = formatSmsInner(withTime, daysAhead, "Left 4 Dead 2");
        Assert.assertTrue(sms.contains(expectedWhen));
        Assert.assertTrue(sms.contains("В лесу"));
    }

    @Test
    public void formatLongSms() {
        String longName =
                new String(new char[NotificationMessageParameters.SMS_MAX_FIELD_LENGTH * 10]).replace('\0', '-');
        String sms = formatSmsInner(true, 0, longName);

        Assert.isTrue(sms.length() < NotificationMessageParameters.SMS_MAX_FIELD_LENGTH * 2);
    }

    private String formatSmsInner(boolean withTime, int daysAhead, String eventName) {
        EventMessageTimezone emtz = EventMessageTimezone.create(DateTimeZone.forID("Asia/Krasnoyarsk"), new Instant());
        final LocalDateTime eventStartTs = new LocalDateTime(2010, 7, 5, 11, 13, 15);
        final LocalDateTime eventEndTs = new LocalDateTime(2010, 7, 5, 12, 13, 15);

        EventTimeParameters time = new EventTimeParameters(eventStartTs, eventEndTs, false, false, Option.empty(), emtz);

        EventMessageInfo emi = new EventMessageInfo(12, 27, "", Option.empty(), time,
                eventName, "", EventLocation.location("В лесу"), PassportSid.CALENDAR, Option.empty(), Option.empty());

        LocalDateTime now = new LocalDateTime(2010, 7, 5, 12, 13, 14).plusDays(daysAhead);
        Sender sender = new Sender(Option.empty(), Option.empty(), new NameI18n("", ""), Option.empty(), new Email("a@b.com"));

        CommonEventMessageParameters commonMessageParameters = new CommonEventMessageParameters(
                Language.RUSSIAN,
                now, sender, Recipient.of(new MailAddress(new Email("a@b.com")), Option.empty()), new Email("a@b.com"),
                "http://calendar.yandex.ru/", false, MessageOverrides.EMPTY);

        NotificationMessageParameters p = new NotificationMessageParameters(
                commonMessageParameters, emi);
        return p.formatSms();
    }
} //~
