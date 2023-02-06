package ru.yandex.market.delivery.transport_manager.service.trip;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.transport_manager.converter.LaunchTimeCalculator;
import ru.yandex.market.delivery.transport_manager.domain.dto.transportation.UpdatedTransportation;
import ru.yandex.market.delivery.transport_manager.domain.dto.trip.TripAndTransportations;
import ru.yandex.market.delivery.transport_manager.domain.dto.trip.TripCreateEntity;
import ru.yandex.market.delivery.transport_manager.domain.entity.CompositeCourierId;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementAdditionalData;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.Route;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteSchedule;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteSchedulePoint;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteScheduleSubtype;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteScheduleType;
import ru.yandex.market.delivery.transport_manager.domain.entity.trip.Trip;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSource;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSubtype;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;
import ru.yandex.market.delivery.transport_manager.service.transportation.InitialStatusSelector;
import ru.yandex.market.delivery.transport_manager.service.trip.transportation_factory.TransportationFactory;
import ru.yandex.market.delivery.transport_manager.util.RouteScheduleTripFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.delivery.transport_manager.util.RouteScheduleTripFactory.pair;
import static ru.yandex.market.delivery.transport_manager.util.RouteScheduleTripFactory.route;
import static ru.yandex.market.delivery.transport_manager.util.RouteScheduleTripFactory.sPoint;
import static ru.yandex.market.delivery.transport_manager.util.RouteScheduleTripFactory.trip;

class ScheduleToTripTransformerTest {

    private static final long SCHEDULE_ID = 100L;
    private static final LocalDate TRIP_START_DATE = LocalDate.parse("2021-11-30");

    private ScheduleToTripTransformer transformer;

    @BeforeEach
    void setUp() {

        LaunchTimeCalculator calculator = Mockito.mock(LaunchTimeCalculator.class);
        Mockito.when(calculator.launchTime(any(LocalDateTime.class), eq(TransportationType.LINEHAUL), any(Long.class)))
            .thenAnswer(i -> ((LocalDateTime) i.getArgument(0)).minusDays(1)); // launch time = outbound time - 1 day

        transformer = new ScheduleToTripTransformer(
            new TransportationFactory(calculator, new InitialStatusSelector(Mockito.mock(TmPropertyService.class)))
        );
    }

    @Test
    @DisplayName("Создние 1-1 рейса")
    void createTripByRouteSchedule() {
        TripCreateEntity trip = transformer.createTripByRouteSchedule(
            routeSample(),
            scheduleSample(
                sPoint(0, time("12:00"), time("13:00")),
                sPoint(1, time("15:00"), time("15:30"))
            ),
            TRIP_START_DATE
        );

        Assertions.assertThat(trip.getTrip()).isEqualTo(trip(null, SCHEDULE_ID, TRIP_START_DATE));
        Assertions.assertThat(trip.getTransportations()).containsExactly(transportation());
        Assertions.assertThat(trip.getUnitIndexes()).hasSize(2);
    }

    @Test
    @DisplayName("Проверка дат интервала, проходящего через полночь")
    void testIntervalThroughMidnight() {
        TripCreateEntity trip = transformer.createTripByRouteSchedule(
            routeSample(),
            scheduleSample(
                sPoint(0, time("23:00"), time("00:00"), 0),
                sPoint(1, time("23:30"), time("00:30"), 1)
            ),
            TRIP_START_DATE
        );

        Transportation t = trip.getTransportations().iterator().next();
        Assertions.assertThat(t.getOutboundUnit().getPlannedIntervalStart()).isEqualTo("2021-11-30T23:00:00");
        Assertions.assertThat(t.getOutboundUnit().getPlannedIntervalEnd()).isEqualTo("2021-12-01T00:00:00");
        Assertions.assertThat(t.getInboundUnit().getPlannedIntervalStart()).isEqualTo("2021-12-01T23:30:00");
        Assertions.assertThat(t.getInboundUnit().getPlannedIntervalEnd()).isEqualTo("2021-12-02T00:30:00");
    }

    @Test
    @DisplayName("Проверка одинакового времени запуска перемещений в одном многодневном рейсе")
    void testCommonLaunchTimeForTrip() {
        TripCreateEntity trip = transformer.createTripByRouteSchedule(
            route(1, pair(0, 10, 100, 3, 11, 111), pair(1, 20, 200, 2, 21, 211)),
            scheduleSample(
                sPoint(0, time("12:00"), time("13:00"), 0),
                sPoint(1, time("15:00"), time("15:30"), 1),
                sPoint(2, time("17:00"), time("18:00"), 2),
                sPoint(3, time("17:00"), time("18:00"), 2)
            ),
            TRIP_START_DATE
        );

        Assertions.assertThat(trip.getTransportations()).hasSize(2);
        Assertions.assertThat(trip.getTransportations())
            .extracting(Transportation::getPlannedLaunchTime)
            .containsOnly(LocalDateTime.parse("2021-11-29T12:00:00"));
    }

