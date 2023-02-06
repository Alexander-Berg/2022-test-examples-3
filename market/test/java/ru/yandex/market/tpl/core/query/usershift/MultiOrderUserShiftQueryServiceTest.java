package ru.yandex.market.tpl.core.query.usershift;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.OrderSummaryDto;
import ru.yandex.market.tpl.api.model.order.OrderTagsDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatisticsDto;
import ru.yandex.market.tpl.api.model.task.MultiOrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTasksDto;
import ru.yandex.market.tpl.api.model.task.RemainingOrderDeliveryTasksDto;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.TaskDto;
import ru.yandex.market.tpl.api.model.task.TaskType;
import ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.CallToRecipientTask;
import ru.yandex.market.tpl.core.domain.usershift.DeliverySubtask;
import ru.yandex.market.tpl.core.domain.usershift.DeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.order.CallRequirement.DO_NOT_CALL;
import static ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus.CLIENT_ASK_NOT_TO_CALL;
import static ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus.SUCCESS;
import static ru.yandex.market.tpl.api.model.user.UserMode.DEFAULT_MODE;
import static ru.yandex.market.tpl.api.model.user.UserMode.SOFT_MODE;
import static ru.yandex.market.tpl.api.model.user.UserMode.STRICT_MODE;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAtHour;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.tomorrowAtHour;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DO_NOT_CALL_ENABLED;
import static ru.yandex.market.tpl.core.service.tracking.TrackingService.DO_NOT_CALL_DELIVERY_PREFIX;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
@Slf4j
class MultiOrderUserShiftQueryServiceTest {

    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftQueryService queryService;
    private final Clock clock;
    private final TestDataFactory testDataFactory;
    private final UserPropertyService userPropertyService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final ClientReturnGenerator clientReturnGenerator;

    private User user;
    private UserShift userShift;
    private Order order1;
    private Order order2;
    private CallToRecipientTask callTask;

    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;

