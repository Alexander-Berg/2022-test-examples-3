package ru.yandex.market.tsum.pipe.engine.runtime.state.calculator;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.pipe.engine.definition.DummyJob;
import ru.yandex.market.tsum.pipe.engine.definition.Pipeline;
import ru.yandex.market.tsum.pipe.engine.definition.builder.JobBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.builder.PipelineBuilder;
import ru.yandex.market.tsum.pipe.engine.definition.common.UpstreamType;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.definition.resources.WiredResource;
import ru.yandex.market.tsum.pipe.engine.runtime.config.MockCuratorConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.PipeServicesConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.config.TestConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StatusChangeType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
    TestConfig.class, PipeServicesConfig.class, MockCuratorConfig.class
})
public class ResourceProviderTest {
    private static final int TEST_RETRY_COUNT = 100;
    private static final String PRODUCER_JOB = "producer_job";
    private static final String PRODUCER_JOB_2 = "producer_job_2";
    private static final String ACCEPTOR_JOB = "acceptor_job";
    private static final String WORKAROUND_JOB = "workaround_job";
    private static final String DUMMY_JOB = "dummy_job";
    private static final String INTERRUPT_RESOURCE_JOB = "interrupt_resource_job";

    @Autowired
    private PipeTester pipeTester;
    @Autowired
    private PipeLaunchDao pipeLaunchDao;

    /*
     * ,__________,    ,__________,
     * | Producer |----| Acceptor |
     * |__________|    |__________|
     */
    @Test
    public void produceResourceTest() {
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder producerJob = builder.withJob(DummyResourceProducer.class, PRODUCER_JOB);
        builder.withJob(DummyResourceAcceptor.class, ACCEPTOR_JOB)
            .withUpstreams(producerJob);
        Pipeline pipeline = builder.build();

        String pipeLaunchId = pipeTester.activateLaunch(pipeline);
        pipeTester.runScheduledJobsToCompletion();
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(pipeLaunchId);
        Assert.assertEquals(StatusChangeType.SUCCESSFUL, pipeLaunch.getJobState(ACCEPTOR_JOB).getLastStatusChangeType());
    }

    /*
     * ,__________,               ,__________,
     * | Producer |--NO_RESOURCE--| Acceptor |
     * |__________|               |__________|
     */
    @Test
    public void noResourcesUpstreamTest() {
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder producerJob = builder.withJob(DummyResourceProducer.class, PRODUCER_JOB);
        builder.withJob(DummyResourceAcceptor.class, ACCEPTOR_JOB)
            .withUpstreams(UpstreamType.NO_RESOURCES, producerJob);
        Pipeline pipeline = builder.build();

        String pipeLaunchId = pipeTester.activateLaunch(pipeline);
        pipeTester.runScheduledJobsToCompletion();
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(pipeLaunchId);
        Assert.assertEquals(StatusChangeType.FAILED, pipeLaunch.getJobState(ACCEPTOR_JOB).getLastStatusChangeType());
    }

    /*
     * ,__________,    ,__________,          ,__________,
     * | Producer |----| Producer |--DIRECT--| Acceptor |
     * |__________|    |__________|          |__________|
     */
    @Test
    public void directResourceUpstreamTest() {
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder producerJob = builder.withJob(DummyResourceProducer.class, PRODUCER_JOB);
        JobBuilder producerJob2 = builder.withJob(DummyResourceProducer.class, PRODUCER_JOB_2)
            .withUpstreams(producerJob);
        builder.withJob(DummyResourceAcceptor.class, ACCEPTOR_JOB)
            .withUpstreams(UpstreamType.DIRECT_RESOURCES, producerJob2);
        Pipeline pipeline = builder.build();

        String pipeLaunchId = pipeTester.activateLaunch(pipeline);
        pipeTester.runScheduledJobsToCompletion();
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(pipeLaunchId);
        Assert.assertEquals(StatusChangeType.SUCCESSFUL, pipeLaunch.getJobState(ACCEPTOR_JOB).getLastStatusChangeType());
    }

