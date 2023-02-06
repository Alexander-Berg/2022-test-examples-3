package ru.yandex.market.tsum.pipe.engine.runtime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.jobs.ProduceRes1AndFail;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.jobs.ProduceRes2;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res1;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res2;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res3;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res4;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res5;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 06.12.2017
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JobContextGetAllResourcesTest {
    @Autowired
    private PipeTester pipeTester;

    @Autowired
    private PipeLaunchDao pipeLaunchDao;

    @Test
    public void getAllProducedResourcesFromAllLaunchesUnsafe() {
        PipelineBuilder builder = PipelineBuilder.create()
            .withManualResource(Res3.class);

        JobBuilder upstream = builder.withJob(ProduceRes2.class)
            .withId("upstream")
            .withResources(new Res5(""));

        JobBuilder notUpstreamAndFailing = builder.withJob(ProduceRes1AndFail.class);

        JobBuilder consumer = builder.withJob(JobThatChecksGetAllResources.class)
            .withResources(new Res4(""))
            .withId("consumer")
            .withUpstreams(upstream)
            .withManualTrigger();  // withManualTrigger нужен чтобы джоба consumer отрабатывала последней

        String pipeLaunchId = pipeTester.runPipeToCompletion(builder.build(), Collections.singletonList(new Res3("")));

        // запускаем upstream ещё раз чтобы проверить что ресурсы из старых запусков берутся
        // если возьмутся, то будет 2 Res2
        pipeTester.triggerJob(pipeLaunchId, upstream.getId());
        pipeTester.runScheduledJobsToCompletion();

        pipeTester.triggerJob(pipeLaunchId, consumer.getId());
        pipeTester.runScheduledJobsToCompletion();

        assertTrue(pipeLaunchDao.getById(pipeLaunchId).getJobs().get(consumer.getId()).isExecutorSuccessful());
    }

    public static class JobThatChecksGetAllResources implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("212e2efb-c48a-4de0-b211-184bf1059a51");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            assertEquals(1, context.resources().getAllProducedResourcesFromAllLaunches(Res1.class).size());
            assertEquals(2, context.resources().getAllProducedResourcesFromAllLaunches(Res2.class).size());
            assertEquals(0, context.resources().getAllProducedResourcesFromAllLaunches(Res3.class).size());
            assertEquals(0, context.resources().getAllProducedResourcesFromAllLaunches(Res4.class).size());
            assertEquals(0, context.resources().getAllProducedResourcesFromAllLaunches(Res5.class).size());
        }
    }
}
