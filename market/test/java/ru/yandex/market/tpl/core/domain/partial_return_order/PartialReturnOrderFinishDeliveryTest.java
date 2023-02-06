package ru.yandex.market.tpl.core.domain.partial_return_order;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderChequeRemoteBatchDto;
import ru.yandex.market.tpl.api.model.order.OrderChequeType;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCheque;
import ru.yandex.market.tpl.core.domain.order.OrderCommand;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partial_return_order.repository.PartialReturnOrderRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptData;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptDataRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftBatchService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.tpl.api.model.order.OrderFlowStatus.TRANSMITTED_TO_RECIPIENT;
import static ru.yandex.market.tpl.api.model.order.OrderFlowStatus.TRANSMITTED_TO_RECIPIENT_AND_NOT_PACK_RETURN_BOXES;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus.DELIVERED_AND_NOT_PACK_RETURN_BOXES;

@RequiredArgsConstructor
public class PartialReturnOrderFinishDeliveryTest extends TplAbstractTest {
    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final PartialReturnOrderGenerateService partialReturnOrderGenerateService;
    private final UserShiftBatchService userShiftBatchService;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftManager userShiftManager;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final OrderCommandService orderCommandService;
    private final OrderRepository orderRepository;
    private final PartialReturnOrderRepository partialReturnOrderRepository;
    private final OrderManager orderManager;
    private final ReceiptDataRepository receiptDataRepository;

    private User user;
    private Shift shift;
    private UserShift userShift;
    private Order order;
    private OrderDeliveryTask orderDeliveryTask;

