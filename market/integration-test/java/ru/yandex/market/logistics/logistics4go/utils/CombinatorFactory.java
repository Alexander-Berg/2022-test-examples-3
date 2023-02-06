package ru.yandex.market.logistics.logistics4go.utils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import lombok.experimental.UtilityClass;

import yandex.market.combinator.common.Common;
import yandex.market.combinator.v0.CombinatorOuterClass;

import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.PICKUP_POINT_ID;
import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.SC_SEGMENT_ID;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class CombinatorFactory {

    @Nonnull
    public static CombinatorOuterClass.DeliveryRouteFromPointRequest.Builder combinatorRequestBuilder(
        boolean isOnlyRequired,
        boolean isCourier
    ) {
        Instant startTime = Instant.parse("2022-02-22T15:00:00Z");

        var builder = CombinatorOuterClass.DeliveryRouteFromPointRequest.newBuilder()
            .setAdditionalInfo(
                CombinatorOuterClass.AdditionalInfo.newBuilder()
                    .setShowDarkstore(true)
                    .build()
            )
            .addAllItems(List.of(deliveryPackageBuilder(isOnlyRequired).build()))
            .setDestination(
                CombinatorOuterClass.PointIds.newBuilder()
                    .setRegionId(213)
                    .build()
            )
            .setDeliveryType(isCourier ? Common.DeliveryType.COURIER : Common.DeliveryType.PICKUP)
            .setDateFrom(
                CombinatorOuterClass.Date.newBuilder()
                    .setDay(23)
                    .setMonth(2)
                    .setYear(2022)
                    .build()
            )
            .setDateTo(
                CombinatorOuterClass.Date.newBuilder()
                    .setDay(25)
                    .setMonth(2)
                    .setYear(2022)
                    .build()
            )
            .setStartSegmentLmsId(SC_SEGMENT_ID)
            .setInterval(
                CombinatorOuterClass.DeliveryInterval.newBuilder()
                    .setFrom(
                        CombinatorOuterClass.Time.newBuilder()
                            .setHour(10)
                            .setMinute(0)
                            .build()
                    )
                    .setTo(
                        CombinatorOuterClass.Time.newBuilder()
                            .setHour(14)
                            .setMinute(0)
                            .build()
                    )
                    .build()
            )
            .setStartTime(
                Timestamp.newBuilder()
                    .setSeconds(startTime.getEpochSecond())
                    .setNanos(startTime.getNano())
                    .build()
            );

        if (!isCourier) {
            builder
                .setDestination(
                    CombinatorOuterClass.PointIds.newBuilder()
                        .setLogisticPointId(PICKUP_POINT_ID)
                        .build()
                )
                .setDeliveryType(Common.DeliveryType.PICKUP)
                .clearInterval();
        }

        return builder;
    }

    @Nonnull
    public static CombinatorOuterClass.DeliveryRoute.Builder combinatorDeliveryRouteBuilder(boolean isCourier) {
        return CombinatorOuterClass.DeliveryRoute.newBuilder()
            .addOffers(
                CombinatorOuterClass.Offer.newBuilder()
                    .setPartnerId(54321)
                    .setAvailableCount(1)
                    .addAllCargoTypes(List.of(100, 200, 300))
                    .build()
            )
            .setRoute(combinatorRouteBuilder(isCourier))
            .setVirtualBox(box())
            .addPackingBoxes(box())
            .setDeliveryDates(
                CombinatorOuterClass.DeliveryDates.newBuilder()
                    .setShipmentDate(timestamp())
                    .setShipmentBySupplier(timestamp())
                    .setReceptionByWarehouse(timestamp())
                    .setPackagingTime(46959)
                    .build()
            )
            .setStringDeliveryDates(
                CombinatorOuterClass.StringDeliveryDates.newBuilder()
                    .setShipmentDate("2022-03-15")
                    .setPackagingTime("PT13H159M")
                    .setShipmentBySupplier("2022-03-15T00:00:00+03:00")
                    .setReceptionByWarehouse("2022-03-15T00:00:00+03:00")
                    .build()
            );
    }

    @Nonnull
    public static CombinatorOuterClass.Route.Builder combinatorRouteBuilder(boolean isCourier) {
        return CombinatorOuterClass.Route.newBuilder()
            .addAllPoints(
                List.of(
                    combinatorPoint(),
                    combinatorPoint(),
                    combinatorPointBuilder().addServices(handingService(isCourier)).build()
                )
            )
            .addAllPaths(
                List.of(
                    CombinatorOuterClass.Route.Path.newBuilder()
                        .setPointFrom(0)
                        .setPointTo(2)
                        .build(),
                    CombinatorOuterClass.Route.Path.newBuilder()
                        .setPointFrom(1)
                        .setPointTo(0)
                        .build()
                )
            )
            .setTariffId(101010)
            .setCostForShop(160)
            .setDateFrom(dateFrom())
            .setDateTo(dateTo())
            .setShipmentWarehouseId(43210);
    }

    @Nonnull
    public static CombinatorOuterClass.DeliveryRequestPackage.Builder deliveryPackageBuilder(boolean isOnlyRequired) {
        return CombinatorOuterClass.DeliveryRequestPackage.newBuilder()
            .setRequiredCount(1)
            .setWeight(1234)
            .addAllDimensions(List.of(40, 50, 30))
            .setPrice(999)
            .addAllCargoTypes(isOnlyRequired ? Collections.emptyList() : List.of(300, 301, 302));
    }

    @Nonnull
    public static CombinatorOuterClass.Route.Point combinatorPoint() {
        return combinatorPointBuilder().build();
    }

    @Nonnull
    public static CombinatorOuterClass.Route.Point.Builder combinatorPointBuilder() {
        return CombinatorOuterClass.Route.Point.newBuilder()
            .setIds(
                CombinatorOuterClass.PointIds.newBuilder()
                    .setPartnerId(54321)
                    .setLogisticPointId(10000051905L)
                    .setRegionId(213)
                    .build()
            )
            .setSegmentType("warehouse")
            .setSegmentId(SC_SEGMENT_ID)
            .setPartnerType("SORTING_CENTER")
            .setPartnerName("SC 1")
            .addAllServices(List.of(deliveryServiceBuilder().build()));
    }

    @Nonnull
    private static CombinatorOuterClass.DeliveryService.Builder deliveryServiceBuilder() {
        return CombinatorOuterClass.DeliveryService.newBuilder()
            .setId(987654)
            .setType(CombinatorOuterClass.DeliveryService.ServiceType.INTERNAL)
            .setCode("SORT")
            .addItems(
                CombinatorOuterClass.DeliveryService.ProcessedItem.newBuilder()
                    .setQuantity(1)
                    .build()
            )
            .setStartTime(timestamp())
            .setDuration(duration())
            .setLogisticDate(dateFrom())
            .setScheduleStartTime(timestamp())
            .setScheduleEndTime(timestamp())
            .setTzOffset(10800)
            .addWorkingSchedule(
                CombinatorOuterClass.DeliveryService.ScheduleDay.newBuilder()
                    .addAllDaysOfWeek(List.of(0, 1, 2, 3, 4))
                    .addTimeWindows(
                        CombinatorOuterClass.DeliveryService.TimeWindow.newBuilder()
                            .setEndTime(86340)
                            .build()
                    )
                    .build()
            );
    }

    @Nonnull
    private static CombinatorOuterClass.DeliveryService handingService(boolean isCourier) {
        CombinatorOuterClass.DeliveryService.Builder builder = deliveryServiceBuilder()
            .setType(CombinatorOuterClass.DeliveryService.ServiceType.OUTBOUND)
            .setCode("HANDING");

        if (isCourier) {
            builder.addDeliveryIntervals(
                CombinatorOuterClass.DeliveryInterval.newBuilder()
                    .setFrom(CombinatorOuterClass.Time.newBuilder().setHour(9).setMinute(0).build())
                    .setTo(CombinatorOuterClass.Time.newBuilder().setHour(18).setMinute(0).build())
                    .build()
            );
        }

        return builder.build();
    }

    @Nonnull
    private static Timestamp timestamp() {
        return Timestamp.newBuilder()
            .setSeconds(1647291600)
            .setNanos(123)
            .build();
    }

    @Nonnull
    private static Duration duration() {
        return Duration.newBuilder()
            .setSeconds(54321)
            .setNanos(123)
            .build();
    }

    @Nonnull
    private static CombinatorOuterClass.Box box() {
        return CombinatorOuterClass.Box.newBuilder()
            .setWeight(1234)
            .addAllDimensions(List.of(40, 50, 30))
            .build();
    }

    @Nonnull
    private static CombinatorOuterClass.Date dateFrom() {
        return CombinatorOuterClass.Date.newBuilder()
            .setDay(15)
            .setMonth(3)
            .setYear(2022)
            .build();
    }

    @Nonnull
    private static CombinatorOuterClass.Date dateTo() {
        return CombinatorOuterClass.Date.newBuilder()
            .setDay(20)
            .setMonth(3)
            .setYear(2022)
            .build();
    }
}
