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
public class DeclaredResourcesNotProducedTest {
    public static final String JOB_ID = "job";

    @Autowired
    private PipeTester pipeTester;

    @Test
    public void test() throws Exception {
        String pipeLaunchId = pipeTester.runPipeToCompletion(
            invalidPipeline(),
            Collections.emptyList()
        );

        JobLaunch jobLastLaunch = pipeTester.getJobLastLaunch(pipeLaunchId, JOB_ID);

        Assert.assertEquals(StatusChangeType.FAILED, jobLastLaunch.getLastStatusChange().getType());
    }

    private Pipeline invalidPipeline() {
        PipelineBuilder builder = PipelineBuilder.create();
        builder.withJob(InvalidJob.class).withId(JOB_ID);
        return builder.build();
    }

    @Produces(single = Res1.class)
    public static class InvalidJob implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("30572e1f-0a40-423d-b9a3-c41f163e1318");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            // this job should provide Res1, but it doesn't want to
        }
    }
}
