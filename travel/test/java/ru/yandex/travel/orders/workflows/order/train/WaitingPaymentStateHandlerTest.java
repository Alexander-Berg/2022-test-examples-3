package ru.yandex.travel.orders.workflows.order.train;

import java.math.BigDecimal;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.Account;
import ru.yandex.travel.orders.entities.TrainOrder;
import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.repository.TrainOrderItemRepository;
import ru.yandex.travel.orders.services.AccountService;
import ru.yandex.travel.orders.services.train.RebookingService;
import ru.yandex.travel.orders.workflow.order.proto.TMoneyAcquired;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.TConfirmationStart;
import ru.yandex.travel.orders.workflow.train.proto.ETrainOrderState;
import ru.yandex.travel.orders.workflow.train.proto.TStartReservationCancellation;
import ru.yandex.travel.orders.workflows.order.train.handlers.WaitingPaymentStateHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

@SuppressWarnings("FieldCanBeLocal")
public class WaitingPaymentStateHandlerTest {

    private AccountService accountService;
    private RebookingService rebookingService;
    private TrainOrderItemRepository trainOrderItemRepository;

    private WaitingPaymentStateHandler handler;

    @Before
    public void setUp() {
        accountService = mock(AccountService.class);
        rebookingService = mock(RebookingService.class);
        trainOrderItemRepository = mock(TrainOrderItemRepository.class);
        handler = new WaitingPaymentStateHandler(accountService, rebookingService, trainOrderItemRepository);
    }

    private TrainOrder createOrderWithOneItemAndFullAmount100500() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_RESERVED);
        factory.setTariffAmount(Money.of(BigDecimal.valueOf(100500), ProtoCurrencyUnit.RUB));
        factory.setServiceAmount(Money.of(BigDecimal.ZERO, ProtoCurrencyUnit.RUB));
        factory.setFeeAmount(Money.of(BigDecimal.ZERO, ProtoCurrencyUnit.RUB));
        var trainOrderItem = factory.createTrainOrderItem();
        factory.fillFiscalItems(trainOrderItem);

        TrainOrder trainOrder = new TrainOrder();
        trainOrder.addOrderItem(trainOrderItem);
        trainOrder.setAccount(Account.createAccount(ProtoCurrencyUnit.RUB));

        return trainOrder;
    }

    @Test
    public void testHandleMoneyAcquired() {
        var trainOrder = this.createOrderWithOneItemAndFullAmount100500();
        when(this.accountService.getAccountBalance(any())).thenReturn(Money.of(100500, ProtoCurrencyUnit.RUB));
        var ctx = testMessagingContext(trainOrder);

        handler.handleEvent(TMoneyAcquired.newBuilder().build(), ctx);

        assertThat(trainOrder.getState()).isEqualTo(ETrainOrderState.OS_WAITING_CONFIRMATION);
        verify(accountService).getAccountBalance(any());
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TConfirmationStart.class);
    }

    @Test(expected = RuntimeException.class)
    public void testHandleMoneyAcquiredInvalidAmount() {
        var trainOrder = this.createOrderWithOneItemAndFullAmount100500();
        when(this.accountService.getAccountBalance(any())).thenReturn(Money.of(1, ProtoCurrencyUnit.RUB));
        var ctx = testMessagingContext(trainOrder);

        handler.handleEvent(TMoneyAcquired.newBuilder().build(), ctx);
    }

    @Test(expected = RuntimeException.class)
    public void testHandleMoneyAcquiredNoOrderItem() {
        var trainOrder = new TrainOrder();
        when(this.accountService.getAccountBalance(any())).thenReturn(Money.of(1, ProtoCurrencyUnit.RUB));
        var ctx = testMessagingContext(trainOrder);

        handler.handleEvent(TMoneyAcquired.newBuilder().build(), ctx);
    }

    @Test(expected = RuntimeException.class)
    public void testHandleMoneyAcquiredMoreThanOneOrderItem() {
        var trainOrder = new TrainOrder();
        trainOrder.addOrderItem(new TrainOrderItem());
        trainOrder.addOrderItem(new TrainOrderItem());
        when(this.accountService.getAccountBalance(any())).thenReturn(Money.of(1, ProtoCurrencyUnit.RUB));
        var ctx = testMessagingContext(trainOrder);

        handler.handleEvent(TMoneyAcquired.newBuilder().build(), ctx);
    }

    @Test
    public void testStartReservationCancellation() {
        var trainOrder = createOrderWithOneItemAndFullAmount100500();
        trainOrder.setState(ETrainOrderState.OS_WAITING_PAYMENT);

        var ctx = testMessagingContext(trainOrder);
        handler.handleEvent(TStartReservationCancellation.newBuilder().build(), ctx);

        assertThat(trainOrder.getState()).isEqualByComparingTo(ETrainOrderState.OS_WAITING_CANCELLATION);
    }
}
