package ru.yandex.travel.orders.workflows.orderitem.train;

import java.util.List;
import java.util.UUID;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.commons.proto.TPrice;
import ru.yandex.travel.orders.entities.FiscalItem;
import ru.yandex.travel.orders.entities.InvoiceItem;
import ru.yandex.travel.orders.entities.TrainOrder;
import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.orders.entities.TrainOrderUserRefund;
import ru.yandex.travel.orders.entities.TrainTicketRefund;
import ru.yandex.travel.orders.entities.TrustInvoice;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.repository.TrainTicketRefundRepository;
import ru.yandex.travel.orders.services.train.TrainDiscountService;
import ru.yandex.travel.orders.workflow.invoice.proto.ETrustInvoiceState;
import ru.yandex.travel.orders.workflow.order.proto.TServiceRefundFailed;
import ru.yandex.travel.orders.workflow.order.proto.TServiceRefunded;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.TRefundingTicketFailed;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.TRefundingTicketSuccess;
import ru.yandex.travel.orders.workflows.orderitem.train.handlers.RefundingTicketStateHandler;
import ru.yandex.travel.train.model.PassengerCategory;
import ru.yandex.travel.train.model.TrainPassenger;
import ru.yandex.travel.train.model.TrainTicketRefundStatus;
import ru.yandex.travel.train.model.refund.PassengerRefundInfo;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

public class RefundingStateHandlerTest {
    private TrainTicketRefundRepository trainTicketRefundRepository;
    private RefundingTicketStateHandler handler;
    private TrainOrderItemFactory orderItemFactory;
    private final List<Integer> blankIdsToRefund = List.of(100001, 100002);

    @Before
    public void SetUp() {
        trainTicketRefundRepository = mock(TrainTicketRefundRepository.class);
        handler = new RefundingTicketStateHandler(trainTicketRefundRepository, mock(TrainDiscountService.class));
        orderItemFactory = new TrainOrderItemFactory();
        orderItemFactory.setOrderItemState(EOrderItemState.IS_REFUNDING);
    }

