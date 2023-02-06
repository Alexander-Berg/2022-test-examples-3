package ru.yandex.market.core.asyncreport.worker;

import java.text.MessageFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.asyncreport.DisabledAsyncReportService;
import ru.yandex.market.core.asyncreport.PendingReportProvider;
import ru.yandex.market.core.asyncreport.ReportsDao;
import ru.yandex.market.core.asyncreport.ReportsService;
import ru.yandex.market.core.asyncreport.ReportsServiceSettings;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.asyncreport.model.ReportsTypeGroup;
import ru.yandex.market.core.asyncreport.worker.model.ReportResult;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.monlib.metrics.primitives.Rate;
import ru.yandex.monlib.metrics.registry.MetricId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.mbi.web.solomon.pull.SolomonUtils.getMetricRegistry;

/**
 * Тесты для {@link ReportsWorker}.
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "ReportsWorkerTest.common.before.csv")
class ReportsWorkerTest extends FunctionalTest {
    private static final int TIMEOUT = 1000;
    private static final String NEXT_REPORT_ID = "1";
    private static final int REPORTS_QUEUE_LIMIT = 10;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EnvironmentService environmentService;

    private PendingReportProvider<ReportsType> reportProvider;
    private ReportsExecutorSettings<ReportsType, ReportsTypeGroup> settings;
    private ReportsWorker<ReportsType> reportWorker;

    @BeforeEach
    void init() {
        ReportsServiceSettings<ReportsType> reportTypesReportsServiceSettings =
                new ReportsServiceSettings.Builder<ReportsType>()
                        .setReportsQueueLimit(REPORTS_QUEUE_LIMIT)
                        .build();

        reportProvider = new ReportsService<ReportsType>(
                reportTypesReportsServiceSettings,
                new ReportsDao<>(jdbcTemplate, ReportsType.class),
                transactionTemplate,
                () -> NEXT_REPORT_ID,
                Clock.fixed(DateTimes.toInstantAtDefaultTz(2019, 11, 21), ZoneOffset.UTC),
                new DisabledAsyncReportService(jdbcTemplate),
                environmentService
        );
        settings = ReportsExecutorSettings.<ReportsType, ReportsTypeGroup>builder()
                .setReportGenerators(
                        Arrays.asList(
                                new SuccessGenerator(),
                                new FailedGenerator(),
                                new ThrowExceptionGenerator()
                        )
                )
                .setTaskBackOffIntervalMs(1)
                .build();

        reportWorker = new ReportsWorker<>(
                reportProvider,
                settings.getReportGenerators(),
                settings.getTaskBackOffIntervalMs(),
                Clock.fixed(DateTimes.toInstantAtDefaultTz(2019, 10, 17), ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("Проверяет, что воркер получает отчёт, генерит его и отдаёт результат")
    @DbUnitDataSet(
            before = "ReportsWorkerTest.reportGeneration.before.csv",
            after = "ReportsWorkerTest.reportGeneration.after.csv"
    )
    void test_reportGeneration() throws InterruptedException {
        Integer reportCount = 4;
        for (int i = 0; i < reportCount; i++) {
            reportWorker.runWork();
        }

        Integer notNullHosts = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM SHOPS_WEB.ASYNC_REPORTS WHERE HOST IS NOT NULL",
                Integer.class
        );
        Assertions.assertEquals(reportCount, notNullHosts);
        Integer notNullTraceIds = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM SHOPS_WEB.ASYNC_REPORTS WHERE TRACE_ID IS NOT NULL",
                Integer.class
        );
        Assertions.assertEquals(reportCount, notNullTraceIds);
        validateSuccessfulMetricsPushed("STOCKS_ON_WAREHOUSES");
        validateSuccessfulMetricsPushed("DAILY_STOCKS");
    }

    @Test
    @DisplayName("Проверяет, что генератор отчётов кинет ошибку, то воркер продолжит работу")
    @DbUnitDataSet(before = "ReportsWorkerTest.test_reportGenerationThrowException.before.csv")
    void test_reportGenerationThrowException() throws InterruptedException {
        // При обработке ReportsType#ASSORTMENT будет выкинута ошибка, см. ThrowExceptionGenerator
        // Ошибка не должна прервать работу задачи.
        reportWorker.runWork();
    }

    @Test
    @DisplayName("При interrupt воркер должен завершить работу")
    void test_workerInterruption() throws InterruptedException {
        Thread thread = new Thread(reportWorker);
        thread.start();
        thread.interrupt();
        thread.join(TIMEOUT);
        assertFalse(thread.isAlive(), "При interrupt воркер должен завершить работу");
    }

    private void validateSuccessfulMetricsPushed(String reportType) {
        Rate reportDelay = (Rate) getMetricRegistry()
                .getMetric(new MetricId("report_delay_start_execution_rate", Labels.of("report_type", reportType)));
        Rate reportExecutionTime = (Rate) getMetricRegistry()
                .getMetric(new MetricId("report_execution_time_rate", Labels.of("report_type", reportType)));
        Rate reportCount = (Rate) getMetricRegistry()
                .getMetric(new MetricId("report_rate", Labels.of("report_type", reportType)));

        assertNotNull(reportDelay);
        assertTrue(reportDelay.get() >= 0);
        assertNotNull(reportExecutionTime);
        assertNotNull(reportCount);
        assertTrue(reportCount.get() >= 0);
    }

    public static class SuccessGenerator implements ReportGenerator<ReportsType, TestParams> {

        @Nonnull
        @Override
        public ReportResult generate(String reportId, TestParams reportParams) {
            return ReportResult.done(
                    MessageFormat.format("https://y.ru/report_{0}.xlsx", reportId),
                    MessageFormat.format("From {0}, to {1}", reportParams.getFrom(), reportParams.getTo())
            );
        }

        @Nonnull
        @Override
        public ReportsType getType() {
            return ReportsType.DAILY_STOCKS;
        }

        @Nonnull
        @Override
        public Class<TestParams> getParamsType() {
            return TestParams.class;
        }
    }

    public static class FailedGenerator implements ReportGenerator<ReportsType, TestParams> {
        @Override
        @Nonnull
        public ReportResult generate(String reportId, TestParams reportParams) {
            return ReportResult.failed(
                    MessageFormat.format(
                            "Error. Report id {0}; from {1}; to {2}",
                            reportId,
                            reportParams.getFrom(),
                            reportParams.getTo()
                    )
            );
        }

        @Nonnull
        @Override
        public ReportsType getType() {
            return ReportsType.STOCKS_ON_WAREHOUSES;
        }

        @Nonnull
        @Override
        public Class<TestParams> getParamsType() {
            return TestParams.class;
        }
    }

    public static class ThrowExceptionGenerator implements ReportGenerator<ReportsType, TestParams> {
        @Override
        @Nonnull
        public ReportResult generate(String reportId, TestParams reportParams) {
            throw new RuntimeException();
        }

        @Nonnull
        @Override
        public ReportsType getType() {
            return ReportsType.ASSORTMENT;
        }

        @Nonnull
        @Override
        public Class<TestParams> getParamsType() {
            return TestParams.class;
        }
    }

    public static class TestParams {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private Instant from;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private Instant to;

        public Instant getFrom() {
            return from;
        }

        public Instant getTo() {
            return to;
        }
    }
}
