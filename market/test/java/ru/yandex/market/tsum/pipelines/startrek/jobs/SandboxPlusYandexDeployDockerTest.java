package ru.yandex.market.tsum.pipelines.startrek.jobs;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.SandboxTask;
import ru.yandex.market.tsum.clients.sandbox.SandboxTaskRunner;
import ru.yandex.market.tsum.clients.sandbox.TaskResource;
import ru.yandex.market.tsum.clients.yav.VaultClient;
import ru.yandex.market.tsum.clients.yp.YandexDeployClient;
import ru.yandex.market.tsum.clients.yp.model.Condition;
import ru.yandex.market.tsum.clients.yp.model.ConditionStatus;
import ru.yandex.market.tsum.clients.yp.model.DeployProgress;
import ru.yandex.market.tsum.clients.yp.model.DeployUnitSpec;
import ru.yandex.market.tsum.clients.yp.model.DeployUnitStatus;
import ru.yandex.market.tsum.clients.yp.model.DockerImageDescription;
import ru.yandex.market.tsum.clients.yp.model.RevisionInfo;
import ru.yandex.market.tsum.clients.yp.model.StageSpec;
import ru.yandex.market.tsum.clients.yp.model.StageStatus;
import ru.yandex.market.tsum.clients.yp.transport.TransportType;
import ru.yandex.market.tsum.context.TestTsumJobContext;
import ru.yandex.market.tsum.context.TsumJobContext;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobInstanceBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipelines.arcadia.resources.NewAndPreviousArcadiaRefs;
import ru.yandex.market.tsum.pipelines.common.jobs.deploy.YandexDeployJob;
import ru.yandex.market.tsum.pipelines.common.jobs.deploy.YandexDeployTransport;
import ru.yandex.market.tsum.pipelines.common.jobs.sandbox.SandboxTaskJob;
import ru.yandex.market.tsum.pipelines.common.resources.ArcadiaRef;
import ru.yandex.market.tsum.pipelines.common.resources.FixVersion;
import ru.yandex.market.tsum.pipelines.common.resources.ReleaseInfo;
import ru.yandex.market.tsum.pipelines.startrek.config.StartrekDeployEnvironmentConfig;
import ru.yandex.market.tsum.pipelines.startrek.config.StartrekDockerConfig;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
@ContextConfiguration(classes = JobTesterConfig.class)
public class SandboxPlusYandexDeployDockerTest {
    private static final String DOCKER_TAG_OLD_VERSION = "2020.01";
    private static final String DOCKER_TAG_NEW_VERSION = "2020.02";
    private static final String DOCKER_TAG_HOST = "registry.yandex.net";
    private static final String DOCKER_TAG_PATH = "tools/startrek-coordinator";
    private static final String DOCKER_TAG = DOCKER_TAG_HOST + '/' + DOCKER_TAG_PATH + ':' + DOCKER_TAG_NEW_VERSION;

    private static final String DEPLOY_STAGE_ID = "tools_startrek-api_coordinator-dev";
    private static final String DEPLOY_UNIT = "app_unit";
    private static final String DEPLOY_BOX = "app_box";
    private static final int DEPLOY_STAGE_SPEC_OLD_REVISION = 42;
    private static final int DEPLOY_STAGE_SPEC_NEW_REVISION = DEPLOY_STAGE_SPEC_OLD_REVISION + 1;
    private static final int DEPLOY_UNIT_OLD_REVISION = 100;
    private static final int DEPLOY_UNIT_NEW_REVISION = DEPLOY_UNIT_OLD_REVISION + 1;

    @Autowired
    private JobTester jobTester;

    private SandboxClient sandboxClient;

    private YandexDeployClient yandexDeployClient;

    private VaultClient vaultClient;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private final YandexDeployTransport transportTypeResource;
    private final TransportType transportType;

