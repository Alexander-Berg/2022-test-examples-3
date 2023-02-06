package ru.yandex.market.tpl.core.external.routing.vrp;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import one.util.streamex.EntryStream;
import one.util.streamex.MoreCollectors;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.domain.routing.tag.RequiredRoutingTag;
import ru.yandex.market.tpl.core.external.routing.api.DimensionsClass;
import ru.yandex.market.tpl.core.external.routing.api.RoutingAddress;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingCourierVehicleType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingDepot;
import ru.yandex.market.tpl.core.external.routing.api.RoutingGeoPoint;
import ru.yandex.market.tpl.core.external.routing.api.RoutingLocationType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItem;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItemType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResponseItem;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResponseItemTask;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResponseRoutePoint;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResult;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResultShift;
import ru.yandex.market.tpl.core.external.routing.api.RoutingScheduleData;
import ru.yandex.market.tpl.core.external.routing.api.RoutingTimeMultiplier;
import ru.yandex.market.tpl.core.external.routing.api.RoutingTimeMultiplierUtil;
import ru.yandex.market.tpl.core.external.routing.util.MultiOrderIdUtil;
import ru.yandex.market.tpl.core.external.routing.vrp.mapper.MvrpResponseItemMapper;
import ru.yandex.market.tpl.core.external.routing.vrp.model.OrderLocation;

/**
 * @author ungomma
 */
public final class RoutingApiDataHelper {

    public static final long DEPOT_ID = 1L;
    public static final double LAT = 55.700182;
    public static final double LON = 37.580158;
    public static final String INTERVAL = "10:00:00-12:00:00";
    public static final RoutingTimeMultiplier DEFAULT_ROUTING_REQUEST_CAR = RoutingTimeMultiplierUtil.DEFAULT_CAR;

    public final Random random = new Random(7L);

    private final MvrpResponseItemMapper customItemMapper;
    private int orderCounter;

    public RoutingApiDataHelper() {
        this.customItemMapper = null;
    }

    public RoutingApiDataHelper(MvrpResponseItemMapper customItemMapper) {
        this.customItemMapper = customItemMapper;
    }

    public RoutingRequest getRoutingRequest(long userId, LocalDate date, int visitedNum) {
        return getRoutingRequest(userId, date, Integer.MAX_VALUE, visitedNum);
    }

    public RoutingRequest getRoutingRequest(long userId, LocalDate date, int orderCount, int visitedNum) {
        ZoneOffset zoneId = DateTimeUtil.DEFAULT_ZONE_ID;

        Map<RoutingRequestItem, LocalTime> items = getRoutingRequestItemLocalTimeMap(orderCount);

        RelativeTimeInterval shiftInterval = RelativeTimeInterval.valueOf("10:00-22:00");
        Duration singleDuration = shiftInterval.getDuration().dividedBy(Math.max(items.size(), 1));
        Duration serviceTime = Duration.ofSeconds(Math.min(singleDuration.getSeconds() / 2, 300));

        Function<LocalTime, Instant> toInstant = time -> time.atDate(date).toInstant(zoneId);

        RoutingDepot depot = new RoutingDepot(DEPOT_ID,
                RoutingGeoPoint.ofLatLon(new BigDecimal("37.716980"), new BigDecimal("55.741526")),
                LocalTimeInterval.valueOf("09:00-22:00")
        );

        RoutingCourier courier = EntryStream.of(items)
                .limit(visitedNum)
                .collect(MoreCollectors.last())
                .map(pair -> RoutingCourier.builder()
                                .id(userId)
                                .ref("user-" + userId)
                                .depotId(depot.getId())
                                .scheduleData(new RoutingScheduleData(RoutingCourierVehicleType.NONE, shiftInterval))
                                .routingTimeMultiplier(RoutingTimeMultiplierUtil.DEFAULT_NONE)
                                .additionalTags(Set.of())
                                .excludedTags(Set.of())
                                .lastKnownLocation(pair.getKey().getAddress().getGeoPoint())
                                .locationTime(toInstant.apply(pair.getValue()).plus(serviceTime))
                                .taskIdsWithFixedOrder(List.of())
                                .plannedTaskIds(List.of())
                                .servicedLocationType(RoutingLocationType.delivery)
                                .priority(100)
                                .vehicleId(1L)
                                .partnerId(1L)
                                .maximalStops(10)
                                .build()
                )
                .orElse(RoutingCourier.builder()
                        .id(userId)
                        .ref("user-" + userId)
                        .depotId(depot.getId())
                        .scheduleData(new RoutingScheduleData(RoutingCourierVehicleType.NONE, shiftInterval))
                        .routingTimeMultiplier(RoutingTimeMultiplierUtil.DEFAULT_NONE)
                        .additionalTags(Set.of())
                        .excludedTags(Set.of())
                        .taskIdsWithFixedOrder(List.of())
                        .plannedTaskIds(List.of())
                        .servicedLocationType(RoutingLocationType.delivery)
                        .priority(100)
                        .vehicleId(1L)
                        .partnerId(1L)
                        .maximalStops(10)
                        .build()
                );

        return RoutingRequest.createCourierRequest(date, zoneId, RoutingMockType.MANUAL,
                courier, 0L, depot, new ArrayList<>(items.keySet()), null);
    }

