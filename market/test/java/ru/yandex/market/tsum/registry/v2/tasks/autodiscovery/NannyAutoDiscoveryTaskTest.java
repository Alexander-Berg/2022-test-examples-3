package ru.yandex.market.tsum.registry.v2.tasks.autodiscovery;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.nanny.NannyClient;
import ru.yandex.market.tsum.clients.nanny.service.NannyService;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.infrasearch.upload.tasks.nanny.NannyServiceUtil;
import ru.yandex.market.tsum.registry.v2.dao.ComponentsDao;
import ru.yandex.market.tsum.registry.v2.dao.InstallationsDao;
import ru.yandex.market.tsum.registry.v2.dao.NannyAutodiscoveryDao;
import ru.yandex.market.tsum.registry.v2.dao.model.Component;
import ru.yandex.market.tsum.registry.v2.dao.model.Installation;
import ru.yandex.market.tsum.registry.v2.dao.model.autodiscovery.NannyAutoDiscoveredService;
import ru.yandex.misc.test.Assert;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.StartrekClientBuilder;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static ru.yandex.market.tsum.core.TestResourceLoader.getTestResourceAsString;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 25/06/2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class NannyAutoDiscoveryTaskTest {

    private static final String AUTH_TOKEN = "XXX";

    private static final String RESOURCE_DIRECTORY_PATH = "registry/v2/tasks/autodiscovery/nanny/";

    private static final Gson GSON = new GsonBuilder().create();

    private static final Type COMPONENT_LIST_TYPE = new TypeToken<List<Component>>() { }.getType();

    private static final Type INSTALLATION_LIST_TYPE = new TypeToken<List<Installation>>() { }.getType();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Autowired
    private MongoTemplate mongoTemplate;

    private NannyClient nannyClient;
    private NannyAutoDiscoveryTask nannyAutoDiscoveryTask;
    private ComponentsDao componentsDao;
    private InstallationsDao installationsDao;
    private NannyAutodiscoveryDao nannyAutodiscoveryDao;

    @Before
    public void setUp() {
        this.installationsDao = new InstallationsDao(mongoTemplate);
        this.componentsDao = new ComponentsDao(mongoTemplate, installationsDao);
        this.nannyAutodiscoveryDao = new NannyAutodiscoveryDao(mongoTemplate);
        this.nannyClient = new NannyClient("http://localhost:" + wireMockRule.port(), AUTH_TOKEN, null);

        Session startrekSession = StartrekClientBuilder.newBuilder()
            .uri("http://localhost:" + wireMockRule.port())
            .userAgent("market-tsum")
            .build(AUTH_TOKEN);

        this.nannyAutoDiscoveryTask = new NannyAutoDiscoveryTask(nannyClient, componentsDao, installationsDao,
            nannyAutodiscoveryDao, startrekSession);
    }

    public void addGetNannyServicesWireMockStub(String servicesResponsePath, int numTestServices) throws IOException {
        wireMockRule.stubFor(get(urlPathEqualTo("/v2/services/"))
            .withQueryParam("category", equalTo("/market"))
            .willReturn(aResponse().withStatus(200).withBody(getTestResourceAsString(
                RESOURCE_DIRECTORY_PATH + servicesResponsePath))));

        wireMockRule.stubFor(get(urlEqualTo("/v2/services/?category=%2Fmarket&skip=" + numTestServices))
            .willReturn(aResponse().withStatus(200).withBody("{ \"result\": [] }")));
    }

    @Test
    public void testExecuteServicesMatchedByInstallationServices() throws IOException {
        addGetNannyServicesWireMockStub("services_matched_by_installation_services.json", 3);

        List<Component> components = GSON.fromJson(getTestResourceAsString(RESOURCE_DIRECTORY_PATH +
            "components.json"), COMPONENT_LIST_TYPE);
        List<Installation> installations = GSON.fromJson(getTestResourceAsString(RESOURCE_DIRECTORY_PATH +
            "installations.json"), INSTALLATION_LIST_TYPE);

        List<NannyService> services = NannyServiceUtil.getMarketNannyServices(nannyClient);

        for (Component component : components) {
            componentsDao.save(component);
        }

        for (Installation installation : installations) {
            installationsDao.save(installation);
        }

        nannyAutodiscoveryDao.save(new NannyAutoDiscoveredService(services.get(0).getId()));

        nannyAutoDiscoveryTask.execute(null);

        installations = installationsDao.getAll();

        Assert.assertEmpty(nannyAutodiscoveryDao.getAll());
        Assert.assertContains(installations.get(0).getNannyServices(), services.get(0).getId());
        Assert.assertContains(installations.get(1).getNannyServices(), services.get(1).getId());
        Assert.assertContains(installations.get(1).getNannyServices(), services.get(2).getId());
    }

    @Test
    public void testExecuteServiceMatchedByInstallationName() throws IOException {
        addGetNannyServicesWireMockStub("services_matched_by_installation_name.json", 1);

        List<Component> components = GSON.fromJson(getTestResourceAsString(RESOURCE_DIRECTORY_PATH +
            "components.json"), COMPONENT_LIST_TYPE);
        List<Installation> installations = GSON.fromJson(getTestResourceAsString(RESOURCE_DIRECTORY_PATH +
            "installations.json"), INSTALLATION_LIST_TYPE);

        List<NannyService> services = NannyServiceUtil.getMarketNannyServices(nannyClient);

        for (Component component : components) {
            componentsDao.save(component);
        }

        for (Installation installation : installations) {
            installationsDao.save(installation);
        }

        nannyAutodiscoveryDao.save(new NannyAutoDiscoveredService(services.get(0).getId()));

        nannyAutoDiscoveryTask.execute(null);

        installations = installationsDao.getAll();

        Assert.assertEmpty(nannyAutodiscoveryDao.getAll());
        Assert.assertContains(installations.get(2).getNannyServices(), services.get(0).getId());
    }

    @Test
    public void testExecuteNewServiceWithoutResourceType() throws IOException {
        addGetNannyServicesWireMockStub("service_without_resource_type.json", 1);
        wireMockRule.stubFor(post(urlEqualTo("/v2/issues?fields=")).willReturn(aResponse().withStatus(201).withBody(
            getTestResourceAsString(RESOURCE_DIRECTORY_PATH + "startrek_create_issue_response_body.json"))));

        List<Component> components = GSON.fromJson(getTestResourceAsString(RESOURCE_DIRECTORY_PATH +
            "components.json"), COMPONENT_LIST_TYPE);
        List<Installation> installations = GSON.fromJson(getTestResourceAsString(RESOURCE_DIRECTORY_PATH +
            "installations.json"), INSTALLATION_LIST_TYPE);

        List<NannyService> services = NannyServiceUtil.getMarketNannyServices(nannyClient);

        for (Component component : components) {
            componentsDao.save(component);
        }

        for (Installation installation : installations) {
            installationsDao.save(installation);
        }

        nannyAutoDiscoveryTask.execute(null);

        List<String> unmatchedServices = nannyAutodiscoveryDao.getAll();
        NannyAutoDiscoveredService unmatchedService = nannyAutodiscoveryDao.get(
            unmatchedServices.get(0));

        Assert.assertEquals(unmatchedService.getServiceName(), services.get(0).getId());
    }

    @Test
    public void testExecuteNewServiceUnmatched() throws IOException {
        addGetNannyServicesWireMockStub("unmatched_service.json", 1);

        wireMockRule.stubFor(post(urlEqualTo("/v2/issues?fields="))
            .withRequestBody(equalToJson(
                getTestResourceAsString(RESOURCE_DIRECTORY_PATH + "startrek_create_issue_request_body.json")))
            .willReturn(aResponse().withStatus(201).withBody(
                getTestResourceAsString(RESOURCE_DIRECTORY_PATH + "startrek_create_issue_response_body.json"))));

        List<Component> components = GSON.fromJson(getTestResourceAsString(RESOURCE_DIRECTORY_PATH +
            "components.json"), COMPONENT_LIST_TYPE);
        List<Installation> installations = GSON.fromJson(getTestResourceAsString(RESOURCE_DIRECTORY_PATH +
            "installations.json"), INSTALLATION_LIST_TYPE);

        List<NannyService> services = NannyServiceUtil.getMarketNannyServices(nannyClient);

        for (Component component : components) {
            componentsDao.save(component);
        }

        for (Installation installation : installations) {
            installationsDao.save(installation);
        }

        nannyAutoDiscoveryTask.execute(null);

        List<String> unmatchedServices = nannyAutodiscoveryDao.getAll();
        NannyAutoDiscoveredService unmatchedService = nannyAutodiscoveryDao.get(
            unmatchedServices.get(0));

        Assert.assertEquals(unmatchedService.getServiceName(), services.get(0).getId());

        wireMockRule.verify(postRequestedFor(urlPathEqualTo("/v2/issues"))
            .withRequestBody(equalToJson(
                getTestResourceAsString(RESOURCE_DIRECTORY_PATH + "startrek_create_issue_request_body.json"))));
    }
}
