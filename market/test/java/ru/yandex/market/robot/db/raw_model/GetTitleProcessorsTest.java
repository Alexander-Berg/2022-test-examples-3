package ru.yandex.market.robot.db.raw_model;

import io.qameta.allure.Issue;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import it.unimi.dsi.fastutil.ints.IntArraySet;
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
import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.market.robot.auto_generation.TitleProcessorFactory;
import ru.yandex.market.robot.db.TitleProcessorDao;
import ru.yandex.market.robot.db.raw_model.config.MarketRawModelAccessorsTestConfig;
import ru.yandex.market.robot.db.raw_model.tables.SessionLogTable;
import ru.yandex.market.robot.db.raw_model.tables.TitleProcessorTable;
import ru.yandex.market.robot.shared.clusterizer.title_utils.TitleProcessor;
import ru.yandex.market.robot.shared.clusterizer.title_utils.TitleProcessorDto;
import ru.yandex.market.robot.shared.clusterizer.title_utils.TitleProcessorType;
import ru.yandex.market.test.db.DatabaseTester;
import ru.yandex.market.test.util.random.RandomBean;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.ninja_squad.dbsetup.Operations.sql;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.market.test.db.DatabaseTester.Utils.insert;


/**
 * @author jkt on 04.12.17.
 */

@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class})
@ContextConfiguration(classes = MarketRawModelAccessorsTestConfig.class)
@Transactional
@Tag("Integrational")
@Issue("MARKETIR-4136")
public class GetTitleProcessorsTest {

    private static final int CATEGORY_ID = 11;
    private static final int ANOTHER_CATEGORY_ID = 22;
    private static final int CATEGORY_WITHOUT_TITLE_PROCESSORS = 999999;


    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();


    @Autowired
    DatabaseTester dataBase;

    @Autowired
    TitleProcessorDao titleProcessorDao;


    private TitleProcessorDto titleProcessorData;


    @Before
    public void prepareDataBase() {
        titleProcessorData = randomTitleProcessor();

        titleProcessorDao.getTitleProcessors(new IntArraySet(1));

        dataBase.checkStateMatches(SessionLogTable.empty());

        dataBase.modify(insert(TitleProcessorTable.entryFor(CATEGORY_ID, titleProcessorData)));
    }


    @Test
    @DisplayName("TitleProcessorDao.getCategoryTitleProcessors отдает для категории верный title_processor")
    public void whenGetCategoryTitleProcessorsShouldReturnCorrectDataForAllTypes() {
        TitleProcessorDto[] titleProcessorsOfAllTypes = titleProcessorsOfAllTypes();
        dataBase.modify(insert(TitleProcessorTable.entryFor(CATEGORY_ID, titleProcessorsOfAllTypes)));

        List<TitleProcessorDto> categoryTitleProcessors = getCategoryTitleProcessorsFor(CATEGORY_ID);

        assertThat(categoryTitleProcessors).containsOnlyOnce(titleProcessorsOfAllTypes);

    }

    @Test
    @DisplayName("TitleProcessorDao.getTitleProcessors отдает для категории верный title_processor")
    public void whenGetTitleProcessorsShouldReturnCorrectDataForAllTypes() {
        TitleProcessorDto[] titleProcessorsOfAllTypes = titleProcessorsOfAllTypes();
        dataBase.modify(insert(TitleProcessorTable.entryFor(CATEGORY_ID, titleProcessorsOfAllTypes)));

        MultiMap<Integer, TitleProcessor> categoryTitleProcessors = getTitleProcessorsFor(CATEGORY_ID);

        assertThat(categoryTitleProcessors.get(CATEGORY_ID))
            .contains(expectedTitleProcessors(titleProcessorsOfAllTypes));

    }


    @Test
    @DisplayName(
        "TitleProcessorDao.getCategoryTitleProcessors отдает пустой список для категории без title_processor-ов"
    )
    public void whenTitleProcessorMissingGetCategoryTitleProcessorsShouldReturnNothing() {

        assertThat(getCategoryTitleProcessorsFor(CATEGORY_WITHOUT_TITLE_PROCESSORS)).isEmpty();
    }

    @Test
    @DisplayName("TitleProcessorDao.getTitleProcessors отдает пустой список для категории без title_processor-ов")
    public void whenTitleProcessorMissingGetTitleProcessorsShouldReturnNothing() {

        assertThat(getTitleProcessorsFor(CATEGORY_WITHOUT_TITLE_PROCESSORS).getMaybeEmpty(CATEGORY_ID)).isEmpty();
    }


    @Test
    @DisplayName("TitleProcessorDao.getCategoryTitleProcessors верно отдает несколько title_processor-ов")
    public void whenMultipleTitleProcessorsGetCategoryTitleProcessorsShouldReturnAll() {

        TitleProcessorDto anotherTitleProcessorData = randomTitleProcessor();
        dataBase.modify(insert(TitleProcessorTable.entryFor(CATEGORY_ID, anotherTitleProcessorData)));

        List<TitleProcessorDto> categoryTitleProcessors = getCategoryTitleProcessorsFor(CATEGORY_ID);

        assertThat(categoryTitleProcessors).containsExactlyInAnyOrder(titleProcessorData, anotherTitleProcessorData);
    }

