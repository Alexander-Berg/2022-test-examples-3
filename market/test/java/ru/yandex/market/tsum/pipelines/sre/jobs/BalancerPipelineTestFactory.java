package ru.yandex.market.tsum.pipelines.sre.jobs;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.market.tsum.clients.JsonList;
import ru.yandex.market.tsum.clients.l3manager.L3ApiRequestResult;
import ru.yandex.market.tsum.clients.l3manager.L3ManagerClient;
import ru.yandex.market.tsum.clients.l3manager.models.Balancer;
import ru.yandex.market.tsum.clients.l3manager.models.Service;
import ru.yandex.market.tsum.clients.l3manager.models.ServiceConfig;
import ru.yandex.market.tsum.clients.l3manager.models.VirtualServer;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekTicket;
import ru.yandex.market.tsum.pipelines.sre.resources.BalancerEnvironment;
import ru.yandex.market.tsum.pipelines.sre.resources.BalancerFlavour;
import ru.yandex.market.tsum.pipelines.sre.resources.IPVersion;
import ru.yandex.market.tsum.pipelines.sre.resources.VirtualIPs;
import ru.yandex.market.tsum.pipelines.sre.resources.balancer.BalancerInfo;
import ru.yandex.startrek.client.model.Issue;

public class BalancerPipelineTestFactory {
    private static final Gson GSON = L3ManagerClient.getGson();

    private static final String DESCRIPTION = "slb.description = Что-то для теста\n" +
        "slb.fqdn = test-service.name.tst.vs.market.yandex.net\n" +
        "slb.port = 55555\n" +
        "slb.type = Внутренний\n" +
        "slb.type_backends = CONDUCTOR_GROUP\n" +
        "slb.real_servers = market_slb_search-stable\n" +
        "slb.health_check_url = /ping\n" +
        "slb.health_check_type = Тело ответа\n" +
        "slb.health_check_text = 0;ok\n" +
        "slb.rps = 20 рпс\n" +
        "slb.ip_version = IPv6-only\n" +
        "slb.offset_port =\n" +
        "slb.realPort = 55556\n" +
        "access.dynamic = Да\n" +
        "access.human = \n" +
        "access.machine = \n" +
        "monitor.needMonitor = Да\n" +
        "monitor.resps = yandex_market_dev,yandex_market_admin,le087";

    private static final String HTTP_DESCRIPTION = "slb.description = Кадаврик. Универсальная мокалка бэкендов\n" +
        "slb.fqdn = kadavr.vs.market.yandex.net\n" +
        "slb.port = 80\n" +
        "slb.type = Внутренний\n" +
        "slb.httpsPort =\n" +
        "slb.realPort =\n" +
        "slb.redirect_to_https = Нет\n" +
        "slb.ssl_backends = Нет\n" +
        "slb.ssl_externalca = Нет\n" +
        "slb.ssl_ca =\n" +
        "slb.ssl_altnames =\n" +
        "slb.type_backends = NANNY_SERVICE\n" +
        "slb.real_servers = testing_market_kadavr_release_vla\n" +
        "slb.offset_port = 1\n" +
        "slb.health_check_url = /ping\n" +
        "slb.health_check_type = 200-ый код ответа\n" +
        "slb.health_check_text =\n" +
        "slb.rps = 300\n" +
        "slb.ip_version = IPv6-only\n" +
        "access.human = Фронтенд Маркета\n" +
        "access.machine = _C_MARKET_FRONT_DEVEL_,_MARKETDEVNETS_,_GENCFG_MARKET_TEST_,_KOPALKA_,_SELENIUMGRIDNETS_," +
        "_SELENIUMGRIDNETS_TEST_NETS_,_CMSEARCHNETS_\n" +
        "monitor.needMonitor = Нет\n" +
        "monitor.resps = ";

    private static final String HTTPS_DESCRIPTION = "slb.description = test\n" +
        "slb.fqdn = test7.tst.vs.market.yandex.net\n" +
        "slb.port = 80\n" +
        "slb.type = Внешний\n" +
        "slb.httpsPort = 443\n" +
        "slb.realPort = 21266\n" +
        "slb.redirect_to_https = Да\n" +
        "slb.ssl_backends = Да\n" +
        "slb.ssl_externalca = Нет\n" +
        "slb.ssl_ca =\n" +
        "slb.ssl_altnames =\n" +
        "slb.type_backends = NANNY_SERVICE\n" +
        "slb.real_servers = bad_nanny_service\n" +
        "slb.offset_port =\n" +
        "slb.health_check_url = /ping\n" +
        "slb.health_check_type = 200-ый код ответа\n" +
        "slb.health_check_text =\n" +
        "slb.rps = 1\n" +
        "slb.ip_version = IPv4 + IPv6\n" +
        "access.human = pashayelkin\n" +
        "access.machine = _C_MARKET_DEVEL_ADMIN_\n" +
        "monitor.needMonitor = Нет\n" +
        "monitor.resps = ";