    @BeforeEach
    public void init() {
        transactionTemplate.execute(ts -> {
                    user = testUserHelper.findOrCreateUser(1L);
                    shift = testUserHelper.findOrCreateOpenShiftForSc(
                            LocalDate.now(clock),
                            sortingCenterService.findSortCenterForDs(239).getId()
                    );
                    userShift = userShiftRepository.findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(),
                            user));

                    order = orderGenerateService.createOrder(
                            OrderGenerateService.OrderGenerateParam.builder()
                                    .externalOrderId("32423445234")
                                    .paymentType(OrderPaymentType.CASH)
                                    .paymentStatus(OrderPaymentStatus.PAID)
                                    .items(
                                            OrderGenerateService.OrderGenerateParam.Items.builder()
                                                    .isFashion(true)
                                                    .build()
                                    )
                                    .build()
                    );

                    userShiftReassignManager.assign(userShift, order);
                    testUserHelper.checkinAndFinishPickup(userShift);
                    orderDeliveryTask = userShift.streamOrderDeliveryTasks().findFirst().orElseThrow();
                    return null;
                }
        );
    }

    @Test
    void finishUserShiftWhenExistPartialReturnOrder() {
        partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(order);
        testUserHelper.arriveAtRoutePoint(orderDeliveryTask.getRoutePoint());

        OrderCheque cheque = registerCheque();

        userShiftManager.returnOrders(userShift.getId());
        assertThatThrownBy(() -> testUserHelper.finishUserShift(userShift.getId()))
                .isInstanceOf(TplIllegalStateException.class);

        //вставляем правильный чп
        orderCommandService.updateFlowStatus(
                new OrderCommand.UpdateFlowStatus(order.getId(), TRANSMITTED_TO_RECIPIENT),
                Source.SYSTEM
        );

        testUserHelper.finishUserShift(userShift.getId());

        dbQueueTestUtil.assertQueueHasSize(QueueType.PROCESS_PARTIAL_RETURN_ORDER, 2);
    }

    @Test
    void returnChequeWithPartialReturn() {
        partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(order);
        testUserHelper.arriveAtRoutePoint(orderDeliveryTask.getRoutePoint());

        registerCheque();
        orderCommandService.updateFlowStatus(
                new OrderCommand.UpdateFlowStatus(order.getId(), TRANSMITTED_TO_RECIPIENT),
                Source.SYSTEM
        );

        var orderCheque = new OrderChequeRemoteBatchDto();
        orderCheque.setTaskId(orderDeliveryTask.getId());
        orderCheque.setPaymentType(OrderPaymentType.CASH);
        orderCheque.setChequeType(OrderChequeType.RETURN);

        userShiftBatchService.registerCheques(user, List.of(orderCheque), null);
        var cheques = transactionTemplate.execute(ts -> {
            var allCheques = orderRepository.findByIdOrThrow(order.getId()).getCheques();
            OrderCheque chequeSell = allCheques.stream().filter(c -> c.getChequeType() == OrderChequeType.SELL).findFirst().orElseThrow();
            OrderCheque chequeReturn = allCheques.stream().filter(c -> c.getChequeType() == OrderChequeType.RETURN).findFirst().orElseThrow();
            List<ReceiptData> receiptDataAll = receiptDataRepository.findAll();
            var receiptDataFirst = receiptDataAll.get(0);
            var receiptDataSecond = receiptDataAll.get(1);
            assertThat(receiptDataFirst.getItems().size()).isEqualTo(receiptDataSecond.getItems().size());
            assertThat(chequeReturn.getTotal()).isEqualTo(chequeSell.getTotal());
            return allCheques;
        });
    }

    @Test
    void changePaymentTypeWithPartialReturn() {
        partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(order);
        testUserHelper.arriveAtRoutePoint(orderDeliveryTask.getRoutePoint());

        registerCheque();
        orderCommandService.updateFlowStatus(
                new OrderCommand.UpdateFlowStatus(order.getId(), TRANSMITTED_TO_RECIPIENT),
                Source.SYSTEM
        );

        orderManager.changeOrderPaymentType(order.getId());
        var cheques = transactionTemplate.execute(ts -> {
            var allCheques = orderRepository.findByIdOrThrow(order.getId()).getCheques();
            assertThat(allCheques).hasSize(3);
            var firstCheque = allCheques.get(0);
            var secondCheque = allCheques.get(1);
            var thirdCheque = allCheques.get(2);
            assertThat(firstCheque.getTotal()).isEqualTo(secondCheque.getTotal());
            assertThat(secondCheque.getTotal()).isEqualTo(thirdCheque.getTotal());
            return allCheques;
        });
    }

    @Test
    void registerChequesWhenExistPartialReturnOrderAndAllItemsInstancesAreReturned() {
        partialReturnOrderGenerateService.generatePartialReturnWithAllReturnItemsInstances(order);

        testUserHelper.arriveAtRoutePoint(orderDeliveryTask.getRoutePoint());

        var orderCheque = new OrderChequeRemoteBatchDto();
        orderCheque.setTaskId(orderDeliveryTask.getId());
        orderCheque.setPaymentType(OrderPaymentType.CASH);
        orderCheque.setChequeType(OrderChequeType.SELL);

        //нельзя оплатить ничего или только доставку
        var exception = assertThrows(RuntimeException.class, () -> userShiftBatchService.registerCheques(
                user,
                List.of(orderCheque),
                null));

        // т.к. перешли на батчи, то кидается общее RuntimeException исключение, содержащее в себе уже n исключений
        assertThat(exception.getMessage()).contains("We can't execute action with all returned items instances.");
    }

    private OrderCheque registerCheque() {
        var orderCheque = new OrderChequeRemoteBatchDto();
        orderCheque.setTaskId(orderDeliveryTask.getId());
        orderCheque.setPaymentType(OrderPaymentType.CASH);
        orderCheque.setChequeType(OrderChequeType.SELL);

        userShiftBatchService.registerCheques(
                user,
                List.of(orderCheque),
                null
        );
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

        cheque = transactionTemplate.execute(ts -> {
            OrderCheque cheque1 = orderRepository.findByIdOrThrow(order.getId()).getCheques().get(0);
            assertThat(cheque1.getTotal()).isEqualTo(cheque1.getOrder().getCostForPurchasedOrderItemsInstancesWithDeliveryPrice());
            return cheque1;
        });

        //проверяем, что таска и заказ перешли в нужные статусы
        assertThat(entityManager.find(OrderDeliveryTask.class, orderDeliveryTask.getId()).getStatus())
                .isEqualTo(DELIVERED_AND_NOT_PACK_RETURN_BOXES);
        assertThat(entityManager.find(Order.class, order.getId()).getOrderFlowStatus())
                .isEqualTo(TRANSMITTED_TO_RECIPIENT_AND_NOT_PACK_RETURN_BOXES);

        userShiftCommandService.finishOrderDeliveryTask(user, UserShiftCommand.FinishOrderDeliveryTask.builder()
                .userShiftId(userShift.getId())
                .taskId(orderDeliveryTask.getId())
                .build());

        return cheque;
    }

}
