package ru.yandex.market.tpl.core.service.lms;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistics.front.library.dto.Action;
import ru.yandex.market.logistics.front.library.dto.detail.DetailChild;
import ru.yandex.market.logistics.front.library.dto.detail.DetailData;
import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.dto.grid.GridItem;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.lms.order.LmsOrderFilterDto;
import ru.yandex.market.tpl.core.domain.lms.order.LmsReopenFashionOrder;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCommand;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partial_return_order.PartialReturnOrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.service.lms.order.LmsOrderService;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LmsOrderServiceTest {

    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final SortingCenterService sortingCenterService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final PartialReturnOrderGenerateService partialReturnOrderGenerateService;
    private final DbQueueTestUtil dbQueueTestUtil;

    private final LmsOrderService lmsOrderService;

    @Test
    public void getOrderDetailData() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("123")
                .paymentType(OrderPaymentType.PREPAID)
                .pickupPoint(null)
                .deliveryDate(LocalDate.of(1990, 1, 2))
                .build()
        );
        DetailData detailData = lmsOrderService.getOrderById(order.getId());
        assertThat(detailData.getItem().getTitle()).isEqualTo("Заказ 123");
        assertThat(detailData.getItem().getValues()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "externalOrderId", "123",
                "orderType", "Клиенту",
                "deliveryDate", LocalDate.of(1990, 1, 2),
                "orderFlowStatus", "Заказ готов к выдаче курьеру на СЦ",
                "paymentType", "Предоплата"
        ));
        assertThat(detailData.getMeta().getActions())
                .filteredOn(this::actionIsActive)
                .extracting(Action::getSlug)
                .isEmpty();
        assertThat(detailData.getMeta().getActions())
                .filteredOn(action -> !actionIsActive(action))
                .extracting(Action::getSlug)
                .hasSize(4);
    }

    @Test
    public void getOrderDetailDataActions() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("123")
                .paymentType(OrderPaymentType.PREPAID)
                .pickupPoint(testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L))
                .deliveryDate(LocalDate.of(1990, 1, 2))
                .flowStatus(OrderFlowStatus.DELIVERED_TO_PICKUP_POINT)
                .build()
        );
        DetailData detailData = lmsOrderService.getOrderById(order.getId());
        List<Action> actions = detailData.getMeta().getActions();
        assertThat(actions)
                .filteredOn(this::actionIsActive)
                .extracting(Action::getSlug)
                .containsExactlyInAnyOrder("/transitionToReadyForReturn", "/transitionToSortingCenterPrepared");
        assertThat(actions)
                .filteredOn(action -> !actionIsActive(action))
                .extracting(Action::getSlug)
                .containsExactlyInAnyOrder("/reopenFashionOrder", "/transitionToDeliveredToPickupPoint");
        List<DetailChild> children = detailData.getMeta().getChildren();
        assertThat(children).hasSize(1);
        assertThat(children.get(0).getSlug()).isEqualTo("tplScOrders");
    }

    @Test
    public void getOrderGridDataWithoutFilters() {
        GridData unfilteredGridData = lmsOrderService.getOrders(null, Pageable.unpaged());
        assertThat(unfilteredGridData.getTotalCount()).isEqualTo(0);
    }

    @Test
    @Sql("classpath:mockPartner/deliveryServiceWithSC.sql")
    public void getScOrders() {
        DeliveryService deliveryService = sortingCenterService.findDsById(100500);
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("123")
                .deliveryDate(LocalDate.of(1990, 1, 1))
                .deliveryServiceId(deliveryService.getId())
                .build()
        );
        List<GridItem> items = lmsOrderService.getScOrders(order.getId()).getItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getValues()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "checkpoint", 100,
                "scId", 100501L,
                "status", "Заказ передан по АПИ, но не подтверждён СО."
        ));
    }

    @Test
    void reopenButtonEnabled_WhenOrderFull() {
        //подготавливаем смену и заказ
        User user = testUserHelper.findOrCreateUser(1L);
        UserShift us = testUserHelper.createEmptyShift(user, LocalDate.now());
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .isFashion(true)
                        .build())
                .build());
        userShiftReassignManager.assign(us, order);
        testUserHelper.checkinAndFinishPickup(us);

        //завершаем заказ
        testUserHelper.finishDelivery(us.getCurrentRoutePoint(), false);

        //Вытаскиваем все действия (кнопки) по нужному слагу
        var lmsOrederResponse = lmsOrderService.getOrderById(order.getId());
        var reopenFashionActions = getActionFilterBySlug("/reopenFashionOrder", lmsOrederResponse);

        //всего одна нужная кнопка
        assertThat(reopenFashionActions).hasSize(1);
        var action = reopenFashionActions.get(0);

        assertThat(actionIsActive(action)).isTrue();
    }

    @Test
    void reopenButtonDisabled_WhenNotAllPurchased() {
        //подготавливаем заказ и смену
        User user = testUserHelper.findOrCreateUser(1L);
        UserShift us = testUserHelper.createEmptyShift(user, LocalDate.now());
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .isFashion(true)
                        .build())
                .build());
        userShiftReassignManager.assign(us, order);
        testUserHelper.checkinAndFinishPickup(us);

        //чатсичный возврат
        partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(order);

        //завершаем заказ
        testUserHelper.finishDelivery(us.getCurrentRoutePoint(), false);

        //Вытаскиваем все действия (кнопки) по нужному слагу
        var lmsOrederResponse = lmsOrderService.getOrderById(order.getId());
        var reopenFashionActions = getActionFilterBySlug("/reopenFashionOrder", lmsOrederResponse);

        //всего одна нужная кнопка
        assertThat(reopenFashionActions).hasSize(1);
        var action = reopenFashionActions.get(0);

        assertThat(actionIsActive(action)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = OrderPaymentType.class, names = {"CASH", "CARD"})
    void reopenButtonEnabled_WhenOrderNotPrepaidAndCanceled(OrderPaymentType paymentType) {
        User user = testUserHelper.findOrCreateUser(1L);
        UserShift us = testUserHelper.createEmptyShift(user, LocalDate.now());
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .isFashion(true)
                        .build())
                .paymentType(paymentType)
                .build());
        userShiftReassignManager.assign(us, order);
        testUserHelper.checkinAndFinishPickup(us);

        order.cancelOrReturn(new OrderCommand.CancelOrReturn(order.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED, "none"), user, false));

        //Вытаскиваем все действия (кнопки) по нужному слагу
        var lmsOrederResponse = lmsOrderService.getOrderById(order.getId());
        var reopenFashionActions = getActionFilterBySlug("/reopenFashionOrder", lmsOrederResponse);

        //всего одна нужная кнопка
        assertThat(reopenFashionActions).hasSize(1);
        var action = reopenFashionActions.get(0);

        assertThat(actionIsActive(action)).isTrue();
    }

    @Test
    void reopenButtonDisabled_WhenOrderPrepaidAndCanceled() {
        User user = testUserHelper.findOrCreateUser(1L);
        UserShift us = testUserHelper.createEmptyShift(user, LocalDate.now());
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .isFashion(true)
                        .build())
                .paymentType(OrderPaymentType.PREPAID)
                .build());
        userShiftReassignManager.assign(us, order);
        testUserHelper.checkinAndFinishPickup(us);

        //отменяем заказ
        order.cancelOrReturn(new OrderCommand.CancelOrReturn(order.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED, "none"), user, false));

        //Вытаскиваем все действия (кнопки) по нужному слагу
        var lmsOrederResponse = lmsOrderService.getOrderById(order.getId());
        var reopenFashionActions = getActionFilterBySlug("/reopenFashionOrder", lmsOrederResponse);

        //всего одна нужная кнопка
        assertThat(reopenFashionActions).hasSize(1);
        var action = reopenFashionActions.get(0);

        assertThat(actionIsActive(action)).isFalse();
    }

    @Test
    void reopenButtonDisabled_WhenOrderNotFashion() {
        User user = testUserHelper.findOrCreateUser(1L);
        UserShift us = testUserHelper.createEmptyShift(user, LocalDate.now());
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .build())
                .paymentType(OrderPaymentType.CASH)
                .build());
        userShiftReassignManager.assign(us, order);
        testUserHelper.checkinAndFinishPickup(us);

        //отменяем заказ
        order.cancelOrReturn(new OrderCommand.CancelOrReturn(order.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED, "none"), user, false));

        //Вытаскиваем все действия (кнопки) по нужному слагу
        var lmsOrederResponse = lmsOrderService.getOrderById(order.getId());
        var reopenFashionActions = getActionFilterBySlug("/reopenFashionOrder", lmsOrederResponse);

        //всего одна нужная кнопка
        assertThat(reopenFashionActions).hasSize(1);
        var action = reopenFashionActions.get(0);

        assertThat(actionIsActive(action)).isFalse();
    }

    @Test
    void reopenButtonEnabled_WhenAllItemsInstancesReturn() {
        //подготавливаем смену и заказ
        User user = testUserHelper.findOrCreateUser(1L);
        UserShift us = testUserHelper.createEmptyShift(user, LocalDate.now());
        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .externalOrderId("32423445234")
                        .paymentType(OrderPaymentType.PREPAID)
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .itemsCount(3)
                                        .isFashion(true)
                                        .build()
                        )
                        .deliveryPrice(BigDecimal.ZERO)
                        .build()
        );
        userShiftReassignManager.assign(us, order);
        testUserHelper.checkinAndFinishPickup(us);

        partialReturnOrderGenerateService.generatePartialReturnWithAllReturnItemsInstances(order);

        assertThat(LmsReopenFashionOrder.reopenFashionOrderEnabledWillAllReturnItemsInstances(order)).isTrue();
        assertThat(LmsReopenFashionOrder.reopenFashionOrderEnabled(order)).isFalse();

        //Вытаскиваем все действия (кнопки) по нужному слагу
        var lmsOrderResponse = lmsOrderService.getOrderById(order.getId());
        var reopenFashionActions = getActionFilterBySlug("/reopenFashionOrder", lmsOrderResponse);

        //всего одна нужная кнопка
        assertThat(reopenFashionActions).hasSize(1);
        var action = reopenFashionActions.get(0);

        assertThat(actionIsActive(action)).isTrue();
    }

    @Test
    void rescheduleOrdersTest() {
        dbQueueTestUtil.clear(QueueType.RESCHEDULE_ORDER);
        var nowDate = LocalDate.now();
        var order1 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(nowDate)
                        .build());
        var order2 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(nowDate.plusDays(1))
                        .build());

        lmsOrderService.rescheduleOrders(Map.of(
                order1.getExternalOrderId(), nowDate.plusDays(2),
                order2.getExternalOrderId(), nowDate.plusDays(3)
        ));

        dbQueueTestUtil.assertQueueHasSize(QueueType.RESCHEDULE_ORDER, 2);
    }

    public boolean actionIsActive(Action action) {
        return action.isActive() == null; //sic!
    }

    private List<Action> getActionFilterBySlug(String slug, DetailData detailData) {
        return detailData.getMeta().getActions().stream()
                .filter(action -> action.getSlug().equals(slug))
                .collect(Collectors.toList());
    }

}
