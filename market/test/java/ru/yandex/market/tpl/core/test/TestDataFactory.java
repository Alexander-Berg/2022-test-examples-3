package ru.yandex.market.tpl.core.test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.api.model.order.OrderChequeDto;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.locker.PickupPointType;
import ru.yandex.market.tpl.api.model.order.partner.OrderEventType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequest;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCheque;
import ru.yandex.market.tpl.core.domain.order.OrderCommand;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEvent;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.difference.AbstractOrderDifference;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestGenerateService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.CollectDropshipTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewCollectDropshipRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewCommonRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderReference;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRuleRepository;
import ru.yandex.market.tpl.core.task.persistence.FlowTaskEntity;
import ru.yandex.market.tpl.core.task.projection.TaskFlowType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.movement.Movement.TAG_DROPOFF_CARGO_RETURN;

/**
 * @author kukabara
 */
@Service
@RequiredArgsConstructor
public class TestDataFactory {

    public static final long DELIVERY_SERVICE_ID = 239L;
    public static final String PICKUP_POINT_CODE_TEST = "test";
    public static final String PICKUP_POINT_DEFAULT_PHONE = "+79999999999";
    private static final Random RND = new Random(0);
    private final UserShiftCommandService commandService;
    private final UserScheduleRuleRepository scheduleRuleRepository;

    private final EntityManager entityManager;
    private final OrderGenerateService orderGenerateService;
    private final SpecialRequestGenerateService specialRequestGenerateService;
    private final MovementGenerator movementGenerator;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final OrderCommandService orderCommandService;
    private final PickupPointRepository pickupPointRepository;
    private final OrderHistoryEventRepository orderHistoryEventRepository;
    private final UserShiftRepository userShiftRepository;
    private final ClientReturnGenerator clientReturnGenerator;
    private final Clock clock;

    @Transactional
    public long createEmptyShift(long shiftId, User user) {
        Shift shift = entityManager.find(Shift.class, shiftId);
        long userShiftId = commandService.createUserShift(UserShiftCommand.Create.builder()
                .shiftId(shiftId)
                .userId(user.getId())
                .active(true)
                .scheduleData(scheduleRuleRepository.findUserScheduleForDate(user, shift.getShiftDate())
                        .getScheduleDataOrThrow(shift.getShiftDate())
                )
                .build());
        flushAndClear();
        return userShiftId;
    }

    public RoutePoint createEmptyRoutePoint(User user, long userShiftId) {
        return createEmptyRoutePoint(user, userShiftId, Instant.now(), Instant.now());
    }

    public PickupPoint createPickupPoint(PartnerSubType partnerSubType, long logisticPointId) {
        return createPickupPoint(partnerSubType, logisticPointId, DELIVERY_SERVICE_ID, false);
    }

    public PickupPoint createPickupPoint(PartnerSubType partnerSubType, long logisticPointId, long deliveryServiceId) {
        return createPickupPoint(partnerSubType, logisticPointId, deliveryServiceId, false);
    }

    public PickupPoint createPickupPoint(
            PartnerSubType partnerSubType, long logisticPointId, long deliveryServiceId, boolean marketBranded
    ) {
        PickupPoint pickupPoint = new PickupPoint();
        pickupPoint.setCode(PICKUP_POINT_CODE_TEST);
        pickupPoint.setName("PickupPoint");
        pickupPoint.setImageUrl("https://avatars.mds.yandex.net/get-market-shop-logo/1528691/package_1/orig");
        pickupPoint.setDescription("Сначала налева, потом направо");
        pickupPoint.setPhoneNumber(PICKUP_POINT_DEFAULT_PHONE);
        pickupPoint.setLogisticPointId(logisticPointId);
        pickupPoint.setPartnerId(deliveryServiceId);
        pickupPoint.setDeliveryServiceId(deliveryServiceId);
        pickupPoint.setPartnerSubType(partnerSubType);
        pickupPoint.setType(mapPartnerSubTypeToOldPickupPointType(partnerSubType));
        pickupPoint.setPartnerType(partnerSubType);
        pickupPoint.setMarketBranded(marketBranded);
        pickupPoint.setAddress("Pickup point address");
        return pickupPointRepository.save(pickupPoint);
    }

