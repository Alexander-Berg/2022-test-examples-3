package ru.yandex.market.tpl.core.query.usershift;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.OrderSummaryDto;
import ru.yandex.market.tpl.api.model.order.OrderType;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.DeliveryIntervalDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTasksDto;
import ru.yandex.market.tpl.api.model.task.RemainingOrderDeliveryTasksDto;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.TaskType;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnService;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommand;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommandService;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequest;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.order.SenderWithoutExtId;
import ru.yandex.market.tpl.core.domain.order.SenderWithoutExtIdRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestGenerateService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerOrderDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.service.locker.LockerDeliveryService;
import ru.yandex.market.tpl.core.service.partnerka.PartnerkaCommand;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.DEFAULT_INTERVAL;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DISPLAY_FLOW_TASKS_FINISHED_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DISPLAY_FLOW_TASKS_REMAINING_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.IS_ORDER_HISTORY_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.NEW_LOCKER_ADDRESS_MAPPING_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SET_FLOW_TASK_ORDINAL_ENABLED;
import static ru.yandex.market.tpl.core.test.TestDataFactory.PICKUP_POINT_CODE_TEST;

@RequiredArgsConstructor
public class UserShiftQueryServiceSummaryTest extends TplAbstractTest {

    public static final String LOGISTICPOINT_ID_FOR_RETURN_DROPOFF = "123456789";
    public static final List<String> CLIENT_RETURN_BARCODES =
            List.of(ClientReturn.CLIENT_RETURN_AT_ADDRESS_BARCODE_PREFIX + "123456");

    private final DropoffCargoCommandService dropoffCargoCommandService;
    private final UserShiftCommandService commandService;
    private final PickupPointRepository pickupPointRepository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final UserShiftRepository userShiftRepository;
    private final LockerDeliveryService lockerDeliveryService;
    private final UserShiftQueryService userShiftQueryService;

    private final TestDataFactory testDataFactory;
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;
    private final MovementGenerator movementGenerator;
    private final UserShiftTestHelper userShiftTestHelper;
    private final ClientReturnGenerator clientReturnGenerator;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final ClientReturnService clientReturnService;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final LockerOrderDataHelper lockerOrderDataHelper;
    private final SenderWithoutExtIdRepository senderWithoutExtIdRepository;
    private final SortingCenterService sortingCenterService;
    private final OrderManager orderManager;
    private final AddressGenerator addressGenerator;
    private final SpecialRequestGenerateService specialRequestGenerateService;

    private PickupPoint pickupPoint;
    private User user;
    private Shift shift;

    @BeforeEach
    void setUp() {
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        pickupPoint = pickupPointRepository.findByCode(PICKUP_POINT_CODE_TEST)
                .orElseGet(() -> pickupPointRepository.save(
                        testDataFactory.createPickupPoint(PartnerSubType.PVZ,
                                Long.valueOf(LOGISTICPOINT_ID_FOR_RETURN_DROPOFF), 1L)));
    }

    @ParameterizedTest
    @MethodSource("twoBooleanArguments")
    void getTaskInfo_withFinishedLocker(boolean newMappingEnabled, boolean addFlowTasksEnabled) {
        //given
        configurationServiceAdapter.insertValue(NEW_LOCKER_ADDRESS_MAPPING_ENABLED, newMappingEnabled);
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.GET_PICKUP_POINT_INFO_BY_ID_FROM_LOCKER_DELIVERY_TASK, newMappingEnabled);
        enableFlowTasks(addFlowTasksEnabled);
        var createdMixedFlowResultDto = prepareMixedCourierFlow();

        finishLockerScanProcessAndTask(createdMixedFlowResultDto);

        //when
        OrderDeliveryTasksDto tasksInfo = userShiftQueryService.getTasksInfo(user, true);

        //then
        assertThat(tasksInfo.getTasks()).hasSize(4);

        tasksInfo.getTasks()
                .stream()
                .map(OrderSummaryDto::getActions)
                .forEach(actions -> {
                    assertThat(actions).hasSize(1);
                    assertThat(actions.get(0).getType()).isEqualTo(LockerDeliveryTaskDto.ActionType.REOPEN);
                });
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void getTaskInfo_whenNotAnyFinishedTasks(boolean addFlowTasksEnabled) {
        enableFlowTasks(addFlowTasksEnabled);

        //given
        prepareMixedCourierFlow();

        //when
        OrderDeliveryTasksDto tasksInfo = userShiftQueryService.getTasksInfo(user, true);

        //then
        assertThat(tasksInfo.getTasks()).isEmpty();
    }

