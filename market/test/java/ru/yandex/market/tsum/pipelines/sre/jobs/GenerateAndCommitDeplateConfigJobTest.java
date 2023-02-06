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
import ru.yandex.market.tsum.clients.yav.VaultClient;
import ru.yandex.market.tsum.clients.yav.model.VaultGetSecretResponse;
import ru.yandex.market.tsum.clients.yav.model.VaultSecret;
import ru.yandex.market.tsum.clients.yav.model.VaultSecretVersion;
import ru.yandex.market.tsum.context.TestTsumJobContext;
import ru.yandex.market.tsum.core.environment.Environment;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.JobManualFailException;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobInstanceBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipelines.common.jobs.sandbox.SandboxTaskJobConfig;
import ru.yandex.market.tsum.pipelines.common.resources.AbcServiceResource;
import ru.yandex.market.tsum.pipelines.common.resources.EnvironmentResource;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekTicket;
import ru.yandex.market.tsum.pipelines.common.resources.YandexDeployStage;
import ru.yandex.market.tsum.pipelines.sre.resources.ApplicationName;
import ru.yandex.market.tsum.pipelines.sre.resources.DeplateSandboxConfig;
import ru.yandex.market.tsum.pipelines.sre.resources.YandexDeployServiceSpec;

import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobTesterConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GenerateAndCommitDeplateConfigJobTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Autowired
    private JobTester jobTester;

    TestTsumJobContext context = new TestTsumJobContext("unknown");
    RootArcadiaClient arcadiaClient = Mockito.mock(RootArcadiaClient.class);

    @Test
    public void testPrepareTask() throws Exception {

        Map<String, Object> expected = new HashMap<>(Map.of(
            "ticket", "TICKET-1",
            "itype", "testitype",
            "service_name", "testing_test_app",
            "app_name", "test_app",
            "root_abc", "abc1",
            "abc", "abc2",
            "environment", "testing",
            "cpu", "1000",
            "ram", "2",
            "disk_type", "hdd"
        ));
        expected.put("disk_size", "10");
        expected.put("sas_pods", 1);
        expected.put("network_macro", "_MARKET_TEST_NETS_");
        expected.put("template_root", "base.jsonnet");
        expected.put("template_modifiers", List.of("box/infra.jsonnet", "box/base_app.jsonnet",
            "remove_duplicates.jsonnet", "update_layers.jsonnet", "set_infra_params.jsonnet"));
        expected.put("template_tools", List.of("tools.libjsonnet"));
        expected.put("exp3_consumer", "consumer");
        expected.put("exp3_secret_id", "sec-01g63032k07w64gwsw5m3f3nj4");
        expected.put("exp3_secret_alias", "alias");
        expected.put("exp3_secret_version", "0");

        TaskInputDto taskInput = new TaskInputDto("");

        YandexDeployServiceSpec serviceSpec = new YandexDeployServiceSpec();

        serviceSpec.setApplicationName(String.valueOf(expected.get("app_name")));
        serviceSpec.setItype(String.valueOf(expected.get("itype")));
        serviceSpec.setAbcQuotaServiceSlug(String.valueOf(expected.get("root_abc")));
        serviceSpec.setStages(List.of(
            buildStage(Environment.PRESTABLE),
            buildStage(Environment.PRODUCTION),
            buildStage(Environment.TESTING)
        ));
        serviceSpec.setAbcSlug("consumer");

        GenerateAndCommitDeplateConfigJob job = createJob(serviceSpec, String.valueOf(expected.get("ticket")),
            String.valueOf(expected.get("abc")));

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

    private YandexDeployStage buildStage(Environment environment) {
        return YandexDeployStage.builder()
            .withName(environment.name().toLowerCase() + "_test_app")
            .withEnvironment(environment)
            .withInstallationName("test")
            .withLocations(List.of("SAS"))
            .withRamGb(2)
            .withCpuMillicores(1000)
            .withInstanceCount(1)
            .withStorageClass("hdd")
            .withVolumeGb(10)
            .build();
    }

    @Test
    public void testProcessResult() throws Exception {

        YandexDeployServiceSpec serviceSpec = new YandexDeployServiceSpec();

        GenerateAndCommitDeplateConfigJob job = createJob(serviceSpec, "TEST-1", "");

        SandboxTask task = Mockito.mock(SandboxTask.class);
        try {
            job.processResult(context, task, List.of());
            Assert.fail("should throw IllegalStateException");
        } catch (IllegalStateException e) {
            Assert.assertEquals("Unknown deplate generate result", e.getMessage());
        }

        when(task.getOutputParameter("result_status", String.class)).thenReturn(Optional.of("NOOP"));

        job.processResult(context, task, List.of());
        Assert.assertEquals("Deplate config already exists", context.getLastProgress().getStatusText());

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
        Assert.assertEquals("Deplate config committed", context.getLastProgress().getStatusText());
    }

    private VaultClient getMockVaultClient() {
        VaultClient vaultClient = Mockito.mock(VaultClient.class);
        VaultGetSecretResponse vaultGetSecretResponse = new VaultGetSecretResponse();
        VaultSecret secret = new VaultSecret();
        secret.setUuid("sec-01g63032k07w64gwsw5m3f3nj4");
        secret.setName("alias");
        VaultSecretVersion version = new VaultSecretVersion();
        version.setCreatedAt(0);
        version.setVersion("0");
        secret.setSecretVersions(List.of(version));
        vaultGetSecretResponse.setSecret(secret);
        when(vaultClient.getSecret("sec-01g63032k07w64gwsw5m3f3nj4")).thenReturn(vaultGetSecretResponse);
        return vaultClient;
    }

    private GenerateAndCommitDeplateConfigJob createJob(YandexDeployServiceSpec serviceSpec, String ticket,
                                                        String abcSlug) {
        JobInstanceBuilder<GenerateAndCommitDeplateConfigJob> jobBuilder =
            jobTester.jobInstanceBuilder(GenerateAndCommitDeplateConfigJob.class)
            .withBean(arcadiaClient)
            .withBean(getMockVaultClient())
            .withBean(Mockito.mock(SandboxClient.class))
            .withResources(serviceSpec)
            .withResource(new DeplateSandboxConfig())
            .withResource(new StartrekTicket(ticket))
            .withResource(new ApplicationName())
            .withResource(new AbcServiceResource(abcSlug, 0))
            .withResource(new EnvironmentResource(Environment.TESTING, "test"))
            .withResource(
                SandboxTaskJobConfig.newBuilder(GenerateAndCommitDeplateConfigJob.SANDBOX_TASK_TYPE)
                    .build()
            );
        return jobBuilder.create();
    }
}
