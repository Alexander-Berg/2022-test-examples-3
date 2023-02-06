package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;

import java.util.UUID;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 23.01.18
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ClassNotFoundTest {
    public static final String FIRST_JOB_ID = "dummy1";
    public static final String CLASS_NOT_FOUND_JOB_ID = "dummyClassNotFound";

    @Autowired
    private PipeTester pipeTester;

    @Test
    public void classNotFoundTest() throws Exception {
        String pipeLaunchId = pipeTester.runPipeToCompletion(classNotFoundPipeline());
        PipeLaunch pipeLaunch = pipeTester.getPipeLaunch(pipeLaunchId);
        pipeLaunch.getJobState(CLASS_NOT_FOUND_JOB_ID).setExecutorClassName("NotExistingClass");
        pipeLaunch.getJobState(CLASS_NOT_FOUND_JOB_ID).setSourceCodeEntityId(
            UUID.fromString("1f6f8c6e-a212-4898-ae2e-d12a43127d7b")
        );

        pipeTester.savePipeLaunch(pipeLaunch);
        pipeTester.triggerJob(pipeLaunchId, FIRST_JOB_ID);
        pipeTester.runScheduledJobsToCompletion();

        JobLaunch jobLaunch = pipeTester.getJobLastLaunch(pipeLaunchId, CLASS_NOT_FOUND_JOB_ID);

        Assert.assertEquals(
            StatusChangeType.FAILED,
            jobLaunch.getLastStatusChange().getType()
        );

        Assert.assertTrue(jobLaunch.getExecutionExceptionStacktrace().startsWith("Class not found"));
        Assert.assertTrue(pipeTester.getPipeLaunch(pipeLaunchId).getJobState(CLASS_NOT_FOUND_JOB_ID).isReadyToRun());
    }

    public Pipeline classNotFoundPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder dummyJob = builder.withJob(DummyJob.class).withId(FIRST_JOB_ID).withManualTrigger();

        JobBuilder dummyJobClassNotFound = builder.withJob(DummyJob.class).withUpstreams(dummyJob)
            .withId(CLASS_NOT_FOUND_JOB_ID)
            .withDescription("Для проверки необходимо в ресурсах поменять класс на несуществующий");

        return builder.build();
    }
}
