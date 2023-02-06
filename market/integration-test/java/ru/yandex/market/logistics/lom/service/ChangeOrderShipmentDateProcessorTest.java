package ru.yandex.market.logistics.lom.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.jobs.consumer.ChangeOrderShipmentDateConsumer;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderShipmentDatePayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;

@DisplayName("Обработка задачи изменения дат отгрузки")
@DatabaseSetup({
    "/controller/admin/order/before/process-change-shipment-dates-orders.xml",
    "/controller/admin/order/before/change-shipment-date-process.xml"
})
class ChangeOrderShipmentDateProcessorTest extends AbstractContextualTest {
    @Autowired
    private ChangeOrderShipmentDateConsumer consumer;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2019-06-12T00:00:00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Дата отгрузки обновлена, обработка заказа продолжена")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/process-change-shipment-date-task-success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() {
        consumer.execute(TaskFactory.createTask(QueueType.CHANGE_ORDER_SHIPMENT_DATE, payload(1)));
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_WAYBILL_CREATE_ORDER,
            PayloadFactory.createWaybillSegmentPayload(1, 1, "1", 1, 1)
        );
    }

    @Test
    @DisplayName("Дата отгрузки не может быть обновлена, т.к. заказ в неподходящем статусе")
    @ExpectedDatabase(
        value = "/controller/admin/order/before/process-change-shipment-dates-orders.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/admin/order/after/process-change-shipment-date-task-unprocessed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unprocessed() {
        consumer.execute(TaskFactory.createTask(QueueType.CHANGE_ORDER_SHIPMENT_DATE, payload(2)));
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Nonnull
    private static ChangeOrderShipmentDatePayload payload(long orderId) {
        return PayloadFactory.changeOrderShipmentDatePayload(
            orderId,
            LocalDate.of(2020, 11, 11),
            new OrderHistoryEventAuthor().setTvmServiceId(1010L).setYandexUid(BigDecimal.TEN),
            "1001",
            1L
        );
    }
}
