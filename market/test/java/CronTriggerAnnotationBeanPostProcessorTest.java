package ru.yandex.market.starter.quartz;

import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import ru.yandex.market.starter.quartz.config.MjQuartzAutoConfiguration;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.market.tms.quartz2.model.PartitionExecutor;
import ru.yandex.market.tms.quartz2.model.RequestRecovery;
import ru.yandex.market.tms.quartz2.model.VerboseExecutor;
import ru.yandex.market.tms.quartz2.spring.CronTrigger;
import ru.yandex.market.tms.quartz2.spring.JobDataEntry;
import ru.yandex.market.tms.quartz2.spring.PartitionedTask;

import static org.assertj.core.api.Assertions.assertThat;

public class CronTriggerAnnotationBeanPostProcessorTest {

    public static final String CRON_EXPRESSION = "30 * * * * ?";
    public static final String OVERRIDE_CRON_EXPRESSION = "40 * * * * ?";
    public static final String DESCRIPTION = "Тестовая задача";
    public static final String OVERRIDE_DESCRIPTION = "Тестовая задача переопределенная";
    public static final int MISFIRE_INSTRUCTION = Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY;
    public static final String JOB_DATA_KEY = "sfdfs";
    public static final String JOB_DATA_VALUE = "yitutewe";
    public static final boolean REQUEST_RECOVERY = true;
    public static final int PARTITION_SIZE = 3;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(MjQuartzAutoConfigurationTest.QuartzTestConfiguration.class)
        .withConfiguration(AutoConfigurations.of(QuartzAutoConfiguration.class, MjQuartzAutoConfiguration.class));


    @Test
    public void executorViaConfigurationTest() {
        contextRunner
            .withPropertyValues(
                "spring.quartz.job-store-type=memory",
                "mj.quartz.annotatedTrigger.cronEnabled=true"
            )
            .withUserConfiguration(ExecutorConfiguration.class)
            .run(ctx -> {
                assertThat(ctx).doesNotHaveBean("executorWithoutTrigger-trigger");
                final CronTriggerImpl trigger = (CronTriggerImpl) ctx.getBean("executorWithTrigger-trigger");

                assertThat(trigger.getCronExpression()).isEqualTo(CRON_EXPRESSION);
                assertThat(trigger.getMisfireInstruction()).isEqualTo(MISFIRE_INSTRUCTION);
                assertThat(trigger.getDescription()).isEqualTo(DESCRIPTION);
                assertThat(trigger.getJobDataMap().get(JOB_DATA_KEY))
                    .isEqualTo(JOB_DATA_VALUE);
//                final JobDetail jobDetail = (JobDetail) trigger.getJobDataMap().get("jobDetail");
//                assertThat(jobDetail.requestsRecovery()).isEqualTo(REQUEST_RECOVERY);
            });
    }

    @Test
    public void executorViaComponentTest() {
        contextRunner
            .withPropertyValues(
                "spring.quartz.job-store-type=memory",
                "mj.quartz.annotatedTrigger.cronEnabled=true"
            )
            .withUserConfiguration(TestExecutor.class)
            .run(ctx -> {
                final String executorName = ctx.getBeanNamesForType(TestExecutor.class)[0];
                final CronTriggerImpl trigger = (CronTriggerImpl) ctx.getBean(executorName + "-trigger");

                assertThat(trigger.getCronExpression()).isEqualTo(CRON_EXPRESSION);
                assertThat(trigger.getMisfireInstruction()).isEqualTo(MISFIRE_INSTRUCTION);
                assertThat(trigger.getDescription()).isEqualTo(DESCRIPTION);
                assertThat(trigger.getJobDataMap().get(JOB_DATA_KEY))
                    .isEqualTo(JOB_DATA_VALUE);
//                final JobDetail jobDetail = (JobDetail) trigger.getJobDataMap().get("jobDetail");
//                assertThat(jobDetail.requestsRecovery()).isEqualTo(REQUEST_RECOVERY);
            });
    }

    @Test
    public void quartzJobBeanViaConfigurationTest() {
        contextRunner
            .withPropertyValues(
                "spring.quartz.job-store-type=memory",
                "mj.quartz.annotatedTrigger.cronEnabled=true"
            )
            .withUserConfiguration(TestQuartzJobConfig.class)
            .run(ctx -> {
                final CronTriggerImpl trigger = (CronTriggerImpl) ctx.getBean("testQuartzJob-trigger");

                assertThat(trigger.getCronExpression()).isEqualTo(OVERRIDE_CRON_EXPRESSION);
                assertThat(trigger.getMisfireInstruction()).isEqualTo(MISFIRE_INSTRUCTION);
                assertThat(trigger.getDescription()).isEqualTo(OVERRIDE_DESCRIPTION);
                assertThat(trigger.getJobDataMap().get(JOB_DATA_KEY))
                    .isEqualTo(JOB_DATA_VALUE);
//                final JobDetail jobDetail = (JobDetail) trigger.getJobDataMap().get("jobDetail");
//                assertThat(jobDetail.requestsRecovery()).isEqualTo(REQUEST_RECOVERY);
            });
    }

