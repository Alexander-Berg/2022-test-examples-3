package ru.yandex.market.tsum.registry.v2.tasks.autodiscovery;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.config.GlobalClientsConfig;
import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.TaskResource;
import ru.yandex.market.tsum.clients.yp.model.AttributeDictionary;
import ru.yandex.market.tsum.clients.yp.model.DeployUnitSpec;
import ru.yandex.market.tsum.clients.yp.model.Layer;
import ru.yandex.market.tsum.clients.yp.model.PodAgentSpec;
import ru.yandex.market.tsum.clients.yp.model.PodSpec;
import ru.yandex.market.tsum.clients.yp.model.PodTemplateSpec;
import ru.yandex.market.tsum.clients.yp.model.ReplicaSetSpec;
import ru.yandex.market.tsum.clients.yp.model.ResourceGang;
import ru.yandex.market.tsum.clients.yp.model.Stage;
import ru.yandex.market.tsum.clients.yp.model.StageMeta;
import ru.yandex.market.tsum.clients.yp.model.StageSpec;
import ru.yandex.market.tsum.clients.yp.transport.TransportType;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.registry.v2.dao.ComponentsDao;
import ru.yandex.market.tsum.registry.v2.dao.InstallationsDao;
import ru.yandex.market.tsum.registry.v2.dao.YandexDeployAutoDiscoveryDao;
import ru.yandex.market.tsum.registry.v2.dao.model.Component;
import ru.yandex.market.tsum.registry.v2.dao.model.Installation;

