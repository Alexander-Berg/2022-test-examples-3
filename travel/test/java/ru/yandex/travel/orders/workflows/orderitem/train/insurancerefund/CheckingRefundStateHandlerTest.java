package ru.yandex.travel.orders.workflows.orderitem.train.insurancerefund;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.orders.entities.TrainInsuranceRefund;
import ru.yandex.travel.orders.management.StarTrekService;
import ru.yandex.travel.orders.workflow.orderitem.train.insurancerefund.proto.ETrainInsuranceRefundState;
import ru.yandex.travel.orders.workflow.orderitem.train.insurancerefund.proto.TRefundingSuccess;
import ru.yandex.travel.orders.workflows.orderitem.train.insurancerefund.handlers.CheckingRefundStateHandler;
import ru.yandex.travel.train.model.refund.InsuranceItemInfo;
import ru.yandex.travel.train.model.refund.InsuranceRefund;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationStatus;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationType;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOrderItemType;
import ru.yandex.travel.train.partners.im.model.orderinfo.OrderInfoResponse;
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
        OrderInfoResponse orderInfoResponse = createOrderInfoWithInsuranceRefund();
        TrainInsuranceRefund refund = createRefund();

        var ctx = testMessagingContext(refund);

        handler.handleEvent(TRefundingSuccess.newBuilder()
                .setPartnerResult(ProtoUtils.toTJson(orderInfoResponse)).build(), ctx);

        assertThat(refund.getState()).isEqualTo(ETrainInsuranceRefundState.RS_REFUNDED);
        assertThat(refund.getPayload().getItems().get(0).getRefundOperationStatus()).isEqualTo(ImOperationStatus.OK);
        assertThat(refund.getPayload().getItems().get(0).getRefundOperationId()).isEqualTo(777111);
        assertThat(refund.getPayload().getItems().get(1).getRefundOperationStatus()).isEqualTo(ImOperationStatus.OK);
        assertThat(refund.getPayload().getItems().get(1).getRefundOperationId()).isEqualTo(777222);
    }

    private OrderInfoResponse createOrderInfoWithInsuranceRefund() {
        OrderInfoResponse resp = new OrderInfoResponse();
        OrderItemResponse buyTicketItem = new OrderItemResponse();
        buyTicketItem.setOperationType(ImOperationType.BUY);
        buyTicketItem.setType(ImOrderItemType.RAILWAY);
        buyTicketItem.setSimpleOperationStatus(ImOperationStatus.OK);
        buyTicketItem.setOrderItemBlanks(new ArrayList<>());

        OrderItemResponse buyInsItem1 = new OrderItemResponse();
        buyInsItem1.setOperationType(ImOperationType.BUY);
        buyInsItem1.setType(ImOrderItemType.INSURANCE);
        buyInsItem1.setSimpleOperationStatus(ImOperationStatus.OK);
        buyInsItem1.setAgentReferenceId("");
        buyInsItem1.setOrderItemBlanks(new ArrayList<>());

        OrderItemResponse buyInsItem2 = new OrderItemResponse();
        buyInsItem2.setOperationType(ImOperationType.BUY);
        buyInsItem2.setType(ImOrderItemType.INSURANCE);
        buyInsItem2.setSimpleOperationStatus(ImOperationStatus.OK);
        buyInsItem2.setAgentReferenceId("");
        buyInsItem2.setOrderItemBlanks(new ArrayList<>());

        OrderItemResponse refundInsItem1 = new OrderItemResponse();
        refundInsItem1.setOperationType(ImOperationType.REFUND);
        refundInsItem1.setType(ImOrderItemType.INSURANCE);
        refundInsItem1.setSimpleOperationStatus(ImOperationStatus.OK);
        refundInsItem1.setAgentReferenceId("refundInsItem1");
        refundInsItem1.setOrderItemId(777111);
        refundInsItem1.setOrderItemBlanks(new ArrayList<>());

        OrderItemResponse refundInsItem2 = new OrderItemResponse();
        refundInsItem2.setOperationType(ImOperationType.REFUND);
        refundInsItem2.setType(ImOrderItemType.INSURANCE);
        refundInsItem2.setSimpleOperationStatus(ImOperationStatus.OK);
        refundInsItem2.setAgentReferenceId("refundInsItem2");
        refundInsItem2.setOrderItemId(777222);
        refundInsItem2.setOrderItemBlanks(new ArrayList<>());

        resp.setOrderItems(List.of(buyTicketItem, buyInsItem1, buyInsItem2, refundInsItem1, refundInsItem2));
        return resp;
    }

    private TrainInsuranceRefund createRefund() {
        TrainInsuranceRefund refund = new TrainInsuranceRefund();
        refund.setId(UUID.randomUUID());
        refund.setState(ETrainInsuranceRefundState.RS_CHECKING_REFUND);
        refund.setPayload(new InsuranceRefund());

        var item1 = new InsuranceItemInfo();
        item1.setRefundReferenceId("refundInsItem1");
        var item2 = new InsuranceItemInfo();
        item2.setRefundReferenceId("refundInsItem2");

        refund.getPayload().setItems(List.of(item1, item2));
        return refund;
    }
}
