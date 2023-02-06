package ru.yandex.market.delivery.transport_manager.controller.health;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.health.DbQueueMonitoringConfig;

@DbUnitConfiguration(
    databaseConnection = "dbUnitDatabaseConnectionDbQueue"
)
class DbQueueHealthControllerTest extends AbstractContextualTest {

    @Autowired
    private DbQueueHealthController dbQueueHealthController;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setClock() {
        clock.setFixed(Instant.parse("2020-08-07T21:00:00.00Z"), ZoneOffset.UTC);
    }

    @Test
    @DisplayName("Нет упавших задач")
    @DatabaseSetup("/repository/health/dbqueue/has_no_failed_tasks.xml")
    void noFailedJobsSuccessfulTest() {
        softly.assertThat(dbQueueHealthController.failedSync("TRANSPORTATION_CHECKER")).isEqualTo("0;OK");
    }

    @Test
    @DisplayName("Очередь не существует")
    void badInputHandled() {
        softly.assertThat(dbQueueHealthController.failedSync("SOME_NONEXISTENT_QUEUE"))
            .isEqualTo("1;Trying to get dbQueue monitoring for non-existent queue: SOME_NONEXISTENT_QUEUE");
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("testCustomAlertConfigurationsData")
    @DisplayName("Тестирование различных конфигураций алертов")
    @DatabaseSetup("/repository/health/dbqueue/has_retrying_and_failed_tasks.xml")
    void testCustomAlertConfigurations(String caseName, DbQueueMonitoringConfig config, String expectedMessage) {
        if (config != null) {
            jdbcTemplate.execute(String.format(
                "INSERT INTO dbqueue_monitoring_config (queue_name, retrying_jobs_warn_threshold, " +
                    "retrying_jobs_crit_threshold, failed_jobs_warn_threshold, failed_jobs_crit_threshold) VALUES " +
                    "('TRANSPORTATION_CHECKER', %s, %s, %s, %s)",
                config.getRetryingJobsWarnThreshold(), config.getRetryingJobsCritThreshold(),
                config.getFailedJobsWarnThreshold(), config.getFailedJobsCritThreshold()
            ));
        }
        softly.assertThat(dbQueueHealthController.failedSync("TRANSPORTATION_CHECKER"))
            .isEqualTo(expectedMessage);
    }

    private static Stream<Arguments> testCustomAlertConfigurationsData() {
        return Stream.of(
            Arguments.of(
                "Конфигурация чекера не настроена",
                null,
                "1;For queue \"TRANSPORTATION_CHECKER\" some jobs are failed or retrying: Failed - 2 jobs"
            ),
            Arguments.of(
                "Пустая Конфигурация чекера",
                new DbQueueMonitoringConfig(),
                "1;For queue \"TRANSPORTATION_CHECKER\" some jobs are failed or retrying: Failed - 2 jobs"
            ),
            Arguments.of(
                "WARN на ретраи",
                new DbQueueMonitoringConfig().setRetryingJobsWarnThreshold(3),
                "1;For queue \"TRANSPORTATION_CHECKER\" some jobs are failed or retrying: " +
                    "Retrying - 4 jobs, Failed - 2 jobs"
            ),
            Arguments.of(
                "CRIT на ретраи",
                new DbQueueMonitoringConfig().setRetryingJobsCritThreshold(3),
                "2;For queue \"TRANSPORTATION_CHECKER\" some jobs are failed or retrying: Retrying - 4 jobs"
            ),
            Arguments.of(
                "CRIT на фейлы",
                new DbQueueMonitoringConfig().setFailedJobsCritThreshold(1),
                "2;For queue \"TRANSPORTATION_CHECKER\" some jobs are failed or retrying: Failed - 2 jobs"
            ),
            Arguments.of(
                "Отсутствие алертов при \"-1\"-конфигурации",
                new DbQueueMonitoringConfig()
                    .setRetryingJobsWarnThreshold(-1)
                    .setRetryingJobsCritThreshold(-1)
                    .setFailedJobsWarnThreshold(-1)
                    .setFailedJobsCritThreshold(-1),
                "0;OK"
            )
        );
    }
}
