package ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.option;

import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.exception.RedeliveryException;
import ru.yandex.market.delivery.mdbapp.integration.service.RedeliveryService;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.change.DenyChangeOrderDeliveryOptionRequest;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class OrderChangeDeliveryOptionQueueConsumerTest extends AllMockContextualTest {
    private static final long LOM_ORDER_ID = 10433;
    private static final long CHANGE_REQUEST_ID = 42;
    private static final long CHECKOUTER_ORDER_ID = 28340891;
    public static final EnumSet<OptionalOrderPart> PARTS = EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS);

    @Autowired
    private OrderChangeDeliveryOptionQueueConsumer queueConsumer;

    @Autowired
    private OrderChangeDeliveryOptionQueueTransformer queueTransformer;

    @Autowired
    private LomClient lomClient;

    @MockBean
    private RedeliveryService redeliveryService;

    @After
    public void tearDown() {
        verifyNoMoreInteractions(
            lomClient,
            redeliveryService
        );
    }

    @Test
    public void queueTaskDeserializeSuccess() {
        softly.assertThat(queueTransformer.toObject("{\"checkouterOrderId\":7410879}"))
            .usingRecursiveComparison()
            .isEqualTo(OrderChangeDeliveryOptionDto.builder().checkouterOrderId(7410879).build());
    }

    @Test
    public void redeliverySuccess() {
        consumeDbQueueTask();
        verify(redeliveryService).redelivery(CHECKOUTER_ORDER_ID);
    }

    @Test
    public void redeliveryErrorLomOrderNotFound() {
        mockRedeliveryServiceError();
        when(lomClient.getOrder(LOM_ORDER_ID, PARTS)).thenReturn(Optional.empty());
        consumeDbQueueTask();
        verify(redeliveryService).redelivery(CHECKOUTER_ORDER_ID);
        verify(lomClient).getOrder(LOM_ORDER_ID, PARTS);
    }

    @Test
    public void redeliveryErrorLomOrderActiveChangeRequestNotFound() {
        mockRedeliveryServiceError();
        when(lomClient.getOrder(LOM_ORDER_ID, PARTS)).thenReturn(Optional.of(lomOrder().setChangeOrderRequests(null)));
        consumeDbQueueTask();
        verify(redeliveryService).redelivery(CHECKOUTER_ORDER_ID);
        verify(lomClient).getOrder(LOM_ORDER_ID, PARTS);
    }

    @Test
    public void redeliveryErrorLomChangeRequestDenySucceeded() {
        mockRedeliveryServiceError();
        when(lomClient.getOrder(LOM_ORDER_ID, PARTS)).thenReturn(Optional.of(lomOrder()));
        consumeDbQueueTask();
        verify(redeliveryService).redelivery(CHECKOUTER_ORDER_ID);
        verify(lomClient).getOrder(LOM_ORDER_ID, PARTS);
        verify(lomClient).processChangeOrderRequest(
            eq(CHANGE_REQUEST_ID),
            refEq(new DenyChangeOrderDeliveryOptionRequest().setMessage("Cannot perform redelivery"))
        );
    }

    private void mockRedeliveryServiceError() {
        doThrow(new RedeliveryException("Cannot perform redelivery")).when(redeliveryService).redelivery(anyLong());
    }

    private void consumeDbQueueTask() {
        queueConsumer.execute(new Task<>(
            new QueueShardId("order.delivery.option.change"),
            OrderChangeDeliveryOptionDto.builder()
                .lomOrderId(LOM_ORDER_ID)
                .checkouterOrderId(CHECKOUTER_ORDER_ID)
                .build(),
            3,
            ZonedDateTime.now(),
            null,
            null
        ));
    }

    @Nonnull
    private OrderDto lomOrder() {
        return new OrderDto()
            .setId(LOM_ORDER_ID)
            .setStatus(OrderStatus.PROCESSING_ERROR)
            .setChangeOrderRequests(
                List.of(
                    ChangeOrderRequestDto.builder()
                        .id(CHANGE_REQUEST_ID)
                        .requestType(ChangeOrderRequestType.DELIVERY_OPTION)
                        .status(ChangeOrderRequestStatus.PROCESSING)
                        .build()
                )
            );
    }
}
