package ru.yandex.market.tsum.clients.yp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.tsum.clients.yp.model.ConditionStatus;
import ru.yandex.market.tsum.clients.yp.model.DeployUnitSpec;
import ru.yandex.market.tsum.clients.yp.model.PodDeployPrimitiveCase;
import ru.yandex.market.tsum.clients.yp.model.Project;
import ru.yandex.market.tsum.clients.yp.model.Stage;
import ru.yandex.market.tsum.clients.yp.model.StageSpec;
import ru.yandex.market.tsum.clients.yp.model.StageStatus;
import ru.yandex.market.tsum.clients.yp.transport.TransportType;
import ru.yandex.yp.YpInstance;
import ru.yandex.yp.YpRawClient;
import ru.yandex.yp.YpRawClientBuilder;
import ru.yandex.yp.model.YpObjectHistory;

import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertThat;

@ParametersAreNonnullByDefault
@Ignore("integration test")
@RunWith(Parameterized.class)
public class YandexDeployClientTest {
    private static final String YP_TOKEN;
    private static final YpRawClient YP_RAW_CLIENT;
    private static final YandexDeployClient DEPLOY_CLIENT;

    static {
        // https://oauth.yandex-team.ru/authorize?response_type=token&client_id=f8446f826a6f4fd581bf0636849fdcd7
        YP_TOKEN = getToken(".yp/token");
        YP_RAW_CLIENT = new YpRawClientBuilder(YpInstance.CROSS_DC, () -> YP_TOKEN)
            .setTimeout(20, TimeUnit.SECONDS)
            .setUseMasterDiscovery(false)
            .build();
        DEPLOY_CLIENT = new YandexDeployClient(YP_RAW_CLIENT);
    }

    private final TransportType transportType;

    public YandexDeployClientTest(TransportType transportType) {
        this.transportType = transportType;
    }

