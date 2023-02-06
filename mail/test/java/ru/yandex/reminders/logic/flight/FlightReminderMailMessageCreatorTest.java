package ru.yandex.reminders.logic.flight;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.commune.mail.HeaderNames;
import ru.yandex.commune.mail.MailMessage;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.io.InputStreamSourceUtils;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
public class FlightReminderMailMessageCreatorTest {

    private static final Email EMAIL = new Email("dbrylev@yandex-team.ru");

    @Test
    public void createAndParse() {
        FlightEventMeta flight = createFlight();

        MailMessage message = FlightReminderMailMessageCreator.create(EMAIL, flight);
        MailMessage parsed = MailMessage.parse(InputStreamSourceUtils.bytes(message.serializeToBytes()));

        Assert.equals(message.getHeader(HeaderNames.SUBJECT), parsed.getHeader(HeaderNames.SUBJECT));
        Assert.equals(message.getHeader(HeaderNames.MESSAGE_ID), parsed.getHeader(HeaderNames.MESSAGE_ID));

        System.out.println(message.serializeToString());
    }

    private FlightEventMeta createFlight() {
        return createFlight(
                Option.some("Домодедово"),
                Option.some("http://mnogonas.ru"),
                Option.some("http://auroexpress.ru/book"),
                Option.some("SU-731"),
                Option.some(with0Secs(LocalDateTime.now().plusHours(10))),
                Option.some("forward"));
    }

    private FlightEventMeta createFlight(
            Option<String> departureAirport, Option<String> checkInLink, Option<String> aeroexpressLink,
            Option<String> lastSegmentFlightNumber, Option<LocalDateTime> lastSegmentDepartureDateTime,
            Option<String> direction)
    {
        return new FlightEventMeta(
                "mid12345", "SU-137", Option.none(), Option.none(),
                new FlightCity("Москва"), departureAirport.map(FlightItem::new),
                with0Secs(MoscowTime.now().toLocalDateTime()),
                DateTimeZone.forID("Europe/Moscow"),
                new FlightCity("Хошимин"), Option.none(),
                Option.none(), Option.none(), Option.none(),
                checkInLink, aeroexpressLink,
                lastSegmentFlightNumber, lastSegmentDepartureDateTime,
                Option.none(), direction, Option.none(), Option.none());
    }

    private static LocalDateTime with0Secs(LocalDateTime localDateTime) {
        return localDateTime.withSecondOfMinute(0).withMillisOfSecond(0);
    }
}