import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.core.TestResourceLoader.getTestResourceAsString;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class, GlobalClientsConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class YandexDeployAutoDiscoveryWorkerTest {

    private static final String RESOURCE_DIRECTORY_PATH = "registry/v2/tasks/autodiscovery/yp/";
    private static final String COMPONENTS = "components.json";
    private static final String INSTALLATIONS = "installations.json";

    private static final Gson GSON = new GsonBuilder().create();

    @Autowired
    private MongoTemplate mongoTemplate;

    @Mock
    private SandboxClient sandboxClient;

    private YandexDeployAutoDiscoveryWorker discoveryWorker;
    private ComponentsDao componentsDao;
    private InstallationsDao installationsDao;
    private YandexDeployAutoDiscoveryDao discoveryDao;

    private List<Component> components;
    private List<Installation> installations;

    private static final Map<Long, String> RESOURCE_MAP = Map.of(
        2455918660L, "MARKET_MBI_FEED_PROCESSOR",
        2254249580L, "MARKET_DEPLOY_LOGKEEPER_BINARY",
        2187761794L, "PORTO_LAYER_MARKET_INFRA_DEPLOY_XENIAL",
        123456L, ""
    );

    @Before
    public void setUp() throws IOException {
        this.installationsDao = new InstallationsDao(mongoTemplate);
        this.componentsDao = new ComponentsDao(mongoTemplate, installationsDao);
        this.discoveryDao = new YandexDeployAutoDiscoveryDao(mongoTemplate);
        sandboxClient = Mockito.mock(SandboxClient.class);

        this.discoveryWorker = new YandexDeployAutoDiscoveryWorker(componentsDao, installationsDao,
            discoveryDao, sandboxClient, null, "", "", true);

        Type componentList = new TypeToken<List<Component>>() { }.getType();
        components = GSON.fromJson(getTestResourceAsString(RESOURCE_DIRECTORY_PATH + COMPONENTS), componentList);

        Type instList = new TypeToken<List<Installation>>() { }.getType();
        installations = GSON.fromJson(getTestResourceAsString(RESOURCE_DIRECTORY_PATH + INSTALLATIONS), instList);

        for (Long key : RESOURCE_MAP.keySet()) {
            when(sandboxClient.getResourceWithNotFoundValidation(key)).thenReturn(buildTaskResource(key));
        }

        components.forEach(componentsDao::save);
        installations.forEach(installationsDao::save);
    }

    @Test
    public void testAddedToInstallation() {
        List<Stage> stages = List.of(
            buildStage("production_market_tsum-test", "production", "application", "sbr:2455918660")
        );
        discoveryWorker.accept(stages);

        var discoveredServices = discoveryDao.getAllObjects();
        Assert.assertEquals(0, discoveredServices.size());

        //MARKET_MBI_FEED_PROCESSOR component; production environment
        Assert.assertTrue(
            installationsDao.getInstallationByComponentId(
                components.get(0).getId()).get(1).getDeployServices().contains(stages.get(0).getStageMeta().getId()));
    }

    @Test
    public void testWithoutComponentDiscovered() {
        List<Stage> stages = List.of(
            buildStage("production_market_no_component", "production", "application", "sbr:2254249580")
        );
        discoveryWorker.accept(stages);

        var discoveredServices = discoveryDao.getAllObjects();
        Assert.assertEquals(1, discoveredServices.size());
        Assert.assertNull(discoveredServices.get(0).getSuggestedComponentId());
    }

    @Test
    public void testWithoutInstallationDiscovered() {
        List<Stage> stages = List.of(
            buildStage("testing_market_tsum-test2", "testing", "application", "sbr:2187761794")
        );
        discoveryWorker.accept(stages);

        var discoveredServices = discoveryDao.getAllObjects();
        Assert.assertEquals(1, discoveredServices.size());
        Assert.assertEquals(components.get(1).getId(), discoveredServices.get(0).getSuggestedComponentId());
    }

    @Test
    public void testWithoutAppResourceDiscovered() {
        List<Stage> stages = List.of(
            buildStage("testing_market_tsum-test", "production", "logkeeper", "sbr:123456")
        );
        discoveryWorker.accept(stages);
        var discoveredServices = discoveryDao.getAllObjects();
        Assert.assertNull(discoveredServices.get(0).getSuggestedComponentId());
    }

    @Test
    public void testRemoveDiscovered() {
        List<Stage> stages = List.of(
            buildStage("production_market_tsum-test", "production", "application", "sbr:2455918660")
        );

        componentsDao.remove(components.get(0));
        discoveryWorker.accept(stages);
        var discoveredServices = discoveryDao.getAllObjects();
        Assert.assertEquals(1, discoveredServices.size());

        componentsDao.save(components.get(0));
        discoveryWorker.accept(stages);
        discoveredServices = discoveryDao.getAllObjects();
        Assert.assertEquals(0, discoveredServices.size());
    }

    private static Optional<TaskResource> buildTaskResource(Long id) {
        if (RESOURCE_MAP.get(id).equals("")) {
            return Optional.empty();
        }
        TaskResource taskResource = new TaskResource(id);
        taskResource.setType(RESOURCE_MAP.get(id));
        return Optional.of(taskResource);
    }

    private static Stage buildStage(String id, String env, String resourceName, String url) {
        return Stage.newBuilder(TransportType.YSON)
            .setMeta(StageMeta.newBuilder(TransportType.YSON)
                .setId(id)
                .build())
            .setSpec(StageSpec.newBuilder(TransportType.YSON)
                .setDeployUnitMap(
                    Map.of("feed-processor",
                        DeployUnitSpec.newBuilder(TransportType.YSON)
                            .setReplicaSet(DeployUnitSpec.ReplicaSetDeploy.newBuilder(TransportType.YSON)
                                .setReplicaSetTemplate(ReplicaSetSpec.newBuilder(TransportType.YSON)
                                    .setPodTemplateSpec(PodTemplateSpec.newBuilder(TransportType.YSON)
                                        .setLabels(AttributeDictionary.newBuilder(TransportType.YSON)
                                            .setStringAttribute("market_env", env)
                                            .build())
                                        .setSpec(PodSpec.newBuilder(TransportType.YSON)
                                            .setPodAgentPayload(PodSpec.PodAgentPayload.newBuilder(TransportType.YSON)
                                                .setSpec(PodAgentSpec.newBuilder(TransportType.YSON)
                                                    .setResources(ResourceGang.newBuilder(TransportType.YSON)
                                                        .addAllLayers(List.of(
                                                            Layer.newBuilder(TransportType.YSON)
                                                                .setId(resourceName)
                                                                .setUrl(url)
                                                                .build(),
                                                            Layer.newBuilder(TransportType.YSON)
                                                                .setId("infra-layer")
                                                                .setUrl("sbr:21844566")
                                                                .build()
                                                        ))
                                                        .build())
                                                    .build())
                                                .build())
                                            .build())
                                        .build())
                                    .build())
                                .build())
                        .build())
                )
                .build())
            .build();
    }
}
