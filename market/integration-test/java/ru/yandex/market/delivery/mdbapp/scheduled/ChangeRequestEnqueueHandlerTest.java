package ru.yandex.market.delivery.mdbapp.scheduled;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import steps.orderSteps.OrderEventSteps;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.parcel.cancel.ChangeRequestCancelParcelDto;
import ru.yandex.market.delivery.mdbapp.configuration.queue.ChangeRequestCancelParcelQueue;
import ru.yandex.market.delivery.mdbapp.configuration.queue.ChangeRequestUpdateOrderItemsQueue;
import ru.yandex.market.delivery.mdbapp.configuration.queue.UpdateDsDeliveryDateQueue;
import ru.yandex.market.delivery.mdbapp.configuration.queue.UpdateLastMileQueueConfiguration;
import ru.yandex.market.delivery.mdbapp.configuration.queue.UpdateRecipientQueue;
import ru.yandex.market.delivery.mdbapp.integration.service.ChangeRequestEnqueueHandler;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;

public class ChangeRequestEnqueueHandlerTest extends MockContextualTest {

    @Autowired
    @Qualifier(UpdateDsDeliveryDateQueue.QUEUE_PRODUCER)
    private QueueProducer updateDsDeliveryDateQueueProducer;
    @Autowired
    @Qualifier(UpdateRecipientQueue.UPDATE_RECIPIENT_QUEUE_PRODUCER)
    private QueueProducer updateRecipientQueueProducer;
    @SpyBean
    @Qualifier(ChangeRequestCancelParcelQueue.QUEUE_PRODUCER)
    private QueueProducer cancellationProducer;
    @SpyBean
    @Qualifier(ChangeRequestUpdateOrderItemsQueue.QUEUE_PRODUCER)
    private QueueProducer updateOrderItemsProducer;
    @SpyBean
    @Qualifier(UpdateLastMileQueueConfiguration.PRODUCER)
    private QueueProducer updateLastMileQueueProducer;
    @Captor
    private ArgumentCaptor<EnqueueParams> cancelRequestCaptor;

    private ChangeRequestEnqueueHandler changeRequestEnqueueHandler;


    @Before
    public void setup() {
        changeRequestEnqueueHandler = new ChangeRequestEnqueueHandler(
            updateDsDeliveryDateQueueProducer,
            updateRecipientQueueProducer,
            cancellationProducer,
            updateOrderItemsProducer,
            updateLastMileQueueProducer
        );
    }

    /**
     * Проверка постановки в очередь на отмену, если пришел change-request.
     */
    @Test
    public void enqueueCancellationChangeRequestTest() {
        Mockito.doReturn(1L)
            .when(cancellationProducer).enqueue(cancelRequestCaptor.capture());

        changeRequestEnqueueHandler.handle(OrderEventSteps.createInternalCancelChangeRequest());

        Assert.assertEquals(
            new ChangeRequestCancelParcelDto(1L, 5L, null, 1L, 51L),
            cancelRequestCaptor.getValue().getPayload()
        );
    }
}
