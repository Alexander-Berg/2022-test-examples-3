package ru.yandex.market.delivery.transport_manager.service.trip;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.transport_manager.config.properties.TmProperties;
import ru.yandex.market.delivery.transport_manager.converter.LaunchTimeCalculator;
import ru.yandex.market.delivery.transport_manager.domain.dto.transportation.UpdatedTransportation;
import ru.yandex.market.delivery.transport_manager.domain.dto.trip.TripCreateEntity;
import ru.yandex.market.delivery.transport_manager.domain.dto.trip.TripsRefresherInputData;
import ru.yandex.market.delivery.transport_manager.domain.entity.CompositeCourierId;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.Route;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteSchedule;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteScheduleType;
import ru.yandex.market.delivery.transport_manager.domain.entity.trip.Trip;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;
import ru.yandex.market.delivery.transport_manager.service.transportation.InitialStatusSelector;
import ru.yandex.market.delivery.transport_manager.service.trip.transportation_factory.TransportationFactory;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.transport_manager.util.RouteScheduleTripFactory.pair;
import static ru.yandex.market.delivery.transport_manager.util.RouteScheduleTripFactory.route;
import static ru.yandex.market.delivery.transport_manager.util.RouteScheduleTripFactory.sPoint;
import static ru.yandex.market.delivery.transport_manager.util.RouteScheduleTripFactory.schedule;
import static ru.yandex.market.delivery.transport_manager.util.RouteScheduleTripFactory.transportation;
import static ru.yandex.market.delivery.transport_manager.util.RouteScheduleTripFactory.trip;

class TripsRefresherTest {
    private final TestableClock clock = new TestableClock();
    private TripsRefresher tripsRefresher;
    private TmProperties tmProperties;

    @BeforeEach
    void setUp() {
        tmProperties = mock(TmProperties.class);
        LaunchTimeCalculator calculator = new LaunchTimeCalculator(tmProperties, mock(TmPropertyService.class), clock);
        tripsRefresher = new TripsRefresher(
            tmProperties,
            new ScheduleToTripTransformer(
                new TransportationFactory(calculator, new InitialStatusSelector(Mockito.mock(TmPropertyService.class)))
            )
        );

        when(tmProperties.getDaysToPlanFor(eq(RouteScheduleType.LINEHAUL))).thenReturn(7);
        when(tmProperties.getDaysToPlanFor(eq(RouteScheduleType.ORDERS_RETURN))).thenReturn(7);
        when(tmProperties.getDaysToLaunchBeforePlannedOutboundTime(eq(TransportationType.LINEHAUL))).thenReturn(2);
    }

    @Test
    @DisplayName("Пустые входные данные")
    void testEmptyInput() {
        LocalDateTime now = LocalDateTime.parse("2021-11-21T10:00:00");
        tripsRefresher.updateAll(input(Set.of(), Set.of(), Set.of(), Set.of()), now);
    }

    @Test
    @DisplayName("Генерация новых рейсов")
    void testCreateNewTrips() {
        LocalDateTime now = LocalDateTime.parse("2021-11-21T10:00:00");
        var input = input(
            Set.of(simpleRoute()),
            Set.of(simpleSchedule(List.of(DayOfWeek.values()), "hash")),
            Set.of(),
            Set.of()
        );
        var output = tripsRefresher.updateAll(input, now);

        Assertions.assertThat(output.getForCreate()).hasSize(7);
        Assertions.assertThat(output.getForUpdate()).isEmpty();
        Assertions.assertThat(output.getForCancel()).isEmpty();

        assertTripDates(output.getForCreate(), getAllDatesInclusive("2021-11-21", "2021-11-27"));
    }