    public PickupPoint createPickupPoint(
            PartnerSubType partnerSubType, long logisticPointId, long deliveryServiceId, boolean marketBranded,
            String address
    ) {
        PickupPoint pickupPoint = new PickupPoint();
        pickupPoint.setCode("test");
        pickupPoint.setName("PickupPoint");
        pickupPoint.setImageUrl("https://avatars.mds.yandex.net/get-market-shop-logo/1528691/package_1/orig");
        pickupPoint.setDescription("Сначала налева, потом направо");
        pickupPoint.setPhoneNumber("+79999999999");
        pickupPoint.setAddress(address);
        pickupPoint.setLogisticPointId(logisticPointId);
        pickupPoint.setPartnerId(deliveryServiceId);
        pickupPoint.setDeliveryServiceId(deliveryServiceId);
        pickupPoint.setPartnerSubType(partnerSubType);
        pickupPoint.setType(mapPartnerSubTypeToOldPickupPointType(partnerSubType));
        pickupPoint.setPartnerType(partnerSubType);
        pickupPoint.setMarketBranded(marketBranded);
        return pickupPointRepository.save(pickupPoint);
    }

    /**
     * Для поддержки тестов, где мы смотрим на старое notNull поле.
     *
     * @param partnerSubType подтип партнера точки
     * @return старый тип точки
     */
    private PickupPointType mapPartnerSubTypeToOldPickupPointType(PartnerSubType partnerSubType) {
        switch (partnerSubType) {
            case PVZ:
            case LOCKER_GO:
            case LAVKA:
                return PickupPointType.PVZ;
            case LOCKER:
                return PickupPointType.LOCKER;
            default:
                throw new TplIllegalStateException("Unexpected value: " + partnerSubType);
        }
    }

    public RoutePoint createEmptyRoutePoint(User user, long userShiftId,
                                            Instant expectedDeliveryTime, Instant expectedArrivalTime) {
        String addressString = "Адрес " + RandomUtils.nextInt(0, 100);
        var deliveryTaskData = NewDeliveryRoutePointData.builder()
                .address(new RoutePointAddress(addressString, GeoPointGenerator.generateLonLat()))
                .name(addressString)
                .expectedDeliveryTime(expectedDeliveryTime)
                .expectedArrivalTime(expectedArrivalTime)
                .build();

        var command = new UserShiftCommand.ManualAddRoutePoint(userShiftId, deliveryTaskData);

        return commandService.addEmptyRoutePoint(user, command);
    }