    public RoutingRequest getRoutingRequest(LocalDate date, int orderCount, int courierCount) {
        ZoneOffset zoneId = DateTimeUtil.DEFAULT_ZONE_ID;

        Map<RoutingRequestItem, LocalTime> items = getRoutingRequestItemLocalTimeMap(orderCount);

        RoutingDepot depot = new RoutingDepot(DEPOT_ID,
                RoutingGeoPoint.ofLatLon(new BigDecimal("37.716980"), new BigDecimal("55.741526")),
                LocalTimeInterval.valueOf("09:00-22:00")
        );

        RelativeTimeInterval shiftInterval = RelativeTimeInterval.valueOf("10:00-22:00");
        Set<RoutingCourier> couriers = Stream.generate(() -> {
            long userId = random.nextLong();

            return RoutingCourier.builder()
                    .id(userId)
                    .ref("user-" + userId)
                    .depotId(depot.getId())
                    .scheduleData(new RoutingScheduleData(RoutingCourierVehicleType.CAR, shiftInterval))
                    .routingTimeMultiplier(RoutingTimeMultiplierUtil.DEFAULT_CAR)
                    .additionalTags(Set.of())
                    .excludedTags(Set.of())
                    .taskIdsWithFixedOrder(List.of())
                    .plannedTaskIds(List.of())
                    .servicedLocationType(RoutingLocationType.delivery)
                    .vehicleCapacity(BigDecimal.valueOf(random.nextDouble()))
                    .priority(100)
                    .vehicleId(1L)
                    .partnerId(1L)
                    .maximalStops(10)
                    .build();
        }).limit(courierCount).collect(Collectors.toSet());

        return RoutingRequest.createShiftRequest(Instant.now(), date, zoneId, RoutingMockType.MANUAL, couriers, depot,
                new ArrayList<>(items.keySet()), null, null, Set.of());
    }

    public RoutingRequest getRoutingRequestPickup(LocalDate date, int orderCount, int courierCount) {
        ZoneOffset zoneId = DateTimeUtil.DEFAULT_ZONE_ID;

        Map<RoutingRequestItem, LocalTime> items = getRoutingRequestItemLocalTimeMap(orderCount);

        RoutingDepot depot = new RoutingDepot(DEPOT_ID,
                RoutingGeoPoint.ofLatLon(new BigDecimal("37.716980"), new BigDecimal("55.741526")),
                LocalTimeInterval.valueOf("09:00-22:00")
        );

        RelativeTimeInterval shiftInterval = RelativeTimeInterval.valueOf("10:00-22:00");
        Set<RoutingCourier> couriers = Stream.generate(() -> {
            long userId = random.nextLong();
            return RoutingCourier.builder()
                    .id(userId)
                    .ref("user-" + userId)
                    .depotId(depot.getId())
                    .scheduleData(new RoutingScheduleData(RoutingCourierVehicleType.CAR, shiftInterval))
                    .routingTimeMultiplier(DEFAULT_ROUTING_REQUEST_CAR)
                    .additionalTags(Set.of())
                    .excludedTags(Set.of())
                    .taskIdsWithFixedOrder(List.of())
                    .plannedTaskIds(List.of())
                    .servicedLocationType(RoutingLocationType.pickup)
                    .vehicleCapacity(BigDecimal.valueOf(random.nextDouble()))
                    .priority(100)
                    .vehicleId(1L)
                    .partnerId(1L)
                    .maximalStops(10)
                    .build();
        }).limit(courierCount).collect(Collectors.toSet());

        return RoutingRequest.createShiftRequest(Instant.now(), date, zoneId, RoutingMockType.MANUAL, couriers, depot,
                new ArrayList<>(items.keySet()), null, null, Set.of());
    }

    private Supplier<Pair<RoutingRequestItem, LocalTime>> order(double lat, double lon, String interval,
                                                                String arrival) {
        return order(lat, lon, null, interval, arrival);
    }

    public Supplier<Pair<RoutingRequestItem, LocalTime>> order(
            double lat, double lon, String house, String interval, String arrival
    ) {
        return order(lat, lon, house, null, null, interval, arrival, null);
    }

    public Supplier<Pair<RoutingRequestItem, LocalTime>> order(
            double lat, double lon, String house, String building, String housing, String interval, String arrival
    ) {
        return order(lat, lon, house, building, housing, interval, arrival, null);

    }