    @Test
    void testCreateQuickTripInThePast() {
        LocalDateTime now = LocalDateTime.parse("2021-11-21T10:00:00");
        RouteSchedule schedule = scheduleWithHolidays(
            List.of(DayOfWeek.values()),
            "hash",
            now.toLocalDate(),
            now.toLocalDate(),
            null,
            new CompositeCourierId(1L, 10L)
        );
        var input = input(
            Set.of(simpleRoute()),
            Set.of(schedule),
            Set.of(),
            Set.of()
        );
        var output = tripsRefresher.updateAll(input, now);

        Assertions.assertThat(output.getForCreate()).hasSize(1);
        Assertions.assertThat(output.getForUpdate()).isEmpty();
        Assertions.assertThat(output.getForCancel()).isEmpty();

        assertTripDates(output.getForCreate(), List.of(now.toLocalDate()));
    }

    @Test
    void testCreateQuickTripReturns() {
        LocalDateTime now = LocalDateTime.parse("2021-11-21T10:00:00");
        RouteSchedule schedule = scheduleWithHolidays(
            List.of(DayOfWeek.values()),
            "hash",
            now.toLocalDate(),
            now.toLocalDate(),
            null,
            new CompositeCourierId(1L, 10L),
            RouteScheduleType.ORDERS_RETURN
        );
        var input = input(
            Set.of(simpleRoute()),
            Set.of(schedule),
            Set.of(),
            Set.of()
        );
        var output = tripsRefresher.updateAll(input, now);

        Assertions.assertThat(output.getForCreate()).hasSize(1);
        Assertions.assertThat(output.getForUpdate()).isEmpty();
        Assertions.assertThat(output.getForCancel()).isEmpty();

        assertTripDates(output.getForCreate(), List.of(now.toLocalDate()));
    }

    @Test
    @DisplayName("Обновление на следующий день. Добавляем один рейс")
    void testUpdateExistingTrips() {
        LocalDateTime now = LocalDateTime.parse("2021-11-22T00:30:00");
        var input = input(
            Set.of(simpleRoute()),
            Set.of(simpleSchedule(List.of(DayOfWeek.values()), "hash")),
            Set.of(
                trip(101, 10, "2021-11-22", 111, 112),
                trip(102, 10, "2021-11-23", 121, 122),
                trip(103, 10, "2021-11-24", 131, 132),
                trip(104, 10, "2021-11-25", 141, 142),
                trip(105, 10, "2021-11-26", 151, 152),
                trip(106, 10, "2021-11-27", 161, 162)
            ),
            Set.of(
                transportation(101, 111, 112, "2021-11-20T21:00:00", "2021-11-22T12:00:00", "hash"),
                transportation(102, 121, 122, "2021-11-21T21:00:00", "2021-11-23T12:00:00", "hash"),
                transportation(103, 131, 132, "2021-11-22T21:00:00", "2021-11-24T12:00:00", "hash"),
                transportation(104, 141, 142, "2021-11-23T21:00:00", "2021-11-25T12:00:00", "hash"),
                transportation(105, 151, 152, "2021-11-24T21:00:00", "2021-11-26T12:00:00", "hash"),
                transportation(106, 161, 162, "2021-11-25T21:00:00", "2021-11-27T12:00:00", "hash")
            )
        );
        var output = tripsRefresher.updateAll(input, now);

        Assertions.assertThat(output.getForCreate()).hasSize(1);
        Assertions.assertThat(output.getForUpdate()).isEmpty();
        Assertions.assertThat(output.getForCancel()).isEmpty();

        assertTripDates(output.getForCreate(), Set.of(LocalDate.parse("2021-11-28")));
    }

