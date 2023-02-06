package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.StoredResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res1;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res2;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res3;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 03.10.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class UpstreamTest {
    @Autowired
    private PipeTester pipeTester;

    @Test
    public void noResourceTest() throws Exception {
        String pipeLaunchId = pipeTester.runPipeToCompletion(TestUpstreamsPipeline.noResourcePipeline());
        JobLaunch consumeRes123 = pipeTester.getJobLastLaunch(pipeLaunchId, TestUpstreamsPipeline.CONSUME_RES1_ID);
        StoredResourceContainer storedResourceContainer = pipeTester.getConsumedResources(
            pipeLaunchId, TestUpstreamsPipeline.CONSUME_RES1_ID
        );

        Assert.assertEquals(StatusChangeType.SUCCESSFUL, consumeRes123.getLastStatusChange().getType());
        Assert.assertEquals(0, storedResourceContainer.getResources().size());
    }

    @Test
    public void directResourceSequenceTest() throws Exception {
        String pipeLaunchId = pipeTester.runPipeToCompletion(TestUpstreamsPipeline.directResourceSequencePipeline());
        JobLaunch consumeRes123 = pipeTester.getJobLastLaunch(pipeLaunchId, TestUpstreamsPipeline.CONSUME_RES1_ID);
        StoredResourceContainer storedResourceContainer = pipeTester.getConsumedResources(
            pipeLaunchId, TestUpstreamsPipeline.CONSUME_RES1_ID
        );

        Assert.assertEquals(StatusChangeType.SUCCESSFUL, consumeRes123.getLastStatusChange().getType());
        Assert.assertEquals(1, storedResourceContainer.getResources().size());
        Assert.assertTrue(storedResourceContainer.containsResource(Res2.class.getCanonicalName()));
    }

    @Test
    public void directResourceSequenceWithDownstreamTest() throws Exception {
        String pipeLaunchId = pipeTester.runPipeToCompletion(
            TestUpstreamsPipeline.directResourceSequenceWithDownstreamPipeline()
        );
        JobLaunch consumeRes123 = pipeTester.getJobLastLaunch(pipeLaunchId, TestUpstreamsPipeline.CONSUME_RES1_ID);
        StoredResourceContainer storedResourceContainer = pipeTester.getConsumedResources(
            pipeLaunchId, TestUpstreamsPipeline.CONSUME_RES1_ID
        );

        Assert.assertEquals(StatusChangeType.SUCCESSFUL, consumeRes123.getLastStatusChange().getType());
        Assert.assertEquals(2, storedResourceContainer.getResources().size());
        Assert.assertTrue(storedResourceContainer.containsResource(Res1.class.getCanonicalName()));
        Assert.assertTrue(storedResourceContainer.containsResource(Res2.class.getCanonicalName()));
    }

    @Test
    public void directResourceParallelTest() throws Exception {
        String pipeLaunchId = pipeTester.runPipeToCompletion(TestUpstreamsPipeline.directResourceParallelPipeline());
        JobLaunch consumeRes123 = pipeTester.getJobLastLaunch(pipeLaunchId, TestUpstreamsPipeline.CONSUME_RES1_ID);
        StoredResourceContainer storedResourceContainer = pipeTester.getConsumedResources(
            pipeLaunchId, TestUpstreamsPipeline.CONSUME_RES1_ID
        );

        Assert.assertEquals(StatusChangeType.SUCCESSFUL, consumeRes123.getLastStatusChange().getType());
        Assert.assertEquals(1, storedResourceContainer.getResources().size());
        Assert.assertTrue(storedResourceContainer.containsResource(Res3.class.getCanonicalName()));
    }

    @Test
    public void allResourceTest() throws Exception {
        String pipeLaunchId = pipeTester.runPipeToCompletion(TestUpstreamsPipeline.allResourcePipeline());
        JobLaunch consumeRes123 = pipeTester.getJobLastLaunch(pipeLaunchId, TestUpstreamsPipeline.CONSUME_RES1_ID);
        StoredResourceContainer storedResourceContainer = pipeTester.getConsumedResources(
            pipeLaunchId, TestUpstreamsPipeline.CONSUME_RES1_ID
        );

        Assert.assertEquals(StatusChangeType.SUCCESSFUL, consumeRes123.getLastStatusChange().getType());
        Assert.assertEquals(2, storedResourceContainer.getResources().size());
        Assert.assertTrue(storedResourceContainer.containsResource(Res1.class.getCanonicalName()));
        Assert.assertTrue(storedResourceContainer.containsResource(Res2.class.getCanonicalName()));
    }
}