    private static final String DESCRIPTION_PRODUCTION = "slb.description = Что-то для теста\n" +
        "slb.fqdn = test-service.name.vs.market.yandex.net\n" +
        "slb.port = 55555\n" +
        "slb.type = Внутренний\n" +
        "slb.type_backends = CONDUCTOR_GROUP\n" +
        "slb.real_servers = market_slb_search-stable\n" +
        "slb.health_check_url = /ping\n" +
        "slb.health_check_type = Тело ответа\n" +
        "slb.health_check_text = 0;ok\n" +
        "slb.rps = 20 рпс\n" +
        "slb.ip_version = IPv6-only\n" +
        "slb.offset_port =\n" +
        "slb.realPort = 55556\n" +
        "access.dynamic = Да\n" +
        "access.human = \n" +
        "access.machine = \n" +
        "monitor.needMonitor = Да\n" +
        "monitor.resps = yandex_market_dev,yandex_market_admin,le087";

    private static final String DESCRIPTION_FAILED = "slb.description = Что-то для теста\n" +
        "slb.fqdn = mbi-crm-proxy.bla.bla.yandex.ru\n" +  // wrong fqdn for balancer
        "slb.port = 55555\n" +
        "slb.type = Внутренний\n" +
        "slb.type_backends = CONDUCTOR_GROUP\n" +
        "slb.real_servers = market_slb_search-stable\n" +
        "slb.health_check_url = /ping\n" +
        "slb.health_check_type = Тело ответа\n" +
        "slb.health_check_text = 0;ok\n" +
        "slb.rps = 20 рпс\n" +
        "slb.ip_version = IPv6-only\n" +
        "slb.offset_port =\n" +
        "slb.realPort =\n" +
        "access.dynamic = Да\n" +
        "access.human = \n" +
        "access.machine = \n" +
        "monitor.needMonitor = \n" +
        "monitor.resps = ";

    private static final String DESCRIPTION_GRPC = "slb.description = Balancer with GRPC\n" +
        "slb.fqdn = test-service-grpc.name.tst.vs.market.yandex.net\n" +
        "slb.port = 55555\n" +
        "slb.type = Внутренний\n" +
        "slb.type_backends = CONDUCTOR_GROUP\n" +
        "slb.real_servers = market_slb_search-stable\n" +
        "slb.health_check_url = /ping\n" +
        "slb.health_check_type = Тело ответа\n" +
        "slb.health_check_text = 0;ok\n" +
        "slb.enable_grpc = Да\n" +
        "slb.enable_grpc_ssl = Нет\n" +
        "slb.grpc_ssl_backends = Нет\n" +
        "slb.grpc_backends_port = 55557\n" +
        "slb.grpc_offset_port =\n" +
        "slb.rps = 20 рпс\n" +
        "slb.ip_version = IPv6-only\n" +
        "slb.offset_port =\n" +
        "slb.realPort = 55556\n" +
        "access.dynamic = Да\n" +
        "access.human = \n" +
        "access.machine = \n" +
        "monitor.needMonitor = Нет\n";

    private static final String DESCRIPTION_GRPC_SSL = "slb.description = Balancer with GRPC and SSL\n" +
        "slb.fqdn = test-service-grpc-ssl.name.tst.vs.market.yandex.net\n" +
        "slb.port = 55555\n" +
        "slb.type = Внутренний\n" +
        "slb.type_backends = CONDUCTOR_GROUP\n" +
        "slb.real_servers = market_slb_search-stable\n" +
        "slb.health_check_url = /ping\n" +
        "slb.health_check_type = Тело ответа\n" +
        "slb.health_check_text = 0;ok\n" +
        "slb.enable_grpc = Да\n" +
        "slb.enable_grpc_ssl = Да\n" +
        "slb.grpc_ssl_backends = Да\n" +
        "slb.grpc_backends_port =\n" +
        "slb.grpc_offset_port = 3\n" +
        "slb.rps = 20 рпс\n" +
        "slb.ip_version = IPv6-only\n" +
        "slb.offset_port =\n" +
        "slb.realPort = 55556\n" +
        "access.dynamic = Да\n" +
        "access.human = \n" +
        "access.machine = \n" +
        "monitor.needMonitor = Нет\n";

    private static final StartrekTicket STARTREK_TICKET = new StartrekTicket("TEST-12345");

    private BalancerPipelineTestFactory() {
    }

    public static String getDescription() {
        return DESCRIPTION;
    }

    public static StartrekTicket getStartrekTicket() {
        return STARTREK_TICKET;
    }

    public static BalancerInfo.Builder getBalancerInfoBuilder() {
        return BalancerInfo.Builder.create()
            .withEnvironment(BalancerEnvironment.TESTING)
            .withStartrekTicket(getStartrekTicket())
            .withBalancerFlavour(BalancerFlavour.MSLB)
            .withDescription("Test balancer")
            .withFqdn("test.tst.vs.market.yandex.net")
            .withRealServers(Collections.emptyList())
            .withHealthCheckUrl("/ping")
            .withHealthCheckType(BalancerInfo.HealthCheckType.RESPONSE_CODE)
            .withRps("0")
            .withIpVersion(IPVersion.IPV6)
            .withTypeOfBackends(BalancerInfo.TypeOfBackends.HOST)
            .withSslBackends(false);
    }


