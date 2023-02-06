package ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.date.update;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import steps.utils.TestableClock;

import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.date.UpdateOrderDeliveryDateDto;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterOrderService;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;
import ru.yandex.market.delivery.mdbapp.configuration.queue.UpdateCheckouterDeliveryDateQueue;
import ru.yandex.market.delivery.mdbapp.util.DeliveryDateUpdateReason;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DirtiesContext
public class UpdateCheckouterDeliveryDateConsumerTest extends AllMockContextualTest {

    @Qualifier(UpdateCheckouterDeliveryDateQueue.QUEUE_PRODUCER)
    @Autowired
    private QueueProducer<UpdateOrderDeliveryDateDto> queueProducer;

    @MockBean
    private CheckouterOrderService checkouterOrderService;

    @Autowired
    private TaskLifecycleListener taskListener;

    @Autowired
    private TestableClock clock;

    @After
    public void clear() {
        clock.clearFixed();
    }

    @Test
    public void testUpdateDeliveryDateThrowsOrderNotAllowedException() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);

        doThrow(OrderStatusNotAllowedException.class)
            .when(checkouterOrderService).updateDeliveryDate(any(UpdateOrderDeliveryDateDto.class));

        queueProducer.enqueue(EnqueueParams.create(getDto()));
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
    }

    @Test
    public void testUpdateDeliveryDateOldDate() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);

        clock.setFixed(LocalDate.of(2020, 10, 19).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneId.systemDefault());

        queueProducer.enqueue(EnqueueParams.create(getDto()));
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(checkouterOrderService, never()).updateDeliveryDate(any(UpdateOrderDeliveryDateDto.class));
    }

    private UpdateOrderDeliveryDateDto getDto() {
        return new UpdateOrderDeliveryDateDto(
            123L,
            null,
            -1L,
            LocalDate.of(2017, 4, 22),
            LocalDate.of(2020, 10, 11).atStartOfDay(),
            LocalDate.of(2020, 10, 18).atStartOfDay(),
            LocalTime.of(8, 0),
            LocalTime.of(19, 0),
            987L,
            null,
            DeliveryDateUpdateReason.SHIPPING_DELAYED
        );
    }
}
