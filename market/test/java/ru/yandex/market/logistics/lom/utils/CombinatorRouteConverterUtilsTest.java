package ru.yandex.market.logistics.lom.utils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.Location;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.combinator.embedded.DeliveryService;
import ru.yandex.market.logistics.lom.entity.combinator.embedded.Point;
import ru.yandex.market.logistics.lom.entity.combinator.embedded.PointIds;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.tags.WaybillSegmentTag;
import ru.yandex.market.logistics.lom.model.enums.PointType;
import ru.yandex.market.logistics.lom.model.enums.ServiceCodeName;
import ru.yandex.market.logistics.lom.service.order.combinator.converter.CombinatorRouteConverterUtils;
import ru.yandex.market.logistics.lom.service.order.combinator.converter.PointsConvertingContext;

@DisplayName("Unit-тесты для CombinatorRouteConverterUtils")
class CombinatorRouteConverterUtilsTest extends AbstractTest {
    private static final Instant START_TIME = Instant.parse("2021-07-12T12:20:30.00Z");

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("getShipmentDateArguments")
    void getShipmentDate(
        @SuppressWarnings("unused") String caseName,
        Point warehousePoint,
        Point movementPoint,
        LocalDate expectedShipmentDate
    ) {
        softly.assertThat(CombinatorRouteConverterUtils.getShipmentDate(warehousePoint, movementPoint))
            .isEqualTo(expectedShipmentDate);
    }

    @Nonnull
    private static Stream<Arguments> getShipmentDateArguments() {
        return StreamEx.of(
            Arguments.of(
                "Самопривоз поставщиком: services=null",
                dropshipPoint(),
                dropshipPoint().setServices(null),
                null
            ),
            Arguments.of(
                "Самопривоз поставщиком: services=[]",
                dropshipPoint(),
                dropshipPoint().setServices(List.of()),
                null
            ),
            Arguments.of(
                "Забор от поставщика: services=null",
                dropshipPoint().setServices(null),
                sortingCenterPoint(),
                null
            ),
            Arguments.of(
                "Забор от поставщика: services=[]",
                dropshipPoint().setServices(List.of()),
                sortingCenterPoint(),
                null
            ),
            Arguments.of(
                "Отгрузка со склада: services=null",
                sortingCenterPoint(),
                deliveryPoint().setServices(null),
                null
            ),
            Arguments.of(
                "Отгрузка со склада: services=[]",
                sortingCenterPoint(),
                deliveryPoint().setServices(List.of()),
                null
            ),
            Arguments.of(
                "Самопривоз поставщиком: 2021-04-03",
                dropshipPoint(),
                dropshipPoint().setServices(List.of(service(ServiceCodeName.SHIPMENT, 2))),
                LocalDate.of(2021, 4, 3)
            ),
            Arguments.of(
                "Забор от поставщика: 2021-04-02",
                dropshipPoint(),
                sortingCenterPoint(),
                LocalDate.of(2021, 4, 2)
            ),
            Arguments.of(
                "Отгрузка со склада: 2021-04-05",
                sortingCenterPoint(),
                deliveryPoint(),
                LocalDate.of(2021, 4, 5)
            )
        );
    }

    @Nonnull
    private static Point dropshipPoint() {
        return new Point()
            .setPartnerType(PartnerType.DROPSHIP)
            .setIds(new PointIds().setPartnerId(1L))
            .setServices(List.of(service(ServiceCodeName.SHIPMENT, 1)));
    }

    @Nonnull
    private static Point sortingCenterPoint() {
        return new Point()
            .setPartnerType(PartnerType.SORTING_CENTER)
            .setIds(new PointIds().setPartnerId(2L))
            .setServices(List.of(service(ServiceCodeName.SHIPMENT, 2), service(ServiceCodeName.MOVEMENT, 3)))
            .setShipmentDateOffsetDays(1);
    }

    @Nonnull
    private static Point deliveryPoint() {
        return new Point()
            .setPartnerType(PartnerType.DELIVERY)
            .setIds(new PointIds().setPartnerId(2L))
            .setServices(List.of(service(ServiceCodeName.MOVEMENT, 4)));
    }

    @Nonnull
    private static DeliveryService service(ServiceCodeName serviceCodeName, int dayOfMonth) {
        return new DeliveryService()
            .setCode(serviceCodeName)
            .setStartTime(
                ZonedDateTime.of(
                        LocalDateTime.of(2021, 4, dayOfMonth, 19, 0),
                        DateTimeUtils.MOSCOW_ZONE
                    )
                    .toInstant()
            )
            .setDuration(Duration.ofHours(5));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Проверка на правильную конвертацию waybill сегмента с сервисом CALL_COURIER")
    void convertLastMileWithCallCourierService(
        @SuppressWarnings("unused") String name,
        Point movementPoint,
        Point handingPoint,
        Point fromWarehousePoint,
        boolean shouldBeProcessed
    ) {
        WaybillSegment convertedSegment = CombinatorRouteConverterUtils
            .convertLastMile(
                PointsConvertingContext.builder().build(),
                new Location(),
                1L,
                fromWarehousePoint,
                movementPoint,
                handingPoint,
                null,
                null
            )
            .findFirst()
            .orElseThrow();

        if (shouldBeProcessed) {
            softly.assertThat(convertedSegment.getCallCourierTime())
                .isEqualTo(START_TIME.atZone(DateTimeUtils.MOSCOW_ZONE).toInstant());
            softly.assertThat(convertedSegment.hasTag(WaybillSegmentTag.CALL_COURIER)).isTrue();
        } else {
            softly.assertThat(convertedSegment.getCallCourierTime()).isNull();
            softly.assertThat(convertedSegment.hasTag(WaybillSegmentTag.CALL_COURIER)).isFalse();
        }
    }

    @Nonnull
    private static Stream<Arguments> convertLastMileWithCallCourierService() {
        return StreamEx.of(
            Arguments.of(
                "Только handging",
                createPointWithType(PointType.MOVEMENT),
                createPointWithType(PointType.HANDING).setServices(List.of(callCourierService(START_TIME))),
                createPointWithType(PointType.WAREHOUSE),
                true
            ),
            Arguments.of(
                "Только movement",
                createPointWithType(PointType.MOVEMENT).setServices(List.of(callCourierService(START_TIME))),
                createPointWithType(PointType.HANDING),
                createPointWithType(PointType.WAREHOUSE),
                true
            ),
            Arguments.of(
                "И handging, и movement",
                createPointWithType(PointType.MOVEMENT).setServices(List.of(callCourierService(START_TIME))),
                createPointWithType(PointType.HANDING).setServices(
                    List.of(callCourierService(START_TIME.plusSeconds(1L)))
                ),
                createPointWithType(PointType.WAREHOUSE),
                true
            ),
            Arguments.of(
                "Неправильный сегмент",
                createPointWithType(PointType.MOVEMENT),
                createPointWithType(PointType.HANDING),
                createPointWithType(PointType.WAREHOUSE).setServices(List.of(callCourierService(START_TIME))),
                false
            )
        );
    }

    @Nonnull
    private static Point createPointWithType(PointType type) {
        return new Point()
            .setSegmentType(type)
            .setServices(List.of())
            .setIds(new PointIds().setLogisticPointId(2L));
    }

    @Nonnull
    private static DeliveryService callCourierService(Instant startTime) {
        return new DeliveryService()
            .setCode(ServiceCodeName.CALL_COURIER)
            .setStartTime(startTime);
    }
}