    public Supplier<Pair<RoutingRequestItem, LocalTime>> order(
            double lat, double lon, String house, String building, String housing, String interval, String arrival,
            String locationGroup
    ) {
        String lastChars = StringUtils.rightPad(Double.toString(lon), 6 + 3, "0");
        String ref = "geo-" + lastChars.substring(lastChars.length() - 4);
        return () -> Pair.of(
                new RoutingRequestItem(
                        RoutingRequestItemType.CLIENT,
                        String.valueOf(orderCounter++),
                        1,
                        String.valueOf(DEPOT_ID),
                        RoutingAddress.builder()
                                .addressString("Москва, " + lon)
                                .house(house)
                                .building(building)
                                .latitude(BigDecimal.valueOf(lat))
                                .longitude(BigDecimal.valueOf(lon))
                                .housing(housing)
                                .locationGroup(locationGroup)
                                .build(),
                        RelativeTimeInterval.valueOf(interval), ref, Set.of(), Set.of(), BigDecimal.valueOf(0.3),
                        false,
                        DimensionsClass.REGULAR_CARGO,
                        123,
                        0,
                        0,
                        false,
                        false
                ),
                LocalTime.parse(arrival)
        );
    }

    private Map<RoutingRequestItem, LocalTime> getRoutingRequestItemLocalTimeMap(int orderCount) {
        return StreamEx.of(
                        order(55.692079, 37.573708, "10:00:00-18:00:00", "10:04:26"),
                        order(55.697904, 37.573007, "10:00:00-18:00:00", "10:20:11"),

                        order(LAT, LON, "10:00:00-18:00:00", "10:33:27"),
                        // one location group
                        order(LAT, LON, INTERVAL, "10:33:27"),
                        order(LAT, LON, INTERVAL, "10:33:27"),
                        order(LAT, LON, INTERVAL, "10:33:27"),
                        order(LAT, LON, INTERVAL, "10:33:27"),

                        // another location group
                        order(LAT, LON, "10:00:00-14:00:00", "10:33:27"),

                        order(55.687964, 37.586491, INTERVAL, "11:20:42"),
                        order(55.683645, 37.587649, "10:00:00-18:00:00", "11:34:28"),
                        order(55.678169, 37.580939, "10:00:00-18:00:00", "11:51:59"),
                        order(55.690506, 37.563611, "10:00:00-18:00:00", "12:23:21"),
                        order(55.691912, 37.56291, "14:00:00-16:00:00", "12:32:07"),
                        order(55.695296, 37.575648, "14:00:00-16:00:00", "12:51:24"),
                        order(55.698695, 37.580041, "10:00:00-22:00:00", "13:04:50"),
                        order(55.704103, 37.565021, INTERVAL, "14:23:20")
                ).limit(orderCount).map(Supplier::get)
                .mapToEntry(Map.Entry::getKey, Map.Entry::getValue)
                .toCustomMap(LinkedHashMap::new);
    }


    public RoutingResult mockResult(RoutingRequest rr, boolean withDroppedPoints) {
        long userId = rr.getUsers().iterator().next().getId();

        Instant startTime = rr.getRoutingDate().atTime(10, 0).atZone(rr.getZoneId()).toInstant();

        var rps = EntryStream.of(rr.getItems())
                .mapKeyValue((idx, item) -> {
                    var arrivalTime = startTime.plusSeconds(idx * 600);
                    return new RoutingResponseRoutePoint(
                            arrivalTime, arrivalTime.plusSeconds(600),
                            new RoutingAddress(item.getAddress().getAddressString(), item.getAddress().getGeoPoint()),
                            List.of(mockItem(item, arrivalTime.plusSeconds(600))),
                            item.getType() == RoutingRequestItemType.DROPSHIP ? RoutingLocationType.pickup :
                                    RoutingLocationType.delivery
                    );
                }).toList();

        RoutingResultShift shift = new RoutingResultShift(userId, rps);
        return RoutingResult.fromRequest(
                rr, "1124", Map.of(userId, shift),
                withDroppedPoints ? Map.of(rr.getItems().get(0).getTaskId(), rr.getItems().get(0)) : Map.of(),
                null
        );
    }

    private RoutingResponseItem mockItem(RoutingRequestItem requestItem, Instant expectedTime) {

        var taskNode = new OrderLocation();
        taskNode.setId(requestItem.getTaskId());
        if (customItemMapper != null && customItemMapper.isApplicable(taskNode)) {
            return customItemMapper.mapItem(taskNode, requestItem, expectedTime);
        }

        return new RoutingResponseItem(
                requestItem.getTaskId(),
                MultiOrderIdUtil.parseSubTaskIds(requestItem.getTaskId()),
                MultiOrderIdUtil.parseSubTaskIds(requestItem.getTaskId()).stream()
                        .map(it -> new RoutingResponseItemTask(it, requestItem.getType(),
                                requestItem.isLogisticRequest()))
                        .collect(Collectors.toList()),
                expectedTime
        );
    }

}
