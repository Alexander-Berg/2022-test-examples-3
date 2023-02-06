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
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;

import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeStateService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res1;
import ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources.Res2;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 13.04.2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JobContextProducedResourcesValidationTest {
    @Autowired
    private PipeTester pipeTester;

    @Autowired
    private PipeLaunchDao pipeLaunchDao;

    @Autowired
    private PipeStateService pipeStateService;

    @Test
    public void jobThatDeclaresAndProducesSingleRes1_shouldSucceed() throws InterruptedException {
        assertSuccessfullyProducesResources(JobThatDeclaresAndProducesSingleRes1.class, 1);
    }

    @Test
    public void jobThatDeclaresSingleRes1ButDoesNotProduceAnything_shouldFail() throws InterruptedException {
        assertFails(JobThatDeclaresSingleRes1ButDoesNotProduceAnything.class);
    }

    @Test
    public void jobThatDeclaresSingleRes1ButProducesMultipleRes1_shouldFail() throws InterruptedException {
        assertFails(JobThatDeclaresSingleRes1ButProducesMultipleRes1.class);
    }

    @Test
    public void jobThatDeclaresAndProducesMultipleRes1_shouldSucceed() throws InterruptedException {
        assertSuccessfullyProducesResources(JobThatDeclaresAndProducesMultipleRes1.class, 2);
    }

    @Test
    public void jobThatProducesUndeclaredResource_shouldFail() throws InterruptedException {
        assertFails(JobThatProducesUndeclaredResource.class);
    }

    private void assertSuccessfullyProducesResources(Class<? extends JobExecutor> jobClass,
                                                     int expectedResourceCount) throws InterruptedException {
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder job = builder.withJob(jobClass);

        String pipeLaunchId = pipeTester.runPipeToCompletion(builder.build());

        assertEquals(
            expectedResourceCount,
            pipeTester.getProducedResources(pipeLaunchId, job.getId()).getResources().size()
        );
    }

    private void assertFails(Class<? extends JobExecutor> jobClass) throws InterruptedException {
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder job = builder.withJob(jobClass);

        String pipeLaunchId = pipeTester.runPipeToCompletion(builder.build());

        assertEquals(
            StatusChangeType.FAILED,
            pipeTester.getJobLastLaunch(pipeLaunchId, job.getId()).getLastStatusChange().getType()
        );
    }


    @Produces(single = Res1.class)
    public static class JobThatDeclaresAndProducesSingleRes1 implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("72602df4-ac1f-47be-a22f-c8e9641e8593");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            context.resources().produce(new Res1(""));
        }
    }

    @Produces(single = Res1.class)
    public static class JobThatDeclaresSingleRes1ButDoesNotProduceAnything implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("b7ddafa5-3f28-47c2-ae92-d083798ac331");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            context.resources().produce(new Res1("1"));
            context.resources().produce(new Res1("2"));
        }
    }

    @Produces(single = Res1.class)
    public static class JobThatDeclaresSingleRes1ButProducesMultipleRes1 implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("13341407-f733-4505-b7de-020f59cb92f0");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            context.resources().produce(new Res1("1"));
            context.resources().produce(new Res1("2"));
        }
    }

    @Produces(multiple = Res1.class)
    public static class JobThatDeclaresAndProducesMultipleRes1 implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("deacf3ca-ea2f-489c-9a04-5b1af356873b");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            context.resources().produce(new Res1("1"));
            context.resources().produce(new Res1("2"));
        }
    }

    @Produces(single = Res1.class)
    public static class JobThatProducesUndeclaredResource implements JobExecutor {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("d617a81c-5702-48fc-b88c-8aee7e51a051");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            context.resources().produce(new Res1("1"));
            context.resources().produce(new Res2("2"));
        }
    }
}
