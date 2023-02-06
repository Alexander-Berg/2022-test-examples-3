package ru.yandex.market.tsum.pipelines.sre.jobs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.SandboxTask;
import ru.yandex.market.tsum.clients.sandbox.TaskInputDto;
import ru.yandex.market.tsum.clients.tsum.MarketMapClient;
import ru.yandex.market.tsum.context.TestTsumJobContext;
import ru.yandex.market.tsum.core.environment.Environment;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobInstanceBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipelines.common.jobs.sandbox.SandboxTaskJobConfig;
import ru.yandex.market.tsum.pipelines.common.resources.EnvironmentResource;
import ru.yandex.market.tsum.pipelines.common.resources.YandexDeployStage;
import ru.yandex.market.tsum.pipelines.sre.resources.AppSandboxResourceRef;
import ru.yandex.market.tsum.pipelines.sre.resources.ApplicationName;
import ru.yandex.market.tsum.pipelines.sre.resources.DeplateSandboxConfig;
import ru.yandex.market.tsum.pipelines.sre.resources.YandexDeployServiceSpec;

import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobTesterConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CreateServiceWithDeplateJobTest {
    @Autowired
    private JobTester jobTester;

    TestTsumJobContext context = new TestTsumJobContext("unknown");

    @Test
    public void testPrepareTask() throws Exception {

        String sbrVersion = "123";
        String appName = "test_app";
        Map<String, Object> expected = new HashMap<>(Map.of(
            "service_name", "testing_test_app",
            "project_name", "test_app",
            "root_abc", "abc1",
            "layers", Map.of(appName + "-app", sbrVersion)
        ));

        TaskInputDto taskInput = new TaskInputDto("");

        AppSandboxResourceRef resourceRef = Mockito.mock(AppSandboxResourceRef.class);
        when(resourceRef.getAppSandboxResourceRef()).thenReturn(sbrVersion);

        YandexDeployServiceSpec serviceSpec = new YandexDeployServiceSpec();

        serviceSpec.setApplicationName(appName);
        serviceSpec.setProjectId(String.valueOf(expected.get("project_name")));
        serviceSpec.setAbcQuotaServiceSlug(String.valueOf(expected.get("root_abc")));
        serviceSpec.setStages(List.of(
            new YandexDeployStage("prestable", Environment.PRESTABLE, "test"),
            new YandexDeployStage("production", Environment.PRODUCTION, "test"),
            new YandexDeployStage(String.valueOf(expected.get("service_name")), Environment.TESTING, "test")
        ));

        CreateServiceWithDeplateJob job = createJob(serviceSpec, resourceRef);

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
        serviceSpec.setStages(List.of(
            new YandexDeployStage("", Environment.TESTING, "test")
        ));

        CreateServiceWithDeplateJob job = createJob(serviceSpec, Mockito.mock(AppSandboxResourceRef.class));

        SandboxTask task = Mockito.mock(SandboxTask.class);
        try {
            job.processResult(context, task, List.of());
            Assert.fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            Assert.assertEquals("Unknown deplate create result", e.getMessage());
        }

        when(task.getOutputParameter("result", String.class)).thenReturn(Optional.of("NOOP"));

        job.processResult(context, task, List.of());
        Assert.assertEquals("Deplate service already exists", context.getLastProgress().getStatusText());

        when(task.getOutputParameter("result", String.class)).thenReturn(Optional.of("OK"));

        job.processResult(context, task, List.of());
        Assert.assertEquals("Deplate service created", context.getLastProgress().getStatusText());
    }

    private CreateServiceWithDeplateJob createJob(YandexDeployServiceSpec serviceSpec,
                                                  AppSandboxResourceRef resourceRef) {
        JobInstanceBuilder<CreateServiceWithDeplateJob> jobBuilder =
            jobTester.jobInstanceBuilder(CreateServiceWithDeplateJob.class)
                .withBean(Mockito.mock(SandboxClient.class))
                .withBean(Mockito.mock(MarketMapClient.class))
                .withResources(serviceSpec)
                .withResource(new DeplateSandboxConfig())
                .withResource(resourceRef)
                .withResource(new ApplicationName())
                .withResource(new EnvironmentResource(Environment.TESTING, "test"))
                .withResource(
                    SandboxTaskJobConfig.newBuilder(CreateServiceWithDeplateJob.SANDBOX_TASK_TYPE)
                        .build()
                );
        return jobBuilder.create();
    }
}
