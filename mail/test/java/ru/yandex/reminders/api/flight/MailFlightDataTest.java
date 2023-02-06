package ru.yandex.reminders.api.flight;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.reminders.logic.flight.FlightCity;
import ru.yandex.reminders.logic.flight.FlightSource;

/**
 * @author Eugene Voytitsky
 */
public class MailFlightDataTest {

    @Test
    public void hackTest1() {
        MailFlightData flightData = MailFlightData.builder()
                .setFlightNumber("f")
                .setDepartureCity("d")
                .setSource(FlightSource.RASP)
                .setArrivalCity("a")
                .setDepartureDateTime(LocalDateTime.now())
                .setLastSegmentSource(FlightSource.IEX)
                .build();

        flightData.validate();
        Assert.none(flightData.getSource());
        Assert.none(flightData.getLastSegmentSource());
    }

    @Test
    public void hackTest2() {
        MailFlightData flightData = MailFlightData.builder()
                .setFlightNumber("f")
                .setDepartureCity(new FlightCity("d", Option.some(1)))
                .setSource(FlightSource.RASP)
                .setArrivalCity("a")
                .setPlannedDepartureDateTime(MoscowTime.now())
                .setPlannedArrivalDateTime(MoscowTime.now())
                .setDepartureDateTime(LocalDateTime.now())
                .setDepartureTz(DateTimeZone.UTC)
                .setLastSegmentSource(FlightSource.IEX)
                .build();

        flightData.validate();
        Assert.some(FlightSource.RASP, flightData.getSource());
        Assert.some(FlightSource.IEX, flightData.getLastSegmentSource());
    }

    @Test
    public void hackTest3() {
        MailFlightData flightData = MailFlightData.builder()
                .setFlightNumber("f")
                .setDepartureCity("d")
                .setSource(FlightSource.RASP)
                .setArrivalCity("a")
                .setDepartureDateTime(LocalDateTime.now())
                .setPlannedDepartureDateTime(MoscowTime.now())
                .setPlannedArrivalDateTime(MoscowTime.now())
                .setLastSegmentSource(FlightSource.RASP)
                .build();


        flightData.validate();
        Assert.none(flightData.getSource());
        Assert.none(flightData.getLastSegmentSource());
    }

    @Test
    public void hackTest4() {
        MailFlightData flightData = MailFlightData.builder()
                .setFlightNumber("f")
                .setDepartureCity("d")
                .setSource(FlightSource.IEX)
                .setArrivalCity("a")
                .setDepartureDateTime(LocalDateTime.now())
                .setLastSegmentSource(FlightSource.RASP)
                .build();

        flightData.validate();
        Assert.some(FlightSource.IEX, flightData.getSource());
        Assert.some(FlightSource.RASP, flightData.getLastSegmentSource());
    }
}
