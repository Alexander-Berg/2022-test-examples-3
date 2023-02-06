package ru.yandex.market.delivery.mdbapp.components.queue.order.items.changedbyitemnotfound;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemsRemovalChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsNotification;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.MissingItemsStrategyDto;
import ru.yandex.market.checkout.checkouter.order.item.MissingItemsStrategyType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.service.LogisticsOrderService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterOrderService;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;
import ru.yandex.market.delivery.mdbapp.configuration.queue.OrderChangedByItemNotFoundRequestQueueConfiguration;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestPayloadDto;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyList;
import static org.mockito.BDDMockito.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.times;
import static org.mockito.Mockito.verify;

public class OrderChangedByItemNotFoundRequestQueueConsumerTest extends AllMockContextualTest {

    private static final EnqueueParams<OrderChangedByItemNotFoundRequestDto> ENQUEUE_PARAMS =
        EnqueueParams.create(getDto());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Qualifier(OrderChangedByItemNotFoundRequestQueueConfiguration.PRODUCER)
    @Autowired
    private QueueProducer<OrderChangedByItemNotFoundRequestDto> queueProducer;

    @MockBean
    private CheckouterOrderService checkouterOrderService;

    @MockBean
    private LogisticsOrderService logisticsOrderService;

    @Autowired
    private TaskLifecycleListener taskListener;

    @Nonnull
    public static ChangeRequest createItemsRemovalChangeRequest() {
        return new ChangeRequest(
            1L,
            234L,
            new ItemsRemovalChangeRequestPayload(
                List.of(new OrderItem()),
                List.of(new Parcel())
            ),
            ChangeRequestStatus.PROCESSING,
            Instant.now(),
            "Any text",
            ClientRole.SYSTEM
        );
    }

    @Nonnull
    private static OrderChangedByItemNotFoundRequestDto getDto() {
        return new OrderChangedByItemNotFoundRequestDto(1L, 123L, 1001L);
    }

    @Test
    public void skipChangeRequestWithUnexpectedStatus() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);

        given(logisticsOrderService.getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS))))
            .willReturn(getLomOrder(ChangeOrderRequestStatus.REJECTED));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(checkouterOrderService, never()).getMissingStrategy(anyLong(), any(MissingItemsNotification.class));
    }

    @Test
    public void orderCancellationTest() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);

        given(logisticsOrderService.getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS))))
            .willReturn(getLomOrder(ChangeOrderRequestStatus.INFO_RECEIVED));
        given(checkouterOrderService.getMissingStrategy(eq(1001L), any(MissingItemsNotification.class)))
            .willReturn(new MissingItemsStrategyDto(MissingItemsStrategyType.CANCEL_ORDER, null));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());

        ArgumentCaptor<MissingItemsNotification> captor = ArgumentCaptor.forClass(MissingItemsNotification.class);
        verify(checkouterOrderService).getMissingStrategy(eq(1001L), captor.capture());

        softly.assertThat(captor.getValue().isAlreadyRemovedByWarehouse())
            .isTrue();
        softly.assertThat(captor.getValue().getRemainedItems())
            .containsExactlyInAnyOrderElementsOf(getItemInfo());

        verify(logisticsOrderService, times(1)).cancelOrder(
            eq(123L),
            eq(CancellationOrderReason.MISSING_ITEM)
        );
    }

    @Test
    public void orderUpdateFailed() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);

        given(logisticsOrderService.getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS))))
            .willReturn(getLomOrder(ChangeOrderRequestStatus.TECH_FAIL));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());

        verify(checkouterOrderService).cancelOrder(eq(234L), eq(OrderSubstatus.MISSING_ITEM));
    }

    @Test
    public void orderRemoveItemsTest() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);

        given(logisticsOrderService.getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS))))
            .willReturn(getLomOrder(ChangeOrderRequestStatus.INFO_RECEIVED));
        given(checkouterOrderService.getOrder(eq(1001L)))
            .willReturn(getOrder());

        given(checkouterOrderService.getMissingStrategy(eq(1001L), any(MissingItemsNotification.class)))
            .willReturn(new MissingItemsStrategyDto(
                MissingItemsStrategyType.REMOVE_ITEMS,
                new ItemsRemovalChangeRequestPayload(List.of(), List.of(new Parcel()), null)
            ));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());

        ArgumentCaptor<MissingItemsNotification> captor = ArgumentCaptor.forClass(MissingItemsNotification.class);
        verify(checkouterOrderService).getMissingStrategy(eq(1001L), captor.capture());

        softly.assertThat(captor.getValue().isAlreadyRemovedByWarehouse())
            .isTrue();
        softly.assertThat(captor.getValue().getRemainedItems())
            .containsExactlyInAnyOrderElementsOf(getItemInfo());

        verify(logisticsOrderService, times(1)).updateOrderItems(eq("YND"), any(), anyList());
    }

    @Nonnull
    @SneakyThrows
    private OrderDto getLomOrder(ChangeOrderRequestStatus changeOrderRequestStatus) {
        return new OrderDto()
            .setId(123L)
            .setBarcode("YND")
            .setItems(List.of(
                ItemDto.builder()
                    .vendorId(123L)
                    .article("12345")
                    .count(2)
                    .build(),
                ItemDto.builder()
                    .vendorId(456L)
                    .article("6789")
                    .count(2)
                    .build()
            ))
            .setChangeOrderRequests(
                List.of(
                    ChangeOrderRequestDto.builder()
                        .id(1L)
                        .requestType(ChangeOrderRequestType.ITEM_NOT_FOUND)
                        .status(changeOrderRequestStatus)
                        .payloads(Set.of(
                            ChangeOrderRequestPayloadDto.builder()
                                .status(ChangeOrderRequestStatus.INFO_RECEIVED)
                                .payload(objectMapper.readValue(
                                    "[\n" +
                                        "  {\n" +
                                        "    \"count\": 1,\n" +
                                        "    \"reason\": null,\n" +
                                        "    \"article\": \"12345\",\n" +
                                        "    \"vendorId\": 123\n" +
                                        "  },\n" +
                                        "  {\n" +
                                        "    \"count\": 2,\n" +
                                        "    \"reason\": null,\n" +
                                        "    \"article\": \"6789\",\n" +
                                        "    \"vendorId\": 456\n" +
                                        "  }\n" +
                                        "]",
                                    JsonNode.class
                                ))
                                .build()
                        ))
                        .build()
                )
            )
            .setExternalId("234");
    }

    @Nonnull
    private Order getOrder() {
        Order order = new Order();
        order.setId(234L);
        order.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        Delivery delivery = new Delivery();
        delivery.setPrice(BigDecimal.ONE);
        order.setDelivery(delivery);
        order.setChangeRequests(List.of(
            createItemsRemovalChangeRequest()
        ));
        return order;
    }

    private List<ItemInfo> getItemInfo() {
        return List.of(
            new ItemInfo(123, "12345", 1),
            new ItemInfo(456, "6789", 2)
        );
    }

}