    public static BalancerInfo getBalancerInfo() throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(DESCRIPTION));
        return new BalancerInfo(STARTREK_TICKET, properties, BalancerEnvironment.TESTING);
    }

    public static BalancerInfo getBalancerInfoHttps() throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(HTTPS_DESCRIPTION));
        return new BalancerInfo(STARTREK_TICKET, properties, BalancerEnvironment.TESTING);
    }

    public static BalancerInfo getBalancerInfoHttp() throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(HTTP_DESCRIPTION));
        return new BalancerInfo(STARTREK_TICKET, properties, BalancerEnvironment.TESTING);
    }

    public static BalancerInfo getBalancerInfoProduction() throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(DESCRIPTION_PRODUCTION));
        return new BalancerInfo(STARTREK_TICKET, properties, BalancerEnvironment.PRODUCTION);
    }

    public static BalancerInfo getBalancerInfoGrpc() throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(DESCRIPTION_GRPC));
        return new BalancerInfo(STARTREK_TICKET, properties, BalancerEnvironment.TESTING);
    }

    public static BalancerInfo getBalancerInfoGrpcSsl() throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(DESCRIPTION_GRPC_SSL));
        return new BalancerInfo(STARTREK_TICKET, properties, BalancerEnvironment.TESTING);
    }

    public static VirtualIPs getVirtualIPs() throws IOException {
        return new VirtualIPs(
            getBalancerInfo().getFqdn(),
            getStartrekTicket(),
            Arrays.asList("213.180.193.120", "2a02:6b8::3:120")
        );
    }

    public static BalancerInfo getBalancerInfoFailed() throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(DESCRIPTION_FAILED));
        return new BalancerInfo(STARTREK_TICKET, properties, BalancerEnvironment.TESTING);
    }

    private static <T> List<T> getL3managerObjects(String path, Class<T> type) throws IOException {
        JsonObject jsonObject = new JsonParser()
            .parse(Resources.toString(Resources.getResource(path), Charsets.UTF_8))
            .getAsJsonObject();

        JsonArray jsonArray = jsonObject.getAsJsonArray("objects");
        return GSON.fromJson(jsonArray, JsonList.of(type));
    }

    private static <T> T getL3managerObject(String path, Class<T> type) throws IOException {
        JsonObject jsonObject = new JsonParser()
            .parse(Resources.toString(Resources.getResource(path), Charsets.UTF_8))
            .getAsJsonObject();
        return GSON.fromJson(jsonObject, type);
    }


    public static List<Service> getL3managerServices() throws IOException {
        return getL3managerObjects(
            "clients/l3manager/services.json",
            Service.class
        );
    }

    public static Service getL3managerService() throws IOException {
        return getL3managerObject(
            "clients/l3manager/service.json",
            Service.class
        );
    }

    public static Service getL3managerNewService() throws IOException {
        return getL3managerObject(
            "clients/l3manager/new_service.json",
            Service.class
        );
    }

    public static L3ApiRequestResult getL3managerResultService() {
        return GSON.fromJson("{\"result\": \"OK\", \"object\": {\"id\": 501}}", L3ApiRequestResult.class);
    }

    public static List<Balancer> getL3managerBalancers() throws IOException {
        return getL3managerObjects(
            "clients/l3manager/balancers.json",
            Balancer.class
        );
    }

    public static VirtualServer getL3managerVirtualServer() throws IOException {
        return getL3managerObject(
            "clients/l3manager/virtual_server.json",
            VirtualServer.class
        );
    }

    public static ServiceConfig getL3managerServiceConfig() throws IOException {
        return getL3managerObject(
            "clients/l3manager/service_config.json",
            ServiceConfig.class
        );
    }

    public static L3ApiRequestResult getL3ManagerDeployResult() throws IOException {
        return getL3managerObject(
            "clients/l3manager/deploy_config.json",
            L3ApiRequestResult.class
        );
    }

    public static L3ApiRequestResult getL3managerResultVirtualServer() throws IOException {
        return getL3managerObject(
            "clients/l3manager/create_virtual_server.json",
            L3ApiRequestResult.class
        );
    }

    public static L3ApiRequestResult getL3ManagerResultServiceConfig() throws IOException {
        return getL3managerObject(
            "clients/l3manager/create_config.json",
            L3ApiRequestResult.class
        );
    }

    public static Issue getIssue() throws URISyntaxException {
        URI uri = new URI("https://st.yandex-team.ru/TEST-1111");
        MapF<String, Object> values = Cf.map("SomeData", "OtherData");

        return new Issue(
            "1111",
            uri,
            "TEST-1111",
            "Test issue",
            12345,
            values,
            null
        );
    }
}
