package ru.yandex.market.delivery.mdbapp.components.queue.order.changetoondemand;

import java.time.ZonedDateTime;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.delivery.mdbapp.components.queue.order.changetoondemand.confirm.ConfirmOrderChangeToOnDemandRequestEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterOrderService;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.mdbapp.components.queue.order.changetoondemand.OrderChangeToOnDemandRequestQueueConsumer.REASON_MAPPING;

@ExtendWith(MockitoExtension.class)
public class OrderChangeToOnDemandDependingOnReasonTest {

    private static final long CHECKOUTER_ORDER_ID = 123;
    private static final long LOM_CHANGE_ORDER_REQUEST_ID = 456;

    @Mock
    private CheckouterOrderService checkouterOrderService;

    @Mock
    private ConfirmOrderChangeToOnDemandRequestEnqueueService confirmOrderChangeToOnDemandRequestEnqueueService;

    @InjectMocks
    private OrderChangeToOnDemandRequestQueueConsumer consumer;

    @BeforeEach
    void setUp() {
        when(checkouterOrderService.changeOrderToOnDemand(
            eq(CHECKOUTER_ORDER_ID),
            any()
        ))
            .thenReturn(ChangeRequestStatus.APPLIED);
    }

    @DisplayName("Успешное изменение заказа в чекаутере в зависимости от причины от LOM")
    @ParameterizedTest
    @EnumSource(
        value = ChangeOrderRequestReason.class,
        names = {"DELIVERY_SERVICE_PROBLEM", "CALL_COURIER_BY_USER"}
    )
    public void successfullChangeToOnDemandDependingOnReason(ChangeOrderRequestReason reason) {
        consumer.processTask(createTask(reason));

        verify(checkouterOrderService).changeOrderToOnDemand(
            eq(CHECKOUTER_ORDER_ID),
            eq(REASON_MAPPING.getOrDefault(reason, null))
        );

        verify(confirmOrderChangeToOnDemandRequestEnqueueService).enqueue(
            eq(LOM_CHANGE_ORDER_REQUEST_ID),
            eq(true)
        );
    }

    @Nonnull
    private Task<OrderChangeToOnDemandRequestDto> createTask(ChangeOrderRequestReason reason) {
        return new Task<>(
            new QueueShardId("order.changetoondemand"),
            createOrderChangeToOnDemandRequestDto(reason),
            5,
            ZonedDateTime.now(),
            null,
            null
        );
    }

    @Nonnull
    private OrderChangeToOnDemandRequestDto createOrderChangeToOnDemandRequestDto(ChangeOrderRequestReason reason) {
        return new OrderChangeToOnDemandRequestDto(
            CHECKOUTER_ORDER_ID,
            LOM_CHANGE_ORDER_REQUEST_ID,
            reason
        );
    }
}
