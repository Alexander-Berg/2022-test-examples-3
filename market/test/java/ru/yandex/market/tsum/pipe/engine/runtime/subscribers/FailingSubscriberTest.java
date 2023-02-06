package ru.yandex.market.tsum.pipe.engine.runtime.subscribers;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.subscriber.PipeSubscriber;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.FullJobLaunchId;
import ru.yandex.market.tsum.pipe.engine.runtime.JobLauncher;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 04.07.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FailingSubscriberTest {
    private static AtomicInteger jobExecutionsCount;
    private static AtomicInteger subscriberExecutionsCount;

    public static final String JOB_ID = "job";

    @Autowired
    private PipeTester pipeTester;

    @Autowired
    private JobLauncher jobLauncher;

    @Before
    public void setUp() throws Exception {
        jobExecutionsCount = new AtomicInteger();
        subscriberExecutionsCount = new AtomicInteger();
    }

    @Test
    public void test() throws Exception {
        String pipeLaunchId = pipeTester.runPipeToCompletion(getPipeline(), Collections.emptyList());

        JobLaunch jobLastLaunch = pipeTester.getJobLastLaunch(pipeLaunchId, JOB_ID);
        Assert.assertEquals(StatusChangeType.SUBSCRIBERS_FAILED, jobLastLaunch.getLastStatusChange().getType());

        jobLauncher.launchJob(new FullJobLaunchId(pipeLaunchId, JOB_ID, 1), null);
        jobLastLaunch = pipeTester.getJobLastLaunch(pipeLaunchId, JOB_ID);
        Assert.assertEquals(StatusChangeType.SUCCESSFUL, jobLastLaunch.getLastStatusChange().getType());
    }

    private Pipeline getPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(Job.class).withId(JOB_ID);
        builder.withSubscriber(Subscriber.class);
        return builder.build();
    }

    public static class Job implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("11afb76b-3daf-49f2-a831-a5f92f027dc1");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            jobExecutionsCount.incrementAndGet();
        }
    }

    public static class Subscriber implements PipeSubscriber {
        @Override
        public void jobExecutorHasFinished(FullJobLaunchId fullJobLaunchId, PipeLaunch pipeLaunch) {
            int counter = subscriberExecutionsCount.getAndIncrement();
            if (counter == 0) {
                throw new RuntimeException("I'm failing subscriber!");
            }
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("0b9e6a0d-1dec-4fc4-bcfe-e2fec3be6f5d");
        }
    }

}
