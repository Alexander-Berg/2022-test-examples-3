#include "processor_test_runner.h"
#include "test_utils.h"

#include <market/idx/feeds/qparser/lib/parser_context.h>
#include <market/idx/feeds/qparser/lib/price_calculator.h>

#include <market/library/offers_common/Geo.h>

#include <library/cpp/testing/common/env.h>

using namespace NMarket;

class TestPriceCalculator : public ::testing::Test {
public:
public:
    NMarket::TFeedInfo FeedInfo;
    NMarket::TFeedShopInfo FeedShopInfo;

    TPriceCalculator& GetPriceCalculator() {
        return FeedShopInfo.PriceCalculator;
    }
private:
    void SetUp() override {
        ::testing::Test::SetUp();
        FeedInfo = GetDefaultWhiteFeedInfo(EFeedType::YML);

    }
};

TEST(PriceCalculator, SetUpDefaultCbrfAndRur) {
    NMarket::TFeedInfo FeedInfo = GetDefaultWhiteFeedInfo(EFeedType::YML);
    NMarket::TFeedShopInfo FeedShopInfo;
    TParserContext ctx(&FeedInfo, &FeedShopInfo, nullptr);

    TPriceCalculator& priceCalculator = FeedShopInfo.PriceCalculator;
    priceCalculator.SetUpDefaultCbAndRefCurrency(&ctx);

    TString bank = priceCalculator.GetBank();
    bool hasCurrencyRur = priceCalculator.HasCurrency("RUR");

    ASSERT_EQ(bank, "CBRF");
    ASSERT_TRUE(hasCurrencyRur);
}

TEST(PriceCalculator, HomeRegionReferenceCurrency) {
    NMarket::TFeedInfo FeedInfo = GetDefaultWhiteFeedInfo(EFeedType::YML);
    FeedInfo.HomeRegion = EGeoRegions::REGION_BELORUS;
    NMarket::TFeedShopInfo FeedShopInfo;
    TParserContext ctx(&FeedInfo, &FeedShopInfo, nullptr);

    TPriceCalculator& priceCalculator = FeedShopInfo.PriceCalculator;
    priceCalculator.SetUpDefaultCbAndRefCurrency(&ctx);

    TString bank = priceCalculator.GetBank();
    bool hasCurrencyByn = priceCalculator.HasCurrency("BYN");

    ASSERT_TRUE(hasCurrencyByn);
    ASSERT_EQ(bank, "NBRB");
}

TEST_F(TestPriceCalculator, AddCurrencyUnknown) {
    TParserContext ctx(&FeedInfo, &FeedShopInfo, nullptr);
    TPriceCalculator& priceCalculator = GetPriceCalculator();

    // should not throw
    priceCalculator.AddCurrency(TCurrencyRecord{"UNK", "1"}, &ctx);

    bool hasCurrencyUnk = priceCalculator.HasCurrency("UNK");

    ASSERT_FALSE(hasCurrencyUnk);
}

TEST_F(TestPriceCalculator, AddCurrenciesNon1Rate) {
    TParserContext ctx(&FeedInfo, &FeedShopInfo, nullptr);
    TPriceCalculator& priceCalculator = GetPriceCalculator();

    // floating rates - log warning, do not throw
    priceCalculator.AddCurrency(TCurrencyRecord{"EUR", "1.2"}, &ctx);
    priceCalculator.AddCurrency(TCurrencyRecord{"USD", "1.67"}, &ctx);
    priceCalculator.AddCurrency(TCurrencyRecord{"KZT", "1"}, &ctx);

    ASSERT_TRUE(priceCalculator.HasCurrency("EUR"));
    ASSERT_TRUE(priceCalculator.HasCurrency("USD"));
    ASSERT_TRUE(priceCalculator.HasCurrency("KZT"));

    // Such rate is considered as invalid
    ASSERT_TRUE(priceCalculator.GetCurrency("EUR").InvalidRate);
    ASSERT_TRUE(priceCalculator.GetCurrency("USD").InvalidRate);
    ASSERT_FALSE(priceCalculator.GetCurrency("KZT").InvalidRate);
}

TEST_F(TestPriceCalculator, AddCurrenciesDuplicate) {
    TParserContext ctx(&FeedInfo, &FeedShopInfo, nullptr);
    TPriceCalculator& priceCalculator = GetPriceCalculator();

    // duplicate currency - log warning, do not throw
    priceCalculator.AddCurrency(TCurrencyRecord{"EUR", "1"}, &ctx);
    priceCalculator.AddCurrency(TCurrencyRecord{"USD", "1"}, &ctx);
    priceCalculator.AddCurrency(TCurrencyRecord{"USD", "1"}, &ctx);

    ASSERT_TRUE(priceCalculator.HasCurrency("EUR"));
    ASSERT_TRUE(priceCalculator.HasCurrency("USD"));
}

TEST(PriceCalculator, CurrencyBYRShouldBeInvalid) {
    //Old Belarusian ruble
    ASSERT_FALSE(NMarket::IsKnownCurrencyId("BYR"));

    //Belarusian ruble since 2016
    ASSERT_TRUE(NMarket::IsKnownCurrencyId("BYN"));
}

TEST_F(TestPriceCalculator, AddCurrencyBYRShouldThrow) {
    TParserContext ctx(&FeedInfo, &FeedShopInfo, nullptr);
    TPriceCalculator& priceCalculator = GetPriceCalculator();

    // BYR is not added to the currencies list
    priceCalculator.AddCurrency(TCurrencyRecord{"BYR", "1"}, &ctx);
    ASSERT_FALSE(priceCalculator.HasCurrency("BYR"));
}
