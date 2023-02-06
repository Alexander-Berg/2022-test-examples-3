package ru.yandex.market.mbo.integration.test.params;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.mbo.db.MboDbSelector;
import ru.yandex.market.mbo.db.TovarTreeDao;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.params.ParameterDAO;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.InheritedParameter;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.params.ParameterOverride;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.integration.test.BaseIntegrationTest;
import ru.yandex.market.mbo.utils.RandomTestUtils;
import ru.yandex.market.mbo.utils.SameObjectEqualsWrapper;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author anmalysh
 * @since 3/12/2019
 */
@Ignore("flaky")
public class ParameterLoaderServiceTest extends BaseIntegrationTest {

    private static final String TARGET_OPTION_NAME = "targetOption";

    private static final String SQL_CREATE_SCHEMA_PG = "CREATE SCHEMA IF NOT EXISTS site_catalog";

    private static final String SQL_CREATE_SEQUENCE_PG =
        "CREATE SEQUENCE IF NOT EXISTS SITE_CATALOG.TOVAR_TREE_ID MINVALUE 1 MAXVALUE 1000000";

    private static final String SQL_SET_SEARCH_PATH = "SET search_path TO site_catalog";

    private static final String SQL_INSERT_INTO_CATEGORY
        = "insert into mc_category (hyper_id, tovar_id, tovar_entity_id, parent_id) values (0, -1, -1, -1)";


    @Inject
    private MboDbSelector funcTestsDbSelector;
    @Inject
    private JdbcTemplate contentJdbcTemplate;
    @Inject
    private JdbcTemplate siteCatalogPgJdbcTemplate;
    @Inject
    private ParameterDAO parameterDAO;
    @Inject
    private IParameterLoaderService parameterLoaderService;
    @Inject
    private TovarTreeDao tovarTreeDao;

    private EnhancedRandom random;

    @Before
    public void setUp() {
        random = RandomTestUtils.createNewRandom(1);

        // create global category
        funcTestsDbSelector.getProxyingJdbcTemplate().update(SQL_INSERT_INTO_CATEGORY);

        //init required state in local pg database
        siteCatalogPgJdbcTemplate.execute(SQL_CREATE_SCHEMA_PG);
        siteCatalogPgJdbcTemplate.execute(SQL_SET_SEARCH_PATH);
        siteCatalogPgJdbcTemplate.execute(SQL_CREATE_SEQUENCE_PG);
    }

