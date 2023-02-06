package ru.yandex.market.delivery.mdbapp.components.queue.order.items.notsupplied;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import steps.utils.TestableClock;

import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsNotification;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.ChangedItemDto;
import ru.yandex.market.delivery.mdbapp.components.service.LogisticsOrderService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterOrderService;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;
import ru.yandex.market.delivery.mdbapp.configuration.queue.OrderItemIsNotSuppliedRequestQueueConfiguration;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class OrderItemIsNotSuppliedRequestQueueConsumerTest extends AllMockContextualTest {
    private ArgumentCaptor<MissingItemsNotification> missingItemsNotificationArgumentCaptor;

    @Qualifier(OrderItemIsNotSuppliedRequestQueueConfiguration.PRODUCER)
    @Autowired
    private QueueProducer<OrderItemIsNotSuppliedRequestDto> queueProducer;

    @MockBean
    private CheckouterOrderService checkouterOrderService;

    @MockBean
    private LogisticsOrderService logisticsOrderService;

    @Autowired
    private TaskLifecycleListener taskListener;

    @Autowired
    private TestableClock clock;

    @After
    public void clear() {
        clock.clearFixed();
    }

    @Before
    public void beforeTest() {
        missingItemsNotificationArgumentCaptor = ArgumentCaptor.forClass(MissingItemsNotification.class);
    }

    @Test
    public void testSkipSuccessStatus() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
        doReturn(getLomOrder()).when(logisticsOrderService).getByIdOrThrow(eq(123L), eq(Set.of()));

        clock.setFixed(LocalDate.of(2020, 10, 19).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneId.systemDefault());

        queueProducer.enqueue(EnqueueParams.create(getDto(ChangeOrderRequestStatus.SUCCESS)));
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(checkouterOrderService, never()).updateOrderMissingItems(
            anyLong(),
            any(MissingItemsNotification.class)
        );
    }

    @Test
    public void testSendMissingItems() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
        doReturn(getLomOrder()).when(logisticsOrderService).getByIdOrThrow(eq(123L), eq(Set.of()));

        clock.setFixed(LocalDate.of(2020, 10, 19).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneId.systemDefault());

        queueProducer.enqueue(EnqueueParams.create(getDto(ChangeOrderRequestStatus.CREATED)));
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(checkouterOrderService).updateOrderMissingItems(
            eq(234L),
            missingItemsNotificationArgumentCaptor.capture()
        );
        softly.assertThat(missingItemsNotificationArgumentCaptor.getValue().isAlreadyRemovedByWarehouse())
            .isEqualTo(true);
        softly.assertThat(missingItemsNotificationArgumentCaptor.getValue().getRemainedItems())
            .containsExactly(getItemInfo());
    }

    @Test
    public void testMissingItemsFailStatus() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
        doReturn(getLomOrder()).when(logisticsOrderService).getByIdOrThrow(eq(123L), eq(Set.of()));

        clock.setFixed(LocalDate.of(2020, 10, 19).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneId.systemDefault());

        queueProducer.enqueue(EnqueueParams.create(getDto(ChangeOrderRequestStatus.FAIL)));
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(checkouterOrderService).cancelOrder(234, OrderSubstatus.MISSING_ITEM);
        verify(checkouterOrderService, never()).updateOrderMissingItems(
            anyLong(),
            any(MissingItemsNotification.class)
        );
    }

    @Nonnull
    private OrderItemIsNotSuppliedRequestDto getDto(ChangeOrderRequestStatus status) {
        return new OrderItemIsNotSuppliedRequestDto(
            123L,
            ChangeOrderRequestType.ORDER_ITEM_IS_NOT_SUPPLIED,
            status,
            Set.of(new OrderItemIsNotSuppliedRequestPayloadWrapperDto(
                status,
                new OrderItemIsNotSuppliedRequestPayloadDto(
                    List.of(
                        new ChangedItemDto(
                            456L,
                            "12345",
                            2,
                            "ITEM_IS_NOT_SUPPLIED"
                        )
                    ),
                    "234"
                )
            ))
        );
    }

    @Nonnull
    private OrderDto getLomOrder() {
        return new OrderDto()
            .setId(123L)
            .setItems(List.of(
                ItemDto.builder()
                    .vendorId(456L)
                    .article("12345")
                    .count(3)
                    .build()
            ))
            .setExternalId("234");
    }

    @Nonnull
    private ItemInfo getItemInfo() {
        return new ItemInfo(456L, "12345", 2);
    }
}