    @Test
    void getTaskInfo_WithFlowTasks() {
        enableFlowTasks(true);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DISPLAY_CLIENT_RETURN_FINISHED_TASKS_ENABLED,
                true);

        var order = orderGenerateService.createOrder();
        var specialRequest = specialRequestGenerateService.createSpecialRequest();
        var clientReturn = clientReturnGenerator.generateReturnFromClient();
        var userShiftId = prepareUserShift(order, specialRequest, clientReturn,
                12, 13, 14);

        //Выполняем доставку и спецзадание, остается возврат
        transactionTemplate.executeWithoutResult(
                cmd -> {
                    userHelper.finishDelivery(userShiftRepository.getById(userShiftId).getCurrentRoutePoint(),
                            false);
                }
        );
        userHelper.finishNextLockerInventory(userShiftId);

        var dtoAllTasks = userShiftQueryService.getTasksInfo(user, null);
        var dtoTasksInProgress = userShiftQueryService.getTasksInfo(user, false);
        var dtoFinishedTasks = userShiftQueryService.getTasksInfo(user, true);

        checkTaskInfo(dtoAllTasks,
                List.of(order.getExternalOrderId(), specialRequest.getExternalId(),
                        clientReturn.getExternalReturnId()));
        checkTaskInfo(dtoTasksInProgress, List.of(clientReturn.getExternalReturnId()));
        checkTaskInfo(dtoFinishedTasks, List.of(order.getExternalOrderId(), specialRequest.getExternalId()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void getTaskInfo_whenChangeOrderType(boolean addFlowTasksEnabled) {
        enableFlowTasks(addFlowTasksEnabled);

        //given
        configurationServiceAdapter.insertValue(NEW_LOCKER_ADDRESS_MAPPING_ENABLED, true);
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.GET_PICKUP_POINT_INFO_BY_ID_FROM_LOCKER_DELIVERY_TASK, true);
        configurationServiceAdapter.insertValue(IS_ORDER_HISTORY_ENABLED, true);

        var mixedCourierFlow = prepareMixedCourierFlow();
        var order = mixedCourierFlow.getOrders().get(0);
        var originalAddress = order.getDelivery().getDeliveryAddress().getAddress();

        //when
        updateOrderType(order);
        OrderDeliveryTasksDto tasksInfo = userShiftQueryService.getTasksInfo(user, true);

        //then
        assertThat(tasksInfo.getTasks()).asList().hasSize(1);
        var taskDto = tasksInfo.getTasks().get(0);
        assertThat(taskDto.getDeliveryAddress()).isEqualTo(originalAddress);
        assertThat(taskDto.getOrderType()).isEqualTo(OrderType.PVZ);
        assertThat(taskDto.getFailReason().getReason()).isEqualTo("ORDER_TYPE_UPDATED");
        assertThat(taskDto.getFailReason().getSource()).isEqualTo(Source.DELIVERY);
        assertThat(taskDto.getTaskStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        assertThat(taskDto.getRecipientFio()).isEqualTo("ПВЗ");
        assertThat(taskDto.getPersonalFioId()).isNull();
        assertThat(taskDto.getActions()).isEmpty();
        assertThat(taskDto.getOrderHistory()).isNotNull();
        assertThat(taskDto.getOrderHistory().getAddress()).isNull();
        assertThat(taskDto.getOrderHistory().getShortAddress()).isNull();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void getTaskInfo_WhenClientReturn(boolean addFlowTasksEnabled) {
        enableFlowTasks(addFlowTasksEnabled);
        var order = orderGenerateService.createOrder();
        var clientReturn = clientReturnGenerator.generateReturnFromClient();
        var orderHour = 12;
        var clienReturnHour = 13;
        var deliveryIntervalsPreOrderFinish = List.of(
                new DeliveryIntervalDto(0, order.getDelivery().getDeliveryIntervalFrom(),
                        order.getDelivery().getDeliveryIntervalTo()),
                new DeliveryIntervalDto(1, clientReturn.getArriveInterval(DateTimeUtil.DEFAULT_ZONE_ID).getStart(),
                        clientReturn.getArriveInterval(DateTimeUtil.DEFAULT_ZONE_ID).getEnd())
        );
        var deliveryIntervalsPostFinish = List.of(
                new DeliveryIntervalDto(0, clientReturn.getArriveInterval(DateTimeUtil.DEFAULT_ZONE_ID).getStart(),
                        clientReturn.getArriveInterval(DateTimeUtil.DEFAULT_ZONE_ID).getEnd())
        );
        var userShiftId = prepareClientReturn(order, clientReturn, orderHour, clienReturnHour);

        //До начала задач на доставку
        var dto = userShiftQueryService.getRemainingTasksInfo(user);
        checkRemainingSummaries(dto, List.of(order.getExternalOrderId(), clientReturn.getExternalReturnId()),
                deliveryIntervalsPreOrderFinish, 2);

        //Выполняем доставку, остается только возврат
        transactionTemplate.executeWithoutResult(
                cmd -> {
                    userHelper.finishDelivery(userShiftRepository.getById(userShiftId).getCurrentRoutePoint(),
                            false);
                }
        );

        //После задачи на доставку с 1м возвратом
        dto = userShiftQueryService.getRemainingTasksInfo(user);
        checkRemainingSummaries(dto, List.of(clientReturn.getExternalReturnId()), deliveryIntervalsPostFinish, 2);
    }

    @Test
    void getRemainingTasks_WithSpecialRequest() {
        enableFlowTasks(true);

        var order = orderGenerateService.createOrder();
        var clientReturn = clientReturnGenerator.generateReturnFromClient();
        var specialRequest = specialRequestGenerateService.createSpecialRequest();

        var orderHour = 12;
        var specialRequestHour = 13;
        var clientReturnHour = 14;

        var deliveryIntervalsPreOrderFinish = List.of(
                new DeliveryIntervalDto(0, order.getDelivery().getDeliveryIntervalFrom(),
                        order.getDelivery().getDeliveryIntervalTo()),
                new DeliveryIntervalDto(1, specialRequest.getInterval(DateTimeUtil.DEFAULT_ZONE_ID).getStart(),
                        specialRequest.getInterval(DateTimeUtil.DEFAULT_ZONE_ID).getEnd()),
                new DeliveryIntervalDto(2, clientReturn.getArriveInterval(DateTimeUtil.DEFAULT_ZONE_ID).getStart(),
                        clientReturn.getArriveInterval(DateTimeUtil.DEFAULT_ZONE_ID).getEnd())
        );
        var deliveryIntervalsPostOrderFinish = List.of(
                new DeliveryIntervalDto(0, specialRequest.getInterval(DateTimeUtil.DEFAULT_ZONE_ID).getStart(),
                        specialRequest.getInterval(DateTimeUtil.DEFAULT_ZONE_ID).getEnd()),
                new DeliveryIntervalDto(1, clientReturn.getArriveInterval(DateTimeUtil.DEFAULT_ZONE_ID).getStart(),
                        clientReturn.getArriveInterval(DateTimeUtil.DEFAULT_ZONE_ID).getEnd())
        );
        var deliveryIntervalsPostSRFinish = List.of(
                new DeliveryIntervalDto(0, clientReturn.getArriveInterval(DateTimeUtil.DEFAULT_ZONE_ID).getStart(),
                        clientReturn.getArriveInterval(DateTimeUtil.DEFAULT_ZONE_ID).getEnd())
        );
        var userShiftId = prepareUserShift(order, specialRequest, clientReturn,
                orderHour, specialRequestHour, clientReturnHour);

        //До начала задач на доставку
        var dto = userShiftQueryService.getRemainingTasksInfo(user);
        checkRemainingSummaries(dto,
                List.of(order.getExternalOrderId(), specialRequest.getExternalId(), clientReturn.getExternalReturnId()),
                deliveryIntervalsPreOrderFinish, 3);

        //Выполняем доставку, остается спецзадание и возврат
        transactionTemplate.executeWithoutResult(
                cmd -> {
                    userHelper.finishDelivery(userShiftRepository.getById(userShiftId).getCurrentRoutePoint(),
                            false);
                }
        );

        //После задачи на доставку с возвратом и спецзаданием
        dto = userShiftQueryService.getRemainingTasksInfo(user);
        checkRemainingSummaries(dto, List.of(specialRequest.getExternalId(), clientReturn.getExternalReturnId()),
                deliveryIntervalsPostOrderFinish, 3);

        // Выполняем спецзадание
        userHelper.finishNextLockerInventory(userShiftId);
        // Остается только возврат
        dto = userShiftQueryService.getRemainingTasksInfo(user);
        checkRemainingSummaries(dto, List.of(clientReturn.getExternalReturnId()), deliveryIntervalsPostSRFinish, 3);

    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void getTaskInfo_WhenClientReturnAndLoOrder(boolean addFlowTasksEnabled) {
        enableFlowTasks(addFlowTasksEnabled);
        shift = userHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        var order = lockerOrderDataHelper.getPickupOrder(
                shift, "LO-EXTERNAL_ORDER_ID", pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED, 1);
        var s = new SenderWithoutExtId(order.getSender().getYandexId());
        senderWithoutExtIdRepository.save(s);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();
        var orderHour = 12;
        var clienReturnHour = 13;
        var deliveryIntervalsPreOrderFinish = List.of(
                new DeliveryIntervalDto(0, order.getDelivery().getDeliveryIntervalFrom(),
                        order.getDelivery().getDeliveryIntervalTo()),
                new DeliveryIntervalDto(1, clientReturn.getArriveInterval(DateTimeUtil.DEFAULT_ZONE_ID).getStart(),
                        clientReturn.getArriveInterval(DateTimeUtil.DEFAULT_ZONE_ID).getEnd())
        );
        var deliveryIntervalsPostFinish = List.of(
                new DeliveryIntervalDto(0, clientReturn.getArriveInterval(DateTimeUtil.DEFAULT_ZONE_ID).getStart(),
                        clientReturn.getArriveInterval(DateTimeUtil.DEFAULT_ZONE_ID).getEnd())
        );
        var userShiftId = prepareClientReturn(order, clientReturn, orderHour, clienReturnHour);

        //До начала задач на доставку
        var dto = userShiftQueryService.getRemainingTasksInfo(user);

        assertThat(dto.getOrders()).hasSize(2);
        assertThat(dto.getSummary().getTotalOrderCount()).isEqualTo(2);
        assertThat(dto.getSummary().getUnfinishedOrderCount()).isEqualTo(2);
        assertThat(dto.getOrders().stream().map(OrderSummaryDto::getType)).containsExactlyInAnyOrder(TaskType.ORDER_DELIVERY, TaskType.CLIENT_RETURN);
        assertThat(dto.getDeliveryIntervals()).containsExactlyInAnyOrderElementsOf(deliveryIntervalsPreOrderFinish);

        //Выполняем доставку, остается только возврат
        transactionTemplate.executeWithoutResult(
                cmd -> {
                    userHelper.finishDelivery(userShiftRepository.getById(userShiftId).getCurrentRoutePoint(),
                            false);
                }
        );

        //После задачи на доставку с 1м возвратом
        dto = userShiftQueryService.getRemainingTasksInfo(user);

        assertThat(dto.getSummary().getUnfinishedOrderCount()).isEqualTo(1);
        assertThat(dto.getSummary().getTotalOrderCount()).isEqualTo(2);
        assertThat(dto.getOrders()).hasSize(1);
        assertThat(dto.getOrders().stream().map(OrderSummaryDto::getExternalOrderId)).containsExactlyInAnyOrder(clientReturn.getExternalReturnId());
        assertThat(dto.getDeliveryIntervals()).containsExactlyInAnyOrderElementsOf(deliveryIntervalsPostFinish);
    }

    @ParameterizedTest
    @DisplayName("Проверка, что порядок заказов и возвратов по ординальным номерам соответствует порядку времени " +
            "доставки")
    @ValueSource(booleans = {true, false})
    void ordinalNumbersCorrespondToDeliveryTimeOrder(boolean addFlowTasksEnabled) {
        enableFlowTasks(addFlowTasksEnabled);
        var order = orderGenerateService.createOrder();
        var clientReturn = clientReturnGenerator.generateReturnFromClient();
        var orderHour = 12;
        var clienReturnHour = 13;
        var usId = prepareClientReturn(order, clientReturn, orderHour, clienReturnHour);

        var dto = userShiftQueryService.getRemainingTasksInfo(user);

        var orders = dto.getOrders();

        var taskIdToOrdinalNumber = StreamEx.of(orders).toMap(OrderSummaryDto::getTaskId,
                OrderSummaryDto::getOrdinalNumber);

        transactionTemplate.executeWithoutResult(
                cmd -> {
                    var us = userShiftRepository.findByIdOrThrow(usId);
                    var deliveryTaskToDeliveryTime =
                            StreamEx.of(us.streamOrderDeliveryTasks()).sortedBy(OrderDeliveryTask::getExpectedDeliveryTime).map(OrderDeliveryTask::getId).toList();

                    assertThat(deliveryTaskToDeliveryTime).hasSize(2);
                    assertThat(taskIdToOrdinalNumber).hasSize(2);
                    assertThat(taskIdToOrdinalNumber.get(deliveryTaskToDeliveryTime.get(0))).isEqualTo(1);
                    assertThat(taskIdToOrdinalNumber.get(deliveryTaskToDeliveryTime.get(1))).isEqualTo(2);
                }
        );
    }

    @Test
    void getFinishedClientReturnTaskInfo() {
        Mockito.when(
                configurationProviderAdapter.isBooleanEnabled(
                        ConfigurationProperties.DISPLAY_CLIENT_RETURN_FINISHED_TASKS_ENABLED
                )
        ).thenReturn(true);
        var order = orderGenerateService.createOrder();
        var clientReturn = clientReturnGenerator.generateReturnFromClient();
        var userShiftId = prepareClientReturn(order, clientReturn, 12, 13);

        //Выполняем доставку, остается только возврат
        transactionTemplate.executeWithoutResult(
                cmd -> {
                    userHelper.finishDelivery(userShiftRepository.getById(userShiftId).getCurrentRoutePoint(),
                            false);
                }
        );

        //После выполнения задачи на доставку заказа
        var dto = userShiftQueryService.getTasksInfo(user, true);

        assertThat(dto.getTasks()).hasSize(1);

        transactionTemplate.executeWithoutResult(
                cmd -> clientReturnService.assignBarcodeAndFinishTask(CLIENT_RETURN_BARCODES,
                        Map.of(), clientReturn.getExternalReturnId(), user,
                        userShiftRepository.getById(userShiftId).getCurrentRoutePoint().streamDeliveryTasks().findFirst().get().getId())
        );

        //После задачи на забор возврата
        dto = userShiftQueryService.getTasksInfo(user, true);
        assertThat(dto.getTasks()).hasSize(2);
        assertThat(dto.getTasks().stream().map(OrderSummaryDto::getExternalOrderId).collect(Collectors.toList())).contains(clientReturn.getExternalReturnId());

    }

    private long prepareUserShift(Order order, LogisticRequest logisticRequest, ClientReturn clientReturn,
                                  int orderHour, int logisticRequestHour, int clientReturnHour) {

        var createCommandBuilder = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId());

        if (order != null) {
            createCommandBuilder.routePoint(helper.taskPrepaid("addr1", orderHour, order.getId()));
        }
        if (clientReturn != null) {
            createCommandBuilder.routePoint(helper.clientReturn("addr4", clientReturnHour, clientReturn.getId()));
        }

        var createCommand = createCommandBuilder.build();
        var userShift = userShiftTestHelper.createUserShift(createCommand);

        if (logisticRequest != null) {
            commandService.addFlowTask(user, new UserShiftCommand.AddFlowTask(userShift.getId(),
                    logisticRequest.resolveTaskFlow(),
                    helper.logisticRequest(logisticRequestHour, logisticRequest)));
        }

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));

        userHelper.finishPickupAtStartOfTheDay(userShift.getId(), true);
        return userShift.getId();
    }

    private long prepareClientReturn(Order order, ClientReturn clientReturn, int orderHour, int clienttReturnHour) {
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskPrepaid("addr1", orderHour, order.getId()))
                .routePoint(helper.clientReturn("addr4", clienttReturnHour, clientReturn.getId()))
                .build();
        var userShiftId = userShiftTestHelper.start(createCommand);
        userHelper.finishPickupAtStartOfTheDay(userShiftId, true);
        return userShiftId;
    }

