package ru.yandex.market.tsum.pipelines.common.jobs.nanny;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.Timestamp;
import nanny.pod_sets_api.PodSetsApi;
import nanny.repo.Repo;
import nanny.repo_api.RepoApi;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.request.netty.retry.RetryIdempotentWithSleepPolicy;
import ru.yandex.market.tsum.clients.gencfg.GenCfgGroupCard;
import ru.yandex.market.tsum.clients.gencfg.GenCfgInstance;
import ru.yandex.market.tsum.clients.gencfg.GenCfgLocation;
import ru.yandex.market.tsum.clients.nanny.NannyRepoApiClient;
import ru.yandex.market.tsum.clients.nanny.NannyYpApiClient;
import ru.yandex.market.tsum.clients.nanny.UntypedNannyClient;
import ru.yandex.market.tsum.clients.nanny.service.untypedservice.AuthAttrs;
import ru.yandex.market.tsum.clients.nanny.service.untypedservice.InfoAttrs;
import ru.yandex.market.tsum.clients.nanny.service.untypedservice.RuntimeAttrs;
import ru.yandex.market.tsum.clients.nanny.service.untypedservice.UntypedNannyService;
import ru.yandex.market.tsum.multitesting.YpAllocationParams;
import ru.yandex.market.tsum.pipelines.common.resources.GenCfgGroup;
import ru.yandex.market.tsum.pipelines.common.resources.PipelineEnvironment;
import ru.yandex.market.tsum.pipelines.common.resources.SandboxResource;
import ru.yandex.yp.client.api.Autogen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.tsum.pipelines.common.jobs.nanny.NannyCreateServiceAndDeployJob.CLEANUP_POLICY_SNAPSHOT_COUNT;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 22.05.2018
 */
public class NannyCreateServiceAndDeployJobTest {
    private static JsonObject createRecipesArrayWithOneRecipe() {
        JsonObject recipe = new JsonObject();
        recipe.add("context", new JsonArray());

        JsonArray content = new JsonArray();
        content.add(recipe);

        JsonObject recipes = new JsonObject();
        recipes.add("content", content);
        return recipes;
    }

    private static JsonObject runtimeAttrsWithSandboxFiles(JsonObject... files) {
        JsonArray sandboxFiles = new JsonArray();
        Stream.of(files).forEach(sandboxFiles::add);

        JsonObject resources = new JsonObject();
        resources.add("sandbox_files", sandboxFiles);

        JsonObject result = new JsonObject();
        result.add("resources", resources);

        return result;
    }

    private static JsonObject sandboxFile(String resourceType) {
        JsonObject result = new JsonObject();
        result.addProperty("resource_type", resourceType);
        return result;
    }

    @Test
    public void modifyInfoAttrs() {
        JsonObject content = new JsonObject();
        content.addProperty("tickets_integration", "");
        content.addProperty("category", "/market/something");
        content.add("recipes", createRecipesArrayWithOneRecipe());

        NannyCreateServiceAndDeployJob.modifyInfoAttrs(new InfoAttrs(null, content), true);

        assertNull(content.get("tickets_integration"));
        assertEquals("/market/multitesting/something", content.get("category").getAsString());
        JsonArray actualRecipesContent = content.getAsJsonObject("recipes").getAsJsonArray("content");
        JsonArray contextOfFirstRecipe = actualRecipesContent.get(0).getAsJsonObject().get("context").getAsJsonArray();
        assertEquals("force", contextOfFirstRecipe.get(0).getAsJsonObject().get("key").getAsString());
        assertTrue(contextOfFirstRecipe.get(0).getAsJsonObject().get("value").getAsBoolean());
        assertEquals(NannyCreateServiceAndDeployJob.MARKET_MULTITESTINGS_ABC_GROUP_ID,
            content.get("abc_group").getAsInt());
    }

    @Test
    public void modifyAuthAttrs() {
        String login = "some_login";
        JsonArray expectedLogins = new JsonArray();
        expectedLogins.add(login);
        String groupName = "some_group";
        JsonArray expectedGroups = new JsonArray();
        expectedGroups.add(groupName);

        JsonObject content = new JsonObject();
        NannyCreateServiceAndDeployJob.modifyAuthAttrs(new AuthAttrs(null, content), Collections.singletonList(login),
            Collections.singletonList(groupName));

        Consumer<String> checker = s -> {
            assertEquals(expectedLogins, content.getAsJsonObject(s).getAsJsonArray("logins"));
            assertEquals(expectedGroups, content.getAsJsonObject(s).getAsJsonArray("groups"));
        };
        checker.accept("owners");
        checker.accept("ops_managers");
        checker.accept("conf_managers");
    }

