package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res1;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res2;

import java.util.Collections;
import java.util.UUID;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 23.05.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class OptionalResourcesTest {
    public static final String JOB_ID = "job";

    @Autowired
    private PipeTester pipeTester;

    @Test
    public void testNoResourcesProvidedException() throws Exception {
        //TODO check for exception when validation is ready
        testPipeline(null, null, new ExpectedResources(null, null), StatusChangeType.FAILED);
    }

    @Test
    public void testDefaultResource() throws Exception {
        testPipeline("res1", null, new ExpectedResources("res1", "default"), StatusChangeType.SUCCESSFUL);
    }

    @Test
    public void testDefaultResourceOverride() throws Exception {
        testPipeline("res11", "res2", new ExpectedResources("res11", "res2"), StatusChangeType.SUCCESSFUL);
    }

    private void testPipeline(String res1, String res2, ExpectedResources expectedResources,
                              StatusChangeType expectedState) throws Exception {
        String pipeLaunchId = pipeTester.runPipeToCompletion(
            pipeline(res1, res2, expectedResources),
            Collections.emptyList()
        );
        JobLaunch launch = pipeTester.getJobLastLaunch(pipeLaunchId, JOB_ID);
        Assert.assertEquals(
            expectedState,
            launch.getLastStatusChange().getType()
        );
    }

    private Pipeline pipeline(String res1, String res2, ExpectedResources expectedResources) {
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder job = builder.withJob(OptionalResourceJob.class).withId(JOB_ID);
        job.withResources(expectedResources);
        if (res1 != null) {
            job.withResources(new Res1(res1));
        }
        if (res2 != null) {
            job.withResources(new Res2(res2));
        }
        return builder.build();
    }

    public static class OptionalResourceJob implements JobExecutor {

        @WiredResource
        private Res1 res1;

        @WiredResource(optional = true)
        private Res2 res2 = new Res2("default");

        @WiredResource
        private ExpectedResources expectedResources;

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("dcfe6a8c-05c1-41d1-9db5-f581720d9ccf");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            Assert.assertEquals(expectedResources.res1, res1);
            Assert.assertEquals(expectedResources.res2, res2);
        }
    }

    private static class ExpectedResources implements Resource {
        public ExpectedResources() {
        }

        public ExpectedResources(String res1, String res2) {
            this.res1 = (res1 == null) ? null : new Res1(res1);
            this.res2 = (res2 == null) ? null : new Res2(res2);
        }

        private Res1 res1;
        private Res2 res2;

        public Res1 getRes1() {
            return res1;
        }

        public Res2 getRes2() {
            return res2;
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("dca7bb1d-a875-4cf2-9a63-5ec5a2cf19fc");
        }
    }
}
