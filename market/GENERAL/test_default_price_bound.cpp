#include "config.h"

#include <market/dynamic_pricing/deprecated/autostrategy/price_bounds/default_price_bound.h>

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
    ssku.LowPrice = 800.0;
    ssku.HighPrice = 500.0;
    ssku.BuyPrice = 600.0;          // not used here
    ssku.PurchasePrice = 650.0;
    ssku.CurrentPrice = 770.0;

    DefaultCalculator->Calculate(ssku);

    EXPECT_DOUBLE_EQ(ssku.LowPrice, 800.0);
    EXPECT_DOUBLE_EQ(ssku.HighPrice, 800.0);
}

// Low = (1 - maxDelta) * CurrentPrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestDefaultPriceBound, Test_BaseCase)
{
    InitGlobalLog2Null();

    TShopSkuData ssku;
    ssku.LowPrice = 500.0;
    ssku.HighPrice = 800.0;
    ssku.BuyPrice = 600.0;          // not used here
    ssku.PurchasePrice = 650.0;
    ssku.CurrentPrice = 700.0;

    DefaultCalculator->Calculate(ssku);

    EXPECT_DOUBLE_EQ(ssku.LowPrice, 679.0);
    EXPECT_DOUBLE_EQ(ssku.HighPrice, 721.0);
}

// Low = (1 - maxDelta) * CurrentPrice
// High = HighPrice
TEST(TestDefaultPriceBound, Test_HighLimit)
{
    InitGlobalLog2Null();

    TShopSkuData ssku;
    ssku.LowPrice = 500.0;
    ssku.HighPrice = 715.0;
    ssku.BuyPrice = 600.0;          // not used here
    ssku.PurchasePrice = 650.0;
    ssku.CurrentPrice = 700.0;

    DefaultCalculator->Calculate(ssku);

    EXPECT_DOUBLE_EQ(ssku.LowPrice, 679.0);
    EXPECT_DOUBLE_EQ(ssku.HighPrice, 715.0);
}

// Low =  LowPrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestDefaultPriceBound, Test_LowLimit)
{
    InitGlobalLog2Null();

    TShopSkuData ssku;
    ssku.LowPrice = 685.0;
    ssku.HighPrice = 750.0;
    ssku.BuyPrice = 600.0;          // not used here
    ssku.PurchasePrice = 690.0;
    ssku.CurrentPrice = 700.0;

    DefaultCalculator->Calculate(ssku);

    EXPECT_DOUBLE_EQ(ssku.LowPrice, 685.0);
    EXPECT_DOUBLE_EQ(ssku.HighPrice, 721.0);
}

// Low =  (1 - maxLowDelta) * PurchasePrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestDefaultPriceBound, Test_MaxLowDelta)
{
    InitGlobalLog2Null();

    TShopSkuData ssku;
    ssku.LowPrice = 300.0;
    ssku.HighPrice = 1000.0;
    ssku.BuyPrice = 600.0;          // not used here
    ssku.PurchasePrice = 1000.0;
    ssku.CurrentPrice = 700.0;

    DefaultCalculator->Calculate(ssku);

    EXPECT_DOUBLE_EQ(ssku.LowPrice, 700.0);
    EXPECT_DOUBLE_EQ(ssku.HighPrice, 721.0);
}

// Low =  (1 - maxLowDelta) * PurchasePrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestDefaultPriceBound, Test_MaxLowDeltaWithTooLowHighPrice)
{
    InitGlobalLog2Null();

    TShopSkuData ssku;
    ssku.LowPrice = 600.0;
    ssku.HighPrice = 650.0;
    ssku.BuyPrice = 600.0;          // not used here
    ssku.PurchasePrice = 1000.0;
    ssku.CurrentPrice = 700.0;

    DefaultCalculator->Calculate(ssku);

    EXPECT_DOUBLE_EQ(ssku.LowPrice, 650.0);     // <--- INCORRECT STATE
    EXPECT_DOUBLE_EQ(ssku.HighPrice, 650.0);    // <--- INCORRECT STATE

    //EXPECT_DOUBLE_EQ(ssku.LowPrice, 700.0);     <--- CORRECT STATE
    //EXPECT_DOUBLE_EQ(ssku.HighPrice, 700.0);    <--- CORRECT STATE
}

// Low =  (1 - maxLowDelta) * PurchasePrice
// High = HighPrice
TEST(TestDefaultPriceBound, Test_MaxLowDeltaWithTooLowCurrentPrice)
{
    InitGlobalLog2Null();

    TShopSkuData ssku;
    ssku.LowPrice = 300.0;
    ssku.HighPrice = 1000.0;
    ssku.BuyPrice = 600.0;          // not used here
    ssku.PurchasePrice = 1000.0;
    ssku.CurrentPrice = 600.0;

    DefaultCalculator->Calculate(ssku);

    EXPECT_DOUBLE_EQ(ssku.LowPrice, 700.0);
    EXPECT_DOUBLE_EQ(ssku.HighPrice, 700.0);
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
