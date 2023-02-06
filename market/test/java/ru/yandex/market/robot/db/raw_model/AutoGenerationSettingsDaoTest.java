package ru.yandex.market.robot.db.raw_model;

import com.ninja_squad.dbsetup.operation.CompositeOperation;
import com.ninja_squad.dbsetup.operation.Operation;
import io.github.benas.randombeans.api.EnhancedRandom;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
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
import ru.yandex.ir.modelsclusterizer.be.ClusterizerSettings;
import ru.yandex.market.robot.db.AutoGenerationSettingsDao;
import ru.yandex.market.robot.db.raw_model.config.MarketRawModelAccessorsTestConfig;
import ru.yandex.market.robot.db.raw_model.tables.CategoriesTable;
import ru.yandex.market.robot.db.raw_model.tables.CategoryParamsTable;
import ru.yandex.market.robot.db.raw_model.tables.SourcesTable;
import ru.yandex.market.robot.db.raw_model.tables.TitleProcessorTable;
import ru.yandex.market.robot.shared.clusterizer.CategorySettings;
import ru.yandex.market.robot.shared.clusterizer.CategorySettingsDto;
import ru.yandex.market.robot.shared.clusterizer.SourceSettings;
import ru.yandex.market.robot.shared.clusterizer.TitleParam;
import ru.yandex.market.robot.shared.clusterizer.title_utils.TitleProcessor;
import ru.yandex.market.robot.shared.clusterizer.title_utils.TitleProcessorDto;
import ru.yandex.market.robot.shared.clusterizer.title_utils.TitleProcessorType;
import ru.yandex.market.test.db.DatabaseTester;
import ru.yandex.market.test.util.random.RandomBean;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.market.robot.auto_generation.TitleProcessorFactory.getTitleProcessor;
import static ru.yandex.market.test.db.DatabaseTester.Utils.insert;

/**
 * @author jkt on 18.12.17.
 */
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class})
@ContextConfiguration(classes = MarketRawModelAccessorsTestConfig.class)
@Transactional
public class AutoGenerationSettingsDaoTest {

    private static final int CATEGORY_ID = 12345;
    private static final int ANOTHER_CATEGORY_ID = 7890;

    private static final String PARAMS = "params";
    private static final String TITLE_PROCESSORS = "titleProcessors";
    private static final String PARAM_NAME = "paramName";
    private static final String PROCESSORS = "processors";

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();


    @Autowired
    DatabaseTester dataBase;

    @Autowired
    AutoGenerationSettingsDao autoGenerationSettingsDao;


    private CategorySettingsDto expectedCategorySettings;


    @Before
    public void initCategoryData() {
        expectedCategorySettings = randomCategorySettings(CATEGORY_ID);

        dataBase.modify(insertCategoryDataToRequiredTables(expectedCategorySettings));
    }


    @Test
    public void whenLoadingSettingsForCategoryShouldReturnDataMatchingDatabase() {
        ClusterizerSettings clusterizerSettings = autoGenerationSettingsDao.loadCategory(CATEGORY_ID);

        assertThat(clusterizerSettings.getCategorySettings())
            .as("Выгруженные настройки категории не совпадают с данными в базе")
            .isEqualToIgnoringGivenFields(expectedCategorySettings, PARAMS, TITLE_PROCESSORS);
    }

    @Test
    public void whenLoadingSettingsForCategoryShouldReturnTitleProcessorsMatchingDatabase() {
        ClusterizerSettings clusterizerSettings = autoGenerationSettingsDao.loadCategory(CATEGORY_ID);

        assertThat(clusterizerSettings.getCategorySettings().getTitleProcessors())
            .as("Выгруженные titleProcessors не совпадают с данными в базе")
            .containsExactlyInAnyOrder(expectedTitleProcessors(expectedCategorySettings.getTitleProcessors()));
    }

    @Test
    public void whenRemoveFromTitleParamShouldAddToRemovingFormalizedParams() {
        TitleParam titleParam = randomParameter();
        titleParam.setRemoveFromTitle(true);

        setParams(expectedCategorySettings, titleParam);

        ClusterizerSettings clusterizerSettings = autoGenerationSettingsDao.loadCategory(CATEGORY_ID);

        assertThat(clusterizerSettings.getRemovingFormalizedParams())
            .as("Неверные значения параметров")
            .containsExactlyInAnyOrder(titleParam.getParamId());
    }


    @Test
    public void whenAddOnDuplicateParamShouldAddValueToAddOnDuplicateParams() {
        TitleParam titleParam = randomParameter();
        titleParam.setAddOnDuplicate(true);

        setParams(expectedCategorySettings, titleParam);

        ClusterizerSettings clusterizerSettings = autoGenerationSettingsDao.loadCategory(CATEGORY_ID);

        assertThat((List<Integer>) clusterizerSettings.getAddOnDuplicateParams())
            .as("Неверные значения параметров")
            .containsExactlyInAnyOrder(titleParam.getParamId());
    }


    @Test
    public void whenAddToTitleNotEmptyShouldAddValueForAddToTitleParams() {
        TitleParam titleParam = randomParameter();
        titleParam.setAddOnDuplicate(true);
        titleParam.setAddToTitle(random(String.class));

        setParams(expectedCategorySettings, titleParam);

        ClusterizerSettings clusterizerSettings = autoGenerationSettingsDao.loadCategory(CATEGORY_ID);

        assertThat(clusterizerSettings.getParamAddString().get(titleParam.getParamId()))
            .as("Неверные значения параметров")
            .isEqualTo(titleParam.getAddToTitle());
    }

