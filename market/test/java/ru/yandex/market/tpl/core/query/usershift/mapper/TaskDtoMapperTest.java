package ru.yandex.market.tpl.core.query.usershift.mapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.CollectDropshipTakePhotoDto;
import ru.yandex.market.tpl.api.model.order.LocationDetailsDto;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.OrderTagsDto;
import ru.yandex.market.tpl.api.model.order.OrderType;
import ru.yandex.market.tpl.api.model.order.PhotoRequirementType;
import ru.yandex.market.tpl.api.model.order.TransferType;
import ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.CollectDropshipTaskDto;
import ru.yandex.market.tpl.api.model.task.CollectDropshipTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryFailReasonDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.api.model.task.PlaceForScanDto;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.TaskDto;
import ru.yandex.market.tpl.api.model.task.TaskType;
import ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.base.property.TplPropertyType;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus;
import ru.yandex.market.tpl.core.domain.clientreturn.mapper.ClientReturnItemDtoMapper;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.OrderProperty;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.order.property.TplOrderProperties;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseRepository;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseSchedule;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.pickup.generator.PickupPointGenerator;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequest;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestGenerateService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.CollectDropshipTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderReturnTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.query.order.mapper.OrderDeliveryAddressDtoMapper;
import ru.yandex.market.tpl.core.query.order.mapper.OrderDtoMapper;
import ru.yandex.market.tpl.core.query.usershift.mapper.task_mapper.GenericTaskDtoMapper;
import ru.yandex.market.tpl.core.task.projection.TaskFlowType;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.order.CallRequirement.DO_NOT_CALL;
import static ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus.CLIENT_ASK_NOT_TO_CALL;
import static ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus.SUCCESS;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DO_NOT_CALL_ENABLED;
import static ru.yandex.market.tpl.core.service.tracking.TrackingService.DO_NOT_CALL_DELIVERY_PREFIX;

/**
 * @author valter
 */
