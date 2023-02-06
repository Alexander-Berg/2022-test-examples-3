package ru.yandex.market.logistics.lom.service.tracker.listener.implementation;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.lom.dto.queue.LomSegmentCheckpoint;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.service.order.OrderService;

@DatabaseSetup("/service/listener/orderReturned/before/setup.xml")
public class OrderReturnedListenerTest extends AbstractCheckpointListenerTest {
    @Autowired
    private OrderReturnedListener orderReturnedListener;
    @Autowired
    private OrderService orderService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("Успешный переход в статус RETURNING")
    @ExpectedDatabase(
        value = "/service/listener/orderReturned/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successReturning() {
        process(1);
    }

    @Test
    @DisplayName("Успешный переход в статус RETURNED")
    @ExpectedDatabase(
        value = "/service/listener/orderReturned/after/success_returned.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successReturned() {
        process(0);
    }

    @Test
    @DisplayName("Не менять статус, если заказ уже возвращен")
    @DatabaseSetup(
        value = "/service/listener/orderReturned/before/success_already_returned.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/service/listener/orderReturned/before/success_already_returned.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successAlreadyReturned() {
        process(1);
    }

    private void process(int waybillIndex) {
        transactionTemplate.execute(arg -> {
            Order orderById = orderService.getOrderById(1L).orElseThrow();
            orderReturnedListener.process(
                orderById,
                LomSegmentCheckpoint.builder()
                    .segmentStatus(SegmentStatus.RETURN_PREPARING)
                    .trackerCheckpointId(181L)
                    .trackerId(101L)
                    .trackerCheckpointStatus("SORTING_CENTER_RETURN_ORDER_PARTIALLY_RECEIPT_AT_SECONDARY_RECEPTION")
                    .build(),
                orderById.getWaybill().get(waybillIndex),
                null
            );
            return null;
        });
    }
}