    public SandboxPlusYandexDeployDockerTest(YandexDeployTransport transportTypeResource) {
        this.transportTypeResource = transportTypeResource;
        this.transportType = YandexDeployTransport.calculateTransportType(transportTypeResource);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        YandexDeployTransport nullSelection = new YandexDeployTransport(null);
        Stream<YandexDeployTransport> nonNullSelection = Stream.of(YandexDeployTransport.TransportSelection.values())
            .map(YandexDeployTransport::new);
        return Stream.concat(Stream.of(null, nullSelection), nonNullSelection)
            .map(resource -> new Object[]{resource})
            .collect(Collectors.toList());
    }

    @Before
    public void setUp() throws Exception {
        TestContextManager manager = new TestContextManager(SandboxPlusYandexDeployDockerTest.class);
        manager.prepareTestInstance(this);
        SandboxTask sandboxTask = mock(SandboxTask.class);
        doReturn("SUCCESS")
            .when(sandboxTask).getStatus();
        doReturn(42L)
            .when(sandboxTask).getId();

        SandboxTaskRunner sandboxTaskRunner = mock(SandboxTaskRunner.class);
        doReturn(sandboxTask)
            .when(sandboxTaskRunner).run();
        doReturn(sandboxTaskRunner)
            .when(sandboxTaskRunner).withTaskInput(any());
        doReturn(sandboxTaskRunner)
            .when(sandboxTaskRunner).withJobTaskTags(any());
        doReturn(sandboxTaskRunner)
            .when(sandboxTaskRunner).withListener(any());
        doReturn(sandboxTaskRunner)
            .when(sandboxTaskRunner).withMaxExecutionDuration(any());
        doReturn(sandboxTaskRunner)
            .when(sandboxTaskRunner).withRetryOnExceptionCount(anyInt());

        TaskResource taskResource = mock(TaskResource.class);
        doReturn("YA_PACKAGE")
            .when(taskResource).getType();
        doReturn(sandboxTask)
            .when(taskResource).getTask();
        doReturn(DOCKER_TAG)
            .when(taskResource).getAttribute(eq("resource_version"));

        sandboxClient = mock(SandboxClient.class);
        doReturn(List.of(taskResource))
            .when(sandboxClient).getResources(anyLong());
        doReturn(sandboxTaskRunner)
            .when(sandboxClient).newSandboxTaskRunner();


        StageSpec stageSpec = StageSpec.newBuilder(transportType)
            .putDeployUnits(DEPLOY_UNIT, getDeployUnitSpec(DOCKER_TAG_OLD_VERSION, transportType))
            .setRevision(DEPLOY_STAGE_SPEC_OLD_REVISION)
            .build();

        DeployProgress deployProgress = DeployProgress.newBuilder(transportType)
            .setPodsReady(1)
            .setPodsTotal(1)
            .build();

        DeployUnitStatus deployUnitStatus = DeployUnitStatus.newBuilder(transportType)
            .setTargetRevision(DEPLOY_UNIT_NEW_REVISION)
            .setReady(Condition.newBuilder(transportType).setStatus(ConditionStatus.CS_TRUE).build())
            .setProgress(deployProgress)
            .build();

        StageStatus stageStatus = StageStatus.newBuilder(transportType)
            .putDeployUnits(DEPLOY_UNIT, deployUnitStatus)
            .build();

        yandexDeployClient = mock(YandexDeployClient.class);
        doReturn(stageSpec)
            .when(yandexDeployClient).getStageSpec(eq(transportType), eq(DEPLOY_STAGE_ID));
        doThrow(new IllegalArgumentException())
            .when(yandexDeployClient).getStageSpec(not(eq(transportType)), eq(DEPLOY_STAGE_ID));
        doReturn(stageStatus)
            .when(yandexDeployClient).getStageStatus(eq(transportType), eq(DEPLOY_STAGE_ID));
        doThrow(new IllegalArgumentException())
            .when(yandexDeployClient).getStageStatus(not(eq(transportType)), eq(DEPLOY_STAGE_ID));
        doReturn(DEPLOY_STAGE_SPEC_NEW_REVISION)
            .when(yandexDeployClient).updateStageSpec(eq(DEPLOY_STAGE_ID), any(StageSpec.class), eq(stageSpec));
        vaultClient = mock(VaultClient.class);
    }

