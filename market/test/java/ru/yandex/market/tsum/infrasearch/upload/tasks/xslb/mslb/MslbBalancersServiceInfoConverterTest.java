package ru.yandex.market.tsum.infrasearch.upload.tasks.xslb.mslb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
import ru.yandex.market.tsum.clients.arcadia.ArcanumClient;
import ru.yandex.market.tsum.infrasearch.model.SearchAttrs;
import ru.yandex.market.tsum.infrasearch.search.InfraSearchDao;
import ru.yandex.market.tsum.infrasearch.upload.SaasUtil;
import ru.yandex.market.tsum.infrasearch.upload.YtService;
import ru.yandex.market.tsum.infrasearch.document.SaasDocumentYt;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static ru.yandex.market.tsum.core.TestResourceLoader.getTestResourceAsString;

public class MslbBalancersServiceInfoConverterTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    private final static ObjectMapper mapper = new ObjectMapper();
    private static final String NGINX_CONF_PROD_STATIC = "market/sre/conf/slb-nginx/prod/etc/nginx/conf-static";
    private static final String NGINX_CONF_TEST_STATIC = "market/sre/conf/slb-nginx/test/etc/nginx/conf-static";
    private static final String HAPROXY_CONF_PROD_STATIC = "market/sre/conf/slb-haproxy/prod/etc/haproxy/conf-static";
    private static final String HAPROXY_CONF_TEST_STATIC = "market/sre/conf/slb-haproxy/test/etc/haproxy/conf-static";
    private static final String NGINX_CONF_PROD_URL_BASE =
        "market/sre/conf/slb-nginx/prod/etc/nginx/values-available";
    private static final String HAPROXY_CONF_PROD_URL_BASE =
        "market/sre/conf/slb-haproxy/prod/etc/haproxy/values-available";
    private static final String NGINX_CONF_TEST_URL_BASE =
        "market/sre/conf/slb-nginx/test/etc/nginx/values-available";
    private static final String HAPROXY_CONF_TEST_URL_BASE =
        "market/sre/conf/slb-haproxy/test/etc/haproxy/values-available";
    private ArcanumClient arcanumClient;

    private YtTables ytTables;
    private YtService ytService;
    private InfraSearchDao infraSearchDao;

    @Before
    public void setUp() {
        arcanumClient = new ArcanumClient("http://localhost:" + wireMockRule.port(), "qwerty");
        Yt yt = mock(Yt.class, Mockito.RETURNS_DEEP_STUBS);
        infraSearchDao = mock(InfraSearchDao.class, RETURNS_DEEP_STUBS);
        ytTables = yt.tables();
        ytService = new YtService(yt);
    }

    @Test
    public void exportTest() throws Exception {
        doReturn(Collections.emptyList()).when(infraSearchDao).searchAllDocuments(any(SearchAttrs.class), any(String.class));
        createStubsForNginx(NGINX_CONF_PROD_URL_BASE);
        createStubsForNginx(NGINX_CONF_TEST_URL_BASE);

        createStubsForHaProxy(HAPROXY_CONF_PROD_URL_BASE);
        createStubsForHaProxy(HAPROXY_CONF_TEST_URL_BASE);

        createStaticStubs(NGINX_CONF_PROD_STATIC);
        createStaticStubs(NGINX_CONF_TEST_STATIC);
        createStaticStubs(HAPROXY_CONF_PROD_STATIC);
        createStaticStubs(HAPROXY_CONF_TEST_STATIC);

        MslbBalancersServiceInfoConverter converter = new MslbBalancersServiceInfoConverter(arcanumClient, infraSearchDao);
        List<SaasDocument> saasDocuments = converter.makeSaasDocumentList(0);
        Assert.assertEquals(2, saasDocuments.size());
        Assert.assertEquals(getTestResourceAsString("infrasearch/upload/tasks/xslb/mslb/saas_document.json"),
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString((saasDocuments.get(0))));

        ListF<SaasDocumentYt> ytSaasDocuments = SaasUtil.convertToYtSaasDocumentsListF(saasDocuments);
        Assert.assertEquals(2, ytSaasDocuments.size());
        Assert.assertEquals(getTestResourceAsString("infrasearch/upload/tasks/xslb/mslb/saas_yt_document.json")
                .replace("https://mslb.yandex-team.ru", "http://localhost:"+wireMockRule.port()),
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString((ytSaasDocuments.get(0))));

        ListF<YTreeMapNode> table = ytSaasDocuments.map(saasDocumentYt ->
            YTree.mapBuilder().key("JsonMessage").value(saasDocumentYt.toString()).buildMap()
        );

        doAnswer(invocationOnMock -> {
            Assert.assertEquals(invocationOnMock.getArgument(0), YPath.simple("//path/to/table"));
            Assert.assertEquals(invocationOnMock.getArgument(1), YTableEntryTypes.YSON);
            Assert.assertEquals(invocationOnMock.getArgument(2), table);
            return null;
        }).when(ytTables).write(any(YPath.class), any(YTableEntryType.class), any(IterableF.class));

        ytService.uploadDocuments("//path/to/table", saasDocuments);
    }

    private void createStaticStubs(String directoryUrl) throws IOException {
        wireMockRule.stubFor(get(
            urlEqualTo("/v2/repos/arc/tree/trunk?path=" +
                directoryUrl.replaceAll("/", "%2F")
                + "&fields=children%28name%2Ctype%29"))
            .withHeader("Authorization", equalTo("OAuth qwerty"))
            .willReturn(aResponse().withStatus(200).withBody(
                getTestResourceAsString("infrasearch/upload/tasks/xslb/mslb/empty_directory.json"))));
    }

    private void createStubsForNginx(String directoryUrl) throws IOException {
        wireMockRule.stubFor(get(
            urlEqualTo("/v2/repos/arc/tree/trunk?path=" +
                directoryUrl.replaceAll("/", "%2F")
                + "&fields=children%28name%2Ctype%29"))
            .withHeader("Authorization", equalTo("OAuth qwerty"))
            .willReturn(aResponse().withStatus(200).withBody(
                getTestResourceAsString("infrasearch/upload/tasks/xslb/mslb/nginx_directory.json"))));
        wireMockRule.stubFor(get(urlEqualTo("/tree/blob/trunk/arcadia/" + directoryUrl + "/nginx.yaml"))
            .withHeader("Authorization", equalTo("OAuth qwerty"))
            .willReturn(aResponse().withStatus(200).withBody(
                getTestResourceAsString("infrasearch/upload/tasks/xslb/mslb/nginx-response.yaml"))));
    }

    private void createStubsForHaProxy(String directoryUrl) throws IOException {
        wireMockRule.stubFor(get(urlEqualTo("/v2/repos/arc/tree/trunk?path=" +
            directoryUrl.replaceAll("/", "%2F") +
            "&fields=children%28name%2Ctype%29"))
            .withHeader("Authorization", equalTo("OAuth qwerty"))
            .willReturn(aResponse().withStatus(200).withBody(
                getTestResourceAsString("infrasearch/upload/tasks/xslb/mslb/haproxy_directory.json"))));
        wireMockRule.stubFor(get(urlEqualTo("/tree/blob/trunk/arcadia/" + directoryUrl + "/haproxy.yaml"))
            .withHeader("Authorization", equalTo("OAuth qwerty"))
            .willReturn(aResponse().withStatus(200).withBody(
                getTestResourceAsString("infrasearch/upload/tasks/xslb/mslb/haproxy-response.yaml"))));
    }
}
