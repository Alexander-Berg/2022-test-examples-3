package ru.yandex.market.tsum.pipe.engine.runtime.state;

import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.AdapterJobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.common.CanRunWhen;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.stage.StageGroup;
import ru.yandex.market.tsum.pipe.engine.definition.subscriber.PipeSubscriber;
import ru.yandex.market.tsum.pipe.engine.runtime.FullJobLaunchId;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.Notificator;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.CommonJobEvents;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.CommonPipeEvents;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.NotificationEvent;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 06.03.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
public class PipeStateNotificatorTest {
    private static final String FIRST_JOB_ID = "firstJob";
    private static final String FIRST_JOB_TITLE = "Первая джоба";

    private static final String DOWNSTREAM_AUTO_TRIGGER_JOB_ID = "downstreamAutoTriggerJob";
    private static final String DOWNSTREAM_AUTO_TRIGGER_JOB_TITLE = "Запускается сама";

    private static final String MANUAL_TRIGGER_JOB_ID = "manualTriggerJob";
    private static final String MANUAL_TRIGGER_JOB_TITLE = "Джоба с ручным запуском";

    private static final String LAST_LAUNCHED_MANUAL_TRIGGER_JOB_ID = "lastLaunchedJob";
    private static final String LAST_LAUNCHED_MANUAL_TRIGGER_JOB_TITLE = "Не запускающаяся джоба с ручным запуском";

    private static final String ONCE_FAILING_JOB_ID = "onceFailingJob";
    private static final String ONCE_FAILING_JOB_TITLE = "Джоба с фейлом";

    private static final String ALWAYS_FAILING_JOB_ID = "alwaysFailingJob";
    private static final String ALWAYS_FAILING_JOB_TITLE = "Джоба всегда фейлится";

    @Autowired
    private Notificator notificator;

    @Autowired
    private PipeTester pipeTester;