    @Test
    public void testHandleRefundingTicketFailed() {
        TrainOrderItem orderItem = createOrderItem();
        var ctx = testMessagingContext(orderItem);
        var event = TRefundingTicketFailed.newBuilder().setRefundId(UUID.randomUUID().toString())
                .addAllBlankIds(blankIdsToRefund).build();
        when(trainTicketRefundRepository.getOne(any())).thenReturn(createTrainTicketRefund(orderItem));
        handler.handleEvent(event, ctx);

        assertThat(orderItem.getState()).isEqualTo(EOrderItemState.IS_CONFIRMED);
        assertThat(orderItem.getPayload().getPassengers().get(0).getTicket().getBlankId()).isEqualTo(100001);
        assertThat(orderItem.getPayload().getPassengers().get(0).getTicket().getRefundStatus()).isNull();
        assertThat(orderItem.getPayload().getPassengers().get(1).getTicket().getBlankId()).isEqualTo(100002);
        assertThat(orderItem.getPayload().getPassengers().get(1).getTicket().getRefundStatus()).isNull();
        assertThat(orderItem.getPayload().getPassengers().get(2).getTicket().getBlankId()).isEqualTo(100003);
        assertThat(orderItem.getPayload().getPassengers().get(2).getTicket().getRefundStatus()).isEqualTo(TrainTicketRefundStatus.REFUNDED);
        assertThat(orderItem.getPayload().getPassengers().get(3).getTicket().getBlankId()).isEqualTo(100001);
        assertThat(orderItem.getPayload().getPassengers().get(3).getTicket().getRefundStatus()).isNull();
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceRefundFailed.class);
    }

    @Test
    public void testHandleRefundingTicketSuccess() {
        TrainOrderItem orderItem = createOrderItem();
        var ctx = testMessagingContext(orderItem);
        var event = TRefundingTicketSuccess.newBuilder().setRefundId(UUID.randomUUID().toString())
                .addAllBlankIds(blankIdsToRefund).build();
        var trainTicketRefund = createTrainTicketRefund(orderItem);
        trainTicketRefund.getPayload().getItems().forEach(x -> x.setRefundOperationStatus(ImOperationStatus.OK));
        when(trainTicketRefundRepository.getOne(any())).thenReturn(trainTicketRefund);

        handler.handleEvent(event, ctx);

        assertThat(orderItem.getState()).isEqualTo(EOrderItemState.IS_REFUNDED);
        assertThat(orderItem.getPayload().getPassengers().get(0).getTicket().getBlankId()).isEqualTo(100001);
        assertThat(orderItem.getPayload().getPassengers().get(0).getTicket().getRefundStatus()).isEqualTo(TrainTicketRefundStatus.REFUNDED);
        assertThat(orderItem.getPayload().getPassengers().get(1).getTicket().getBlankId()).isEqualTo(100002);
        assertThat(orderItem.getPayload().getPassengers().get(1).getTicket().getRefundStatus()).isEqualTo(TrainTicketRefundStatus.REFUNDED);
        assertThat(orderItem.getPayload().getPassengers().get(2).getTicket().getBlankId()).isEqualTo(100003);
        assertThat(orderItem.getPayload().getPassengers().get(2).getTicket().getRefundStatus()).isEqualTo(TrainTicketRefundStatus.REFUNDED);
        assertThat(orderItem.getPayload().getPassengers().get(3).getTicket().getBlankId()).isEqualTo(100001);
        assertThat(orderItem.getPayload().getPassengers().get(3).getTicket().getRefundStatus()).isEqualTo(TrainTicketRefundStatus.REFUNDED);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceRefunded.class);
        TServiceRefunded serviceRefundedMessage = TServiceRefunded.newBuilder()
                .setServiceId(orderItem.getId().toString())
                .setOrderRefundId(trainTicketRefund.getOrderRefund().getId().toString())
                .putTargetFiscalItems(9000000, tPriceOf(0))
                .putTargetFiscalItems(9000001, tPriceOf(700))
                .putTargetFiscalItems(9000002, tPriceOf(0))
                .putTargetFiscalItems(9000003, tPriceOf(0))
                .putTargetFiscalItems(9000012, tPriceOf(180))
                .putTargetFiscalItems(9000030, tPriceOf(0))
                .build();
        TServiceRefunded actualMessage = (TServiceRefunded) ctx.getScheduledEvents().get(0).getMessage();
        assertThat(actualMessage.getServiceId()).isEqualTo(serviceRefundedMessage.getServiceId());
        assertThat(actualMessage.getOrderRefundId()).isEqualTo(serviceRefundedMessage.getOrderRefundId());
        assertThat(actualMessage.getTargetFiscalItemsMap()).isEqualTo(serviceRefundedMessage.getTargetFiscalItemsMap());
    }

    private static TPrice tPriceOf(Number number) {
        return ProtoUtils.toTPrice(Money.of(number, ProtoCurrencyUnit.RUB));
    }

    private TrainTicketRefund createTrainTicketRefund(TrainOrderItem orderItem) {
        var refundItem1 = new PassengerRefundInfo();
        refundItem1.setBlankId(100001);
        refundItem1.setCustomerId(200001);
        refundItem1.setPassengerCategory(PassengerCategory.ADULT);
        refundItem1.setCalculatedRefundTicketAmount(Money.of(100500, ProtoCurrencyUnit.RUB));
        refundItem1.setActualRefundTicketAmount(Money.of(500, ProtoCurrencyUnit.RUB));
        refundItem1.setCalculatedRefundFeeAmount(Money.of(150, ProtoCurrencyUnit.RUB));
        refundItem1.setCalculatedRefundInsuranceAmount(Money.of(100, ProtoCurrencyUnit.RUB));
        var refundItem2 = new PassengerRefundInfo();
        refundItem2.setBlankId(100002);
        refundItem2.setCustomerId(200002);
        refundItem2.setPassengerCategory(PassengerCategory.ADULT);
        refundItem2.setCalculatedRefundTicketAmount(Money.of(100500, ProtoCurrencyUnit.RUB));
        refundItem2.setActualRefundTicketAmount(Money.of(20, ProtoCurrencyUnit.RUB));
        refundItem2.setCalculatedRefundFeeAmount(Money.of(0, ProtoCurrencyUnit.RUB));
        refundItem2.setCalculatedRefundInsuranceAmount(Money.of(0, ProtoCurrencyUnit.RUB));
        var refundItem4 = new PassengerRefundInfo();
        refundItem4.setBlankId(100001);
        refundItem4.setCustomerId(200004);
        refundItem4.setPassengerCategory(PassengerCategory.BABY);
        refundItem4.setCalculatedRefundTicketAmount(Money.of(0, ProtoCurrencyUnit.RUB));
        refundItem4.setActualRefundTicketAmount(Money.of(0, ProtoCurrencyUnit.RUB));
        refundItem4.setCalculatedRefundFeeAmount(Money.of(0, ProtoCurrencyUnit.RUB));
        refundItem4.setCalculatedRefundInsuranceAmount(Money.of(100, ProtoCurrencyUnit.RUB));
        var order = new TrainOrder();
        order.setId(UUID.randomUUID());
        var orderRefund = TrainOrderUserRefund.createForOrder(order);
        return TrainTicketRefund.createRefund(orderItem, List.of(refundItem1, refundItem2, refundItem4), orderRefund);
    }

    private static void addFiscalItem(TrainOrderItem orderItem, Number amount, int internalId) {
        FiscalItem fiscalItem = new FiscalItem();
        fiscalItem.setMoneyAmount(Money.of(amount, ProtoCurrencyUnit.RUB));
        fiscalItem.setInternalId(internalId);
        fiscalItem.setId((long) (9000000 + internalId));
        orderItem.addFiscalItem(fiscalItem);
    }

    @SuppressWarnings("SameParameterValue")
    private TrainPassenger createBabyPassenger(TrainOrderItem orderItem, int startFiscalItemInternalId) {
        TrainPassenger p1 = orderItemFactory.createTrainPassenger();
        p1.setCategory(PassengerCategory.BABY);
        p1.getInsurance().setFiscalItemInternalId(startFiscalItemInternalId);
        p1.getInsurance().setAmount(Money.of(100, ProtoCurrencyUnit.RUB));
        addFiscalItem(orderItem, 100, startFiscalItemInternalId);
        p1.getTicket().setTariffAmount(Money.zero(ProtoCurrencyUnit.RUB));
        p1.getTicket().setServiceAmount(Money.zero(ProtoCurrencyUnit.RUB));
        p1.getTicket().setFeeAmount(Money.zero(ProtoCurrencyUnit.RUB));
        return p1;
    }

    private TrainPassenger createTrainPassenger(TrainOrderItem orderItem, int startFiscalItemInternalId) {
        TrainPassenger p1 = orderItemFactory.createTrainPassenger();
        p1.getInsurance().setFiscalItemInternalId(startFiscalItemInternalId);
        p1.getInsurance().setAmount(Money.of(100, ProtoCurrencyUnit.RUB));
        addFiscalItem(orderItem, 100, startFiscalItemInternalId);

        p1.getTicket().setTariffFiscalItemInternalId(startFiscalItemInternalId + 1);
        p1.getTicket().setTariffAmount(Money.of(1000, ProtoCurrencyUnit.RUB));
        addFiscalItem(orderItem, 1000, startFiscalItemInternalId + 1);

        p1.getTicket().setServiceFiscalItemInternalId(startFiscalItemInternalId + 2);
        p1.getTicket().setServiceAmount(Money.of(200, ProtoCurrencyUnit.RUB));
        addFiscalItem(orderItem, 200, startFiscalItemInternalId + 2);

        p1.getTicket().setFeeFiscalItemInternalId(startFiscalItemInternalId + 3);
        p1.getTicket().setFeeAmount(Money.of(150, ProtoCurrencyUnit.RUB));
        addFiscalItem(orderItem, 150, startFiscalItemInternalId + 3);
        return p1;
    }

    private TrainOrderItem createOrderItem() {
        TrainOrderItem orderItem = orderItemFactory.createTrainOrderItem();

        TrainPassenger p1 = createTrainPassenger(orderItem, 0);
        p1.setCustomerId(200001);
        p1.getTicket().setBlankId(100001);
        p1.getTicket().setRefundStatus(TrainTicketRefundStatus.REFUNDING);

        TrainPassenger p2 = createTrainPassenger(orderItem, 10);
        p2.setCustomerId(200002);
        p2.getTicket().setBlankId(100002);
        p2.getTicket().setRefundStatus(TrainTicketRefundStatus.REFUNDING);

        TrainPassenger p3 = createTrainPassenger(orderItem, 20);
        p3.setCustomerId(200003);
        p3.getTicket().setBlankId(100003);
        p3.getTicket().setRefundStatus(TrainTicketRefundStatus.REFUNDED);

        TrainPassenger p4 = createBabyPassenger(orderItem, 30);
        p4.setCustomerId(200004);
        p4.getTicket().setBlankId(100001);
        p4.getTicket().setRefundStatus(TrainTicketRefundStatus.REFUNDING);

        orderItem.getPayload().setPassengers(List.of(p1, p2, p3, p4));

        var invoice = new TrustInvoice();
        orderItem.getOrder().setCurrentInvoice(invoice);
        invoice.setOrder(orderItem.getOrder());
        for (var fiscalItem : orderItem.getFiscalItems()) {
            var invoiceItem = new InvoiceItem();
            invoice.addInvoiceItem(invoiceItem);
            invoiceItem.setFiscalItemId(fiscalItem.getId());
            invoiceItem.setPriceMoney(fiscalItem.getMoneyAmount());
        }
        invoice.setState(ETrustInvoiceState.IS_CLEARED);

        return orderItem;
    }
}
