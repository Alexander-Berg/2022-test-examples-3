package ru.yandex.market.tpl.core.service.lms;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderChequeRemoteBatchDto;
import ru.yandex.market.tpl.api.model.order.OrderChequeRemoteDto;
import ru.yandex.market.tpl.api.model.order.OrderChequeType;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCheque;
import ru.yandex.market.tpl.core.domain.order.OrderCommand;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partial_return_order.PartialReturnOrderGenerateService;
import ru.yandex.market.tpl.core.domain.partial_return_order.repository.PartialReturnOrderRepository;
import ru.yandex.market.tpl.core.domain.partial_return_order.state_processing_lrm.repository.PartialReturnStateProcessingLrmRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptHelper;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftBatchService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.service.lms.order.LmsOrderService;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LmsOrderServiceReopenButtonTest {

    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final SortingCenterService sortingCenterService;
    private final SortingCenterRepository sortingCenterRepository;
    private final CacheManager cacheManager;
    private final PartialReturnStateProcessingLrmRepository partialReturnStateProcessingLrmRepository;
    private final PartialReturnOrderRepository partialReturnOrderRepository;
    private final UserShiftReassignManager userShiftReassignManager;
    private final PartialReturnOrderGenerateService partialReturnOrderGenerateService;
    private final UserShiftBatchService userShiftBatchService;
    private final TransactionTemplate transactionTemplate;
    private final UserShiftRepository userShiftRepository;
    private final OrderRepository orderRepository;
    private final OrderCommandService orderCommandService;
    private final EntityManager entityManager;
    private final LmsOrderService lmsOrderService;
    private final ReceiptHelper receiptHelper;
    private final UserShiftCommandService userShiftCommandService;

    private User user;
    private Order order;
    private Shift shift;
    private UserShift userShift;
    private OrderDeliveryTask orderDeliveryTask;


    @BeforeEach
    void init() {

        user = testUserHelper.findOrCreateUser(1L);
        shift = testUserHelper.findOrCreateOpenShiftForSc(
                LocalDate.now(),
                sortingCenterService.findSortCenterForDs(239).getId()
        );

        userShift = userShiftRepository.findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(),
                user));

        order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .externalOrderId("32423445234")
                        .paymentType(OrderPaymentType.CASH)
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .itemsCount(3)
                                        .isFashion(true)
                                        .build()
                        )
                        .build()
        );

        userShiftReassignManager.assign(userShift, order);
        testUserHelper.checkinAndFinishPickup(userShift);
        orderDeliveryTask = userShift.streamOrderDeliveryTasks().findFirst().orElseThrow();

        testUserHelper.arriveAtRoutePoint(orderDeliveryTask.getRoutePoint());
    }

    @Test
    void buttonDisabled_WhenNotPrePaidNotBoxedWrongRoute() {
        partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(order);

        var orderCheque = new OrderChequeRemoteBatchDto();
        orderCheque.setTaskId(orderDeliveryTask.getId());
        orderCheque.setPaymentType(OrderPaymentType.CASH);
        orderCheque.setChequeType(OrderChequeType.SELL);

        registerCheques(orderCheque);
        fillCheque();

        var task = entityManager.find(OrderDeliveryTask.class, orderDeliveryTask.getId());
        var finalStateOrder = entityManager.find(Order.class, order.getId());

        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERED_AND_NOT_PACK_RETURN_BOXES);
        assertThat(finalStateOrder.getPaymentType()).isEqualTo(OrderPaymentType.CASH);
        assertThat(task.getRoutePoint()).isEqualTo(userShift.getCurrentRoutePoint());

    }

    @Test
    void buttonEnabled_WhenOrderDeliveredFull() {

        var orderCheque = new OrderChequeRemoteDto(OrderPaymentType.CASH, OrderChequeType.SELL);
        orderCommandService.registerCheque(new OrderCommand.RegisterCheque(
                orderDeliveryTask, order.getId(), "aaa", null, orderCheque, true, false, null, Optional.empty()
        ));

        fillCheque();

        var task = entityManager.find(OrderDeliveryTask.class, orderDeliveryTask.getId());
        var finalStateOrder = entityManager.find(Order.class, order.getId());

        assertThat(finalStateOrder.getDeliveryStatus()).isEqualTo(OrderDeliveryStatus.DELIVERED);
    }

    private void registerCheques(OrderChequeRemoteBatchDto orderCheque) {
        userShiftBatchService.registerCheques(
                user,
                List.of(orderCheque),
                null
        );
    }

    private OrderCheque fillCheque() {
        var cheque = transactionTemplate.execute(ts -> {
            OrderCheque cheque1 = orderRepository.findByIdOrThrow(order.getId()).getCheques().get(0);
            assertThat(cheque1.getTotal()).isEqualTo(cheque1.getOrder().getCostForPurchasedOrderItemsInstancesWithDeliveryPrice());
            return cheque1;
        });

        orderCommandService.fillCheque(new OrderCommand.FillCheque(
                orderDeliveryTask.getOrderId(),
                orderDeliveryTask.getId(),
                cheque,
                partialReturnOrderRepository.findByOrderId(orderDeliveryTask.getOrderId())
        ));

        return cheque;
    }
}
