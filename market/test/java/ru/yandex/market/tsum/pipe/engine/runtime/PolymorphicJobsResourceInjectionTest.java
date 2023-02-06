package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.StoredResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res1;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res2;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.jobs.Producer451Job;

import java.util.Collections;
import java.util.UUID;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 26.05.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class PolymorphicJobsResourceInjectionTest {
    private static final String JOB_ID = "job";

    @Autowired
    private PipeTester pipeTester;

    @Test
    public void simpleInjection() throws Exception {
        String pipeLaunchId = pipeTester.runPipeToCompletion(
            getPipeline(), Collections.emptyList()
        );

        StoredResourceContainer producedResources = pipeTester.getProducedResources(
            pipeLaunchId, JOB_ID
        );

        Res1 res1 = pipeTester.getResourceOfType(producedResources, Res1.class);
        Res2 res2 = pipeTester.getResourceOfType(producedResources, Res2.class);

        Assert.assertEquals("res1 res1", res1.getS());
        Assert.assertEquals("res2 res2", res2.getS());
    }

    private Pipeline getPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();

        JobBuilder producer = builder.withJob(Producer451Job.class);

        builder.withJob(ChildJob.class)
            .withId(JOB_ID)
            .withResources(new Res1("res1"), new Res2("res2"))
            .withUpstreams(producer);

        return builder.build();
    }

    @Produces(single = Res1.class)
    public static class ParentJob implements JobExecutor {
        @WiredResource
        private Res1 res1;

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("c6f72564-e5e0-4db1-9872-80c8b5b82680");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            context.resources().produce(new Res1("res1 " + res1.getS()));
        }
    }

    @Produces(single = Res2.class)
    public static class ChildJob extends ParentJob {
        @WiredResource
        private Res2 res2;

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("dd4433d4-ce1e-49c9-9671-02c2e9a4be80");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            super.execute(context);
            context.resources().produce(new Res2("res2 " + res2.getS()));
        }
    }
}
