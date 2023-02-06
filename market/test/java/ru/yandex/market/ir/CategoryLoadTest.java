package ru.yandex.market.ir;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.ir.dao.CategoryCreator;
import ru.yandex.market.ir.dao.CategoryXmlDao;
import ru.yandex.market.ir.decision.DependencyRuleFactory;
import ru.yandex.market.ir.decision.RuleFactory;
import ru.yandex.market.ir.parser.TypeFactory;
import ru.yandex.market.ir.parser.view.automat.StateFactory;
import ru.yandex.market.ir.utils.InstanceTypeService;
import ru.yandex.utils.string.DeNullingProcessor;
import ru.yandex.utils.string.indexed.IndexedStringFactory;

public class CategoryLoadTest {

    private static final int CATEGORY_ID = 13041567;
    private static final int LAST_MODIFICATION_TIME = 1511698114;
    private static final String CATEGORY_PATH = TestUtil.getSrcTestResourcesPath();

    private CategoryXmlDao categoryXmlDao;

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
        categoryXmlDao.setInstanceTypeService(new InstanceTypeService());
    }

    @Test
    public void test() throws ParserConfigurationException, SAXException, IOException {
        FormalizerKnowledgeData formalizerKnowledge = categoryXmlDao.loadCategory(CATEGORY_ID, LAST_MODIFICATION_TIME);

        Assert.assertEquals(CATEGORY_ID, formalizerKnowledge.getCategoryId());
    }
}
