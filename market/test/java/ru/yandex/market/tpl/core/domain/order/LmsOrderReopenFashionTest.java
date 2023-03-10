package ru.yandex.market.tpl.core.domain.order;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.partner.OrderEventType;
import ru.yandex.market.tpl.api.model.partial_return_order.LinkReturnableItemsInstancesWithBoxesRequestDto;
import ru.yandex.market.tpl.core.domain.order.property.TplOrderProperties;
import ru.yandex.market.tpl.core.domain.partial_return_order.PartialReturnOrderGenerateService;
import ru.yandex.market.tpl.core.domain.partial_return_order.state_processing_lrm.repository.PartialReturnStateProcessingLrmRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.lms.order.LmsOrderService;
import ru.yandex.market.tpl.core.service.partial_return.LinkReturnableItemsInstancesWithBoxesService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@RequiredArgsConstructor
class LmsOrderReopenFashionTest extends TplAbstractTest {

    private final PartialReturnOrderGenerateService partialReturnOrderGenerateService;
    private final TestDataFactory testDataFactory;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftRepository userShiftRepository;
    private final LmsOrderService lmsOrderService;
    private final TestUserHelper testUserHelper;
    private final LinkReturnableItemsInstancesWithBoxesService linkReturnableItemsInstancesWithBoxesService;
    private final PartialReturnStateProcessingLrmRepository partialReturnStateProcessingLrmRepository;
    private final OrderHistoryEventRepository orderHistoryEventRepository;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftCommandDataHelper helper;

    private User user;
    private Shift shift;
    private Order order;
    private OrderDeliveryTask orderDeliveryTask;

    @BeforeEach
    void init() {
        user = testUserHelper.findOrCreateUser(2L);
        shift = testUserHelper.findOrCreateOpenShift(LocalDate.now());
    }