    @Test
    @DisplayName("Изменился хеш. Незапущенные рейсы со старым хешом обновились")
    void testUpdateAllNonLaunchedTrips() {
        when(tmProperties.getDaysToPlanFor(eq(RouteScheduleType.LINEHAUL))).thenReturn(4);
        LocalDateTime now = LocalDateTime.parse("2021-11-22T08:00:00");
        var input = input(
            Set.of(simpleRoute()),
            Set.of(simpleSchedule(List.of(DayOfWeek.values()), "newHash")),
            Set.of(
                trip(101, 10, "2021-11-22", 111, 112),
                trip(102, 10, "2021-11-23", 121, 122),
                trip(103, 10, "2021-11-24", 131, 132),
                trip(104, 10, "2021-11-25", 141, 142)
            ),
            Set.of(
                transportation(101, 111, 112, "2021-11-21T21:00:00", "2021-11-22T12:00:00", "launched"),
                transportation(102, 121, 122, "2021-11-22T21:00:00", "2021-11-23T12:00:00", "hash"),
                transportation(103, 131, 132, "2021-11-23T21:00:00", "2021-11-24T12:00:00", "newHash"),
                transportation(104, 141, 142, "2021-11-24T21:00:00", "2021-11-25T12:00:00", "hash")
            )
        );
        var output = tripsRefresher.updateAll(input, now);

        Assertions.assertThat(output.getForCreate()).isEmpty();
        Assertions.assertThat(output.getForUpdate()).hasSize(2);
        Assertions.assertThat(output.getForCancel()).isEmpty();

        Assertions.assertThat(output.getForUpdate())
            .extracting(UpdatedTransportation::getTransportation)
            .extracting(Transportation::getId)
            .containsExactlyInAnyOrder(102L, 104L);
    }

    @Test
    @DisplayName("Убрали выходные дни из расписания. Субота запущена, воскресенье пометилось удаленным")
    void testMarkDeleted() {
        when(tmProperties.getDaysToPlanFor(eq(RouteScheduleType.LINEHAUL))).thenReturn(4);
        LocalDateTime now = LocalDateTime.parse("2021-11-20T08:00:00");
        var input = input(
            Set.of(simpleRoute()),
            Set.of(simpleSchedule(List.of(DayOfWeek.MONDAY), "hash")),
            Set.of(
                trip(101, 10, "2021-11-20", 111, 112), // суббота
                trip(102, 10, "2021-11-21", 121, 122), // воскресенье
                trip(103, 10, "2021-11-22", 131, 132)  // понедельник
            ),
            Set.of(
                transportation(101, 111, 112, "2021-11-19T21:00:00", "2021-11-20T12:00:00", "hash"),
                transportation(102, 121, 122, "2021-11-20T21:00:00", "2021-11-21T12:00:00", "hash"),
                transportation(103, 131, 132, "2021-11-21T21:00:00", "2021-11-22T12:00:00", "hash")
            )
        );
        var output = tripsRefresher.updateAll(input, now);

        Assertions.assertThat(output.getForCreate()).isEmpty();
        Assertions.assertThat(output.getForUpdate()).isEmpty();
        Assertions.assertThat(output.getForCancel()).hasSize(1);

        Assertions.assertThat(output.getForCancel())
            .extracting(Transportation::getId)
            .containsExactlyInAnyOrder(102L);
    }

