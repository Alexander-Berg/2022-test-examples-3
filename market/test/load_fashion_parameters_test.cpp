#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <market/report/library/global/fashion_parameters/fashion_parameters.h>


TEST(TestFashionParameters, LoadJsonFiles) {

    const auto pathCategories = SRC_("./../../svn-data/package-data/fashion_categories.json");
    const auto pathFirstPermium = SRC_("./../../svn-data/package-data/1p_fashion_premium.json");
    const auto pathThirdPermium = SRC_("./../../svn-data/package-data/3p_fashion_premium.json");

    EXPECT_NO_THROW(NMarketReport::NGlobal::NFashionParameters::LoadFashionParameters(pathCategories, pathFirstPermium, pathThirdPermium));
}

TEST(TestFashionParameters, LoadJsonBadFiles) {

    const auto pathCategories = SRC_("./../../svn-data/package-data/bad_file_name_1.json");
    const auto pathFirstPermium = SRC_("./../../svn-data/package-data/bad_file_name_2.json");
    const auto pathThirdPermium = SRC_("./../../svn-data/package-data/bad_file_name_3.json");

    EXPECT_NO_THROW(NMarketReport::NGlobal::NFashionParameters::LoadFashionParameters(pathCategories, pathFirstPermium, pathThirdPermium));
}

using namespace NMarket::NFashionParameters;

TEST(TestFashionParameters, TestCurretnCategoriesLoading) {

    TFashionCategories categories;

    const auto path = SRC_("./../../svn-data/package-data/fashion_categories.json");

    ASSERT_NO_THROW(LoadFashionCategories(path, categories));
}

TEST(TestFashionParameters, TestCurrentFirstPartyPremiumBrands) {

    TFashionPremiumBrands brands;

    const auto path = SRC_("./../../svn-data/package-data/1p_fashion_premium.json");

    ASSERT_NO_THROW(LoadPremiumBrands(path, brands));
}

TEST(TestFashionParameters, TestCurrentThirdPartyPremiumBrands) {

    TFashionPremiumBrands brands;

    const auto path = SRC_("./../../svn-data/package-data/3p_fashion_premium.json");

    ASSERT_NO_THROW(LoadPremiumBrands(path, brands));
}