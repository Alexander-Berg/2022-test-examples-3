package ru.yandex.market.mbi.tms.monitor;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.core.database.EmbeddedPostgresConfig;
import ru.yandex.market.tms.quartz2.model.Executor;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = ExecutorRegistryTest.Config.class)
class ExecutorRegistryTest extends JupiterDbUnitTest {

    private static final MbiComponent TEST_COMPONENT = MbiComponent.RG;

    private ExecutorRegistry executorRegistry;

    @Autowired
    private ExecutorInfoService executorInfoService;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        executorRegistry = new ExecutorRegistry(executorInfoService);
    }

    @Test
    void onApplicationEvent() {
        executorInfoService.save("obsoleteMonitoredExecutor", MonitoringCriticality.IGNORED, MbiTeam.BILLING);
        executorInfoService.save("monitoredExecutorWithCriticalityChanges", MonitoringCriticality.IGNORED,
                MbiTeam.BILLING);

        executorRegistry.onApplicationEvent(new ContextRefreshedEvent(applicationContext));
        Map<String, ExecutorInfo> activeExecutors = executorInfoService.getActiveExecutors().stream()
                .collect(Collectors.toMap(ExecutorInfo::getExecutorName, Function.identity()));

        assertThat(activeExecutors)
                .doesNotContainKey("obsoleteMonitoredExecutor")
                .containsKey("newMonitoredExecutor")
                .containsEntry("monitoredExecutorWithCriticalityChanges",
                        new ExecutorInfo(TEST_COMPONENT, "monitoredExecutorWithCriticalityChanges",
                                MonitoringCriticality.CRIT_ALWAYS, MbiTeam.BILLING))
                .containsEntry("defaultMonitoredExecutor", new ExecutorInfo(TEST_COMPONENT,
                        "defaultMonitoredExecutor", MonitorFriendly.DEFAULT_CRITICALITY,
                        MonitorFriendly.DEFAULT_TEAM));
    }

    @Configuration
    @Import(EmbeddedPostgresConfig.class)
    static class Config {

        @Autowired
        private TransactionTemplate transactionTemplate;

        @Bean
        @Autowired
        public ExecutorInfoService executorInfoService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
            return new DBExecutorInfoService(
                    TEST_COMPONENT,
                    namedParameterJdbcTemplate,
                    transactionTemplate);
        }

        @Bean
        public Executor defaultMonitoredExecutor() {
            return context -> System.currentTimeMillis();
        }

        @Bean
        public Executor newMonitoredExecutor() {
            return new MonitoredExecutor();
        }

        @Bean
        public Executor monitoredExecutorWithCriticalityChanges() {
            return new MonitoredExecutor();
        }

    }

    static class MonitoredExecutor implements Executor, MonitorFriendly {

        @Override
        public MonitoringCriticality getTaskCriticality() {
            return MonitoringCriticality.CRIT_ALWAYS;
        }

        @Override
        public MbiTeam getAssignedMbiTeam() {
            return MbiTeam.BILLING;
        }

        @Override
        public void doJob(JobExecutionContext context) {
        }
    }
}
