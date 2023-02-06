package ru.yandex.market.mbo.db.recipes;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.gwt.models.recipe.Recipe;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeFilter;
import ru.yandex.market.mbo.recipe.url.RecipeFromUrlBuilder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.mbo.db.recipes.RecipeValidatorTest.getRecipe;

/**
 * @author padme
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class RecipeFromUrlBuilderTest {

    private RecipeFromUrlBuilder recipeFromUrlBuilder;

    @Before
    public void setUp() throws Exception {
        recipeFromUrlBuilder = new RecipeFromUrlBuilder(new RecipeFromUrlHelperMock());
    }

    @Test
    public void checkValidRecipe() throws Exception {
        Recipe recipe = getRecipe();
        Recipe builtRecipe =
                recipeFromUrlBuilder.build("https://market.yandex.ru/search.xml?glfilter=10:100&hid=1");

        compareRecipes(recipe, builtRecipe);
    }

    @Test
    public void checkUrWithoutHid() throws Exception {
        /*
         * recipeFromUrlBuilder should throw exception
         * if it not happen then builder includes an error
         */
        try {
            recipeFromUrlBuilder.build("https://market.yandex.ru/search.xml?gfilter=10:100");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Not found parameter 'hid' in url");
            return;
        }
        assertEquals(true, false);
    }

    @Test
    public void checkEnumUrl() throws Exception {
        //There is a enum multi value checker
        Recipe recipe = getRecipe();

        RecipeFilter filter = new RecipeFilter();
        filter.setParamId(RecipeServiceDaoMock.EXIST_PARAM_ID);
        filter.setValueIds(Arrays.asList(RecipeServiceDaoMock.EXIST_OPTION_ID1,
                RecipeServiceDaoMock.EXIST_OPTION_ID2,
                RecipeServiceDaoMock.EXIST_OPTION_ID3));
        recipe.setFilters(Collections.singletonList(filter));

        Recipe builtRecipe =
                recipeFromUrlBuilder.build("https://market.yandex.ru/search.xml?glfilter=10:100,200,300&hid=1");

        compareRecipes(recipe, builtRecipe);
    }

    @Test
    public void checkBooleanUrl() throws Exception {
        //There is a boolean value checker
        Recipe recipe = getRecipe();

        RecipeFilter filter = new RecipeFilter();
        filter.setParamId(RecipeServiceDaoMock.BOOLEAN_PARAM_ID);
        filter.setBooleanValue(true);
        recipe.setFilters(Collections.singletonList(filter));

        Recipe builtRecipe =
                recipeFromUrlBuilder.build("https://market.yandex.ru/search.xml?glfilter=20:1&hid=1");

        compareRecipes(recipe, builtRecipe);
    }

    @Test
    public void checkNumericUrl() throws Exception {
        //There is a boolean value checker
        Recipe recipe = getRecipe();

        RecipeFilter filter = new RecipeFilter();
        filter.setParamId(RecipeServiceDaoMock.NUMERIC_PARAM_ID);
        filter.setMinValue(new BigDecimal(10));
        filter.setMaxValue(new BigDecimal(20));
        recipe.setFilters(Collections.singletonList(filter));

        Recipe builtRecipe =
                recipeFromUrlBuilder.build("https://market.yandex.ru/search.xml?glfilter=30:10,20&hid=1");

        compareRecipes(recipe, builtRecipe);
    }

    private void compareRecipes(Recipe recipe, Recipe builtRecipe) {
        assertEquals(builtRecipe.getFilters().size(), recipe.getFilters().size());

        List<RecipeFilter> filters = recipe.getFilters();
        Collections.sort(filters, (RecipeFilter r1, RecipeFilter r2) -> r1.getParamId().compareTo(r2.getParamId()));
        List<RecipeFilter> builtRecipeFilters = builtRecipe.getFilters();
        Collections.sort(builtRecipeFilters, (RecipeFilter r1, RecipeFilter r2) ->
                                                r1.getParamId().compareTo(r2.getParamId()));

        for (int index = 0; index < filters.size(); index++) {
            RecipeFilter filter1 = filters.get(index);
            RecipeFilter filter2 = builtRecipeFilters.get(index);
            assertEquals(filter1.getParamId(), filter2.getParamId());
            assertEquals(filter1.getBooleanValue(), filter2.getBooleanValue());
            assertEquals(filter1.getMinValue(), filter2.getMinValue());
            assertEquals(filter1.getMinValue(), filter2.getMinValue());
            Set<Long> values1 = filter1.getValueIds();
            Set<Long> values2 = filter2.getValueIds();
            assertEquals(values1.size(), values2.size());
            assertEquals(values1.containsAll(values2), true);
            assertEquals(values2.containsAll(values1), true);
        }
    }

}