    @Test
    public void modifyRuntimeAttrs() {
        JsonObject content = new JsonObject();
        content.add("engines", new JsonObject());
        JsonObject instances = new JsonObject();
        instances.add("extended_gencfg_groups", new JsonObject());
        content.add("instances", instances);

        GenCfgGroup genCfgGroup = new GenCfgGroup(
            "name",
            null,
            new ru.yandex.market.tsum.pipelines.common.resources.GenCfgGroupInfo(
                "name",
                "some-tag",
                null,
                null,
                null,
                Collections.singletonList(new GenCfgInstance(
                    null,
                    null,
                    GenCfgLocation.VLA,
                    0,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                ))
            )
        );

        NannyCreateServiceAndDeployJob.modifyRuntimeAttrs(new RuntimeAttrs(null, content), genCfgGroup, false);

        assertEquals(GenCfgLocation.VLA.getIssEngine(),
            content.getAsJsonObject("engines").get("engine_type").getAsString());
        assertEquals("EXTENDED_GENCFG_GROUPS", content.getAsJsonObject("instances").get("chosen_type").getAsString());

        JsonObject actualGenCfgGroup = content.getAsJsonObject("instances").getAsJsonObject("extended_gencfg_groups")
            .getAsJsonArray("groups").get(0).getAsJsonObject();
        assertEquals(genCfgGroup.getName(), actualGenCfgGroup.get("name").getAsString());
        assertEquals("tags/some-tag", actualGenCfgGroup.get("release").getAsString());

        assertTrue(content.getAsJsonObject("instances").getAsJsonObject("extended_gencfg_groups")
            .getAsJsonObject("network_settings").get("use_mtn").getAsBoolean());
    }

