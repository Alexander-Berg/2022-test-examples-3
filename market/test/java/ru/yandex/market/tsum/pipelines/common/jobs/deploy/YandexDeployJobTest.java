package ru.yandex.market.tsum.pipelines.common.jobs.deploy;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.yav.VaultClient;
import ru.yandex.market.tsum.clients.yav.model.VaultGetSecretResponse;
import ru.yandex.market.tsum.clients.yav.model.VaultResponseStatus;
import ru.yandex.market.tsum.clients.yav.model.VaultSecret;
import ru.yandex.market.tsum.clients.yav.model.VaultSecretVersion;
import ru.yandex.market.tsum.clients.yp.YandexDeployClient;
import ru.yandex.market.tsum.clients.yp.model.DeployUnitSpec;
import ru.yandex.market.tsum.clients.yp.model.DockerImageDescription;
import ru.yandex.market.tsum.clients.yp.model.PodSpec;
import ru.yandex.market.tsum.clients.yp.model.PodTemplateSpec;
import ru.yandex.market.tsum.clients.yp.model.ReplicaSetSpec;
import ru.yandex.market.tsum.clients.yp.model.Secret;
import ru.yandex.market.tsum.clients.yp.model.SecretRef;
import ru.yandex.market.tsum.clients.yp.model.StageSpec;
import ru.yandex.market.tsum.clients.yp.transport.TransportType;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipelines.common.jobs.rollback.YandexDeployRollbackType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
@ContextConfiguration(classes = {JobTesterConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class YandexDeployJobTest {

    public static final String FIRST_DEPLOY_UNIT = "first_deploy_unit";
    public static final String SECOND_DEPLOY_UNIT = "second_deploy_unit";
    public static final String FIRST_BOX_OF_FIRST_UNIT = "first_box_of_first_deploy_unit";
    public static final String SECOND_BOX_OF_SECOND_UNIT = "second_box_of_second_deploy_unit";

    private static final DateTimeFormatter TAG_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm");
    public static final String SOME_CUSTOM_TAG = "some_custom_tag";

    @Autowired
    private JobTester jobTester;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private final TransportType transportType;

    public YandexDeployJobTest(TransportType transportType) {
        this.transportType = transportType;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.stream(TransportType.values())
            .map(type -> new Object[]{type})
            .collect(Collectors.toList());
    }

    @Test
    public void dockerTagsUpdateTest() {
        YandexDeployJob job = Mockito.spy(YandexDeployJob.class);
        StageSpec spec = StageSpec.newBuilder(transportType).build();

        Assert.assertEquals(spec, job.updateSpecWithDockerImages(spec, Collections.emptyMap()));

        /* Creating spec with 2 units */
        spec = spec.toBuilder()
            .putDeployUnits(FIRST_DEPLOY_UNIT, getDeployUnitSpec(FIRST_DEPLOY_UNIT, transportType))
            .putDeployUnits(SECOND_DEPLOY_UNIT, getDeployUnitSpec(SECOND_DEPLOY_UNIT, transportType))
            .build();

        /* Retrieve a generated tag of the box */
        String tagInFirstBox = getTag(spec, FIRST_DEPLOY_UNIT, FIRST_BOX_OF_FIRST_UNIT);
        String tagInSecondBox = getTag(spec, SECOND_DEPLOY_UNIT, SECOND_BOX_OF_SECOND_UNIT);
        Assert.assertNotEquals(SOME_CUSTOM_TAG, tagInFirstBox);
        Assert.assertNotEquals(SOME_CUSTOM_TAG, tagInSecondBox);

        /* Updating the tag of the first box */
        StageSpec newSpec = job.updateSpecWithDockerImages(
            spec, getUnitToImages(FIRST_DEPLOY_UNIT, FIRST_BOX_OF_FIRST_UNIT, SOME_CUSTOM_TAG, transportType)
        );

        tagInFirstBox = getTag(newSpec, FIRST_DEPLOY_UNIT, FIRST_BOX_OF_FIRST_UNIT);
        String tagInSecondBoxShouldntBeUpdated = getTag(spec, SECOND_DEPLOY_UNIT, SECOND_BOX_OF_SECOND_UNIT);
        /* Checking that the tag of the first box was updated */
        Assert.assertEquals(SOME_CUSTOM_TAG, tagInFirstBox);
        /* Checking that the tag of the second box is the same */
        Assert.assertEquals(tagInSecondBox, tagInSecondBoxShouldntBeUpdated);
    }

    @Test
    public void secretsUpdateTest() {
        String vaultSecretId = "vaultSecretId";
        String vaultDelegationToken = "vaultDelegationToken";
        String deploySecretId = "deploySecretKey";
        String vaultSecretUuid = "vaultSecretUuid";
        String updatedSecretVersion = "updatedSecretVersion";

        VaultClient mockedVaultedClient = Mockito.mock(VaultClient.class);
        VaultGetSecretResponse vaultGetSecretResponse = new VaultGetSecretResponse();
        vaultGetSecretResponse.setStatus(VaultResponseStatus.OK);
        VaultSecret vaultSecret = new VaultSecret();
        VaultSecretVersion vaultSecretVersionGetSecret = new VaultSecretVersion();
        vaultSecretVersionGetSecret.setVersion(updatedSecretVersion);
        List<VaultSecretVersion> vaultSecretVersions = new ArrayList<>();
        vaultSecretVersions.add(vaultSecretVersionGetSecret);
        vaultSecret.setSecretVersions(vaultSecretVersions);
        vaultGetSecretResponse.setSecret(vaultSecret);
        when(mockedVaultedClient.getSecret(vaultSecretUuid)).thenReturn(vaultGetSecretResponse);
        SandboxClient sandboxClient = mock(SandboxClient.class);
        YandexDeployClient yandexDeployClient = Mockito.mock(YandexDeployClient.class);

        YandexDeployJob job = jobTester.jobInstanceBuilder(YandexDeployJob.class)
            .withBeanIfNotPresent(mockedVaultedClient)
            .withBeanIfNotPresent(yandexDeployClient)
            .withBeanIfNotPresent(sandboxClient)
            .withResource(new YandexDeployJobConfig("stageId", FIRST_DEPLOY_UNIT, Map.of(), 1,
                1, YandexDeployRollbackType.SpecRedeployment,
                false, false, true,
                new YandexDeployTransport(YandexDeployTransport.TransportSelection.valueOf(transportType.name())),
                List.of(new YandexDeployVaultSecret(deploySecretId, vaultSecretUuid))))
            .create();
        StageSpec spec = StageSpec.newBuilder(transportType).build();

        /* Creating spec with 2 units. Only the first one contains secret */
        DeployUnitSpec deployUnitSpec = getDeployUnitSpec(FIRST_DEPLOY_UNIT, transportType);
        DeployUnitSpec.ReplicaSetDeploy replicaSetDeploy = deployUnitSpec.getReplicaSet();
        ReplicaSetSpec replicaSetSpec = replicaSetDeploy.getReplicaSetTemplate();
        PodTemplateSpec podTemplateSpec = replicaSetSpec.getPodTemplateSpec();
        PodSpec podSpec = podTemplateSpec.getSpec();
        spec = spec.toBuilder()
            .putDeployUnits(FIRST_DEPLOY_UNIT, deployUnitSpec.toBuilder().setReplicaSet(
                    replicaSetDeploy.toBuilder().setReplicaSetTemplate(
                        replicaSetSpec.toBuilder().setPodTemplateSpec(
                            podTemplateSpec.toBuilder().setSpec(
                                podSpec.toBuilder().setSecret(
                                    deploySecretId, Secret.newBuilder(transportType)
                                        .setSecretId(vaultSecretId)
                                        .setSecretVersion("oldSecretVersion")
                                        .setDelegationToken(vaultDelegationToken)
                                        .build()
                                ).setSecretRef(
                                        deploySecretId, SecretRef.newBuilder(transportType)
                                                .setSecretId(vaultSecretId)
                                                .setSecretVersion("oldSecretVersion")
                                                .build()
                                ).build()
                            ).build()
                        ).build()
                    ).build()
                ).build()
            ).putDeployUnits(SECOND_DEPLOY_UNIT, getDeployUnitSpec(SECOND_DEPLOY_UNIT, transportType))
            .build();

        StageSpec newSpec = job.updateSecrets(spec);
        Secret secret = newSpec.getDeployUnitsOrThrow(FIRST_DEPLOY_UNIT).getReplicaSet()
            .getReplicaSetTemplate().getPodTemplateSpec().getSpec().getSecret(deploySecretId);
        Assert.assertEquals(vaultSecretId, secret.getSecretId());
        Assert.assertEquals(updatedSecretVersion, secret.getSecretVersion());
        Assert.assertEquals(vaultDelegationToken, secret.getDelegationToken());

        SecretRef secretRef = newSpec.getDeployUnitsOrThrow(FIRST_DEPLOY_UNIT).getReplicaSet()
                .getReplicaSetTemplate().getPodTemplateSpec().getSpec().getSecretRef(deploySecretId);
        Assert.assertEquals(vaultSecretId, secretRef.getSecretId());
        Assert.assertEquals(updatedSecretVersion, secretRef.getSecretVersion());
    }

    @Test
    public void lazyVaultClientInjectionTest() {
        SandboxClient sandboxClient = mock(SandboxClient.class);
        YandexDeployClient yandexDeployClient = Mockito.mock(YandexDeployClient.class);

        YandexDeployJob job = jobTester.jobInstanceBuilder(YandexDeployJob.class)
            .withBeanIfNotPresent(yandexDeployClient)
            .withBeanIfNotPresent(sandboxClient)
            .withResource(new YandexDeployJobConfig("stageId", FIRST_DEPLOY_UNIT, Map.of(), 1,
                1, YandexDeployRollbackType.SpecRedeployment,
                false, false, true,
                new YandexDeployTransport(YandexDeployTransport.TransportSelection.valueOf(transportType.name())),
                Collections.emptyList()))
            .create();
        StageSpec spec = StageSpec.newBuilder(transportType).build();

        spec = spec.toBuilder()
            .putDeployUnits(FIRST_DEPLOY_UNIT, getDeployUnitSpec(SECOND_DEPLOY_UNIT, transportType))
            .putDeployUnits(SECOND_DEPLOY_UNIT, getDeployUnitSpec(SECOND_DEPLOY_UNIT, transportType))
            .build();

        StageSpec newSpec = job.updateSecrets(spec);
        Assert.assertTrue(newSpec.getDeployUnitsOrThrow(FIRST_DEPLOY_UNIT).getReplicaSet().getReplicaSetTemplate()
            .getPodTemplateSpec().getSpec().getSecrets().isEmpty());
    }

    private String getTag(StageSpec spec, String unit, String box) {
        return spec.getDeployUnitsOrThrow(unit)
            .getImagesForBoxesOrThrow(box)
            .getTag();
    }

    private Map<String, Map<String, DockerImageDescription>> getUnitToImages(String unit, String box, String tag,
                                                                             TransportType transportType) {
        DockerImageDescription dockerImageDescription = getDockerImageDescription(box, tag, transportType);
        return Map.of(unit, Map.of(box, dockerImageDescription));
    }

    private DeployUnitSpec getDeployUnitSpec(String deployUnitName, TransportType transportType) {
        String dockerTag = LocalDateTime.now().format(TAG_FORMATTER);

        switch (deployUnitName) {
            case FIRST_DEPLOY_UNIT:
                return getDeployUnitSpec(dockerTag, FIRST_BOX_OF_FIRST_UNIT, transportType);

            case SECOND_DEPLOY_UNIT:
                return getDeployUnitSpec(dockerTag, SECOND_BOX_OF_SECOND_UNIT, transportType);

            default:
                throw new UnsupportedOperationException("unknown deploy unit");
        }
    }

    private DeployUnitSpec getDeployUnitSpec(String dockerTag, String boxId, TransportType transportType) {
        return DeployUnitSpec.newBuilder(transportType)
            .putImagesForBoxes(
                boxId,
                getDockerImageDescription(boxId, dockerTag, transportType)
            )
            .build();
    }

    private DockerImageDescription getDockerImageDescription(String boxId, String tag, TransportType transportType) {
        return DockerImageDescription.newBuilder(transportType)
            .setRegistryHost("registry.yandex.net")
            .setName("market/project/path/" + boxId)
            .setTag(tag)
            .build();
    }
}
