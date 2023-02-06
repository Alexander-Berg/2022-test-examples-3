package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.droppeditem.DroppedItemService;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCommand;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderFlowStatusHistory;
import ru.yandex.market.tpl.core.domain.order.OrderFlowStatusHistoryRepository;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftManager;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class ArriveOrdersTest extends TplAbstractTest {

    private final UserShiftCommandService commandService;
    private final OrderCommandService orderCommandService;
    private final OrderRepository orderRepository;
    private final OrderFlowStatusHistoryRepository orderFlowStatusHistoryRepository;
    private final DroppedItemService droppedItemService;
    private final ShiftManager shiftCommandService;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final SortingCenterRepository sortingCenterRepository;
    private final SortingCenterPropertyService sortingCenterPropertyService;


    private final Clock clock;

    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandDataHelper helper;


    @BeforeEach
    void init() {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterRepository.findByIdOrThrow(SortingCenter.DEFAULT_SC_ID),
                SortingCenterProperties.GET_RESCHEDULE_DATE_FROM_COMBINATOR_ENABLED,
                false
        );
        ClockUtil.initFixed(clock);
    }

    @Test
    void shouldRescheduleAtArriveToScIfDeliveryDateBeforeNow() {
        ClockUtil.initFixed(clock, ClockUtil.defaultDateTime().plusDays(1));
        LocalDate initialDeliveryDate = LocalDate.now(clock);

        shouldRescheduleAtArriveToSc(initialDeliveryDate);
    }

    @Test
    void shouldRescheduleAtArriveToScIfAbsentActiveDeliveryTask() {
        LocalDate initialDeliveryDate = LocalDate.now(clock);

        shouldRescheduleAtArriveToSc(initialDeliveryDate);
    }

    private void shouldRescheduleAtArriveToSc(LocalDate initialDeliveryDate) {
        Shift shift = shiftCommandService.findOrCreate(initialDeliveryDate, SortingCenter.DEFAULT_SC_ID);
        shiftCommandService.openShift(shift.getId());
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(initialDeliveryDate)
                        .flowStatus(OrderFlowStatus.SORTING_CENTER_CREATED)
                        .build()
        );
        Order order2 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(initialDeliveryDate.plusDays(1))
                        .flowStatus(OrderFlowStatus.SORTING_CENTER_CREATED)
                        .build()
        );

        OrderCommand.UpdateFlowStatus arriveCommand = new OrderCommand.UpdateFlowStatus(
                order.getId(),
                OrderFlowStatus.SORTING_CENTER_ARRIVED);
        orderCommandService.updateFlowStatusFromSc(arriveCommand);
        OrderCommand.UpdateFlowStatus arriveCommand2 = new OrderCommand.UpdateFlowStatus(
                order2.getId(),
                OrderFlowStatus.SORTING_CENTER_ARRIVED);
        orderCommandService.updateFlowStatusFromSc(arriveCommand2);

        dbQueueTestUtil.assertTasksHasSize(QueueType.RESCHEDULE_ORDER, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.RESCHEDULE_ORDER);

        Order updatedOrder = orderRepository.findByIdOrThrow(order.getId());

        LocalDate actualDeliveryDate = updatedOrder.getDelivery().getDeliveryDate(DateTimeUtil.DEFAULT_ZONE_ID);
        assertThat(actualDeliveryDate).isEqualTo(LocalDate.now(clock).plusDays(1));

        Order updatedOrder2 = orderRepository.findByIdOrThrow(order.getId());
        LocalDate actualDeliveryDate2 = updatedOrder2.getDelivery().getDeliveryDate(DateTimeUtil.DEFAULT_ZONE_ID);
        assertThat(actualDeliveryDate2).isEqualTo(order2.getDelivery().getDeliveryDate(DateTimeUtil.DEFAULT_ZONE_ID));

        List<OrderFlowStatusHistory> history =
                orderFlowStatusHistoryRepository.findByExternalOrderIdHistory(order.getExternalOrderId());
        assertThat(history)
                .extracting(OrderFlowStatusHistory::getOrderFlowStatusAfter)
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                OrderFlowStatus.SORTING_CENTER_CREATED,
                                OrderFlowStatus.SORTING_CENTER_ARRIVED,
                                OrderFlowStatus.DELIVERY_DATE_UPDATED_BY_SHOP,
                                OrderFlowStatus.SORTING_CENTER_ARRIVED)
                );

    }

    @Test
    void shouldNotRescheduleAtArriveToScIfDeliveryTaskIsActive() {
        LocalDate initialDeliveryDate = LocalDate.now(clock);

        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(initialDeliveryDate)
                        .flowStatus(OrderFlowStatus.SORTING_CENTER_CREATED)
                        .build()
        );

        var user = testUserHelper.findOrCreateUser(955L, initialDeliveryDate);
        var shift = testUserHelper.findOrCreateOpenShift(initialDeliveryDate);

        var deliveryTask = helper.taskUnpaid("addr1", 12, order.getId());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(deliveryTask)
                .active(true)
                .build();
        commandService.createUserShift(createCommand);

        OrderCommand.UpdateFlowStatus arriveCommand = new OrderCommand.UpdateFlowStatus(
                order.getId(),
                OrderFlowStatus.SORTING_CENTER_ARRIVED);
        orderCommandService.updateFlowStatusFromSc(arriveCommand);

        Order updatedOrder = orderRepository.findByIdOrThrow(order.getId());
        LocalDate actualDeliveryDate = updatedOrder.getDelivery().getDeliveryDate(DateTimeUtil.DEFAULT_ZONE_ID);
        assertThat(actualDeliveryDate).isEqualTo(initialDeliveryDate);

        List<OrderFlowStatusHistory> history =
                orderFlowStatusHistoryRepository.findByExternalOrderIdHistory(order.getExternalOrderId());
        assertThat(history)
                .extracting(OrderFlowStatusHistory::getOrderFlowStatusAfter)
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                OrderFlowStatus.SORTING_CENTER_CREATED,
                                OrderFlowStatus.SORTING_CENTER_ARRIVED)
                );
    }

    @Test
    void shouldNotRescheduleDroppedOrderAtArriveToSc() {
        LocalDate initialDeliveryDate = LocalDate.now(clock);

        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(initialDeliveryDate)
                        .flowStatus(OrderFlowStatus.SORTING_CENTER_CREATED)
                        .build()
        );

        var shift = testUserHelper.findOrCreateOpenShift(initialDeliveryDate);

        droppedItemService.saveDroppedOrders(shift.getId(), Set.of(order.getExternalOrderId()));


        OrderCommand.UpdateFlowStatus arriveCommand = new OrderCommand.UpdateFlowStatus(
                order.getId(),
                OrderFlowStatus.SORTING_CENTER_ARRIVED);
        orderCommandService.updateFlowStatusFromSc(arriveCommand);

        Order updatedOrder = orderRepository.findByIdOrThrow(order.getId());
        LocalDate actualDeliveryDate = updatedOrder.getDelivery().getDeliveryDate(DateTimeUtil.DEFAULT_ZONE_ID);
        assertThat(actualDeliveryDate).isEqualTo(initialDeliveryDate);

        List<OrderFlowStatusHistory> history =
                orderFlowStatusHistoryRepository.findByExternalOrderIdHistory(order.getExternalOrderId());
        assertThat(history)
                .extracting(OrderFlowStatusHistory::getOrderFlowStatusAfter)
                .containsExactlyInAnyOrderElementsOf(
                        List.of(
                                OrderFlowStatus.SORTING_CENTER_CREATED,
                                OrderFlowStatus.SORTING_CENTER_ARRIVED)
                );
    }

}