    @SuppressWarnings("checkstyle:magicNumber")
    @Test
    public void testOptionsChainCreations() {
        List<TovarCategory> categories = createCategoriesForChains();
        TovarCategory category = categories.get(1);


        Map<Long, String> subHids = tovarTreeDao.getSubtreeIdsAndNames(category.getHid());
        assertThat(subHids.size()).isEqualTo(7L);

        List<CategoryParam> parameters = createAndOverrideGlobalParameter(categories);
        CategoryParam globalParameter = parameters.get(0);

        CategoryEntities categoryEntities = parameterLoaderService.loadCategoryEntitiesByHid(category.getHid());
        assertThat(categoryEntities.getParameters()).hasSize(1);

        List<Option> oldOptions = categoryEntities.getParameterById(globalParameter.getId()).getEffectiveOptions();

        assertThat(oldOptions).isEmpty();

        // создаем опцию в глобальном параметре
        OptionImpl globalOption = createOption(
            globalParameter,
            null,
            TARGET_OPTION_NAME);
        assertThat(globalOption.getName()).isEqualTo(TARGET_OPTION_NAME);

        // создать оверрайд в категории 1
        OptionImpl override1 = createOption(
            parameters.get(1),
            globalOption,
            TARGET_OPTION_NAME + " 1");

        CategoryEntities categoryEntities1 =
            parameterLoaderService.loadCategoryEntitiesByHid(categories.get(1).getHid());
        List<OptionImpl> options1 = getOptionsForTargetParam(globalParameter, categoryEntities1);
        assertThat(options1.size()).isEqualTo(1L);
        OptionImpl newOption1 = options1.get(0);
        assertThat(newOption1.getParentOptionId()).isEqualTo(globalOption.getId());

        // создать оверрайд в категории 2
        createOption(
            parameters.get(2),
            override1,
            TARGET_OPTION_NAME + " 2");


        // создать оверрайд в категории 4
        createOption(
            parameters.get(4),
            override1,
            TARGET_OPTION_NAME + " 4");

        // проверить что оверрайде в категории 2 не обновился родитель
        CategoryEntities categoryEntities2 =
            parameterLoaderService.loadCategoryEntitiesByHid(categories.get(2).getHid());
        List<OptionImpl> options2 = getOptionsForTargetParam(globalParameter, categoryEntities2);
        assertThat(options2.size()).isEqualTo(1L);
        OptionImpl newOption2 = options2.get(0);
        assertThat(newOption2.getParentOptionId()).isEqualTo(override1.getId());

        // создать оверрайд в категории 3
        OptionImpl override3 = createOption(
            parameters.get(3),
            override1,
            TARGET_OPTION_NAME + " 3");

        // проверить что в оверрайде в категории 4 обновился родитель на оверрайд в категории 3
        CategoryEntities categoryEntities4 =
            parameterLoaderService.loadCategoryEntitiesByHid(categories.get(4).getHid());

        List<OptionImpl> options4 = getOptionsForTargetParam(globalParameter, categoryEntities4);
        assertThat(options4.size()).isEqualTo(1L);
        OptionImpl newOption4 = options4.get(0);
        assertThat(newOption4.getParentOptionId()).isEqualTo(override3.getId());

        // проверить что оверрайде в категории 2 не обновился родитель
        categoryEntities2 =
            parameterLoaderService.loadCategoryEntitiesByHid(categories.get(2).getHid());

        options2 = getOptionsForTargetParam(globalParameter, categoryEntities2);
        assertThat(options2.size()).isEqualTo(1L);
        newOption2 = options2.get(0);
        assertThat(newOption2.getParentOptionId()).isEqualTo(override1.getId());
    }

    @Test
    public void testOptionsReused() {
        TovarCategory category = createCategories();

        CategoryParam globalParameter = createAndOverrideGlobalParameter(category);

        CategoryEntities categoryEntities = parameterLoaderService.loadCategoryEntitiesByHid(category.getHid());

        assertThat(categoryEntities.getParameters()).hasSize(1);
        List<Option> oldOptions = categoryEntities.getParameterById(globalParameter.getId()).getOptions();
        assertThat(oldOptions).hasSize(0);  // Из глобального параметра не наследуем енумы

        OptionImpl newOption = random.nextObject(OptionImpl.class, "parent", "categoryId");
        newOption.setParamId(globalParameter.getId());
        parameterDAO.insertEnumOption(globalParameter, newOption);
        parameterDAO.touchParam(globalParameter);
        parameterDAO.touchByGlobalParamId(globalParameter.getId());

        CategoryEntities newEntities = parameterLoaderService.loadCategoryEntitiesByHid(category.getHid());
        assertThat(newEntities.getParameters()).hasSize(1);
        List<Option> newOptions = newEntities.getParameterById(globalParameter.getId()).getOptions();
        assertThat(newOptions).hasSize(0);  // Из глобального параметра не наследуем енумы

        List<SameObjectEqualsWrapper<Option>> sortedOldOptions = oldOptions.stream()
            .sorted(Comparator.comparing(Option::getId))
            .map(SameObjectEqualsWrapper::new)
            .collect(Collectors.toList());
        List<SameObjectEqualsWrapper<Option>> sortedNewOptions = newOptions.stream()
            .sorted(Comparator.comparing(Option::getId))
            .map(SameObjectEqualsWrapper::new)
            .collect(Collectors.toList());
        assertThat(sortedNewOptions).containsAll(sortedOldOptions);
    }

