package ru.yandex.market.robot.db.raw_model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.market.robot.auto_generation.InvalidPatternException;
import ru.yandex.market.robot.auto_generation.RemovePatternProcessor;
import ru.yandex.market.robot.db.TitleProcessorDao;
import ru.yandex.market.robot.db.raw_model.config.MarketRawModelAccessorsTestConfig;
import ru.yandex.market.robot.db.raw_model.tables.SessionLogTable;
import ru.yandex.market.robot.shared.clusterizer.title_utils.TitleProcessor;
import ru.yandex.market.robot.shared.clusterizer.title_utils.TitleProcessorDto;
import ru.yandex.market.robot.shared.clusterizer.title_utils.TitleProcessorType;
import ru.yandex.market.test.db.DatabaseTester;

import java.util.Collections;
import java.util.List;


/**
 * @author nkondratyeva
 */

@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class})
@ContextConfiguration(classes = MarketRawModelAccessorsTestConfig.class)
@Transactional
public class UpdateTitleProcessorsTest {
    private static final int CATEGORY_ID = 11;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    DatabaseTester dataBase;

    @Autowired
    TitleProcessorDao titleProcessorDao;

    @Before
    public void prepareDataBase() {
        dataBase.checkStateMatches(SessionLogTable.empty());
    }

    @Test(expected = InvalidPatternException.class)
    public void validationFailsForRemovePattern() {
        trySaveCategoryTitleProcessor("[", TitleProcessorType.REMOVE);
    }

    @Test
    public void saveOkForRemovePattern() {
        String pattern = "test";

        trySaveCategoryTitleProcessor(pattern, TitleProcessorType.REMOVE);

        List<TitleProcessor> processors = titleProcessorDao.getCategoryTitleProcessors(CATEGORY_ID);
        Assert.assertEquals(1, processors.size());
        TitleProcessor processor = processors.get(0);
        Assert.assertTrue(processor instanceof RemovePatternProcessor);
        Assert.assertEquals(pattern, processor.getSettings());
    }

    @Test(expected = InvalidPatternException.class)
    public void validationFailsForSelectPattern() {
        trySaveCategoryTitleProcessor("[", TitleProcessorType.SELECT);
    }

    @Test
    public void saveOkForSelectPattern() {
        trySaveCategoryTitleProcessor("test", TitleProcessorType.SELECT);
    }

    @Test(expected = InvalidPatternException.class)
    public void validationFailsForReplacePatternWithNoSplit() {
        trySaveCategoryTitleProcessor("no split", TitleProcessorType.REPLACE);
    }

    @Test(expected = InvalidPatternException.class)
    public void validationFailsForReplacePatternWithMoreThanOneSplit() {
        trySaveCategoryTitleProcessor("three|splits|in|pattern", TitleProcessorType.REPLACE);
    }

    @Test
    public void saveOkForReplacePattern() {
        trySaveCategoryTitleProcessor("[a-z]|[not pattern here", TitleProcessorType.REPLACE);
    }

    private void trySaveCategoryTitleProcessor(String pattern, TitleProcessorType type) {
        TitleProcessorDto tp = new TitleProcessorDto();
        tp.setSettings(pattern);
        tp.setType(type);
        titleProcessorDao.updateCategoryTitleProcessors(CATEGORY_ID, Collections.singletonList(tp));
    }
}
