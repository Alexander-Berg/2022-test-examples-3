package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.StoredResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res1;

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
public class ProduceMultipleResourcesTest {
    public static final String JOB_ID = "job";

    @Autowired
    private PipeTester pipeTester;

    @Test
    public void produceZeroResourcesIsOK() throws Exception {
        String pipeLaunchId = pipeTester.runPipeToCompletion(
            pipeline(ZeroResourcesJob.class),
            Collections.emptyList()
        );

        JobLaunch jobLastLaunch = pipeTester.getJobLastLaunch(pipeLaunchId, JOB_ID);

        Assert.assertEquals(
            StatusChangeType.SUCCESSFUL,
            jobLastLaunch.getLastStatusChange().getType()
        );
    }

    @Test
    public void producesTwoResources() throws Exception {
        String pipeLaunchId = pipeTester.runPipeToCompletion(
            pipeline(TwoResourcesJob.class),
            Collections.emptyList()
        );

        StoredResourceContainer producedResources = pipeTester.getProducedResources(pipeLaunchId, JOB_ID);
        Assert.assertEquals(2, producedResources.getResources().size());
    }

    private Pipeline pipeline(Class<? extends JobExecutor> executorClass) {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(executorClass).withId(JOB_ID);
        return builder.build();
    }

    @Produces(multiple = Res1.class)
    public static class ZeroResourcesJob implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("3d77c82c-e667-484d-961e-d6cbc905041a");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            // this job could provide Res1, but it doesn't want to
        }
    }

    @Produces(multiple = Res1.class)
    public static class TwoResourcesJob implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("d9afe8fe-a0fa-4390-9a04-1e984d9d9fd4");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            context.resources().produce(new Res1("x1"));
            context.resources().produce(new Res1("x2"));
        }
    }
}
