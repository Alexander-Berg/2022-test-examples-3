package ru.yandex.market.logistics.lom.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.jobs.consumer.MultipleRecallCourierConsumer;
import ru.yandex.market.logistics.lom.jobs.model.MdsFileIdAuthorPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.lom.validators.enums.ExpressRecallCourierValidationError;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.TestUtils.mockMdsS3ClientDownload;

@DisplayName("Перевызов курьров для заказов через файл")
@DatabaseSetup("/controller/admin/order/before/orders_for_recall.xml")
public class MultipleRecallCourierProcessorTest extends AbstractContextualTest {
    private static final Instant VALID_TIME = Instant.parse("2022-04-20T04:40:00.00Z");

    private static final Set<String> LOG_CODES = Set.of(
        ExpressRecallCourierValidationError.ORDER_HAS_CANCELLATION_REQUEST.name(),
        ExpressRecallCourierValidationError.ORDER_NOT_EXPRESS.name(),
        ExpressRecallCourierValidationError.DATE_NOT_CHANGED_BY_DELIVERY.name(),
        ExpressRecallCourierValidationError.ORDER_NOT_FOUND.name(),
        ExpressRecallCourierValidationError.WAREHOUSE_CLOSED.name()
    );

    private static final MdsFileIdAuthorPayload PAYLOAD = PayloadFactory.mdsFileIdAuthorPayload(
        1,
        new OrderHistoryEventAuthor().setTvmServiceId(1010L).setYandexUid(BigDecimal.TEN),
        "1001",
        1L
    );
    private static final Task<MdsFileIdAuthorPayload> TASK =
        TaskFactory.createTask(QueueType.MULTIPLE_RECALL_COURIER_VIA_FILE, PAYLOAD);

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private MultipleRecallCourierConsumer consumer;

    @BeforeEach
    void setup() {
        clock.setFixed(VALID_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("В файле все заказы")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/all-orders-courier-recall.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void allOrders() {
        mockMdsS3ClientDownload(mdsS3Client, "controller/admin/order/request/recall-courier-all-orders.xlsx");
        consumer.execute(TASK);

        softly.assertThat(backLogCaptor.getResults().stream().anyMatch(line -> line.contains(
                "level=INFO\t"
                    + "format=plain\t"
                    + "code=MULTIPLE_RECALL_COURIER_VIA_FILE\t"
                    + "payload=Business process state is changed with comment: "
                    + "Couriers for some orders were not recalled. "
                    + "Errors by types: {ORDER_NOT_EXPRESS=[2], DATE_NOT_CHANGED_BY_DELIVERY=[4], "
                    + "ORDER_HAS_CANCELLATION_REQUEST=[1], ORDER_NOT_FOUND=[3, 100]}\t"
                    + "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\t"
                    + "tags=BUSINESS_PROCESS_STATE_UPDATE\t"
                    + "extra_keys=sequenceId,entityIds,status\t"
                    + "extra_values=1001,[BusinessProcessStateEntityId(id=1, entityType=MDS_FILE, entityId=1)],"
                    + "SYNC_PROCESS_SUCCEEDED"
            )))
            .isTrue();

        courierRecalled();
    }

    @Test
    @DisplayName("В файле некоторые заказы")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/some-orders-courier-recall.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void someOrders() {
        mockMdsS3ClientDownload(mdsS3Client, "controller/admin/order/request/recall-courier-some-orders.xlsx");
        consumer.execute(TASK);

        softly.assertThat(backLogCaptor.getResults().stream().anyMatch(line -> line.contains(
                "level=INFO\t"
                    + "format=plain\t"
                    + "code=MULTIPLE_RECALL_COURIER_VIA_FILE\t"
                    + "payload=Business process state is changed with comment: "
                    + "Couriers for some orders were not recalled. Errors by types: {DATE_NOT_CHANGED_BY_DELIVERY=[4], "
                    + "ORDER_HAS_CANCELLATION_REQUEST=[1], ORDER_NOT_FOUND=[100]}\t"
                    + "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\t"
                    + "tags=BUSINESS_PROCESS_STATE_UPDATE\t"
                    + "extra_keys=sequenceId,entityIds,status\t"
                    + "extra_values=1001,[BusinessProcessStateEntityId(id=1, entityType=MDS_FILE, entityId=1)],"
                    + "SYNC_PROCESS_SUCCEEDED"
            )))
            .isTrue();

        courierRecalled();
    }

    @Test
    @DisplayName("Ошибка скачивания")
    @ExpectedDatabase(
        value = "/controller/admin/order/after/process-recall-courier-from-file-unprocessed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void unprocessed() {
        when(mdsS3Client.download(any(), any())).thenThrow(new RuntimeException("MDS S3 client exception"));
        consumer.execute(TASK);

        softly.assertThat(LOG_CODES.stream()
            .noneMatch(code -> backLogCaptor.getResults().stream().anyMatch(line -> line.contains(code)))).isTrue();
    }

    private void courierRecalled() {
        softly.assertThat(backLogCaptor.getResults().stream().anyMatch(line -> line.contains(
                "level=INFO\t"
                    + "format=plain\t"
                    + "code=COURIER_RECALLED\t"
                    + "payload=Courier recalled\t"
                    + "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd/1\t"
                    + "tags=COURIER_RECALLED\t"
                    + "entity_types=order,lom_order\t"
                    + "entity_values=order:100500,lom_order:5"
            )))
            .isTrue();
    }
}