    @BeforeEach
    void init() {
        user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));

        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        AddressGenerator.AddressGenerateParam addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(geoPoint)
                .street("Колотушкина")
                .house("1")
                .build();
        order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(addressGenerateParam)
                .build());
        order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(addressGenerateParam)
                .items(
                        OrderGenerateService.OrderGenerateParam.Items.builder()
                                .isFashion(true)
                                .build()
                )
                .build());

        Instant deliveryTime = order1.getDelivery().getDeliveryIntervalFrom();
        RoutePointAddress myAddress = new RoutePointAddress("my_address", geoPoint);

        NewDeliveryRoutePointData delivery1 = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(deliveryTime)
                .expectedDeliveryTime(deliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(order1, false, false)
                .build();

        NewDeliveryRoutePointData delivery2 = NewDeliveryRoutePointData.builder()
                .address(myAddress)
                .expectedArrivalTime(deliveryTime)
                .expectedDeliveryTime(deliveryTime)
                .name("my_name")
                .withOrderReferenceFromOrder(order2, false, false)
                .build();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .active(true)
                .shiftId(shift.getId())
                .routePoint(delivery1)
                .routePoint(delivery2)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        callTask = userShift.streamCallTasks().findFirst().orElseThrow();
    }

    @Test
    void getUserShiftStatistics() {
        UserShiftStatisticsDto statisticsDto = queryService.getUserShiftStatisticsDto(user, userShift.getId());
        assertThat(statisticsDto).isNotNull();
        assertThat(statisticsDto.getNumberOfAllTasks()).isEqualTo(1);
        assertThat(statisticsDto.getNumberOfFinishedTasks()).isEqualTo(0);
    }

    @Test
    void getTasksInfo() {
        OrderDeliveryTasksDto tasksInfo = queryService.getTasksInfo(user, false);

        assertThat(tasksInfo.getTasks()).hasSize(2);
        assertThat(tasksInfo.getTasks())
                .extracting(OrderSummaryDto::getMultiOrderId)
                .containsExactly(String.valueOf(callTask.getId()), String.valueOf(callTask.getId()));

        assertThat(tasksInfo.getTasks())
                .extracting(OrderSummaryDto::isMultiOrder)
                .containsExactly(true, true);

        assertThat(tasksInfo.getTasks())
                .extracting(OrderSummaryDto::isCanBeGrouped)
                .containsExactly(true, true);

        assertThat(tasksInfo.getTasks())
                .extracting(OrderSummaryDto::getPlaceCount)
                .containsExactly(1, 1);
    }

    @Test
    void getTasksInfoDifferentUserMode() {
        OrderDeliveryTasksDto tasksInfo = queryService.getTasksInfo(user, false);

        assertThat(tasksInfo.getTasks()).hasSize(2);

        userPropertyService.addPropertyToUser(user, UserProperties.USER_MODE, STRICT_MODE.name());
        testUserHelper.clearUserPropertiesCache();

        tasksInfo = queryService.getTasksInfo(user, false);

        assertThat(tasksInfo.getTasks()).hasSize(0);

        userPropertyService.addPropertyToUser(user, UserProperties.USER_MODE, SOFT_MODE.name());
        testUserHelper.clearUserPropertiesCache();

        tasksInfo = queryService.getTasksInfo(user, false);

        assertThat(tasksInfo.getTasks()).hasSize(2);

        userPropertyService.addPropertyToUser(user, UserProperties.USER_MODE, DEFAULT_MODE.name());
        testUserHelper.clearUserPropertiesCache();

        tasksInfo = queryService.getTasksInfo(user, false);

        assertThat(tasksInfo.getTasks()).hasSize(2);

    }

    @Test
    void getLockerTasksInfo() {
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        PickupPoint pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L);

        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        Order orderToReturn = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("123")
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        Order orderToReturn2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("456")
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        String unknownOrderExternalOrderId = "unknown";
        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);
        DeliveryTask lockerDeliveryTask = userShift.streamDeliveryTasks()
                .filter(dt -> dt.streamDeliveryOrderSubtasks()
                        .map(DeliverySubtask::getOrderId)
                        .toList().contains(order.getId()))
                .findFirst()
                .orElseThrow();

        testUserHelper.checkinAndFinishPickup(userShift);
        testUserHelper.finishDelivery(userShift.getCurrentRoutePoint(), false);
        testUserHelper.arriveAtRoutePoint(lockerDeliveryTask.getRoutePoint());
        commandService.finishLoadingLocker(user, new UserShiftCommand.FinishLoadingLocker(userShift.getId(),
                lockerDeliveryTask.getRoutePoint().getId(), lockerDeliveryTask.getId(), null, ScanRequest.builder()
                .successfullyScannedOrders(List.of(order.getId()))
                .build()));
        commandService.finishUnloadingLocker(user, new UserShiftCommand.FinishUnloadingLocker(
                userShift.getId(),
                lockerDeliveryTask.getRoutePoint().getId(),
                lockerDeliveryTask.getId(),
                Set.of(
                        new UnloadedOrder(orderToReturn.getExternalOrderId(), null, List.of()),
                        new UnloadedOrder(orderToReturn2.getExternalOrderId(), null, List.of()),
                        new UnloadedOrder(unknownOrderExternalOrderId, null, List.of())
                )
        ));

        OrderDeliveryTasksDto tasksInfo = queryService.getTasksInfo(user, null);
        List<OrderSummaryDto> lockerSummaries = tasksInfo.getTasks().stream()
                .filter(s -> s.getType() == TaskType.LOCKER_DELIVERY)
                .collect(Collectors.toList());
        Map<Boolean, List<OrderSummaryDto>> isReturnToLockerOrderSummaries = lockerSummaries.stream()
                .collect(Collectors.partitioningBy(OrderSummaryDto::getHasReturn));

        assertThat(lockerSummaries)
                .hasSize(4)
                .extracting(OrderSummaryDto::getMultiOrderId)
                .containsOnly("pickup" + lockerDeliveryTask.getId());
        assertThat(isReturnToLockerOrderSummaries.get(true))
                .hasSize(3)
                .extracting(OrderSummaryDto::getExternalOrderId, OrderSummaryDto::isCanBeGrouped)
                .containsExactlyInAnyOrder(
                        tuple(orderToReturn.getExternalOrderId(), true),
                        tuple(orderToReturn2.getExternalOrderId(), true),
                        tuple(unknownOrderExternalOrderId, true)
                );
    }

    @Test
    void getDoNotCallTasks() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        createMultiOrders();

        OrderDeliveryTasksDto tasksInfo = queryService.getTasksInfo(user, null);

        Long secondMultiOrderId = userShift.streamCallTasks().collect(Collectors.toList()).get(1).getId();

        List<OrderSummaryDto> summaryDtoList = tasksInfo.getTasks().stream()
                .filter(e -> e.getMultiOrderId().equals(secondMultiOrderId.toString()))
                .collect(Collectors.toList());

        assertThat(summaryDtoList).extracting("callRequirement")
                .containsOnly(DO_NOT_CALL);

        List<CallToRecipientTaskStatus> callTaskStatuses = summaryDtoList.stream()
                .map(OrderSummaryDto::getCallStatus)
                .collect(Collectors.toList());
        assertThat(callTaskStatuses).containsOnly(CLIENT_ASK_NOT_TO_CALL);
    }

    @Test
    void getRemainingTasksInfo() {

        RemainingOrderDeliveryTasksDto remainingTasksInfo = queryService.getRemainingTasksInfo(user);

        assertThat(remainingTasksInfo.getOrders()).hasSize(2);
        assertThat(remainingTasksInfo.getOrders())
                .extracting(OrderSummaryDto::getMultiOrderId)
                .containsExactly(String.valueOf(callTask.getId()), String.valueOf(callTask.getId()));

        assertThat(remainingTasksInfo.getOrders())
                .extracting(OrderSummaryDto::isMultiOrder)
                .containsExactly(true, true);

        assertThat(remainingTasksInfo.getOrders())
                .extracting(OrderSummaryDto::isCanBeGrouped)
                .containsExactly(true, true);
    }

    @Test
    void getRemainingTasksDoNotCall() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        createMultiOrders();

        RemainingOrderDeliveryTasksDto remainingTasksInfo = queryService.getRemainingTasksInfo(user);

        Long secondMultiOrderId = userShift.streamCallTasks().collect(Collectors.toList()).get(1).getId();

        List<OrderSummaryDto> summaryDtoList = remainingTasksInfo.getOrders().stream()
                .filter(e -> e.getMultiOrderId().equals(secondMultiOrderId.toString()))
                .collect(Collectors.toList());

        assertThat(summaryDtoList).extracting("callRequirement")
                .containsOnly(DO_NOT_CALL);

        List<CallToRecipientTaskStatus> callTaskStatuses = summaryDtoList.stream()
                .map(OrderSummaryDto::getCallStatus)
                .collect(Collectors.toList());
        assertThat(callTaskStatuses).containsOnly(CLIENT_ASK_NOT_TO_CALL);
    }

    @Test
    void getDeliveryTaskInfoDoNotCall() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        createMultiOrders();

        var secondMultiOrderId = userShift.streamCallTasks().collect(Collectors.toList()).get(1).getId();
        var multiOrderDeliveryTaskDto = queryService.getMultiOrderDeliveryTaskDto(user, secondMultiOrderId.toString());

        for (OrderDeliveryTaskDto task : multiOrderDeliveryTaskDto.getTasks()) {
            var routePointId = queryService.getRoutePointId(user, task.getId());
            var deliveryTaskInfo = queryService.getDeliveryTaskInfo(user, routePointId, task.getId());

            assertThat(deliveryTaskInfo.getCallRequirement()).isEqualTo(DO_NOT_CALL);
            assertThat(deliveryTaskInfo.getCallStatus()).isEqualTo(CLIENT_ASK_NOT_TO_CALL);
        }
    }

    @Test
    void getMultiOrderDeliveryTasksInfoDoNotCall() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        createMultiOrders();

        var secondMultiOrderId = userShift.streamCallTasks().collect(Collectors.toList()).get(1).getId();
        var multiOrderDeliveryTaskDto = queryService.getMultiOrderDeliveryTaskDto(user, secondMultiOrderId.toString());

        assertThat(multiOrderDeliveryTaskDto.getTasks())
                .extracting("callRequirement")
                .containsOnly(DO_NOT_CALL);
        assertThat(multiOrderDeliveryTaskDto.getTasks())
                .extracting("callStatus")
                .containsOnly(CLIENT_ASK_NOT_TO_CALL);
    }

    @Test
    void getMultiOrderDeliveryTasksInfoDoNotCallNegative() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(false);

        createMultiOrders();

        var secondMultiOrderId = userShift.streamCallTasks().collect(Collectors.toList()).get(1).getId();
        var multiOrderDeliveryTaskDto = queryService.getMultiOrderDeliveryTaskDto(user, secondMultiOrderId.toString());

        for (OrderDeliveryTaskDto task : multiOrderDeliveryTaskDto.getTasks()) {
            assertThat(task.getCallRequirement()).isNull();
        }
        assertThat(multiOrderDeliveryTaskDto.getTasks())
                .extracting("callStatus")
                .containsOnly(SUCCESS);
    }

    @Test
    void getDeliveryTasksInfoDoNotCall() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        createMultiOrders();

        var secondMultiOrderId = userShift.streamCallTasks().collect(Collectors.toList()).get(1).getId();
        var multiOrderDeliveryTaskDto = queryService.getMultiOrderDeliveryTaskDto(user, secondMultiOrderId.toString());

        OrderDeliveryTaskDto firstTask = multiOrderDeliveryTaskDto.getTasks().get(0);
        List<Long> taskIds = multiOrderDeliveryTaskDto.getTasks().stream()
                .map(TaskDto::getId).collect(Collectors.toList());

        var routePointId = queryService.getRoutePointId(user, firstTask.getId());
        var deliveryTaskInfo = queryService.getDeliveryTasksInfo(user, routePointId, taskIds);
        assertThat(deliveryTaskInfo.getTasks())
                .extracting("callRequirement")
                .containsOnly(DO_NOT_CALL);
        assertThat(deliveryTaskInfo.getTasks())
                .extracting("callStatus")
                .containsOnly(CLIENT_ASK_NOT_TO_CALL);
    }

    @Test
    void getRoutePointInfo() {
        testUserHelper.checkinAndFinishPickup(userShift);

        RoutePointDto routePointInfo = queryService.getRoutePointInfo(user, userShift.getCurrentRoutePoint().getId());

        assertThat(routePointInfo.getTasks()).hasSize(2);

        for (TaskDto task : routePointInfo.getTasks()) {
            OrderDeliveryTaskDto deliveryTaskDto = (OrderDeliveryTaskDto) task;

            assertThat(deliveryTaskDto.isMultiOrder()).isTrue();
            assertThat(deliveryTaskDto.getMultiOrderId()).isEqualTo(String.valueOf(callTask.getId()));
        }
    }

    @Test
    void getMultiOrderDeliveryTaskDto() {
        String multiOrderId = String.valueOf(callTask.getId());
        MultiOrderDeliveryTaskDto multiOrderDeliveryTaskDto =
                queryService.getMultiOrderDeliveryTaskDto(user, multiOrderId);

        assertThat(multiOrderDeliveryTaskDto.getTasks()).hasSize(2);

        for (OrderDeliveryTaskDto task : multiOrderDeliveryTaskDto.getTasks()) {

            assertThat(task.isMultiOrder()).isTrue();
            assertThat(task.getMultiOrderId()).isEqualTo(multiOrderId);
        }
    }

    @Test
    void getMultiOrderDeliveryTaskDtoWithPartialReturnOrderFilter() {
        String multiOrderId = String.valueOf(callTask.getId());
        MultiOrderDeliveryTaskDto multiOrderDeliveryTaskDto;

        multiOrderDeliveryTaskDto = queryService.getMultiOrderDeliveryTaskDto(user, multiOrderId);
        assertThat(multiOrderDeliveryTaskDto.getTasks()).hasSize(2);

        multiOrderDeliveryTaskDto = queryService.getMultiOrderDeliveryTaskDto(user, multiOrderId, true);
        assertThat(multiOrderDeliveryTaskDto.getTasks()).hasSize(1);
        multiOrderDeliveryTaskDto.getTasks().forEach(
                task -> {
                    assertThat(task.getTags()).contains(OrderTagsDto.PARTIAL_RETURN);
                    assertThat(task.getOrder().getExternalOrderId()).isEqualTo(order2.getExternalOrderId());
                }
        );

        multiOrderDeliveryTaskDto = queryService.getMultiOrderDeliveryTaskDto(user, multiOrderId, false);
        assertThat(multiOrderDeliveryTaskDto.getTasks()).hasSize(1);
        multiOrderDeliveryTaskDto.getTasks().forEach(
                task -> {
                    assertThat(task.getTags()).doesNotContain(OrderTagsDto.PARTIAL_RETURN);
                    assertThat(task.getOrder().getExternalOrderId()).isEqualTo(order1.getExternalOrderId());
                }
        );

    }

    @Test
    void getMultiOrderDeliveryTaskDtoWithoutClientReturn() {

        var routePoint = testDataFactory.createEmptyRoutePoint(user, userShift.getId());
        long routePointId = routePoint.getId();
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();

        var tod = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );

        String multiOrderId = String.valueOf(callTask.getId());
        MultiOrderDeliveryTaskDto multiOrderDeliveryTaskDto;

        multiOrderDeliveryTaskDto = queryService.getMultiOrderDeliveryTaskDto(user, multiOrderId);
        assertThat(multiOrderDeliveryTaskDto.getTasks()).hasSize(2);


    }

    @Test
    void checkFailReasonForOrderInCreatedStatusAfterItWasNotPickedUp() {
        Order notPreparedOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("451234")
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(GeoPointGenerator.generateLonLat())
                        .build())
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .itemsPrice(BigDecimal.valueOf(5000.0))
                        .build()
                )
                .flowStatus(OrderFlowStatus.CREATED)
                .paymentType(OrderPaymentType.PREPAID)
                .build());
        userShiftReassignManager.assign(userShift, notPreparedOrder);


        testUserHelper.checkinAndFinishPickup(
                userShift,
                List.of(order1.getId()),
                List.of(order2.getId()),
                true,
                true
        );

        Map<Long, Optional<OrderDeliveryFailReason>> map = userShift.streamOrderDeliveryTasks()
                .toMap(OrderDeliveryTask::getOrderId, t -> Optional.ofNullable(t.getFailReason()));

        assertThat(map.get(notPreparedOrder.getId()).get().getType())
                .isEqualTo(OrderDeliveryTaskFailReasonType.ORDER_NOT_PREPARED);
        assertThat(map.get(order1.getId()))
                .isEqualTo(Optional.empty());
        assertThat(map.get(order2.getId()).get().getType())
                .isEqualTo(OrderDeliveryTaskFailReasonType.ORDER_NOT_ACCEPTED);
    }

    @Test
    void shouldRescheduleMultiOrderDeliveryTaskToNextDay() {
        testUserHelper.checkinAndFinishPickup(userShift);

        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();

        Instant from = tomorrowAtHour(18, clock);
        Instant to = tomorrowAtHour(20, clock);
        Map<LocalDate, List<LocalTimeInterval>> intervals = Map.of(LocalDate.now(clock).plusDays(1),
                List.of(LocalTimeInterval.valueOf("18:00-20:00")));

        UserShiftCommand.RescheduleMultiOrderCommand rescheduleCommand =
                new UserShiftCommand.RescheduleMultiOrderCommand(
                        userShift.getId(),
                        currentRoutePoint.getId(),
                        String.valueOf(callTask.getId()),
                        DeliveryReschedule.fromCourier(
                                user, from, to, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST,
                                ""),
                        DateTimeUtil.atDefaultZone(LocalDateTime.of(LocalDate.now(clock),
                                LocalTime.of(18, 20))),
                        userShift.getZoneId()
                );

        commandService.rescheduleMultiOrder(user, rescheduleCommand, intervals);

        userShift.streamOrderDeliveryTasks()
                .forEach(t -> {
                    assertThat(t.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
                    assertThat(t.getFailReason().getType()).isEqualTo(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED);
                    assertThat(t.getFailReason().getSource()).isEqualTo(Source.COURIER);
                });
    }

    @Test
    void shouldRescheduleMultiOrderDeliveryIntervalsFromCombinator() {
        testUserHelper.checkinAndFinishPickup(userShift);

        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();

        Instant from = tomorrowAtHour(18, clock);
        Instant to = tomorrowAtHour(20, clock);

        Map<LocalDate, List<LocalTimeInterval>> intervals = Map.of(LocalDate.now(clock).plusDays(1),
                List.of(LocalTimeInterval.valueOf("18:00-20:00")));

        UserShiftCommand.RescheduleMultiOrderCommand rescheduleCommand =
                new UserShiftCommand.RescheduleMultiOrderCommand(
                        userShift.getId(),
                        currentRoutePoint.getId(),
                        String.valueOf(callTask.getId()),
                        DeliveryReschedule.fromCourier(
                                user, from, to, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST,
                                ""),
                        DateTimeUtil.atDefaultZone(LocalDateTime.of(LocalDate.now(clock),
                                LocalTime.of(18, 20))),
                        userShift.getZoneId()
                );

        commandService.rescheduleMultiOrder(user, rescheduleCommand, intervals);

        userShift.streamOrderDeliveryTasks()
                .forEach(t -> {
                    assertThat(t.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
                    assertThat(t.getFailReason().getType()).isEqualTo(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED);
                    assertThat(t.getFailReason().getSource()).isEqualTo(Source.COURIER);
                });
    }

    @Test
    void throwNotFoundDayRescheduleMultiOrderDeliveryIntervalsFromCombinator() {
        testUserHelper.checkinAndFinishPickup(userShift);

        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();

        Instant from = tomorrowAtHour(18, clock);
        Instant to = tomorrowAtHour(20, clock);

        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);

        Map<LocalDate, List<LocalTimeInterval>> intervals = Map.of(tomorrow.plusDays(1),
                List.of(LocalTimeInterval.valueOf("18:00-20:00")));

        UserShiftCommand.RescheduleMultiOrderCommand rescheduleCommand =
                new UserShiftCommand.RescheduleMultiOrderCommand(
                        userShift.getId(),
                        currentRoutePoint.getId(),
                        String.valueOf(callTask.getId()),
                        DeliveryReschedule.fromCourier(
                                user, from, to, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST,
                                ""),
                        DateTimeUtil.atDefaultZone(LocalDateTime.of(LocalDate.now(clock),
                                LocalTime.of(18, 20))),
                        userShift.getZoneId()
                );

        assertThatThrownBy(() -> commandService.rescheduleMultiOrder(user, rescheduleCommand, intervals))
                .isInstanceOf(TplInvalidParameterException.class)
                .hasMessage("Day is not available, delivery: " +
                        tomorrow + ", partner: " +
                        intervals.keySet());
    }

    @Test
    void throwNotFoundIntervalRescheduleMultiOrderDeliveryIntervalsFromCombinator() {
        testUserHelper.checkinAndFinishPickup(userShift);

        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();

        Instant from = tomorrowAtHour(18, clock);
        Instant to = tomorrowAtHour(20, clock);

        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);

        Map<LocalDate, List<LocalTimeInterval>> intervals = Map.of(
                LocalDate.now(clock), List.of(LocalTimeInterval.valueOf("09:00-11:00")),
                tomorrow,
                List.of(LocalTimeInterval.valueOf("10:00-14:00")));

        UserShiftCommand.RescheduleMultiOrderCommand rescheduleCommand =
                new UserShiftCommand.RescheduleMultiOrderCommand(
                        userShift.getId(),
                        currentRoutePoint.getId(),
                        String.valueOf(callTask.getId()),
                        DeliveryReschedule.fromCourier(
                                user, from, to, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST,
                                ""),
                        DateTimeUtil.atDefaultZone(LocalDateTime.of(LocalDate.now(clock),
                                LocalTime.of(18, 20))),
                        userShift.getZoneId()
                );
        assertThatThrownBy(() -> commandService.rescheduleMultiOrder(user, rescheduleCommand, intervals))
                .isInstanceOf(TplInvalidParameterException.class)
                .hasMessage("Interval is not available, delivery: " +
                        LocalTimeInterval.valueOf("18:00-20:00") + ", partner: " +
                        intervals.get(tomorrow));
    }

    @Test
    void shouldRescheduleMultiOrderDeliveryTaskToSameDay() {
        testUserHelper.checkinAndFinishPickup(userShift);

        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();

        Instant from = todayAtHour(18, clock);
        Instant to = todayAtHour(22, clock);

        Map<LocalDate, List<LocalTimeInterval>> intervals = Map.of(LocalDate.now(clock),
                List.of(LocalTimeInterval.valueOf("18:00-22:00")));

        UserShiftCommand.RescheduleMultiOrderCommand rescheduleCommand =
                new UserShiftCommand.RescheduleMultiOrderCommand(
                        userShift.getId(),
                        currentRoutePoint.getId(),
                        String.valueOf(callTask.getId()),
                        DeliveryReschedule.fromCourier(
                                user, from, to, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST,
                                ""),
                        DateTimeUtil.atDefaultZone(LocalDateTime.of(LocalDate.now(clock),
                                LocalTime.of(18, 20))),
                        userShift.getZoneId()
                );

        commandService.rescheduleMultiOrder(user, rescheduleCommand, intervals);

        LocalTimeInterval newInterval = LocalTimeInterval.valueOf("18:00-22:00");

        userShift.streamOrderDeliveryTasks()
                .forEach(t -> {
                    assertThat(t.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
                    assertThat(newInterval.contains(DateTimeUtil.toLocalTime(t.getRoutePoint().getExpectedDateTime()))).isTrue();
                });
    }

    private void createMultiOrders() {
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

        userShiftReassignManager.assign(userShift, multiOrder1);
        userShiftReassignManager.assign(userShift, multiOrder2);

        testUserHelper.checkinAndFinishPickup(userShift);
    }
}