    @Test
    @DisplayName("Поменяли ср-чт на чт-пт и время. Среда удалилась, четверг поменялся, пятница добавилась")
    void testAllCases() {
        LocalDateTime now = LocalDateTime.parse("2021-11-20T08:00:00");
        var input = input(
            Set.of(simpleRoute()),
            Set.of(simpleSchedule(List.of(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), "newHash")),
            Set.of(
                trip(101, 10, "2021-11-24",  111, 112), // среда
                trip(102, 10, "2021-11-25",  121, 122)  // четверг
            ),
            Set.of(
                transportation(101, 111, 112, "2021-11-24T05:00:00", "2021-11-24T12:00:00", "hash"),
                transportation(102, 121, 122, "2021-11-25T05:00:00", "2021-11-25T12:00:00", "hash")
            )
        );
        var output = tripsRefresher.updateAll(input, now);

        Assertions.assertThat(output.getForCreate()).hasSize(1);
        Assertions.assertThat(output.getForUpdate()).hasSize(1);
        Assertions.assertThat(output.getForCancel()).hasSize(1);


        assertTripDates(output.getForCreate(), Set.of(LocalDate.parse("2021-11-26")));
        Assertions.assertThat(output.getForUpdate().get(0).getTransportation().getId()).isEqualTo(102L);
        Assertions.assertThat(output.getForCancel().get(0).getId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("7/0 расписание ограничено одной неделей + праздничные дни во вт и пт-сб")
    void testHolidaysStartEndDays() {
        when(tmProperties.getDaysToPlanFor(eq(RouteScheduleType.LINEHAUL))).thenReturn(15);
        LocalDateTime now = LocalDateTime.parse("2021-11-20T10:00:00");
        var input = input(
            Set.of(simpleRoute()), Set.of(scheduleWithHolidays()), Set.of(), Set.of()
        );
        var output = tripsRefresher.updateAll(input, now);

        Assertions.assertThat(output.getForCreate()).hasSize(4);
        Assertions.assertThat(output.getForUpdate()).isEmpty();
        Assertions.assertThat(output.getForCancel()).isEmpty();

        assertTripDates(output.getForCreate(), List.of(
            LocalDate.parse("2021-11-22"), // понедельник
            LocalDate.parse("2021-11-24"), // среда
            LocalDate.parse("2021-11-27"), // суббота
            LocalDate.parse("2021-11-28")  // воскресенье
        ));
    }

    private TripsRefresherInputData input(
        Set<Route> routes,
        Set<RouteSchedule> schedules,
        Set<Trip> trips,
        Set<Transportation> transportations
    ) {
        return new TripsRefresherInputData(routes, schedules, trips, transportations);
    }

    private void assertTripDates(Collection<TripCreateEntity> createEntities, Collection<LocalDate> dates) {
        List<Transportation> transportations = StreamEx.of(createEntities)
            .flatMap(trip -> trip.getTransportations().stream())
            .toList();

        Assertions.assertThat(transportations)
            .extracting(Transportation::getOutboundUnit)
            .extracting(TransportationUnit::getPlannedIntervalStart)
            .extracting(LocalDateTime::toLocalDate)
            .containsExactlyElementsOf(dates);
    }

    private Route simpleRoute() {
        return route(1, pair(0, 10, 100, 1, 11, 111));
    }

    private RouteSchedule simpleSchedule(List<DayOfWeek> daysOfWeek, String hash) {
        return scheduleWithHolidays(daysOfWeek, hash, LocalDate.parse("2021-11-01"), null, null, null);
    }

    private RouteSchedule scheduleWithHolidays() {
        return scheduleWithHolidays(
            List.of(DayOfWeek.values()),
            "hash",
            LocalDate.parse("2021-11-22"),
            LocalDate.parse("2021-11-28"),
            Set.of(LocalDate.parse("2021-11-23"), LocalDate.parse("2021-11-25"), LocalDate.parse("2021-11-26")),
            null
        );
    }

    private RouteSchedule scheduleWithHolidays(
        List<DayOfWeek> daysOfWeek,
        String hash,
        LocalDate start,
        LocalDate end,
        Set<LocalDate> holidays,
        CompositeCourierId compositeCourierId
    ) {
        return scheduleWithHolidays(
            daysOfWeek,
            hash,
            start,
            end,
            holidays,
            compositeCourierId,
            RouteScheduleType.LINEHAUL
        );
    }

    private RouteSchedule scheduleWithHolidays(
        List<DayOfWeek> daysOfWeek,
        String hash,
        LocalDate start,
        LocalDate end,
        Set<LocalDate> holidays,
        CompositeCourierId compositeCourierId,
        RouteScheduleType type
    ) {
        return schedule(
            10,
            1L,
            type,
            1003937L,
            2000L,
            daysOfWeek,
            hash,
            start,
            end,
            holidays,
            null,
            compositeCourierId,
            sPoint(0, LocalTime.of(12, 0), LocalTime.of(13, 0)),
            sPoint(1, LocalTime.of(15, 0), LocalTime.of(15, 30))
        );
    }

    private Collection<LocalDate> getAllDatesInclusive(String from, String to) {
        return LocalDate.parse(from).datesUntil(LocalDate.parse(to).plusDays(1)).collect(Collectors.toList());
    }
}
