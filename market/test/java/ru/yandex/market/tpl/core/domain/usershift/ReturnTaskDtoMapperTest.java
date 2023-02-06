package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.locker.boxbot.request.MarketExtraditionSuccessNotifyDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskDto;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.lrm.client.api.ReturnsApi;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.barcode_prefix.ReturnBarcodePrefixRepository;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnCommandService;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnService;
import ru.yandex.market.tpl.core.domain.clientreturn.CreatedSource;
import ru.yandex.market.tpl.core.domain.clientreturn.commands.ClientReturnCommand;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.query.order.mapper.OrderDeliveryMapper;
import ru.yandex.market.tpl.core.query.usershift.mapper.task_mapper.ReturnTaskDtoMapper;
import ru.yandex.market.tpl.core.service.locker.LockerDeliveryService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.util.TplTaskUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static ru.yandex.market.tpl.api.model.locker.boxbot.request.ReturnType.EXTRADITION;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.CLIENT_RETURN_TASK_MAPPING_ENABLED;


@RequiredArgsConstructor
public class ReturnTaskDtoMapperTest extends TplAbstractTest {
    private final static List<String> BARCODES = List.of(ClientReturn.CLIENT_RETURN_AT_ADDRESS_BARCODE_PREFIX +
            "123456");
    private final static LocalDate CURRENT_DATE = LocalDate.of(2022, 2, 22);
    private final static LocalDateTime CURRENT_DATETIME = LocalDateTime.of(CURRENT_DATE, LocalTime.of(12, 30));
    private final static int CLIENT_RETURN_HOUR = 12;
    private final static LocalTimeInterval LOCKER_DELIVERY_TIME_INTERVAL = LocalTimeInterval.valueOf("00:00-23:59");
    private final static Long USER_ID = 2L;
    private final TestUserHelper testUserHelper;
    private final Clock clock;
    private final TestDataFactory testDataFactory;
    private final OrderGenerateService orderGenerateService;
    private final ClientReturnCommandService clientReturnCommandService;
    private final UserShiftCommandService commandService;
    private final ReturnTaskDtoMapper returnTaskDtoMapper;
    private final UserShiftRepository userShiftRepository;
    private final TransactionTemplate transactionTemplate;
    private final ReturnBarcodePrefixRepository barcodePrefixRepository;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final ReturnsApi returnsApi;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final OrderDeliveryMapper orderDeliveryMapper;
    private final ClientReturnGenerator clientReturnGenerator;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final ClientReturnService clientReturnService;
    private final ClientReturnRepository clientReturnRepository;
    private final MovementGenerator movementGenerator;
    private final LockerDeliveryService lockerDeliveryService;

    private final UserShiftTestHelper userShiftTestHelper;
    private User user;
    private ClientReturn clientReturn;
    private Order order;
    private Shift shift;
    private PickupPoint pickupPoint;
    private Movement movement;
    private String clientReturnBarcodeExternalCreated;


