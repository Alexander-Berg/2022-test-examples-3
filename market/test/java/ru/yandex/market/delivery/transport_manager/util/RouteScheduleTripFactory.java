package ru.yandex.market.delivery.transport_manager.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import lombok.experimental.UtilityClass;

import ru.yandex.market.delivery.transport_manager.domain.entity.CompositeCourierId;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.Route;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.RoutePoint;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.RoutePointPair;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.RoutePointType;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.RouteStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteSchedule;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteScheduleAdditionalData;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteSchedulePoint;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteScheduleStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteScheduleType;
import ru.yandex.market.delivery.transport_manager.domain.entity.trip.Trip;
import ru.yandex.market.delivery.transport_manager.domain.entity.trip.TripPoint;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;

@SuppressWarnings({"HideUtilityClassConstructor", "ParameterNumber"})
@UtilityClass
public class RouteScheduleTripFactory {

    public static Route route(long id, RoutePointPair... pairs) {
        return Route.builder()
            .id(id)
            .status(RouteStatus.ACTIVE)
            .pointPairs(List.of(pairs))
            .build();
    }

    public static RoutePointPair pair(
        int fromIndex, long fromPartner, long fromPoint,
        int toIndex, long toPartner, long toPoint
    ) {
        return new RoutePointPair()
            .setOutboundPoint(
                RoutePoint.builder()
                    .index(fromIndex)
                    .partnerId(fromPartner)
                    .logisticsPointId(fromPoint)
                    .type(RoutePointType.OUTBOUND)
                    .build()
            )
            .setInboundPoint(
                RoutePoint.builder()
                    .index(toIndex)
                    .partnerId(toPartner)
                    .logisticsPointId(toPoint)
                    .type(RoutePointType.INBOUND)
                    .build()
            );
    }

    public static RouteSchedule schedule(long id, long routeId) {
        return schedule(id, routeId, RouteScheduleType.LINEHAUL, null, Arrays.asList(DayOfWeek.values()), null);
    }

    public static RouteSchedule schedule(
        long id,
        long routeId,
        RouteScheduleType type,
        Long movingPartner,
        List<DayOfWeek> dayOfWeeks,
        String hash,
        RouteSchedulePoint... points
    ) {
        return schedule(id, routeId, type, movingPartner, null, dayOfWeeks, hash, null, null, points);
    }

    public static RouteSchedule schedule(
        long id,
        long routeId,
        RouteScheduleType type,
        Long movingPartner,
        Long price,
        List<DayOfWeek> dayOfWeeks,
        String hash,
        String runID,
        CompositeCourierId courier,
        RouteSchedulePoint... points
    ) {
        return schedule(
            id, routeId, type, movingPartner, price, dayOfWeeks, hash,
            LocalDate.parse("2021-11-01"), null, null, runID, courier, points
        );
    }

    public static RouteSchedule schedule(
        long id,
        long routeId,
        RouteScheduleType type,
        Long movingPartner,
        Long price,
        List<DayOfWeek> dayOfWeeks,
        String hash,
        LocalDate startDate,
        LocalDate endDate,
        Set<LocalDate> holidays,
        String runId,
        CompositeCourierId courier,
        RouteSchedulePoint... points
    ) {
        return RouteSchedule.builder()
            .id(id)
            .status(RouteScheduleStatus.ACTIVE)
            .routeId(routeId)
            .type(type)
            .movingPartnerId(movingPartner)
            .price(price)
            .daysOfWeek(dayOfWeeks)
            .points(Set.of(points))
            .startDate(startDate)
            .endDate(endDate)
            .holidays(holidays)
            .hash(hash)
            .additionalData(RouteScheduleAdditionalData.builder().runId(runId).courier(courier).build())
            .build();
    }

    public static RouteSchedulePoint sPoint(int index, LocalTime from, LocalTime to) {
        return sPoint(index, from, to, 0);
    }

    public static RouteSchedulePoint sPoint(int index, LocalTime from, LocalTime to, int offset) {
        return RouteSchedulePoint.builder()
            .routeScheduleId(0L)
            .index(index)
            .daysOffset(offset)
            .timeFrom(from)
            .timeTo(to)
            .build();
    }

    public static Trip trip(long id, long scheduleId, String date, long outboundId, long inboundId) {
        return trip(id, scheduleId, LocalDate.parse(date), outboundId, inboundId);
    }

    public static Trip trip(long id, long scheduleId, LocalDate date, long outboundId, long inboundId) {
        return trip(id, scheduleId, date, point(0, outboundId), point(1, inboundId));
    }

    public static Trip trip(Long id, long scheduleId, LocalDate date, TripPoint... points) {
        return new Trip()
            .setStartDate(date)
            .setId(id)
            .setRouteScheduleId(scheduleId)
            .setPoints(Set.of(points));
    }

    public static TripPoint point(int index, long unitId) {
        return TripPoint.builder()
            .index(index)
            .transportationUnitId(unitId)
            .build();
    }

    public static Transportation transportation(long id, long outboundId, long inboundId) {
        return transportation(id, outboundId, inboundId, null, null, null);
    }

    public static Transportation transportation(
        long id,
        long outboundId,
        long inboundId,
        String outboundTime,
        String hash
    ) {
        return new Transportation()
            .setId(id)
            .setStatus(TransportationStatus.SCHEDULED)
            .setTransportationType(TransportationType.LINEHAUL)
            .setHash(hash)
            .setOutboundUnit(
                new TransportationUnit()
                    .setId(outboundId)
                    .setPlannedIntervalStart(parseDateTime(outboundTime))
            )
            .setMovement(
                new Movement()
            )
            .setInboundUnit(
                new TransportationUnit()
                    .setId(inboundId)
            );
    }

    public static Transportation transportation(
        long id,
        long outboundId,
        long inboundId,
        String launchTime,
        String outboundTime,
        String hash
    ) {
        return new Transportation()
            .setId(id)
            .setStatus(TransportationStatus.DRAFT)
            .setTransportationType(TransportationType.LINEHAUL)
            .setPlannedLaunchTime(parseDateTime(launchTime))
            .setHash(hash)
            .setOutboundUnit(
                new TransportationUnit()
                    .setId(outboundId)
                    .setPlannedIntervalStart(parseDateTime(outboundTime))
            )
            .setMovement(
                new Movement()
            )
            .setInboundUnit(
                new TransportationUnit()
                    .setId(inboundId)
            );
    }

    private static LocalDateTime parseDateTime(@Nullable String dateTime) {
        return Optional.ofNullable(dateTime).map(LocalDateTime::parse).orElse(null);
    }
}
