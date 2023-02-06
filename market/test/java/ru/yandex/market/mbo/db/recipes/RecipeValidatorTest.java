package ru.yandex.market.mbo.db.recipes;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.MultiValueMap;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.TovarTreeDaoMock;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.recipe.Recipe;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeErrorCause;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeFilter;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.mbo.db.recipes.ReportResultCheckerMock.TOTAL_OFFERS;

/**
 * @author padme
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class RecipeValidatorTest {

    private RecipeValidator recipeValidator;

    private static final CategoryEntities CATEGORY_ENTITY = new CategoryEntities(1, Collections.emptyList());

    private static final long RECIPE_ID = 1L;
    private static final long NONEXIST_HID = 5L;
    private static final long UNPUBLISHED_HID = 2L;
    private static final long EXIST_PARAM_ID = 10L;
    private static final long STRING_PARAM_ID = 11L;
    private static final long NUMBER_PARAM_ID = 13L;
    private static final long UNPUBLISHED_PARAM_ID = 12L;
    private static final long NOT_EXIST_PARAM_ID = 20L;
    private static final long EXIST_OPTION_ID = 100L;
    private static final long NOT_SHOW_IN_FILTER_OPTION_ID = 102L;
    private static final long UNPUBLISHED_OPTION_ID = 101L;
    private static final long NOT_EXIST_OPTION_ID = 200L;

    private static final String EXIST_PARAM_NAME = "exist";
    private static final String UNPUBLISHED_PARAM_NAME = "unpublished";
    private static final String STRING_PARAM_NAME = "string_name";
    private static final String NUMBER_PARAM_NAME = "number_name";

    @BeforeClass
    public static void init() {
        List<CategoryParam> params = new ArrayList<>();
        params.add(getValidEnumParam());
        params.add(getStringParam());
        params.add(getUnpublishedEnumParam());
        params.add(getNumberParam());
        CATEGORY_ENTITY.setParameters(params);
    }

    @Before
    public void setUp() throws Exception {
        recipeValidator = new RecipeValidator(
            new ParameterLoaderServiceStub(CATEGORY_ENTITY),
            new TovarTreeDaoMock(getPublishedCategory(),
                getUnpublishedTovarCategory()),
            new ReportResultCheckerMock());
    }

    public static TovarCategory getPublishedCategory() {
        return TovarCategoryBuilder.newBuilder()
                .setId(1)
                .setHid(1)
                .setParentHid(0)
                .setPublished(true)
                .setName("test category")
                .create();
    }

    private TovarCategory getUnpublishedTovarCategory() {
        return TovarCategoryBuilder.newBuilder()
                .setId(2)
                .setHid(2)
                .setParentHid(1)
                .setPublished(false)
                .setName("unpublished test category")
                .create();
    }

    private static CategoryParam getValidEnumParam() {
        CategoryParam param = new Parameter();
        param.setId(EXIST_PARAM_ID);
        param.setXslName(EXIST_PARAM_NAME);
        param.setType(Param.Type.ENUM);
        param.setCommonFilterIndex(1);
        param.addOption(new OptionImpl(EXIST_OPTION_ID));
        Option optionNotShowInFilter = new OptionImpl(NOT_SHOW_IN_FILTER_OPTION_ID);
        optionNotShowInFilter.setFilterValue(false);
        param.addOption(optionNotShowInFilter);
        return param;
    }

    private static CategoryParam getStringParam() {
        CategoryParam param = new Parameter();
        param.setId(STRING_PARAM_ID);
        param.setXslName(STRING_PARAM_NAME);
        param.setType(Param.Type.STRING);
        param.setCommonFilterIndex(1);
        param.addOption(new OptionImpl(EXIST_OPTION_ID));
        return param;
    }

    private static CategoryParam getNumberParam() {
        CategoryParam param = new Parameter();
        param.setId(NUMBER_PARAM_ID);
        param.setXslName(NUMBER_PARAM_NAME);
        param.setType(Param.Type.NUMERIC);
        param.setCommonFilterIndex(1);
        return param;
    }

    private static CategoryParam getUnpublishedEnumParam() {
        CategoryParam param = new Parameter();
        param.setId(UNPUBLISHED_PARAM_ID);
        param.setXslName(UNPUBLISHED_PARAM_NAME);
        param.setType(Param.Type.ENUM);
        param.setCommonFilterIndex(-1); //
        param.setAdvFilterIndex(-1); //
        param.addOption(new OptionImpl(UNPUBLISHED_OPTION_ID));
        return param;
    }

    @Test
    public void checkValidRecipe() throws Exception {
        Recipe recipe = getRecipe();
        MultiValueMap<Object, RecipeValidationResult> validationResults =
                recipeValidator.checkAndGetResults(Collections.singletonList(recipe));

        assertEquals(0, validationResults.size());
    }

    @Test
    public void checkNonExistHid() throws Exception {
        Recipe recipe = getRecipe();
        recipe.setHid(NONEXIST_HID);
        MultiValueMap<Object, RecipeValidationResult> validationResults =
                recipeValidator.checkAndGetResults(Collections.singletonList(recipe));

        assertEquals(1, validationResults.size());
        List<RecipeValidationResult> result = validationResults.get(recipe.getId());
        assertEquals(1, result.size());
        assertEquals(RecipeErrorCause.UNKNOWN_HID, result.get(0).getCause());
    }

    @Test
    public void checkNonPublishedHid() throws Exception {
        Recipe recipe = getRecipe();
        recipe.setHid(UNPUBLISHED_HID);
        recipe.setFilters(new ArrayList<>());
        MultiValueMap<Object, RecipeValidationResult> validationResults =
                recipeValidator.checkAndGetResults(Collections.singletonList(recipe));

        assertEquals(1, validationResults.size());
        List<RecipeValidationResult> result = validationResults.get(recipe.getId());
        assertEquals(1, result.size());
        assertEquals(RecipeErrorCause.UNPUBLISHED_CATEGORY, result.get(0).getCause());
    }

    @Test
    public void checkNoFilter() throws Exception {
        Recipe recipe = getRecipe();
        recipe.setFilters(new ArrayList<>());
        MultiValueMap<Object, RecipeValidationResult> validationResults =
                recipeValidator.checkAndGetResults(Collections.singletonList(recipe));

        assertEquals(1, validationResults.size());
        List<RecipeValidationResult> result = validationResults.get(recipe.getId());
        assertEquals(1, result.size());
        assertEquals(RecipeErrorCause.NO_FILTERS, result.get(0).getCause());

        recipe.setWithoutFilters(true);
        validationResults = recipeValidator.checkAndGetResults(Collections.singletonList(recipe));
        assertEquals(0, validationResults.size());
    }

    @Test
    public void checkParamAbsent() throws Exception {
        Recipe recipe = getRecipe();

        RecipeFilter filter = new RecipeFilter();
        filter.setParamId(NOT_EXIST_PARAM_ID);
        filter.setValueIds(Collections.singletonList(NOT_EXIST_OPTION_ID));
        recipe.setFilters(Collections.singletonList(filter));

        MultiValueMap<Object, RecipeValidationResult> validationResults =
                recipeValidator.checkAndGetResults(Collections.singletonList(recipe));

        assertEquals(1, validationResults.size());
        List<RecipeValidationResult> result = validationResults.get(recipe.getId());
        assertEquals(1, result.size());
        assertEquals(RecipeErrorCause.PARAM_ABSENT, result.get(0).getCause());
    }

    @Test
    public void checkParamNotAllowed() throws Exception {
        Recipe recipe = getRecipe();

        RecipeFilter filter = new RecipeFilter();
        filter.setParamId(STRING_PARAM_ID);
        filter.setValueIds(Collections.singletonList(NOT_EXIST_OPTION_ID));
        recipe.setFilters(Collections.singletonList(filter));

        MultiValueMap<Object, RecipeValidationResult> validationResults =
                recipeValidator.checkAndGetResults(Collections.singletonList(recipe));

        assertEquals(1, validationResults.size());
        List<RecipeValidationResult> result = validationResults.get(recipe.getId());
        assertEquals(1, result.size());
        assertEquals(RecipeErrorCause.PARAM_NOT_ALLOWED, result.get(0).getCause());
    }

    @Test
    public void checkCannotInitialize() throws Exception {
        Recipe recipe = getRecipe();

        RecipeFilter filter = new RecipeFilter();
        filter.setParamId(EXIST_PARAM_ID);
        recipe.setFilters(Collections.singletonList(filter));

        MultiValueMap<Object, RecipeValidationResult> validationResults =
                recipeValidator.checkAndGetResults(Collections.singletonList(recipe));

        assertEquals(1, validationResults.size());
        List<RecipeValidationResult> result = validationResults.get(recipe.getId());
        assertEquals(1, result.size());
        assertEquals(RecipeErrorCause.PARAM_CANNOT_INITIALIZE, result.get(0).getCause());
    }

    @Test
    public void checkCannotInitialize2() throws Exception {
        Recipe recipe = getRecipe();

        RecipeFilter filter = new RecipeFilter();
        filter.setParamId(NUMBER_PARAM_ID);
        filter.setMinValue(null);
        filter.setMaxValue(null);
        recipe.setFilters(Collections.singletonList(filter));

        MultiValueMap<Object, RecipeValidationResult> validationResults =
                recipeValidator.checkAndGetResults(Collections.singletonList(recipe));

        assertEquals(1, validationResults.size());
        List<RecipeValidationResult> result = validationResults.get(recipe.getId());
        assertEquals(1, result.size());
        assertEquals(RecipeErrorCause.PARAM_CANNOT_INITIALIZE, result.get(0).getCause());
    }

    @Test
    public void checkOptionIsNotFilterValue() throws Exception {
        Recipe recipe = getRecipe();

        RecipeFilter filter = new RecipeFilter();
        filter.setParamId(EXIST_PARAM_ID);
        filter.setValueIds(Collections.singletonList(NOT_SHOW_IN_FILTER_OPTION_ID));
        recipe.setFilters(Collections.singletonList(filter));

        MultiValueMap<Object, RecipeValidationResult> validationResults =
                recipeValidator.checkAndGetResults(Collections.singletonList(recipe));

        assertEquals(1, validationResults.size());
        List<RecipeValidationResult> result = validationResults.get(recipe.getId());
        assertEquals(1, result.size());
        assertEquals(RecipeErrorCause.OPTION_IS_NOT_FILTER_VALUE, result.get(0).getCause());
    }

    @Test
    public void checkNotInFilters() throws Exception {
        Recipe recipe = getRecipe();

        RecipeFilter filter = new RecipeFilter();
        filter.setParamId(UNPUBLISHED_PARAM_ID);
        filter.setValueIds(Collections.singletonList(UNPUBLISHED_OPTION_ID));
        recipe.setFilters(Collections.singletonList(filter));

        MultiValueMap<Object, RecipeValidationResult> validationResults =
                recipeValidator.checkAndGetResults(Collections.singletonList(recipe));

        assertEquals(1, validationResults.size());
        List<RecipeValidationResult> result = validationResults.get(recipe.getId());
        assertEquals(1, result.size());
        assertEquals(RecipeErrorCause.PARAM_NOT_IN_FILTERS, result.get(0).getCause());
    }

    @Test
    public void checkOptionsAbsent() throws Exception {
        Recipe recipe = getRecipe();

        RecipeFilter filter = new RecipeFilter();
        filter.setParamId(EXIST_PARAM_ID);
        filter.setValueIds(Collections.singletonList(NOT_EXIST_OPTION_ID));
        recipe.setFilters(Collections.singletonList(filter));

        MultiValueMap<Object, RecipeValidationResult> validationResults =
                recipeValidator.checkAndGetResults(Collections.singletonList(recipe));

        assertEquals(1, validationResults.size());
        List<RecipeValidationResult> result = validationResults.get(recipe.getId());
        assertEquals(1, result.size());
        assertEquals(RecipeErrorCause.PARAM_OPTION_ABSENT, result.get(0).getCause());
    }

    @Test
    public void checkBadReportResult() throws Exception {
        Recipe recipe = getRecipe();
        MultiValueMap<Object, RecipeValidationResult> validationResults =
                recipeValidator.checkAndGetResults(Collections.singletonList(recipe), true);
        assertEquals(1, validationResults.size());
        List<RecipeValidationResult> result = validationResults.get(recipe.getId());
        assertEquals(1, result.size());
        assertEquals(0, recipe.getTotalModelsMsk().longValue());
        assertEquals(0, recipe.getTotalOffersMsk().longValue());
        assertEquals(RecipeErrorCause.NO_REPORT_RESULT, result.get(0).getCause());
    }

    @Test
    public void checkGoodReportResult() throws Exception {
        Recipe recipe = getRecipe();
        recipe.setId(2L);
        MultiValueMap<Object, RecipeValidationResult> validationResults =
                recipeValidator.checkAndGetResults(Collections.singletonList(recipe), true);
        assertEquals(2, recipe.getTotalModelsMsk().longValue());
        assertEquals(TOTAL_OFFERS, recipe.getTotalOffersMsk().longValue());
        assertEquals(0, validationResults.size());
    }

    public static Recipe getRecipe() {
        Recipe recipe = new Recipe();
        recipe.setHid(RecipeServiceDaoMock.HID);
        recipe.setId(RECIPE_ID);
        RecipeFilter filter = new RecipeFilter();
        filter.setParamId(EXIST_PARAM_ID);
        filter.setParamType(Param.Type.ENUM);
        filter.setValueIds(Collections.singletonList(EXIST_OPTION_ID));
        recipe.setFilters(Collections.singletonList(filter));
        return recipe;
    }
}
