package ru.yandex.market.delivery.transport_manager.service.les.exporter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.les.trip.TripToYardLesPayload;
import ru.yandex.market.delivery.transport_manager.util.TimeUtil;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.les.tm.TripInfoEvent;
import ru.yandex.market.logistics.les.tm.dto.MovementCourierDto;
import ru.yandex.market.logistics.les.tm.dto.TransportationPartnerExtendedInfoDto;
import ru.yandex.market.logistics.les.tm.dto.TripPointDto;
import ru.yandex.market.logistics.les.tm.enums.OwnershipType;
import ru.yandex.market.logistics.les.tm.enums.TransportationUnitType;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class LesTripToYardExporterTest extends AbstractContextualTest {
    public static final long TRIP_ID = 2L;

    @Autowired
    private LesTripToYardExporter exporter;

    @Autowired
    private LesProducer lesProducer;
    @Autowired
    private DataFieldMaxValueIncrementer lesEventIdIncrementer;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2022-01-21T10:00:00Z"), ZoneId.systemDefault());
        reset(lesProducer, lesEventIdIncrementer);
        when(lesEventIdIncrementer.nextLongValue()).thenReturn(22L);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lesProducer);
    }

    @Test
    void testMissing() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> exporter.send(new TripToYardLesPayload(1L))
        );
    }

    @DatabaseSetup({
        "/repository/route/full_routes.xml",
        "/repository/route_schedule/full_schedules.xml",
        "/repository/trip/before/trips_and_transportations.xml",
        "/repository/trip/before/courier_for_les_export.xml",
    })
    @Test
    void send() {
        exporter.send(new TripToYardLesPayload(TRIP_ID));

        MovementCourierDto courier = new MovementCourierDto(
            "1",
            "Эльдар",
            "Сарыев",
            "Бахрам оглы",
            "R420",
            "A012BC177",
            "Scania",
            "AB1234199",
            "+7 (999) 123-45-67",
            "+7 (902) 567-89-01",
            OwnershipType.RENT,
            null
        );
        TripInfoEvent eventPayload = new TripInfoEvent(
            TRIP_ID,
            Map.of(0L, courier),
            List.of(
                new TripPointDto(
                    3L,
                    LocalDateTime.parse("2021-11-26T10:00:00").atOffset(TimeUtil.DEFAULT_ZONE_OFFSET),
                    null,
                    TransportationUnitType.OUTBOUND,
                    1L,
                    "Партнёр №12",
                    "TMU3"
                ),
                new TripPointDto(
                    4L,
                    LocalDateTime.parse("2021-11-26T12:00:00").atOffset(TimeUtil.DEFAULT_ZONE_OFFSET),
                    0L,
                    TransportationUnitType.OUTBOUND,
                    2L,
                    "Партнёр №12",
                    "TMU5"
                ),
                new TripPointDto(
                    5L,
                    LocalDateTime.parse("2021-11-26T15:00:00").atOffset(TimeUtil.DEFAULT_ZONE_OFFSET),
                    0L,
                    TransportationUnitType.INBOUND,
                    3L,
                    "Партнёр №12",
                    "TMU6"
                ),
                new TripPointDto(
                    6L,
                    LocalDateTime.parse("2021-11-26T15:00:00").atOffset(TimeUtil.DEFAULT_ZONE_OFFSET),
                    null,
                    TransportationUnitType.INBOUND,
                    3L,
                    "Партнёр №12",
                    "TMU4"
                )
            ),
            new TransportationPartnerExtendedInfoDto(1L, "Партнёр №12", null)
        );

        Event event = new Event(
            "tm",
            String.valueOf(22L),
            Instant.now(clock).toEpochMilli(),
            "TRIP_INFO",
            eventPayload,
            "Информация о рейсе"
        );

        verify(lesProducer).send(eq(event), eq("tm_out"));
    }

    @DatabaseSetup({
        "/repository/route/full_routes.xml",
        "/repository/route_schedule/full_schedules.xml",
        "/repository/trip/before/trips_and_transportations.xml",
    })
    @Test
    void sendNoCourier() {
        exporter.send(new TripToYardLesPayload(TRIP_ID));

        TripInfoEvent eventPayload = new TripInfoEvent(
            TRIP_ID,
            Collections.emptyMap(),
            List.of(
                new TripPointDto(
                    3L,
                    LocalDateTime.parse("2021-11-26T10:00:00").atOffset(TimeUtil.DEFAULT_ZONE_OFFSET),
                    null,
                    TransportationUnitType.OUTBOUND,
                    1L,
                    "Партнёр №12",
                    "TMU3"
                ),
                new TripPointDto(
                    4L,
                    LocalDateTime.parse("2021-11-26T12:00:00").atOffset(TimeUtil.DEFAULT_ZONE_OFFSET),
                    null,
                    TransportationUnitType.OUTBOUND,
                    2L,
                    "Партнёр №12",
                    "TMU5"
                ),
                new TripPointDto(
                    5L,
                    LocalDateTime.parse("2021-11-26T15:00:00").atOffset(TimeUtil.DEFAULT_ZONE_OFFSET),
                    null,
                    TransportationUnitType.INBOUND,
                    3L,
                    "Партнёр №12",
                    "TMU6"
                ),
                new TripPointDto(
                    6L,
                    LocalDateTime.parse("2021-11-26T15:00:00").atOffset(TimeUtil.DEFAULT_ZONE_OFFSET),
                    null,
                    TransportationUnitType.INBOUND,
                    3L,
                    "Партнёр №12",
                    "TMU4"
                )
            ),
            new TransportationPartnerExtendedInfoDto(1L, "Партнёр №12", null)
        );

        Event event = new Event(
            "tm",
            String.valueOf(22L),
            Instant.now(clock).toEpochMilli(),
            "TRIP_INFO",
            eventPayload,
            "Информация о рейсе"
        );

        verify(lesProducer).send(eq(event), eq("tm_out"));
    }
}
