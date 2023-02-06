package ru.yandex.travel.orders.workflows.orderitem.train;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.services.train.ImClientProvider;
import ru.yandex.travel.orders.services.train.TrainDiscountService;
import ru.yandex.travel.orders.workflow.order.proto.TServiceCancelled;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.TCancellationCommit;
import ru.yandex.travel.orders.workflows.orderitem.train.handlers.CancellingStateHandler;
import ru.yandex.travel.train.partners.im.ImClient;
import ru.yandex.travel.train.partners.im.ImClientException;
import ru.yandex.travel.train.partners.im.ImClientRetryableException;
import ru.yandex.travel.workflow.exceptions.RetryableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

@SuppressWarnings("FieldCanBeLocal")
public class CancellingStateHandlerTest {
    private CancellingStateHandler handler;
    private TrainOrderItemFactory orderItemFactory;
    private ImClient imClient;
    private ImClientProvider imClientProvider;
    private TrainDiscountService trainDiscountService;

    @Before
    public void setUp() {
        imClient = mock(ImClient.class);
        imClientProvider = mock(ImClientProvider.class);
        when(imClientProvider.getImClientForOrderItem(any())).thenReturn(imClient);
        TrainWorkflowProperties trainWorkflowProperties = new TrainWorkflowProperties();
        trainWorkflowProperties.setCancellationMaxTries(1);
        trainDiscountService = mock(TrainDiscountService.class);

        handler = new CancellingStateHandler(imClientProvider, trainWorkflowProperties, trainDiscountService);

        orderItemFactory = new TrainOrderItemFactory();
        orderItemFactory.setOrderItemState(EOrderItemState.IS_CONFIRMING);
    }

    @Test
    public void testHandleCancellingSuccessfully() {
        TrainOrderItem trainOrderItem = orderItemFactory.createTrainOrderItem();

        var ctx = testMessagingContext(trainOrderItem);
        handler.handleEvent(TCancellationCommit.newBuilder().build(), ctx);

        assertThat(trainOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_CANCELLED);
        verify(imClient).reservationCancel(trainOrderItem.getPayload().getPartnerOrderId());
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceCancelled.class);
    }

    @Test
    public void testHandleCancellingErrorFromIm() {
        TrainOrderItem trainOrderItem = orderItemFactory.createTrainOrderItem();

        doThrow(ImClientException.class).when(imClient).reservationCancel(anyInt());

        var ctx = testMessagingContext(trainOrderItem);
        handler.handleEvent(TCancellationCommit.newBuilder().build(), ctx);

        assertThat(trainOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_CANCELLED);
        verify(imClient).reservationCancel(trainOrderItem.getPayload().getPartnerOrderId());
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceCancelled.class);
    }

    @Test(expected = RetryableException.class)
    public void testHandleCancellingRetryableErrorFromIm() {
        TrainOrderItem trainOrderItem = orderItemFactory.createTrainOrderItem();

        doThrow(ImClientRetryableException.class).when(imClient).reservationCancel(anyInt());

        var ctx = testMessagingContext(trainOrderItem);
        handler.handleEvent(TCancellationCommit.newBuilder().build(), ctx);
    }

    @Test
    public void testHandleCancellingRetryableErrorFromImMaxTries() {
        TrainOrderItem trainOrderItem = orderItemFactory.createTrainOrderItem();

        doThrow(ImClientException.class).when(imClient).reservationCancel(anyInt());

        var ctx = testMessagingContext(trainOrderItem);
        handler.handleEvent(TCancellationCommit.newBuilder().build(), ctx);

        assertThat(trainOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_CANCELLED);
        verify(imClient).reservationCancel(trainOrderItem.getPayload().getPartnerOrderId());
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TServiceCancelled.class);
    }
}
