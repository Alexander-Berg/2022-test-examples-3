package ru.yandex.market.tsum.pipelines.common.jobs.sandbox;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.TaskInputDto;
import ru.yandex.market.tsum.context.TestTsumJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobInstanceBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipelines.common.resources.ArcadiaRef;
import ru.yandex.market.tsum.pipelines.common.resources.DeliveryPipelineParams;
import ru.yandex.market.tsum.pipelines.sre.jobs.CreateDeplateServiceFolders;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobTesterConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SandboxTaskWithArcadiaUrlJobTest {
    @Autowired
    private JobTester jobTester;

    TestTsumJobContext context = new TestTsumJobContext("unknown");

    @Test
    public void testTaskEmptyParams() throws Exception {
        SandboxTaskWithArcadiaUrlJob job = createJob(null, null);
        TaskInputDto taskInput = new TaskInputDto("");

        try {
            job.prepareTask(context, taskInput);
            Assert.fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            Assert.assertEquals("DeliveryPipelineParams and ArcadiaRef is null, need at least one", e.getMessage());
        }
    }

    @Test
    public void testTaskDeliveryParams() throws Exception {
        SandboxTaskWithArcadiaUrlJob job = createJob(new ArcadiaRef(), new DeliveryPipelineParams("10", "0", null));
        TaskInputDto taskInput = new TaskInputDto("");

        job.prepareTask(context, taskInput);
        Assert.assertEquals("arcadia:/arc/trunk/arcadia@10", taskInput.getCustomFields().stream()
            .filter(field -> field.getName().equals("ar_ur")).findFirst().orElseThrow().getValue());

    }

    @Test
    public void testTaskRefParams() throws Exception {
        SandboxTaskWithArcadiaUrlJob job = createJob(new ArcadiaRef(), null);
        TaskInputDto taskInput = new TaskInputDto("");

        job.prepareTask(context, taskInput);
        Assert.assertEquals("arcadia:/arc/trunk/arcadia", taskInput.getCustomFields().stream()
            .filter(field -> field.getName().equals("ar_ur")).findFirst().orElseThrow().getValue());

    }

    private SandboxTaskWithArcadiaUrlJob createJob(ArcadiaRef arcadiaRef,
                                                   DeliveryPipelineParams deliveryPipelineParams) {
        JobInstanceBuilder<SandboxTaskWithArcadiaUrlJob> jobBuilder =
            jobTester.jobInstanceBuilder(SandboxTaskWithArcadiaUrlJob.class)
                .withBean(Mockito.mock(SandboxClient.class))
                .withResource(new SandboxTaskWithArcadiaUrlJobConfig("ar_ur"))
                .withResource(
                    SandboxTaskJobConfig.newBuilder(CreateDeplateServiceFolders.SANDBOX_TASK_TYPE)
                        .build()
                );
        if (arcadiaRef != null) {
            jobBuilder.withResource(arcadiaRef);
        }
        if (deliveryPipelineParams != null) {
            jobBuilder.withResource(deliveryPipelineParams);
        }
        return jobBuilder.create();
    }
}
