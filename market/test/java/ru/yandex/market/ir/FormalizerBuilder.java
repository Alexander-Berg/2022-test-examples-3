package ru.yandex.market.ir;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.mockito.Mockito;
import org.xml.sax.SAXException;

import ru.yandex.market.ir.dao.CategoryCreator;
import ru.yandex.market.ir.dao.CategoryXmlDao;
import ru.yandex.market.ir.dao.ReportAliasesDao;
import ru.yandex.market.ir.dao.ReportParamsBlacklist;
import ru.yandex.market.ir.decision.DependencyRuleFactory;
import ru.yandex.market.ir.decision.RuleFactory;
import ru.yandex.market.ir.parser.TypeFactory;
import ru.yandex.market.ir.parser.view.automat.StateFactory;
import ru.yandex.market.ir.processor.FormalizationStringIndexer;
import ru.yandex.market.ir.processor.PatternEntriesExtractor;
import ru.yandex.market.ir.utils.InstanceTypeService;
import ru.yandex.utils.string.DeNullingProcessor;
import ru.yandex.utils.string.indexed.IndexedStringFactory;

import static org.junit.Assert.assertNotEquals;

public class FormalizerBuilder {

    private static final int LAST_MODIFICATION_TIME = 1511698114;
    private String categoryPath = TestUtil.getSrcTestResourcesPath();
    //в случае запуска отдельного теста надо прописывать абсолютный путь(ну или придумать другое решение)
    //например "/Users/gavrilov-mi/arcadia/market/ir/formalizer/src/test/resources";

    private String fullPath;
    private int categoryId = -1;
    private String categoryPathPostfix = "";

    private boolean isRootCategoryAcceptable = false;
    private String reportAliasesFileName;

    public DefaultFormalizer build() throws ParserConfigurationException, SAXException, IOException {
        validate();

        CategoryXmlDao categoryXmlDao = new CategoryXmlDao();
        if (categoryPathPostfix.isBlank()) {
            fullPath = categoryPath;
        } else {
            fullPath = categoryPath + "/" + categoryPathPostfix;
        }
        categoryXmlDao.setDownloadedDumpsPath(fullPath);
        categoryXmlDao.setPrestableDumpsPath(fullPath);
        categoryXmlDao.setStableDumpsPath(fullPath);

        IndexedStringFactory indexedStringFactory = new IndexedStringFactory();
        indexedStringFactory.setSteam(true);
        indexedStringFactory.setStringProcessor(new DeNullingProcessor());

        CategoryCreator categoryCreator = new CategoryCreator();

        categoryCreator.setIndexedStringFactory(indexedStringFactory);
        categoryCreator.setRuleFactory(new RuleFactory());
        categoryCreator.setStateFactory(new StateFactory());
        TypeFactory typeFactory = new TypeFactory();
        typeFactory.setIndexedStringFactory(indexedStringFactory);
        if (reportAliasesFileName != null && !reportAliasesFileName.isBlank()) {
            typeFactory.setReportAliasesDao(new ReportAliasesDao(fullPath + "/" + reportAliasesFileName));
        }
        categoryCreator.setTypeFactory(typeFactory);
        categoryCreator.setDependencyRuleFactory(new DependencyRuleFactory());

        categoryXmlDao.setCategoryCreator(categoryCreator);
        categoryXmlDao.setInstanceTypeService(new InstanceTypeService());

        FormalizerKnowledgeData formalizerKnowledge = categoryXmlDao.loadCategory(categoryId, LAST_MODIFICATION_TIME);

        DefaultFormalizer formalizer = new DefaultFormalizer();

        PatternEntriesExtractor patternEntriesExtractor = new PatternEntriesExtractor(true);
        patternEntriesExtractor.setIndexedStringFactory(indexedStringFactory);
        FormalizerContractor formalizerContractor = new FormalizerContractor();
        formalizerContractor.setPatternEntriesExtractor(patternEntriesExtractor);
        formalizer.setFormalizerContractor(formalizerContractor);
        formalizerContractor.setReportParamsBlacklist(new ReportParamsBlacklist(categoryPath + "/blacklist.txt"));

        FormalizationStringIndexer formalizationStringIndexer = new FormalizationStringIndexer();
        formalizationStringIndexer.setIndexedStringFactory(indexedStringFactory);
        try {
            formalizationStringIndexer.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        formalizer.setStringIndexer(formalizationStringIndexer);

        KnowledgeHolder knowledgeHolder = Mockito.mock(KnowledgeHolder.class);
        Mockito.when(knowledgeHolder.isAlive()).thenReturn(true);
        Mockito.when(knowledgeHolder.getFormalizerKnowledge(categoryId, isRootCategoryAcceptable))
                .thenReturn(formalizerKnowledge);

        formalizer.setKnowledgeHolder(knowledgeHolder);

        return formalizer;
    }

    public FormalizerBuilder setCategoryPathPostfix(String categoryPathPostfix) {
        this.categoryPathPostfix = categoryPathPostfix;
        return this;
    }

    public FormalizerBuilder setCategoryPath(String categoryPath) {
        this.categoryPath = categoryPath;
        return this;
    }

    public FormalizerBuilder setCategoryId(int categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    private void validate() {
        assertNotEquals(-1, categoryId);
    }

    public FormalizerBuilder setRootCategoryAcceptable(boolean rootCategoryAcceptable) {
        isRootCategoryAcceptable = rootCategoryAcceptable;
        return this;
    }

    public FormalizerBuilder setReportAliasesFileName(String reportAliasesFileName) {
        this.reportAliasesFileName = reportAliasesFileName;
        return this;
    }
}
