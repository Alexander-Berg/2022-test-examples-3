package ru.yandex.market.mbo.recipe;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.recipe.Recipe;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeFilter;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeGoodState;
import ru.yandex.market.mbo.recipe.csvimport.RecipeJsonConverter;

@SuppressWarnings("checkstyle:MagicNumber")
public class RecipeJsonConverterTest {

    @Test
    public void testEmptyRecipe() {
        Recipe recipe = new Recipe();

        JSONObject object = RecipeJsonConverter.recipeToJson(recipe);
        Assert.assertEquals(
            "{\"hid\":0,\"is_seo\":false,\"popularity\":0,\"market_button\":false," +
                "\"discount\":false,\"without_filters\":false,\"id\":0,\"filters\":\"\",\"aprice\":false," +
                "\"discount_and_promo\":false}",
            object.toString());
    }

    @Test
    public void testSimpleRecipe() {
        Recipe recipe = new Recipe();
        recipe.setId(1L);
        recipe.setHid(10L);
        recipe.setHeader("header");
        recipe.setName("name");

        JSONObject object = RecipeJsonConverter.recipeToJson(recipe);
        Assert.assertEquals(
            "{\"hid\":10,\"is_seo\":false,\"popularity\":0,\"name\":\"name\",\"market_button\":false," +
                "\"header\":\"header\",\"discount\":false,\"without_filters\":false,\"id\":1,\"filters\":\"\"," +
                "\"aprice\":false,\"discount_and_promo\":false}",
            object.toString());
    }

    @Test
    public void testWholeRecipe() {
        //в этом тесте находятся все поля, которые есть в заголовке файла загрузки рецептов
        Recipe recipe = new Recipe();
        recipe.setId(1L);
        recipe.setPopularity(111);
        recipe.setHid(10L);
        recipe.setHeader("header");
        recipe.setName("name");
        recipe.setCorrect(true);
        recipe.setPublished(true);
        recipe.setSeo(true);
        recipe.setMarketButton(true);
        recipe.setButtonIndex(11L);
        recipe.setButtonName("button name");
        recipe.setDiscount(false);
        recipe.setDiscountAndPromo(true);
        recipe.setAprice(true);
        recipe.setWithoutFilters(false);
        recipe.setGoodState(RecipeGoodState.CUTPRICE);
        recipe.setSearchQuery("some text");

        RecipeFilter filter = new RecipeFilter();
        filter.setParamType(Param.Type.BOOLEAN);
        filter.setParamId(2);
        filter.setBooleanValue(true);

        recipe.addFilter(filter);

        JSONObject object = RecipeJsonConverter.recipeToJson(recipe);
        Assert.assertEquals(
            "{\"hid\":10,\"is_seo\":true,\"button_name\":\"button name\",\"market_button\":true," +
                "\"discount\":false,\"filters\":\"2:BOOLEAN:1\",\"search_query\":\"some text\",\"aprice\":true," +
                "\"discount_and_promo\":true,\"popularity\":111,\"name\":\"name\",\"header\":\"header\"," +
                "\"without_filters\":false,\"id\":1,\"button_index\":11,\"good_state\":\"cutprice\"}",
            object.toString());
    }
}
