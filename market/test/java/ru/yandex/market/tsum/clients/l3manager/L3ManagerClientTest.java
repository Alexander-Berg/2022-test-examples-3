package ru.yandex.market.tsum.clients.l3manager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.market.tsum.clients.l3manager.models.Balancer;
import ru.yandex.market.tsum.clients.l3manager.models.Service;
import ru.yandex.market.tsum.clients.l3manager.models.ServiceConfig;
import ru.yandex.market.tsum.clients.l3manager.models.VirtualServer;
import ru.yandex.market.tsum.clients.l3manager.models.VirtualServerConfig;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class L3ManagerClientTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());
    private L3ManagerClient client;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(L3ManagerClient.DATE_FORMAT);
    private final Path testDataDir = Paths.get("clients/l3manager");

    @Before
    public void before() {
        client = new L3ManagerClient("http://localhost:" + wireMockRule.port() + "/api/v1",
            "http://localhost:" + wireMockRule.port(),
            "testtoken", null);
    }

    private void addJsonStub(String url, String jsonFile) {
        try {
            wireMockRule.stubFor(get(urlPathEqualTo(url))
                .withQueryParam("_all", equalTo("true"))
                .willReturn(aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(getTestData(jsonFile))));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource.", e);
        }
    }

    private String getTestData(String filename) throws IOException {
        return Resources.toString(Resources.getResource(testDataDir.resolve(filename).toString()), Charsets.UTF_8);
    }

    private void validateService(Service service) throws Exception {
        assertEquals(501, service.getId());
        assertEquals("pricelabs-api.vs.market.yandex.net", service.getFqdn());
        assertEquals("MARKETITO", service.getAbcServiceName());
        assertEquals(State.ACTIVE, service.getState());
        assertFalse(service.isArchive());
        validateServiceConfig(service.getConfig());
    }

    private void validateServiceConfig(ServiceConfig config) throws Exception {
        assertEquals(974, config.getId());
        int previousId = config.getIdValuesHistory().get(0);
        assertEquals(955, previousId);
        assertEquals("test comment", config.getComment());
        assertEquals("/api/v1/service/501/config/974", config.getUrl());
        assertEquals(dateFormat.parse("2017-09-27T13:20:54.988Z"), config.getTimestamp());
        assertEquals(State.ACTIVE, config.getState());
        int vsId = config.getVirtualServerIds().get(0);
        assertEquals(1295, vsId);
        assertEquals("test description", config.getDescription());
    }

    private void validateVirtualServer(VirtualServer vs) throws Exception {
        assertEquals(1295, vs.getId());
        assertEquals("91d995600c27787c51a652d65b9dcf804b125f34218bdfbe20acd183b54ddfc1", vs.getExtId());
        assertEquals(InetAddress.getByName("2a02:6b8:0:3400:0:3c9:0:10"), vs.getIp());
        assertEquals(80, vs.getPort());
        assertEquals(VirtualServer.Protocol.TCP, vs.getProtocol());
        assertEquals("", vs.getConfig().getHost());
        assertEquals(VirtualServerConfig.Method.TUN, vs.getConfig().getMethod());
        assertEquals(1, vs.getConfig().getQuorum());
        assertEquals(1, vs.getConfig().getHysteresis());
        assertEquals(true, vs.getConfig().getAnnounce());
        assertEquals("/ping", vs.getConfig().getCheckUrl());
        assertEquals(VirtualServerConfig.Scheduler.WRR, vs.getConfig().getScheduler());
        assertEquals(VirtualServerConfig.CheckType.HTTP_GET, vs.getConfig().getCheckType());
        assertEquals(200, vs.getConfig().getStatusCode());
        assertEquals(true, vs.getConfig().getDcFilter());
        assertEquals(true, vs.getConfig().getDynamicAccess());
        assertEquals(true, vs.getConfig().getDynamicWeight());
        assertEquals(true, vs.getConfig().getDynamicWeightInHeader());
        assertEquals(false, vs.getConfig().getDynamicWeightAllowZero());
        assertEquals(30, vs.getConfig().getDynamicWeightRatio());
        assertEquals(false, vs.getConfig().getInhibitOnFailure());
        assertEquals(1, vs.getGroups().size());
        assertEquals("%market_slb_search-stable", vs.getGroups().get(0));
    }

    @Test
    public void testGetService() throws Exception {
        addJsonStub("/api/v1/service/501", "service.json");
        Service service = client.getService(501);
        validateService(service);
    }

    @Test
    public void testGetVirtualServers() throws Exception {
        addJsonStub("/api/v1/service/501/vs", "virtual_servers.json");
        List<VirtualServer> vss = client.getVirtualServers(501);
        validateVirtualServer(vss.get(0));
    }

    @Test
    public void testGetVirtualServer() throws Exception {
        addJsonStub("/api/v1/service/501/vs/1295", "virtual_server.json");
        VirtualServer vs = client.getVirtualServer(501, 1295);
        validateVirtualServer(vs);
    }

    @Test
    public void testGetServiceConfigs() throws Exception {
        addJsonStub("/api/v1/service/501", "service.json");
        addJsonStub("/api/v1/service/501/config", "service_configs.json");
        Service service = client.getService(501);
        List<ServiceConfig> configs = client.getServiceConfigs(service);
        ServiceConfig config = configs.get(0);
        validateServiceConfig(config);
    }

    @Test
    public void testGetServiceConfig() throws Exception {
        addJsonStub("/api/v1/service/501/config/974", "service_config.json");
        ServiceConfig config = client.getServiceConfig(501, 974);
        validateServiceConfig(config);
    }

    @Test
    public void testFindService() throws Exception {
        // XXX: mock не валидирует get-аргументы по которым фильтруется коллекция
        addJsonStub("/api/v1/service", "services.json");
        addJsonStub("/api/v1/service/501", "service.json");
        Service service = client.findService("pricelabs-api.vs.market.yandex.net");
        validateService(service);
    }

    @Test
    public void testCreateService() throws Exception {
        addJsonStub("/api/v1/service", "empty_services.json");
        addJsonStub("/api/v1/service/501", "service.json");
        wireMockRule.stubFor(post(urlPathEqualTo("/api/v1/service"))
            .withRequestBody(containing("fqdn=test.tst.vs.market.yandex.net"))
            .withRequestBody(containing("abc=MARKETITO"))
            .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_CREATED)
                .withHeader("Content-Type", "application/json")
                .withHeader("Location", "/api/v1/service/501")
                .withBody("{\"result\": \"OK\", \"object\": {\"id\": 501}}")));

        L3ApiRequestResult result = client.createService("test.tst.vs.market.yandex.net", "MARKETITO");
        assertEquals(L3ApiRequestResult.Status.OK, result.getStatus());
        Service service = client.getService(result.getId());
        validateService(service);
    }

    @Test
    public void testCreateServiceFailed() throws Exception {
        addJsonStub("/api/v1/service", "empty_services.json");
        wireMockRule.stubFor(post(urlPathEqualTo("/api/v1/service"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_BAD_REQUEST)
                .withHeader("Content-Type", "application/json")
                .withHeader("Location", "/api/v1/service/501")
                .withBody("{\"result\": \"ERROR\", " +
                    "\"message\": \"* fqdn\\n  * Service with this FQDN already exists.\", " +
                    "\"errors\": {\"fqdn\": [\"Service with this FQDN already exists.\"]}}")));

        L3ApiRequestResult result = client.createService("test.tst.vs.market.yandex.net", "MARKETITO");
        assertEquals(L3ApiRequestResult.Status.ERROR, result.getStatus());
    }

    @Test
    public void testFindVirtualServer() throws Exception {
        wireMockRule.stubFor(get(urlPathEqualTo("/api/v1/service/501/vs"))
            .withQueryParam("_all", equalTo("true"))
            .withQueryParam("ip__exact", equalTo("2a02:6b8:0:3400:0:3c9:0:10"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(getTestData("virtual_servers.json"))));
        VirtualServer vs = client.findVirtualServer(501, "2a02:6b8:0:3400:0:3c9:0:10");
        validateVirtualServer(vs);
    }

    @Test
    public void testCreateVirtualServer() throws Exception {
        addJsonStub("/api/v1/balancer", "balancers.json");
        addJsonStub("/api/v1/service/501", "service.json");
        // XXX: wiremock не умеет матчить formdata, поэтому ищу данные в теле запроса вручную
        wireMockRule.stubFor(post(urlPathEqualTo("/api/v1/service/501/vs"))
            .withRequestBody(containing("ip=" + URLEncoder.encode("2a02:6b8:0:3400:0:3c9:0:10",
                StandardCharsets.UTF_8)))
            .withRequestBody(containing("port=80"))
            .withRequestBody(containing("abc=MARKETITO"))
            .withRequestBody(containing("protocol=TCP"))
            .withRequestBody(containing("config-HOST="))
            .withRequestBody(containing("config-METHOD=TUN"))
            .withRequestBody(containing("config-QUORUM=1"))
            .withRequestBody(containing("config-INHIBIT_ON_FAILURE=false"))
            .withRequestBody(containing("config-ANNOUNCE=true"))
            .withRequestBody(containing("config-CHECK_URL=" + URLEncoder.encode("/ping", StandardCharsets.UTF_8)))
            .withRequestBody(containing("config-SCHEDULER=wrr"))
            .withRequestBody(containing("config-CHECK_TYPE=HTTP_GET"))
            .withRequestBody(containing("config-HYSTERESIS=1"))
            .withRequestBody(containing("config-STATUS_CODE=200"))
            .withRequestBody(containing("config-DC_FILTER=true"))
            .withRequestBody(containing("config-DYNAMICACCESS=true"))
            .withRequestBody(containing("config-DYNAMICWEIGHT=true"))
            .withRequestBody(containing("config-DYNAMICWEIGHT_ALLOW_ZERO=false"))
            .withRequestBody(containing("config-DYNAMICWEIGHT_IN_HEADER=true"))
            .withRequestBody(containing("config-DYNAMICWEIGHT_RATIO=30"))
            .withRequestBody(containing("groups=" + URLEncoder.encode("%market_slb_search-stable",
                StandardCharsets.UTF_8)))
            .withRequestBody(containing("config-WEIGHT_LB67=100"))
            .withRequestBody(containing("lb=67"))
            .withHeader("Authorization", equalTo("OAuth testtoken"))
            .willReturn(aResponse()
                .withStatus(org.apache.http.HttpStatus.SC_CREATED)
                .withHeader("Content-Type", "application/json")
                .withBody(Resources.toString(Resources.getResource("clients/l3manager/create_virtual_server.json"),
                    Charsets.UTF_8))));

        Service service = client.getService(501);
        L3ApiRequestResult result = client.createVirtualServer(service, createVirtualServer());
        assertEquals(L3ApiRequestResult.Status.OK, result.getStatus());
    }

    @Test
    public void testCreateVirtualServerFailed() throws Exception {
        addJsonStub("/api/v1/balancer", "balancers.json");
        addJsonStub("/api/v1/service/501", "service.json");
        wireMockRule.stubFor(post(urlPathEqualTo("/api/v1/service/501/vs"))
            .withHeader("Authorization", equalTo("OAuth testtoken"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_BAD_REQUEST)
                .withHeader("Content-Type", "application/json")
                .withBody(Resources.toString(Resources.getResource("clients/l3manager/create_virtual_server_failed" +
                    ".json"), Charsets.UTF_8))));

        Service service = client.getService(501);
        L3ApiRequestResult result = client.createVirtualServer(service, createVirtualServer());
        assertEquals(L3ApiRequestResult.Status.ERROR, result.getStatus());
    }

    private VirtualServer createVirtualServer() throws Exception {
        List<Balancer> balancers = client.getBalancers("MARKETITO");
        VirtualServerConfig config = new VirtualServerConfig();
        config.setMethod(VirtualServerConfig.Method.TUN);
        config.setScheduler(VirtualServerConfig.Scheduler.WRR);
        config.setCheckType(VirtualServerConfig.CheckType.HTTP_GET);
        config.setCheckUrl("/ping");
        config.setStatusCode(200);
        config.setQuorum(1);
        config.setHysteresis(1);
        config.setDcFilter(true);
        config.setDynamicAccess(true);
        config.setDynamicWeight(true);
        config.setDynamicWeightInHeader(true);
        config.setDynamicWeightRatio(30);
        config.setInhibitOnFailure(false);
        config.setAnnounce(true);
        for (Balancer balancer : balancers) {
            config.addBalancer(balancer, 100);
        }
        VirtualServer vs = new VirtualServer();
        vs.setIp(InetAddress.getByName("2a02:6b8:0:3400:0:3c9:0:10"));
        vs.setPort(80);
        vs.setProtocol(VirtualServer.Protocol.TCP);
        vs.setConfig(config);
        vs.addGroup("%market_slb_search-stable");

        return vs;
    }

    @Test
    public void testCreateConfig() throws Exception {
        wireMockRule.stubFor(post(urlPathEqualTo("/api/v1/service/501/config"))
            .withRequestBody(containing("vs=1295"))
            .withRequestBody(containing("comment=" + URLEncoder.encode("test comment", StandardCharsets.UTF_8)))
            .withHeader("Authorization", equalTo("OAuth testtoken"))
            .willReturn(aResponse()
                .withStatus(org.apache.http.HttpStatus.SC_CREATED)
                .withHeader("Content-Type", "application/json")
                .withBody(getTestData("create_config.json"))));

        L3ApiRequestResult result = client.createServiceConfig(
            501, Collections.singletonList(1295), "test comment"
        );
        assertEquals(L3ApiRequestResult.Status.OK, result.getStatus());
    }

    @Test
    public void testCreateConfigFailed() throws Exception {
        wireMockRule.stubFor(post(urlPathEqualTo("/api/v1/service/501/config"))
            .withRequestBody(containing("vs=1295"))
            .withRequestBody(containing("comment=" + URLEncoder.encode("test comment", StandardCharsets.UTF_8)))
            .withHeader("Authorization", equalTo("OAuth testtoken"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_BAD_REQUEST)
                .withHeader("Content-Type", "application/json")
                .withBody(getTestData("create_config_failed.json"))));

        L3ApiRequestResult result = client.createServiceConfig(
            501, Collections.singletonList(1295), "test comment"
        );
        assertEquals(L3ApiRequestResult.Status.ERROR, result.getStatus());
    }

    @Test
    public void testDeployConfig() throws Exception {
        wireMockRule.stubFor(post(urlPathEqualTo("/api/v1/service/501/config/974/process"))
            .withHeader("Authorization", equalTo("OAuth testtoken"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_ACCEPTED)
                .withHeader("Content-Type", "application/json")
                .withBody(getTestData("deploy_config.json"))));
        L3ApiRequestResult result = client.deployConfig(501, 974);
        assertEquals(L3ApiRequestResult.Status.OK, result.getStatus());
    }

    @Test
    public void testDeployConfigFailed() throws Exception {
        wireMockRule.stubFor(post(urlPathEqualTo("/api/v1/service/501/config/974/process"))
            .withHeader("Authorization", equalTo("OAuth testtoken"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_NOT_FOUND)
                .withHeader("Content-Type", "application/json")
                .withBody(getTestData("deploy_config_failed.json"))));

        L3ApiRequestResult result = client.deployConfig(501, 974);
        assertEquals(L3ApiRequestResult.Status.ERROR, result.getStatus());
    }

    @Test
    public void testGetIpAddress() throws Exception {
        wireMockRule.stubFor(post(urlPathEqualTo("/api/v1/abc/MARKETITO/getip"))
            .withHeader("Authorization", equalTo("OAuth testtoken"))
            .withRequestBody(containing("v4=false"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"result\": \"OK\", \"object\": \"2a02:6b8:0:3400:0:3c9:0:19\"}")));
        assertEquals(
            InetAddress.getByName("2a02:6b8:0:3400:0:3c9:0:19"),
            client.getAddress("MARKETITO", false, false)
        );
    }

    @Test(expected = L3ManagerException.class)
    public void testGetIpAddressFailed() throws Exception {
        wireMockRule.stubFor(post(urlPathEqualTo("/api/v1/abc/MARKETITO/getip"))
            .withHeader("Authorization", equalTo("OAuth testtoken"))
            .withRequestBody(containing("v4=true"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_BAD_REQUEST)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\": \"There no available IPs\", \"result\": \"ERROR\"}")));
        client.getAddress("MARKETITO", true, false);
    }
}
