#include "config.h"

#include <market/dynamic_pricing/pricing/dynamic_pricing/autostrategy/price_bounds/default_price_bound.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <util/generic/maybe.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NPricingBounds::NTestConfig;

namespace {

const auto DefaultCalculator = NPricingBounds::CreateCalculator("default", CreatePricingConfig());

}



// High < Low -> High = Low (sanity check)
TEST(TestDefaultPriceBound, Test_LowAboveHigh)
{
    InitGlobalLog2Null();

    TShopSkuData ssku;
    ssku.LowPrice = 800;
    ssku.HighPrice = 500;
    ssku.BuyPrice = 600;          // not used here
    ssku.PurchasePrice = 650;
    ssku.CurrentPrice = 770;

    DefaultCalculator->Calculate(ssku);

    EXPECT_EQ(ssku.LowPrice, 800);
    EXPECT_EQ(ssku.HighPrice, 800);
}

// Low = (1 - maxDelta) * CurrentPrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestDefaultPriceBound, Test_BaseCase)
{
    InitGlobalLog2Null();

    TShopSkuData ssku;
    ssku.LowPrice = 500;
    ssku.HighPrice = 800;
    ssku.BuyPrice = 600;          // not used here
    ssku.PurchasePrice = 650;
    ssku.CurrentPrice = 700;

    DefaultCalculator->Calculate(ssku);

    EXPECT_EQ(ssku.LowPrice, 679);
    EXPECT_EQ(ssku.HighPrice, 721);
}

// Low = (1 - maxDelta) * CurrentPrice
// High = HighPrice
TEST(TestDefaultPriceBound, Test_HighLimit)
{
    InitGlobalLog2Null();

    TShopSkuData ssku;
    ssku.LowPrice = 500;
    ssku.HighPrice = 715;
    ssku.BuyPrice = 600;          // not used here
    ssku.PurchasePrice = 650;
    ssku.CurrentPrice = 700;

    DefaultCalculator->Calculate(ssku);

    EXPECT_EQ(ssku.LowPrice, 679);
    EXPECT_EQ(ssku.HighPrice, 715);
}

// Low =  LowPrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestDefaultPriceBound, Test_LowLimit)
{
    InitGlobalLog2Null();

    TShopSkuData ssku;
    ssku.LowPrice = 685;
    ssku.HighPrice = 750;
    ssku.BuyPrice = 600;          // not used here
    ssku.PurchasePrice = 690;
    ssku.CurrentPrice = 700;

    DefaultCalculator->Calculate(ssku);

    EXPECT_EQ(ssku.LowPrice, 685);
    EXPECT_EQ(ssku.HighPrice, 721);
}

// Low =  (1 - maxLowDelta) * PurchasePrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestDefaultPriceBound, Test_MaxLowDelta)
{
    InitGlobalLog2Null();

    TShopSkuData ssku;
    ssku.LowPrice = 300;
    ssku.HighPrice = 1000;
    ssku.BuyPrice = 600;          // not used here
    ssku.PurchasePrice = 1000;
    ssku.CurrentPrice = 700;

    DefaultCalculator->Calculate(ssku);

    EXPECT_EQ(ssku.LowPrice, 700);
    EXPECT_EQ(ssku.HighPrice, 721);
}

// Low =  (1 - maxLowDelta) * PurchasePrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestDefaultPriceBound, Test_MaxLowDeltaWithTooLowHighPrice)
{
    InitGlobalLog2Null();

    TShopSkuData ssku;
    ssku.LowPrice = 600;
    ssku.HighPrice = 650;
    ssku.BuyPrice = 600;          // not used here
    ssku.PurchasePrice = 1000;
    ssku.CurrentPrice = 700;

    DefaultCalculator->Calculate(ssku);

    EXPECT_EQ(ssku.LowPrice, 650);
    EXPECT_EQ(ssku.HighPrice, 650);
}

// Low =  (1 - maxLowDelta) * PurchasePrice
// High = HighPrice
TEST(TestDefaultPriceBound, Test_MaxLowDeltaWithTooLowCurrentPrice)
{
    InitGlobalLog2Null();

    TShopSkuData ssku;
    ssku.LowPrice = 300;
    ssku.HighPrice = 1000;
    ssku.BuyPrice = 600;          // not used here
    ssku.PurchasePrice = 1000;
    ssku.CurrentPrice = 600;

    DefaultCalculator->Calculate(ssku);

    EXPECT_EQ(ssku.LowPrice, 700);
    EXPECT_EQ(ssku.HighPrice, 700);
}

TEST(TestDefaultPriceBound, Test_ConfigExceptions)
{
    InitGlobalLog2Null();

    ForEach(std::make_tuple("max_delta", "max_lower_price_delta"), [](const auto& removedConfig) {
        NJson::TJsonValue config = CreateConfig();
        config.EraseValue(removedConfig);
        TPricingConfig pricingConfig(config);
        EXPECT_THROW(NPricingBounds::CreateCalculator("default", pricingConfig), NPrivateException::yexception);
    });
}
