package ru.yandex.reminders.api.flight;

import lombok.val;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Option;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.reminders.logic.flight.FlightEventMeta;
import ru.yandex.reminders.logic.flight.FlightItem;
import ru.yandex.reminders.logic.flight.FlightSource;
import ru.yandex.reminders.logic.flight.airport.AirportManager;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class FlightDataConverterTest {
    private static final DateTimeZone LOCAL_TZ = DateTimeZone.getDefault();

    private static AirportManager airportManagerMock() {
        val mock = Mockito.mock(AirportManager.class);

        when(mock.findTimezoneByAirportCodesOrCityGeoId(
                eq(Option.empty()),
                eq(Option.empty()),
                eq(Option.empty()))).thenReturn(Option.empty());

        when(mock.findAirportByCodes(eq(Option.empty()), eq(Option.empty()))).thenReturn(Option.empty());

        return mock;
    }

    @Test
    public void departureTz() {
        val airportManager = airportManagerMock();

        val builder = MailFlightData.builder();
        builder.setFlightNumber("SU777").setDepartureCity("Москва").setArrivalCity("Хошимин");

        builder.setDepartureDateTime(new LocalDateTime(2014, 11, 7, 19, 30));
        builder.setArrivalDateTime(new LocalDateTime(2014, 11, 7, 20, 30));

        when(airportManager.chooseTimezoneForCityAndAirportName(any(), any(), any())).thenReturn(DateTimeZone.UTC);

        FlightEventMeta result = FlightDataConverter.toFlightEventMeta("mid", airportManager, builder.build());
        Assert.equals(DateTimeZone.UTC, result.getDepartureCityTz());

        builder.setDepartureTz(MoscowTime.TZ);
        result = FlightDataConverter.toFlightEventMeta("mid", airportManager, builder.build());

        Assert.equals(MoscowTime.TZ, result.getDepartureCityTz());
    }

    @Test
    public void arrivalTz() {
        val airportManager = airportManagerMock();

        val builder = MailFlightData.builder();
        builder.setFlightNumber("SU777").setDepartureCity("Москва").setArrivalCity("Хошимин");

        builder.setDepartureDateTime(new LocalDateTime(2014, 11, 7, 19, 30));
        builder.setArrivalDateTime(new LocalDateTime(2014, 11, 7, 20, 30));

        builder.setPlannedArrivalDateTime(new DateTime(2014, 11, 7, 20, 30, LOCAL_TZ));

        FlightEventMeta result = FlightDataConverter.toFlightEventMeta("mid", airportManager, builder.build());

        Assert.some(LOCAL_TZ, result.getArrivalTz());

        builder.setArrivalAirport(new FlightItem(Option.of("DME"), Option.empty(),
                Option.of("DME"), Option.of("DME"), Option.of("DME")));

        when(airportManager.findTimezoneByAirportCodesOrCityGeoId(any(), any(), any()))
                .thenReturn(Option.of(MoscowTime.TZ));
        when(airportManager.findAirportByCodes(any(), any())).thenReturn(Option.empty());

        result = FlightDataConverter.toFlightEventMeta("mid", airportManager, builder.build());

        Assert.some(MoscowTime.TZ, result.getArrivalTz());
    }

    @Test
    public void arrivalTimeByPlan() {
        val airportManager = airportManagerMock();

        val builder = MailFlightData.builder();
        builder.setFlightNumber("SU777").setDepartureCity("Москва").setArrivalCity("Хошимин");

        val plannedDeparture = new DateTime(2014, 11, 7, 19, 30, LOCAL_TZ);

        builder.setDepartureDateTime(plannedDeparture.plusHours(3).toLocalDateTime());
        builder.setPlannedDepartureDateTime(plannedDeparture);
        builder.setPlannedArrivalDateTime(plannedDeparture.plusHours(10));

        FlightEventMeta result = FlightDataConverter.toFlightEventMeta("mid", airportManager, builder.build());
        Assert.some(plannedDeparture.plusHours(13), result.getArrivalDateTime());

        builder.setLastSegmentSource(FlightSource.RASP);
        result = FlightDataConverter.toFlightEventMeta("mid", airportManager, builder.build());
        Assert.some(plannedDeparture.plusHours(10), result.getArrivalDateTime());
    }

    @Test
    public void arrivalBeforeDeparture() {
        testArrivalBeforeDeparture(
                new DateTime(2014, 11, 1, 0, 0, DateTimeZone.forOffsetHours(1)),
                new DateTime(2014, 11, 1, 0, 0, DateTimeZone.forOffsetHours(2)), 0, true);

        testArrivalBeforeDeparture(
                new DateTime(2014, 11, 1, 0, 0, DateTimeZone.forOffsetHoursMinutes(-2, 30)),
                new DateTime(2014, 11, 1, 0, 0, DateTimeZone.forOffsetHoursMinutes(-1, 30)), -3, true);

        testArrivalBeforeDeparture(
                new DateTime(2014, 11, 1, 2, 0, DateTimeZone.forOffsetHoursMinutes(-1, 30)),
                new DateTime(2014, 11, 1, 0, 0, DateTimeZone.forOffsetHoursMinutes(-1, 30)), -4, true);

        testArrivalBeforeDeparture(
                new DateTime(2014, 12, 13, 16, 10, 0, DateTimeZone.UTC),
                new DateTime(2014, 11, 30, 17, 20, 0, DateTimeZone.UTC), 0, false);
    }

    private void testArrivalBeforeDeparture(
            DateTime departure, DateTime arrival,
            int expectedArrivalTzOffsetHours, boolean expectedDepBeforeArr)
    {
        Assert.isTrue(departure.isAfter(arrival));

        val airportManager = airportManagerMock();
        val builder = MailFlightData.builder();

        builder.setFlightNumber("SU777").setArrivalCity("Москва").setDepartureCity("Хошимин");

        builder.setDepartureDateTime(departure.toLocalDateTime()).setDepartureTz(departure.getZone());
        builder.setArrivalDateTime(arrival.toLocalDateTime()).setPlannedArrivalDateTime(arrival);

        val result = FlightDataConverter.toFlightEventMeta("mid", airportManager, builder.build());

        Assert.equals(expectedDepBeforeArr, result.getDepartureDateTime().isBefore(result.getArrivalDateTime().get()));

        Assert.equals(departure.toLocalDateTime(), result.getDepartureDateTime().toLocalDateTime());
        Assert.equals(arrival.toLocalDateTime(), result.getArrivalDateTime().get().toLocalDateTime());

        Assert.equals(departure.toInstant(), result.getDepartureTs());
        Assert.equals(DateTimeZone.forOffsetHours(expectedArrivalTzOffsetHours),
                result.getArrivalDateTime().get().getZone());
    }
}