    @Test
    @DisplayName("TitleProcessorDao.getTitleProcessors верно отдает несколько title_processor-ов")
    public void whenMultipleTitleProcessorsGetTitleProcessorsShouldReturnAll() {

        TitleProcessorDto anotherTitleProcessorData = randomTitleProcessor();
        dataBase.modify(insert(TitleProcessorTable.entryFor(CATEGORY_ID, anotherTitleProcessorData)));

        List<TitleProcessor> categoryTitleProcessors = getTitleProcessorsFor(CATEGORY_ID).get(CATEGORY_ID);

        assertThat(categoryTitleProcessors)
            .containsExactlyInAnyOrder(expectedTitleProcessors(titleProcessorData, anotherTitleProcessorData));
    }

    @Test
    @DisplayName("TitleProcessorDao.getTitleProcessors верно отдает несколько title_processor-ов")
    public void whenMultipleCategoriesRequestedGetTitleProcessorsShouldReturnAll() {

        TitleProcessorDto[] anotherCategoryTitleProcessorData = multipleRandomTitleProcessors(3);
        dataBase.modify(insert(TitleProcessorTable.entryFor(ANOTHER_CATEGORY_ID, anotherCategoryTitleProcessorData)));

        MultiMap<Integer, TitleProcessor> titleProcessors = getTitleProcessorsFor(CATEGORY_ID, ANOTHER_CATEGORY_ID);

        assertSoftly(soft -> {
            assertThat(titleProcessors.get(CATEGORY_ID))
                .containsExactlyInAnyOrder(expectedTitleProcessors(titleProcessorData));
            assertThat(titleProcessors.get(ANOTHER_CATEGORY_ID))
                .containsExactlyInAnyOrder(expectedTitleProcessors(anotherCategoryTitleProcessorData));
        });

    }


    @Test
    @DisplayName("TitleProcessorDao отдает пустые sources если не заданы для title_processor-а")
    public void whenSourceMissingShouldReturnEmptySources() {
        setSourcesFor(CATEGORY_ID, Stream.empty().toArray(Integer[]::new));

        TitleProcessorDto titleProcessor = getSingleCategoryTitleProcessorFor(CATEGORY_ID);

        assertThat(titleProcessor.getSources()).isEmpty();
    }


    @Test
    @DisplayName("TitleProcessorDao.getCategoryTitleProcessors парсит title_processor.sources, когда они заданы")
    public void whenSourcePresentGetCategoryTitleProcessorsShouldParseSources() {

        Integer[] sources = new Integer[]{1, 2, 3};

        setSourcesFor(CATEGORY_ID, sources);

        TitleProcessorDto titleProcessor = getSingleCategoryTitleProcessorFor(CATEGORY_ID);

        assertThat(titleProcessor.getSources()).containsExactlyInAnyOrder(sources);
    }

    @Test
    @DisplayName("TitleProcessorDao.getTitleProcessors парсит title_processor.sources, когда они заданы")
    public void whenSourcePresentGetTitleProcessorsShouldParseSources() {

        Integer[] sources = new Integer[]{1, 2, 3};

        setSourcesFor(CATEGORY_ID, sources);

        TitleProcessor titleProcessor = getSingleTitleProcessorFor(CATEGORY_ID);

        assertThat(titleProcessor.getSources()).containsExactlyInAnyOrder(sources);
    }


    @Step("Задаем title_processor.sources = {sources} для категории {categoryId}")
    private void setSourcesFor(int categoryId, Integer... sources) {
        String sourcesString = Stream.of(sources).map(Object::toString).collect(Collectors.joining("|"));

        String updateSourcesSql = String.format(
            "update title_processor " +
                "set sources = '%s' " +
                "where category_id = %s",
            sourcesString, categoryId);

        dataBase.modify(sql(updateSourcesSql));
    }


    private TitleProcessorDto getSingleCategoryTitleProcessorFor(int categoryId) {
        return getCategoryTitleProcessorsFor(categoryId).stream()
            .findAny()
            .orElseThrow(() -> new AssertionError("Нет ни одного процессора у категории: " + categoryId));
    }

    private TitleProcessor getSingleTitleProcessorFor(int categoryId) {
        return getTitleProcessorsFor(categoryId).getMaybeEmpty(categoryId).stream()
            .findAny()
            .orElseThrow(() -> new AssertionError("Нет ни одного процессора у категории: " + categoryId));
    }

    @Step("Запрашиваем title_processor для категории {categoryId}")
    private List<TitleProcessorDto> getCategoryTitleProcessorsFor(int categoryId) {
        return titleProcessorDao.getCategoryTitleProcessorDtos(categoryId);
    }

    @Step("Запрашиваем title_processor-ы для категорий {categories}")
    private MultiMap<Integer, TitleProcessor> getTitleProcessorsFor(int... categories) {
        return titleProcessorDao.getTitleProcessors(new IntArraySet(categories));
    }


    private TitleProcessor[] expectedTitleProcessors(TitleProcessorDto... titleProcessors) {
        return Stream.of(titleProcessors)
            .map(TitleProcessorFactory::getTitleProcessor)
            .toArray(TitleProcessor[]::new);
    }

    private TitleProcessorDto[] multipleRandomTitleProcessors(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> randomTitleProcessor())
            .toArray(TitleProcessorDto[]::new);
    }

    private TitleProcessorDto randomTitleProcessor() {
        TitleProcessorDto titleProcessor = RandomBean.generateComplete(TitleProcessorDto.class);
        titleProcessor.setSettings(random(String.class) + "|" + random(String.class));
        return titleProcessor;
    }

    private TitleProcessorDto[] titleProcessorsOfAllTypes() {
        return Stream.of(TitleProcessorType.values())
            .map(type -> {
                TitleProcessorDto titleProcessor = randomTitleProcessor();
                titleProcessor.setType(type);
                return titleProcessor;
            }).toArray(TitleProcessorDto[]::new);
    }
}
