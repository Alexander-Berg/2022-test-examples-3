#include <library/cpp/testing/unittest/gtest.h>
#include <market/tools/recipes_xml_to_mmap_converter/ut/util.h>

using namespace NMarket::NRecipes;

TEST(PARSER, PARSED_RECIPES_COUNT) {
    GuruLightMBO2::TParams mboParams;
    THolder<NGlMbo::IReader> glMboReader;

    auto parser = InitParser(mboParams, glMboReader, "data/MixedGLCategories.json");
    auto recipes = parser.ParseRecipes(SRC_("data/recipes.xml"));
    ASSERT_EQ(recipes.size(), 2);
    ASSERT_EQ(parser.GetInvalidRecipesCount(), 1);
}

TEST(PARSER, SEARCH_RECIPE_PARSING) {
    GuruLightMBO2::TParams mboParams;
    THolder<NGlMbo::IReader> glMboReader;

    auto parser = InitParser(mboParams, glMboReader, "data/MixedGLCategories.json");
    auto recipes = parser.ParseRecipes(SRC_("data/search_recipe.xml"));

    ASSERT_EQ(recipes.size(), 1);
    const auto& recipe = recipes.at(0);

    ASSERT_EQ(recipe.RecipeId, 3);
    ASSERT_EQ(recipe.SearchQuery, "Поисковый запрос");
    ASSERT_EQ(recipe.ContainsDiscountOrPromo, false);
    ASSERT_EQ(recipe.ContainsDiscount, false);
    ASSERT_EQ(recipe.ButtonName, "Button");
    ASSERT_EQ(recipe.ButtonIndex, 10);
    ASSERT_EQ(recipe.IsButton, true);
    ASSERT_EQ(recipe.IsSeo, false);
    ASSERT_EQ(recipe.ContainsReviews, false);
    ASSERT_EQ(recipe.Sponsored, true);
    ASSERT_EQ(recipe.Popularity, 3);
    ASSERT_EQ(recipe.CategoryId, 7811944);
    ASSERT_EQ(recipe.Header, "Валидный рецепт без фильтров");
    ASSERT_EQ(recipe.Name, "Валидный без фильтров");
    ASSERT_EQ(recipe.Sorting, "best_by_factor:1234");
    ASSERT_EQ(recipe.GoodState, "new");
    ASSERT_TRUE(recipe.Filters.empty());
}

TEST(PARSER, SEARCH_RECIPE_WITH_FILTER_PARSING) {
    GuruLightMBO2::TParams mboParams;
    THolder<NGlMbo::IReader> glMboReader;

    auto parser = InitParser(mboParams, glMboReader, "data/MixedGLCategories.json");
    auto recipes = parser.ParseRecipes(SRC_("data/search_recipe_with_filters.xml"));

    ASSERT_EQ(recipes.size(), 1);
    const auto& recipe = recipes.at(0);

    ASSERT_EQ(recipe.RecipeId, 4);
    ASSERT_EQ(recipe.SearchQuery, "Поисковый запрос");
    ASSERT_EQ(recipe.ContainsDiscountOrPromo, false);
    ASSERT_EQ(recipe.ContainsDiscount, false);
    ASSERT_EQ(recipe.ButtonName, "Button");
    ASSERT_EQ(recipe.ButtonIndex, 10);
    ASSERT_EQ(recipe.IsButton, true);
    ASSERT_EQ(recipe.IsSeo, false);
    ASSERT_EQ(recipe.ContainsReviews, false);
    ASSERT_EQ(recipe.Sponsored, true);
    ASSERT_EQ(recipe.Popularity, 3);
    ASSERT_EQ(recipe.CategoryId, 7811944);
    ASSERT_EQ(recipe.Header, "Валидный с фильтрами");
    ASSERT_EQ(recipe.Name, "Валидный с фильтрами");
    ASSERT_EQ(recipe.Filters.size(), 1);
    ASSERT_EQ(recipe.GoodState, "cutprice");
    ASSERT_EQ(recipe.Sorting, "");
}

