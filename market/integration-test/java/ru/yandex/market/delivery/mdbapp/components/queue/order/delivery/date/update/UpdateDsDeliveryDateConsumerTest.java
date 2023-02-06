package ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.date.update;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import steps.utils.TestableClock;

import ru.yandex.market.checkout.checkouter.order.changerequest.exception.ChangeRequestException;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.date.UpdateOrderDeliveryDateDto;
import ru.yandex.market.delivery.mdbapp.components.service.LogisticsOrderService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterOrderService;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;
import ru.yandex.market.delivery.mdbapp.configuration.queue.UpdateDsDeliveryDateQueue;
import ru.yandex.market.delivery.mdbapp.util.DeliveryDateUpdateReason;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderDeliveryDateRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.date.UpdateOrderDeliveryDateDto.FAKE_CHANGE_REQUEST_ID;

@DirtiesContext
public class UpdateDsDeliveryDateConsumerTest extends AllMockContextualTest {
    private static final Instant FIXED_TIME = LocalDate.of(2021, 3, 15).atStartOfDay().toInstant(ZoneOffset.UTC);
    private static final Long ORDER_ID = 123L;

    @Qualifier(UpdateDsDeliveryDateQueue.QUEUE_PRODUCER)
    @Autowired
    private QueueProducer<UpdateOrderDeliveryDateDto> queueProducer;

    @MockBean
    private CheckouterOrderService checkouterOrderService;

    @MockBean
    private LogisticsOrderService logisticsOrderService;

    @Autowired
    private TaskLifecycleListener taskListener;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    public void setup() {
        clock.setFixed(FIXED_TIME, ZoneId.systemDefault());
    }

    @Test
    public void testUpdateDeliveryDate() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);

        queueProducer.enqueue(EnqueueParams.create(getDto(DeliveryDateUpdateReason.USER_MOVED_DELIVERY_DATES)));
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());

        ArgumentCaptor<UpdateOrderDeliveryDateRequestDto> captor =
            ArgumentCaptor.forClass(UpdateOrderDeliveryDateRequestDto.class);
        verify(checkouterOrderService).processChangeRequest(eq(ORDER_ID), eq(FAKE_CHANGE_REQUEST_ID));
        verify(logisticsOrderService).updateDeliveryDate(captor.capture());
        softly.assertThat(captor.getValue())
            .isEqualTo(getRequest(ChangeOrderRequestReason.DELIVERY_DATE_UPDATED_BY_USER));
    }

    @Test
    public void testUpdateDeliveryDateUnknownReason() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);

        queueProducer.enqueue(EnqueueParams.create(getDto(DeliveryDateUpdateReason.DELIVERY_SERVICE_DELIVERED)));
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());

        ArgumentCaptor<UpdateOrderDeliveryDateRequestDto> captor =
            ArgumentCaptor.forClass(UpdateOrderDeliveryDateRequestDto.class);
        verify(checkouterOrderService).processChangeRequest(eq(ORDER_ID), eq(FAKE_CHANGE_REQUEST_ID));
        verify(logisticsOrderService).updateDeliveryDate(captor.capture());
        softly.assertThat(captor.getValue()).isEqualTo(getRequest(ChangeOrderRequestReason.UNKNOWN));
    }

    @Test
    public void testUpdateDeliveryDateChangeRequestException() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);

        doThrow(ChangeRequestException.class).when(checkouterOrderService).processChangeRequest(anyLong(), anyLong());

        queueProducer.enqueue(EnqueueParams.create(getDto(DeliveryDateUpdateReason.USER_MOVED_DELIVERY_DATES)));
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());

        verify(checkouterOrderService).processChangeRequest(eq(ORDER_ID), eq(FAKE_CHANGE_REQUEST_ID));
        verifyZeroInteractions(logisticsOrderService);
    }

    private UpdateOrderDeliveryDateDto getDto(DeliveryDateUpdateReason reason) {
        return new UpdateOrderDeliveryDateDto(
            ORDER_ID,
            null,
            FAKE_CHANGE_REQUEST_ID,
            null,
            LocalDate.of(2021, 3, 20).atStartOfDay(),
            LocalDate.of(2021, 3, 21).atStartOfDay(),
            LocalTime.of(10, 0),
            LocalTime.of(18, 0),
            987L,
            null,
            reason
        );
    }

    private UpdateOrderDeliveryDateRequestDto getRequest(ChangeOrderRequestReason reason) {
        return UpdateOrderDeliveryDateRequestDto.builder()
            .barcode(String.valueOf(ORDER_ID))
            .dateMin(LocalDate.of(2021, 3, 20))
            .dateMax(LocalDate.of(2021, 3, 21))
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(18, 0))
            .reason(reason)
            .changeRequestExternalId(FAKE_CHANGE_REQUEST_ID)
            .build();
    }
}