    /*
     * ,__________,               ,___________,    ,__________,
     * | Producer |--NO_RESOURCE--| interrupt |----| Acceptor |
     * |__________| \             |__resource_|   /|__________|
     *               \           ,____________,  /
     *                \__________| workaround |_/
     *                           |____________|
     */
    @Test
    public void workaroundNoResourcesUpstreamTest() {
        repeatTest(this::runWorkaroundNoResourcesUpstreamTest);
    }

    private void runWorkaroundNoResourcesUpstreamTest() {
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder producerJob = builder.withJob(DummyResourceProducer.class, PRODUCER_JOB);
        JobBuilder interruptResourceJob = builder.withJob(DummyJob.class, INTERRUPT_RESOURCE_JOB)
            .withUpstreams(UpstreamType.NO_RESOURCES, producerJob);
        JobBuilder workaroundJob = builder.withJob(DummyJob.class, WORKAROUND_JOB)
            .withUpstreams(producerJob);
        builder.withJob(DummyResourceAcceptor.class, ACCEPTOR_JOB)
            .withUpstreams(interruptResourceJob, workaroundJob);
        Pipeline pipeline = builder.build();

        String pipeLaunchId = pipeTester.activateLaunch(pipeline);
        pipeTester.runScheduledJobsToCompletion();
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(pipeLaunchId);
        Assert.assertEquals(StatusChangeType.SUCCESSFUL, pipeLaunch.getJobState(ACCEPTOR_JOB).getLastStatusChangeType());
    }

    /*
     * ,__________,    ,_______,          ,___________,    ,__________,
     * | Producer |----| Dummy |--DIRECT--| interrupt |----| Acceptor |
     * |__________|    |_______| \        |__resource_|   /|__________|
     *                            \      ,____________,  /
     *                             \_____| workaround |_/
     *                                   |____________|
     */
    @Test
    public void workaroundDirectResourcesUpstreamTest() {
        repeatTest(this::runWorkaroundDirectResourcesUpstreamTest);
    }

    private void runWorkaroundDirectResourcesUpstreamTest() {
        PipelineBuilder builder = PipelineBuilder.create();
        JobBuilder producerJob = builder.withJob(DummyResourceProducer.class, PRODUCER_JOB);
        JobBuilder dummyJob = builder.withJob(DummyJob.class, DUMMY_JOB)
            .withUpstreams(producerJob);
        JobBuilder interruptResourceJob = builder.withJob(DummyJob.class, INTERRUPT_RESOURCE_JOB)
            .withUpstreams(UpstreamType.DIRECT_RESOURCES, dummyJob);
        JobBuilder workaroundJob = builder.withJob(DummyJob.class, WORKAROUND_JOB)
            .withUpstreams(dummyJob);
        builder.withJob(DummyResourceAcceptor.class, ACCEPTOR_JOB)
            .withUpstreams(interruptResourceJob, workaroundJob);
        Pipeline pipeline = builder.build();

        String pipeLaunchId = pipeTester.activateLaunch(pipeline);
        pipeTester.runScheduledJobsToCompletion();
        PipeLaunch pipeLaunch = pipeLaunchDao.getById(pipeLaunchId);
        Assert.assertEquals(StatusChangeType.SUCCESSFUL, pipeLaunch.getJobState(ACCEPTOR_JOB).getLastStatusChangeType());
    }

    private static void repeatTest(Runnable test) {
        for (int i = 0; i < TEST_RETRY_COUNT; i++) {
            test.run();
        }
    }

    public static class DummyResource implements Resource {
        private String name = "dummy";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("798e05b3-276e-449c-91e8-70ae623987e8");
        }
    }

    @Produces(single = {DummyResource.class})
    public static class DummyResourceProducer implements JobExecutor<JobContext> {
        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("0a2b32e9-9a55-46d9-8d4b-59565773e624");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            context.resources().produce(new DummyResource());
        }
    }

    public static class DummyResourceAcceptor implements JobExecutor<JobContext> {
        @WiredResource
        private DummyResource dummyResource;

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("721a95fe-eeb6-4650-a56d-6a2aa2a5c669");
        }

        @Override
        public void execute(JobContext context) throws Exception {
            Assert.assertNotNull(dummyResource);
        }
    }
}