    @BeforeEach
    void init() {
        clientReturnBarcodeExternalCreated = barcodePrefixRepository.findBarcodePrefixByName(
                "CLIENT_RETURN_BARCODE_PREFIX_SF").getBarcodePrefix() + "3";
        ClockUtil.initFixed(clock, CURRENT_DATETIME);
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        user = testUserHelper.findOrCreateUser(USER_ID);
        shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 3453L, DeliveryService.DEFAULT_DS_ID);
        clientReturn = clientReturnGenerator.generateReturnFromClient();
        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LOCKER_DELIVERY_TIME_INTERVAL)
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .pickupPoint(pickupPoint)
                .build());
        clientReturnCommandService.create(ClientReturnCommand.Create.builder()
                .barcode(clientReturnBarcodeExternalCreated)
                .returnId(clientReturnBarcodeExternalCreated)
                .pickupPoint(pickupPoint)
                .createdSource(CreatedSource.EXTERNAL)
                .source(Source.SYSTEM)
                .build());
        movement = movementGenerator.generate(MovementCommand.Create.builder().build());
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(CLIENT_RETURN_TASK_MAPPING_ENABLED)).thenReturn(true);
        Mockito.when(returnsApi.updateReturnWithHttpInfo(any(), any())).thenReturn(
                ResponseEntity.ok().build()
        );
        Mockito.when(returnsApi.commitReturnWithHttpInfo(anyLong())).thenReturn(
                ResponseEntity.ok().build()
        );
    }

    @Test
    void lockerClientReturnAndDropshipAndCourrierClientReturnTasks() {
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskLockerDelivery(order.getId(), pickupPoint.getId()))
                .routePoint(userShiftCommandDataHelper.taskCollectDropship(CURRENT_DATE, movement))
                .routePoint(userShiftCommandDataHelper.clientReturn("addr4", CLIENT_RETURN_HOUR, clientReturn.getId()))
                .build();
        var userShiftId = userShiftTestHelper.start(createCommand);
        testUserHelper.finishPickupAtStartOfTheDay(userShiftId, true);

        transactionTemplate.execute(st -> {
            doAllTasks(userShiftId);

            var usershift = userShiftRepository.findByIdOrThrow(userShiftId);

            var returnTask = usershift.streamReturnRoutePoints()
                    .findFirst().orElseThrow().streamReturnTasks().findFirst().orElseThrow();
            var orderReturnTaskDto =
                    (OrderReturnTaskDto) returnTaskDtoMapper.mapToTaskDto(returnTask,
                            TplTaskUtils.EMPTY_MAPPER_CONTEXT);
            assertThat(orderReturnTaskDto.isReturnDropships()).isFalse();
            assertThat(orderReturnTaskDto.getOrders()).hasSize(2);
            return null;
        });
    }

    @Test
    void lockerClientReturnAndDropshipTask() {
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskLockerDelivery(order.getId(), pickupPoint.getId()))
                .routePoint(userShiftCommandDataHelper.taskCollectDropship(CURRENT_DATE, movement))
                .build();
        var userShiftId = userShiftTestHelper.start(createCommand);
        testUserHelper.finishPickupAtStartOfTheDay(userShiftId, true);

        transactionTemplate.execute(st -> {
            doAllTasks(userShiftId);

            var usershift = userShiftRepository.findByIdOrThrow(userShiftId);
            var returnTask = usershift.streamReturnRoutePoints()
                    .findFirst().orElseThrow().streamReturnTasks().findFirst().orElseThrow();
            var orderReturnTaskDto =
                    (OrderReturnTaskDto) returnTaskDtoMapper.mapToTaskDto(returnTask,
                            TplTaskUtils.EMPTY_MAPPER_CONTEXT);
            assertThat(orderReturnTaskDto.isReturnDropships()).isFalse();
            assertThat(orderReturnTaskDto.getOrders()).hasSize(1);
            return null;
        });
    }

    @Test
    void lockerClientReturnTask() {
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskLockerDelivery(order.getId(), pickupPoint.getId()))
                .build();

        var userShiftId = userShiftTestHelper.start(createCommand);
        testUserHelper.finishPickupAtStartOfTheDay(userShiftId, true);

        OrderReturnTaskDto orderReturnTaskDto = transactionTemplate.execute(st -> {
            doAllTasks(userShiftId);
            OrderReturnTask returnTask =
                    userShiftRepository.findByIdOrThrow(userShiftId).streamReturnRoutePoints()
                            .findFirst().orElseThrow().streamReturnTasks().findFirst().orElseThrow();
            return (OrderReturnTaskDto) returnTaskDtoMapper.mapToTaskDto(returnTask, TplTaskUtils.EMPTY_MAPPER_CONTEXT);
        });

        assertThat(orderReturnTaskDto.isReturnDropships()).isFalse();
    }

    @Test
    void lockerClientReturnTask_with_Extradition() {
        //given
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskLockerDelivery(order.getId(), pickupPoint.getId()))
                .build();

        var userShiftId = userShiftTestHelper.start(createCommand);
        testUserHelper.finishPickupAtStartOfTheDay(userShiftId, true);


        var lockerDeliveryTask = transactionTemplate.execute(st ->
                userShiftRepository.getById(userShiftId).streamLockerDeliveryTasks().findFirst()
                        .orElseThrow()
        );

        testUserHelper.arriveAtRoutePoint(lockerDeliveryTask.getRoutePoint());

        lockerDeliveryService.extraditionSuccess("", MarketExtraditionSuccessNotifyDto.builder()
                .taskId(lockerDeliveryTask.getId())
                .returnType(EXTRADITION)
                .externalOrderId(order.getExternalOrderId())
                .barcode("")
                .build(), user);


        //when
        OrderReturnTaskDto orderReturnTaskDto = transactionTemplate.execute(st -> {
            OrderReturnTask returnTask = userShiftRepository.findByIdOrThrow(userShiftId).streamReturnRoutePoints()
                            .findFirst().orElseThrow().streamReturnTasks().findFirst().orElseThrow();
            return (OrderReturnTaskDto) returnTaskDtoMapper.mapToTaskDto(returnTask, TplTaskUtils.EMPTY_MAPPER_CONTEXT);
        });

        //then
        assertThat(orderReturnTaskDto.getOrders()).hasSize(1);
    }

    @Test
    void dropshipTask() {
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskCollectDropship(CURRENT_DATE, movement))
                .build();

        var userShiftId = userShiftTestHelper.start(createCommand);

        OrderReturnTaskDto orderReturnTaskDto = transactionTemplate.execute(st -> {
            doAllTasks(userShiftId);
            OrderReturnTask returnTask =
                    userShiftRepository.findByIdOrThrow(userShiftId).streamReturnRoutePoints()
                            .findFirst().orElseThrow().streamReturnTasks().findFirst().orElseThrow();
            return (OrderReturnTaskDto) returnTaskDtoMapper.mapToTaskDto(returnTask, TplTaskUtils.EMPTY_MAPPER_CONTEXT);
        });

        assertThat(orderReturnTaskDto.isReturnDropships()).isTrue();
    }

    @Test
    void courrierClientReturnTask() {
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.clientReturn("addr4", CLIENT_RETURN_HOUR, clientReturn.getId()))
                .build();

        var userShiftId = userShiftTestHelper.start(createCommand);
        testUserHelper.finishPickupAtStartOfTheDay(userShiftId, true);

        transactionTemplate.execute(st -> {
            doAllTasks(userShiftId);

            var usershift = userShiftRepository.findByIdOrThrow(userShiftId);
            var returnTask = usershift.streamReturnRoutePoints()
                    .findFirst().orElseThrow().streamReturnTasks().findFirst().orElseThrow();
            var orderReturnTaskDto =
                    (OrderReturnTaskDto) returnTaskDtoMapper.mapToTaskDto(returnTask,
                            TplTaskUtils.EMPTY_MAPPER_CONTEXT);
            assertThat(orderReturnTaskDto.isReturnDropships()).isFalse();
            assertThat(orderReturnTaskDto.getOrders()).hasSize(1);
            var clientReturnTaskDto = orderReturnTaskDto.getOrders().get(0);
            var expectedDelivery = orderDeliveryMapper.mapDelivery(clientReturn);
            assertThat(clientReturnTaskDto.getDelivery()).isEqualTo(expectedDelivery);
            assertThat(clientReturnTaskDto.getExternalOrderId()).isEqualTo(BARCODES.get(0));
            return null;
        });
    }

    private void doLockerDeliveryTask(LockerDeliveryTask deliveryTask, UserShift userShift) {
        User user = userShift.getUser();
        testUserHelper.arriveAtRoutePoint(deliveryTask.getRoutePoint());
        commandService.finishLoadingLocker(user, new UserShiftCommand.FinishLoadingLocker(userShift.getId(),
                deliveryTask.getRoutePoint().getId(), deliveryTask.getId(), null, ScanRequest.builder()
                .successfullyScannedOrders(new ArrayList<>(deliveryTask.getOrderIds()))
                .build()));

        commandService.finishUnloadingLocker(user, new UserShiftCommand.FinishUnloadingLocker(
                userShift.getId(), deliveryTask.getRoutePoint().getId(), deliveryTask.getId(),
                Set.of(new UnloadedOrder(clientReturnBarcodeExternalCreated,
                        null, null))
        ));
    }


    private void doClientReturnTask(OrderDeliveryTask task, UserShift userShift, ClientReturn clientReturn) {
        var user = userShift.getUser();
        testUserHelper.arriveAtRoutePoint(task.getRoutePoint());
        clientReturnService.assignBarcodeAndFinishTask(BARCODES, Map.of(), clientReturn.getExternalReturnId(), user,
                task.getId());
        dbQueueTestUtil.assertQueueHasSize(QueueType.CLIENT_RETURN_ATTACH_BARCODE, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.CLIENT_RETURN_ATTACH_BARCODE);
    }

    private void doAllTasks(long userShiftId) {
        var userShift = userShiftRepository.getById(userShiftId);
        var currentTasks =
                userShift.streamRoutePoints()
                        .filter(rp -> rp.getStatus().equals(RoutePointStatus.IN_TRANSIT))
                        .filter(rp -> rp.streamReturnTasks().findAny().isEmpty())
                        .flatMap(RoutePoint::streamTasks)
                        .collect(Collectors.toList());
        while (!currentTasks.isEmpty()) {
            StreamEx.of(currentTasks)
                    .select(LockerDeliveryTask.class)
                    .forEach(
                            task -> doLockerDeliveryTask(task, userShift)
                    );
            StreamEx.of(currentTasks)
                    .select(OrderDeliveryTask.class)
                    .forEach(
                            task -> {
                                assert task.getClientReturnId() != null;
                                var cr = clientReturnRepository.getById(task.getClientReturnId());
                                doClientReturnTask(task, userShift, cr);
                            }
                    );
            StreamEx.of(currentTasks)
                    .select(CollectDropshipTask.class)
                    .forEach(
                            task -> testUserHelper.doCollectDropshipTask(task, userShift)
                    );

            currentTasks =
                    userShift.streamRoutePoints()
                            .filter(rp -> rp.getStatus().equals(RoutePointStatus.IN_TRANSIT))
                            .filter(rp -> rp.streamReturnTasks().findAny().isEmpty())
                            .flatMap(RoutePoint::streamTasks)
                            .collect(Collectors.toList());
        }

    }
}
