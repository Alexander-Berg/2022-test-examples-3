package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.builder.BaseJobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.di.model.StoredResourceContainer;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res1;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res3;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.nested_pipes.PipeWithNestedPipe;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 10.05.2017
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class NestedPipesTest {
    @Autowired
    private PipeTester pipeTester;

    @Test
    public void getJobsWithoutUpstreamsAndDownstreamsTest() {
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder job1 = builder.withJob(DummyJob.class);
        JobBuilder job2 = builder.withJob(DummyJob.class)
            .withUpstreams(job1);

        List<BaseJobBuilder<?>> jobBuildersWithoutUpstreams = builder.getJobBuildersWithoutUpstreams();
        assertEquals(1, jobBuildersWithoutUpstreams.size());
        assertSame(job1, jobBuildersWithoutUpstreams.get(0));

        List<BaseJobBuilder<?>> jobsBuilderWithoutDownstreams = builder.getJobBuildersWithoutDownstreams();
        assertEquals(1, jobsBuilderWithoutDownstreams.size());
        assertSame(job2, jobsBuilderWithoutDownstreams.get(0));
    }

    @Test
    public void nestedPipes() throws Exception {
        String pipeLaunchId = pipeTester.runPipeToCompletion(
            PipeWithNestedPipe.PARENT_PIPE_NAME,
            Collections.singletonList(new Res1("manual"))
        );
        StoredResourceContainer producedResources = pipeTester.getProducedResources(
            pipeLaunchId,
            PipeWithNestedPipe.JOB_ID
        );
        assertEquals(
            "manual ConvertRes1ToRes2 ConvertRes2ToRes3",
            pipeTester.getResourceOfType(producedResources, Res3.class).getS()
        );
    }
}
