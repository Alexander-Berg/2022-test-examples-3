package ru.yandex.market.logistics.lom.model.utils;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import yandex.market.combinator.common.Common;
import yandex.market.combinator.v0.CombinatorOuterClass;

import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.PointType;
import ru.yandex.market.logistics.lom.model.enums.ServiceCodeName;

@UtilityClass
@ParametersAreNonnullByDefault
public class CombinatorFactory {
    private static final Set<PointType> SEGMENT_TYPES_WITH_LOGISTIC_POINT = EnumSet.of(
        PointType.WAREHOUSE,
        PointType.PICKUP
    );

    private static final Map<ServiceCodeName, CombinatorOuterClass.DeliveryService.ServiceType> SERVICE_MAPPING =
        Map.of(
            ServiceCodeName.PROCESSING, CombinatorOuterClass.DeliveryService.ServiceType.INTERNAL,
            ServiceCodeName.SORT, CombinatorOuterClass.DeliveryService.ServiceType.INTERNAL,
            ServiceCodeName.SHIPMENT, CombinatorOuterClass.DeliveryService.ServiceType.OUTBOUND,
            ServiceCodeName.INBOUND, CombinatorOuterClass.DeliveryService.ServiceType.INBOUND,
            ServiceCodeName.MOVEMENT, CombinatorOuterClass.DeliveryService.ServiceType.INTERNAL,
            ServiceCodeName.DELIVERY, CombinatorOuterClass.DeliveryService.ServiceType.INTERNAL,
            ServiceCodeName.LAST_MILE, CombinatorOuterClass.DeliveryService.ServiceType.INTERNAL,
            ServiceCodeName.HANDING, CombinatorOuterClass.DeliveryService.ServiceType.OUTBOUND
        );

    private static final Map<Pair<PartnerType, PointType>, List<ServiceCodeName>> SERVICES_BY_PARTNER_AND_SEGMENT =
        Map.of(
            Pair.of(PartnerType.FULFILLMENT, PointType.WAREHOUSE),
            List.of(ServiceCodeName.PROCESSING, ServiceCodeName.SHIPMENT),
            Pair.of(PartnerType.DELIVERY, PointType.MOVEMENT),
            List.of(ServiceCodeName.INBOUND, ServiceCodeName.MOVEMENT, ServiceCodeName.SHIPMENT),
            Pair.of(PartnerType.SORTING_CENTER, PointType.MOVEMENT),
            List.of(ServiceCodeName.INBOUND, ServiceCodeName.MOVEMENT, ServiceCodeName.SHIPMENT),
            Pair.of(PartnerType.SORTING_CENTER, PointType.WAREHOUSE),
            List.of(ServiceCodeName.INBOUND, ServiceCodeName.SORT, ServiceCodeName.SHIPMENT),
            Pair.of(PartnerType.DELIVERY, PointType.LINEHAUL),
            List.of(ServiceCodeName.DELIVERY, ServiceCodeName.LAST_MILE, ServiceCodeName.SHIPMENT),
            Pair.of(PartnerType.DELIVERY, PointType.HANDING),
            List.of(ServiceCodeName.HANDING),
            Pair.of(PartnerType.DELIVERY, PointType.PICKUP),
            List.of(ServiceCodeName.INBOUND, ServiceCodeName.HANDING)
        );

    public static class ExampleRoute {
        public static final List<Triple<Long, PointType, PartnerType>> FF_SC_COURIER = List.of(
            Triple.of(172L, PointType.WAREHOUSE, PartnerType.FULFILLMENT),
            Triple.of(49784L, PointType.MOVEMENT, PartnerType.SORTING_CENTER),
            Triple.of(49784L, PointType.WAREHOUSE, PartnerType.SORTING_CENTER),
            Triple.of(1005705L, PointType.MOVEMENT, PartnerType.DELIVERY),
            Triple.of(1005705L, PointType.LINEHAUL, PartnerType.DELIVERY),
            Triple.of(1005705L, PointType.HANDING, PartnerType.DELIVERY)
        );
    }

