package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import com.google.common.collect.Range;
import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderDto;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderSummaryDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatisticsDto;
import ru.yandex.market.tpl.api.model.task.LockerSubtaskType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTasksDto;
import ru.yandex.market.tpl.api.model.task.PlaceForScanDto;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.PickupPointReturnReason;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.barcode_prefix.ReturnBarcodePrefixRepository;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnCommandService;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus;
import ru.yandex.market.tpl.core.domain.clientreturn.CreatedSource;
import ru.yandex.market.tpl.core.domain.clientreturn.commands.ClientReturnCommand;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.order.OrderPlace;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.sc.OrderStatusUpdate;
import ru.yandex.market.tpl.core.domain.sc.ScManager;
import ru.yandex.market.tpl.core.domain.sc.model.ScOrder;
import ru.yandex.market.tpl.core.domain.sc.model.ScOrderRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.tomorrowAtHour;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LockerDeliveryTaskTest {

    public static final String UNKNOWN_ORDER_ID = "UNKNOWN_ORDER_ID";
    public static final String EXTERNAL_ORDER_ID_1 = "EXTERNAL_ORDER_ID_1";
    public static final String EXTERNAL_ORDER_ID_2 = "EXTERNAL_ORDER_ID_2";
    public static final String RETURN_EXTERNAL_ORDER_ID_1 = "RETURN_EXTERNAL_ORDER_ID_1";
    public static final String RETURN_EXTERNAL_ORDER_ID_2 = "RETURN_EXTERNAL_ORDER_ID_2";

    private final TestDataFactory testDataFactory;
    private final EntityManager entityManager;
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final PickupPointRepository pickupPointRepository;
    private final OrderManager orderManager;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftQueryService userShiftQueryService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftRepository userShiftRepository;
    private final OrderRepository orderRepository;
    private final ClientReturnGenerator clientReturnGenerator;
    private final ClientReturnRepository clientReturnRepository;
    private final Clock clock;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final SortingCenterService sortingCenterService;
    private final ScManager scManager;
    private final ScOrderRepository scOrderRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final ClientReturnCommandService clientReturnCommandService;
    private final ReturnBarcodePrefixRepository barcodePrefixRepository;


    private User user;
    private Shift shift;
    private Long userShiftId;

    private Order order;
    private Order order2;
    private Order orderToReturn;
    private Order multiplaceOrderToReturn;

    private RoutePoint routePoint;
    private LockerDeliveryTask lockerDeliveryTask;

    private String clientReturnBarcodeExternalCreated1;

    @BeforeEach
    void init() {
        clientReturnBarcodeExternalCreated1 = barcodePrefixRepository.findBarcodePrefixByName(
                "CLIENT_RETURN_BARCODE_PREFIX_SF").getBarcodePrefix() + "3";
        user = testUserHelper.findOrCreateUser(1L);
        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        order = getPickupOrder(EXTERNAL_ORDER_ID_1, pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED, 1);
        order2 = getPickupOrder(EXTERNAL_ORDER_ID_2, pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED, 1);
        clientReturnCommandService.create(ClientReturnCommand.Create.builder()
                .barcode(clientReturnBarcodeExternalCreated1)
                .returnId(clientReturnBarcodeExternalCreated1)
                .pickupPoint(pickupPoint)
                .createdSource(CreatedSource.EXTERNAL)
                .source(Source.SYSTEM)
                .build());
        orderToReturn = getPickupOrder(RETURN_EXTERNAL_ORDER_ID_1, pickupPoint, geoPoint,
                OrderFlowStatus.DELIVERED_TO_PICKUP_POINT, 1);
        multiplaceOrderToReturn = getPickupOrder(RETURN_EXTERNAL_ORDER_ID_2, pickupPoint, geoPoint,
                OrderFlowStatus.DELIVERED_TO_PICKUP_POINT, 2);

        userShiftReassignManager.assign(userShift, order);
        userShiftReassignManager.assign(userShift, order2);

        testUserHelper.checkinAndFinishPickup(userShift);

        routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
        lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
        assertThat(lockerDeliveryTask.getSubtasks())
                .hasSize(2);

        configurationServiceAdapter.insertValue(ConfigurationProperties.IS_UPDATE_CANCEL_SC_130_160, true);
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED, true);
    }

    private Order getPickupOrder(
            String externalOrderId,
            PickupPoint pickupPoint,
            GeoPoint geoPoint,
            OrderFlowStatus orderFlowStatus,
            int placeCount
    ) {
        OrderGenerateService.OrderGenerateParam.OrderGenerateParamBuilder orderGenerateParamBuilder =
                OrderGenerateService.OrderGenerateParam.builder()
                        .externalOrderId(externalOrderId)
                        .deliveryDate(LocalDate.now(clock))
                        .deliveryServiceId(239L)
                        .pickupPoint(pickupPoint)
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .build())
                        .flowStatus(orderFlowStatus);

        if (placeCount > 1) {
            List<OrderPlaceDto> places = new ArrayList<>();
            for (int i = 0; i < placeCount; i++) {
                places.add(OrderPlaceDto.builder()
                        .barcode(new OrderPlaceBarcode("145", externalOrderId + "-" + (i + 1)))
                        .build());
            }

            orderGenerateParamBuilder.places(places);
        }

        Order order = orderGenerateService.createOrder(
                orderGenerateParamBuilder
                        .build());


        scManager.createOrders();
        ScOrder scOrder = scOrderRepository.findByYandexIdAndPartnerId(order.getExternalOrderId(),
                shift.getSortingCenter().getId())
                .orElseThrow();
        scManager.updateWhenCreatedOrder(scOrder.getYandexId(), "SC-" + scOrder.getId(), scOrder.getPartnerId());
        scManager.updateOrderStatuses(order.getExternalOrderId(), scOrder.getPartnerId(), List.of(
                new OrderStatusUpdate(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE.getCode(),
                        Instant.now(clock).minusSeconds(2 * 60 * 60 * 24)),
                new OrderStatusUpdate(OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF.getCode(),
                        Instant.now(clock).minusSeconds(2 * 60 * 60 * 24))
        ));
        return order;
    }

    @Test
    void getUserShiftStatistics() {
        UserShiftStatisticsDto statisticsDto = userShiftQueryService.getUserShiftStatisticsDto(user,
                routePoint.getUserShift().getId());
        assertThat(statisticsDto).isNotNull();
        assertThat(statisticsDto.getNumberOfAllTasks()).isEqualTo(1);
        assertThat(statisticsDto.getNumberOfFinishedTasks()).isEqualTo(0);
    }

    @Test
    void save() {
        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.NOT_STARTED);
        List<LockerSubtask> subtasks = lockerDeliveryTask.getSubtasks();
        assertThat(subtasks).hasSize(2);
        assertThat(subtasks)
                .extracting(LockerSubtask::getStatus)
                .containsOnly(LockerDeliverySubtaskStatus.NOT_STARTED);
        assertThat(subtasks)
                .extracting(LockerSubtask::getOrderId)
                .containsExactlyInAnyOrder(order.getId(), order2.getId());
    }

    @Test
    void shouldCancelTaskWhenCancelOrders() {
        for (Order o : List.of(order, order2)) {
            orderManager.cancelOrder(o);
        }

        assertThatFailSubtasksAndCancelTask();
    }

    @Test
    void shouldCancelTaskWhenRescheduleOrders() {
        for (Order o : List.of(order, order2)) {
            orderManager.rescheduleOrder(o, new Interval(tomorrowAtHour(18, clock), tomorrowAtHour(20, clock)),
                    Source.DELIVERY);
        }

        assertThatFailSubtasksAndCancelTask();
    }

    @Test
    void shouldNotCancelAllSubtasks() {
        orderManager.rescheduleOrder(order, new Interval(tomorrowAtHour(18, clock), tomorrowAtHour(20, clock)),
                Source.DELIVERY);

        LockerDeliveryTask savedTask = fetchLockerDeliveryTask();
        assertThat(savedTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.NOT_STARTED);
        var cancelledSubtask = savedTask.streamSubtask()
                .filter(task -> order.getId().equals(task.getOrderId()))
                .findFirst()
                .orElseThrow();
        assertThat(cancelledSubtask.getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FAILED);
        assertThat(cancelledSubtask.getFailReason().getType())
                .isEqualTo(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED);
        var notCancelledTask = savedTask.streamSubtask()
                .filter(task -> order2.getId().equals(task.getOrderId()))
                .findFirst()
                .orElseThrow();
        assertThat(notCancelledTask.getStatus()).isEqualTo(LockerDeliverySubtaskStatus.NOT_STARTED);
        assertThat(notCancelledTask.getFailReason()).isNull();
    }

    private void assertThatFailSubtasksAndCancelTask() {
        LockerDeliveryTask savedTask = fetchLockerDeliveryTask();
        assertThat(savedTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.CANCELLED);
        List<LockerSubtask> subtasks = savedTask.getSubtasks();
        assertThat(subtasks).extracting(LockerSubtask::getStatus)
                .containsOnly(LockerDeliverySubtaskStatus.FAILED);
    }

    private LockerDeliveryTask fetchLockerDeliveryTask() {
        List<Task<?>> tasks = entityManager.find(RoutePoint.class, routePoint.getId()).getTasks();
        return (LockerDeliveryTask) tasks.get(0);
    }

    @Test
    void shouldThrowIfFinishLoadingWithoutComment() {
        assertThatThrownBy(() -> {
            Set<Long> loadedOrderIds = Set.of();
            lockerDeliveryTask.finishLoadingLocker(
                    Instant.now(),
                    null,
                    ScanRequest.builder()
                            .successfullyScannedOrders(List.of())
                            .build()
            );
        })
                .isInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void shouldNotShowCancelledLockerOrder() {
        orderManager.cancelOrder(order);

        RoutePointDto routePointInfo = userShiftQueryService.getRoutePointInfo(user, routePoint.getId());
        assertThat(routePointInfo.getType()).isEqualTo(RoutePointType.LOCKER_DELIVERY);
        List<OrderDto> orders = ((LockerDeliveryTaskDto) routePointInfo.getTasks().get(0)).getOrders();
        assertThat(orders).hasSize(1);
        assertThat(orders).extracting(OrderDto::getExternalOrderId)
                .containsExactlyInAnyOrder(order2.getExternalOrderId());
    }

    @Test
    void shouldBeAbleToDeliverCancelledOrder() {
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.SEND_COURIER_RECEIVED_RETURN_PICKUP_EVENT_FOR_CANCELLATION_RETURN_ENABLED,
                true
        );
        finishLoadingLocker();
        orderManager.cancelOrder(order);

        userShiftCommandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Set.of(
                                new UnloadedOrder(order.getExternalOrderId(), null, List.of()),
                                new UnloadedOrder(orderToReturn.getExternalOrderId(), null,
                                        getPlaceBarcodes(orderToReturn)),
                                new UnloadedOrder(UNKNOWN_ORDER_ID, null, null))
                ));

        var updatedOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(updatedOrder.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.DELIVERED);
        assertThat(updatedOrder.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);

        dbQueueTestUtil.assertQueueHasSize(QueueType.REVERT_RETURN_ORDER, 1);
        assertThat(dbQueueTestUtil.getQueue(QueueType.CREATE_CLIENT_RETURN))
                .containsExactly(orderToReturn.getExternalOrderId());
    }

    @Test
    void shouldNotReturnNotPickupOrder() {
        finishLoadingLocker();

        Order deliveryOrder =
                orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder().build());
        userShiftCommandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Set.of(new UnloadedOrder(deliveryOrder.getExternalOrderId(), null, List.of()))
                ));
        assertThat(orderRepository.findByIdOrThrow(deliveryOrder.getId()).getOrderFlowStatus())
                .isEqualTo(deliveryOrder.getOrderFlowStatus());
    }

    @Test
    void shouldThrowIfFinishUnloadingDuplicateClientReturnBarcode() {
        ClientReturn existedClientReturn = clientReturnGenerator.generate();

        finishLoadingLocker();

        assertThatThrownBy(() -> userShiftCommandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Set.of(new UnloadedOrder(existedClientReturn.getBarcode(), null, List.of()))
                ))
        )
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void shouldCreateClientReturnAfterFinishUnloading() {
        finishLoadingLocker();

        userShiftCommandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Set.of(new UnloadedOrder(clientReturnBarcodeExternalCreated1, null, List.of()))
                ));

        Optional<ClientReturn> clientReturnO =
                clientReturnRepository.findByBarcode(clientReturnBarcodeExternalCreated1);

        assertThat(clientReturnO).isPresent();

        ClientReturn clientReturn = clientReturnO.get();
        assertThat(clientReturn.getStatus()).isEqualTo(ClientReturnStatus.RECEIVED);

        Optional<LockerSubtask> pickupClientReturnLockerSubtaskO = lockerDeliveryTask.streamSubtask()
                .filter(lst -> lst.getType() == LockerSubtaskType.PICKUP_CLIENT_RETURN)
                .filter(lst -> lst.getClientReturnId().equals(clientReturn.getId()))
                .findFirst();

        assertThat(pickupClientReturnLockerSubtaskO).isPresent();
        assertThat(pickupClientReturnLockerSubtaskO.get().getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);

        assertThat(dbQueueTestUtil.getQueue(QueueType.CREATE_CLIENT_RETURN)).containsExactly(clientReturnBarcodeExternalCreated1);
    }

    @Test
    void finishTask() {
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.SEND_COURIER_RECEIVED_RETURN_PICKUP_EVENT_FOR_CANCELLATION_RETURN_ENABLED,
                true
        );
        finishLoadingLocker();
        finishUnloadingLocker();

        assertThat(dbQueueTestUtil.getQueue(QueueType.CREATE_CLIENT_RETURN))
                .containsExactly(orderToReturn.getExternalOrderId());
    }

    @Test
    void showCorrectPlaceBarcodesAfterUnloadReturnMultiPlaceOrder() {
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.SEND_COURIER_RECEIVED_RETURN_PICKUP_EVENT_FOR_CANCELLATION_RETURN_ENABLED,
                true
        );
        finishLoadingLocker();
        finishUnloadingLocker();

        OrderDeliveryTasksDto tasksInfo = userShiftQueryService.getTasksInfo(user, true);

        assertThat(lockerDeliveryTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);

        assertThat(tasksInfo.getTasks())
                .extracting(OrderSummaryDto::getExternalOrderId)
                .contains(RETURN_EXTERNAL_ORDER_ID_2);

        OrderSummaryDto orderSummaryDto = StreamEx.of(tasksInfo.getTasks())
                .filter(t -> t.getExternalOrderId().equals(RETURN_EXTERNAL_ORDER_ID_2))
                .findFirst()
                .orElseThrow();

        assertThat(orderSummaryDto.getPlaces()).hasSize(2);
        assertThat(orderSummaryDto.getPlaces())
                .extracting(PlaceForScanDto::getBarcode)
                .containsExactlyInAnyOrderElementsOf(getPlaceBarcodes(multiplaceOrderToReturn));
    }

    @Test
    @Disabled("https://st.yandex-team.ru/MARKETTPL-7095")
    void updateCourierAndCancelOrderForOrdersToReturn() {
        ScOrder scOrder = scOrderRepository.findByYandexIdAndPartnerId(order.getExternalOrderId(),
                shift.getSortingCenter().getId())
                .orElseThrow();

        scManager.updateOrderStatuses(orderToReturn.getExternalOrderId(), scOrder.getPartnerId(), List.of(
                new OrderStatusUpdate(OrderStatusType.ORDER_SHIPPED_TO_SO_FF.getCode(),
                        Instant.now(clock).minusSeconds(2 * 60 * 60 * 24))
        ));


        finishLoadingLocker();
        //невыкуп из постомата
        assertThat(dbQueueTestUtil.getQueue(QueueType.UPDATE_ORDER).stream()
                .filter(e -> e.equals(orderToReturn.getExternalOrderId()))
                .count()).isEqualTo(0);
        assertThat(dbQueueTestUtil.getQueue(QueueType.CANCEL_ORDER).stream()
                .filter(e -> e.equals(orderToReturn.getExternalOrderId()))
                .count()).isEqualTo(0);

        finishUnloadingLocker();
        assertThat(dbQueueTestUtil.getQueue(QueueType.UPDATE_ORDER).stream()
                .filter(e -> e.equals(orderToReturn.getExternalOrderId()))
                .count()).isEqualTo(1);
        assertThat(dbQueueTestUtil.getQueue(QueueType.RETURN_ORDER).stream()
                .filter(e -> e.startsWith(orderToReturn.getExternalOrderId()))
                .count()).isEqualTo(1);
    }

    @Test
    void reassignOrders() {
        var user2 = testUserHelper.findOrCreateUser(2L);
        long userShift2Id = testDataFactory.createEmptyShift(shift.getId(), user2);
        var userShift2 = userShiftRepository.findByIdOrThrow(userShift2Id);

        userShiftReassignManager.reassignOrders(Set.of(order.getId()), Set.of(), Set.of(), user2.getId());

        routePoint = userShift2.streamDeliveryRoutePoints().findFirst().orElseThrow();
        lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();

        List<LockerSubtask> user1Subtasks = userShiftRepository.findByIdOrThrow(userShiftId)
                .streamDeliveryTasks()
                .flatMap(DeliveryTask::streamDeliveryOrderSubtasks)
                .select(LockerSubtask.class)
                .collect(Collectors.toList());
        assertThat(user1Subtasks)
                .hasSize(2)
                .extracting(LockerSubtask::getStatus)
                .containsExactlyInAnyOrder(LockerDeliverySubtaskStatus.NOT_STARTED, LockerDeliverySubtaskStatus.FAILED);
        assertThat(lockerDeliveryTask.getSubtasks())
                .hasSize(1)
                .extracting(LockerSubtask::getStatus)
                .containsExactlyInAnyOrder(LockerDeliverySubtaskStatus.NOT_STARTED);
    }

    @Test
    void reassignOrdersAfterFinishingTask() {
        var user2 = testUserHelper.findOrCreateUser(2L);
        long userShift2Id = testDataFactory.createEmptyShift(shift.getId(), user2);
        var userShift2 = userShiftRepository.findByIdOrThrow(userShift2Id);
        String date = userShift2.getShift().getShiftDate().plusDays(1).toString();
        String date1 = userShift2.getShift().getShiftDate().toString();
        orderManager.rescheduleOrder(order, Interval.fromRange(Range.closed(
                Instant.parse(date + "T08:00:00.00Z"),
                Instant.parse(date + "T12:00:00.00Z")
                )
        ), Source.OPERATOR);

        testUserHelper.arriveAtRoutePoint(routePoint);
        userShiftCommandService.finishLoadingLocker(user,
                new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                        lockerDeliveryTask.getId(), null, ScanRequest.builder()
                        .successfullyScannedOrders(List.of(order2.getId()))
                        .build()));
        orderManager.rescheduleOrder(order, Interval.fromRange(Range.closed(
                Instant.parse(date1 + "T08:00:00.00Z"),
                Instant.parse(date1 + "T12:00:00.00Z")
                )
        ), Source.COURIER);
        userShiftCommandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Set.of()
                ));

        assertThat(orderRepository.findById(order2.getId()).get().getOrderFlowStatus())
                .isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);

        userShiftReassignManager.reassignOrders(Set.of(order.getId()), Set.of(), Set.of(), user2.getId());

        routePoint = userShift2.streamDeliveryRoutePoints().findFirst().orElseThrow();
        lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();

        List<LockerSubtask> user1Subtasks = userShiftRepository.findByIdOrThrow(userShiftId)
                .streamDeliveryTasks()
                .flatMap(DeliveryTask::streamDeliveryOrderSubtasks)
                .select(LockerSubtask.class)
                .collect(Collectors.toList());
        assertThat(user1Subtasks)
                .hasSize(2)
                .extracting(LockerSubtask::getStatus)
                .containsExactlyInAnyOrder(LockerDeliverySubtaskStatus.FINISHED, LockerDeliverySubtaskStatus.FAILED);
        assertThat(lockerDeliveryTask.getSubtasks())
                .hasSize(1)
                .extracting(LockerSubtask::getStatus)
                .containsExactlyInAnyOrder(LockerDeliverySubtaskStatus.NOT_STARTED);
        assertThat(orderRepository.findById(order2.getId()).get().getOrderFlowStatus())
                .isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
    }

    @ParameterizedTest
    @MethodSource("provideExpectedData")
    void returnOrderFromPickupPointIfLoadingFailed(
            PickupPointReturnReason returnReason,
            OrderDeliveryStatus expectedOrderDeliveryStatusAfterFinishUnloading,
            OrderFlowStatus expectedOrderFlowStatusAfterShiftFinished,
            OrderFlowStatus expectedOrderFlowStatusAfterFailedUnload
    ) {
        testUserHelper.arriveAtRoutePoint(routePoint);
        checkAssignment(user, order, order2);

        userShiftCommandService.finishLoadingLocker(user,
                new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                        lockerDeliveryTask.getId(), null, ScanRequest.builder()
                        .successfullyScannedOrders(List.of(order.getId(), order2.getId()))
                        .build()));

        LockerDeliveryTask savedTask = fetchLockerDeliveryTask();
        Map<Long, LockerSubtask> orderIdToSubtaskMap = savedTask.getSubtasks().stream()
                .collect(Collectors.toMap(LockerSubtask::getOrderId, st -> st));
        assertThat(orderIdToSubtaskMap.get(order.getId()).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
        assertThat(orderIdToSubtaskMap.get(order2.getId()).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
        assertThat(savedTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.ORDERS_LOADED);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
        assertThat(order2.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
        checkAssignment(user, order, order2);

        userShiftCommandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Set.of(
                                new UnloadedOrder(
                                        order2.getExternalOrderId(),
                                        returnReason,
                                        List.of())
                        )
                )
        );

        assertThat(savedTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);
        List<Order> currentUserOrders = orderRepository.findCurrentUserOrders(user.getId());
        assertThat(currentUserOrders)
                .extracting(Order::getExternalOrderId)
                .containsExactlyInAnyOrder(order2.getExternalOrderId());

        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
        assertThat(order2.getDeliveryStatus()).isEqualTo(expectedOrderDeliveryStatusAfterFinishUnloading);
        assertThat(order2.getOrderFlowStatus()).isEqualTo(expectedOrderFlowStatusAfterFailedUnload);

        DeliverySubtask deliverySubtask = savedTask.streamDeliveryOrderSubtasks()
                .filter(st -> order2.getId().equals(st.getOrderId()))
                .findFirst()
                .orElseThrow();
        assertThat(deliverySubtask.getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FAILED);

        checkAssignment(user, order2);

        UserShift userShift = userShiftRepository.findById(userShiftId).orElseThrow();
        testUserHelper.finishFullReturnAtEnd(userShift);
        userShiftCommandService.finishUserShift(new UserShiftCommand.Finish(userShiftId));

        assertThat(order2.getOrderFlowStatus()).isEqualTo(expectedOrderFlowStatusAfterShiftFinished);

        checkAssignment(user);
    }

    @Test
    void reopenAndCancelLockerDeliveryTask() {
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.ORDER_IDS_TO_SEND_COURIER_RECEIVED_RETURN_PICKUP_EVENT_FOR_CANCELLATION_RETURN,
                RETURN_EXTERNAL_ORDER_ID_1
        );
        testUserHelper.arriveAtRoutePoint(routePoint);
        checkAssignment(user, order, order2);

        userShiftCommandService.finishLoadingLocker(user,
                new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                        lockerDeliveryTask.getId(), null, ScanRequest.builder()
                        .successfullyScannedOrders(List.of(order.getId(), order2.getId()))
                        .build()));
        checkAssignment(user, order, order2);

        userShiftCommandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Set.of(
                                new UnloadedOrder(
                                        order.getExternalOrderId(),
                                        PickupPointReturnReason.CELL_DID_NOT_OPEN,
                                        List.of()),
                                new UnloadedOrder(orderToReturn.getExternalOrderId(), null,
                                        getPlaceBarcodes(orderToReturn))
                        )));

        checkAssignment(user, order, orderToReturn);
        assertThat(dbQueueTestUtil.getQueue(QueueType.CREATE_CLIENT_RETURN))
                .containsExactly(orderToReturn.getExternalOrderId());

        // reopen locker task
        userShiftCommandService.reopenDeliveryTask(
                user,
                new UserShiftCommand.ReopenOrderDeliveryTask(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Source.COURIER)
        );

        checkAssignment(user, order, order2);

        assertThat(order.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.NOT_DELIVERED);
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
        assertThat(order2.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.NOT_DELIVERED);
        assertThat(order2.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
        assertThat(orderToReturn.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.DELIVERED);
        assertThat(orderToReturn.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
    }


    private static Stream<Arguments> provideExpectedData() {
        return Stream.of(
                Arguments.of(
                        PickupPointReturnReason.DIMENSIONS_EXCEEDS,
                        OrderDeliveryStatus.CANCELLED,
                        OrderFlowStatus.READY_FOR_RETURN,
                        OrderFlowStatus.READY_FOR_RETURN
                ),
                Arguments.of(
                        PickupPointReturnReason.CELL_DID_NOT_OPEN,
                        OrderDeliveryStatus.NOT_DELIVERED,
                        OrderFlowStatus.SORTING_CENTER_PREPARED,
                        OrderFlowStatus.TRANSPORTATION_RECIPIENT
                )
        );
    }

    private void finishUnloadingLocker() {
        //when
        userShiftCommandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Set.of(
                                new UnloadedOrder(order.getExternalOrderId(), null, List.of()),
                                new UnloadedOrder(orderToReturn.getExternalOrderId(), null,
                                        getPlaceBarcodes(orderToReturn)),
                                new UnloadedOrder(multiplaceOrderToReturn.getExternalOrderId(), null,
                                        getPlaceBarcodes(multiplaceOrderToReturn)),
                                new UnloadedOrder(UNKNOWN_ORDER_ID, null, null))
                ));

        //then
        LockerDeliveryTask savedTask = fetchLockerDeliveryTask();
        assertThat(savedTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.FINISHED);
        assertThat(savedTask.getExternalOrderIdsToReturn())
                .containsExactlyInAnyOrder(
                        orderToReturn.getExternalOrderId(),
                        multiplaceOrderToReturn.getExternalOrderId(),
                        UNKNOWN_ORDER_ID
                );

        assertThat(orderRepository.findByIdOrThrow(order.getId()).getOrderFlowStatus())
                .isEqualTo(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT);
        assertThat(orderRepository.findByIdOrThrow(orderToReturn.getId()).getOrderFlowStatus())
                .isEqualTo(OrderFlowStatus.READY_FOR_RETURN);
        assertThat(orderRepository.findByIdOrThrow(multiplaceOrderToReturn.getId()).getOrderFlowStatus())
                .isEqualTo(OrderFlowStatus.READY_FOR_RETURN);
        checkAssignment(user, order2, orderToReturn, multiplaceOrderToReturn);
    }

    private List<String> getPlaceBarcodes(Order order) {
        return StreamEx.of(order.getPlaces())
                .map(OrderPlace::getBarcode)
                .filter(Objects::nonNull)
                .map(OrderPlaceBarcode::getBarcode)
                .toList();
    }

    private void finishLoadingLocker() {
        //when
        testUserHelper.arriveAtRoutePoint(routePoint);
        checkAssignment(user, order, order2);

        userShiftCommandService.finishLoadingLocker(user,
                new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP, "help me!"),
                        ScanRequest.builder()
                                .successfullyScannedOrders(List.of(order.getId()))
                                .build()));

        //then
        LockerDeliveryTask savedTask = fetchLockerDeliveryTask();
        Map<Long, LockerSubtask> orderIdToSubtaskMap = savedTask.getSubtasks().stream()
                .collect(Collectors.toMap(LockerSubtask::getOrderId, st -> st));
        assertThat(orderIdToSubtaskMap.get(order.getId()).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FINISHED);
        assertThat(orderIdToSubtaskMap.get(order2.getId()).getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FAILED);
        assertThat(orderIdToSubtaskMap.get(order2.getId()).getFailReason().getType()).isEqualTo(OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP);
        assertThat(savedTask.getStatus()).isEqualTo(LockerDeliveryTaskStatus.ORDERS_LOADED);
        assertThat(orderRepository.findByIdOrThrow(order.getId()).getOrderFlowStatus())
                .isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
        checkAssignment(user, order, order2);
    }

    private void checkAssignment(User user, Order... orders) {
        List<Order> currentUserOrders = orderRepository.findCurrentUserOrders(user.getId());
        assertThat(currentUserOrders).containsExactlyInAnyOrder(orders);
    }

}
