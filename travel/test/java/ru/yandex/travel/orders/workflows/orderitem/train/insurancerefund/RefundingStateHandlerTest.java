package ru.yandex.travel.orders.workflows.orderitem.train.insurancerefund;

import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.orders.entities.TrainInsuranceRefund;
import ru.yandex.travel.orders.services.train.ImClientProvider;
import ru.yandex.travel.orders.workflow.orderitem.train.insurancerefund.proto.ETrainInsuranceRefundState;
import ru.yandex.travel.orders.workflow.orderitem.train.insurancerefund.proto.TRefundOne;
import ru.yandex.travel.orders.workflows.orderitem.train.insurancerefund.handlers.RefundingStateHandler;
import ru.yandex.travel.train.model.refund.InsuranceItemInfo;
import ru.yandex.travel.train.model.refund.InsuranceRefund;
import ru.yandex.travel.train.partners.im.ImClient;
import ru.yandex.travel.train.partners.im.model.insurance.InsuranceReturnResponse;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

@SuppressWarnings("FieldCanBeLocal")
public class RefundingStateHandlerTest {
    private RefundingStateHandler handler;
    private ImClientProvider imClientProvider;
    private ImClient imClient;

    @Before
    public void setUp() {
        imClient = mock(ImClient.class);
        imClientProvider = mock(ImClientProvider.class);
        when(imClientProvider.getImClientForOrderItem(any())).thenReturn(imClient);
        handler = new RefundingStateHandler(imClientProvider);
    }

    @Test
    public void testRefundOne() {
        when(imClient.insuranceReturn(any())).thenReturn(new InsuranceReturnResponse());
        TrainInsuranceRefund refund = createRefund();
        var ctx = testMessagingContext(refund);

        handler.handleEvent(TRefundOne.newBuilder().build(), ctx);

        assertThat(refund.getState()).isEqualTo(ETrainInsuranceRefundState.RS_REFUNDING);
        assertThat(refund.getPayload().getItems().get(0).getRefundReferenceId())
                .isEqualTo("11-2222-3333-444444444444:555111");
        assertThat(refund.getPayload().getItems().get(1).getRefundReferenceId()).isNull();

        handler.handleEvent(TRefundOne.newBuilder().build(), ctx);

        assertThat(refund.getState()).isEqualTo(ETrainInsuranceRefundState.RS_CHECKING_REFUND);
        assertThat(refund.getPayload().getItems().get(1).getRefundReferenceId())
                .isEqualTo("11-2222-3333-444444444444:555222");
        verify(imClient, times(2)).insuranceReturn(any());
    }

    private TrainInsuranceRefund createRefund() {
        TrainInsuranceRefund refund = new TrainInsuranceRefund();
        refund.setId(UUID.fromString("00000000-1111-2222-3333-444444444444"));
        refund.setState(ETrainInsuranceRefundState.RS_REFUNDING);
        refund.setPayload(new InsuranceRefund());

        var item1 = new InsuranceItemInfo();
        item1.setBuyOperationId(555111);
        item1.setBuyOperationStatus(ImOperationStatus.IN_PROCESS);
        var item2 = new InsuranceItemInfo();
        item2.setBuyOperationId(555222);
        item2.setBuyOperationStatus(ImOperationStatus.OK);

        refund.getPayload().setItems(List.of(item1, item2));
        return refund;
    }
}