    @Test
    public void testChangedOptionsReloaded() {
        TovarCategory category = createCategories();

        CategoryParam globalParameter = createAndOverrideGlobalParameter(category);

        CategoryEntities categoryEntities = parameterLoaderService.loadCategoryEntitiesByHid(category.getHid());

        assertThat(categoryEntities.getParameters()).hasSize(1);
        assertThat(categoryEntities.getParameters().get(0).getId()).isEqualTo(globalParameter.getId());
        List<Option> oldOptions = categoryEntities.getParameterById(globalParameter.getId()).getOptions();
        assertThat(oldOptions.isEmpty());   // Опции из глобального параметра енум не наследуются

        categoryEntities = parameterLoaderService.loadCategoryEntitiesByHid(0);

        assertThat(categoryEntities.getParameters()).hasSize(1);
        assertThat(categoryEntities.getParameters().get(0).getId()).isEqualTo(globalParameter.getId());
        oldOptions = categoryEntities.getParameterById(globalParameter.getId()).getOptions();

        // Меняем опцию глобального параметра
        Option optionToModify = oldOptions.get(0);
        optionToModify.setPublished(!optionToModify.isPublished());
        parameterDAO.updateEnumOption(optionToModify);
        parameterDAO.touchParam(globalParameter);
        parameterDAO.touchByGlobalParamId(globalParameter.getId());

        // Проверяем, что в локальный ничего не пронаследовалось
        CategoryEntities newEntities = parameterLoaderService.loadCategoryEntitiesByHid(category.getHid());
        assertThat(newEntities.getParameters()).hasSize(1);
        List<Option> newOptions = newEntities.getParameterById(globalParameter.getId()).getOptions();
        assertThat(newOptions).hasSize(0);

        // Проверяем, что в глобальном количество опций не поменялось
        newEntities = parameterLoaderService.loadCategoryEntitiesByHid(0);
        assertThat(newEntities.getParameters()).hasSize(1);
        newOptions = newEntities.getParameterById(globalParameter.getId()).getOptions();
        assertThat(newOptions).hasSize(globalParameter.getOptions().size());

        Option modifiedOption = newOptions.stream()
            .filter(o -> o.getId() == optionToModify.getId())
            .findFirst().orElse(null);
        assertThat(modifiedOption.fullyEquals(optionToModify)).isTrue();

        List<SameObjectEqualsWrapper<Option>> sortedAndFilteredOldOptions = oldOptions.stream()
            .filter(o -> o.getId() != optionToModify.getId())
            .sorted(Comparator.comparing(Option::getId))
            .map(SameObjectEqualsWrapper::new)
            .collect(Collectors.toList());
        List<SameObjectEqualsWrapper<Option>> sortedNewOptions = newOptions.stream()
            .sorted(Comparator.comparing(Option::getId))
            .map(SameObjectEqualsWrapper::new)
            .collect(Collectors.toList());
        assertThat(sortedNewOptions).containsAll(sortedAndFilteredOldOptions);
    }

    private TovarCategory createCategories() {
        TovarCategory rootCategory = random.nextObject(TovarCategory.class);
        rootCategory.setParentHid(-1);
        tovarTreeDao.createCategory(rootCategory, 1L);
        TovarCategory childCategory = random.nextObject(TovarCategory.class);
        childCategory.setParentHid(rootCategory.getHid());
        tovarTreeDao.createCategory(childCategory, 1L);
        return childCategory;
    }

    // --          cat1
    // -- cat2              cat 3
    // --              cat4       cat 5
    // --                     cat6  cat 7
    private List<TovarCategory> createCategoriesForChains() {
        List<TovarCategory> result = new ArrayList<>();
        result.add(null); // global category mock
        TovarCategory rootCategory = random.nextObject(TovarCategory.class);
        rootCategory.setParentHid(-1);
        rootCategory.setName("cat1");
        tovarTreeDao.createCategory(rootCategory, 1L);
        result.add(rootCategory);

        TovarCategory childCategory2 = random.nextObject(TovarCategory.class);
        childCategory2.setParentHid(rootCategory.getHid());
        childCategory2.setName("cat2");
        tovarTreeDao.createCategory(childCategory2, 1L);
        result.add(childCategory2);

        TovarCategory childCategory3 = random.nextObject(TovarCategory.class);
        childCategory3.setParentHid(rootCategory.getHid());
        childCategory3.setName("cat3");
        tovarTreeDao.createCategory(childCategory3, 1L);
        result.add(childCategory3);

        TovarCategory childCategory4 = random.nextObject(TovarCategory.class);
        childCategory4.setParentHid(childCategory3.getHid());
        childCategory4.setName("cat4");
        tovarTreeDao.createCategory(childCategory4, 1L);
        result.add(childCategory4);

        TovarCategory childCategory5 = random.nextObject(TovarCategory.class);
        childCategory5.setParentHid(childCategory3.getHid());
        childCategory5.setName("cat5");
        tovarTreeDao.createCategory(childCategory5, 1L);
        result.add(childCategory5);

        TovarCategory childCategory6 = random.nextObject(TovarCategory.class);
        childCategory6.setParentHid(childCategory5.getHid());
        childCategory6.setName("cat6");
        tovarTreeDao.createCategory(childCategory6, 1L);
        result.add(childCategory6);

        TovarCategory childCategory7 = random.nextObject(TovarCategory.class);
        childCategory7.setParentHid(childCategory5.getHid());
        childCategory7.setName("cat7");
        tovarTreeDao.createCategory(childCategory7, 1L);
        result.add(childCategory7);

        return result;
    }