TEST(PARSER, RECIPE_PARSING) {
    GuruLightMBO2::TParams mboParams;
    THolder<NGlMbo::IReader> glMboReader;

    auto parser = InitParser(mboParams, glMboReader, "data/MixedGLCategories.json");
    auto recipes = parser.ParseRecipes(SRC_("data/recipe.xml"));

    ASSERT_EQ(recipes.size(), 1);
    const auto& recipe = recipes.at(0);

    ASSERT_EQ(recipe.RecipeId, 1);
    ASSERT_EQ(recipe.SearchQuery, "");
    ASSERT_EQ(recipe.ContainsDiscountOrPromo, false);
    ASSERT_EQ(recipe.ContainsDiscount, false);
    ASSERT_EQ(recipe.ButtonName, "");
    ASSERT_EQ(recipe.ButtonIndex, 0);
    ASSERT_EQ(recipe.IsButton, false);
    ASSERT_EQ(recipe.IsSeo, true);
    ASSERT_EQ(recipe.ContainsReviews, false);
    ASSERT_EQ(recipe.Sponsored, false);
    ASSERT_EQ(recipe.Popularity, 1);
    ASSERT_EQ(recipe.CategoryId, 7811944);
    ASSERT_EQ(recipe.Header, "Валидный рецепт");
    ASSERT_EQ(recipe.Name, "Валидный");
    ASSERT_EQ(recipe.GoodState, "");
    ASSERT_EQ(recipe.Sorting, "");

    ASSERT_EQ(recipe.Filters.size(), 3);
}

TEST(PARSER, FILTERS) {
    GuruLightMBO2::TParams mboParams;
    THolder<NGlMbo::IReader> glMboReader;

    auto parser = InitParser(mboParams, glMboReader, "data/MixedGLCategories.json");
    auto recipes = parser.ParseRecipes(SRC_("data/filters.xml"));

    ASSERT_EQ(recipes.size(), 3);

    for (const auto& recipe : recipes) {
        const auto& filter = recipe.Filters.at(0);
        if (recipe.RecipeId == 1) {
            ASSERT_EQ("Values: [10977906]", filter->ValueAsString());
        }
        if (recipe.RecipeId == 2) {
            ASSERT_EQ("MinValue: 1900; MaxValue: 1900", filter->ValueAsString());
        }
        if (recipe.RecipeId == 3) {
            ASSERT_EQ("Value: 1", filter->ValueAsString());
        }
    }
}

TEST(PARSER, EMPTY_FILE) {
    GuruLightMBO2::TParams mboParams;
    THolder<NGlMbo::IReader> glMboReader;

    auto parser = InitParser(mboParams, glMboReader, "data/MixedGLCategories.json");
    auto recipes = parser.ParseRecipes(SRC_("data/empty_file.xml"));

    ASSERT_TRUE(parser.HasFatalErrors());
    ASSERT_TRUE(recipes.empty());
}

TEST(PARSER, EMPTY_RECIPES_TAG) {
    GuruLightMBO2::TParams mboParams;
    THolder<NGlMbo::IReader> glMboReader;

    auto parser = InitParser(mboParams, glMboReader, "data/MixedGLCategories.json");
    auto recipes = parser.ParseRecipes(SRC_("data/empty_recipes.xml"));

    ASSERT_TRUE(recipes.empty());
}

TEST(PARSER, NUMERIC_FILTERS) {
    GuruLightMBO2::TParams mboParams;
    THolder<NGlMbo::IReader> glMboReader;

    auto parser = InitParser(mboParams, glMboReader, "data/MixedGLCategories.json");
    auto recipes = parser.ParseRecipes(SRC_("data/numeric_filters.xml"));

    ASSERT_EQ(recipes.at(0).Filters.at(0)->ValueAsString(), "MinValue: 1900; MaxValue: (empty maybe)");
    ASSERT_EQ(recipes.at(0).Filters.at(1)->ValueAsString(), "MinValue: (empty maybe); MaxValue: 1900");
    ASSERT_EQ(recipes.at(0).Filters.at(2)->ValueAsString(), "MinValue: 1900; MaxValue: 1900");
}

TEST(PARSER, ENUM_FILTERS) {
    GuruLightMBO2::TParams mboParams;
    THolder<NGlMbo::IReader> glMboReader;

    auto parser = InitParser(mboParams, glMboReader, "data/MixedGLCategories.json");
    auto recipes = parser.ParseRecipes(SRC_("data/enum_filters.xml"));

    ASSERT_EQ(recipes.at(0).Filters.at(0)->ValueAsString(), "Values: [10977906, 10977907]");
}

TEST(PARSER, DISCOUNT) {
    GuruLightMBO2::TParams mboParams;
    THolder<NGlMbo::IReader> glMboReader;

    auto parser = InitParser(mboParams, glMboReader, "data/MixedGLCategories.json");
    auto recipes = parser.ParseRecipes(SRC_("data/discount.xml"));

    ASSERT_TRUE(recipes.at(0).ContainsDiscount);
    ASSERT_TRUE(recipes.at(0).Filters.empty());
}
