package ru.yandex.market.tsum.infrasearch.upload.tasks.nanny;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.*;
import org.mockito.Mockito;
import ru.yandex.bolts.collection.IterableF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.saas.indexer.document.SaasDocument;
import ru.yandex.market.saas.indexer.document.SaasDocumentProperty;
import ru.yandex.market.tsum.clients.gencfg.GenCfgClient;
import ru.yandex.market.tsum.clients.nanny.NannyClient;
import ru.yandex.market.tsum.infrasearch.model.entities.nanny.services.SaasInstance;
import ru.yandex.market.tsum.infrasearch.model.entities.nanny.services.SaasServiceInfo;
import ru.yandex.market.tsum.infrasearch.upload.SaasUtil;
import ru.yandex.market.tsum.infrasearch.upload.YtService;
import ru.yandex.market.tsum.infrasearch.document.SaasDocumentYt;
import ru.yandex.market.tsum.infrasearch.upload.tasks.nanny.services.NannyServiceInfoConverter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.tsum.core.TestResourceLoader.getTestResourceAsString;

public class NannyServiceInfoSaasExportTest {
    private static final Gson GSON = new GsonBuilder().create();

    private final static ObjectMapper mapper = new ObjectMapper();

    private final static String RESOURCE_DIRECTORY_PATH = "infrasearch/upload/tasks/nanny/service/";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    private GenCfgClient genCfgClient;
    private NannyClient nannyClient;
    private YtTables ytTables;
    private YtService ytService;
    private NannyServiceInfoConverter nannyServiceInfoConverter;

    @Before
    public void setUp() throws Exception {
        nannyClient = new NannyClient("http://localhost:" + wireMockRule.port(),
            "AQAD-qJSJwR8AAABT2AmPI5EfUS_k4lv3Mw2nz0", null);
        genCfgClient = new GenCfgClient("http://localhost:" + wireMockRule.port() + "/");
        Yt yt = mock(Yt.class, Mockito.RETURNS_DEEP_STUBS);
        ytTables = yt.tables();
        ytService = new YtService(yt);
        nannyServiceInfoConverter = new NannyServiceInfoConverter(genCfgClient, nannyClient, 0);
    }

    @Before
    public void prepareWireMock() {
        addJsonStubWithAuthHeader("/v2/services/?category=%2Fmarket", "nanny_response_one_item.json");
        addJsonStubWithAuthHeader("/v2/services/?category=%2Fmarket&skip=1", "nanny_response_empty.json");
        addJsonStubWithAuthHeader("/v2/services/belmatter_testing_service/current_state/instances/", "nanny_response_instances.json");

        addJsonStub("/trunk/groups/MAN_SQR3", "gencfg_group_response.json");
        addJsonStub("/trunk/groups/MAN_SQR3/card", "gencfg_group_card_response.json");
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

    private void addJsonStubWithAuthHeader(String url, String jsonFile) {
        try {
            wireMockRule.stubFor(get(urlEqualTo(url))
                .withHeader("Authorization", equalTo("OAuth AQAD-qJSJwR8AAABT2AmPI5EfUS_k4lv3Mw2nz0"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(getTestResourceAsString(RESOURCE_DIRECTORY_PATH + jsonFile))));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource", e);
        }
    }

    @Test
    public void checkSaasDocumentAttrsTest() {
        List<SaasDocument> saasDocuments = nannyServiceInfoConverter.makeSaasDocumentsList();
        Assert.assertEquals(1, saasDocuments.size());
        SaasDocument document = saasDocuments.get(0);
        List<SaasDocumentProperty> properties = (List<SaasDocumentProperty>) document.getProperty("s_connections");
        List<String> instances = properties.stream()
            .map(property -> (String)property.getValue())
            .collect(Collectors.toList());

        Assert.assertEquals(1, instances.size());
        Assert.assertEquals("instance__man1-2955-man-sqr3-23280.gencfg-c.yandex.net", instances.get(0));
    }

    @Test
    public void checkSaasDocumentStructureTest() throws IOException {
        List<SaasDocument> saasDocuments = nannyServiceInfoConverter.makeSaasDocumentsList();
        Assert.assertEquals(1, saasDocuments.size());
        SaasDocument document = saasDocuments.get(0);
        SaasDocumentProperty property = (SaasDocumentProperty) document.getProperty("body");
        SaasServiceInfo serviceInfo = GSON.fromJson((String)property.getValue(), SaasServiceInfo.class);
        List<SaasInstance> instances = serviceInfo.getData().getInstances();

        Assert.assertEquals(1, instances.size());

        Assert.assertEquals("man1-2955-man-sqr3-23280.gencfg-c.yandex.net", instances.get(0).getInstanceHostname());
        Assert.assertEquals("man1-2955.search.yandex.net", instances.get(0).getServerHostname());
        Assert.assertEquals("23280", instances.get(0).getPort());
    }

    @Test
    public void uploadNannyServiceInfoToSaasTest() throws Exception {

        List<SaasDocument> saasDocuments = nannyServiceInfoConverter.makeSaasDocumentsList();
        Assert.assertEquals(1, saasDocuments.size());
        Assert.assertEquals(getTestResourceAsString(RESOURCE_DIRECTORY_PATH + "saas_document.json")
                .replace("https://nanny.yandex-team.ru", "http://localhost:"+wireMockRule.port()),
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString((saasDocuments.get(0))));

        ListF<SaasDocumentYt> ytSaasDocuments = SaasUtil.convertToYtSaasDocumentsListF(saasDocuments);

        Assert.assertEquals(1, ytSaasDocuments.size());
        Assert.assertEquals(getTestResourceAsString(RESOURCE_DIRECTORY_PATH + "saas_yt_document.json")
                .replace("https://nanny.yandex-team.ru", "http://localhost:"+wireMockRule.port()),
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
