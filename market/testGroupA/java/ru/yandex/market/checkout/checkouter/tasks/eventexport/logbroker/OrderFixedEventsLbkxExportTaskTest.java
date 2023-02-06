package ru.yandex.market.checkout.checkouter.tasks.eventexport.logbroker;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;

import org.apache.zookeeper.KeeperException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.event.OrderEventPublishService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.tasks.Partition;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author : poluektov
 * date: 2020-08-29.
 */
public class OrderFixedEventsLbkxExportTaskTest extends AbstractWebTestBase {

    @Autowired
    private ZooTask orderFixedEventsLbkxExportTask;
    @Autowired
    private OrderEventPublishService orderEventPublishService;

    private AsyncProducer lbkxAsyncProducer;

    @Test
    public void simpleExportTest() throws Exception {
        ArgumentCaptor<AsyncProducerConfig> configCaptor = initCaptor();

        createOrderWithEvent(BlueParametersProvider.defaultBlueOrderParameters());
        setLastEventId(0L);
        setUploadUntilEventId(10L);
        Long lastEventIdBefore = getLastEventId();
        assertEquals(0L, lastEventIdBefore);
        orderFixedEventsLbkxExportTask.runOnce();
        Long lastExportedId = getLastEventId();
        assertNotEquals(lastEventIdBefore, lastExportedId);

        AsyncProducerConfig config = configCaptor.getValue();
        assertEquals(config.getTopic(), "market-checkout/production/checkouter-order-event-log_data-fix");
    }

    @Nonnull
    private ArgumentCaptor<AsyncProducerConfig> initCaptor() throws InterruptedException {
        ArgumentCaptor<AsyncProducerConfig> configCaptor = ArgumentCaptor.forClass(AsyncProducerConfig.class);
        lbkxAsyncProducer = Mockito.mock(AsyncProducer.class);

        Mockito.when(lbkxClientFactory.asyncProducer(configCaptor.capture()))
                .thenReturn(lbkxAsyncProducer);

        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        when(lbkxAsyncProducer.write(captor.capture(), Mockito.anyLong()))
                .thenReturn(CompletableFuture.completedFuture(new ProducerWriteResponse(1, 1, false)));
        CompletableFuture<ProducerInitResponse> initFuture = CompletableFuture.completedFuture(
                new ProducerInitResponse(Long.MAX_VALUE, "1", 1, "1")
        );
        Mockito.doReturn(initFuture)
                .when(lbkxAsyncProducer).init();
        return configCaptor;
    }

    @Test
    public void testExportWithEventTypes() throws Exception {
        initCaptor();

        createOrderWithEvent(BlueParametersProvider.defaultBlueOrderParameters());

        // экспортируем все события без фильтрации по типам
        setLastEventId(0L);
        setUploadUntilEventId(10L);
        Long lastEventIdBefore = getLastEventId();
        assertEquals(0L, lastEventIdBefore);
        orderFixedEventsLbkxExportTask.runOnce();
        Long lastExportedId = getLastEventId();
        assertNotEquals(lastEventIdBefore, lastExportedId);


        setLastEventId(0L);
        setUploadUntilEventId(10L);

        // устанавливаем фильтр только на определенный вид событий
        orderFixedEventsLbkxExportTask.getZooClient()
                .setOrCreateData(orderFixedEventsLbkxExportTask.getNodePath() + "/eventTypes", "NEW_ORDER");

        orderFixedEventsLbkxExportTask.runOnce();
        Long lastExportedWithFiltering = getLastEventId();
        // в случае с фильтрацией id событий должен быть точно меньше
        assertTrue(lastExportedId > lastExportedWithFiltering);
    }

    private Order createOrderWithEvent(Parameters parameters) {
        Order basicOrder = orderCreateHelper.createOrder(parameters);
        orderEventPublishService.publishEventsBatch(Integer.MAX_VALUE, Partition.of(0, 10));
        orderEventPublishService.publishEventsBatch(Integer.MAX_VALUE, Partition.NULL);
        return basicOrder;
    }

    private Long getLastEventId() throws KeeperException {
        return Long.parseLong(orderFixedEventsLbkxExportTask.getZooClient()
                .getStringData("/checkout/tasks/order-fixed-events-mstat-export-task/lastEventId"));
    }

    private void setLastEventId(Long value) throws KeeperException {
        orderFixedEventsLbkxExportTask.getZooClient()
                .setOrCreateData(orderFixedEventsLbkxExportTask.getNodePath() + "/lastEventId", String.valueOf(value));
    }


    private void setUploadUntilEventId(Long value) throws KeeperException {
        orderFixedEventsLbkxExportTask.getZooClient()
                .setOrCreateData(OrderFixedEventsLbkxExportTaskRunnable.WRITE_UNTIL_EVENT_ID_PATH,
                        String.valueOf(value));
    }
}
