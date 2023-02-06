package ru.yandex.market.tsum.infrasearch.upload.tasks.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
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
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.infrasearch.document.SaasDocumentYt;
import ru.yandex.market.tsum.infrasearch.upload.SaasUtil;
import ru.yandex.market.tsum.infrasearch.upload.YtService;
import ru.yandex.market.tsum.registry.v2.dao.ComponentsDao;
import ru.yandex.market.tsum.registry.v2.dao.InstallationsDao;
import ru.yandex.market.tsum.registry.v2.dao.ServicesDao;
import ru.yandex.market.tsum.registry.v2.dao.model.Component;
import ru.yandex.market.tsum.registry.v2.dao.model.Installation;
import ru.yandex.market.tsum.registry.v2.dao.model.Service;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.tsum.core.TestResourceLoader.getTestResourceAsString;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class, ServicesDao.class, InstallationsDao.class, ComponentsDao.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RegistryInfoSaasExportTest {
    private static final Gson GSON = new GsonBuilder().create();

    @Autowired
    private ServicesDao servicesDao;

    @Autowired
    private ComponentsDao componentsDao;

    @Autowired
    private InstallationsDao installationsDao;

    private final static ObjectMapper mapper = new ObjectMapper();

    private final static String RESOURCE_DIRECTORY_PATH = "infrasearch/upload/tasks/registry/";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    private YtTables ytTables;
    private YtService ytService;
    private RegistryInfoConverter registryInfoConverter;

    @Before
    public void setUp() throws Exception {
        Yt yt = mock(Yt.class, Mockito.RETURNS_DEEP_STUBS);
        ytTables = yt.tables();
        ytService = new YtService(yt);
        Service service = GSON.fromJson(
            getTestResourceAsString(RESOURCE_DIRECTORY_PATH + "service.json"), Service.class);
        servicesDao.save(service);
        Component component = GSON.fromJson(
            getTestResourceAsString(RESOURCE_DIRECTORY_PATH + "component.json"), Component.class);
        componentsDao.save(component);
        Installation installation = GSON.fromJson(
            getTestResourceAsString(RESOURCE_DIRECTORY_PATH + "installation.json"), Installation.class);
        installationsDao.save(installation);
        registryInfoConverter = new RegistryInfoConverter(servicesDao, componentsDao, installationsDao, 0);
    }

    @Test
    public void uploadRegistryInfoToSaasTest() throws Exception {

        List<SaasDocument> saasDocuments = registryInfoConverter.makeSaasDocumentsList();
        Assert.assertEquals(1, saasDocuments.size());
        Assert.assertEquals(getTestResourceAsString(RESOURCE_DIRECTORY_PATH + "saas_document.json"),
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString((saasDocuments.get(0))));

        ListF<SaasDocumentYt> ytSaasDocuments = SaasUtil.convertToYtSaasDocumentsListF(saasDocuments);

        Assert.assertEquals(1, ytSaasDocuments.size());
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