    public OrderDeliveryTask addDeliveryTaskManual(User user, long userShiftId, long routePointId,
                                                   OrderGenerateService.OrderGenerateParam orderGenerateParam) {
        Order order = orderGenerateService.createOrder(orderGenerateParam);
        Instant deliveryTime = order.getDelivery().getDeliveryIntervalFrom()
                .plus(RandomUtils.nextInt(0, 121), ChronoUnit.MINUTES);

        return commandService.addDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddDeliveryTask(userShiftId, routePointId,
                        OrderReference.fromSources(order, false), deliveryTime, true)
        );
    }

    public OrderDeliveryTask addClientReturnDeliveryTaskManual(User user, long userShiftId, long routePointId) {
        var clientReturn = clientReturnGenerator.generateReturnFromClient();
        return commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShiftId, routePointId, clientReturn.getId(), Instant.now(clock)
                )
        );
    }

    public FlowTaskEntity addFlowTask(long userShiftId, TaskFlowType flowType) {
        return addFlowTask(userShiftId, flowType, List.of(specialRequestGenerateService.createSpecialRequest()));
    }

    public FlowTaskEntity addFlowTask(long userShiftId, TaskFlowType flowType, List<LogisticRequest> logisticRequests) {
        var userShift = entityManager.find(UserShift.class, userShiftId);
        return commandService.addFlowTask(userShift.getUser(), new UserShiftCommand.AddFlowTask(
                userShiftId, flowType,
                NewCommonRoutePointData.builder()
                        .type(flowType.getRoutePointType())
                        .address(new RoutePointAddress("my_address", GeoPointGenerator.generateLonLat()))
                        .expectedArrivalTime(Instant.now())
                        .name("my_name")
                        .withLogisticRequests(logisticRequests)
                        .build()
        ));
    }

    @Transactional
    public LockerDeliveryTask addLockerDeliveryTask(long userShiftId) {
        return addLockerDeliveryTask(userShiftId, PartnerSubType.LOCKER);
    }

    @Transactional
    public LockerDeliveryTask addLockerDeliveryTask(long userShiftId, PartnerSubType subtype) {
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        Order lockerOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .recipientPhone("phone1")
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .pickupPoint(createPickupPoint(subtype, 1L, 1L))
                .build());

        UserShift userShift = entityManager.find(UserShift.class, userShiftId);

        UserShiftCommand.AddDeliveryTask command = new UserShiftCommand.AddDeliveryTask(userShiftId,
                NewDeliveryRoutePointData.builder()
                        .address(new RoutePointAddress("my_address", GeoPointGenerator.generateLonLat()))
                        .expectedArrivalTime(Instant.now())
                        .expectedDeliveryTime(Instant.now())
                        .name("my_name")
                        .withOrderReferenceFromOrder(lockerOrder, false, false)
                        .type(RoutePointType.LOCKER_DELIVERY)
                        .build(),
                SimpleStrategies.NO_MERGE,
                GeoPoint.GEO_POINT_SCALE
        );
        commandService.addDeliveryTask(userShift.getUser(), command);

        return userShift.streamDeliveryTasks()
                .select(LockerDeliveryTask.class)
                .findFirst()
                .orElseThrow(IllegalStateException::new);

    }

    public LockerDeliveryTask addLockerDeliveryTask(long userShiftId, Order lockerOrder) {
        UserShift userShift = entityManager.find(UserShift.class, userShiftId);

        UserShiftCommand.AddDeliveryTask command = new UserShiftCommand.AddDeliveryTask(userShiftId,
                NewDeliveryRoutePointData.builder()
                        .address(new RoutePointAddress("my_address", GeoPointGenerator.generateLonLat()))
                        .expectedArrivalTime(Instant.now())
                        .expectedDeliveryTime(Instant.now())
                        .name("my_name")
                        .withOrderReferenceFromOrder(lockerOrder, false, false)
                        .type(RoutePointType.LOCKER_DELIVERY)
                        .build(),
                SimpleStrategies.NO_MERGE
        );
        commandService.addDeliveryTask(userShift.getUser(), command);

        return userShift.streamDeliveryTasks()
                .select(LockerDeliveryTask.class)
                .findFirst()
                .orElseThrow(IllegalStateException::new);

    }

    public CollectDropshipTask addDropshipTask(long userShiftId) {
        return addDropshipTask(userShiftId, movementGenerator.generate(MovementCommand.Create.builder()
                .deliveryServiceId(-1L)
                .build()));
    }

    public CollectDropshipTask addDropshipTask(long userShiftId, Movement movement) {
        UserShift userShift = getUserShift(userShiftId);

        UserShiftCommand.AddCollectDropshipTask command = new UserShiftCommand.AddCollectDropshipTask(
                userShiftId,
                NewCollectDropshipRoutePointData.buildManual(
                        userShift.getShift().getShiftDate(),
                        movement
                )
        );
        commandService.addCollectDropshipTask(userShift.getUser(), command);

        return getUserShift(userShiftId).streamCollectDropshipTasks()
                .findFirst()
                .orElseThrow(IllegalStateException::new);

    }

    @NotNull
    private UserShift getUserShift(long userShiftId) {
        Optional<UserShift> userShiftOpt = userShiftRepository.findByIdWithRoutePoints(userShiftId);
        assertThat(userShiftOpt).isNotEmpty();
        return userShiftOpt.get();
    }

    public OrderDeliveryTask addDeliveryTaskAuto(User user, long userShiftId,
                                                 OrderPaymentStatus paymentStatus,
                                                 OrderPaymentType paymentType) {
        return addDeliveryTaskAuto(user, userShiftId, paymentStatus, paymentType, RND.nextInt(121));
    }

    public OrderDeliveryTask addDeliveryTaskAuto(User user, long userShiftId,
                                                 OrderPaymentStatus paymentStatus,
                                                 OrderPaymentType paymentType,
                                                 int deliveryTimePlus) {
        return addDeliveryTaskAuto(user, userShiftId, paymentStatus, paymentType, deliveryTimePlus, null,
                AddressGenerator.AddressGenerateParam.builder().build());
    }

    public OrderDeliveryTask addDeliveryTaskAuto(User user, long userShiftId,
                                                 OrderPaymentStatus paymentStatus,
                                                 OrderPaymentType paymentType,
                                                 int deliveryTimePlus,
                                                 Long buyerYandexId,
                                                 AddressGenerator.AddressGenerateParam addressGenerateParam) {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(paymentStatus)
                .paymentType(paymentType)
                .deliveryInterval(new LocalTimeInterval(
                        LocalTime.of(13, deliveryTimePlus), LocalTime.of(13, deliveryTimePlus)
                ))
                .addressGenerateParam(addressGenerateParam)
                .buyerYandexUid(buyerYandexId)
                .build());
        Instant deliveryTime = order.getDelivery().getDeliveryIntervalFrom()
                .plus(deliveryTimePlus, ChronoUnit.MINUTES);

        return (OrderDeliveryTask) commandService.addDeliveryTask(user,
                new UserShiftCommand.AddDeliveryTask(userShiftId,
                        NewDeliveryRoutePointData.builder()
                                .address(new RoutePointAddress("my_address", GeoPointGenerator.generateLonLat()))
                                .expectedArrivalTime(deliveryTime)
                                .expectedDeliveryTime(deliveryTime)
                                .name("my_name")
                                .withOrderReferenceFromOrder(order, false, false)
                                .build(),
                        SimpleStrategies.NO_MERGE,
                        GeoPoint.GEO_POINT_SCALE
                )
        );
    }

    public Order generateOrder(OrderGenerateService.OrderGenerateParam param) {
        return orderGenerateService.createOrder(param);
    }

    public OrderCheque createOrderWithCheque(OrderChequeDto chequeDto) {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .paymentStatus(OrderPaymentStatus.PAID)
                .paymentType(chequeDto.getPaymentType())
                .build());

        orderCommandService.forceUpdateFlowStatus(
                new OrderCommand.UpdateFlowStatus(order.getId(), OrderFlowStatus.TRANSPORTATION_RECIPIENT));

        order.registerCheque(new OrderCommand.RegisterCheque(getTask(), -1, "Кассир Иванов",
                chequeDto, null, false, false, null, Optional.empty()));
        return order.streamCheques().findFirst().orElseThrow();
    }

    public OrderDeliveryTask getTask() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(-1L);

        UserShift userShift = mock(UserShift.class);
        when(userShift.getUser()).thenReturn(user);

        RoutePoint routePoint = mock(RoutePoint.class);
        when(routePoint.getUserShift()).thenReturn(userShift);

        OrderDeliveryTask task = mock(OrderDeliveryTask.class);
        when(task.getId()).thenReturn(-1L);

        when(task.getRoutePoint()).thenReturn(routePoint);
        return task;
    }

    public void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    public OrderHistoryEvent createEvent(Long orderId, OrderEventType type, AbstractOrderDifference diff) {
        return createEvent(orderId, type, diff, Instant.now());
    }

    public OrderHistoryEvent createEvent(Long orderId, OrderEventType type, AbstractOrderDifference diff,
                                         Instant date) {
        return orderHistoryEventRepository.save(
                OrderHistoryEvent.builder()
                        .orderId(orderId)
                        .type(type)
                        .source(Source.OPERATOR)
                        .difference(diff)
                        .date(date)
                        .build()
        );
    }

    public Movement buildDropOffReturnMovement(String logisticPointId) {
        return movementGenerator.generate(
                MovementCommand.Create.builder()
                        .tags(List.of(TAG_DROPOFF_CARGO_RETURN))
                        .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse(ow ->
                                ow.setYandexId(logisticPointId)))
                        .build());
    }

    public Movement buildDropOffDirectMovement(String logisticPointId) {
        return movementGenerator.generate(
                MovementCommand.Create.builder()
                        .orderWarehouse(orderWarehouseGenerator.generateWarehouse(ow ->
                                ow.setYandexId(logisticPointId)))
                        .build());
    }

    public LockerDeliveryTask createLockerDeliveryTask(User user, long userShiftId, long logisticPointId,
                                                       long deliveryServiceId) {
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        PickupPoint pickupPoint = createPickupPoint(PartnerSubType.LOCKER, logisticPointId,
                deliveryServiceId);

        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("00:00-23:59"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .pickupPoint(pickupPoint)
                .build());

        Instant deliveryTime = order.getDelivery().getDeliveryIntervalFrom();

        var address = new RoutePointAddress("my_address", geoPoint);
        return (LockerDeliveryTask) commandService.addDeliveryTask(user,
                new UserShiftCommand.AddDeliveryTask(userShiftId,
                        NewDeliveryRoutePointData.builder()
                                .address(address)
                                .expectedArrivalTime(deliveryTime)
                                .expectedDeliveryTime(deliveryTime)
                                .name("my_name")
                                .withOrderReferenceFromOrder(order, false, false)
                                .type(RoutePointType.LOCKER_DELIVERY)
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE,
                        GeoPoint.GEO_POINT_SCALE
                )
        );
    }

    public CollectDropshipTask createCollectDropshipTask(User user, long userShiftId) {
        Movement movement = movementGenerator.generate(MovementCommand.Create.builder().build());
        return commandService.addCollectDropshipTask(user,
                new UserShiftCommand.AddCollectDropshipTask(
                        userShiftId,
                        NewCollectDropshipRoutePointData.builder()
                                .movement(movement)
                                .expectedArrivalTime(clock.instant())
                                .name("test")
                                .address(new RoutePointAddress(
                                        movement.getWarehouse().getAddress().getAddress(),
                                        movement.getWarehouse().getAddress().getGeoPoint()))
                                .build()
                ));
    }


}
