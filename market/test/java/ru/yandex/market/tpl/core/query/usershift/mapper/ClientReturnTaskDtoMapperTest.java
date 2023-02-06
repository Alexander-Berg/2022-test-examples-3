package ru.yandex.market.tpl.core.query.usershift.mapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.OrderTagsDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.TaskDto;
import ru.yandex.market.tpl.api.model.task.TaskType;
import ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseRepository;
import ru.yandex.market.tpl.core.domain.order.warehouse.OrderWarehouseSchedule;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePointRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.ORDER_ITEMS_QUANTITY_MISMATCH;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus.PREPARED_FOR_CANCEL;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DO_NOT_CALL_ENABLED;

@RequiredArgsConstructor
public class ClientReturnTaskDtoMapperTest extends TplAbstractTest {

    private final TaskDtoMapper taskDtoMapper;
    private final TestUserHelper testUserHelper;
    private final UserShiftCommandService commandService;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandDataHelper helper;
    private final MovementGenerator movementGenerator;
    private final AddressGenerator addressGenerator;
    private final Clock clock;
    private final OrderWarehouseRepository orderWarehouseRepository;
    private final TransactionTemplate transactionTemplate;
    private final ClientReturnGenerator clientReturnGenerator;
    private final RoutePointRepository routePointRepository;
    private final TestDataFactory testDataFactory;
    private final JdbcTemplate jdbcTemplate;

    private UserShift userShift;
    private OrderDeliveryTask deliveryTask1;
    private OrderDeliveryTask clientReturnTask1;
    private ClientReturn clientReturn1;
    private ClientReturn clientReturn2;
    private OrderPickupTask pickupTask;
    private Movement movement;
    private Order order1;
    private Order order2;
    private Order multiPlaceOrder;
    private User user;
    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;
    private final UserShiftCommandService userShiftCommandService;
    private final ClientReturnRepository clientReturnRepository;

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

        multiPlaceOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .paymentType(OrderPaymentType.CARD)
                .places(List.of(
                        OrderPlaceDto.builder().barcode(new OrderPlaceBarcode("123", "place1")).build(),
                        OrderPlaceDto.builder().barcode(new OrderPlaceBarcode("123", "place2")).build()
                ))
                .build());

        clientReturn1 = clientReturnGenerator.generateReturnFromClient();
        clientReturn2 = clientReturnGenerator.generateReturnFromClient();

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

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskUnpaid("addr1", 12, order1.getId()))
                .routePoint(helper.taskUnpaid("addr1", 12, order2.getId()))
                .routePoint(helper.taskUnpaid("addr3", 14, multiPlaceOrder.getId()))
                .routePoint(helper.clientReturn("addr4", 13, clientReturn1.getId()))
                .routePoint(helper.taskCollectDropship(LocalDate.now(clock), movement))
                .build();

        long userShiftId = commandService.createUserShift(createCommand);

        userShift = transactionTemplate.execute(
                cmd -> {
                    var temp = userShiftRepository.findByIdOrThrow(userShiftId);
                    Hibernate.initialize(temp.streamRoutePoints().collect(Collectors.toList()));
                    return temp;
                }
        );

        deliveryTask1 =
                userShift.streamOrderDeliveryTasks()
                        .filter(t -> Objects.equals(t.getOrderId(), order1.getId()))
                        .findFirst()
                        .orElseThrow();
        clientReturnTask1 =
                userShift.streamOrderDeliveryTasks()
                        .filter(t -> Objects.equals(t.getClientReturnId(), clientReturn1.getId()))
                        .findFirst()
                        .orElseThrow();

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

        commandService.pickupOrders(user, new UserShiftCommand.FinishScan(
                userShiftId, pickupRoutePoint.getId(), pickupTask.getId(),
                ScanRequest.builder()
                        .successfullyScannedOrders(List.of(order1.getId(), order2.getId(), multiPlaceOrder.getId()))
                        .comment(comment)
                        .finishedAt(clock.instant())
                        .build()
        ));

        commandService.finishLoading(user, new UserShiftCommand.FinishLoading(
                userShiftId, pickupRoutePoint.getId(), pickupTask.getId())
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @Transactional
    @DisplayName("Проверяем, что при клиентских возвратах и доставках, получаем дто задач, соответсвующие флагу и " +
            "задачам.")
    void clientReturnAndDeliveryOrderMap(boolean isFlagEnabled) {
        when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.CLIENT_RETURN_TASK_MAPPING_ENABLED))
                .thenReturn(isFlagEnabled);
        var routePoint = deliveryTask1.getRoutePoint();
        userShiftCommandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePoint.getId(), clientReturn2.getId(), routePoint.getExpectedDateTime()
                ));

        var updatedRoutePoint = routePointRepository.getById(routePoint.getId());

        var tasksDto = taskDtoMapper.mapTasks(
                updatedRoutePoint,
                List.of(order1),
                List.of(clientReturn2),
                true,
                user
        );

        var clientReturnCount = getClientReturnCount(tasksDto);
        var orderDeliveryCount = getDeliveryOrderTask(tasksDto);

        if (isFlagEnabled) {
            assertThat(tasksDto).hasSize(2);
            assertThat(clientReturnCount).isEqualTo(1L);
        } else {
            assertThat(tasksDto).hasSize(1);
            assertThat(clientReturnCount).isEqualTo(0);
        }

        assertThat(orderDeliveryCount).isEqualTo(1L);
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @Transactional
    @DisplayName("Проверяем, что при только клиентских возвратах получаем только клиентские возвраты в соответствии с" +
            " флагом.")
    void clientReturnOnlyMap(boolean isFlagEnabled) {
        when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.CLIENT_RETURN_TASK_MAPPING_ENABLED))
                .thenReturn(isFlagEnabled);
        var routePoint = clientReturnTask1.getRoutePoint();

        var tasksDto = taskDtoMapper.mapTasks(
                routePoint,
                List.of(),
                List.of(clientReturn1),
                true,
                user
        );

        var clientReturnCount = getClientReturnCount(tasksDto);
        var deliveryTaskCount = getDeliveryOrderTask(tasksDto);

        if (isFlagEnabled) {
            assertThat(tasksDto).hasSize(1);
            assertThat(clientReturnCount).isEqualTo(1L);
            assertThat(deliveryTaskCount).isEqualTo(0);
        } else {
            assertThat(tasksDto).hasSize(0);
            assertThat(clientReturnCount).isEqualTo(0);
            assertThat(deliveryTaskCount).isEqualTo(0);
        }
    }

    @Test
    @Transactional
    void clientReturnReopenMap() {
        when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.CLIENT_RETURN_TASK_MAPPING_ENABLED))
                .thenReturn(true);
        var routePoint = testDataFactory.createEmptyRoutePoint(user, userShift.getId());
        long routePointId = routePoint.getId();
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();

        var tod = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );

        var callTask = commandService.createCallToRecipientTask(UserShiftCommand.CreateCallToRecipientTask.builder()
                        .userShiftId(userShift.getId())
                        .orderDeliveryTaskId(tod.getId())
                        .expectedCallTime(deliveryTime)
                        .build(),
                userShift,
                tod,
                CallToRecipientTaskStatus.NOT_CALLED,
                clock
        );

        testUserHelper.openShift(user, userShift.getId());
        jdbcTemplate.execute("update route_point " +
                "set status = 'IN_PROGRESS' " +
                "where id =" + routePointId);

        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(userShift.getId(),
                routePointId, tod.getId(),
                new OrderDeliveryFailReason(ORDER_ITEMS_QUANTITY_MISMATCH, "")));

        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(PREPARED_FOR_CANCEL);

        var tasksDto = taskDtoMapper.mapTasks(
                routePoint,
                List.of(),
                List.of(clientReturn),
                true,
                user
        );
        assertThat(tasksDto).isNotEmpty();
        assertThat(((OrderDeliveryTaskDto) tasksDto.get(0)).getActions()).isNotEmpty();
        assertThat(((OrderDeliveryTaskDto) tasksDto.get(0)).getActions())
                .contains(new OrderDeliveryTaskDto.Action(OrderDeliveryTaskDto.ActionType.REOPEN));
        assertThat(((OrderDeliveryTaskDto) tasksDto.get(0)).getTags()).isNotEmpty();
        assertThat(((OrderDeliveryTaskDto) tasksDto.get(0)).getTags()).contains(OrderTagsDto.CLIENT_RETURN);
        assertThat(((OrderDeliveryTaskDto) tasksDto.get(0)).getMultiOrderId()).isEqualTo(tod.getParentId());

    }

    private long getClientReturnCount(List<TaskDto> tasks) {
        return tasks.stream().filter(it -> it.getType() == TaskType.CLIENT_RETURN).count();
    }

    private long getDeliveryOrderTask(List<TaskDto> tasks) {
        return tasks.stream().filter(it -> it.getType() == TaskType.ORDER_DELIVERY).count();
    }
}
