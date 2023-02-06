package ru.yandex.market.tpl.billing.executor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.pvz.client.billing.PvzClient;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.checker.QueueTaskChecker;
import ru.yandex.market.tpl.billing.queue.calculatepvzorderfee.CalculatePvzOrderFeeProducer;
import ru.yandex.market.tpl.billing.queue.model.DatePayload;
import ru.yandex.market.tpl.billing.queue.model.QueueType;
import ru.yandex.market.tpl.billing.service.PvzOrderImportService;
import ru.yandex.market.tpl.billing.task.executor.ImportPvzDeliveredOrdersExecutor;
import ru.yandex.market.tpl.billing.task.executor.TplBillingExecutor;
import ru.yandex.market.tpl.billing.util.DateTimeUtil;
import ru.yandex.market.tpl.billing.utils.PvzModelFactory;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportPvzDeliveredOrdersExecutorTest extends AbstractFunctionalTest {
    private static final long BATCH_SIZE = 20000;
    @Autowired
    private PvzOrderImportService pvzOrderImportService;
    @Autowired
    private TestableClock clock;
    @Autowired
    private CalculatePvzOrderFeeProducer calculatePvzOrderFeeProducer;
    private TplBillingExecutor importPvzDeliveredOrdersExecutor;
    @Autowired
    private PvzClient pvzClient;
    @Autowired
    private QueueTaskChecker queueTaskChecker;
    private final LocalDate yesterday = LocalDate.of(2021, 2, 22);

    private RetryTemplate createRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-02-23T12:00:00Z"), DateTimeUtil.DEFAULT_ZONE_ID);
        importPvzDeliveredOrdersExecutor = new ImportPvzDeliveredOrdersExecutor(
                pvzOrderImportService,
                clock,
                calculatePvzOrderFeeProducer,
                createRetryTemplate()
        );
    }

    @Test
    @DisplayName("Задача CALCULATE_PVZ_ORDERS_FEE поставлена в очередь")
    @DbUnitDataSet(after = "/database/executor/importpvzorders/after/single_order.csv")
    void singleOrder() {
        when(pvzClient.getDeliveredOrders(yesterday, yesterday, BATCH_SIZE, 0L)).thenReturn(
                List.of(PvzModelFactory.order(OffsetDateTime.now(clock)))
        );
        importPvzDeliveredOrdersExecutor.doJob();
        queueTaskChecker.assertQueueTaskCreated(
                QueueType.CALCULATE_PVZ_ORDERS_FEE,
                new DatePayload(REQUEST_ID, yesterday)
        );
    }

    @Test
    void testRetry() {
        var mockPvzOrderImportService = mock(PvzOrderImportService.class);
        var mockCalculatePvzOrderFeeProducer = mock(CalculatePvzOrderFeeProducer.class);
        var executor = new ImportPvzDeliveredOrdersExecutor(
                mockPvzOrderImportService,
                clock,
                mockCalculatePvzOrderFeeProducer,
                createRetryTemplate()
        );
        doThrow(RuntimeException.class)
                .when(mockCalculatePvzOrderFeeProducer)
                .produce(yesterday);
        try {
            executor.doJob();
        } catch (final RuntimeException ignore) {
        }
        verify(mockPvzOrderImportService, times(3))
                .importDeliveredOrders(yesterday, yesterday);
    }
}
