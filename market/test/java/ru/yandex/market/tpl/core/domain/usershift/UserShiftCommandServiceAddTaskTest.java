package ru.yandex.market.tpl.core.domain.usershift;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.partner.OrderEventType;
import ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEvent;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus.NOT_CALLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DO_NOT_CALL_ENABLED;
import static ru.yandex.market.tpl.core.service.tracking.TrackingService.DO_NOT_CALL_DELIVERY_PREFIX;

@RequiredArgsConstructor
class UserShiftCommandServiceAddTaskTest extends TplAbstractTest {
    private static final long UID = 2234562L;

    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftReassignManager userShiftReassignManager;
    private final OrderHistoryEventRepository orderHistoryEventRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final TransactionTemplate transactionTemplate;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftCommandDataHelper helper;
    private final MovementGenerator movementGenerator;
    private final PickupPointRepository pickupPointRepository;

    private User user;
    private UserShift userShift;

    @BeforeEach
    void setUp() {
        user = testUserHelper.findOrCreateUser(UID);
        userShift = testUserHelper.createEmptyShift(user, LocalDate.now());
    }

    @Test
    void shouldAddPickupPoint() {
        testUserHelper.addDeliveryTaskToShift(user, userShift, orderGenerateService.createOrder());
        Optional<UserShift> userShiftOpt = userShiftRepository.findByIdWithRoutePoints(userShift.getId());
        assertThat(userShiftOpt).isNotEmpty();

        List<RoutePoint> pickupRoutePoints = userShiftOpt.get().streamPickupRoutePoints()
                .collect(Collectors.toList());
        assertThat(pickupRoutePoints).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldNotAddPickupTask_whenOnlyCargoDirectFlow(boolean isReturn) {
        //given
        var movement = movementGenerator.generate(
                MovementCommand.Create.builder()
                        .volume(BigDecimal.ONE)
                        .tags(List.of(Movement.TAG_DROPOFF_CARGO_RETURN))
                        .build()
        );
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ,
                        Long.parseLong("1111777"),
                        1L)
        );
        this.clearAfterTest(pickupPoint);
        //when
        userShiftCommandService.addLockerDeliveryTask(user,
                new UserShiftCommand.ManualAddRoutePoint(
                        userShift.getId(),
                        helper.taskDropOff(movement.getId(), pickupPoint.getId(), isReturn)));

        //then
        Optional<UserShift> userShiftOpt = userShiftRepository.findByIdWithRoutePoints(userShift.getId());
        assertThat(userShiftOpt).isNotEmpty();

        List<RoutePoint> pickupRoutePoints = userShiftOpt.get().streamPickupRoutePoints()
                .collect(Collectors.toList());