    private List<CategoryParam> createAndOverrideGlobalParameter(
        List<TovarCategory> categories
    ) {
        List<CategoryParam> result = new ArrayList<>();
        Parameter globalParameter = random.nextObject(Parameter.class);
        // hack for now
        globalParameter.getOptions().forEach(o -> ((OptionImpl) o).setCategoryId(null));

        globalParameter.setCategoryHid(0);
        parameterDAO.insertParameter(globalParameter);
        globalParameter.getOptions().forEach(o -> parameterDAO.insertEnumOption(globalParameter, (OptionImpl) o));
        result.add(globalParameter);

        boolean globalOverrides = true;
        for (TovarCategory category : categories) {
            if (category == null) {
                continue;
            }
            InheritedParameter ip = getInheritedParameter(
                globalParameter,
                category,
                globalOverrides);
            result.add(ip);
            globalOverrides = false;
        }
        return result;
    }

    @NotNull
    private InheritedParameter getInheritedParameter(
        Parameter globalParameter,
        TovarCategory tovarCategory,
        boolean globalOverride) {
        InheritedParameter ip1 = new InheritedParameter(globalParameter);
        ParameterOverride override = new ParameterOverride();
        override.setGlobalOverride(globalOverride);
        ip1.addOverride(override);
        parameterDAO.createOverride(tovarCategory.getHid(), ip1);
        return ip1;
    }

    private CategoryParam createAndOverrideGlobalParameter(TovarCategory category) {
        Parameter globalParameter = random.nextObject(Parameter.class);
        // hack for now
        globalParameter.getOptions().forEach(o -> ((OptionImpl) o).setCategoryId(null));

        globalParameter.setCategoryHid(0);
        parameterDAO.insertParameter(globalParameter);
        globalParameter.getOptions().forEach(o -> parameterDAO.insertEnumOption(globalParameter, (OptionImpl) o));

        InheritedParameter ip = getInheritedParameter(
            globalParameter,
            category,
            true);
        return globalParameter;
    }

    @NotNull
    private List<OptionImpl> getOptionsForTargetParam(CategoryParam globalParameter,
                                                      CategoryEntities categoryEntities1) {
        return categoryEntities1
            .getParameterById(globalParameter.getId())
            .getEffectiveOptions()
            .stream()
            .filter(x -> x.getName() != null)
            .filter(x -> x.getName().contains(TARGET_OPTION_NAME))
            .map(x -> (OptionImpl) x)
            .collect(Collectors.toList());
    }

    private OptionImpl createOption(CategoryParam parameter,
                                    OptionImpl parent,
                                    String title) {
        OptionImpl newOption = random.nextObject(OptionImpl.class,
            "parent",
            "categoryId",
            "parentOptionId");
        newOption.setParamId(parameter.getId());
        newOption.setParent(parent);
        if (parent != null) {
            newOption.setParentOptionId(parent.getId());
        }
        newOption.setNames(Collections.singletonList(
            new Word(Word.DEFAULT_LANG_ID, title)));
        parameterDAO.insertEnumOption(parameter, newOption);
        parameterDAO.touchParam(parameter);
        parameterDAO.touchByGlobalParamId(parameter.getId());
        return newOption;
    }
}
