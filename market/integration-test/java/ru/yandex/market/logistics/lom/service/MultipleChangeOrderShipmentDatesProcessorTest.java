package ru.yandex.market.logistics.lom.service;

import java.math.BigDecimal;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.jobs.consumer.MultipleChangeOrderShipmentDatesConsumer;
import ru.yandex.market.logistics.lom.jobs.model.MdsFileIdAuthorPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.TestUtils.mockMdsS3ClientDownload;

@DisplayName("Обработка задачи изменения дат отгрузки")
@DatabaseSetup({
    "/controller/admin/order/before/process-change-shipment-dates-orders.xml",
    "/controller/admin/order/before/process-change-shipment-dates-task.xml",
})
class MultipleChangeOrderShipmentDatesProcessorTest extends AbstractContextualTest {
    private static final MdsFileIdAuthorPayload PAYLOAD = PayloadFactory.mdsFileIdAuthorPayload(
        1,
        new OrderHistoryEventAuthor().setTvmServiceId(1010L).setYandexUid(BigDecimal.TEN),
        "1001",
        1L
    );

    private static final Task<MdsFileIdAuthorPayload> TASK =
        TaskFactory.createTask(QueueType.MULTIPLE_CHANGE_ORDER_SHIPMENT_DATES_VIA_FILE, PAYLOAD);

    @Autowired
    private MultipleChangeOrderShipmentDatesConsumer consumer;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Test
    @DisplayName("Для всех заказов из файла созданы задачи на обновление")
    @ExpectedDatabase(
        value = "/controller/admin/order/before/process-change-shipment-dates-orders.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/admin/order/after/process-change-shipment-dates-task-success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() {
        mockMdsS3ClientDownload(mdsS3Client, "orders/change_order_shipment_dates/change_shipment_dates_success.xlsx");
        consumer.execute(TASK);
    }

    @Test
    @DisplayName("Для части заказов из файла созданы задачи на обновление")
    @ExpectedDatabase(
        value = "/controller/admin/order/before/process-change-shipment-dates-orders.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/admin/order/after/process-change-shipment-dates-task-partial-success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void partialSuccess() {
        mockMdsS3ClientDownload(
            mdsS3Client,
            "orders/change_order_shipment_dates/change_shipment_dates_partial_success.xlsx"
        );
        consumer.execute(TASK);
    }

    @Test
    @DisplayName("Ошибка скачивания")
    @ExpectedDatabase(
        value = "/controller/admin/order/before/process-change-shipment-dates-orders.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/admin/order/after/process-change-shipment-dates-task-unprocessed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unprocessed() {
        when(mdsS3Client.download(any(), any())).thenThrow(new RuntimeException("MDS S3 client exception"));
        consumer.execute(TASK);
    }
}
