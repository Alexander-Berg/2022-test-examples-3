package ru.yandex.reminders.logic.panel;

import lombok.val;
import org.joda.time.Instant;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;
import ru.yandex.reminders.api.flight.FlightDataConverter;
import ru.yandex.reminders.api.flight.MailFlightData;
import ru.yandex.reminders.logic.event.Event;
import ru.yandex.reminders.logic.event.EventData;
import ru.yandex.reminders.logic.event.EventId;
import ru.yandex.reminders.logic.event.SpecialClientIds;
import ru.yandex.reminders.logic.flight.FlightCity;
import ru.yandex.reminders.logic.flight.FlightEventMeta;
import ru.yandex.reminders.logic.flight.FlightItem;
import ru.yandex.reminders.logic.flight.FlightSource;
import ru.yandex.reminders.logic.flight.airport.AirportManager;

/**
 * @author dbrylev
 */
public class PanelManagerTest {
    private static final Logger logger = LoggerFactory.getLogger(PanelManagerTest.class);

    @Test
    public void flight() {
        MailFlightData flightData = MailFlightData.builder()
                .setFlightNumber("SU2030")
                .setDepartureCity(new FlightCity("Москва", Option.some(75)))
                .setSource(FlightSource.RASP)
                .setArrivalCity(new FlightCity("Хошимин", Option.some(76)))
                .setAirline(new FlightItem(
                        Option.some("S7 Airlines"), Option.some(77),
                        Option.some("s7-icao"), Option.some("s7-iata"), Option.some("s7-sirena")))
                .setDepartureAirport(new FlightItem(
                        Option.some("Внуково"), Option.some(78),
                        Option.some("vko-icao"), Option.some("vko-iata"), Option.some("vko-sirena")))
                .setArrivalAirport(new FlightItem(Option.some("Хошиминовый"), Option.some(79),
                        Option.some("hos-icao"), Option.some("hos-iata"), Option.some("hos-sirena")))
                .setPlannedDepartureDateTime(MoscowTime.now().plusDays(3))
                .setPlannedArrivalDateTime(MoscowTime.now().plusDays(3))
                .setDepartureDateTime(LocalDateTime.now().plusDays(3))
                .setDepartureTz(MoscowTime.TZ)
                .setCheckInLink("http://mnogonas.ru")
                .setLastSegmentSource(FlightSource.IEX)
                .build();

        AirportManager airportManager = Mockito.mock(AirportManager.class, (Answer) invocation -> Option.none());
        FlightEventMeta meta = FlightDataConverter.toFlightEventMeta("mid", airportManager, flightData);

        Event event = new Event(
                new EventId(PassportUid.cons(1), SpecialClientIds.FLIGHT),
                new EventData(Option.none(), Option.none(), Option.none(), Option.none(), Cf.list(), Option.some(meta)),
                Option.none(), Instant.now(), "" + Instant.now());

        PanelPushClient clientMock = Mockito.mock(PanelPushClient.class);

        PanelManager panelManager = new PanelManager(clientMock);
        panelManager.sendFlight(event.getId(), event.getFlightMeta().toOptional());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(clientMock).push(Mockito.any(), Mockito.any(), messageCaptor.capture());

        logger.info(messageCaptor.getValue());
        Assert.equals(messageCaptor.getValue(), new String(PanelBender.mapper.serializeJson(new Flight(meta))));
    }

    @Test
    public void hotel() {
        String dataString = "{ " +
                "\"type\": \"hotel\", " +
                "\"messageDate\": 1418984624000, " +
                "\"data\": { " +
                    "\"domain\": \"booking.com\", " +
                    "\"mid\": \"2530000002819054818\", " +
                    "\"people\": \"2\", " +
                    "\"check-out_date\": \"2014-12-26 00:00:00\", " +
                    "\"hotel\": \"Pension Prague City\", " +
                    "\"reservation_number\": \"256074676\", " +
                    "\"cancellation_info\": \"21.2.2015 0:0:0\", " +
                    "\"city_geoid\": \"10511\", " +
                    "\"city\": \"Прага\", " +
                    "\"country\": \"Чехия\", " +
                    "\"number_of_nights\": \"5\", " +
                    "\"check-inn_date\": \"2014-12-21 00:00:00\", " +
                    "\"price\": \"\\u20AC 140\", " +
                    "\"address\": \"Stitneho 13 Prague 03 Prague 3 , 13000 , Чехия\", " +
                    "\"link\": \"https:\\/\\/secure.booking.com\\/app_link\\/myreservations.ru.html?bn=&pincode=\", " +
                    "\"cancellation_date\": \"2015-02-21 00:00:00\", " +
                    "\"country_geoid\": \"125\", " +
                    "\"uniq_id\": \"7997c0d95b0af81876a918d3f544789f\", " +
                    "\"check-inn_timestamp\": 1419105600000, " +
                    "\"check-out_timestamp\": 1419537600000, " +
                    "\"cancellation_timestamp\": 1424466000000 " +
                "} }";

        JsonObject data = JsonObject.parseObject(dataString);

        Event event = new Event(
                new EventId(PassportUid.cons(1), SpecialClientIds.HOTEL),
                new EventData(Option.none(), Option.none(), Option.none(), Option.some(data), Cf.list(), Option.none()),
                Option.none(), Instant.now(), "" + Instant.now());

        val id = event.getId();
        val jsonData = event.getEventData().getData().get();
        Hotel hotel = HotelConverter.convertHotelReservation(id, jsonData);

        PanelPushClient clientMock = Mockito.mock(PanelPushClient.class);

        PanelManager panelManager = new PanelManager(clientMock);
        panelManager.sendHotel(id, jsonData);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(clientMock).push(Mockito.any(), Mockito.any(), messageCaptor.capture());

        logger.info(messageCaptor.getValue());
        Assert.equals(messageCaptor.getValue(), new String(PanelBender.mapper.serializeJson(hotel)));
    }
}
