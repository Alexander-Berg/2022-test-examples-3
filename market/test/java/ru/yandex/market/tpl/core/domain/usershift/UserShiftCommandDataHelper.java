package ru.yandex.market.tpl.core.domain.usershift;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.logisticrequest.LogisticRequestType;
import ru.yandex.market.tpl.api.model.order.OrderChequeDto;
import ru.yandex.market.tpl.api.model.order.OrderChequeStatus;
import ru.yandex.market.tpl.api.model.order.OrderChequeType;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.specialrequest.SpecialRequestType;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequest;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.CargoReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.ClientReturnReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewCollectDropshipRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewCommonRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewOrderPickupRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewOrderReturnRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderReference;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.query.usershift.mapper.task_mapper.DeliveryTaskAddressToDeliveryAddressMapper;

import static ru.yandex.market.tpl.common.util.DateTimeUtil.DEFAULT_ZONE_ID;

/**
 * @author ungomma
 */
@Component
@RequiredArgsConstructor
public class UserShiftCommandDataHelper {

    private final Clock clock;
    private final PickupPointRepository pickupPointRepository;
    private final DeliveryTaskAddressToDeliveryAddressMapper addressMapper;

    public NewOrderPickupRoutePointData taskOrderPickup(Instant arrivalTime) {
        return new NewOrderPickupRoutePointData(
                "my_service_center",
                new RoutePointAddress("my_service_center_address", GeoPoint.ofLatLon(
                        BigDecimal.ZERO, BigDecimal.ZERO
                )),
                arrivalTime,
                null
        );
    }

    public NewOrderReturnRoutePointData taskOrderReturn(Instant arrivalTime) {
        return new NewOrderReturnRoutePointData(
                "my_service_center",
                new RoutePointAddress("my_service_center_address", GeoPoint.ofLatLon(
                        BigDecimal.ZERO, BigDecimal.ZERO
                )),
                arrivalTime,
                null
        );
    }

    public NewDeliveryRoutePointData taskDropOffReturn(long movementId, int hour) {
        String address = "г.Москва, 3я ул. Строителей";
        return NewDeliveryRoutePointData.builder()
                .expectedDeliveryTime(DateTimeUtil.todayAtHour(hour, clock).plusSeconds(150))
                .expectedArrivalTime(DateTimeUtil.todayAtHour(hour, clock))
                .name("Доставка " + address)
                .address(new RoutePointAddress(address, GeoPointGenerator.generateLonLat()))
                .cargoReference(CargoReference.builder()
                        .movementId(movementId)
                        .build())
                .updateSc(true)
                .type(RoutePointType.LOCKER_DELIVERY)
                .build();
    }

    public NewDeliveryRoutePointData taskDropOffReturn(long movementId) {
        int hour = 10;
        return taskDropOffReturn(movementId, hour);
    }

    public NewDeliveryRoutePointData taskDropOffReturn(long movementId, long pickupPointId) {
        return taskDropOff(movementId, pickupPointId, true);
    }

    public NewDeliveryRoutePointData taskDropOff(long movementId, long pickupPointId, boolean isReturn) {
        int hour = 10;
        String address = "г.Москва, 3я ул. Строителей";
        return NewDeliveryRoutePointData.builder()
                .expectedDeliveryTime(DateTimeUtil.todayAtHour(hour, clock).plusSeconds(150))
                .expectedArrivalTime(DateTimeUtil.todayAtHour(hour, clock))
                .name("Доставка " + address)
                .address(new RoutePointAddress(address, GeoPointGenerator.generateLonLat()))
                .cargoReference(CargoReference.builder()
                        .movementId(movementId)
                        .isReturn(isReturn)
                        .build())
                .pickupPointId(pickupPointId)
                .updateSc(true)
                .type(RoutePointType.LOCKER_DELIVERY)
                .build();
    }

    public NewDeliveryRoutePointData taskLockerDelivery(long orderId, long pickupPointId) {
        return taskLockerDelivery(orderId, pickupPointId, 10);
    }

    public NewDeliveryRoutePointData taskLockerDelivery(long orderId, long pickupPointId, int hour) {
        String address = "г.Москва, 3я ул. Строителей";


        return NewDeliveryRoutePointData.builder()
                .expectedDeliveryTime(DateTimeUtil.todayAtHour(hour, clock).plusSeconds(150))
                .expectedArrivalTime(DateTimeUtil.todayAtHour(hour, clock))
                .name("Доставка " + address)
                .orderReference(new OrderReference(
                        orderId,
                        Set.of(),
                        OrderPaymentType.CASH,
                        OrderPaymentStatus.UNPAID,
                        false
                ))
                .address(new RoutePointAddress(address, GeoPointGenerator.generateLonLat()))
                .pickupPointId(pickupPointId)
                .updateSc(true)
                .type(RoutePointType.LOCKER_DELIVERY)
                .build();
    }