    @Nonnull
    public CombinatorOuterClass.DeliveryRoute deliveryRoute() {
        return deliveryRoute(ExampleRoute.FF_SC_COURIER);
    }

    @Nonnull
    public CombinatorOuterClass.DeliveryRoute deliveryRoute(List<Triple<Long, PointType, PartnerType>> points) {
        return CombinatorOuterClass.DeliveryRoute.newBuilder()
            .addAllOffers(offers())
            .setRoute(route(points))
            .setVirtualBox(box())
            .setDeliveryDates(deliveryDates())
            .setStringDeliveryDates(stringDeliveryDates())
            .addAllPackingBoxes(List.of(box(), box()))
            .setPromise("go_promise")
            .setError("Some error!")
            .setDeliverySubtype(CombinatorOuterClass.DeliverySubtype.ORDINARY)
            .addAllRouteDebugMessages(List.of(
                CombinatorOuterClass.RouteDebugMessage.newBuilder().setDescription("Warn 1").build(),
                CombinatorOuterClass.RouteDebugMessage.newBuilder().setDescription("Info 2").build()
            ))
            .build();
    }

    @Nonnull
    public List<CombinatorOuterClass.Offer> offers() {
        return List.of(
            CombinatorOuterClass.Offer.newBuilder()
                .setShopSku("217176139.alisa3p")
                .setShopId(10427354)
                .setPartnerId(172)
                .setAvailableCount(100185)
                .setFeedId(475690)
                .addAllCategoryIds(List.of(1L))
                .addAllCargoTypes(List.of(950, 910))
                .build()
        );
    }

    @Nonnull
    public CombinatorOuterClass.Box box() {
        return CombinatorOuterClass.Box.newBuilder()
            .setWeight(100)
            .addAllDimensions(List.of(10, 10, 10))
            .build();
    }

    @Nonnull
    public CombinatorOuterClass.DeliveryDates deliveryDates() {
        return CombinatorOuterClass.DeliveryDates.newBuilder()
            .setShipmentDate(timestamp())
            .setPackagingTime(97200)
            .setShipmentBySupplier(timestamp())
            .setReceptionByWarehouse(timestamp())
            .setLastWarehouseOffset(CombinatorOuterClass.ShipmentDayOffsetWarehouse.newBuilder().setOffset(-1).build())
            .build();
    }

    @Nonnull
    public Timestamp timestamp() {
        return Timestamp.newBuilder().setSeconds(1650056400).setNanos(123).build();
    }

    @Nonnull
    public Duration duration() {
        return Duration.newBuilder().setSeconds(800).setNanos(345).build();
    }

    @Nonnull
    public CombinatorOuterClass.StringDeliveryDates stringDeliveryDates() {
        return CombinatorOuterClass.StringDeliveryDates.newBuilder()
            .setShipmentDate("2022-04-16")
            .setPackagingTime("PT27H0M")
            .setShipmentBySupplier("2022-04-16T00:00:00+03:00")
            .setReceptionByWarehouse("2022-04-16T00:00:00+03:00")
            .setLastWarehouseOffset(CombinatorOuterClass.ShipmentDayOffsetWarehouse.newBuilder().setOffset(-1).build())
            .build();
    }

    @Nonnull
    public CombinatorOuterClass.Route route(List<Triple<Long, PointType, PartnerType>> points) {
        return CombinatorOuterClass.Route.newBuilder()
            .setDeliveryType(Common.DeliveryType.COURIER)
            .addAllPoints(
                points.stream()
                    .map(p -> point(p.getLeft(), p.getMiddle(), p.getRight()))
                    .collect(Collectors.toList())
            )
            .addAllPaths(paths(points.size()))
            .setTariffId(100528L)
            .setCost(250)
            .setCostForShop(260)
            .setDateFrom(date())
            .setDateTo(date())
            .setShipmentWarehouseId(172L)
            .setIsMarketCourier(true)
            .setAirshipDelivery(false)
            .setIsExternalLogistics(false)
            .setIsDsbsToMarketOutlet(false)
            .build();
    }

