package ru.yandex.market.logistics.lom.service;

import java.math.BigDecimal;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.jobs.consumer.MultipleChangeOrderReturnSegmentViaFileConsumer;
import ru.yandex.market.logistics.lom.jobs.model.MdsFileIdAuthorPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.service.marketid.MarketIdService;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.TestUtils.mockMdsS3ClientDownload;

@DisplayName("Обработка задачи изменения возвратных сегментов заказов")
@DatabaseSetup("/controller/admin/order/before/process-change-order-return-segment-task.xml")
class MultipleChangeOrderReturnSegmentViaFileProcessorTest extends AbstractContextualTest {
    private static final MdsFileIdAuthorPayload PAYLOAD = PayloadFactory.mdsFileIdAuthorPayload(
        1,
        new OrderHistoryEventAuthor().setTvmServiceId(1010L).setYandexUid(BigDecimal.TEN),
        "1001",
        1L
    );

    private static final Task<MdsFileIdAuthorPayload> TASK =
        TaskFactory.createTask(QueueType.MULTIPLE_CHANGE_ORDER_RETURN_SEGMENT_VIA_FILE, PAYLOAD);

    @Autowired
    private MultipleChangeOrderReturnSegmentViaFileConsumer consumer;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private MarketIdService marketIdService;

    @AfterEach
    void tearDown() {
        verify(mdsS3Client).download(any(ResourceLocation.class), any(ContentConsumer.class));
        verifyNoMoreInteractions(lmsClient, marketIdService, mdsS3Client);
    }

    @Test
    @DisplayName("Ошибка скачивания")
    @DatabaseSetup("/controller/admin/order/before/process-change-order-return-segment-orders-create.xml")
    @ExpectedDatabase(
        value = "/controller/admin/order/before/process-change-order-return-segment-orders-create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/admin/order/after/process-change-order-return-segments-unprocessed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unprocessed() {
        when(mdsS3Client.download(
            eq(ResourceLocation.create("lom-doc-test", "1")),
            any(ContentConsumer.class)
        ))
            .thenThrow(new RuntimeException("MDS S3 client exception"));
        consumer.execute(TASK);
    }

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/controller/admin/order/before/process-change-order-return-segment-orders-create.xml")
    @ExpectedDatabase(
        value = "/controller/admin/order/before/process-change-order-return-segment-orders-create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/admin/order/after/process-change-order-return-segment-via-file.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() {
        mockMdsS3ClientDownload(
            mdsS3Client,
            "orders/change_order_return_segments/change_order_return_segments_with_warehouse.xlsx"
        );
        consumer.execute(TASK);
    }
}
