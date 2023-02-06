package ru.yandex.market.starter.quartz;

import java.io.IOException;
import java.util.List;

import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerListener;
import org.quartz.listeners.JobListenerSupport;
import org.quartz.listeners.SchedulerListenerSupport;
import org.quartz.simpl.RAMJobStore;
import org.quartz.spi.JobFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.quartz.LocalDataSourceJobStore;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.terminal.CommandExecutor;
import ru.yandex.market.starter.quartz.config.MjQuartzAutoConfiguration;
import ru.yandex.market.starter.quartz.listeners.RemoveUnusedTriggersSchedulerListener;
import ru.yandex.market.starter.quartz.listeners.TmsDisposingSchedulerListener;
import ru.yandex.market.tms.quartz2.logging.JobHistoryService;
import ru.yandex.market.tms.quartz2.logging.JobMonitoringResultCache;
import ru.yandex.market.tms.quartz2.remote.ListJobsCommand;
import ru.yandex.market.tms.quartz2.service.JobLogAnalysisService;
import ru.yandex.market.tms.quartz2.service.JobService;
import ru.yandex.market.tms.quartz2.service.TmsMonitoringService;
import ru.yandex.market.tms.quartz2.spring.config.TmsDataSourceConfig;
import ru.yandex.market.tms.quartz2.util.QrtzLogTableCleaner;

import static org.assertj.core.api.Assertions.assertThat;

