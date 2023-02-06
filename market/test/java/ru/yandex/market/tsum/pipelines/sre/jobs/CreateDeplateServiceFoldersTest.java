package ru.yandex.market.tsum.pipelines.sre.jobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.arcadia.RootArcadiaClient;
import ru.yandex.market.tsum.clients.arcadia.review.ArcadiaReviewsClient;
import ru.yandex.market.tsum.clients.arcadia.review.model.ReviewRequest;
import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.SandboxTask;
import ru.yandex.market.tsum.clients.sandbox.TaskInputDto;
import ru.yandex.market.tsum.context.TestTsumJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.JobManualFailException;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobInstanceBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipelines.common.jobs.sandbox.SandboxTaskJobConfig;
import ru.yandex.market.tsum.pipelines.sre.resources.DeplateSandboxConfig;
import ru.yandex.market.tsum.pipelines.sre.resources.YandexDeployServiceSpec;

import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobTesterConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CreateDeplateServiceFoldersTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Autowired
    private JobTester jobTester;

    TestTsumJobContext context = new TestTsumJobContext("unknown");
    RootArcadiaClient arcadiaClient = Mockito.mock(RootArcadiaClient.class);

    @Test
    public void testPrepareTask() throws Exception {

        Map<String, String> expected = new HashMap<>(Map.of(
            "ticket", "TICKET-1",
            "root_abc", "abc1",
            "arc_token", "token",
            "arc_user", "user"
        ));

        YandexDeployServiceSpec serviceSpec = new YandexDeployServiceSpec();

        serviceSpec.setAbcQuotaServiceSlug(expected.get("root_abc"));

        CreateDeplateServiceFolders job = createJob(
            new DeplateSandboxConfig("", expected.get("arc_token"), expected.get("arc_user")),
            new CreateDeplateServiceFoldersConfig(expected.get("ticket"), expected.get("root_abc")));

        TaskInputDto taskInput = new TaskInputDto("");

        job.prepareTask(context, taskInput);

        Assert.assertEquals(expected,
            taskInput.getCustomFields()
                .stream()
                .filter(item -> expected.containsKey(item.getName()))
                .collect(Collectors.toMap(
                    TaskInputDto.TaskFieldValidateItem::getName,
                    TaskInputDto.TaskFieldValidateItem::getValue
                )));
    }

    @Test
    public void testProcessResult() throws Exception {

        YandexDeployServiceSpec serviceSpec = new YandexDeployServiceSpec();

        CreateDeplateServiceFolders job = createJob(
            new DeplateSandboxConfig(), new CreateDeplateServiceFoldersConfig("TEST-1", "root_abc"));

        SandboxTask task = Mockito.mock(SandboxTask.class);
        try {
            job.processResult(context, task, List.of());
            Assert.fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            Assert.assertEquals("Unknown deplate folders create result", e.getMessage());
        }

        when(task.getOutputParameter("result_status", String.class)).thenReturn(Optional.of("NOOP"));

        job.processResult(context, task, List.of());
        Assert.assertEquals("Deplate service folders exists", context.getLastProgress().getStatusText());

        when(task.getOutputParameter("result_status", String.class)).thenReturn(Optional.of("OK"));
        when(task.getOutputParameter("result_pr", Double.class)).thenReturn(Optional.of(100.0));

        ArcadiaReviewsClient client = Mockito.mock(ArcadiaReviewsClient.class);
        ReviewRequest review = Mockito.mock(ReviewRequest.class);
        when(arcadiaClient.getReviewsClient()).thenReturn(client);
        when(client.getReview(100)).thenReturn(review);
        when(review.getStatus()).thenReturn("discarded", "discarded", "submitted");

        expectedException.expect(JobManualFailException.class);
        expectedException.expectMessage(("PR was discarded"));
        job.processResult(context, task, List.of());

        job.processResult(context, task, List.of());
        Assert.assertEquals("Deplate service folders created", context.getLastProgress().getStatusText());

    }

    private CreateDeplateServiceFolders createJob(DeplateSandboxConfig sandboxConfig,
                                                  CreateDeplateServiceFoldersConfig config) {
        JobInstanceBuilder<CreateDeplateServiceFolders> jobBuilder =
            jobTester.jobInstanceBuilder(CreateDeplateServiceFolders.class)
                .withBean(arcadiaClient)
                .withBean(Mockito.mock(SandboxClient.class))
                .withResource(sandboxConfig)
                .withResource(config)
                .withResource(
                    SandboxTaskJobConfig.newBuilder(CreateDeplateServiceFolders.SANDBOX_TASK_TYPE)
                        .build()
                );
        return jobBuilder.create();
    }
}
