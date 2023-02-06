#include <market/idx/promos/promo-recipes-indexer/src/recipes_generator.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>


class TRecipesGeneratorTest
    : public NPromoRecipesIndexer::TRecipesGenerator
{
public:
    explicit TRecipesGeneratorTest(const NPromoRecipesIndexer::TParams& params)
        : NPromoRecipesIndexer::TRecipesGenerator(params)
    {
    }

private:
    void LoadRawPromoDiscountsData(
            const TString&,
            TSet<size_t>& shopWithPromoCodesIds,
            TSet<size_t>& shopWithPromoIds,
            TSet<size_t>& vendorWithPromoCodesIds,
            TSet<size_t>& vendorWithPromoIds,
            TSet<size_t>& categoryWithPromoCodesIds,
            TSet<size_t>& categoryWithPromoIds
    ) const override {
        shopWithPromoCodesIds = {1, 2, 206567, 4, 5};
        shopWithPromoIds = {1, 413298, 206567};
        vendorWithPromoCodesIds = {1, 2, 1006806};
        vendorWithPromoIds = {1, 2, 1006806, 488709};
        categoryWithPromoCodesIds = {3, 5, 2};
        categoryWithPromoIds = {3, 5, 2, 42};
    }

    TMap<size_t, TString> GetCategoryNames() const override {
        return {
            {1, "first"},
            {2, "second"},
            {42, "omg"},
        };
    }

};


TEST(Recipes, testGen) {
    NPromoRecipesIndexer::TParams params;
    params.ShopsDatPath = SRC_("data/shops.dat");
    params.GlobalVendorsPath = SRC_("data/global.vendors.xml");
    TRecipesGeneratorTest recipesGenerator(params);
    const auto protoData = recipesGenerator.GenerateAllRecipes();

    ASSERT_EQ(protoData.promo_code_recipes_size(), 1);
    ASSERT_EQ(protoData.promo_recipes_size(), 2);

    const auto shopRecipe = protoData.promo_code_recipes().Get(0);
    ASSERT_EQ(shopRecipe.shop_name(), "PNEUMOEXPERT");
    ASSERT_EQ(shopRecipe.link(), "/promo/promocodes_for_206567");

    ASSERT_EQ(protoData.vendor_promo_code_recipes_size(), 1);
    ASSERT_EQ(protoData.vendor_promo_recipes_size(), 2);

    const auto vendorRecipe = protoData.vendor_promo_code_recipes().Get(0);
    ASSERT_EQ(vendorRecipe.vendor_name(), "B.O.N.E.");
    ASSERT_EQ(vendorRecipe.link(), "/promo/vendor_promocodes_for_1006806");

    ASSERT_EQ(protoData.category_promo_code_recipes_size(), 1);
    ASSERT_EQ(protoData.category_promo_recipes_size(), 2);

    const auto catRecipe = protoData.category_promo_code_recipes().Get(0);
    ASSERT_EQ(catRecipe.category_name(), "second");
    ASSERT_EQ(catRecipe.link(), "/promo/category_promocodes_for_2");

}
