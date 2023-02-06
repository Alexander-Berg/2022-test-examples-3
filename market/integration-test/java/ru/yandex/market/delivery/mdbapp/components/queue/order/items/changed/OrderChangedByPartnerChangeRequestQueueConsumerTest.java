package ru.yandex.market.delivery.mdbapp.components.queue.order.items.changed;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import steps.utils.TestableClock;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
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
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.order.items.changedbychangerequest.OrderChangedByPartnerChangeRequestDto;
import ru.yandex.market.delivery.mdbapp.components.service.LogisticsOrderService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterOrderService;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;
import ru.yandex.market.delivery.mdbapp.configuration.queue.OrderChangedByPartnerChangeRequestQueueConfiguration;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestPayloadDto;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

public class OrderChangedByPartnerChangeRequestQueueConsumerTest extends AllMockContextualTest {
    private static final EnqueueParams<OrderChangedByPartnerChangeRequestDto> ENQUEUE_PARAMS =
        EnqueueParams.create(getDto());
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ArgumentCaptor<MissingItemsNotification> missingItemsNotificationArgumentCaptor;

    @Qualifier(OrderChangedByPartnerChangeRequestQueueConfiguration.PRODUCER)
    @Autowired
    private QueueProducer<OrderChangedByPartnerChangeRequestDto> queueProducer;

    @MockBean
    private CheckouterOrderService checkouterOrderService;

    @MockBean
    private LogisticsOrderService logisticsOrderService;

    @Autowired
    private LomClient lomClient;

    @Autowired
    private TaskLifecycleListener taskListener;

    @Autowired
    private TestableClock clock;

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
    private static OrderChangedByPartnerChangeRequestDto getDto() {
        return new OrderChangedByPartnerChangeRequestDto(1L, 123L, 234L);
    }

    @After
    public void clear() {
        clock.clearFixed();
    }

    @Before
    public void beforeTest() {
        missingItemsNotificationArgumentCaptor = ArgumentCaptor.forClass(MissingItemsNotification.class);
    }

    @Test
    public void testSendMissingItemsRemoveItems() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
        doReturn(getLomOrder(ChangeOrderRequestStatus.INFO_RECEIVED))
            .when(logisticsOrderService)
            .getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)));

        clock.setFixed(
            LocalDate.of(2020, 10, 19).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneId.systemDefault()
        );

        doReturn(new MissingItemsStrategyDto(MissingItemsStrategyType.REMOVE_ITEMS, null))
            .when(checkouterOrderService).getMissingStrategy(anyLong(), any());

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(checkouterOrderService).getMissingStrategy(
            eq(234L),
            missingItemsNotificationArgumentCaptor.capture()
        );

        softly.assertThat(missingItemsNotificationArgumentCaptor.getValue().isAlreadyRemovedByWarehouse())
            .isEqualTo(true);
        softly.assertThat(missingItemsNotificationArgumentCaptor.getValue().getRemainedItems())
            .containsExactlyInAnyOrderElementsOf(getItemInfo());

        verify(checkouterOrderService).updateOrderMissingItems(
            eq(234L),
            missingItemsNotificationArgumentCaptor.capture()
        );
        softly.assertThat(missingItemsNotificationArgumentCaptor.getValue().isAlreadyRemovedByWarehouse())
            .isEqualTo(true);
        softly.assertThat(missingItemsNotificationArgumentCaptor.getValue().getRemainedItems())
            .containsExactlyInAnyOrderElementsOf(getItemInfo());
    }

    @Test
    public void testSendMissingItemsCancelOrder() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
        OrderDto lomOrder = getLomOrder(ChangeOrderRequestStatus.INFO_RECEIVED);
        doReturn(lomOrder)
            .when(logisticsOrderService).getByIdOrThrow(123L, EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS));

        doReturn(new MissingItemsStrategyDto(MissingItemsStrategyType.CANCEL_ORDER, null))
            .when(checkouterOrderService).getMissingStrategy(anyLong(), any());

        clock.setFixed(
            LocalDate.of(2020, 10, 19).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneId.systemDefault()
        );

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(checkouterOrderService).getMissingStrategy(
            eq(234L),
            missingItemsNotificationArgumentCaptor.capture()
        );

        softly.assertThat(missingItemsNotificationArgumentCaptor.getValue().isAlreadyRemovedByWarehouse())
            .isEqualTo(true);
        softly.assertThat(missingItemsNotificationArgumentCaptor.getValue().getRemainedItems())
            .containsExactlyInAnyOrderElementsOf(getItemInfo());

        verify(logisticsOrderService).processDenyOrderChangedByPartnerRequest(1L);

        verifyNoMoreInteractions(checkouterOrderService);
    }

    @Test
    public void testConfirmSuccess() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
        doReturn(getLomOrder(ChangeOrderRequestStatus.SUCCESS))
            .when(logisticsOrderService)
            .getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)));
        doReturn(getOrder()).when(checkouterOrderService).getOrderWithChangeRequests(eq(234L));

        clock.setFixed(
            LocalDate.of(2020, 10, 19).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneId.systemDefault()
        );

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(checkouterOrderService).applyChangeRequest(eq(234L), eq(1L));
    }

    @Test
    public void testConfirmFail() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
        doReturn(getLomOrder(ChangeOrderRequestStatus.FAIL))
            .when(logisticsOrderService)
            .getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)));
        doReturn(getOrder()).when(checkouterOrderService).getOrderWithChangeRequests(eq(234L));

        clock.setFixed(
            LocalDate.of(2020, 10, 19).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneId.systemDefault()
        );

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(checkouterOrderService).rejectChangeRequest(eq(234L), eq(1L), isNull());
        verify(checkouterOrderService).cancelOrder(234, OrderSubstatus.MISSING_ITEM);
    }

    @Test
    @DisplayName("Checkouter ответил NOTHING_CHANGED для не дропшип заказа")
    public void testNothingChanged() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
        doReturn(getLomOrder(ChangeOrderRequestStatus.INFO_RECEIVED)).when(logisticsOrderService)
            .getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)));

        clock.setFixed(
            LocalDate.of(2020, 10, 19).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneId.systemDefault()
        );

        doReturn(new MissingItemsStrategyDto(MissingItemsStrategyType.NOTHING_CHANGED, null))
            .when(checkouterOrderService).getMissingStrategy(anyLong(), any());

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());

        verify(checkouterOrderService).getMissingStrategy(
            eq(234L),
            missingItemsNotificationArgumentCaptor.capture()
        );
        softly.assertThat(missingItemsNotificationArgumentCaptor.getValue().isAlreadyRemovedByWarehouse())
            .isEqualTo(true);
        softly.assertThat(missingItemsNotificationArgumentCaptor.getValue().getRemainedItems())
            .containsExactlyInAnyOrderElementsOf(getItemInfo());

        verifyNoMoreInteractions(checkouterOrderService);
        verifyZeroInteractions(lomClient);
    }

    @Nonnull
    @SneakyThrows
    private OrderDto getLomOrder(ChangeOrderRequestStatus changeOrderRequestStatus) {
        return new OrderDto()
            .setId(123L)
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
            .setWaybill(List.of(
                WaybillSegmentDto.builder()
                    .partnerType(PartnerType.FULFILLMENT)
                    .build()
            ))
            .setChangeOrderRequests(
                List.of(
                    ChangeOrderRequestDto.builder()
                        .id(1L)
                        .requestType(ChangeOrderRequestType.ORDER_CHANGED_BY_PARTNER)
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
        order.setDropship(false);
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
