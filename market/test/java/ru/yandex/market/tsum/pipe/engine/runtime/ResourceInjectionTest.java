package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.StoredResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.MultiInjectionPipe;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.SimpleInjectionPipe;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.StaticResourcesPipe;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.jobs.ProducerDerived451Job;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.jobs.Resource451DoubleJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.resources.DerivedResource451;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.resources.Pipe451Result;

import java.util.Collections;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 22.03.17
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ResourceInjectionTest {
    @Autowired
    private PipeTester pipeTester;

    @Test
    public void simpleInjection() throws Exception {
        String pipeLaunchId = pipeTester.runPipeToCompletion(
            SimpleInjectionPipe.PIPE_NAME,
            Collections.emptyList()
        );

        StoredResourceContainer producedResources = pipeTester.getProducedResources(
            pipeLaunchId, SimpleInjectionPipe.DOUBLER_JOB_ID
        );

        Pipe451Result result = pipeTester.getResourceOfType(producedResources, Pipe451Result.class);

        // Producer451 +-----> Doubler451
        Assert.assertEquals(451 * 2, (int) result.getValue());
    }

    @Test
    public void simpleDerivedInjection() throws Exception {
        final String jobId = "Resource451DoubleJob";

        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(Resource451DoubleJob.class)
            .withId(jobId)
            .withUpstreams(builder.withJob(ProducerDerived451Job.class));

        String pipeLaunchId = pipeTester.runPipeToCompletion(builder.build(), Collections.emptyList());
        StoredResourceContainer producedResources = pipeTester.getProducedResources(pipeLaunchId, jobId);

        // ProducerDerived451Job +-----> Doubler451
        Assert.assertEquals(451 * 2, (int) pipeTester.getResourceOfType(producedResources, Pipe451Result.class).getValue());
    }

    @Test
    public void staticDerivedInjection() throws Exception {
        final String jobId = "Resource451DoubleJob";

        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(Resource451DoubleJob.class)
            .withId(jobId)
            .withResources(new DerivedResource451(1));

        String pipeLaunchId = pipeTester.runPipeToCompletion(builder.build(), Collections.emptyList());
        StoredResourceContainer producedResources = pipeTester.getProducedResources(pipeLaunchId, jobId);

        Assert.assertEquals(2, (int) pipeTester.getResourceOfType(producedResources, Pipe451Result.class).getValue());
    }

    @Test
    public void multiDerivedInjection() throws Exception {
        String pipeLaunchId = pipeTester.runPipeToCompletion(
            MultiInjectionPipe.PIPE_NAME,
            Collections.emptyList()
        );

        StoredResourceContainer producedResources = pipeTester.getProducedResources(
            pipeLaunchId, MultiInjectionPipe.SUMMATOR_JOB_ID
        );

        Pipe451Result result = pipeTester.getResourceOfType(producedResources, Pipe451Result.class);

        /*
             +-->ProducerNested451Job+--+
             |                          v
        Producer451                 Summator451
             |                          ^
             +-->Producer451+-----------+
         */

        Assert.assertEquals(451 * 3, (int) result.getValue());
    }

    @Test
    public void staticResourceInjection() throws Exception {
        String pipeLaunchId = pipeTester.runPipeToCompletion(
            StaticResourcesPipe.PIPE_NAME,
            Collections.emptyList()
        );

        StoredResourceContainer producedResources = pipeTester.getProducedResources(
            pipeLaunchId, StaticResourcesPipe.DOUBLER_JOB_ID
        );

        Pipe451Result result = pipeTester.getResourceOfType(producedResources, Pipe451Result.class);

        // Doubler451
        Assert.assertEquals(451 * 2, (int) result.getValue());
    }
}
