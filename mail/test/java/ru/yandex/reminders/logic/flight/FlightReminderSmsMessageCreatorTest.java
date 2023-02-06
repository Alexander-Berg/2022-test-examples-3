package ru.yandex.reminders.logic.flight;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.misc.test.Assert;
import ru.yandex.reminders.logic.flight.shift.FlightShift;

/**
 * @author Eugene Voytitsky
 */
public class FlightReminderSmsMessageCreatorTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    public void createFlightShiftSmsMessage() {
        // the test checks both: 1).sms text, 2).and that new actual time will be ok,
        // even if day light saving rule was applied between planned and actual time
        FlightEventMeta flight = createFlight();
        LocalDateTime plannedDateTime = parse("2013-10-26 23:13:00");
        LocalDateTime actualDateTime = parse("2013-10-27 07:17:00");
        FlightShift shift = new FlightShift(
                "SU-137", 10, DateTimeZone.forID("+03:00"), plannedDateTime, actualDateTime);

        DateTimeZone kievTz = DateTimeZone.forID("Europe/Kiev");
        Assert.equals(3*60*60*1000, kievTz.getOffset(plannedDateTime.toDateTime(kievTz)));
        Assert.equals(2*60*60*1000, kievTz.getOffset(actualDateTime.toDateTime(kievTz)));

        Assert.equals(
                "Внимание! Время вылета вашего рейса изменилось. " +
                        "Рейс: SU-137 Москва-Хошимин, новое время вылета: 27/10 в 07:17",
                FlightReminderSmsMessageCreator.createFlightShiftSmsMessage(flight, shift));
    }

    private FlightEventMeta createFlight() {
        return new FlightEventMeta(
                "mid31829", "SU-137", Option.none(), Option.none(),
                new FlightCity("Москва"), Option.some(new FlightItem("Домодедово")),
                parse("2013-10-26 23:00:00"), DateTimeZone.forID("Europe/Kiev"),
                new FlightCity("Хошимин"), Option.none(),
                Option.none(), Option.none(),
                Option.none(), Option.some("http://mnogonas.ru"), Option.some("http://auroexpress.ru/book"),
                Option.some("SU-731"), Option.none(), Option.none(), Option.some("forward"),
                Option.none(), Option.none());
    }

    private static LocalDateTime parse(String s) {
        return FORMATTER.parseLocalDateTime(s);
    }
}