    private void finishLockerScanProcessAndTask(CreatedFlowResultDto createdMixedFlowResultDto) {
        transactionTemplate.execute(st -> {

            var userShift = userShiftRepository.findByIdOrThrow(createdMixedFlowResultDto.getUserShiftId());
            userHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
            LockerDeliveryTask ldt = userShift.streamLockerDeliveryTasks().findFirst().orElseThrow();

            Set<Long> dropoffCargosSuccess = createdMixedFlowResultDto.getDropoffCargos()
                    .stream()
                    .map(DropoffCargo::getId)
                    .collect(Collectors.toSet());

            List<Long> ordersSuccess = createdMixedFlowResultDto.getOrders()
                    .stream()
                    .map(Order::getId)
                    .collect(Collectors.toList());

            commandService.finishLoadingLocker(user,
                    new UserShiftCommand.FinishLoadingLocker(
                            createdMixedFlowResultDto.getUserShiftId(),
                            ldt.getRoutePoint().getId(),
                            ldt.getId(),
                            null,
                            false,
                            ScanRequest.builder()
                                    .successfullyScannedDropoffCargos(dropoffCargosSuccess)
                                    .successfullyScannedOrders(ordersSuccess)
                                    .build()
                    )
            );

            lockerDeliveryService.finishTask(ldt.getId(), null, user);
            return null;

        });
    }