        assertThat(pickupRoutePoints.isEmpty()).isEqualTo(!isReturn);
    }

    @Test
    void shouldAddPickupTask_whenOnlyCargoReturnFlow() {
        //given
        var movement = movementGenerator.generate(
                MovementCommand.Create.builder()
                        .volume(BigDecimal.ONE)
                        .tags(List.of(Movement.TAG_DROPOFF_CARGO_RETURN))
                        .build()
        );
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ,
                        Long.parseLong("1111777"),
                        1L)
        );
        this.clearAfterTest(pickupPoint);
        //when
        userShiftCommandService.addLockerDeliveryTask(user,
                new UserShiftCommand.ManualAddRoutePoint(
                        userShift.getId(),
                        helper.taskDropOff(movement.getId(), pickupPoint.getId(), true)));

        //then
        Optional<UserShift> userShiftOpt = userShiftRepository.findByIdWithRoutePoints(userShift.getId());
        assertThat(userShiftOpt).isNotEmpty();

        List<RoutePoint> pickupRoutePoints = userShiftOpt.get().streamPickupRoutePoints()
                .collect(Collectors.toList());
        assertThat(pickupRoutePoints).hasSize(1);
    }

    @Test
    void shouldAddPickupPointEvenIfDropshipWasAddedFirst() {
        testDataFactory.addDropshipTask(userShift.getId());
        testUserHelper.addDeliveryTaskToShift(user, userShift, orderGenerateService.createOrder());
        testUserHelper.addDeliveryTaskToShift(user, userShift, orderGenerateService.createOrder());

        Optional<UserShift> userShiftOpt = userShiftRepository.findByIdWithRoutePoints(userShift.getId());
        assertThat(userShiftOpt).isNotEmpty();

        List<RoutePoint> pickupRoutePoints = userShiftOpt.get().streamPickupRoutePoints()
                .collect(Collectors.toList());
        assertThat(pickupRoutePoints).hasSize(1);
    }

    @Test
    void shouldHaveCallTaskWithCallNotRequired() {
        configurationServiceAdapter.mergeValue(DO_NOT_CALL_ENABLED, true);

        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .recipientNotes(DO_NOT_CALL_DELIVERY_PREFIX)
                .build());
        DeliveryTask deliveryTask = testUserHelper.addDeliveryTaskToShift(user, userShift, order);

        Optional<UserShift> userShiftOpt = userShiftRepository.findByIdWithCallTasks(userShift.getId());
        assertThat(userShiftOpt).isNotEmpty();

        assertThat(userShiftOpt.get().getCallToRecipientTasks())
                .extracting("status").containsOnly(CallToRecipientTaskStatus.CLIENT_ASK_NOT_TO_CALL);

        List<OrderHistoryEvent> clientEvents = orderHistoryEventRepository.findAllByOrderId(order.getId())
                .stream()
                .filter(e -> e.getType() == OrderEventType.CLIENT_MESSAGE)
                .collect(Collectors.toList());

        CallToRecipientTask callTask = userShiftOpt.get().getCallToRecipientTasks().iterator().next();

        assertThat(clientEvents).hasSize(1)
                .extracting(OrderHistoryEvent::getContext)
                .containsOnly("Клиент изменил необходимость звонка с: Не задано на: Клиент просил не звонить");

        assertThat(clientEvents).hasSize(1)
                .extracting(OrderHistoryEvent::getDifference)
                .extracting("deliveryTaskId", "callTaskId")
                .containsOnly(Tuple.tuple(deliveryTask.getId(), callTask.getId()));
    }

    @Test
    void shouldNotHaveCallTaskWithCallNotRequired() {
        configurationServiceAdapter.mergeValue(DO_NOT_CALL_ENABLED, false);

        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .recipientNotes(DO_NOT_CALL_DELIVERY_PREFIX)
                .build());
        testUserHelper.addDeliveryTaskToShift(user, userShift, order);

        Optional<UserShift> userShiftOpt = userShiftRepository.findByIdWithCallTasks(userShift.getId());
        assertThat(userShiftOpt).isNotEmpty();

        assertThat(userShiftOpt.get().getCallToRecipientTasks())
                .extracting("status").containsOnly(NOT_CALLED);
    }

    @Test
    void shouldHaveCallTaskInMultiOrderWithCallNotRequired() {
        configurationServiceAdapter.mergeValue(DO_NOT_CALL_ENABLED, true);

        AddressGenerator.AddressGenerateParam addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(GeoPointGenerator.generateLonLat())
                .street("Колотушкина")
                .house("1")
                .build();

        Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("451234")
                .deliveryDate(LocalDate.now())
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .itemsPrice(BigDecimal.valueOf(5000.0))
                        .build()
                )
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .paymentType(OrderPaymentType.PREPAID)
                .recipientNotes("Консьержка.")
                .build());

        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("4321231")
                .deliveryDate(LocalDate.now())
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .itemsPrice(BigDecimal.valueOf(3000.0))
                        .build()
                )
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .paymentType(OrderPaymentType.PREPAID)
                .recipientNotes("Консьержка." + DO_NOT_CALL_DELIVERY_PREFIX)
                .build());

        transactionTemplate.execute(t -> {
            userShiftReassignManager.assign(userShift, order1);
            userShiftReassignManager.assign(userShift, order2);
            return true;
        });


        Optional<UserShift> userShiftOpt = userShiftRepository.findByIdWithCallTasks(userShift.getId());
        assertThat(userShiftOpt).isNotEmpty();

        assertThat(userShiftOpt.get().getCallToRecipientTasks())
                .extracting("status").containsOnly(CallToRecipientTaskStatus.CLIENT_ASK_NOT_TO_CALL);

        List<OrderHistoryEvent> order1Events = orderHistoryEventRepository.findAllByOrderId(order1.getId());
        List<OrderHistoryEvent> order2Events = orderHistoryEventRepository.findAllByOrderId(order2.getId());

        List<OrderHistoryEvent> clientEvents = Stream.of(order1Events, order2Events)
                .flatMap(Collection::stream)
                .filter(e -> e.getType() == OrderEventType.CLIENT_MESSAGE)
                .collect(Collectors.toList());

        OrderDeliveryTask deliveryTask = transactionTemplate.execute(e ->
                        userShiftRepository.findTasksByOrderIds(List.of(order1.getId(),
                                order2.getId())).collect(Collectors.toList()))
                .iterator().next();

        CallToRecipientTask callTask = userShiftOpt.get().getCallToRecipientTasks().iterator().next();

        assertThat(clientEvents).hasSize(2)
                .extracting(OrderHistoryEvent::getContext)
                .containsOnly("Клиент изменил необходимость звонка с: Не задано на: Клиент просил не звонить");

        assertThat(clientEvents).hasSize(2)
                .extracting(OrderHistoryEvent::getDifference)
                .extracting("deliveryTaskId", "callTaskId")
                .containsOnly(Tuple.tuple(deliveryTask.getId(), callTask.getId()));
    }
}