    @Test
    void reopenFashionOrder_WhenOrderPurchasedFull() {

        order = testDataFactory.generateOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder().isFashion(true).itemsItemCount(3).build())
                        .paymentType(OrderPaymentType.CARD)
                        .build()
        );

        //????????????????, ?????? ?????????? ???????????????? ?????????????????? IS_TRYING_AVAILABLE ?? ?????? ?????? ?????????? ???????????????? true
        assertThat(order.getPropertyValueOrDefault(TplOrderProperties.IS_TRYING_AVAILABLE)).isNotNull();
        assertThat(order.getPropertyValueOrDefault(TplOrderProperties.IS_TRYING_AVAILABLE).booleanValue()).isTrue();

        transactionTemplate.executeWithoutResult(action -> {
            var us = userShiftRepository.findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(),
                    user));
            userShiftReassignManager.assign(us, order);
            testUserHelper.checkinAndFinishPickup(us);
            testUserHelper.finishDelivery(us.getCurrentRoutePoint(), false);
        });

        var orderWithItemsWithInstances = getUpdatedOrderWithItemsWithInstances();

        lmsOrderService.reopenFashionOrder(orderWithItemsWithInstances.getId());

        var updatedOrder = getUpdatedOrderWithItems();

        //??????????????????, ?????? ?????????? ???????????? ?? ?????? ???????????????? ???????????????? ?????????? false
        assertThat(updatedOrder).isNotNull();
        assertThat(updatedOrder.getPropertyValueOrDefault(TplOrderProperties.IS_TRYING_AVAILABLE).booleanValue()).isFalse();
        assertThat(
                partialReturnStateProcessingLrmRepository.findAllByExternalOrderIds(List.of(updatedOrder.getExternalOrderId())).isEmpty()
        ).isTrue();

        //??????????????????, ?????? ???????????? ?? ?????????????? ??????????????
        var orderHistoryEventsCount =
                orderHistoryEventRepository.findAllByOrderId(updatedOrder.getId()).stream().filter(
                        event -> event.getType().equals(OrderEventType.FASHION_ORDER_REOPEN_ENABLED)
                ).count();
        assertThat(orderHistoryEventsCount).isEqualTo(1);
    }

    @Test
    void fashionOrederToOrder_WhenOrderReturnedFullAndNotPrepaid() {

        //?????????????????????????????? ??????????
        order = testDataFactory.generateOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder().isFashion(true).itemsItemCount(3).build())
                        .paymentType(OrderPaymentType.CARD)
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .build()
        );

        //?????????????????????????????? ???????????????? ?????? ???????????????????? ????????????????
        List<OrderItemInstance> instances = order.getItems().stream()
                .flatMap(OrderItem::streamInstances)
                .collect(Collectors.toList());

        transactionTemplate.executeWithoutResult(action -> {
            var us = userShiftRepository.findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(), user));
            userShiftReassignManager.assign(us, order);
            var orderDeliveryTask = us.streamOrderDeliveryTasks()
                    .filter(t -> Objects.equals(t.getOrderId(), order.getId()))
                    .findFirst().orElseThrow();
            testUserHelper.checkinAndFinishPickup(us);

            partialReturnOrderGenerateService.generatePartialReturnWithAllReturnItemsInstances(order);

            var request = prepareLinkRequest(instances, orderDeliveryTask, order.getExternalOrderId());

            linkReturnableItemsInstancesWithBoxesService.link(request, user);

        });

        var orderWithItemsWithInstances = getUpdatedOrderWithItemsWithInstances();

        //?????????????????????????? ??????????.
        lmsOrderService.reopenFashionOrder(orderWithItemsWithInstances.getId());

        var updatedOrder = getUpdatedOrderWithItems();

        //??????????????????, ?????? ?????????? ???????????? ?? ?????? ???????????????? ???????????????? ?????????? false
        assertThat(updatedOrder).isNotNull();
        assertThat(updatedOrder.getPropertyValueOrDefault(TplOrderProperties.IS_TRYING_AVAILABLE).booleanValue()).isFalse();
        assertThat(
                partialReturnStateProcessingLrmRepository.findAllByExternalOrderIds(List.of(updatedOrder.getExternalOrderId())).isEmpty()
        ).isTrue();

        //??????????????????, ?????? ???????????? ?? ?????????????? ??????????????
        var orderHistoryEventsCount =
                orderHistoryEventRepository.findAllByOrderId(updatedOrder.getId()).stream().filter(
                        event -> event.getType().equals(OrderEventType.FASHION_ORDER_REOPEN_ENABLED)
                ).count();
        assertThat(orderHistoryEventsCount).isEqualTo(1);
    }

    @Test
    void reopenFashionThrowsException_WhenOrderPrepaidAndCanceled() {
        order = testDataFactory.generateOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder().isFashion(true).itemsItemCount(3).build())
                        .paymentType(OrderPaymentType.PREPAID)
                        .paymentStatus(OrderPaymentStatus.PAID)
                        .build()
        );

        //???????????????????? ?????????? ?? ??????????????????????
        order.setDeliveryStatus(OrderDeliveryStatus.CANCELLED);

        assertThatThrownBy(() -> lmsOrderService.reopenFashionOrder(order.getId())).isInstanceOf(RuntimeException.class);

        //??????????????????, ?????? ???????????? ?? ?????????????? ??????????????
        var orderHistoryEventsCount = orderHistoryEventRepository.findAllByOrderId(order.getId()).stream().filter(
                event -> event.getType().equals(OrderEventType.FASHION_ORDER_REOPEN_ENABLED)
        ).count();
        assertThat(orderHistoryEventsCount).isEqualTo(0);
    }

    @Test
    void reopenOrderThrowsException_WhenNotFullPurchased() {
        order = testDataFactory.generateOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder().isFashion(true).itemsItemCount(3).build())
                        .paymentType(OrderPaymentType.CASH)
                        .build()
        );

        //?????????? ?? ?????????????????? ?????????????????? 1 ????????????
        transactionTemplate.executeWithoutResult(action -> {
            var us = userShiftRepository.findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(), user));
            userShiftReassignManager.assign(us, order);
            testUserHelper.checkinAndFinishPickup(us);

            partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(order);
            testUserHelper.finishDelivery(us.getCurrentRoutePoint(), false);
        });

        var orderWithItemsWithInstances = getUpdatedOrderWithItemsWithInstances();

        assertThatThrownBy(() -> lmsOrderService.reopenFashionOrder(orderWithItemsWithInstances.getId())).isInstanceOf(RuntimeException.class);

        //??????????????????, ?????? ???????????? ?? ?????????????? ???? ??????????????????????????
        var orderHistoryEventsCount =
                orderHistoryEventRepository.findAllByOrderId(orderWithItemsWithInstances.getId())
                        .stream().filter(
                                event -> event.getType().equals(OrderEventType.FASHION_ORDER_REOPEN_ENABLED)
                        ).count();
        assertThat(orderHistoryEventsCount).isEqualTo(0);
    }

    @Test
    void reopenedOrderTransitionsToTransporation_Recipient_WhenAfterCanceledFashionReopned() {
        //?????????????????????????????? ??????????
        order = testDataFactory.generateOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(OrderGenerateService.OrderGenerateParam.Items.builder().isFashion(true).itemsItemCount(3).build())
                        .paymentType(OrderPaymentType.CARD)
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .build()
        );

        //?????????????????????????????? ???????????????? ?????? ???????????????????? ????????????????
        List<OrderItemInstance> instances = order.getItems().stream()
                .flatMap(OrderItem::streamInstances)
                .collect(Collectors.toList());

        transactionTemplate.executeWithoutResult(action -> {
            var us = userShiftRepository.findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(), user));
            userShiftReassignManager.assign(us, order);
            orderDeliveryTask = us.streamOrderDeliveryTasks()
                    .filter(t -> Objects.equals(t.getOrderId(), order.getId()))
                    .findFirst().orElseThrow();
            testUserHelper.checkinAndFinishPickup(us);

            partialReturnOrderGenerateService.generatePartialReturnWithAllReturnItemsInstances(order);

            var request = prepareLinkRequest(instances, orderDeliveryTask, order.getExternalOrderId());
            linkReturnableItemsInstancesWithBoxesService.link(request, user);

            //?????????????????? ?????????? ?????????? ???????????????? ???????????? ???? ????????????????
            order = getUpdatedOrderWithItems();

            //?????????????????????????? ??????????.
            lmsOrderService.reopenFashionOrder(order.getId());

            var route = us.streamRoutePoints()
                    .findFirst().get();

            assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.PACK_RETURN_BOXES_AND_READY_FOR_RETURN);

            userShiftCommandService.returnCheque(user, new UserShiftCommand.PrintOrReturnCheque(
                    us.getId(), route.getId(), route.streamTasks().findFirst().get().getId(),
                    helper.getChequeDto(OrderPaymentType.CARD), Instant.now(), true, null, Optional.empty()
            ));

            order = getUpdatedOrderWithItems();

            assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
            assertThat(order.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.NOT_DELIVERED);

            testUserHelper.finishDelivery(us.streamRoutePoints().collect(Collectors.toList()).get(0), false);
        });

        var updatedOrder = getUpdatedOrderWithItems();

        assertThat(updatedOrder.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.DELIVERED);
        assertThat(updatedOrder.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSMITTED_TO_RECIPIENT);
    }

    private Order getUpdatedOrderWithItemsWithInstances() {
        return transactionTemplate.execute(action -> {
            var tempOrder = entityManager.find(Order.class, order.getId());
            Hibernate.initialize(tempOrder.getItems());
            tempOrder.getItems().forEach(item -> Hibernate.initialize(item.getInstances()));
            return tempOrder;
        });
    }

    private Order getUpdatedOrderWithItems() {
        return transactionTemplate.execute(action -> {
            var tmpOrder = entityManager.find(Order.class, order.getId());
            Hibernate.initialize(tmpOrder.getProperties());
            Hibernate.initialize(tmpOrder.getCheques());

            return tmpOrder;
        });
    }

    private LinkReturnableItemsInstancesWithBoxesRequestDto prepareLinkRequest(List<OrderItemInstance> instances,
                                                                               OrderDeliveryTask orderDeliveryTask,
                                                                               String orderExternalId) {
        return LinkReturnableItemsInstancesWithBoxesRequestDto.builder()
                .orders(List.of(
                        LinkReturnableItemsInstancesWithBoxesRequestDto.LinkReturnableItemsInstancesWithBoxesByOrderRequestDto.builder()
                                .boxBarcodes(List.of("FSN_RET_123", "FSN_RET_456"))
                                .uits(instances.stream().map(OrderItemInstance::getUit).collect(Collectors.toList()))
                                .externalOrderId(orderExternalId)
                                .taskId(orderDeliveryTask.getId())
                                .build()
                ))
                .build();
    }
}