    public CombinatorOuterClass.Date date() {
        return CombinatorOuterClass.Date.newBuilder()
            .setDay(17)
            .setMonth(4)
            .setYear(2022)
            .build();
    }

    @Nonnull
    public List<CombinatorOuterClass.Route.Path> paths(int length) {
        return IntStream.range(1, length)
            .mapToObj(i -> CombinatorOuterClass.Route.Path.newBuilder().setPointFrom(i - 1).setPointTo(i).build())
            .collect(Collectors.toList());
    }

    @Nonnull
    public CombinatorOuterClass.Route.Point point(
        long partnerId,
        PointType segmentType,
        PartnerType partnerType
    ) {
        return CombinatorOuterClass.Route.Point.newBuilder()
            .setIds(pointIds(
                partnerId,
                segmentType == PointType.MOVEMENT ? 0 : 213,
                SEGMENT_TYPES_WITH_LOGISTIC_POINT.contains(segmentType)
            ))
            .setSegmentType(segmentType.name().toLowerCase())
            .addAllServices(
                CollectionUtils.emptyIfNull(SERVICES_BY_PARTNER_AND_SEGMENT.get(Pair.of(partnerType, segmentType)))
                    .stream()
                    .map(serviceCode -> service(partnerId, segmentType, serviceCode))
                    .collect(Collectors.toList())
            )
            .setSegmentId(100000 + partnerId * 20 + segmentType.ordinal())
            .setPartnerType(partnerType.name())
            .setPartnerName(partnerType.name() + " partner")
            .build();
    }

    @Nonnull
    public CombinatorOuterClass.PointIds pointIds(
        long partnerId,
        int regionId,
        boolean needLogisticPointId
    ) {
        return CombinatorOuterClass.PointIds.newBuilder()
            .setPartnerId(partnerId)
            .setLogisticPointId(needLogisticPointId ? 10000000000L + partnerId : 0)
            .setRegionId(regionId)
            .build();
    }

    @Nonnull
    public CombinatorOuterClass.DeliveryService service(
        long partnerId,
        PointType pointType,
        ServiceCodeName serviceCode
    ) {
        var builder = CombinatorOuterClass.DeliveryService.newBuilder()
            .setId(1000000L + partnerId * 1000L + serviceCode.ordinal())
            .setType(SERVICE_MAPPING.get(serviceCode))
            .setCode(serviceCode.name())
            .setCost((int) ((partnerId + serviceCode.ordinal()) % 250))
            .setStartTime(timestamp())
            .setDuration(duration())
            .setLogisticDate(date())
            .setScheduleStartTime(timestamp())
            .setScheduleEndTime(timestamp())
            .setTzOffset(pointType == PointType.HANDING && serviceCode == ServiceCodeName.HANDING ? -14400 : 0)
            .setDurationDelta(0)
            .addAllDisabledDates(List.of(10));

        if (pointType == PointType.HANDING && serviceCode == ServiceCodeName.HANDING) {
            builder.addAllDeliveryIntervals(List.of(deliveryInterval()));
        }

        if (pointType == PointType.WAREHOUSE && serviceCode == ServiceCodeName.SHIPMENT) {
            builder.addAllServiceMeta(List.of(serviceMeta()));
        }

        return builder.build();
    }

    @Nonnull
    public CombinatorOuterClass.DeliveryInterval deliveryInterval() {
        return CombinatorOuterClass.DeliveryInterval.newBuilder()
            .setFrom(CombinatorOuterClass.Time.newBuilder().setHour(10).setMinute(0).build())
            .setTo(CombinatorOuterClass.Time.newBuilder().setHour(18).setMinute(30).build())
            .build();
    }

    @Nonnull
    public CombinatorOuterClass.DeliveryService.ServiceMeta serviceMeta() {
        return CombinatorOuterClass.DeliveryService.ServiceMeta.newBuilder()
            .setKey("key")
            .setValue("value")
            .build();
    }
}
