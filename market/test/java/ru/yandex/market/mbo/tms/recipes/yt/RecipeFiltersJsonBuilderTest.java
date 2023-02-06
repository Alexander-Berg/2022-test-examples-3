package ru.yandex.market.mbo.tms.recipes.yt;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.recipe.Recipe;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeFilter;
import ru.yandex.market.mbo.gwt.models.visual.Word;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.Silent.class)
public class RecipeFiltersJsonBuilderTest {

    RecipeFiltersJsonBuilder recipeFiltersJsonBuilder;

    private static final long HID = 1;

    private static final long ENUM_PARAM_ID = 10;
    private static final long BOOL_PARAM_ID = 11;
    private static final long OPTION_ID = 100;

    private static final long RECIPE_ID = 1000;

    private static final long TOTAL_OFFERS = 321;
    private static final long TOTAL_MODELS = 5;

    @Before
    public void prepare() throws InterruptedException {
        recipeFiltersJsonBuilder = new RecipeFiltersJsonBuilder(
            new ParameterLoaderServiceStub(generateCategoryEntities()));
    }

    @Test
    public void testJsonBuilder() {
        Recipe recipe = generateRecipe();
        String json = recipeFiltersJsonBuilder.buildFilterJson(recipe);
        Assert.assertEquals(json, "[{\"values\":[{\"name\":[\"option name\"],\"id\":[100]}]," +
                                           "\"name\":[\"enum param name\"],\"id\":[10]}," +
                                          "{\"name\":[\"bool param name\"],\"id\":[11]}]");

        recipe.setFilters(new ArrayList<>());
        recipe.addFilter(generateEnumFilter());
        json = recipeFiltersJsonBuilder.buildFilterJson(recipe);
        Assert.assertEquals(json, "[{\"values\":[{\"name\":[\"option name\"],\"id\":[100]}]," +
                                                        "\"name\":[\"enum param name\"],\"id\":[10]}]");
    }

    private Recipe generateRecipe() {
        Recipe recipe = new Recipe();
        recipe.setHid(HID);
        recipe.setId(RECIPE_ID);
        recipe.setName("Name "  + RECIPE_ID);
        recipe.setHeader("HEADER "  + RECIPE_ID);
        recipe.setTotalOffersMsk(TOTAL_OFFERS);
        recipe.setTotalOffersMsk(TOTAL_MODELS);
        recipe.addFilter(generateEnumFilter());
        recipe.addFilter(generateBoolFilter());

        return recipe;
    }

    private RecipeFilter generateEnumFilter() {
        RecipeFilter enumFilter = new RecipeFilter();
        enumFilter.setParamId(ENUM_PARAM_ID);
        enumFilter.setParamType(Param.Type.ENUM);
        enumFilter.addValueId(OPTION_ID);
        return enumFilter;
    }

    private RecipeFilter generateBoolFilter() {
        RecipeFilter boolFilter = new RecipeFilter();
        boolFilter.setParamId(BOOL_PARAM_ID);
        boolFilter.setParamType(Param.Type.BOOLEAN);
        boolFilter.setBooleanValue(true);
        return boolFilter;
    }

    private CategoryEntities generateCategoryEntities() {
        CategoryEntities entities = new CategoryEntities(HID, Collections.emptyList());
        List<CategoryParam> params = new ArrayList<>();

        CategoryParam enumParam = new Parameter();
        enumParam.setHyperId(HID);
        enumParam.setId(ENUM_PARAM_ID);
        enumParam.setType(Param.Type.ENUM);
        enumParam.addName(new Word(Word.DEFAULT_LANG_ID, "enum param name"));
        enumParam.addOption(new OptionImpl(OPTION_ID, "option name"));
        params.add(enumParam);

        CategoryParam boolParam = new Parameter();
        boolParam.setHyperId(HID);
        boolParam.setId(BOOL_PARAM_ID);
        boolParam.setType(Param.Type.BOOLEAN);
        boolParam.addName(new Word(Word.DEFAULT_LANG_ID, "bool param name"));
        params.add(boolParam);

        entities.setParameters(params);
        return entities;
    }
}
