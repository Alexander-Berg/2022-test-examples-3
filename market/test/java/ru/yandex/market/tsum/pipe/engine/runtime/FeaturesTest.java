package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.Features;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobFeature;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceRef;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;

import java.util.List;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
public class FeaturesTest {
    private static final String JOB_ID = "test";

    @Autowired
    private PipeTester pipeTester;

    @Test
    public void producesResourcesFromFeature() {
        String pipeLaunchId = pipeTester.runPipeToCompletion(pipeline());

        PipeLaunch launch = pipeTester.getPipeLaunch(pipeLaunchId);
        List<ResourceRef> resources = launch.getJobs().get(JOB_ID)
            .getLastLaunch().getProducedResources().getResources();

        Assert.assertEquals(1, resources.size());
        Assert.assertEquals(new TestFeatureResource().getSourceCodeId(), resources.get(0).getSourceCodeId());
    }

    private Pipeline pipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(TestJob.class, "test");

        return builder.build();
    }

    private static class TestFeatureResource implements Resource {
        private int field;

        public int getField() {
            return field;
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("ebceca1f-60c8-43a3-ab5d-0bb58c47f6f6");
        }
    }

    @Produces(single = TestFeatureResource.class)
    private static class TestFeature implements JobFeature {
        @Override
        public void execute(JobContext context) throws Exception {
            context.resources().produce(new TestFeatureResource());
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("a0edbcf3-05b5-4a06-9376-a787a4bc27bd");
        }
    }

    @Features(TestFeature.class)
    private static class TestJob implements JobExecutor {

        @Override
        public void execute(JobContext context) throws Exception {
            context.features().runAll();
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("4fa603e8-5b7e-44b3-be2c-d729416506d6");
        }
    }
}
