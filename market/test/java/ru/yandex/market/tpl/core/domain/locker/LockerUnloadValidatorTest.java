package ru.yandex.market.tpl.core.domain.locker;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.pickupPoint.PickupPointReturnReason;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.domain.barcode_prefix.ReturnBarcodePrefixRepository;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.pickup.LockerUnloadScanSummary;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTaskRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;


@RequiredArgsConstructor
class LockerUnloadValidatorTest extends TplAbstractTest {

    private static final String EXTERNAL_ORDER_ID = "123";
    private static final String PREVIOUS_CORRECT_STATUS_EXTERNAL_ORDER_ID = "456";
    private static final String PREVIOUS_INCORRECT_STATUS_EXTERNAL_ORDER_ID = "789";
    private static final String MULTI_PLACE_ORDER_ID = "963";
    private static final String UNKNOWN_EXTERNAL_ORDER_ID = "unknown";
    private static final String DIMENSIONS_EXCEEDS_ORDER_ID_1 = "dimensions_exceeds_1";
    private static final String DIMENSIONS_EXCEEDS_ORDER_ID_2 = "dimensions_exceeds_2";
    private static final String DIMENSIONS_EXCEEDS_ORDER_ID_3 = "dimensions_exceeds_3";
    private static final String CELL_DID_NOT_OPEN_ORDER_ID = "cell_not_open";
    private static final String OTHER_REASON_ORDER_ID = "other_reason";

    private final TestUserHelper userHelper;
    private final OrderGenerateService orderGenerateService;
    private final TestDataFactory testDataFactory;
    private final EntityManager entityManager;
    private final UserShiftCommandService commandService;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final LockerDeliveryTaskRepository lockerDeliveryTaskRepository;
    private final LockerUnloadValidator lockerUnloadValidator;
    private final ReturnBarcodePrefixRepository barcodePrefixRepository;

    private LockerDeliveryTask lockerDeliveryTask;


