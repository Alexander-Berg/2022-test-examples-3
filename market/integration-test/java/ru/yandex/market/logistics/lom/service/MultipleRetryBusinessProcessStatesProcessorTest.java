package ru.yandex.market.logistics.lom.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateStatusHistoryYdb;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateYdb;
import ru.yandex.market.logistics.lom.jobs.consumer.MultipleRetryBusinessProcessStatesConsumer;
import ru.yandex.market.logistics.lom.jobs.model.MdsFileIdAuthorPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.service.businessProcess.AbstractBusinessProcessStateYdbServiceTest;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.TestUtils.mockMdsS3ClientDownload;

@DisplayName("Обработка задачи перевыставления процессов")
@DatabaseSetup("/service/multiple_retry/before/prepare.xml")
class MultipleRetryBusinessProcessStatesProcessorTest extends AbstractBusinessProcessStateYdbServiceTest {
    private static final MdsFileIdAuthorPayload PAYLOAD = PayloadFactory.mdsFileIdAuthorPayload(
        1,
        new OrderHistoryEventAuthor().setTvmServiceId(1010L).setYandexUid(BigDecimal.TEN),
        "1001",
        1L
    );

    private static final Task<MdsFileIdAuthorPayload> TASK =
        TaskFactory.createTask(QueueType.MULTIPLE_RETRY_BUSINESS_PROCESS_STATES_VIA_FILE, PAYLOAD);

    private static final Instant NOW_TIME = Instant.parse("2020-05-01T20:30:00Z");

    private static final String REQUEST_ID = "1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1";

    @Autowired
    private MultipleRetryBusinessProcessStatesConsumer consumer;

    @Autowired
    private MdsS3Client mdsS3Client;

    @BeforeEach
    void setUp() {
        clock.setFixed(NOW_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Обработка задачи перевыставления процессов")
    @ExpectedDatabase(
        value = "/service/multiple_retry/after/multiple_retry_task_partial_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void partialSuccess() {
        mockMdsS3ClientDownload(mdsS3Client, "business_process_states/multiple_retry/multiple_retry.xlsx");
        consumer.execute(TASK);

        assertRetryTaskCreated(REQUEST_ID, List.of(1003L));
        assertProcessStatusHistory(List.of(
            new BusinessProcessStateStatusHistoryYdb()
                .setId(1001L)
                .setSequenceId(1001L)
                .setStatus(BusinessProcessStatus.SYNC_PROCESS_SUCCEEDED)
                .setCreated(NOW_TIME)
                .setMessage(
                    "Some processes were not retried. Errors by types: {"
                        + "ALREADY_IN_SUCCESS_STATUS=[1002], "
                        + "ORDER_HAS_DELIVERY_OPTION_CHANGE_REQUEST=[1004], "
                        + "NOT_FOUND=[1005]"
                        + "}"
                )
        ));

        assertSuccessProcessSavedToYdb();
    }

    @Test
    @DisplayName("Перевыставление процессов из YDB через файл")
    @DatabaseSetup("/service/multiple_retry/before/retry_task_from_ydb.xml")
    @ExpectedDatabase(
        value = "/service/multiple_retry/after/retried_from_ydb.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void retryYdbProcessesFromFile() {
        insertProcessesToYdb(
            ImmutableMap.<Long, BusinessProcessStatus>builder()
                .put(1002L, BusinessProcessStatus.ERROR_RESPONSE_PROCESSING_FAILED)
                .put(1003L, BusinessProcessStatus.SUCCESS_RESPONSE_PROCESSING_FAILED)
                .put(1004L, BusinessProcessStatus.QUEUE_TASK_ERROR)
                .put(1005L, BusinessProcessStatus.ERROR_RESPONSE_PROCESSING_FAILED)
                .build()
        );
        clock.setFixed(Instant.parse("2021-08-30T17:12:13.00Z"), ZoneOffset.UTC);

        mockMdsS3ClientDownload(mdsS3Client, "business_process_states/multiple_retry/multiple_retry.xlsx");
        consumer.execute(TASK);

        assertProcessStatusHistory(List.of(
            new BusinessProcessStateStatusHistoryYdb()
                .setId(1001L)
                .setSequenceId(1001L)
                .setStatus(BusinessProcessStatus.SYNC_PROCESS_SUCCEEDED)
                .setCreated(clock.instant())
        ));

        assertRetryTaskCreated(REQUEST_ID, List.of(1002L, 1003L, 1004L, 1005L));
    }

    @Test
    @DisplayName("Ошибка скачивания файла")
    @ExpectedDatabase(
        value = "/service/multiple_retry/after/multiple_retry_task_partial_downloading_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void downloadingError() {
        when(mdsS3Client.download(any(ResourceLocation.class), any(ContentConsumer.class)))
            .thenThrow(new RuntimeException("MDS S3 client exception"));
        consumer.execute(TASK);

        queueTaskChecker.assertNoQueueTasksCreated();

        assertYdbNotContainsProcesses();
    }

    @Test
    @DisplayName("Ошибка парсинга файла")
    @ExpectedDatabase(
        value = "/service/multiple_retry/after/multiple_retry_task_partial_parsing_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void parsingError() {
        mockMdsS3ClientDownload(mdsS3Client, "business_process_states/multiple_retry/multiple_retry.csv");
        consumer.execute(TASK);

        queueTaskChecker.assertNoQueueTasksCreated();

        assertYdbNotContainsProcesses();
    }

    private void assertSuccessProcessSavedToYdb() {
        assertYdbContainsBusinessProcessWithEntities(
            List.of(
                new BusinessProcessStateYdb()
                    .setId(1001L)
                    .setCreated(Instant.parse("2020-05-01T12:00:00Z"))
                    .setUpdated(Instant.parse("2020-05-01T20:00:00Z"))
                    .setQueueType(QueueType.MULTIPLE_RETRY_BUSINESS_PROCESS_STATES_VIA_FILE)
                    .setStatus(BusinessProcessStatus.SYNC_PROCESS_SUCCEEDED)
                    .setSequenceId(1001L)
                    .setMessage(
                        "Some processes were not retried. Errors by types: "
                            + "{ALREADY_IN_SUCCESS_STATUS=[1002], "
                            + "ORDER_HAS_DELIVERY_OPTION_CHANGE_REQUEST=[1004], "
                            + "NOT_FOUND=[1005]}"
                    )
                    .setPayload(
                        "{\"requestId\":\"1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\","
                            + "\"fileId\":1,\"sequenceId\":1001}"
                    )
            ),
            NOW_TIME
        );
    }
}
