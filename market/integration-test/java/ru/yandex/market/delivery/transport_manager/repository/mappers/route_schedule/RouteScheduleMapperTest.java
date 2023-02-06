package ru.yandex.market.delivery.transport_manager.repository.mappers.route_schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteSchedule;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteSchedulePoint;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteScheduleStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteScheduleSubtype;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteScheduleType;

@DatabaseSetup(
    value = {
        "/repository/route/full_routes.xml",
        "/repository/route_schedule/full_schedules.xml",
        "/repository/route_schedule/holidays.xml"
    }
)
class RouteScheduleMapperTest extends AbstractContextualTest {

    @Autowired
    private RouteScheduleMapper mapper;

    private static final List<RouteSchedule> SCHEDULES = List.of(schedule100(), schedule101(), schedule102(),
        schedule103());

    @Test
    void findIdsByStatus() {
        softly.assertThat(
                mapper.findRelevantIdsByStatus(LocalDate.parse("2021-12-01"), RouteScheduleStatus.ACTIVE, 7)
            )
            .containsExactlyInAnyOrder(
                SCHEDULES.get(1).getId(), SCHEDULES.get(2).getId(), SCHEDULES.get(3).getId()
            );
    }

    @Test
    void getByIds() {
        softly.assertThat(mapper.getByIds(List.of(100L, 101L, 102L))).containsExactlyInAnyOrder(
            SCHEDULES.get(0), SCHEDULES.get(1), SCHEDULES.get(2)
        );
    }

    @Test
    void getByRouteIdWithoutEmptyIntervals() {
        softly.assertThat(mapper.getByRouteId(20L, true)).containsExactlyInAnyOrder(
            SCHEDULES.get(0), SCHEDULES.get(1)
        );
    }

    @Test
    void getByRouteIdAll() {
        softly.assertThat(mapper.getByRouteId(20L, false)).containsExactlyInAnyOrder(
            SCHEDULES.get(0), SCHEDULES.get(1), SCHEDULES.get(3)
        );
    }

