package ru.yandex.market.tpl.core.query.usershift.mapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.OrderType;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.address.DeliveryAddress;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.query.usershift.mapper.task_mapper.OrderPickupTaskDtoMapper;
import ru.yandex.market.tpl.core.query.usershift.mapper.task_mapper.ReturnTaskDtoMapper;
import ru.yandex.market.tpl.core.service.partnerka.PartnerkaCommand;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.util.TplTaskUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.tpl.api.model.order.OrderType.CLIENT;
import static ru.yandex.market.tpl.api.model.order.OrderType.LOCKER;
import static ru.yandex.market.tpl.core.query.usershift.mapper.task_mapper.OrderPickupTaskStatusDetailsMapper.ACCEPT_BUT_NOT_DELIVER_RESCHEDULED_TEXT;

@RequiredArgsConstructor
public class UpdatedOrderTypeMappingTest extends TplAbstractTest {

    private final TestUserHelper testUserHelper;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final UserShiftTestHelper userShiftTestHelper;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final TestDataFactory testDataFactory;
    private final OrderManager orderManager;
    private final AddressGenerator addressGenerator;
    private final TransactionTemplate transactionTemplate;
    private final OrderPickupTaskDtoMapper orderPickupTaskDtoMapper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final ReturnTaskDtoMapper returnTaskDtoMapper;
    private final OrderRepository orderRepository;

    private PickupPoint pickupPoint;
    private Long userShiftId;
    private DeliveryAddress originalDeliveryAddress;
    private Order clientOrder;
    private Order lockerOrder;

    @BeforeEach
    void init() {
        pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L);
        User user = testUserHelper.findOrCreateUser(1L);
        originalDeliveryAddress = addressGenerator.generate(AddressGenerator.AddressGenerateParam.builder().build());
        lockerOrder = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .pickupPoint(pickupPoint)
                .build()
        );
        lockerOrder.getDelivery().setDeliveryAddress(originalDeliveryAddress);
        clientOrder = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.PAID)
                .build()
        );
        clientOrder.getDelivery().setDeliveryAddress(originalDeliveryAddress);
        Shift shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), 18769);
        userShiftId = userShiftTestHelper.start(UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskOrderPickup(clock.instant()))
                .routePoint(userShiftCommandDataHelper.taskLockerDelivery(lockerOrder, pickupPoint.getId(), 10))
                .routePoint(userShiftCommandDataHelper.taskPrepaid("addr1", 10, clientOrder.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build()
        );
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));
    }

    @Test
    void doTest() {
        configurationServiceAdapter.insertValue(ConfigurationProperties.NEW_LOCKER_ADDRESS_MAPPING_ENABLED, true);
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.GET_PICKUP_POINT_INFO_BY_ID_FROM_LOCKER_DELIVERY_TASK, true);

        transactionTemplate.execute(ts -> {
            orderManager.updateDsOrderData(buildUpdateOrderCommand(clientOrder));
            clientOrder = orderRepository.findById(clientOrder.getId()).orElseThrow();
            orderManager.updateDsOrderData(buildUpdateOrderCommand(lockerOrder));
            lockerOrder = orderRepository.findById(lockerOrder.getId()).orElseThrow();
            return true;
        });

        OrderPickupTaskDto pickupTaskDto = (OrderPickupTaskDto) transactionTemplate.execute(t -> {
            var userShift = userShiftRepository.findById(userShiftId).orElseThrow();
            var pickupTask = userShift.streamPickupTasks().findFirst().orElseThrow();
            return orderPickupTaskDtoMapper.mapToTaskDto(pickupTask, TplTaskUtils.EMPTY_MAPPER_CONTEXT);
        });
        assertThat(pickupTaskDto).isNotNull();
        assertThat(pickupTaskDto.getOrders())
                .allMatch(this::checkOrdersDto)
                .allMatch(this::checkOrderScanText);
        assertThat(pickupTaskDto.getDestinations()).asList()
                .hasSize(1);
        assertEquals(pickupTaskDto.getDestinations().get(0).getDelivery().getAddress(),
                originalDeliveryAddress.getAddress());

        var returnTaskDto = (OrderReturnTaskDto) transactionTemplate.execute(ts -> {
            var userShift = userShiftRepository.findById(userShiftId).orElseThrow();
            testUserHelper.finishPickupAtStartOfTheDay(userShift);
            var returnTaskRp = userShift.streamReturnRoutePoints()
                    .findFirst().orElseThrow();
            testUserHelper.arriveAtRoutePoint(returnTaskRp);
            var returnTask = returnTaskRp.streamReturnTasks().findFirst().orElseThrow();
            return returnTaskDtoMapper.mapToTaskDto(returnTask, TplTaskUtils.EMPTY_MAPPER_CONTEXT);
        });
        assertThat(returnTaskDto).isNotNull();
        assertThat(returnTaskDto.getOrders()).allMatch(this::checkOrdersDto);
    }

    boolean checkOrderScanText(OrderScanTaskDto.OrderForScanDto order) {
        assertThat(order.getText()).isEqualTo(ACCEPT_BUT_NOT_DELIVER_RESCHEDULED_TEXT);
        return true;
    }

    boolean checkOrdersDto(OrderScanTaskDto.OrderForScanDto order) {
        if (Objects.equals(order.getExternalOrderId(), clientOrder.getExternalOrderId())) {
            assertThat(order.getType()).isEqualTo(CLIENT);
            assertThat(order.getDelivery().getAddress())
                    .isEqualTo(clientOrder.getDelivery().getDeliveryAddress().getAddress());
        } else if (Objects.equals(order.getExternalOrderId(), lockerOrder.getExternalOrderId())) {
            assertThat(order.getType()).isEqualTo(LOCKER);
            assertThat(order.getDelivery().getAddress()).isEqualTo(originalDeliveryAddress.getAddress());
        } else {
            return false;
        }
        return true;
    }

    PartnerkaCommand.UpdateDsOrderData buildUpdateOrderCommand(Order order) {
        return PartnerkaCommand.UpdateDsOrderData.builder()
                .newAddress(addressGenerator.generate(AddressGenerator.AddressGenerateParam.builder().build()))
                .oldAddress(order.getDelivery().getDeliveryAddress())
                .orderType(order.isPickup() ? OrderType.CLIENT : LOCKER)
                .orderPaymentStatus(OrderPaymentStatus.PAID)
                .orderPaymentType(OrderPaymentType.PREPAID)
                .existingOrder(order)
                .intervalFrom(LocalTime.from(order.getDelivery().getDeliveryIntervalFrom().atOffset(ZoneOffset.UTC)))
                .intervalTo(LocalTime.from(order.getDelivery().getDeliveryIntervalTo().atOffset(ZoneOffset.UTC)))
                .deliveryDate(order.getDelivery().getDeliveryDateAtDefaultTimeZone().plusDays(3))
                .pickupPoint(order.isPickup() ? null : pickupPoint)
                .recipientNotes("new notes")
                .build();
    }
}