    public static String getToken(String path) {
        try {
            // TODO поменять на Files.readString, когда не надо будет поддерживать Java 8
            //noinspection ReadWriteStringCanBeUsed
            String fileContent = new String(
                Files.readAllBytes(FileSystems.getDefault().getPath(System.getenv("HOME"), path)),
                StandardCharsets.UTF_8);
            return fileContent.trim();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.stream(TransportType.values())
            .map(type -> new Object[]{type})
            .collect(Collectors.toList());
    }

    @Test
    public void getStageSpec() throws ExecutionException, InterruptedException {
        StageSpec stageSpec = DEPLOY_CLIENT.getStageSpec(transportType, "andy-ilyin-test-stage");
        System.out.println(stageSpec);
        assertThat(stageSpec.getDeployUnitsOrThrow("DeployUnit1").getPodDeployPrimitiveCase(),
            isOneOf(PodDeployPrimitiveCase.values()));
    }

    @Test
    public void getStageStatus() throws ExecutionException, InterruptedException {
        StageStatus stageStatus = DEPLOY_CLIENT.getStageStatus(transportType, "andy-ilyin-test-stage");
        System.out.println(stageStatus);
        assertThat(stageStatus.getDeployUnitsOrThrow("DeployUnit1").getReady().getStatus(),
            isOneOf(ConditionStatus.values()));
    }

    @Test
    public void getStages() throws ExecutionException, InterruptedException {
        List<Stage> stages = DEPLOY_CLIENT.listStages(0, 100);
        System.out.println(stages);
        Optional<Stage> stage = stages.stream().filter(s ->
            s.getStageMeta().getId().equals("andy-ilyin-test-stage")).findAny();
        Assert.assertTrue(stage.isPresent());
        assertThat(stage.get().getStageSpec().getDeployUnitsOrThrow("DeployUnit1").getPodDeployPrimitiveCase(),
            isOneOf(PodDeployPrimitiveCase.values()));
    }

    @Test
    public void getProjects() throws ExecutionException, InterruptedException {
        List<Project> projects = DEPLOY_CLIENT.listProjects(0, 100);
        System.out.println(projects);
    }

    @Test
    public void getStagesByProjects() throws ExecutionException, InterruptedException {
        List<Project> projects = DEPLOY_CLIENT.listProjects(0, 100);
        Assert.assertTrue(projects.size() > 0);
        List<Stage> stages = DEPLOY_CLIENT.stagesByProject(0, 100, projects.get(0).getProjectMeta().getId());
        System.out.println(stages);
    }

    @Test
    public void updateStageSpec() throws ExecutionException, InterruptedException {
        StageSpec stageSpec = DEPLOY_CLIENT.getStageSpec(transportType, "andy-ilyin-test-stage");
        String deployUnit = "DeployUnit1";
        StageSpec.Builder newStageSpecBuilder = stageSpec.toBuilder();
        DeployUnitSpec.Builder deployUnitsBuilder =
            newStageSpecBuilder.getDeployUnitsOrThrow(deployUnit).toBuilder();

        deployUnitsBuilder.getReplicaSetBuilder()
            .getReplicaSetTemplateBuilder()
            .getPodTemplateSpecBuilder()
            .getSpecBuilder()
            .getPodAgentPayloadBuilder()
            .getSpecBuilder()
            .getWorkloadsBuilder(0)
            .getEnvBuilder(0)
            .getValueBuilder()
            .getLiteralEnvBuilder()
            .setValue("mySuperValue" + RandomStringUtils.randomAlphabetic(5));

        newStageSpecBuilder.putDeployUnits(deployUnit, deployUnitsBuilder.build());

        DEPLOY_CLIENT.updateStageSpec("andy-ilyin-test-stage", newStageSpecBuilder.build(), stageSpec);
    }

    @Test
    public void updateDeployUnit() throws ExecutionException, InterruptedException, TimeoutException {
        String stageId = "andy-ilyin-test-stage";
        StageSpec stageSpec = DEPLOY_CLIENT.getStageSpec(transportType, stageId);
        String deployUnit = "DeployUnit1";

        DeployUnitSpec.Builder deployUnitSpecBuilder = stageSpec.getDeployUnitsOrThrow(deployUnit).toBuilder();
        deployUnitSpecBuilder.getReplicaSetBuilder()
            .getReplicaSetTemplateBuilder()
            .getPodTemplateSpecBuilder()
            .getSpecBuilder()
            .getPodAgentPayloadBuilder()
            .getSpecBuilder()
            .getWorkloadsBuilder(0)
            .getEnvBuilder(0)
            .getValueBuilder()
            .getLiteralEnvBuilder()
            .setValue("mySuperValue" + RandomStringUtils.randomAlphabetic(5));

        YandexDeployClient.DeployUnitSpecUpdateResult updateResult = DEPLOY_CLIENT.updateDeployUnitSpecs(
            stageId, stageSpec,
            Collections.singletonMap(deployUnit, deployUnitSpecBuilder.build()));

        DEPLOY_CLIENT.waitForDeployUnitSpecsUpdateToComplete(transportType, stageId, updateResult,
            new TestDeployUnitSpecUpdateProgressListener(), Duration.ofHours(1), 3);
    }

    @Test
    public void getObjectHistory() throws ExecutionException, InterruptedException {
        YpObjectHistory<StageSpec> history = DEPLOY_CLIENT.getObjectHistory(transportType,
            "andy-ilyin-test-stage", Instant.now().minus(30, ChronoUnit.DAYS), Instant.now(), null);
        System.out.println(history);
    }

    private static class TestDeployUnitSpecUpdateProgressListener
        implements YandexDeployUnitSpecUpdateProgressListener {

        @Override
        public void progress(int podsReady, int podsTotal) {
            System.out.println("pods ready: " + podsReady + ", podsTotal: " + podsTotal);
        }

        @Override
        public void complete() {
            System.out.println("complete");
        }
    }
}