@Log4j2
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class TaskDtoMapperTest {

    private final TaskDtoMapper taskDtoMapper;
    private final GenericTaskDtoMapper genericTaskDtoMapper;
    private final OrderDtoMapper orderDtoMapper;
    private final TestUserHelper testUserHelper;
    private final UserShiftCommandService commandService;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandDataHelper helper;
    private final MovementGenerator movementGenerator;
    private final AddressGenerator addressGenerator;
    private final Clock clock;
    private final OrderRepository orderRepository;
    private final OrderWarehouseRepository orderWarehouseRepository;
    private final UserShiftReassignManager userShiftReassignManager;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;
    private final OrderDeliveryAddressDtoMapper orderDeliveryAddressDtoMapper;
    private final ClientReturnItemDtoMapper clientReturnItemDtoMapper;
    private final ClientReturnGenerator clientReturnGenerator;
    private final TestDataFactory testDataFactory;
    private final SpecialRequestGenerateService specialRequestGenerateService;
    private final TestUserHelper userHelper;
    private final UserShiftCommandService userShiftCommandService;
    private final TransactionTemplate transactionTemplate;
    private final ClientReturnGenerator generator;
    private final PickupPointRepository pickupPointRepository;
    private final ClientReturnRepository clientReturnRepository;

    private UserShift userShift;
    private UserShift userShift2;
    private OrderDeliveryTask deliveryTask1;
    private OrderDeliveryTask deliveryTask2;
    private OrderDeliveryTask deliveryTask3;
    private OrderDeliveryTask clientReturnTask;
    private LockerDeliveryTask lockerDeliveryTask;
    private ClientReturn clientReturn;
    private OrderPickupTask pickupTask;
    private OrderReturnTask returnTask;
    private Movement movement;
    private CollectDropshipTask collectDropshipTask;
    private Order order1;
    private Order order2;
    private Order order3;
    private Order multiPlaceOrder;
    private User user;
    private SpecialRequest specialRequest;
    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;

    @AfterEach
    void after() {
        ClockUtil.initFixed(clock);
    }

    @BeforeEach
    void init() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));

        order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .paymentType(OrderPaymentType.CARD)
                .build());

        order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .paymentType(OrderPaymentType.CARD)
                .build());

        var locker = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 444L, DeliveryService.DEFAULT_DS_ID);
        order3 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.PAID)
                .paymentType(OrderPaymentType.PREPAID)
                .pickupPoint(locker)
                .build());

        multiPlaceOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .paymentType(OrderPaymentType.CARD)
                .places(List.of(
                        OrderPlaceDto.builder().barcode(new OrderPlaceBarcode("123", "place1")).build(),
                        OrderPlaceDto.builder().barcode(new OrderPlaceBarcode("123", "place2")).build()
                ))
                .build());

        clientReturn = clientReturnGenerator.generateReturnFromClient();

        movement = movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouse(orderWarehouseRepository.saveAndFlush(new OrderWarehouse(
                        "123",
                        "corp",
                        addressGenerator.generateWarehouseAddress(
                                AddressGenerator.AddressGenerateParam.builder()
                                        .street("Пушкина")
                                        .house("Колотушкина")
                                        .apartment("10")
                                        .floor(1)
                                        .build()
                        ),
                        Arrays.stream(DayOfWeek.values())
                                .collect(Collectors.toMap(
                                                d -> d,
                                                d -> new OrderWarehouseSchedule(
                                                        d,
                                                        OffsetTime.of(LocalTime.of(9, 27),
                                                                DateTimeUtil.DEFAULT_ZONE_ID),
                                                        OffsetTime.of(LocalTime.of(10, 27),
                                                                DateTimeUtil.DEFAULT_ZONE_ID)
                                                )
                                        )
                                ),
                        List.of("223322223322"),
                        "Спросить старшего",
                        "Иван Дропшипов")))
                .build());

        specialRequest = specialRequestGenerateService.createSpecialRequest(
                SpecialRequestGenerateService.SpecialRequestGenerateParam.builder()
                        .pickupPointId(order3.getPickupPoint().getId())
                        .build()
        );

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskUnpaid("addr1", 12, order1.getId()))
                .routePoint(helper.taskUnpaid("addr2", 13, order2.getId()))
                .routePoint(helper.taskUnpaid("addr3", 14, multiPlaceOrder.getId()))
                .routePoint(helper.clientReturn("addr4", 15, clientReturn.getId()))
                .routePoint(helper.taskCollectDropship(LocalDate.now(clock), movement))
                .routePoint(helper.taskLockerDelivery(order3.getId(), order3.getPickupPoint().getId()))
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        userShift = userShiftRepository.findByIdOrThrow(userShiftId);

        deliveryTask1 =
                userShift.streamOrderDeliveryTasks()
                        .filter(t -> Objects.equals(t.getOrderId(), order1.getId()))
                        .findFirst()
                        .orElseThrow();
        deliveryTask2 =
                userShift.streamOrderDeliveryTasks()
                        .filter(t -> Objects.equals(t.getOrderId(), order2.getId()))
                        .findFirst()
                        .orElseThrow();
        deliveryTask3 =
                userShift.streamOrderDeliveryTasks()
                        .filter(t -> Objects.equals(t.getOrderId(), multiPlaceOrder.getId()))
                        .findFirst()
                        .orElseThrow();
        clientReturnTask =
                userShift.streamOrderDeliveryTasks()
                        .filter(t -> Objects.equals(t.getClientReturnId(), clientReturn.getId()))
                        .findFirst()
                        .orElseThrow();

        lockerDeliveryTask =
                userShift.streamLockerDeliveryTasks()
                        .findFirst()
                        .orElseThrow();

        testDataFactory.addFlowTask(userShiftId, TaskFlowType.TEST_FLOW, List.of(specialRequest));

        collectDropshipTask = userShift.streamCollectDropshipTasks().findFirst().orElseThrow();
        pickupTask = userShift.streamPickupRoutePoints()
                .findFirst().orElseThrow().streamPickupTasks().findFirst().orElseThrow();
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        var pickupRoutePoint = pickupTask.getRoutePoint();
        commandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
                userShift.getId(), pickupRoutePoint.getId(),
                new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShift.getId())
        ));
        commandService.startOrderPickup(user, new UserShiftCommand.StartScan(
                userShiftId, pickupRoutePoint.getId(), pickupTask.getId()
        ));
        String comment = "my_comment";

        assertThatPickupTaskExpectedMapped();

        commandService.pickupOrders(user, new UserShiftCommand.FinishScan(
                userShiftId, pickupRoutePoint.getId(), pickupTask.getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(List.of(order1.getId(), order2.getId(), order3.getId(),
                                multiPlaceOrder.getId()))
                        .comment(comment)
                        .finishedAt(clock.instant())
                        .build()
        ));
        commandService.finishLoading(user, new UserShiftCommand.FinishLoading(
                userShiftId, pickupRoutePoint.getId(), pickupTask.getId())
        );
        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShiftId, deliveryTask1.getRoutePoint().getId(), deliveryTask1.getId(),
                new OrderDeliveryFailReason(
                        OrderDeliveryTaskFailReasonType.CLIENT_REFUSED, null
                )
        ));
        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShiftId, deliveryTask3.getRoutePoint().getId(), deliveryTask3.getId(),
                new OrderDeliveryFailReason(
                        OrderDeliveryTaskFailReasonType.NO_CONTACT, null
                )
        ));
        returnTask = userShift.streamReturnRoutePoints()
                .findFirst().orElseThrow().streamReturnTasks().findFirst().orElseThrow();
    }

    private void assertThatPickupTaskExpectedMapped() {
        List<TaskDto> actualPickupTasks = taskDtoMapper.mapTasks(pickupTask.getRoutePoint(), List.of(), user);

        assertThat(actualPickupTasks)
                .usingElementComparatorIgnoringFields("orders", "destinations", "expendables")
                .isEqualTo(expectedPickupTasks());

        OrderPickupTaskDto actualPickupTask = (OrderPickupTaskDto) actualPickupTasks.iterator().next();

        assertThat(actualPickupTask.getOrders())
                .usingComparatorForElementFieldsWithNames(
                        (Comparator<List<PlaceForScanDto>>) (o1, o2) -> {
                            int result = o1.size() == o2.size() ? (o1.containsAll(o2) ? 0 : 1) : 1;
                            return result;
                        },
                        "places")
                .usingFieldByFieldElementComparator()
                .usingElementComparatorIgnoringFields("delivery")
                .containsExactlyInAnyOrderElementsOf(expectedOrdersForPickup());
    }

    @Test
    void mapTasks() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(false);

        assertThat(taskDtoMapper.mapTasks(deliveryTask1.getRoutePoint(), List.of(order1, order2), user))
                .isEqualTo(expectedDeliveryTasks1());
        assertThat(taskDtoMapper.mapTasks(deliveryTask2.getRoutePoint(), List.of(order1, order2), user))
                .isEqualTo(expectedDeliveryTasks2());
        assertThat(taskDtoMapper.mapTasks(collectDropshipTask.getRoutePoint(), List.of(order1, order2), user))
                .isEqualTo(List.of(expectedCollectDropshipTask()));
        List<TaskDto> actualReturnTasks = taskDtoMapper.mapTasks(returnTask.getRoutePoint(), List.of(), user);
        assertThat(actualReturnTasks)
                .usingElementComparatorIgnoringFields("orders", "batches", "outsideOrders", "destinations")
                .isEqualTo(expectedReturnTasks());
        OrderReturnTaskDto actualReturnTask = (OrderReturnTaskDto) actualReturnTasks.iterator().next();

        assertThat(actualReturnTask.getOrders())
                .usingComparatorForElementFieldsWithNames(
                        (Comparator<List<PlaceForScanDto>>) (o1, o2) -> {
                            int result = o1.size() == o2.size() ? (o1.containsAll(o2) ? 0 : 1) : 1;
                            return result;
                        },
                        "places")
                .usingFieldByFieldElementComparator()
                .usingElementComparatorIgnoringFields("delivery")
                .containsExactlyInAnyOrderElementsOf(expectedOrdersForReturn());
    }

    @Test
    void mapTasksWithR18Orders() {
        jdbcTemplate.update("UPDATE order_item SET cargo_type_codes = string_to_array(?, ',')::integer[]" +
                        " WHERE id = ?",
                "10", order1.getItems().get(0).getId());
        entityManager.detach(order1);
        order1 = orderRepository.findByIdOrThrow(order1.getId());

        var orderDeliveryTaskDtoBefore = taskDtoMapper
                .mapOrderDeliveryTaskDto(deliveryTask1, order1, List.of(order1), null, false, null, null);
        assertThat(orderDeliveryTaskDtoBefore.isR18()).isEqualTo(false);
        assertThat(orderDeliveryTaskDtoBefore.getTags())
                .doesNotContain(OrderTagsDto.IS_R18, OrderTagsDto.SHOW_DOCUMENT);
        assertThat(orderDeliveryTaskDtoBefore.getOrder().isR18()).isEqualTo(false);
        assertThat(orderDeliveryTaskDtoBefore.getOrder().getTags())
                .doesNotContain(OrderTagsDto.IS_R18, OrderTagsDto.SHOW_DOCUMENT);
        assertThat(orderDeliveryTaskDtoBefore.getTaskOrdinal()).isEqualTo(1);

        jdbcTemplate.update("UPDATE order_item SET cargo_type_codes = string_to_array(?, ',')::integer[]" +
                        " WHERE id = ?",
                "10,20", order1.getItems().get(0).getId());
        entityManager.detach(order1);
        order1 = orderRepository.findByIdOrThrow(order1.getId());

        var orderDeliveryTaskDtoAfter = taskDtoMapper
                .mapOrderDeliveryTaskDto(deliveryTask1, order1, List.of(order1), null, false, null, null);
        assertThat(orderDeliveryTaskDtoAfter.isR18()).isEqualTo(true);
        assertThat(orderDeliveryTaskDtoAfter.getTags()).contains(OrderTagsDto.IS_R18, OrderTagsDto.SHOW_DOCUMENT);
        assertThat(orderDeliveryTaskDtoAfter.getOrder().isR18()).isEqualTo(true);
        assertThat(orderDeliveryTaskDtoAfter.getOrder().getTags()).contains(OrderTagsDto.IS_R18,
                OrderTagsDto.SHOW_DOCUMENT);
    }

    @Test
    void mapTasksWithDoNotCallOrders() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        List<Order> multiOrders = createMultiOrders();

        Order multiOrder = multiOrders.get(0);
        var deliveryTaskMultiOrder =
                userShift2.streamOrderDeliveryTasks()
                        .filter(t -> Objects.equals(t.getOrderId(), multiOrder.getId()))
                        .findFirst()
                        .orElseThrow();

        var orderDeliveryTaskDto = taskDtoMapper.mapOrderDeliveryTaskDto(deliveryTaskMultiOrder, multiOrder,
                multiOrders, null, true, null, null);

        assertThat(orderDeliveryTaskDto.getCallRequirement()).isEqualTo(DO_NOT_CALL);
        assertThat(orderDeliveryTaskDto.getCallStatus()).isEqualTo(CLIENT_ASK_NOT_TO_CALL);
    }

    @Test
    void mapTasksWithDoNotCallOrdersNegative() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(false);

        List<Order> multiOrders = createMultiOrders();

        Order multiOrder = multiOrders.get(0);
        var deliveryTaskMultiOrder =
                userShift2.streamOrderDeliveryTasks()
                        .filter(t -> Objects.equals(t.getOrderId(), multiOrder.getId()))
                        .findFirst()
                        .orElseThrow();

        var orderDeliveryTaskDto = taskDtoMapper.mapOrderDeliveryTaskDto(deliveryTaskMultiOrder, multiOrder,
                multiOrders, null, true, null, null);

        assertThat(orderDeliveryTaskDto.getCallRequirement()).isNull();
        assertThat(orderDeliveryTaskDto.getCallStatus()).isEqualTo(SUCCESS);
        assertThat(orderDeliveryTaskDto.getDeliveryCodeLength()).isNull();
    }

    @Test
    void orderIds() {
        assertThat(taskDtoMapper.getOrdersMap(List.of(
                order1.getExternalOrderId(), order2.getExternalOrderId()
        ))).isEqualTo(Map.of(
                order1.getExternalOrderId(), order1.getId(),
                order2.getExternalOrderId(), order2.getId()
        ));
    }

    @DisplayName("Проверяем маппинг заказа для б2б")
    @Test
    void mapB2bCustomersOrderTest() {
        List<Order> multiOrdersTo2BCustomers = createMultiOrdersTo2BCustomers();
        Order multiOrder = multiOrdersTo2BCustomers.get(0);
        var deliveryTaskMultiOrder =
                userShift2.streamOrderDeliveryTasks()
                        .filter(t -> Objects.equals(t.getOrderId(), multiOrder.getId()))
                        .findFirst()
                        .orElseThrow();

        var orderDeliveryTaskDto = taskDtoMapper.mapOrderDeliveryTaskDto(deliveryTaskMultiOrder, multiOrder,
                multiOrdersTo2BCustomers, null, true, null, null);

        assertThat(orderDeliveryTaskDto.getTags()).contains(OrderTagsDto.IS_NEED_ORDER_CODE_VALIDATE);
        assertThat(orderDeliveryTaskDto.getDeliveryCodeLength()).isNotNull();
    }

    @DisplayName("Проверяем маппинг обычного заказа")
    @Test
    void mapSimpleOrderTest() {
        List<Order> multiOrders = createMultiOrders();

        Order multiOrder = multiOrders.get(0);
        var deliveryTaskMultiOrder =
                userShift2.streamOrderDeliveryTasks()
                        .filter(t -> Objects.equals(t.getOrderId(), multiOrder.getId()))
                        .findFirst()
                        .orElseThrow();

        var orderDeliveryTaskDto = taskDtoMapper.mapOrderDeliveryTaskDto(deliveryTaskMultiOrder, multiOrder,
                multiOrders, null, true, null, null);

        assertThat(orderDeliveryTaskDto.getDeliveryCodeLength()).isNull();
        assertThat(orderDeliveryTaskDto.getTags()).doesNotContain(OrderTagsDto.IS_NEED_ORDER_CODE_VALIDATE);
    }

    @DisplayName("Проверяем маппинг клиентского возврата.")
    @Test
    void mapClientReturn() {
        var orderDeliveryTaskDto = taskDtoMapper.mapClientReturnOrderDeliveryTaskDto(clientReturnTask, clientReturn,
                false);
        var clientReturnTaskDto = orderDeliveryTaskDto.getClientReturnOrderDto();
        var returnDto = clientReturnTaskDto.getReturnDto();
        var logisticReq = clientReturn.getLogisticRequestPointFrom();
        var clientReturnItems = clientReturnTaskDto.getItems();
        assertThat(returnDto.getAddressDetails()).isEqualTo(orderDeliveryAddressDtoMapper.mapAddress(logisticReq));
        assertThat(returnDto.getAddress()).isEqualTo(logisticReq.getAddress());
        assertThat(clientReturnItems).containsExactlyInAnyOrderElementsOf(
                clientReturn
                        .getItems()
                        .stream()
                        .map(clientReturnItemDtoMapper::mapReturnItemDto)
                        .collect(Collectors.toList())
        );
        assertThat(clientReturnTaskDto.getExternalReturnId()).isEqualTo(clientReturn.getExternalReturnId());
        assertThat(orderDeliveryTaskDto.getType()).isEqualTo(TaskType.CLIENT_RETURN);
    }

    @Test
    void mapFlowTaskTest() {
        var tasks = taskDtoMapper.mapTasks(lockerDeliveryTask.getRoutePoint(), List.of(order3), user);

        assertThat(tasks).hasSize(2);
        var task1 = tasks.get(0);
        var task2 = tasks.get(1);
        assertThat(task1.getType()).isEqualTo(TaskType.FLOW_TASK);
        assertThat(task2.getType()).isEqualTo(TaskType.LOCKER_DELIVERY);
        assertThat(task1.getTaskOrdinal()).isEqualTo(1);
        assertThat(task2.getTaskOrdinal()).isEqualTo(2);
    }

    List<TaskDto> expectedPickupTasks() {
        var pickupTaskDto = new OrderPickupTaskDto();
        List<OrderScanTaskDto.OrderForScanDto> orders = expectedOrdersForPickup();
        genericTaskDtoMapper.mapGenericTask(pickupTaskDto, pickupTask);
        pickupTaskDto.setStatus(OrderPickupTaskStatus.IN_PROGRESS);
        pickupTaskDto.setOrders(orders);
        pickupTaskDto.setCompletedOrders(List.of());
        pickupTaskDto.setSkippedOrders(List.of());
        pickupTaskDto.setComment(null);
        pickupTaskDto.setDestinations(List.of());
        pickupTaskDto.setBatches(Set.of());
        pickupTaskDto.setInvitedArrivalTimeToLoading(null);
        pickupTaskDto.setVehicleInstances(List.of());
        return List.of(pickupTaskDto);
    }

    private List<OrderScanTaskDto.OrderForScanDto> expectedOrdersForPickup() {
        var lockerSubTask = lockerDeliveryTask.streamLockerDeliverySubtasks()
                .filter(st -> Objects.equals(st.getOrderId(), order3.getId()))
                .findFirst()
                .orElseThrow();
        return List.of(
                new OrderScanTaskDto.OrderForScanDto(false, lockerSubTask.getParentId(), order3.getExternalOrderId(),
                        OrderScanTaskDto.ScanOrderDisplayMode.OK, "Доставка в 10:02",
                        null, null, OrderType.LOCKER, List.of(new PlaceForScanDto(order3.getExternalOrderId())),
                        OrderFlowStatus.SORTING_CENTER_PREPARED, null, 1),
                new OrderScanTaskDto.OrderForScanDto(false, deliveryTask1.getParentId(), order1.getExternalOrderId(),
                        OrderScanTaskDto.ScanOrderDisplayMode.OK, "Доставка в 12:02",
                        null, null, OrderType.CLIENT, List.of(new PlaceForScanDto(order1.getExternalOrderId())),
                        OrderFlowStatus.SORTING_CENTER_PREPARED, null, 2),
                new OrderScanTaskDto.OrderForScanDto(false, deliveryTask2.getParentId(), order2.getExternalOrderId(),
                        OrderScanTaskDto.ScanOrderDisplayMode.OK, "Доставка в 13:02",
                        null, null, OrderType.CLIENT, List.of(new PlaceForScanDto(order2.getExternalOrderId())),
                        OrderFlowStatus.SORTING_CENTER_PREPARED, null, 3),
                new OrderScanTaskDto.OrderForScanDto(false, deliveryTask3.getParentId(),
                        multiPlaceOrder.getExternalOrderId(),
                        OrderScanTaskDto.ScanOrderDisplayMode.OK, "Доставка в 14:02",
                        null, null, OrderType.CLIENT,
                        List.of(
                                new PlaceForScanDto("place1"),
                                new PlaceForScanDto("place2")
                        ), OrderFlowStatus.SORTING_CENTER_PREPARED, null, 4)
        );
    }

    List<TaskDto> expectedDeliveryTasks1() {
        var deliveryTaskDto1 = new OrderDeliveryTaskDto();
        genericTaskDtoMapper.mapGenericTask(deliveryTaskDto1, deliveryTask1);
        deliveryTaskDto1.setStatus(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        deliveryTaskDto1.setMultiOrderId(deliveryTask1.getParentId());
        deliveryTaskDto1.setMultiOrder(false);
        deliveryTaskDto1.setOrder(orderDtoMapper.mapOrderDto(
                order1, deliveryTask1.getExpectedDeliveryTime(), deliveryTask1.getOrdinalNumber()
        ));
        deliveryTaskDto1.setFailReason(new OrderDeliveryFailReasonDto(
                OrderDeliveryTaskFailReasonType.CLIENT_REFUSED, null, Source.COURIER));
        deliveryTaskDto1.setActions(List.of(new OrderDeliveryTaskDto.Action(OrderDeliveryTaskDto.ActionType.REOPEN)));
        deliveryTaskDto1.setPhotos(Collections.emptyList());
        deliveryTaskDto1.setTransferType(TransferType.HAND_TO_HAND);

        deliveryTaskDto1.setCallStatus(CallToRecipientTaskStatus.FAILED);
        deliveryTaskDto1.setCallAttemptCount(0);
        deliveryTaskDto1.setTimeForFitting(Duration.ZERO);

        return List.of(deliveryTaskDto1);
    }

    List<TaskDto> expectedDeliveryTasks2() {
        var deliveryTaskDto2 = new OrderDeliveryTaskDto();
        genericTaskDtoMapper.mapGenericTask(deliveryTaskDto2, deliveryTask2);
        deliveryTaskDto2.setStatus(OrderDeliveryTaskStatus.NOT_DELIVERED);
        deliveryTaskDto2.setMultiOrderId(deliveryTask2.getParentId());
        deliveryTaskDto2.setMultiOrder(false);
        deliveryTaskDto2.setOrder(orderDtoMapper.mapOrderDto(
                order2, deliveryTask2.getExpectedDeliveryTime(), deliveryTask2.getOrdinalNumber()));
        deliveryTaskDto2.setPhotos(Collections.emptyList());
        deliveryTaskDto2.setTransferType(TransferType.HAND_TO_HAND);

        deliveryTaskDto2.setCallStatus(CallToRecipientTaskStatus.NOT_CALLED);
        deliveryTaskDto2.setCallAttemptCount(0);
        deliveryTaskDto2.setTimeForFitting(Duration.ZERO);

        return List.of(deliveryTaskDto2);
    }

    List<TaskDto> expectedReturnTasks() {
        var returnTaskDto = new OrderReturnTaskDto();
        List<OrderScanTaskDto.OrderForScanDto> orders = expectedOrdersForReturn();
        genericTaskDtoMapper.mapGenericTask(returnTaskDto, returnTask);
        returnTaskDto.setStatus(OrderReturnTaskStatus.NOT_STARTED);
        returnTaskDto.setOrders(orders);
        returnTaskDto.setCompletedOrders(List.of());
        returnTaskDto.setSkippedOrders(List.of());
        returnTaskDto.setComment(null);
        returnTaskDto.setBatches(Set.of());
        returnTaskDto.setOutsideOrders(Set.of());
        returnTaskDto.setTakePhoto(new CollectDropshipTakePhotoDto(PhotoRequirementType.REQUIRED));
        return List.of(returnTaskDto);
    }

    private List<OrderScanTaskDto.OrderForScanDto> expectedOrdersForReturn() {
        var lockerSubTask = lockerDeliveryTask.streamLockerDeliverySubtasks()
                .filter(st -> Objects.equals(st.getOrderId(), order3.getId()))
                .findFirst()
                .orElseThrow();
        return List.of(new OrderScanTaskDto.OrderForScanDto(
                        false, deliveryTask1.getParentId(), order1.getExternalOrderId(), null, null,
                        order1.getDelivery().getDeliveryAddress().getAddress(),
                        order1.getDelivery().getDeliveryAddress().getAddressPersonalId(), OrderType.CLIENT,
                        List.of(new PlaceForScanDto(order1.getExternalOrderId())),
                        null, null, null),
                new OrderScanTaskDto.OrderForScanDto(
                        false, deliveryTask2.getParentId(), order2.getExternalOrderId(), null, null,
                        order2.getDelivery().getDeliveryAddress().getAddress(),
                        order2.getDelivery().getDeliveryAddress().getAddressPersonalId(), OrderType.CLIENT,
                        List.of(new PlaceForScanDto(order2.getExternalOrderId())),
                        null, null, null),
                new OrderScanTaskDto.OrderForScanDto(
                        false, deliveryTask3.getParentId(), multiPlaceOrder.getExternalOrderId(), null, null,
                        multiPlaceOrder.getDelivery().getDeliveryAddress().getAddress(),
                        multiPlaceOrder.getDelivery().getDeliveryAddress().getAddressPersonalId(), OrderType.CLIENT,
                        List.of(
                                new PlaceForScanDto("place1"),
                                new PlaceForScanDto("place2")),
                        null, null, null),
                new OrderScanTaskDto.OrderForScanDto(
                        false, lockerSubTask.getParentId(), order3.getExternalOrderId(), null, null,
                        order3.getDelivery().getDeliveryAddress().getAddress(),
                        order3.getDelivery().getDeliveryAddress().getAddressPersonalId(), OrderType.LOCKER,
                        List.of(new PlaceForScanDto(order3.getExternalOrderId())),
                        null, null, null)
        );
    }

    private CollectDropshipTaskDto expectedCollectDropshipTask() {
        var collectDropshipTaskDto = new CollectDropshipTaskDto();
        genericTaskDtoMapper.mapGenericTask(collectDropshipTaskDto, collectDropshipTask);
        collectDropshipTaskDto.setStatus(CollectDropshipTaskStatus.NOT_STARTED);

        var locationDetailsDto = new LocationDetailsDto();
        locationDetailsDto.setContact("Иван Дропшипов");
        locationDetailsDto.setAddress("г. Москва, Пушкина, д. Колотушкина, кв. 10, этаж 1");
        locationDetailsDto.setPhones(List.of("223322223322"));
        locationDetailsDto.setDescription("Спросить старшего");
        locationDetailsDto.setWorkingHours("09:27 - 10:27");

        collectDropshipTaskDto.setLocationDetails(locationDetailsDto);
        collectDropshipTaskDto.setTakePhoto(new CollectDropshipTakePhotoDto(PhotoRequirementType.OPTIONAL));
        return collectDropshipTaskDto;
    }

    private List<Order> createMultiOrders() {
        var user = testUserHelper.findOrCreateUser(2L);
        userShift2 = testUserHelper.createEmptyShift(user, LocalDate.now(clock));

        AddressGenerator.AddressGenerateParam addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(GeoPointGenerator.generateLonLat())
                .street("Колотушкина")
                .house("1")
                .build();

        Order multiOrder1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("451234")
                .deliveryDate(LocalDate.now(clock))
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

        Order multiOrder2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("4321231")
                .deliveryDate(LocalDate.now(clock))
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
                .recipientNotes("Консьержка. " + DO_NOT_CALL_DELIVERY_PREFIX)
                .build());

        userShiftReassignManager.assign(userShift2, multiOrder1);
        userShiftReassignManager.assign(userShift2, multiOrder2);

        testUserHelper.checkinAndFinishPickup(userShift2);

        return List.of(multiOrder1, multiOrder2);
    }

    private List<Order> createMultiOrdersTo2BCustomers() {
        var user = testUserHelper.findOrCreateUser(3L);
        userShift2 = testUserHelper.createEmptyShift(user, LocalDate.now(clock));

        AddressGenerator.AddressGenerateParam addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(GeoPointGenerator.generateLonLat())
                .build();

        String verificationCodeProperty = TplOrderProperties.Names.VERIFICATION_CODE_BEFORE_HANDING.name();
        String customerTypeProperty = TplOrderProperties.Names.CUSTOMER_TYPE.name();
        Map<String, OrderProperty> propertyMap = Map.of(
                verificationCodeProperty,
                new OrderProperty(null, TplPropertyType.STRING, verificationCodeProperty, "verificationCode"),
                customerTypeProperty,
                new OrderProperty(null, TplPropertyType.STRING, customerTypeProperty,
                        TplOrderProperties.CustomerValues.B2B_CUSTOMER_TYPE.name())
        );

        Order multiOrder1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("4512341")
                .deliveryDate(LocalDate.now(clock))
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
                .properties(propertyMap)
                .build());

        Order multiOrder2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("4512342")
                .deliveryDate(LocalDate.now(clock))
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
                .properties(propertyMap)
                .build());

        userShiftReassignManager.assign(userShift2, multiOrder1);
        userShiftReassignManager.assign(userShift2, multiOrder2);

        testUserHelper.checkinAndFinishPickup(userShift2);

        return List.of(multiOrder1, multiOrder2);
    }

    @Test
    void mapClientReturnDto() {
        ClockUtil.initFixed(clock, LocalDateTime.of(LocalDate.now().plusYears(1000), LocalTime.now()));

        User user = userHelper.findOrCreateUser(824145649L, LocalDate.now(clock));
        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .build();
        long userShiftId = userShiftCommandService.createUserShift(createCommand);


        transactionTemplate.execute(status -> {
            ClientReturn clientReturn1 = generator.generate();
            PickupPoint pickupPoint1 = PickupPointGenerator.generatePickupPoint(3463476346L);
            clientReturn1.setPickupPoint(pickupPoint1);
            clientReturn1.setStatus(ClientReturnStatus.READY_FOR_RECEIVED);
            pickupPointRepository.save(pickupPoint1);
            clientReturnRepository.save(clientReturn1);

            ClientReturn clientReturn12 = generator.generate();
            clientReturn12.setPickupPoint(pickupPoint1);
            clientReturn12.setStatus(ClientReturnStatus.READY_FOR_RECEIVED);
            clientReturnRepository.save(clientReturn12);

            ClientReturn clientReturn2 = generator.generate();
            PickupPoint pickupPoint2 = PickupPointGenerator.generatePickupPoint(3467456344L);
            clientReturn2.setPickupPoint(pickupPoint2);
            clientReturn2.setStatus(ClientReturnStatus.READY_FOR_RECEIVED);
            pickupPointRepository.save(pickupPoint2);
            clientReturnRepository.save(clientReturn2);


            ClientReturn clientReturn3 = generator.generate();
            clientReturn3.setPickupPoint(pickupPoint1);
            clientReturn3.setStatus(ClientReturnStatus.RECEIVED);
            clientReturnRepository.save(clientReturn3);

            Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .pickupPoint(pickupPoint1)
                    .build());
            LockerDeliveryTask lockerDeliveryTask = testUserHelper.addLockerDeliveryTaskToShift(user,
                    userShiftRepository.getById(userShiftId), order);

            List<TaskDto> list = taskDtoMapper.mapTasks(lockerDeliveryTask.getRoutePoint(), Set.of(order), user);
            assertThat(list.size()).isEqualTo(1);
            LockerDeliveryTaskDto taskDto = (LockerDeliveryTaskDto) list.get(0);
            List<ClientReturnDto> clietnReturns = taskDto.getClientReturns();
            assertThat(clietnReturns.size()).isEqualTo(2);
            Set<Long> ids = clietnReturns.stream().map(ClientReturnDto::getId).collect(Collectors.toSet());
            assertThat(ids).contains(clientReturn1.getId());
            assertThat(ids).contains(clientReturn12.getId());

            return status;
        });
    }
}
