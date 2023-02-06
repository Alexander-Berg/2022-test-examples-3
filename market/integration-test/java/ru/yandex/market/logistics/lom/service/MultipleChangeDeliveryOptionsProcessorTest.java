package ru.yandex.market.logistics.lom.service;

import java.math.BigDecimal;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.jobs.consumer.MultipleChangeDeliveryOptionsConsumer;
import ru.yandex.market.logistics.lom.jobs.model.MdsFileIdAuthorPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.TestUtils.mockMdsS3ClientDownload;

@DisplayName("Обработка задачи изменения опции доставки заказов")
@DatabaseSetup({
    "/controller/admin/change-request/before/create.xml",
    "/controller/admin/change-request/before/process-multiple-change-delivery-option-requests-task.xml",
})
class MultipleChangeDeliveryOptionsProcessorTest extends AbstractContextualTest {
    private static final MdsFileIdAuthorPayload PAYLOAD = PayloadFactory.mdsFileIdAuthorPayload(
        1,
        new OrderHistoryEventAuthor().setTvmServiceId(1010L).setYandexUid(new BigDecimal("19216801")),
        "1001",
        1L
    );

    private static final Task<MdsFileIdAuthorPayload> TASK =
        TaskFactory.createTask(QueueType.MULTIPLE_CHANGE_DELIVERY_OPTIONS_VIA_FILE, PAYLOAD);

    @Autowired
    private MultipleChangeDeliveryOptionsConsumer consumer;

    @Autowired
    private MdsS3Client mdsS3Client;

    @ParameterizedTest
    @ValueSource(strings = {
        "orders/change_delivery_options/change_delivery_options_success.xlsx",
        "orders/change_delivery_options/change_delivery_options_success_column_title_2.xlsx"
    })
    @DisplayName("Полностью успешная обработка файла и создание заявок на изменение опций доставки")
    @ExpectedDatabase(
        value = "/controller/admin/change-request/after/task-success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/admin/change-request/after/create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success(String filepath) {
        mockMdsS3ClientDownload(mdsS3Client, filepath);
        consumer.execute(TASK);
    }

    @Test
    @DisplayName("Частично успешная обработка файла и создание заявок на изменение опций доставки")
    @ExpectedDatabase(
        value = "/controller/admin/change-request/after/task-partial-success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/admin/change-request/after/create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void partialSuccess() {
        mockMdsS3ClientDownload(
            mdsS3Client,
            "orders/change_delivery_options/change_delivery_options_partial_success.xlsx"
        );
        consumer.execute(TASK);
    }

    @Test
    @DisplayName("Ошибка скачивания")
    @ExpectedDatabase(
        value = "/controller/admin/change-request/after/task-error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unprocessed() {
        when(mdsS3Client.download(any(), any())).thenThrow(new RuntimeException("MDS S3 client exception"));
        consumer.execute(TASK);
    }
}