    public NewDeliveryRoutePointData taskLockerDelivery(Order order, long pickupPointId, int hour) {
        return NewDeliveryRoutePointData.builder()
                .expectedDeliveryTime(DateTimeUtil.todayAtHour(hour, clock).plusSeconds(150))
                .expectedArrivalTime(DateTimeUtil.todayAtHour(hour, clock))
                .name("Доставка " + order.getDelivery().getDeliveryAddress().getAddress())
                .deliveryTaskAddress(addressMapper.map(order.getDelivery().getDeliveryAddress()))
                .orderReference(new OrderReference(
                        order.getId(),
                        Set.of(),
                        OrderPaymentType.CASH,
                        OrderPaymentStatus.UNPAID,
                        false
                ))
                .address(order.getDelivery().getRoutePointAddress())
                .pickupPointId(pickupPointId)
                .updateSc(true)
                .type(RoutePointType.LOCKER_DELIVERY)
                .build();
    }

    public NewCollectDropshipRoutePointData taskCollectDropship(LocalDate date, Movement movement) {
        return NewCollectDropshipRoutePointData.buildManual(date, movement);
    }

    public NewDeliveryRoutePointData taskUnpaid(String address, int hour, long orderId) {
        return taskUnpaid(address, hour, orderId, false);
    }

    public NewDeliveryRoutePointData clientReturn(String address, int hour, long clientReturnId) {
        return NewDeliveryRoutePointData.builder()
                .expectedDeliveryTime(DateTimeUtil.todayAtHour(hour, clock).plusSeconds(150))
                .expectedArrivalTime(DateTimeUtil.todayAtHour(hour, clock))
                .name("Клиентский возврат " + address)
                .address(new RoutePointAddress(address, GeoPointGenerator.generateLonLat()))
                .clientReturnReference(
                        ClientReturnReference.builder()
                                .clientReturnId(clientReturnId)
                                .build()
                )
                .updateSc(true)
                .build();
    }

    public NewCommonRoutePointData logisticRequest(int hour, LogisticRequest logisticRequest) {
        var address = logisticRequest.getPointTo().getAddress();
        var builder = NewCommonRoutePointData.builder()
                .expectedArrivalTime(DateTimeUtil.todayAtHour(hour, clock))
                .name("Спецзадание " + address)
                .address(new RoutePointAddress(address, GeoPointGenerator.generateLonLat(),
                        logisticRequest.getPointTo().getAddressPersonalId(),
                        logisticRequest.getPointTo().getGpsPersonalId()))
                .updateSc(false);

        if (logisticRequest.getType() == LogisticRequestType.SPECIAL_REQUEST) {
            var specialRequest = (SpecialRequest) logisticRequest;
            if (specialRequest.getSpecialRequestType() == SpecialRequestType.LOCKER_INVENTORY) {
                builder.type(RoutePointType.LOCKER_DELIVERY);
            }
        }

        return builder.withLogisticRequests(List.of(logisticRequest)).build();
    }

    public NewDeliveryRoutePointData taskUnpaid(String address, int hour, long orderId, boolean canBeLeftAtReception) {
        return NewDeliveryRoutePointData.builder()
                .expectedDeliveryTime(DateTimeUtil.todayAtHour(hour, clock).plusSeconds(150))
                .expectedArrivalTime(DateTimeUtil.todayAtHour(hour, clock))
                .name("Доставка " + address)
                .address(new RoutePointAddress(address, GeoPointGenerator.generateLonLat()))
                .orderReference(new OrderReference(
                        orderId,
                        null,
                        OrderPaymentType.CASH,
                        OrderPaymentStatus.UNPAID,
                        canBeLeftAtReception
                ))
                .updateSc(true)
                .build();
    }

    public NewDeliveryRoutePointData taskUnpaid(
            String address, LocalDate deliveryDate, int hour, long orderId, boolean canBeLeftAtReception) {
        return NewDeliveryRoutePointData.builder()
                .expectedDeliveryTime(LocalDateTime.of(deliveryDate, LocalTime.of(hour, 0).plusSeconds(150)).toInstant(DEFAULT_ZONE_ID))
                .expectedArrivalTime(LocalDateTime.of(deliveryDate, LocalTime.of(hour, 0)).toInstant(DEFAULT_ZONE_ID))
                .name("Доставка " + address)
                .address(new RoutePointAddress(address, GeoPointGenerator.generateLonLat()))
                .orderReference(new OrderReference(
                        orderId,
                        null,
                        OrderPaymentType.CASH,
                        OrderPaymentStatus.UNPAID,
                        canBeLeftAtReception
                ))
                .updateSc(true)
                .build();
    }