    @Captor
    private ArgumentCaptor<NotificationEvent> notificationEventCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> variablesCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.reset(notificator);
    }

    @Test
    public void notifyAboutEvent() throws InterruptedException {
        String pipeLaunchId = pipeTester.runPipeToCompletion(allCasesPipeline());

        Mockito.verify(notificator, Mockito.times(8)).notifyAboutEvent(
            notificationEventCaptor.capture(),
            variablesCaptor.capture()
        );

        Assert.assertEquals(1, getNotificationsCountByEventId(CommonPipeEvents.PIPELINE_STARTED.getId()));
        Assert.assertEquals(2, getNotificationsCountByEventId(CommonJobEvents.JOB_STARTED.getId()));
        Assert.assertEquals(2, getNotificationsCountByEventId(CommonJobEvents.JOB_SUCCEEDED.getId()));
        Assert.assertEquals(2, getNotificationsCountByEventId(CommonJobEvents.JOB_NEEDS_MANUAL_TRIGGER.getId()));
        Assert.assertEquals(1, getNotificationsCountByEventId(CommonPipeEvents.PIPELINE_NEEDS_MANUAL_TRIGGER.getId()));

        Assert.assertEquals(2, getVariablesCountByJobTitle(FIRST_JOB_TITLE));
        Assert.assertEquals(2, getVariablesCountByJobTitle(DOWNSTREAM_AUTO_TRIGGER_JOB_TITLE));
        Assert.assertEquals(1, getVariablesCountByJobTitle(MANUAL_TRIGGER_JOB_TITLE));
        Assert.assertTrue(hasMentionedInJobTitles(MANUAL_TRIGGER_JOB_TITLE));
        Assert.assertTrue(hasMentionedInJobTitles(LAST_LAUNCHED_MANUAL_TRIGGER_JOB_TITLE));
        Assert.assertEquals(8, getVariablesCountByPipeLaunchId(pipeLaunchId));

        setUp();

        pipeTester.triggerJob(pipeLaunchId, MANUAL_TRIGGER_JOB_ID);
        pipeTester.runScheduledJobsToCompletion();

        Mockito.verify(notificator, Mockito.times(8)).notifyAboutEvent(
            notificationEventCaptor.capture(),
            variablesCaptor.capture()
        );

        Assert.assertEquals(3, getNotificationsCountByEventId(CommonJobEvents.JOB_STARTED.getId()));
        Assert.assertEquals(2, getNotificationsCountByEventId(CommonJobEvents.JOB_SUCCEEDED.getId()));
        Assert.assertEquals(1, getNotificationsCountByEventId(CommonJobEvents.JOB_FAILED.getId()));
        Assert.assertEquals(1, getNotificationsCountByEventId(CommonPipeEvents.PIPELINE_FAILED.getId()));
        Assert.assertEquals(1, getNotificationsCountByEventId(CommonPipeEvents.PIPELINE_STOPPED_ON_FAIL.getId()));

        Assert.assertEquals(2, getVariablesCountByJobTitle(MANUAL_TRIGGER_JOB_TITLE));
        Assert.assertEquals(3, getVariablesCountByJobTitle(ONCE_FAILING_JOB_TITLE));
        Assert.assertTrue(hasMentionedInJobTitles(ONCE_FAILING_JOB_TITLE));
        Assert.assertEquals(8, getVariablesCountByPipeLaunchId(pipeLaunchId));

        setUp();

        pipeTester.triggerJob(pipeLaunchId, ONCE_FAILING_JOB_ID);
        pipeTester.runScheduledJobsToCompletion();

        Mockito.verify(notificator, Mockito.times(6)).notifyAboutEvent(
            notificationEventCaptor.capture(),
            variablesCaptor.capture()
        );

        Assert.assertEquals(2, getNotificationsCountByEventId(CommonJobEvents.JOB_STARTED.getId()));
        Assert.assertEquals(1, getNotificationsCountByEventId(CommonJobEvents.JOB_SUCCEEDED.getId()));
        Assert.assertEquals(1, getNotificationsCountByEventId(CommonJobEvents.JOB_FAILED.getId()));
        Assert.assertEquals(1, getNotificationsCountByEventId(CommonPipeEvents.PIPELINE_STOPPED_ON_FAIL.getId()));
        Assert.assertEquals(1, getNotificationsCountByEventId(CommonPipeEvents.PIPELINE_FAILED.getId()));

        Assert.assertEquals(2, getVariablesCountByJobTitle(ONCE_FAILING_JOB_TITLE));
        Assert.assertEquals(3, getVariablesCountByJobTitle(ALWAYS_FAILING_JOB_TITLE));
        Assert.assertEquals(6, getVariablesCountByPipeLaunchId(pipeLaunchId));

        setUp();

        pipeTester.forceTriggerJob(pipeLaunchId, ALWAYS_FAILING_JOB_ID);
        pipeTester.runScheduledJobsToCompletion();

        Mockito.verify(notificator, Mockito.times(2)).notifyAboutEvent(
            notificationEventCaptor.capture(),
            variablesCaptor.capture()
        );

        Assert.assertEquals(1, getNotificationsCountByEventId(CommonJobEvents.JOB_FORCE_SUCCEEDED.getId()));
        Assert.assertEquals(1, getNotificationsCountByEventId(CommonJobEvents.JOB_SUCCEEDED.getId()));
        Assert.assertEquals(2, getVariablesCountByJobTitle(ALWAYS_FAILING_JOB_TITLE));

    }

    private boolean hasMentionedInJobTitles(String jobTitle) {
        return variablesCaptor.getAllValues().stream()
            .anyMatch(v -> {
                String variable = ((String) v.get("jobTitles"));
                return variable != null && variable.contains(jobTitle);
            });
    }

    private long getNotificationsCountByEventId(String eventId) {
        return notificationEventCaptor.getAllValues().stream()
            .filter(n -> n.getEventMeta().getId().equals(eventId))
            .count();
    }

    private long getVariablesCountByJobTitle(String jobTitle) {
        return variablesCaptor.getAllValues().stream()
            .filter(v -> jobTitle.equals(v.get("jobTitle")) || jobTitle.equals(v.get("failedJobTitle")))
            .count();
    }

    private long getVariablesCountByPipeLaunchId(String pipeLaunchId) {
        return variablesCaptor.getAllValues().stream()
            .filter(v -> {
                String pipeLaunchUrlVariable = ((String) v.get("pipeLaunchUrl"));
                return pipeLaunchUrlVariable != null && pipeLaunchUrlVariable.contains(pipeLaunchId);
            })
            .count();
    }

    private Pipeline allCasesPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withSubscriber(DisablePipeSubscriber.class);

        StageGroup stageGroup = new StageGroup(
            StageBuilder.create("first_stage").uninterruptable(),
            StageBuilder.create("second_stage").uninterruptable()
        );

        JobBuilder firstJob = builder.withJob(DummyJob.class)
            .withId(FIRST_JOB_ID)
            .withTitle(FIRST_JOB_TITLE)
            .beginStage(stageGroup.getStage("first_stage"));

        JobBuilder downstreamAutoTriggerJob = builder.withJob(DummyJob.class)
            .withId(DOWNSTREAM_AUTO_TRIGGER_JOB_ID)
            .withTitle(DOWNSTREAM_AUTO_TRIGGER_JOB_TITLE)
            .withUpstreams(firstJob);

        JobBuilder manualTriggerJob = builder.withJob(DummyJob.class)
            .withId(MANUAL_TRIGGER_JOB_ID)
            .withTitle(MANUAL_TRIGGER_JOB_TITLE)
            .withManualTrigger()
            .withUpstreams(firstJob);

        JobBuilder lastLaunchedManualTriggerJob = builder.withJob(DummyJob.class)
            .withId(LAST_LAUNCHED_MANUAL_TRIGGER_JOB_ID)
            .withTitle(LAST_LAUNCHED_MANUAL_TRIGGER_JOB_TITLE)
            .withManualTrigger()
            .withUpstreams(firstJob);

        AdapterJobBuilder andAdapter = builder.withAdapterJob("and-adapter")
            .withUpstreams(downstreamAutoTriggerJob, manualTriggerJob);

        JobBuilder onceFailingJob = builder.withJob(FailNotFailJob.class)
            .beginStage(stageGroup.getStage("second_stage"))
            .withId(ONCE_FAILING_JOB_ID)
            .withTitle(ONCE_FAILING_JOB_TITLE)
            .withUpstreams(
                CanRunWhen.ANY_COMPLETED, andAdapter, lastLaunchedManualTriggerJob
            );

        JobBuilder alwaysFailJob = builder.withJob(AlwaysFailJob.class, ALWAYS_FAILING_JOB_ID)
            .withTitle(ALWAYS_FAILING_JOB_TITLE)
            .withUpstreams(onceFailingJob);

        return builder.build();
    }

    public static class FailNotFailJob implements JobExecutor {
        private static boolean shouldFail = true;

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("4c5f6add-9259-4cf7-a120-691f57d36d86");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            if (shouldFail) {
                shouldFail = false;
                throw new Exception("Once failing. Trigger me again");
            }
        }
    }

    private static class AlwaysFailJob implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("a335eb05-5304-4676-8555-809e70fcc424");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            throw new RuntimeException();
        }
    }

    public static class DisablePipeSubscriber implements PipeSubscriber {
        @Autowired
        private PipeStateService pipeStateService;

        @Override
        public void jobExecutorHasFinished(FullJobLaunchId fullJobLaunchId, PipeLaunch pipeLaunch) {
            if (fullJobLaunchId.getJobId().equals(LAST_LAUNCHED_MANUAL_TRIGGER_JOB_ID) &&
                pipeLaunch.getJobState(fullJobLaunchId.getJobId()).isExecutorSuccessful()) {

                pipeStateService.disableLaunchGracefully(fullJobLaunchId.getPipeLaunchId(), false);
            }
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("d483a214-fa25-4832-8359-9cad321b6510");
        }
    }
}