    @Test
    public void whenAddToTitleEmptyShouldNotAddValueForAddToTitleParams() {
        Collection<String> aliases = EnhancedRandom.randomCollectionOf(3, String.class);

        TitleParam titleParam = randomParameter();
        titleParam.setAliases(aliases.stream().collect(Collectors.joining("|")));

        setParams(expectedCategorySettings, titleParam);

        ClusterizerSettings clusterizerSettings = autoGenerationSettingsDao.loadCategory(CATEGORY_ID);

        assertSoftly(soft -> {
            soft.assertThat(clusterizerSettings.getAliasToParam().keySet())
                .as("Неверно распарсились алиасы")
                .containsExactlyInAnyOrder(aliases.stream().map(String::toLowerCase).toArray(String[]::new));

            aliases.forEach(alias ->
                soft.assertThat(clusterizerSettings.getAliasToParam().get(alias.toLowerCase()))
                    .as("Неверный параметр для алиаса " + alias)
                    .isEqualToIgnoringGivenFields(titleParam, PARAM_NAME, PROCESSORS));
        });
    }

    @Test
    public void whenLoadingAllCategoriesShouldReturnAllCategories() {
        CategorySettingsDto anotherCategory = randomCategorySettings(ANOTHER_CATEGORY_ID);
        dataBase.modify(insertCategoryDataToRequiredTables(anotherCategory));

        Int2ObjectMap<ClusterizerSettings> clusterizerSettings = autoGenerationSettingsDao.loadCategories();

        assertThat(clusterizerSettings.keySet())
            .as("Возвращает настройки не для всех категорий из базы")
            .containsExactlyInAnyOrder(expectedCategorySettings.getCategoryId(), anotherCategory.getCategoryId());
    }

    @Test
    public void whenCategoryNotEnabledShouldIgnore() {
        CategorySettingsDto disabledCategory = randomCategorySettings();
        disabledCategory.setEnabled(false);
        dataBase.modify(insertCategoryDataToRequiredTables(disabledCategory));

        Int2ObjectMap<ClusterizerSettings> clusterizerSettings = autoGenerationSettingsDao.loadCategories();

        assertThat(clusterizerSettings.keySet())
            .as("Возвращает категорию с enabled=false")
            .doesNotContain(disabledCategory.getCategoryId());
    }


    @Test
    public void whenCategoryLoadedShouldEnableStopWordMachine() {
        ClusterizerSettings clusterizerSettings = autoGenerationSettingsDao.loadCategory(CATEGORY_ID);

        assertThat(clusterizerSettings.getStopWordsMachine())
            .as("Не инициализировался обработчик стоп слов")
            .isNotNull();
    }

    private TitleProcessor[] expectedTitleProcessors(Collection<TitleProcessorDto> titleProcessors) {
        return titleProcessors.stream()
            .map(titleProcessor -> getTitleProcessor(titleProcessor.getType().ordinal(), titleProcessor.getSettings()))
            .toArray(TitleProcessor[]::new);
    }


    private TitleParam randomParameter() {
        return RandomBean.generateComplete(TitleParam.class);
    }

    private CategorySettingsDto randomCategorySettings(int categoryId) {
        CategorySettingsDto categorySettings = randomCategorySettings();
        categorySettings.setEnabled(true);
        categorySettings.setCategoryId(categoryId);
        return categorySettings;
    }

    private CategorySettingsDto randomCategorySettings() {
        EnhancedRandom generator = RandomBean.defaultRandom();

        CategorySettingsDto categorySettings = generator.nextObject(CategorySettingsDto.class);

        for (TitleProcessorDto titleProcessor : categorySettings.getTitleProcessors()) {
            if (titleProcessor.getType() != TitleProcessorType.REPLACE) {
                continue;
            }
            titleProcessor.setSettings(
                generator.nextObject(String.class) + '|' + generator.nextObject(String.class)
            ); // replace title processor require '|' inside pattern
        }

        categorySettings.setSources(generator.objects(SourceSettings.class, 3)
            .collect(Collectors.toMap(
                SourceSettings::getSourceId,
                sourceSettings -> {
                    sourceSettings.setSourceName(null); // не используется в этой логике
                    return sourceSettings;
                }
            )));

        return categorySettings;
    }


    private void setParams(CategorySettingsDto categoryData, TitleParam... params) {
        categoryData.setParams(Stream.of(params).collect(Collectors.toList()));

        dataBase.modify(
            deleteAllFrom(CategoryParamsTable.NAME),
            insert(CategoryParamsTable::entryFor, categoryData)
        );
    }


    private Operation insertCategoryDataToRequiredTables(CategorySettingsDto... categoriesData) {
        return CompositeOperation.sequenceOf(
            insert(CategoriesTable::entryFor, categoriesData),
            insert(CategoryParamsTable::entryFor, categoriesData),
            insert(SourcesTable::entryFor, categoriesData),
            insert(TitleProcessorTable::entryFor, categoriesData)
        );
    }

}