    public NewDeliveryRoutePointData taskPrepaid(String address, int hour, long orderId) {
        return taskPrepaid(address, orderId, DateTimeUtil.todayAtHour(hour, clock), false);
    }

    public NewDeliveryRoutePointData taskPrepaid(String address, int hour, long orderId, boolean canBeLeftAtReception) {
        return taskPrepaid(address, orderId, DateTimeUtil.todayAtHour(hour, clock), canBeLeftAtReception);
    }

    public NewDeliveryRoutePointData taskPrepaid(
            String address,
            long orderId,
            Instant expectedArrivalTime,
            boolean canBeLeftAtReception) {
        return taskPrepaid(
                address,
                orderId,
                expectedArrivalTime,
                canBeLeftAtReception,
                GeoPointGenerator.generateLonLat());
    }

    public NewDeliveryRoutePointData taskPrepaid(
            String address,
            long orderId,
            Instant expectedArrivalTime,
            boolean canBeLeftAtReception,
            GeoPoint geoPoint) {
        return NewDeliveryRoutePointData.builder()
                .expectedDeliveryTime(expectedArrivalTime.plusSeconds(150))
                .expectedArrivalTime(expectedArrivalTime)
                .name("Доставка " + address)
                .address(new RoutePointAddress(address, geoPoint))
                .orderReference(new OrderReference(
                        orderId,
                        null,
                        OrderPaymentType.PREPAID,
                        OrderPaymentStatus.PAID,
                        canBeLeftAtReception
                ))
                .updateSc(true)
                .build();
    }

    public NewDeliveryRoutePointData task(
            String address,
            long orderId,
            Instant expectedArrivalTime,
            boolean canBeLeftAtReception,
            GeoPoint geoPoint,
            OrderPaymentType paymentType) {
        return NewDeliveryRoutePointData.builder()
                .expectedDeliveryTime(expectedArrivalTime.plusSeconds(150))
                .expectedArrivalTime(expectedArrivalTime)
                .name("Доставка " + address)
                .address(new RoutePointAddress(address, geoPoint))
                .orderReference(new OrderReference(
                        orderId,
                        null,
                        paymentType,
                        OrderPaymentStatus.PAID,
                        canBeLeftAtReception
                ))
                .updateSc(true)
                .build();
    }

    public NewDeliveryRoutePointData taskPrepaid(
            String address, Order order, Instant expectedArrivalTime, boolean canBeLeftAtReception
    ) {
        return NewDeliveryRoutePointData.builder()
                .expectedDeliveryTime(expectedArrivalTime.plusSeconds(150))
                .expectedArrivalTime(expectedArrivalTime)
                .name("Доставка " + address)
                .address(order.getDelivery().getRoutePointAddress())
                .orderReference(new OrderReference(
                        order.getId(),
                        order.getPlaces(),
                        OrderPaymentType.PREPAID,
                        OrderPaymentStatus.PAID,
                        canBeLeftAtReception
                ))
                .updateSc(true)
                .build();
    }


    public NewDeliveryRoutePointData cloneTask(
            NewDeliveryRoutePointData data, Instant newArrivalTime, long newOrderId
    ) {
        return NewDeliveryRoutePointData.builder()
                .expectedDeliveryTime(newArrivalTime.plusSeconds(150))
                .expectedArrivalTime(newArrivalTime)
                .name(data.getName())
                .address(data.getAddress())
                .orderReference(
                        new OrderReference(newOrderId, null, OrderPaymentType.PREPAID, OrderPaymentStatus.PAID, false)
                )
                .updateSc(true)
                .build();
    }

    public LocationDto getLocationDto(Long userShiftId) {
        LocationDto location = new LocationDto();
        location.setLatitude(new BigDecimal("55.74"));
        location.setLongitude(new BigDecimal("37.62"));
        location.setUserShiftId(userShiftId);
        return location;
    }

    public OrderChequeDto getChequeDto(OrderPaymentType paymentType) {
        return getChequeDto(paymentType, OrderChequeType.SELL);
    }

    public OrderChequeDto getChequeDto(OrderPaymentType paymentType, OrderChequeType chequeType) {
        return new OrderChequeDto(
                LocalDateTime.now(),
                1,
                5,
                "00106708505564",
                "9280440300233631",
                BigDecimal.valueOf(3021819398.000),
                paymentType,
                OrderChequeStatus.PRINTED,
                null,
                chequeType
        );
    }

}
