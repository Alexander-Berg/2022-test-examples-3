package ru.yandex.market.ir;

import java.io.IOException;
import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import ru.yandex.ir.formalize.FormalizerClient;
import ru.yandex.market.ir.dao.CategoryCreator;
import ru.yandex.market.ir.dao.CategoryXmlDao;
import ru.yandex.market.ir.decision.DependencyRuleFactory;
import ru.yandex.market.ir.decision.RuleFactory;
import ru.yandex.market.ir.parser.TypeFactory;
import ru.yandex.market.ir.parser.view.automat.StateFactory;
import ru.yandex.market.ir.utils.InstanceTypeService;
import ru.yandex.utils.string.DeNullingProcessor;
import ru.yandex.utils.string.indexed.IndexedStringFactory;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.ir.formalize.FormalizerClient.REPORT;
import static ru.yandex.ir.formalize.FormalizerClient.UC;

public class ClientLoadTest {
    private static final int CATEGORY_FOR_REPORT = 1;
    private static final int CATEGORY_FOR_UC = 2;
    private static final int LAST_MODIFICATION_TIME = 1511698114;
    private static final String CATEGORY_PATH = TestUtil.getSrcTestResourcesPath() + "/client_load_test";

    private CategoryXmlDao categoryXmlDao;
    private InstanceTypeService instanceTypeService;

    @Before
    public void setup() {
        categoryXmlDao = new CategoryXmlDao();
        categoryXmlDao.setDownloadedDumpsPath(CATEGORY_PATH);
        categoryXmlDao.setPrestableDumpsPath(CATEGORY_PATH);
        categoryXmlDao.setStableDumpsPath(CATEGORY_PATH);

        IndexedStringFactory indexedStringFactory = new IndexedStringFactory();
        indexedStringFactory.setSteam(true);
        indexedStringFactory.setStringProcessor(new DeNullingProcessor());

        CategoryCreator categoryCreator = new CategoryCreator();

        categoryCreator.setIndexedStringFactory(indexedStringFactory);
        categoryCreator.setRuleFactory(new RuleFactory());
        categoryCreator.setStateFactory(new StateFactory());
        TypeFactory typeFactory = new TypeFactory();
        typeFactory.setIndexedStringFactory(indexedStringFactory);
        categoryCreator.setTypeFactory(typeFactory);
        categoryCreator.setDependencyRuleFactory(new DependencyRuleFactory());

        categoryXmlDao.setCategoryCreator(categoryCreator);
        instanceTypeService = mock(InstanceTypeService.class);
        categoryXmlDao.setInstanceTypeService(instanceTypeService);
    }

    @Test
    public void loadCategoryForReport() throws IOException, ParserConfigurationException, SAXException {
        assertLoad(REPORT, CATEGORY_FOR_REPORT, formalizerKnowledge -> assertEquals(CATEGORY_FOR_REPORT,
                formalizerKnowledge.getCategoryId()));
    }

    @Test
    public void loadUcCategoryForReport()
            throws IOException, ParserConfigurationException, SAXException {
        assertLoad(UC, CATEGORY_FOR_REPORT, Assert::assertNull);
    }

    @Test
    public void loadCategoryForUc() throws IOException, ParserConfigurationException, SAXException {
        assertLoad(UC, CATEGORY_FOR_UC, formalizerKnowledge -> assertEquals(CATEGORY_FOR_UC,
                formalizerKnowledge.getCategoryId()));
    }

    private void assertLoad(FormalizerClient client, int categoryId, Consumer<FormalizerKnowledgeData> assertF)
            throws IOException, SAXException, ParserConfigurationException {
        when(instanceTypeService.getType()).thenReturn(client);
        FormalizerKnowledgeData formalizerKnowledge = categoryXmlDao.loadCategory(categoryId, LAST_MODIFICATION_TIME);
        assertF.accept(formalizerKnowledge);
    }
}