    private static RouteSchedule schedule100() {
        return RouteSchedule.builder()
            .id(100L)
            .name("schedule-1")
            .routeId(20L)
            .type(RouteScheduleType.LINEHAUL)
            .subtype(RouteScheduleSubtype.MAIN)
            .status(RouteScheduleStatus.TURNED_OFF)
            .price(2000L)
            .points(Set.of(
                RouteSchedulePoint.builder()
                    .id(101L)
                    .routeScheduleId(100L)
                    .index(0)
                    .daysOffset(0)
                    .timeFrom(LocalTime.of(10, 0))
                    .timeTo(LocalTime.of(11, 0))
                    .maxPallets(10)
                    .calendaringServiceId(101L)
                    .build(),
                RouteSchedulePoint.builder()
                    .id(102L)
                    .routeScheduleId(100L)
                    .index(1)
                    .daysOffset(0)
                    .timeFrom(LocalTime.of(12, 0))
                    .timeTo(LocalTime.of(13, 0))
                    .maxPallets(null)
                    .calendaringServiceId(102L)
                    .build()
            ))
            .holidays(Set.of(
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2022, 1, 1)
            ))
            .movingPartnerId(1L)
            .maxPallet(15)
            .daysOfWeek(List.of(DayOfWeek.MONDAY))
            .startDate(LocalDate.of(2021, 11, 4))
            .endDate(LocalDate.of(2021, 11, 10))
            .segmentId(123456L)
            .hash("abcd")
            .created(LocalDateTime.of(2021, 11, 1, 12, 30))
            .updated(LocalDateTime.of(2021, 11, 2, 12, 30))
            .build();
    }

    private static RouteSchedule schedule101() {
        return RouteSchedule.builder()
            .id(101L)
            .name("schedule-2")
            .routeId(20L)
            .type(RouteScheduleType.LINEHAUL)
            .subtype(RouteScheduleSubtype.SUPPLEMENTARY_1)
            .status(RouteScheduleStatus.ACTIVE)
            .holidays(Set.of())
            .points(Set.of(
                RouteSchedulePoint.builder()
                    .id(111L)
                    .routeScheduleId(101L)
                    .index(0)
                    .daysOffset(0)
                    .timeFrom(LocalTime.of(10, 0))
                    .timeTo(LocalTime.of(11, 0))
                    .maxPallets(33)
                    .build(),
                RouteSchedulePoint.builder()
                    .id(112L)
                    .routeScheduleId(101L)
                    .index(1)
                    .daysOffset(0)
                    .timeFrom(LocalTime.of(12, 0))
                    .timeTo(LocalTime.of(13, 0))
                    .build()
            ))
            .price(3000L)
            .daysOfWeek(
                List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
            )
            .startDate(LocalDate.of(2010, 1, 1))
            .segmentId(123456L)
            .hash("abcde")
            .created(LocalDateTime.of(2021, 11, 1, 12, 30))
            .updated(LocalDateTime.of(2021, 11, 2, 12, 30))
            .build();
    }

    private static RouteSchedule schedule102() {
        return RouteSchedule.builder()
            .id(102L)
            .name("schedule-3")
            .routeId(30L)
            .type(RouteScheduleType.LINEHAUL)
            .subtype(RouteScheduleSubtype.UNSCHEDULED)
            .status(RouteScheduleStatus.ACTIVE)
            .holidays(Set.of())
            .points(Set.of(
                RouteSchedulePoint.builder()
                    .id(121L)
                    .routeScheduleId(102L)
                    .index(0)
                    .daysOffset(0)
                    .timeFrom(LocalTime.of(10, 0))
                    .timeTo(LocalTime.of(11, 0))
                    .maxPallets(1)
                    .build(),
                RouteSchedulePoint.builder()
                    .id(122L)
                    .routeScheduleId(102L)
                    .index(1)
                    .daysOffset(0)
                    .timeFrom(LocalTime.of(12, 0))
                    .timeTo(LocalTime.of(13, 0))
                    .maxPallets(2)
                    .build(),
                RouteSchedulePoint.builder()
                    .id(123L)
                    .routeScheduleId(102L)
                    .index(2)
                    .daysOffset(1)
                    .timeFrom(LocalTime.of(15, 0))
                    .timeTo(LocalTime.of(17, 0))
                    .build(),
                RouteSchedulePoint.builder()
                    .id(124L)
                    .routeScheduleId(102L)
                    .index(3)
                    .daysOffset(1)
                    .timeFrom(LocalTime.of(15, 0))
                    .timeTo(LocalTime.of(17, 0))
                    .build()
            ))
            .movingPartnerId(1L)
            .maxPallet(33)
            .daysOfWeek(Arrays.asList(DayOfWeek.values()))
            .startDate(LocalDate.of(2021, 12, 8))
            .endDate(LocalDate.of(2021, 12, 8))
            .segmentId(6543221L)
            .hash("dcba")
            .created(LocalDateTime.of(2021, 11, 1, 12, 30))
            .updated(LocalDateTime.of(2021, 11, 2, 12, 30))
            .build();
    }

    private static RouteSchedule schedule103() {
        return RouteSchedule.builder()
            .id(103L)
            .name("schedule-4")
            .routeId(20L)
            .type(RouteScheduleType.LINEHAUL)
            .status(RouteScheduleStatus.ACTIVE)
            .holidays(Set.of())
            .subtype(RouteScheduleSubtype.MAIN)
            .points(Set.of(
                RouteSchedulePoint.builder()
                    .id(125L)
                    .routeScheduleId(103L)
                    .index(0)
                    .daysOffset(0)
                    .timeFrom(LocalTime.of(10, 0))
                    .timeTo(LocalTime.of(11, 0))
                    .build(),
                RouteSchedulePoint.builder()
                    .id(126L)
                    .routeScheduleId(103L)
                    .index(1)
                    .daysOffset(0)
                    .timeFrom(LocalTime.of(12, 0))
                    .timeTo(LocalTime.of(13, 0))
                    .build(),
                RouteSchedulePoint.builder()
                    .id(127L)
                    .routeScheduleId(103L)
                    .index(2)
                    .daysOffset(1)
                    .timeFrom(LocalTime.of(15, 0))
                    .timeTo(LocalTime.of(17, 0))
                    .build(),
                RouteSchedulePoint.builder()
                    .id(128L)
                    .routeScheduleId(103L)
                    .index(3)
                    .daysOffset(1)
                    .timeFrom(LocalTime.of(15, 0))
                    .timeTo(LocalTime.of(17, 0))
                    .build()
            ))
            .movingPartnerId(1L)
            .maxPallet(33)
            .daysOfWeek(Arrays.asList(DayOfWeek.values()))
            .startDate(LocalDate.of(2021, 12, 8))
            .endDate(LocalDate.of(2021, 12, 7))
            .segmentId(6543221L)
            .hash("dcba")
            .created(LocalDateTime.of(2021, 11, 1, 12, 30))
            .updated(LocalDateTime.of(2021, 11, 2, 12, 30))
            .build();
    }


}