    @Test
    @DisplayName("Проверка добавления подтипа при создании перемещения с заполненным подтипом")
    void testSubtypeTagAddedOnCreate() {
        TripCreateEntity trip = transformer.createTripByRouteSchedule(
            routeSample(),
            scheduleSample(
                sPoint(0, time("23:00"), time("00:00"), 0),
                sPoint(1, time("23:30"), time("00:30"), 1)
            )
                .setSubtype(RouteScheduleSubtype.UNSCHEDULED),
            TRIP_START_DATE
        );

        Assertions.assertThat(trip.getTransportations()).extracting(Transportation::getSubtype)
            .containsOnly(TransportationSubtype.UNSCHEDULED);
    }

    @Test
    @DisplayName("Проверка добавления подтипа при обновлении перемещения с заполненным подтипом")
    void testSubtypeTagAddedOnUpdate() {
        List<UpdatedTransportation> result = transformer.updateTrip(
            routeSample(),
            scheduleSample(
                sPoint(0, time("23:00"), time("00:00"), 0),
                sPoint(1, time("23:30"), time("00:30"), 1)
            ).setSubtype(RouteScheduleSubtype.SUPPLEMENTARY_1),
            TRIP_START_DATE,
            new TripAndTransportations()
                .setTrip(trip(1L, SCHEDULE_ID, TRIP_START_DATE, 10, 11))
                .setTransportations(List.of(transportationWithIds()))
        );

        Assertions.assertThat(result.get(0).getTransportation().getSubtype())
            .isEqualTo(TransportationSubtype.SUPPLEMENTARY_1);
    }

    @Test
    @DisplayName("Проверка max_pallets для точки при создании перемещения")
    void testMaxPalletsOnCreate() {
        TripCreateEntity trip = transformer.createTripByRouteSchedule(
            routeSample(),
            scheduleSample(
                sPoint(0, time("23:00"), time("00:00"), 0).setMaxPallets(10),
                sPoint(1, time("23:30"), time("00:30"), 1).setMaxPallets(20)
            )
                .setMaxPallet(33)
                .setSubtype(RouteScheduleSubtype.UNSCHEDULED),
            TRIP_START_DATE
        );

        Assertions.assertThat(trip.getTransportations())
            .extracting(Transportation::getMovement)
            .extracting(Movement::getMaxPallet)
            .containsOnly(10);
    }

    @Test
    @DisplayName("Проверка run_id при создании перемещения")
    void testRunIDOnCreate() {
        TripCreateEntity trip = transformer.createTripByRouteSchedule(
            routeSample(),
            scheduleSample(
                "10001",
                sPoint(0, time("23:00"), time("00:00"), 0).setMaxPallets(10),
                sPoint(1, time("23:30"), time("00:30"), 1).setMaxPallets(20)
            )
                .setMaxPallet(33)
                .setSubtype(RouteScheduleSubtype.DUTY),
            TRIP_START_DATE
        );

        Assertions.assertThat(trip.getTransportations())
            .extracting(Transportation::getMovement)
            .extracting(Movement::getMaxPallet)
            .containsOnly(10);

        Assertions.assertThat(trip.getTrip())
            .extracting(Trip::getExternalId)
            .isEqualTo("10001");
    }

    @Test
    @DisplayName("Проверка additional_data у movement при создании перемещения")
    void testAdditionalMovementDataOnCreate() {
        TripCreateEntity trip = transformer.createTripByRouteSchedule(
            routeSample(),
            scheduleSample(
                "10001",
                new CompositeCourierId(12L, 15L),
                sPoint(0, time("23:00"), time("00:00"), 0).setMaxPallets(10),
                sPoint(1, time("23:30"), time("00:30"), 1).setMaxPallets(20)
            )
                .setMaxPallet(33)
                .setSubtype(RouteScheduleSubtype.DUTY),
            TRIP_START_DATE
        );

        MovementAdditionalData additionalData =
            trip.getTransportations().stream().findFirst().get().getMovement().getAdditionalData();

        Assertions.assertThat(additionalData.getCourier())
            .isEqualTo(new CompositeCourierId(12L, 15L));
    }

    @Test
    @DisplayName("Проверка max_pallets для всего расписания при создании перемещения")
    void testMaxPalletsFromScheduleOnCreate() {
        TripCreateEntity trip = transformer.createTripByRouteSchedule(
            routeSample(),
            scheduleSample(
                sPoint(0, time("23:00"), time("00:00"), 0),
                sPoint(1, time("23:30"), time("00:30"), 1).setMaxPallets(20)
            )
                .setMaxPallet(33)
                .setSubtype(RouteScheduleSubtype.UNSCHEDULED),
            TRIP_START_DATE
        );

        Assertions.assertThat(trip.getTransportations())
            .extracting(Transportation::getMovement)
            .extracting(Movement::getMaxPallet)
            .containsOnly(33);
    }

