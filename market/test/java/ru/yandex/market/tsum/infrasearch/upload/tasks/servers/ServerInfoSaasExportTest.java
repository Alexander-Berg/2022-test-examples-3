package ru.yandex.market.tsum.infrasearch.upload.tasks.servers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import ru.yandex.bolts.collection.IterableF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.request.netty.retry.RetryIdempotentWithSleepPolicy;
import ru.yandex.market.saas.indexer.document.SaasDocument;
import ru.yandex.market.tsum.clients.conductor.ConductorClient;
import ru.yandex.market.tsum.clients.gencfg.GenCfgClient;
import ru.yandex.market.tsum.clients.oops.OopsClient;
import ru.yandex.market.tsum.infrasearch.document.SaasDocumentYt;
import ru.yandex.market.tsum.infrasearch.upload.SaasUtil;
import ru.yandex.market.tsum.infrasearch.upload.YtService;

import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.tsum.core.TestResourceLoader.getTestResourceAsString;

public class ServerInfoSaasExportTest {

    private final static ObjectMapper mapper = new ObjectMapper();

    private final static String RESOURCE_DIRECTORY_PATH = "infrasearch/upload/tasks/servers/";

    private final static String SAS_HOSTNAME = "sas1-1081.search.yandex.net";
    private final static String VLA_HOSTNAME = "vla1-4712.search.yandex.net";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Value("${tsum.conductor.oauth-token}")
    private String conductorRobotOAuthToken;

    @Value("${tsum.external-services.retry-count:5}")
    private int retryCount;

    @Value("${tsum.external-services.retry-sleep-millis:5000}")
    private int retrySleepMillis;

    private ConductorClient conductorClient;
    private GenCfgClient genCfgClient;
    private OopsClient oopsClient;
    private YtTables ytTables;
    private YtService ytService;
    private ServersInfoConverter serversInfoConverter;

    @Before
    public void setUp() throws Exception {
        genCfgClient = new GenCfgClient("http://localhost:" + wireMockRule.port() + "/");
        oopsClient = new OopsClient("http://localhost:" + wireMockRule.port()  + "/", null);
        HttpClientConfig config = new HttpClientConfig();
        config.setRetryPolicy(new RetryIdempotentWithSleepPolicy(retryCount, retrySleepMillis));
        conductorClient = new ConductorClient("http://localhost:" + wireMockRule.port() + "/",
            conductorRobotOAuthToken, new NettyHttpClientContext(config));

        Yt yt = mock(Yt.class, Mockito.RETURNS_DEEP_STUBS);
        ytTables = yt.tables();
        ytService = new YtService(yt);
        serversInfoConverter = new ServersInfoConverter(conductorClient, genCfgClient, oopsClient, 0);
    }

    @Before
    public void prepareWireMock() {
        addJsonStub("/api/groups2hosts/cs_all", "conductor_gproups2hosts_responce");
        addJsonStub("/trunk/groups", "gencfg_all_groups_response.json");
        addJsonStub("/trunk/groups/SAS_MARKET_DEV_HTTP_ADAPTER_MAILCORP", "gencfg_group_response.json");
        addJsonStub("/trunk/searcherlookup/groups/SAS_MARKET_DEV_HTTP_ADAPTER_MAILCORP/instances",
            "gencfg_group_instances_response.json");

        addJsonStub("/api/hosts/" + VLA_HOSTNAME + "/attributes/disks", "oops_vla_disks_response.json");
        addJsonStub("/api/hosts/" + VLA_HOSTNAME + "/attributes/cpu_info", "oops_vla_cpu_response.json");
        addJsonStub("/api/hosts/" + VLA_HOSTNAME + "/attributes/vlan_list", "oops_vla_vlans_response.json");
        addJsonStub("/api/hosts/" + VLA_HOSTNAME + "/attributes/dns_records", "oops_vla_dns_response.json");

        addJsonStub("/api/hosts/" + SAS_HOSTNAME + "/attributes/disks", "oops_sas_disks_response.json");
        addJsonStub("/api/hosts/" + SAS_HOSTNAME + "/attributes/cpu_info", "oops_sas_cpu_response.json");
        addJsonStub("/api/hosts/" + SAS_HOSTNAME + "/attributes/vlan_list", "oops_sas_vlans_response.json");
        addJsonStub("/api/hosts/" + SAS_HOSTNAME + "/attributes/dns_records", "oops_sas_dns_response.json");
    }

    private void addJsonStub(String url, String jsonFile) {
        try {
            wireMockRule.stubFor(get(urlEqualTo(url))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(getTestResourceAsString(RESOURCE_DIRECTORY_PATH + jsonFile))));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource", e);
        }
    }

    @Test
    public void uploadServersInfoToSaasTest() throws Exception {

        List<SaasDocument> saasDocuments = serversInfoConverter.makeSaasDocumentsList();
        Assert.assertEquals(2, saasDocuments.size());
        Assert.assertEquals(getTestResourceAsString(RESOURCE_DIRECTORY_PATH + "saas_document.json"),
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString((saasDocuments.get(0))));

        ListF<SaasDocumentYt> ytSaasDocuments = SaasUtil.convertToYtSaasDocumentsListF(saasDocuments);
        Assert.assertEquals(2, ytSaasDocuments.size());
        Assert.assertEquals(getTestResourceAsString(RESOURCE_DIRECTORY_PATH + "saas_yt_document.json"),
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString((ytSaasDocuments.get(0))));

        ListF<YTreeMapNode> table = ytSaasDocuments.map(saasDocumentYt ->
            YTree.mapBuilder().key("JsonMessage").value(saasDocumentYt.toString()).buildMap()
        );
        checkYtTablesHasNextParameters("//path/to/table", YTableEntryTypes.YSON, table);

        ytService.uploadDocuments("//path/to/table", saasDocuments);
    }

    private void checkYtTablesHasNextParameters(String yPath, YTableEntryType<YTreeMapNode> yTableEntryType,
                                                ListF<YTreeMapNode> table) {
        doAnswer(invocationOnMock -> {
            Assert.assertEquals(invocationOnMock.getArgument(0), YPath.simple(yPath));
            Assert.assertEquals(invocationOnMock.getArgument(1), yTableEntryType);
            Assert.assertEquals(invocationOnMock.getArgument(2), table);
            return null;
        }).when(ytTables).write(any(YPath.class), any(YTableEntryType.class), any(IterableF.class));
    }
}
