package ru.yandex.market.mbo.gwt.utils;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.recipe.Recipe;

import static org.junit.Assert.assertEquals;

public class RecipeUrlBuilderTest {
    @Test
    public void testSearchRecipesUrl() {
        Recipe recipe = new Recipe();
        recipe.setHid(1);
        recipe.setWithoutFilters(true);
        recipe.setSearchQuery("iphone");

        RecipeUrlBuilder builder = new RecipeUrlBuilder();
        assertEquals("https://market.yandex.ru/search.xml?hid=1&text=iphone", builder.build(recipe));
    }

    @Test
    public void testRecipeWithNoSearchQueryUrl() {
        Recipe recipe = new Recipe();
        recipe.setHid(1);
        recipe.setWithoutFilters(true);

        RecipeUrlBuilder builder = new RecipeUrlBuilder();
        assertEquals("https://market.yandex.ru/search.xml?hid=1", builder.build(recipe));
    }

    @Test
    public void testRecipeWithEmptySearchQueryUrl() {
        Recipe recipe = new Recipe();
        recipe.setHid(1);
        recipe.setWithoutFilters(true);
        recipe.setSearchQuery("");

        RecipeUrlBuilder builder = new RecipeUrlBuilder();
        assertEquals("https://market.yandex.ru/search.xml?hid=1", builder.build(recipe));
    }

    @Test
    public void testRecipeWithFilters() {
        // Если у рецепта не указано, что он без фильтров, поисковая строка должна игнорироваться
        Recipe recipe = new Recipe();
        recipe.setHid(1);
        recipe.setWithoutFilters(false);
        recipe.setSearchQuery("samsung");

        RecipeUrlBuilder builder = new RecipeUrlBuilder();
        assertEquals("https://market.yandex.ru/search.xml?hid=1", builder.build(recipe));
    }
}