    @Test
    public void quartzJobBeanViaComponentTest() {
        contextRunner
            .withPropertyValues(
                "spring.quartz.job-store-type=memory",
                "mj.quartz.annotatedTrigger.cronEnabled=true"
            )
            .withUserConfiguration(TestQuartzJob.class)
            .run(ctx -> {
                final String quartzJobName = ctx.getBeanNamesForType(TestQuartzJob.class)[0];
                final CronTriggerImpl trigger = (CronTriggerImpl) ctx.getBean(quartzJobName + "-trigger");

                assertThat(trigger.getCronExpression()).isEqualTo(CRON_EXPRESSION);
                assertThat(trigger.getMisfireInstruction()).isEqualTo(MISFIRE_INSTRUCTION);
                assertThat(trigger.getDescription()).isEqualTo(DESCRIPTION);
                assertThat(trigger.getJobDataMap().get(JOB_DATA_KEY))
                    .isEqualTo(JOB_DATA_VALUE);
//                final JobDetail jobDetail = (JobDetail) trigger.getJobDataMap().get("jobDetail");
//                assertThat(jobDetail.requestsRecovery()).isEqualTo(REQUEST_RECOVERY);
            });
    }

    @Test
    public void partitionedExecutorTest() {
        contextRunner
            .withPropertyValues(
                "spring.quartz.job-store-type=memory",
                "mj.quartz.annotatedTrigger.cronEnabled=true"
            )
            .withUserConfiguration(PartitionedExecutorConfiguration.class)
            .run(ctx -> {
                final String[] beanNamesForType = ctx.getBeanNamesForType(org.quartz.CronTrigger.class);
                assertThat(beanNamesForType.length).isEqualTo(PARTITION_SIZE);
            });
    }

    private static class ExecutorConfiguration {

        @Bean
        public Executor executorWithoutTrigger() {
            return executionContext -> {
                System.out.println("executorWithoutTrigger");
            };
        }

        @CronTrigger(
            cronExpression = CRON_EXPRESSION,
            description = DESCRIPTION,
            misfireInstruction = MISFIRE_INSTRUCTION,
            requestRecovery = RequestRecovery.TRUE,
            jobData = {
                @JobDataEntry(key = JOB_DATA_KEY, value = JOB_DATA_VALUE)
            }
        )
        @Bean
        public Executor executorWithTrigger() {
            return executionContext -> {
                System.out.println("executorWithoutTrigger");
            };
        }
    }

    private static class TestQuartzJobConfig {

        @CronTrigger(
            cronExpression = OVERRIDE_CRON_EXPRESSION,
            description = OVERRIDE_DESCRIPTION,
            misfireInstruction = MISFIRE_INSTRUCTION,
            requestRecovery = RequestRecovery.TRUE,
            jobData = {
                @JobDataEntry(key = JOB_DATA_KEY, value = JOB_DATA_VALUE)
            }
        )
        @Bean
        public TestQuartzJob testQuartzJob() {
            return new TestQuartzJob();
        }
    }

    @CronTrigger(
        cronExpression = CRON_EXPRESSION,
        description = DESCRIPTION,
        misfireInstruction = MISFIRE_INSTRUCTION,
        requestRecovery = RequestRecovery.TRUE,
        jobData = {
            @JobDataEntry(key = JOB_DATA_KEY, value = JOB_DATA_VALUE)
        }
    )
    private static class TestQuartzJob extends VerboseExecutor {

        @Override
        public void doRealJob(JobExecutionContext context) throws Exception {
            System.out.println("fsdfsd");
        }
    }

    private static class PartitionedExecutorConfiguration {

        @CronTrigger(
            cronExpression = CRON_EXPRESSION,
            description = DESCRIPTION,
            misfireInstruction = MISFIRE_INSTRUCTION,
            requestRecovery = RequestRecovery.TRUE,
            jobData = {
                @JobDataEntry(key = JOB_DATA_KEY, value = JOB_DATA_VALUE)
            }
        )
        @PartitionedTask(size = PARTITION_SIZE+"")
        @Bean
        public PartitionExecutor partitionExecutor() {
            return (executionContext, n) -> {
                System.out.println("executor_" + n);
            };
        }
    }

    @CronTrigger(
        cronExpression = CRON_EXPRESSION,
        description = DESCRIPTION,
        misfireInstruction = MISFIRE_INSTRUCTION,
        requestRecovery = RequestRecovery.TRUE,
        jobData = {
            @JobDataEntry(key = JOB_DATA_KEY, value = JOB_DATA_VALUE)
        }
    )
    private static class TestExecutor implements Executor {

        @Override
        public void doJob(JobExecutionContext context) {

        }
    }
}
