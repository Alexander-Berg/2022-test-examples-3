package ru.yandex.travel.orders.workflows.orderitem.train.ticketrefund;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.orders.entities.OrderRefund;
import ru.yandex.travel.orders.entities.TrainOrderUserRefund;
import ru.yandex.travel.orders.entities.TrainTicketRefund;
import ru.yandex.travel.orders.management.StarTrekService;
import ru.yandex.travel.orders.workflow.orderitem.train.ticketrefund.proto.ETrainTicketRefundState;
import ru.yandex.travel.orders.workflow.orderitem.train.ticketrefund.proto.TRefundingSuccess;
import ru.yandex.travel.orders.workflows.orderitem.train.ticketrefund.handlers.CheckingRefundStateHandler;
import ru.yandex.travel.train.model.refund.PassengerRefundInfo;
import ru.yandex.travel.train.model.refund.TicketRefund;
import ru.yandex.travel.train.partners.im.model.ImBlankStatus;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationStatus;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationType;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOrderItemType;
import ru.yandex.travel.train.partners.im.model.orderinfo.OrderInfoResponse;
import ru.yandex.travel.train.partners.im.model.orderinfo.OrderItemBlank;
import ru.yandex.travel.train.partners.im.model.orderinfo.OrderItemResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

public class CheckingRefundStateHandlerTest {
    private CheckingRefundStateHandler handler;

    @Before
    public void setUp() {
        handler = new CheckingRefundStateHandler(mock(StarTrekService.class));
    }

    @Test
    public void testRefundingSuccess() {
        OrderInfoResponse orderInfoResponse = createOrderInfoWithTicketRefund();
        TrainTicketRefund refund = createRefund();

        var ctx = testMessagingContext(refund);

        handler.handleEvent(TRefundingSuccess.newBuilder()
                .setPartnerResult(ProtoUtils.toTJson(orderInfoResponse)).build(), ctx);

        assertThat(refund.getState()).isEqualTo(ETrainTicketRefundState.RS_REFUNDED);
        assertThat(refund.getPayload().getItems().size()).isEqualTo(1);
        PassengerRefundInfo refundBlank1 = refund.getPayload().getItems().get(0);
        assertThat(refundBlank1.getActualRefundTicketAmount()).isEqualTo(Money.of(100, ProtoCurrencyUnit.RUB));
        assertThat(refundBlank1.getCalculatedRefundTicketAmount()).isEqualTo(Money.of(150, ProtoCurrencyUnit.RUB));
        assertThat(refundBlank1.getRefundOperationStatus()).isEqualTo(ImOperationStatus.OK);
        assertThat(refundBlank1.getRefundOperationId()).isEqualTo(777111);
        assertThat(refundBlank1.getRefundBlankId()).isEqualTo(30001);
    }

    private OrderInfoResponse createOrderInfoWithTicketRefund() {
        var blank1 = new OrderItemBlank();
        blank1.setAmount(BigDecimal.valueOf(200));
        blank1.setOrderItemBlankId(20001);
        var blank2 = new OrderItemBlank();
        blank2.setAmount(BigDecimal.valueOf(300));
        blank2.setOrderItemBlankId(20002);
        var refundBlank1 = new OrderItemBlank();
        refundBlank1.setPreviousOrderItemBlankId(20001);
        refundBlank1.setOrderItemBlankId(30001);
        refundBlank1.setAmount(BigDecimal.valueOf(100));
        refundBlank1.setBlankStatus(ImBlankStatus.REFUNDED);

        OrderInfoResponse resp = new OrderInfoResponse();
        OrderItemResponse buyTicketItem = new OrderItemResponse();
        buyTicketItem.setOperationType(ImOperationType.BUY);
        buyTicketItem.setType(ImOrderItemType.RAILWAY);
        buyTicketItem.setSimpleOperationStatus(ImOperationStatus.OK);
        buyTicketItem.setOrderItemBlanks(List.of(blank1, blank2));

        OrderItemResponse buyInsItem1 = new OrderItemResponse();
        buyInsItem1.setOperationType(ImOperationType.BUY);
        buyInsItem1.setType(ImOrderItemType.INSURANCE);
        buyInsItem1.setSimpleOperationStatus(ImOperationStatus.OK);
        buyInsItem1.setAgentReferenceId("");
        buyInsItem1.setOrderItemBlanks(new ArrayList<>());

        OrderItemResponse refundTicketItem1 = new OrderItemResponse();
        refundTicketItem1.setOperationType(ImOperationType.REFUND);
        refundTicketItem1.setType(ImOrderItemType.RAILWAY);
        refundTicketItem1.setSimpleOperationStatus(ImOperationStatus.OK);
        refundTicketItem1.setAgentReferenceId("refundItem1");
        refundTicketItem1.setOrderItemId(777111);
        refundTicketItem1.setOrderItemBlanks(List.of(refundBlank1));

        OrderItemResponse refundTicketItem2 = new OrderItemResponse();
        refundTicketItem2.setOperationType(ImOperationType.REFUND);
        refundTicketItem2.setType(ImOrderItemType.RAILWAY);
        refundTicketItem2.setSimpleOperationStatus(ImOperationStatus.FAILED);
        refundTicketItem2.setAgentReferenceId("refundItem2");
        refundTicketItem2.setOrderItemId(777113);
        refundTicketItem2.setOrderItemBlanks(List.of());

        OrderItemResponse refundInsItem1 = new OrderItemResponse();
        refundInsItem1.setOperationType(ImOperationType.REFUND);
        refundInsItem1.setType(ImOrderItemType.INSURANCE);
        refundInsItem1.setSimpleOperationStatus(ImOperationStatus.OK);
        refundInsItem1.setAgentReferenceId("");
        refundInsItem1.setOrderItemId(777112);
        refundInsItem1.setOrderItemBlanks(new ArrayList<>());

        resp.setOrderItems(List.of(buyTicketItem, buyInsItem1, refundTicketItem1, refundInsItem1, refundTicketItem2));
        return resp;
    }

    private TrainTicketRefund createRefund() {
        OrderRefund orderRefund = new TrainOrderUserRefund();
        orderRefund.setId(UUID.randomUUID());
        TrainTicketRefund refund = new TrainTicketRefund();
        refund.setId(UUID.randomUUID());
        refund.setOrderRefund(orderRefund);
        refund.setState(ETrainTicketRefundState.RS_CHECKING_REFUND);
        refund.setPayload(new TicketRefund());

        var blankRefund = new PassengerRefundInfo();
        blankRefund.setRefundReferenceId("refundItem1");
        blankRefund.setBlankId(20001);
        blankRefund.setCalculatedRefundTicketAmount(Money.of(150, ProtoCurrencyUnit.RUB));
        refund.getPayload().setItems(List.of(blankRefund));
        return refund;
    }
}
