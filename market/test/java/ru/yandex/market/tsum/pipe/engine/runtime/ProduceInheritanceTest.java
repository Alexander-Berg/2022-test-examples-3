package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.ResourceRef;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res1;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res2;
import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;
import ru.yandex.market.tsum.pipe.engine.source_code.model.JobExecutorObject;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProduceInheritanceTest {
    public static final String JOB_ID = "job";

    @Autowired
    private PipeTester pipeTester;

    @Autowired
    private SourceCodeService sourceCodeService;

    @Test
    public void testProducesWhenNoAnnotationOnNested() {
        String pipeLaunchId = pipeTester.runPipeToCompletion(
            pipeline(),
            Collections.emptyList()
        );

        JobLaunch jobLastLaunch = pipeTester.getJobLastLaunch(pipeLaunchId, JOB_ID);
        List<ResourceRef> resources = jobLastLaunch.getProducedResources().getResources();
        Assert.assertEquals(resources.size(), 1);
    }

    @Test
    public void testMergesProduces() {
        JobExecutorObject executor = sourceCodeService.getJobExecutor(InheritedJobWithTwoResourceProduces.class);
        Assert.assertEquals(2, executor.getProducedResources().size());
    }

    private Pipeline pipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(InheritedJob.class, JOB_ID);
        return builder.build();
    }

    @Produces(single = Res1.class)
    public static abstract class BaseJob implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("2ccc01c6-baa4-465b-a5bd-5d60db2ecd9a");
        }
    }

    public static class InheritedJob extends BaseJob {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("2ccc01c6-baa4-465b-a5bd-5d60db2ecd9a");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            context.resources().produce(new Res1("1"));
        }
    }

    @Produces(single = {Res1.class, Res2.class})
    public static class InheritedJobWithTwoResourceProduces extends BaseJob {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("e50177ae-5620-4cff-b3c3-b3ddd36f2843");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            context.resources().produce(new Res1("1"));
            context.resources().produce(new Res2("2"));
        }
    }
}
