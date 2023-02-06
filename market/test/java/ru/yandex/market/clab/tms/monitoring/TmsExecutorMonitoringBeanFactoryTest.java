package ru.yandex.market.clab.tms.monitoring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.tms.quartz2.model.VerboseExecutor;

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
    }

    @Configuration
    public static class Context {
        @MockBean
        private QuartzLogRepository quartzLogRepository;

        @Bean
        @ExecutorMonitoring(checkName = "my-custom-check", lastRunHoursAgo = HOURS_AGO, lastRunDaysAgo = DAYS_AGO,
            sequentialFailsToCritical = SEQUENTIAL_FAILS, sequentialFailsToWarning = SEQUENTIAL_FAILS_WARNING)
        public VerboseExecutor someExecutor() {
            return new VerboseExecutor() {
                @Override
                public void doRealJob(JobExecutionContext context) {
                    //
                }
            };
        }

        @Bean
        public static TmsExecutorMonitoringBeanFactory monitoringBeanFactory() {
            return new TmsExecutorMonitoringBeanFactory();
        }
    }
}