    private CreatedFlowResultDto prepareMixedCourierFlow() {

        Order firstOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .pickupPoint(pickupPoint)
                        .build()
        );
        Order secondOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .pickupPoint(pickupPoint)
                        .build()
        );
        var movementDropoffReturn = testDataFactory.buildDropOffReturnMovement(LOGISTICPOINT_ID_FOR_RETURN_DROPOFF);
        var movementDropoShip = movementGenerator.generate(MovementCommand.Create.builder().build());


        List<String> barcodes = List.of("barcode1", "barcode2");
        var dropoffCargos = barcodes
                .stream().map(this::addDropoffCargo)
                .collect(Collectors.toList());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .routePoint(helper.taskDropOffReturn(movementDropoffReturn.getId(), pickupPoint.getId()))
                .routePoint(helper.taskLockerDelivery(firstOrder, pickupPoint.getId(), 10))
                .routePoint(helper.taskLockerDelivery(secondOrder, pickupPoint.getId(), 10))
                .routePoint(helper.taskCollectDropship(LocalDate.now(), movementDropoShip))
                .build();

        Long userShiftId = prepareCourierFlowUserShift(createCommand, List.of(firstOrder.getId(), secondOrder.getId()),
                dropoffCargos);


        return CreatedFlowResultDto.builder()
                .dropOffMovements(List.of(movementDropoffReturn))
                .dropShipMovements(List.of(movementDropoShip))
                .dropoffCargos(dropoffCargos)
                .orders(List.of(firstOrder, secondOrder))
                .userShiftId(userShiftId)
                .build();
    }

    private Long prepareCourierFlowUserShift(UserShiftCommand.Create createCommand,
                                             List<Long> successfullyScannedOrders, List<DropoffCargo> dropoffCargos) {

        Long userShiftId = transactionTemplate.execute(status -> commandService.createUserShift(createCommand));

        OrderPickupTask orderPickupTask = transactionTemplate.execute(status -> {
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);

            userHelper.openShift(user, userShift.getId());
            userHelper.arriveAtRoutePoint(userShift.getCurrentRoutePoint());
            var pickupTask = userShift.streamPickupRoutePoints()
                    .findFirst().orElseThrow().streamPickupTasks().findFirst().orElseThrow();
            commandService.startOrderPickup(user, new UserShiftCommand.StartScan(
                    userShift.getId(), userShift.getCurrentRoutePoint().getId(), pickupTask.getId()
            ));
            return pickupTask;
        });

        commandService.pickupOrders(user,
                new UserShiftCommand.FinishScan(userShiftId,
                        orderPickupTask.getRoutePoint().getId(),
                        orderPickupTask.getId(),
                        ScanRequest.builder()
                                .successfullyScannedDropoffCargos(Set.of(dropoffCargos.get(0).getId(),
                                        dropoffCargos.get(1).getId()))
                                .successfullyScannedOrders(successfullyScannedOrders)
                                .skippedDropoffCargos(Set.of())
                                .build()
                )
        );

        commandService.finishLoading(
                user,
                new UserShiftCommand.FinishLoading(
                        userShiftId,
                        orderPickupTask.getRoutePoint().getId(),
                        orderPickupTask.getId()
                )
        );
        return userShiftId;
    }

    private DropoffCargo addDropoffCargo(String barcode) {
        return dropoffCargoCommandService.createOrGet(
                DropoffCargoCommand.Create.builder()
                        .barcode(barcode)
                        .logisticPointIdFrom("fakeIdFrom")
                        .logisticPointIdTo(LOGISTICPOINT_ID_FOR_RETURN_DROPOFF)
                        .build());
    }

    private void updateOrderType(Order order) {
        transactionTemplate.execute(ts -> {
            orderManager.updateDsOrderData(
                    PartnerkaCommand.UpdateDsOrderData.builder()
                            .orderType(OrderType.CLIENT)
                            .orderPaymentType(OrderPaymentType.PREPAID)
                            .orderPaymentStatus(OrderPaymentStatus.PAID)
                            .deliveryDate(order.getDelivery().getDeliveryDate(DateTimeUtil.DEFAULT_ZONE_ID).plusDays(1))
                            .intervalFrom(DEFAULT_INTERVAL.getStart())
                            .intervalTo(DEFAULT_INTERVAL.getEnd())
                            .existingOrder(order)
                            .newAddress(addressGenerator.generate(AddressGenerator.AddressGenerateParam.builder().build()))
                            .oldAddress(order.getDelivery().getDeliveryAddress())
                            .build()
            );
            return null;
        });
    }

    private void checkRemainingSummaries(RemainingOrderDeliveryTasksDto dto, List<String> externalIds,
                                         List<DeliveryIntervalDto> intervals, int totalOrdersCount) {
        assertThat(dto.getSummary().getUnfinishedOrderCount()).isEqualTo(externalIds.size());
        assertThat(dto.getSummary().getTotalOrderCount()).isEqualTo(totalOrdersCount);
        assertThat(dto.getOrders()).hasSize(externalIds.size());
        assertThat(dto.getOrders().stream().map(OrderSummaryDto::getExternalOrderId))
                .containsExactlyInAnyOrderElementsOf(externalIds);
        assertThat(dto.getDeliveryIntervals()).containsExactlyInAnyOrderElementsOf(intervals);
    }

    private void checkTaskInfo(OrderDeliveryTasksDto dto, List<String> externalIds) {
        assertThat(dto.getTasks()).hasSize(externalIds.size());
        assertThat(dto.getTasks().stream().map(OrderSummaryDto::getExternalOrderId))
                .containsExactlyInAnyOrderElementsOf(externalIds);
    }

    public static Stream<Arguments> twoBooleanArguments() {
        return Stream.of(
                Arguments.of(false, false),
                Arguments.of(true, false),
                Arguments.of(false, true),
                Arguments.of(true, true)
        );
    }

    private void enableFlowTasks(boolean enable) {
        configurationServiceAdapter.mergeValue(SET_FLOW_TASK_ORDINAL_ENABLED, enable);
        configurationServiceAdapter.mergeValue(DISPLAY_FLOW_TASKS_REMAINING_ENABLED, enable);
        configurationServiceAdapter.mergeValue(DISPLAY_FLOW_TASKS_FINISHED_ENABLED, enable);
    }

    @Value
    @Builder
    public static class CreatedFlowResultDto {
        @Builder.Default
        List<DropoffCargo> dropoffCargos = List.of();
        @Builder.Default
        List<Movement> dropOffMovements = List.of();
        @Builder.Default
        List<Movement> dropShipMovements = List.of();
        @Builder.Default
        List<Order> orders = List.of();
        Long userShiftId;
    }
}