    @Test
    @DisplayName("Проверка max_pallets для точки при обновлении перемещения")
    void testMaxPalletsOnUpdate() {
        List<UpdatedTransportation> result = transformer.updateTrip(
            routeSample(),
            scheduleSample(
                sPoint(0, time("23:00"), time("00:00"), 0).setMaxPallets(10),
                sPoint(1, time("23:30"), time("00:30"), 1).setMaxPallets(20)
            )
                .setMaxPallet(33)
                .setSubtype(RouteScheduleSubtype.SUPPLEMENTARY_1),
            TRIP_START_DATE,
            new TripAndTransportations()
                .setTrip(trip(1L, SCHEDULE_ID, TRIP_START_DATE, 10, 11))
                .setTransportations(List.of(transportationWithIds()))
        );

        Assertions.assertThat(result)
            .extracting(UpdatedTransportation::getTransportation)
            .extracting(Transportation::getMovement)
            .extracting(Movement::getMaxPallet)
            .containsOnly(10);
    }

    @Test
    @DisplayName("Проверка max_pallets для всего расписания при обновлении перемещения")
    void testMaxPalletsFromScheduleOnUpdate() {
        List<UpdatedTransportation> result = transformer.updateTrip(
            routeSample(),
            scheduleSample(
                sPoint(0, time("23:00"), time("00:00"), 0),
                sPoint(1, time("23:30"), time("00:30"), 1).setMaxPallets(20)
            )
                .setMaxPallet(33)
                .setSubtype(RouteScheduleSubtype.SUPPLEMENTARY_1),
            TRIP_START_DATE,
            new TripAndTransportations()
                .setTrip(trip(1L, SCHEDULE_ID, TRIP_START_DATE, 10, 11))
                .setTransportations(List.of(transportationWithIds()))
        );

        Assertions.assertThat(result)
            .extracting(UpdatedTransportation::getTransportation)
            .extracting(Transportation::getMovement)
            .extracting(Movement::getMaxPallet)
            .containsOnly(33);
    }

    private Route routeSample() {
        return route(1, pair(0, 10, 100, 1, 11, 111));
    }

    private RouteSchedule scheduleSample(
        String runId,
        RouteSchedulePoint... points
    ) {
        return scheduleSample(runId, null, points);
    }

    private RouteSchedule scheduleSample(
        String runId,
        CompositeCourierId compositeCourierId,
        RouteSchedulePoint... points
    ) {
        return RouteScheduleTripFactory.schedule(
            SCHEDULE_ID,
            1L,
            RouteScheduleType.LINEHAUL,
            107L,
            3000L,
            List.of(DayOfWeek.values()),
            "hash",
            runId,
            compositeCourierId,
            points
        );
    }

    private RouteSchedule scheduleSample(
        RouteSchedulePoint... points
    ) {
        return scheduleSample(null, points);
    }

    private Transportation transportationWithIds() {
        var result = transportation().setId(1L);
        result.getOutboundUnit().setId(10L);
        result.getInboundUnit().setId(11L);
        return result;
    }

    private Transportation transportation() {
        return new Transportation()
            .setStatus(TransportationStatus.NEW)
            .setOutboundUnit(
                new TransportationUnit()
                    .setStatus(TransportationUnitStatus.NEW)
                    .setType(TransportationUnitType.OUTBOUND)
                    .setPartnerId(10L)
                    .setLogisticPointId(100L)
                    .setPlannedIntervalStart(LocalDateTime.parse("2021-11-30T12:00"))
                    .setPlannedIntervalEnd(LocalDateTime.parse("2021-11-30T13:00"))
            )
            .setMovement(
                new Movement()
                    .setStatus(MovementStatus.NEW)
                    .setPartnerId(107L)
                    .setPrice(3000L)
                    .setPlannedIntervalStart(LocalDateTime.parse("2021-11-30T12:00"))
                    .setPlannedIntervalEnd(LocalDateTime.parse("2021-11-30T15:30"))
            )
            .setInboundUnit(
                new TransportationUnit()
                    .setStatus(TransportationUnitStatus.NEW)
                    .setType(TransportationUnitType.INBOUND)
                    .setPartnerId(11L)
                    .setLogisticPointId(111L)
                    .setPlannedIntervalStart(LocalDateTime.parse("2021-11-30T15:00"))
                    .setPlannedIntervalEnd(LocalDateTime.parse("2021-11-30T15:30"))
            )
            .setDeleted(false)
            .setHash("hash")
            .setTransportationType(TransportationType.LINEHAUL)
            .setTransportationSource(TransportationSource.TM_GENERATED)
            .setRegular(true)
            .setPlannedLaunchTime(LocalDateTime.parse("2021-11-29T12:00:00"));
    }

    private LocalTime time(String strTime) {
        return LocalTime.parse(strTime);
    }
}