    @Test
    public void modifyRuntimeAttrsWithPod() {
        NannyCreateServiceAndDeployJob job = new NannyCreateServiceAndDeployJob();
        job.setConfig(NannyCreateServiceAndDeployJobConfig.builder()
            .withUseYpLite(true)
            .build());

        UntypedNannyClient nannyClient = Mockito.mock(UntypedNannyClient.class);
        job.setNannyUntyped(nannyClient);

        String serviceName = "service_name";
        NannyRepoApiClient nannyRepoApiClient = Mockito.mock(NannyRepoApiClient.class);
        Mockito.when(nannyRepoApiClient.getCleanupPolicy(serviceName)).thenReturn(null);
        job.setNannyRepoApiClient(nannyRepoApiClient);

        String podId = "pod_id";
        NannyYpApiClient ypApiClient = Mockito.mock(NannyYpApiClient.class);
        Mockito.when(ypApiClient.listPods(any(), any())).thenReturn(
            PodSetsApi.ListPodsResponse.newBuilder()
                .addPods(
                    Autogen.TPod.newBuilder()
                        .setMeta(Autogen.TPodMeta.newBuilder().setId(podId).build()).build())
                .build()
        );
        job.setNannyYpApiClient(ypApiClient);

        JsonObject content = new JsonObject();
        content.add("engines", new JsonObject());
        JsonObject instances = new JsonObject();
        instances.add("extended_gencfg_groups", new JsonObject());
        content.add("instances", instances);

        JsonArray bind = new JsonArray();
        bind.add(RuntimeAttrs.createBindObject("/place/db/www/logs"));
        bind.add(RuntimeAttrs.createBindObject("/place/db/bsconfig/webcache"));
        bind.add(RuntimeAttrs.createBindObject("/place/db/bsconfig/webstate"));
        JsonObject layersConfig = new JsonObject();
        layersConfig.add("bind", bind);
        JsonObject instanceSpec = new JsonObject();
        instanceSpec.add("layersConfig", layersConfig);
        content.add("instance_spec", instanceSpec);

        String defaultLocation = "IVA";

        String itype = "itype_from_gencfg";
        String ctype = "testing";
        String prj = "tsum";
        String metaprj = "metaprj_from_gencfg";

        GenCfgGroupCard genCfgGroupCard = new GenCfgGroupCard();
        Map<String, Object> tags = new HashMap<>();
        tags.put("itype", itype);
        tags.put("ctype", ctype);
        tags.put("metaprj", metaprj);
        genCfgGroupCard.setTags(tags);
        Map<String, String> instanceYpPodTagsMap = tags.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> (String) e.getValue()));

        UntypedNannyService nannyService = new UntypedNannyService(
            serviceName, null, new RuntimeAttrs(null, content), null
        );

        NannyCreateServiceAndDeployJob.GenCfgService genCfgService = new NannyCreateServiceAndDeployJob.GenCfgService();
        genCfgService.setLocation(defaultLocation);
        genCfgService.setGenCfgGroupInfo(null);
        genCfgService.setGenCfgGroupCard(genCfgGroupCard);
        genCfgService.setCpuMillis(1);
        genCfgService.setMemoryMb(1024);
        genCfgService.setRootFsQuotaGb(1);
        genCfgService.setWorkDirQuotaGb(1);
        List<YpAllocationParams.Volume> volumes = new ArrayList<>();
        volumes.add(new YpAllocationParams.Volume("/logs", 1024));
        genCfgService.setVolumes(volumes);

        job.modifyRuntimeAttrsWithPod(
            nannyService,
            new NannyCreateServiceAndDeployJob.TargetServiceInfo(defaultLocation, serviceName),
            false,
            genCfgService,
            "service created",
            null,
            instanceYpPodTagsMap
        );

        assertEquals("YP_LITE", content.getAsJsonObject("engines").get("engine_type").getAsString());
        assertEquals("YP_POD_IDS", content.getAsJsonObject("instances").get("chosen_type").getAsString());

        JsonObject pod = content.getAsJsonObject("instances").getAsJsonObject("yp_pod_ids")
            .getAsJsonArray("pods").get(0).getAsJsonObject();
        assertEquals(podId, pod.get("pod_id").getAsString());
        assertEquals(defaultLocation, pod.get("cluster").getAsString());

        JsonObject orthogonalTags = content.getAsJsonObject("instances").getAsJsonObject("yp_pod_ids")
            .getAsJsonObject("orthogonal_tags");
        assertEquals(itype, orthogonalTags.get("itype").getAsString());
        assertEquals(ctype, orthogonalTags.get("ctype").getAsString());
        assertEquals(metaprj, orthogonalTags.get("metaprj").getAsString());
        assertEquals(prj, orthogonalTags.get("prj").getAsString());
        assertEquals("[]",
            content.getAsJsonObject("instance_spec").getAsJsonObject("layersConfig").get("bind").toString());
    }

    @Test
    public void ensureProperCleanupPolicy_correctPolicyToBeginWith() {
        NannyCreateServiceAndDeployJob job = new NannyCreateServiceAndDeployJob();
        job.setConfig(NannyCreateServiceAndDeployJobConfig.builder()
            .withUseYpLite(true)
            .build());

        String serviceName = "service_name";

        NannyRepoApiClient nannyRepoApiClient = Mockito.mock(NannyRepoApiClient.class);
        Mockito.when(nannyRepoApiClient.getCleanupPolicy(serviceName)).thenReturn(
            RepoApi.GetCleanupPolicyResponse.newBuilder()
                .setPolicy(Repo.CleanupPolicy.newBuilder()
                    .setSpec(Repo.CleanupPolicySpec.newBuilder()
                        .setType(Repo.CleanupPolicySpec.PolicyType.SIMPLE_COUNT_LIMIT)
                        .setSimpleCountLimit(Repo.CleanupPolicySimpleCountLimit.newBuilder()
                            .setSnapshotsCount(CLEANUP_POLICY_SNAPSHOT_COUNT)
                            .setDisposableCount(0)
                            .setStalledTtl("PT24H"))))
                .build());
        job.setNannyRepoApiClient(nannyRepoApiClient);

        job.ensureProperCleanupPolicy(serviceName);

        Mockito.verify(nannyRepoApiClient, Mockito.never()).updateCleanupPolicy(any());
    }

    @Test
    public void ensureProperCleanupPolicy_noPolicyToBeginWith() {
        NannyCreateServiceAndDeployJob job = new NannyCreateServiceAndDeployJob();
        job.setConfig(NannyCreateServiceAndDeployJobConfig.builder()
            .withUseYpLite(true)
            .build());

        String serviceName = "service_name";

        NannyRepoApiClient nannyRepoApiClient = Mockito.mock(NannyRepoApiClient.class);
        Mockito.when(nannyRepoApiClient.getCleanupPolicy(serviceName)).thenReturn(null);
        job.setNannyRepoApiClient(nannyRepoApiClient);

        job.ensureProperCleanupPolicy(serviceName);

        Mockito.verify(nannyRepoApiClient, Mockito.never()).updateCleanupPolicy(any());
    }

    @Test
    public void ensureProperCleanupPolicy_fixPolicy() {
        NannyCreateServiceAndDeployJob job = new NannyCreateServiceAndDeployJob();
        job.setConfig(NannyCreateServiceAndDeployJobConfig.builder()
            .withUseYpLite(true)
            .build());

        String serviceName = "service_name";
        Timestamp.Builder ts = Timestamp.newBuilder().setSeconds(1589450445);
        Repo.CleanupPolicyMeta cleanupPolicyMeta = Repo.CleanupPolicyMeta.newBuilder()
            .setAuthor("robot-market-infra")
            .setGeneration(167)
            .setLastModificationTime(ts)
            .setCreationTime(ts)
            .setVersion("31eded96-60c8-4631-ba8d-9e6b70ceed7a")
            .setId(serviceName)
            .build();

        NannyRepoApiClient nannyRepoApiClient = Mockito.mock(NannyRepoApiClient.class);
        Mockito.when(nannyRepoApiClient.getCleanupPolicy(serviceName))
            .thenReturn(RepoApi.GetCleanupPolicyResponse.newBuilder()
                .setPolicy(Repo.CleanupPolicy.newBuilder().setMeta(cleanupPolicyMeta).build())
                .build());
        job.setNannyRepoApiClient(nannyRepoApiClient);

        job.ensureProperCleanupPolicy(serviceName);

        Mockito.verify(nannyRepoApiClient).getCleanupPolicy(serviceName);
        Mockito.verify(nannyRepoApiClient).updateCleanupPolicy(RepoApi.UpdateCleanupPolicyRequest.newBuilder()
            .setMeta(cleanupPolicyMeta)
            .setSpec(Repo.CleanupPolicySpec.newBuilder()
                .setSimpleCountLimit(Repo.CleanupPolicySimpleCountLimit.newBuilder()
                    .setSnapshotsCount(CLEANUP_POLICY_SNAPSHOT_COUNT)
                    .setDisposableCount(0)
                    .setStalledTtl("PT24H")
                    .build()))
            .build());
    }

    @Test
    @Ignore("integration test")
    public void ensureProperCleanupPolicy_integrationWithRealNanny() {
        NannyCreateServiceAndDeployJob job = new NannyCreateServiceAndDeployJob();
        job.setConfig(NannyCreateServiceAndDeployJobConfig.builder()
            .withUseYpLite(true)
            .build());

        String serviceName = "mt_mbo--mbo01_56194070_sas";

        HttpClientConfig config = new HttpClientConfig();
        config.setRetryPolicy(new RetryIdempotentWithSleepPolicy(5, 5000));
        NannyRepoApiClient nannyRepoApiClient = new NannyRepoApiClient("http://nanny.yandex-team.ru",
            // NB это неправильный токен, получить правильный можно здесь:
            // https://nanny.yandex-team.ru/ui/#/oauth/
            // (коммитить его не надо)
            "********",
            new NettyHttpClientContext(config));
        job.setNannyRepoApiClient(nannyRepoApiClient);

        job.ensureProperCleanupPolicy(serviceName);

        // а потом можно проверить, что сохранилось, здесь:
        // https://nanny.yandex-team.ru/api/repo/GetCleanupPolicy/?policyId=mt_mbo--mbo01_56194070_sas
    }

    @Test(expected = IllegalStateException.class)
    public void modifyResources_shouldUpdateAtLeastOneResourceTrue_noSandboxResources() {
        NannyUtils.modifyResources(
            new RuntimeAttrs(null, runtimeAttrsWithSandboxFiles(sandboxFile("RESOURCE_TYPE"))),
            Collections.emptyList(),
            true
        );
    }

    @Test(expected = IllegalStateException.class)
    public void modifyResources_shouldUpdateAtLeastOneResourceTrue_noMatchingSandboxResources() {
        NannyUtils.modifyResources(
            new RuntimeAttrs(null, runtimeAttrsWithSandboxFiles(sandboxFile("RESOURCE_TYPE1"))),
            Collections.singletonList(new SandboxResource(null, 0L, "RESOURCE_TYPE2", 0L)),
            true
        );
    }

    @Test
    public void modifyResources_shouldUpdateAtLeastOneResourceFalse_noErrors() {
        NannyUtils.modifyResources(
            new RuntimeAttrs(null, runtimeAttrsWithSandboxFiles(sandboxFile("RESOURCE_TYPE"))),
            Collections.emptyList(),
            false
        );
        NannyUtils.modifyResources(
            new RuntimeAttrs(null, runtimeAttrsWithSandboxFiles(sandboxFile("RESOURCE_TYPE1"))),
            Collections.singletonList(new SandboxResource(null, 0L, "RESOURCE_TYPE2", 0L)),
            false
        );
    }

    @Test
    public void modifyResources_oneUpdatedResource() {
        JsonObject content = runtimeAttrsWithSandboxFiles(sandboxFile("RESOURCE_TYPE"));

        SandboxResource expectedResource = new SandboxResource("TASK_TYPE", 123L, "RESOURCE_TYPE", 456L);
        NannyUtils.modifyResources(
            new RuntimeAttrs(null, content),
            Collections.singletonList(expectedResource),
            true
        );

        JsonObject actualResource =
            content.getAsJsonObject("resources").getAsJsonArray("sandbox_files").get(0).getAsJsonObject();
        assertEquals(String.valueOf(expectedResource.getTaskId()), actualResource.get("task_id").getAsString());
        assertEquals(expectedResource.getResourceType(), actualResource.get("resource_type").getAsString());
        assertEquals(String.valueOf(expectedResource.getId()), actualResource.get("resource_id").getAsString());
    }

    @Test
    public void modifyResources_replaceDatasources() {
        JsonObject content = runtimeAttrsWithSandboxFiles(sandboxFile("MARKET_DATASOURCES_TESTING"));

        SandboxResource expectedResource = new SandboxResource("TASK_TYPE", 123L, "MARKET_DATASOURCES_MULTITESTING",
            456L);
        NannyUtils.modifyResources(
            new RuntimeAttrs(null, content),
            Collections.singletonList(expectedResource),
            true
        );

        JsonObject actualResource =
            content.getAsJsonObject("resources").getAsJsonArray("sandbox_files").get(0).getAsJsonObject();
        assertEquals(String.valueOf(expectedResource.getTaskId()), actualResource.get("task_id").getAsString());
        assertEquals(expectedResource.getResourceType(), actualResource.get("resource_type").getAsString());
        assertEquals(String.valueOf(expectedResource.getId()), actualResource.get("resource_id").getAsString());
    }

    @Test
    public void modifyResources_addDatasources() {
        JsonObject content = runtimeAttrsWithSandboxFiles();

        SandboxResource expectedResource = new SandboxResource("TASK_TYPE", 123L, "MARKET_DATASOURCES_MULTITESTING",
            456L);
        NannyUtils.modifyResources(
            new RuntimeAttrs(null, content),
            Collections.singletonList(expectedResource),
            true
        );

        JsonObject actualResource =
            content.getAsJsonObject("resources").getAsJsonArray("sandbox_files").get(0).getAsJsonObject();
        assertEquals(String.valueOf(expectedResource.getTaskId()), actualResource.get("task_id").getAsString());
        assertEquals(expectedResource.getResourceType(), actualResource.get("resource_type").getAsString());
        assertEquals(String.valueOf(expectedResource.getId()), actualResource.get("resource_id").getAsString());
    }

    @Test
    public void modifyResources_addStaticFile() {
        JsonObject content = runtimeAttrsWithSandboxFiles();

        NannyStaticFile expectedFile = new NannyStaticFile("path", "content");
        NannyCreateServiceAndDeployJob.modifyStaticFiles(
            new RuntimeAttrs(null, content),
            Collections.singletonList(expectedFile)
        );

        JsonObject actualResource =
            content.getAsJsonObject("resources").getAsJsonArray("static_files").get(0).getAsJsonObject();
        assertEquals(expectedFile.getLocalPath(), actualResource.get("local_path").getAsString());
        assertEquals(expectedFile.getContent(), actualResource.get("content").getAsString());
    }

    @Test
    public void getEnvironmentNameFromNannyServiceName() {
        String mtEnvironmentId = "test_environment";
        String serviceName = NannyCreateServiceAndDeployJob.getServiceName(
            "testJob", "sas", new PipelineEnvironment(mtEnvironmentId, null, null)
        );

        assertEquals(
            mtEnvironmentId,
            NannyCreateServiceAndDeployJob.getEnvironmentNameFromNannyServiceName(serviceName)
        );
    }
}
