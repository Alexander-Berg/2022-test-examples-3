package ru.yandex.travel.orders.workflows.orderitem.train.ticketrefund;

import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.orders.entities.TrainTicketRefund;
import ru.yandex.travel.orders.services.train.ImClientProvider;
import ru.yandex.travel.orders.workflow.orderitem.train.ticketrefund.proto.ETrainTicketRefundState;
import ru.yandex.travel.orders.workflow.orderitem.train.ticketrefund.proto.TRefundAll;
import ru.yandex.travel.orders.workflow.orderitem.train.ticketrefund.proto.TRefundOne;
import ru.yandex.travel.orders.workflows.orderitem.train.ticketrefund.handlers.RefundingStateHandler;
import ru.yandex.travel.train.model.refund.PassengerRefundInfo;
import ru.yandex.travel.train.model.refund.TicketRefund;
import ru.yandex.travel.train.partners.im.ImClient;
import ru.yandex.travel.train.partners.im.model.AutoReturnRequest;
import ru.yandex.travel.train.partners.im.model.AutoReturnResponse;
import ru.yandex.travel.train.partners.im.model.RailwayAutoReturnRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

@SuppressWarnings("FieldCanBeLocal")
public class RefundingStateHandlerTest {
    private RefundingStateHandler handler;
    private ImClient imClient;
    private ImClientProvider imClientProvider;

    @Before
    public void setUp() {
        imClient = mock(ImClient.class);
        imClientProvider = mock(ImClientProvider.class);
        when(imClientProvider.getImClientForOrderItem(any())).thenReturn(imClient);
        handler = new RefundingStateHandler(imClientProvider);
    }

    @Test
    public void testRefundOne() {
        when(imClient.autoReturn(any())).thenReturn(new AutoReturnResponse());
        TrainTicketRefund refund = createRefund();
        var ctx = testMessagingContext(refund);
        var imReq1 = new AutoReturnRequest();
        imReq1.setServiceAutoReturnRequest(new RailwayAutoReturnRequest());
        imReq1.getServiceAutoReturnRequest().setOrderItemBlankIds(List.of(555111));
        imReq1.getServiceAutoReturnRequest().setAgentReferenceId("11-2222-3333-444444444444:555111");
        imReq1.getServiceAutoReturnRequest().setOrderItemId(7777777);
        imReq1.getServiceAutoReturnRequest().setCheckDocumentNumber("123456789");
        var imReq2 = new AutoReturnRequest();
        imReq2.setServiceAutoReturnRequest(new RailwayAutoReturnRequest());
        imReq2.getServiceAutoReturnRequest().setOrderItemBlankIds(List.of(555222));
        imReq2.getServiceAutoReturnRequest().setAgentReferenceId("11-2222-3333-444444444444:555222");
        imReq2.getServiceAutoReturnRequest().setOrderItemId(7777777);
        imReq2.getServiceAutoReturnRequest().setCheckDocumentNumber("123456789");

        handler.handleEvent(TRefundOne.newBuilder().build(), ctx);

        assertThat(refund.getState()).isEqualTo(ETrainTicketRefundState.RS_REFUNDING);
        assertThat(refund.getPayload().getItems().get(0).getRefundReferenceId())
                .isEqualTo("11-2222-3333-444444444444:555111");
        assertThat(refund.getPayload().getItems().get(1).getRefundReferenceId()).isNull();

        handler.handleEvent(TRefundOne.newBuilder().build(), ctx);

        assertThat(refund.getState()).isEqualTo(ETrainTicketRefundState.RS_CHECKING_REFUND);
        assertThat(refund.getPayload().getItems().get(1).getRefundReferenceId())
                .isEqualTo("11-2222-3333-444444444444:555222");
        verify(imClient, times(1)).autoReturn(eq(imReq1));
        verify(imClient, times(1)).autoReturn(eq(imReq2));
    }

    @Test
    public void testRefundAll() {
        when(imClient.autoReturn(any())).thenReturn(new AutoReturnResponse());
        TrainTicketRefund refund = createRefund();
        var ctx = testMessagingContext(refund, 3);
        var imReq = new AutoReturnRequest();
        imReq.setServiceAutoReturnRequest(new RailwayAutoReturnRequest());
        imReq.getServiceAutoReturnRequest().setAgentReferenceId("111-2222-3333-444444444444:all:3");
        imReq.getServiceAutoReturnRequest().setOrderItemId(7777777);
        imReq.getServiceAutoReturnRequest().setCheckDocumentNumber("123456789");

        handler.handleEvent(TRefundAll.newBuilder().build(), ctx);

        assertThat(refund.getState()).isEqualTo(ETrainTicketRefundState.RS_CHECKING_REFUND);
        assertThat(refund.getPayload().getItems().get(0).getRefundReferenceId())
                .isEqualTo("111-2222-3333-444444444444:all:3");
        assertThat(refund.getPayload().getItems().get(1).getRefundReferenceId())
                .isEqualTo("111-2222-3333-444444444444:all:3");
        verify(imClient, times(1)).autoReturn(eq(imReq));
    }

    private TrainTicketRefund createRefund() {
        TrainTicketRefund refund = new TrainTicketRefund();
        refund.setId(UUID.fromString("00000000-1111-2222-3333-444444444444"));
        refund.setState(ETrainTicketRefundState.RS_REFUNDING);
        refund.setPayload(new TicketRefund());

        var item1 = new PassengerRefundInfo();
        item1.setBlankId(555111);
        item1.setBuyOperationId(7777777);
        var item2 = new PassengerRefundInfo();
        item2.setBlankId(555222);
        item2.setBuyOperationId(7777777);

        refund.getPayload().setCheckDocumentNumber("123456789");
        refund.getPayload().setItems(List.of(item1, item2));
        return refund;
    }
}