    @BeforeEach
    void init() {
        User user = userHelper.findOrCreateUser(356L);
        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);

        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        PickupPoint pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L);
        OrderGenerateService.OrderGenerateParam orderGenerateParam = OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .recipientPhone("phone1")
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .pickupPoint(pickupPoint)
                .build();

        orderGenerateService.createOrder(orderGenerateParam.toBuilder()
                .externalOrderId(PREVIOUS_CORRECT_STATUS_EXTERNAL_ORDER_ID)
                .flowStatus(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT)
                .build());
        orderGenerateService.createOrder(orderGenerateParam.toBuilder()
                .externalOrderId(PREVIOUS_INCORRECT_STATUS_EXTERNAL_ORDER_ID)
                .flowStatus(OrderFlowStatus.READY_FOR_RETURN)
                .build());

        Order lockerOrder = orderGenerateService.createOrder(orderGenerateParam.toBuilder()
                .externalOrderId(EXTERNAL_ORDER_ID)
                .build());

        orderGenerateService.createOrder(orderGenerateParam.toBuilder()
                .externalOrderId(MULTI_PLACE_ORDER_ID)
                .places(
                        List.of(
                                OrderPlaceDto.builder()
                                        .barcode(new OrderPlaceBarcode("145", "place1"))
                                        .build(),
                                OrderPlaceDto.builder()
                                        .barcode(new OrderPlaceBarcode("145", "place2"))
                                        .build()
                        )
                )
                .flowStatus(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT)
                .build());

        Order dimensionsExceedOrder_1 = orderGenerateService.createOrder(orderGenerateParam.toBuilder()
                .externalOrderId(DIMENSIONS_EXCEEDS_ORDER_ID_1)
                .build());

        Order dimensionsExceedOrder_2 = orderGenerateService.createOrder(orderGenerateParam.toBuilder()
                .externalOrderId(DIMENSIONS_EXCEEDS_ORDER_ID_2)
                .build());

        Order dimensionsExceedOrder_3 = orderGenerateService.createOrder(orderGenerateParam.toBuilder()
                .externalOrderId(DIMENSIONS_EXCEEDS_ORDER_ID_3)
                .build());

        Order cellDidNotOpenOrder = orderGenerateService.createOrder(orderGenerateParam.toBuilder()
                .externalOrderId(CELL_DID_NOT_OPEN_ORDER_ID)
                .build());

        Order otherReasonOrder = orderGenerateService.createOrder(orderGenerateParam.toBuilder()
                .externalOrderId(OTHER_REASON_ORDER_ID)
                .build());

        UserShift userShift = entityManager.find(UserShift.class, userShiftId);

        Instant now = Instant.now(clock);
        commandService.addDeliveryTask(
                userShift.getUser(),
                createAddDeliveryTaskCommand(userShiftId, lockerOrder, now));
        commandService.addDeliveryTask(
                userShift.getUser(),
                createAddDeliveryTaskCommand(userShiftId, dimensionsExceedOrder_1, now));
        commandService.addDeliveryTask(
                userShift.getUser(),
                createAddDeliveryTaskCommand(userShiftId, dimensionsExceedOrder_2, now));
        commandService.addDeliveryTask(
                userShift.getUser(),
                createAddDeliveryTaskCommand(userShiftId, dimensionsExceedOrder_3, now));
        commandService.addDeliveryTask(
                userShift.getUser(),
                createAddDeliveryTaskCommand(userShiftId, cellDidNotOpenOrder, now));
        commandService.addDeliveryTask(
                userShift.getUser(),
                createAddDeliveryTaskCommand(userShiftId, otherReasonOrder, now));


        lockerDeliveryTask = transactionTemplate.execute(st -> entityManager.find(UserShift.class, userShiftId)
                .streamDeliveryTasks()
                .select(LockerDeliveryTask.class)
                .findFirst()
                .orElseThrow(IllegalStateException::new));
    }

    @Test
    void getSummary() {
        String clientReturnBarcode = barcodePrefixRepository.findBarcodePrefixByName(
                "CLIENT_RETURN_BARCODE_PREFIX_SF").getBarcodePrefix() + "3";
        LockerUnloadScanSummary summary = transactionTemplate.execute(st -> lockerUnloadValidator.getSummary(
                lockerDeliveryTaskRepository.findByIdOrThrow(lockerDeliveryTask.getId()),
                Set.of(
                        new UnloadedOrder(UNKNOWN_EXTERNAL_ORDER_ID, null, null),
                        new UnloadedOrder(EXTERNAL_ORDER_ID, null, null),
                        new UnloadedOrder(PREVIOUS_CORRECT_STATUS_EXTERNAL_ORDER_ID, null, null),
                        new UnloadedOrder(PREVIOUS_INCORRECT_STATUS_EXTERNAL_ORDER_ID, null, null),
                        new UnloadedOrder(MULTI_PLACE_ORDER_ID, null, List.of("place1", "place2")),
                        new UnloadedOrder(CELL_DID_NOT_OPEN_ORDER_ID, PickupPointReturnReason.CELL_DID_NOT_OPEN, null),
                        new UnloadedOrder(OTHER_REASON_ORDER_ID, PickupPointReturnReason.OTHER, null),
                        new UnloadedOrder(
                                DIMENSIONS_EXCEEDS_ORDER_ID_1,
                                PickupPointReturnReason.DIMENSIONS_EXCEEDS,
                                null),
                        new UnloadedOrder(
                                DIMENSIONS_EXCEEDS_ORDER_ID_2,
                                PickupPointReturnReason.DIMENSIONS_EXCEEDS_LOCKER,
                                null),
                        new UnloadedOrder(
                                DIMENSIONS_EXCEEDS_ORDER_ID_3,
                                PickupPointReturnReason.DIMENSIONS_EXCEEDS_PICKUP_POINT,
                                null),

                        new UnloadedOrder(clientReturnBarcode, null, null)
                )
        ));
        assertThat(summary.getCorrectlyScannedOrderIdsS())
                .containsExactlyInAnyOrder(
                        UNKNOWN_EXTERNAL_ORDER_ID,
                        PREVIOUS_CORRECT_STATUS_EXTERNAL_ORDER_ID,
                        PREVIOUS_INCORRECT_STATUS_EXTERNAL_ORDER_ID,
                        MULTI_PLACE_ORDER_ID,
                        DIMENSIONS_EXCEEDS_ORDER_ID_1,
                        DIMENSIONS_EXCEEDS_ORDER_ID_2,
                        DIMENSIONS_EXCEEDS_ORDER_ID_3,
                        CELL_DID_NOT_OPEN_ORDER_ID,
                        OTHER_REASON_ORDER_ID
                );
        assertThat(summary.getFalselyScannedExternalOrderIdsS())
                .containsExactly(EXTERNAL_ORDER_ID);
        assertThat(summary.getExternalOrderIdsToReturnS())
                .containsExactlyInAnyOrder(PREVIOUS_CORRECT_STATUS_EXTERNAL_ORDER_ID, MULTI_PLACE_ORDER_ID);
        assertThat(summary.getNotFoundExternalOrderIdsS())
                .containsExactly(UNKNOWN_EXTERNAL_ORDER_ID);
        assertThat(summary.getIncorrectStatusExternalOrderIdsS())
                .containsExactly(PREVIOUS_INCORRECT_STATUS_EXTERNAL_ORDER_ID);
        assertThat(summary.getDimensionsExceedsOrderIdsS())
                .containsExactlyInAnyOrder(
                        DIMENSIONS_EXCEEDS_ORDER_ID_1, DIMENSIONS_EXCEEDS_ORDER_ID_2, DIMENSIONS_EXCEEDS_ORDER_ID_3);
        assertThat(summary.getOrderToRescheduleS())
                .containsExactlyInAnyOrder(CELL_DID_NOT_OPEN_ORDER_ID, OTHER_REASON_ORDER_ID);
        // успешно отсканированный клиентский возврат
        assertThat(summary.getClientReturnBarcodesS()).contains(clientReturnBarcode);

        // частично отсканированный многоместный заказ
        summary = transactionTemplate.execute(st -> lockerUnloadValidator.getSummary(
                lockerDeliveryTaskRepository.findByIdOrThrow(lockerDeliveryTask.getId()),
                Set.of(
                        new UnloadedOrder(MULTI_PLACE_ORDER_ID, null, List.of("place1"))
                )
        ));

        assertThat(summary.getCorrectlyScannedOrderIdsS()).containsExactly(MULTI_PLACE_ORDER_ID);
        assertThat(summary.getPartiallyScannedMultiOrderIdsS())
                .containsExactly(MULTI_PLACE_ORDER_ID);
    }

    private UserShiftCommand.AddDeliveryTask createAddDeliveryTaskCommand(
            long userShiftId,
            Order order,
            Instant now) {
        RoutePointAddress address = new RoutePointAddress(
                "my_address",
                order.getDelivery().getDeliveryAddress().getGeoPoint());
        return new UserShiftCommand.AddDeliveryTask(userShiftId,
                NewDeliveryRoutePointData.builder()
                        .address(address)
                        .expectedArrivalTime(now)
                        .expectedDeliveryTime(now)
                        .name("my_name")
                        .withOrderReferenceFromOrder(order, false, false)
                        .type(RoutePointType.LOCKER_DELIVERY)
                        .build(),
                SimpleStrategies.BY_DATE_INTERVAL_MERGE
        );
    }
}
