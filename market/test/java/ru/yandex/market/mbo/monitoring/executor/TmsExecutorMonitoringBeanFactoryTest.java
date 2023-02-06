package ru.yandex.market.mbo.monitoring.executor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 26.12.2018
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TmsExecutorMonitoringBeanFactoryTest.Context.class)
public class TmsExecutorMonitoringBeanFactoryTest {
    private static final int HOURS_AGO = 13;
    private static final int DAYS_AGO = 4;
    private static final int SEQUENTIAL_FAILS = 13;
    private static final int SEQUENTIAL_FAILS_WARNING = 7;
    private static final int FAILED_OF_LAST_RUNS_TO_WARNING = 3;
    private static final int RUNS_TO_ANALYSE = 10;

    @Autowired
    private ApplicationContext context;

    @Test
    public void createsTmsExecutorBean() {
        Object bean = context.getBean("someExecutorMonitoring");
        assertThat(bean).isInstanceOf(TmsExecutorMonitoring.class);
        TmsExecutorMonitoring monitoring = ((TmsExecutorMonitoring) bean);

        assertThat(monitoring.getCheckName()).isEqualTo("my-custom-check");
        assertThat(monitoring.getExecutorName()).isEqualTo("someExecutor");
        assertThat(monitoring.getSequentialFailsToCritical()).isEqualTo(SEQUENTIAL_FAILS);
        assertThat(monitoring.getSequentialFailsToWarning()).isEqualTo(SEQUENTIAL_FAILS_WARNING);
        assertThat(monitoring.getLastRunAgo()).isEqualTo(Duration.ofDays(DAYS_AGO).plusHours(HOURS_AGO));
        assertThat(monitoring.getFailsOfLastRunsToWarning())
            .usingRecursiveComparison()
            .isEqualTo(new TmsExecutorMonitoring.FailsOfLastRuns(FAILED_OF_LAST_RUNS_TO_WARNING, RUNS_TO_ANALYSE));
    }

    @Configuration
    public static class Context {

        @Bean
        public QuartzLogRepository quartzLogRepository() {
            return Mockito.mock(QuartzLogRepository.class);
        }

        @Bean
        @ExecutorMonitoring(checkName = "my-custom-check",
            lastRunHoursAgo = HOURS_AGO,
            lastRunDaysAgo = DAYS_AGO,
            sequentialFailsToCritical = SEQUENTIAL_FAILS,
            sequentialFailsToWarning = SEQUENTIAL_FAILS_WARNING,
            failedOfLastRunsToWarning = @ExecutorMonitoring.FailsOfLastRuns(
                fails = FAILED_OF_LAST_RUNS_TO_WARNING,
                runsToAnalyse = RUNS_TO_ANALYSE)
        )
        public Executor someExecutor() {
            return new Executor() {
                @Override
                public void doJob(JobExecutionContext context) {
                    //
                }
            };
        }

        @Bean
        public static TmsExecutorMonitoringBeanFactory monitoringBeanFactory() {
            return new TmsExecutorMonitoringBeanFactory();
        }

        public interface Executor {
            void doJob(JobExecutionContext context);
        }
    }
}
