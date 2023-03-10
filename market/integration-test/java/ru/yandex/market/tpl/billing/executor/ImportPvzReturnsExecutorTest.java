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
import ru.yandex.market.tpl.billing.queue.calculatepvzreturnsfee.CalculatePvzReturnsFeeProducer;
import ru.yandex.market.tpl.billing.queue.model.DatePayload;
import ru.yandex.market.tpl.billing.queue.model.QueueType;
import ru.yandex.market.tpl.billing.service.PvzReturnImportService;
import ru.yandex.market.tpl.billing.task.executor.ImportPvzReturnsExecutor;
import ru.yandex.market.tpl.billing.task.executor.TplBillingExecutor;
import ru.yandex.market.tpl.billing.util.DateTimeUtil;
import ru.yandex.market.tpl.billing.utils.PvzModelFactory;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ImportPvzReturnsExecutorTest extends AbstractFunctionalTest {

    @Autowired
    private PvzReturnImportService pvzReturnImportService;
    @Autowired
    private TestableClock clock;
    @Autowired
    private CalculatePvzReturnsFeeProducer calculatePvzReturnsFeeProducer;
    private TplBillingExecutor importPvzReturnsExecutor;
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
        importPvzReturnsExecutor = new ImportPvzReturnsExecutor(
                pvzReturnImportService,
                clock,
                calculatePvzReturnsFeeProducer,
                createRetryTemplate()
        );
    }

    @Test
    @DbUnitDataSet(
            before = "/database/executor/importpvzreturns/before/one_return.csv",
            after = "/database/executor/importpvzreturns/after/single_return.csv")
    @DisplayName("???????????? CALCULATE_PVZ_ORDERS_FEE ???????????????????? ?? ??????????????")
    void singleReturn() {
        when(pvzClient.getDispatchedReturns(yesterday, yesterday)).thenReturn(
                List.of(PvzModelFactory.getReturn(OffsetDateTime.now(clock)))
        );
        importPvzReturnsExecutor.doJob();
        queueTaskChecker.assertQueueTaskCreated(
                QueueType.CALCULATE_PVZ_RETURNS_FEE,
                new DatePayload(REQUEST_ID, yesterday)
        );
    }

    @Test
    void testRetry() {
        var mockPvzReturnsImportService = mock(PvzReturnImportService.class);
        var mockCalculatePvzReturnFreeProducer = mock(CalculatePvzReturnsFeeProducer.class);
        var executor = new ImportPvzReturnsExecutor(
                mockPvzReturnsImportService,
                clock,
                mockCalculatePvzReturnFreeProducer,
                createRetryTemplate()
        );
        doThrow(RuntimeException.class)
                .when(mockCalculatePvzReturnFreeProducer)
                .produce(yesterday);
        try {
            executor.doJob();
        } catch (final RuntimeException ignore) {
        }
        verify(mockPvzReturnsImportService, times(3))
                .importDispatchedReturns(yesterday, yesterday);
    }
}