    @Test
    public void dockerTagsUpdateTest() throws Exception {
        var context = new TestTsumJobContext("test-user");

        var sandboxConfigJob = getSandboxConfigJob();
        sandboxConfigJob.execute(context);

        var sandboxTaskJob = getSandboxTaskJob(context);
        sandboxTaskJob.execute(context);

        var deployDockerConfigJob = getDeployDockerConfigJob(context, transportTypeResource);
        deployDockerConfigJob.execute(context);

        var deployJob = getDeployJob(context);
        deployJob.execute(context);
        ArgumentCaptor<StageSpec> newSpec = ArgumentCaptor.forClass(StageSpec.class);
        verify(yandexDeployClient).updateStageSpec(eq(DEPLOY_STAGE_ID), newSpec.capture(), any(StageSpec.class));

        StageSpec expectedSpec = StageSpec.newBuilder(transportType)
            .putDeployUnits(DEPLOY_UNIT, getDeployUnitSpec(DOCKER_TAG_NEW_VERSION, transportType))
            //we send to YD the old revision, but a new docker tag. YD actually ignores the revision in the updated spec
            .setRevision(DEPLOY_STAGE_SPEC_OLD_REVISION)
            .setRevisionInfo(RevisionInfo.newBuilder(transportType)
                .setDescription("Release " + context.getPipeLaunchUrl()).build())
            .build();

        assertEquals(expectedSpec, newSpec.getValue());
    }

    private TrackerSandboxDockerOnlyConfigJob getSandboxConfigJob() {
        return jobTester.jobInstanceBuilder(TrackerSandboxDockerOnlyConfigJob.class)
            .withResources(
                new ReleaseInfo(new FixVersion(0L, DOCKER_TAG_NEW_VERSION)),
                new StartrekDockerConfig("tracker/startrek-coordinator/package-dev.json", "tools"),
                new NewAndPreviousArcadiaRefs(new ArcadiaRef(), new ArcadiaRef())
            )
            .create();
    }

    private SandboxTaskJob getSandboxTaskJob(TsumJobContext context) throws Exception {
        return jobTester.jobInstanceBuilder(SandboxTaskJob.class)
            .withBeanIfNotPresent(sandboxClient)
            .withResources(context.resources().getProducedResources().toArray(Resource[]::new))
            .create();
    }

    private DeployDockerConfigJob getDeployDockerConfigJob(TsumJobContext context,
                                                           YandexDeployTransport transportTypeResource) {
        JobInstanceBuilder<DeployDockerConfigJob> builder = jobTester.jobInstanceBuilder(DeployDockerConfigJob.class)
            .withBeanIfNotPresent(sandboxClient)
            .withResources(context.resources().getProducedResources().toArray(Resource[]::new))
            .withResource(new StartrekDeployEnvironmentConfig(
                DEPLOY_BOX, DEPLOY_STAGE_ID,
                DEPLOY_UNIT, 48, 10, Collections.emptyList())
            );

        if (transportTypeResource != null) {
            builder.withResource(transportTypeResource);
        }

        return builder.create();
    }

    private YandexDeployJob getDeployJob(TestTsumJobContext context) {
        return jobTester.jobInstanceBuilder(YandexDeployJob.class)
            .withBeanIfNotPresent(sandboxClient)
            .replaceBean(yandexDeployClient)
            .withBeanIfNotPresent(vaultClient)
            .withResources(context.resources().getProducedResources().toArray(Resource[]::new)).create();
    }

    private DeployUnitSpec getDeployUnitSpec(String version, TransportType transportType) {
        return DeployUnitSpec.newBuilder(transportType)
            .putImagesForBoxes(
                DEPLOY_BOX,
                DockerImageDescription.newBuilder(transportType)
                    .setRegistryHost("registry.yandex.net")
                    .setName(DOCKER_TAG_PATH)
                    .setTag(version)
                    .build()
            )
            .setRevision(DEPLOY_UNIT_OLD_REVISION)
            .build();
    }

}