public class MjQuartzAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(QuartzTestConfiguration.class)
        .withConfiguration(AutoConfigurations.of(QuartzAutoConfiguration.class, MjQuartzAutoConfiguration.class));

    @Test
    void inMemoryQuartzDefaultConfigurationTest() {
        contextRunner
            .withPropertyValues("spring.quartz.job-store-type=memory")
            .run(context -> {
                    assertThat(context.getEnvironment().getProperty("spring.quartz.scheduler-name"))
                        .isNotNull();

                    commonContextCheck(context);
                    assertThat(context).doesNotHaveBean(TmsDisposingSchedulerListener.class);
                    assertThat(context).doesNotHaveBean(CommandExecutor.class);

                    final Scheduler scheduler = context.getBean(Scheduler.class);

                    assertThat(scheduler.getMetaData().getJobStoreClass()).isEqualTo(RAMJobStore.class);

                }
            );
    }

    @Test
    void removeUnusedTriggerTest() {
        contextRunner
            .withPropertyValues(
                "spring.quartz.job-store-type=memory",
                "mj.quartz.cleanOnStartup=true"
            )
            .run(context -> {
                    assertThat(context.getEnvironment().getProperty("spring.quartz.scheduler-name"))
                        .isNotNull();

                    final RemoveUnusedTriggersSchedulerListener removeUnusedTriggersSchedulerListener =
                        context.getBean(RemoveUnusedTriggersSchedulerListener.class);

                    final Scheduler scheduler = context.getBean(Scheduler.class);

                    assertThat(scheduler.getMetaData().getJobStoreClass()).isEqualTo(RAMJobStore.class);

                    final List<SchedulerListener> schedulerListeners =
                        scheduler.getListenerManager().getSchedulerListeners();

                    assertThat(schedulerListeners).contains(removeUnusedTriggersSchedulerListener);
                }
            );
    }

    @Test
    void jdbcQuartzDefaultConfigurationTest() {
        contextRunner
            .withUserConfiguration(TestDataSourceConfiguration.class)
            .run(context -> {
                    assertThat(context.getEnvironment()
                        .getProperty("spring.quartz.job-store-type")).isEqualTo("jdbc");
                    assertThat(context.getEnvironment()
                        .getProperty("spring.quartz.properties.org.quartz.jobStore.driverDelegateClass")).isNotNull();

                    commonContextCheck(context);
                    assertThat(context).hasSingleBean(TmsDisposingSchedulerListener.class);
                    assertThat(context).doesNotHaveBean(CommandExecutor.class);

                    final Scheduler scheduler = context.getBean(Scheduler.class);

                    assertThat(scheduler.getMetaData().getJobStoreClass()).isEqualTo(LocalDataSourceJobStore.class);

                    final TmsDisposingSchedulerListener tmsDisposingSchedulerListener =
                        context.getBean(TmsDisposingSchedulerListener.class);

                    final List<SchedulerListener> schedulerListeners =
                        scheduler.getListenerManager().getSchedulerListeners();

                    assertThat(schedulerListeners).contains(
                        tmsDisposingSchedulerListener
                    );
                }
            );
    }

    private void commonContextCheck(AssertableApplicationContext context) {
        assertThat(context).doesNotHaveBean(RemoveUnusedTriggersSchedulerListener.class);
        assertThat(context).hasSingleBean(Scheduler.class);
        assertThat(context).hasSingleBean(JobService.class);
        assertThat(context).hasSingleBean(JobHistoryService.class);
        assertThat(context).hasSingleBean(JobLogAnalysisService.class);
        assertThat(context).hasSingleBean(JobMonitoringResultCache.class);
        assertThat(context).hasSingleBean(TmsMonitoringService.class);
        assertThat(context).hasSingleBean(QrtzLogTableCleaner.class);
        assertThat(context).hasSingleBean(JobFactory.class);
    }

    //TODO: test with common datasource and quartz datasource
    @Test
    void customQuartzDataSourceTest() {
        // Context fails without datasource
        contextRunner.run(context -> assertThat(context).hasFailed());

        contextRunner
            .withUserConfiguration(TestCustomQuartzDataSourceConfiguration.class)
            .run(context -> {
                // we doesn't have common datasource
                assertThat(context).doesNotHaveBean(DataSource.class);
                // but we have TmsDataSourceConfig. Quartz get datasource from it and context gets started.
                assertThat(context).hasSingleBean(TmsDataSourceConfig.class);
                assertThat(context).hasNotFailed();
            });
    }

    @Test
    void tmsCommandsTest() {
        contextRunner
            .withPropertyValues(
                "spring.quartz.job-store-type=memory",
                "mj.quartz.tmsCommands.enabled=true"
            ).run(context -> {
                assertThat(context).hasSingleBean(CommandExecutor.class);
                assertThat(context).hasSingleBean(ListJobsCommand.class);
            });
    }

    @Test
    void userDefinedListenersTest() {
        contextRunner
            .withPropertyValues("spring.quartz.job-store-type=memory")
            .withUserConfiguration(UserListenersConfiguration.class)
            .run(context -> {
                    assertThat(context).hasSingleBean(UserListenersConfiguration.UserJobListener.class);
                    assertThat(context).hasSingleBean(UserListenersConfiguration.UserSchedulerListener.class);

                    final Scheduler scheduler = context.getBean(Scheduler.class);

                    final UserListenersConfiguration.UserJobListener userJobListener =
                        context.getBean(UserListenersConfiguration.UserJobListener.class);
                    final UserListenersConfiguration.UserSchedulerListener userSchedulerListener =
                        context.getBean(UserListenersConfiguration.UserSchedulerListener.class);

                    final ListenerManager listenerManager = scheduler.getListenerManager();

                    assertThat(listenerManager.getSchedulerListeners()).contains(userSchedulerListener);
                    assertThat(listenerManager.getJobListeners()).contains(userJobListener);
                }
            );
    }

    static class QuartzTestConfiguration {


        @Bean
        public JdbcTemplate jdbcTemplate() {
            return Mockito.mock(JdbcTemplate.class);
        }

        @Bean
        public TransactionTemplate transactionTemplate() {
            return Mockito.mock(TransactionTemplate.class);
        }
    }

    static class TestDataSourceConfiguration {


        @Bean(destroyMethod = "close")
        EmbeddedPostgres embeddedPostgres() throws IOException {
            return EmbeddedPostgres.builder().start();
        }

        @Bean
        DataSource dataSource(EmbeddedPostgres embeddedPostgres) {
            return embeddedPostgres.getPostgresDatabase();
        }

    }

    static class TestCustomQuartzDataSourceConfiguration {


        @Bean(destroyMethod = "close")
        EmbeddedPostgres embeddedPostgres() throws IOException {
            return EmbeddedPostgres.builder().start();
        }

        @Bean
        TmsDataSourceConfig tmsDataSourceConfig(EmbeddedPostgres embeddedPostgres) {
            return new TmsDataSourceConfig() {

                private final DataSource dataSource = embeddedPostgres.getPostgresDatabase();
                private final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                private final PlatformTransactionManager transactionManager =
                    new DataSourceTransactionManager(dataSource);
                private final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);


                @Override
                public DataSource tmsDataSource() {
                    return dataSource;
                }

                @Override
                public JdbcTemplate tmsJdbcTemplate() {
                    return jdbcTemplate;
                }

                @Override
                public TransactionTemplate tmsTransactionTemplate() {
                    return transactionTemplate;
                }

                @Override
                public PlatformTransactionManager tmsTransactionManager() {
                    return transactionManager;
                }
            };
        }

    }

    static class UserListenersConfiguration {

        static class UserJobListener extends JobListenerSupport {

            @Override
            public String getName() {
                return "userJob";
            }
        }

        static class UserSchedulerListener extends SchedulerListenerSupport {

        }

        @Bean
        UserJobListener userJobListener() {
            return new UserJobListener();
        }

        @Bean
        UserSchedulerListener userSchedulerListener() {
            return new UserSchedulerListener();
        }

    }

}
