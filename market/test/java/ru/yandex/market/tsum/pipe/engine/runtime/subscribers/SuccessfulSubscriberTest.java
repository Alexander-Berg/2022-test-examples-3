package ru.yandex.market.tsum.pipe.engine.runtime.subscribers;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipe.engine.definition.subscriber.PipeSubscriber;
import ru.yandex.market.tsum.pipe.engine.runtime.FullJobLaunchId;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res1;

import java.util.Collections;
import java.util.UUID;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 04.07.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SuccessfulSubscriberTest {
    private static volatile SubscriberCallInfo subscriberCallInfo = null;

    public static final String JOB_ID = "job";

    @Autowired
    private PipeTester pipeTester;

    @Before
    public void setUp() throws Exception {
        subscriberCallInfo = null;
    }

    @Test
    public void test() throws Exception {
        String resourceValue = "res1";
        String pipeLaunchId = pipeTester.runPipeToCompletion(getPipeline(resourceValue), Collections.emptyList());

        Assert.assertNotNull(subscriberCallInfo);
        Assert.assertEquals(pipeLaunchId, subscriberCallInfo.pipeLaunchId);
        Assert.assertEquals(JOB_ID, subscriberCallInfo.jobId);
        Assert.assertEquals(1, subscriberCallInfo.jobLaunchNumber);
        Assert.assertEquals(resourceValue, subscriberCallInfo.resourceValue);
    }

    private Pipeline getPipeline(String resourceValue) {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(DummyJob.class).withId(JOB_ID);
        builder.withSubscriber(Subscriber.class).withResources(new Res1(resourceValue));
        return builder.build();
    }

    public static class Subscriber implements PipeSubscriber {
        @WiredResource
        Res1 res1;

        @Override
        public void jobExecutorHasFinished(FullJobLaunchId fullJobLaunchId, PipeLaunch pipeLaunch) {
            Assert.assertEquals(fullJobLaunchId.getPipeLaunchId(), pipeLaunch.getId().toString());
            subscriberCallInfo = new SubscriberCallInfo(fullJobLaunchId, res1.getS());
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("81757c53-e434-48e3-bc29-1d73075b5a20");
        }
    }

    static class SubscriberCallInfo {
        private final String pipeLaunchId;
        private final String jobId;
        private final int jobLaunchNumber;
        private final String resourceValue;

        SubscriberCallInfo(FullJobLaunchId fullJobLaunchId, String resourceValue) {
            this.pipeLaunchId = fullJobLaunchId.getPipeLaunchId();
            this.jobId = fullJobLaunchId.getJobId();
            this.jobLaunchNumber = fullJobLaunchId.getJobLaunchNumber();
            this.resourceValue = resourceValue;
        }
    }
}
