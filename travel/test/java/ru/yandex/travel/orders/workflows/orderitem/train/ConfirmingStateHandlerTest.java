package ru.yandex.travel.orders.workflows.orderitem.train;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.services.DeduplicationService;
import ru.yandex.travel.orders.services.train.ImClientProvider;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflow.orderitem.train.proto.TConfirmationCommit;
import ru.yandex.travel.orders.workflows.orderitem.train.handlers.ConfirmingStateHandler;
import ru.yandex.travel.train.partners.im.ImClient;
import ru.yandex.travel.train.partners.im.ImClientIOException;
import ru.yandex.travel.train.partners.im.model.ReservationConfirmResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

@SuppressWarnings("FieldCanBeLocal")
public class ConfirmingStateHandlerTest {

    private ImClient imClient;
    private ImClientProvider imClientProvider;
    private DeduplicationService deduplicationService;
    private ConfirmingStateHandler handler;
    private TrainWorkflowProperties trainWorkflowProperties;

    @Before
    public void setUp() {
        imClient = mock(ImClient.class);
        imClientProvider = mock(ImClientProvider.class);
        when(imClientProvider.getImClientForOrderItem(any())).thenReturn(imClient);
        deduplicationService = mock(DeduplicationService.class);
        trainWorkflowProperties = new TrainWorkflowProperties();
        trainWorkflowProperties.setCheckConfirmationTryDelay(List.of(Duration.ofSeconds(2)));
        handler = new ConfirmingStateHandler(deduplicationService, imClientProvider, trainWorkflowProperties);
    }

    @Test
    public void testOrderConfirmed() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_RESERVED);
        var trainOrderItem = factory.createTrainOrderItem();
        var response = new ReservationConfirmResponse();
        when(imClient.reservationConfirm(anyInt())).thenReturn(response);

        var ctx = testMessagingContext(trainOrderItem);
        var dKey = UUID.randomUUID();
        handler.handleEvent(TConfirmationCommit.newBuilder().setDeduplicationKey(dKey.toString()).build(), ctx);

        assertThat(trainOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_CHECKING_CONFIRMATION_TRAINS);
        verify(imClient).reservationConfirm(trainOrderItem.getPayload().getPartnerOrderId());
        verify(deduplicationService).registerAtMostOnceCall(dKey);
    }

    @Test
    public void testOrderConfirmedWithError() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_RESERVED);
        var trainOrderItem = factory.createTrainOrderItem();
        when(imClient.reservationConfirm(anyInt())).thenThrow(new ImClientIOException("error"));

        var ctx = testMessagingContext(trainOrderItem);
        var dKey = UUID.randomUUID();
        handler.handleEvent(TConfirmationCommit.newBuilder().setDeduplicationKey(dKey.toString()).build(), ctx);

        assertThat(trainOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_CHECKING_CONFIRMATION_TRAINS);
        verify(imClient).reservationConfirm(trainOrderItem.getPayload().getPartnerOrderId());
        verify(deduplicationService).registerAtMostOnceCall(dKey);
    }

    @Test
    public void testDeduplicationError() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_RESERVED);
        var trainOrderItem = factory.createTrainOrderItem();
        doThrow(new IllegalStateException()).when(deduplicationService).registerAtMostOnceCall(any());

        var ctx = testMessagingContext(trainOrderItem);
        var dKey = UUID.randomUUID();
        handler.handleEvent(TConfirmationCommit.newBuilder().setDeduplicationKey(dKey.toString()).build(), ctx);

        assertThat(trainOrderItem.getItemState()).isEqualTo(EOrderItemState.IS_CHECKING_CONFIRMATION_TRAINS);
        verify(imClient, never()).reservationConfirm(anyInt());
        verify(deduplicationService).registerAtMostOnceCall(dKey);
    }
}
