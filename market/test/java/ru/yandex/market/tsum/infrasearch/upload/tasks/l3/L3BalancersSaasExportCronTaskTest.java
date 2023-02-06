package ru.yandex.market.tsum.infrasearch.upload.tasks.l3;

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
import ru.yandex.market.saas.search.SaasSearchException;
import ru.yandex.market.tsum.clients.racktables.RacktablesClient;
import ru.yandex.market.tsum.infrasearch.model.SearchAttrs;
import ru.yandex.market.tsum.infrasearch.search.InfraSearchDao;
import ru.yandex.market.tsum.infrasearch.upload.SaasUtil;
import ru.yandex.market.tsum.infrasearch.upload.YtService;
import ru.yandex.market.tsum.infrasearch.document.SaasDocumentYt;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ru.yandex.market.tsum.core.TestResourceLoader.getTestResourceAsString;

public class L3BalancersSaasExportCronTaskTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    private final static ObjectMapper mapper = new ObjectMapper();

    private RacktablesClient racktablesClient;

    private YtTables ytTables;
    private YtService ytService;
    private InfraSearchDao infraSearchDao;

    @Before
    public void setUp() {
        racktablesClient = new RacktablesClient("http://localhost:"+wireMockRule.port());
        Yt yt = mock(Yt.class, Mockito.RETURNS_DEEP_STUBS);
        infraSearchDao = mock(InfraSearchDao.class, RETURNS_DEEP_STUBS);
        ytTables = yt.tables();
        ytService = new YtService(yt);
    }

    @Test
    public void exportTest() throws IOException, SaasSearchException {
        doReturn(Collections.emptyList()).when(infraSearchDao).searchAllDocuments(any(SearchAttrs.class), any(String.class));
        wireMockRule.stubFor(get(urlEqualTo("/export/golem-slb-list.php"))
            .willReturn(aResponse().withStatus(200).withBody(
                getTestResourceAsString("infrasearch/upload/tasks/l3/golem-slb-list.xml"))));

        wireMockRule.stubFor(get(urlEqualTo("/export/allvs.php?text=%7B%D0%9C%D0%B0%D1%80%D0%BA%D0%B5%D1%82%7D"))
            .willReturn(aResponse().withStatus(200).withBody(
                getTestResourceAsString("infrasearch/upload/tasks/l3/market-balancers.txt"))));

        List<SaasDocument> saasDocuments = L3BalancersServiceInfoConverter.makeSaasDocumentsList(racktablesClient, infraSearchDao, 0);
        Assert.assertEquals(1, saasDocuments.size());
        Assert.assertEquals(getTestResourceAsString("infrasearch/upload/tasks/l3/saas_document.json"),
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString((saasDocuments.get(0))));

        ListF<SaasDocumentYt> ytSaasDocuments = SaasUtil.convertToYtSaasDocumentsListF(saasDocuments);
        Assert.assertEquals(1, ytSaasDocuments.size());
        Assert.assertEquals(getTestResourceAsString("infrasearch/upload/tasks/l3/saas_yt_document.json"),
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
}
