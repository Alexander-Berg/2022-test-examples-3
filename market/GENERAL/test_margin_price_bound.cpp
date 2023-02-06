#include "config.h"

#include <market/dynamic_pricing/deprecated/autostrategy/price_bounds/margin_price_bound.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <util/generic/maybe.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NPricingBounds::NTestConfig;

namespace {

constexpr double THRESHOLD = 1e-9;

// Data from global config:
// targetMargin = 0.1;
// maxExpandDelta = 0.1;
const auto MarginCalculator = NPricingBounds::CreateCalculator("margin", CreatePricingConfig());

}

// High < Low -> High = Low (sanity check)
TEST(TestMarginPriceBound, Test_LowAboveHigh)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 800.0;
    data.HighPrice = 500.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 650.0;
    data.PreviousPurchasePrice = 700.0;
    data.CurrentPrice = 770.0;

    MarginCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 800.0, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 800.0, THRESHOLD);
}

// Low = (1 - maxDelta) * CurrentPrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestMarginPriceBound, Test_NoExpansion)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 800.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 550.0;
    data.CurrentPrice = 620.0;

    MarginCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 601.4, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 638.6, THRESHOLD);
}

// Low = PurchasePrice / (1 - targetMargin)
// High = (1 + maxDelta) * CurrentPrice
TEST(TestMarginPriceBound, Test_LowBoundExpansion)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 800.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 540.0;
    data.CurrentPrice = 650.0;

    MarginCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 600.0, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 669.5, THRESHOLD);
}

// Low = (1 - maxDelta) * CurrentPrice
// High = PurchasePrice / (1 - targetMargin)
TEST(TestMarginPriceBound, Test_HighBoundExpansion)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 800.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 630.0;
    data.CurrentPrice = 650.0;

    MarginCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 630.5, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 700.0, THRESHOLD);
}

// Low = (1 - maxDelta) * CurrentPrice
// High = HighPrice
TEST(TestMarginPriceBound, Test_HighLimit)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 690.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 630.0;
    data.CurrentPrice = 650.0;

    MarginCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 630.5, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 690.0, THRESHOLD);
}

// Low = LowPrice
// High = PurchasePrice / (1 - targetMargin)
TEST(TestMarginPriceBound, Test_LowLimit)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 640.0;
    data.HighPrice = 800.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 630.0;
    data.CurrentPrice = 650.0;

    MarginCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 640.0, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 700.0, THRESHOLD);
}

// Negative margin
// Low = PurchasePrice / (1 - targetMargin)
// High = (1 + maxDelta) * CurrentPrice
TEST(TestMarginPriceBound, Test_NegativeMarkup)
{
    InitGlobalLog2Null();

    // Set negative margin, other parameters are default (as in global config)
    double targetMargin = -0.01;
    const auto config = CreateConfig(targetMargin);

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 800.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 606.0;
    data.CurrentPrice = 650.0;

    const auto calculator = NPricingBounds::CreateCalculator("margin", TPricingConfig(config));
    calculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 600.0, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 669.5, THRESHOLD);
}

// Low =  (1 - maxLowDelta) * PurchasePrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestMarginPriceBound, Test_MaxLowDeltaWithTooLowHighPrice)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 600.0;
    data.HighPrice = 650.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 1000.0;
    data.CurrentPrice = 1050.0;

    MarginCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 650.0, THRESHOLD);     // <--- INCORRECT STATE
    EXPECT_NEAR(data.HighPrice, 650.0, THRESHOLD);    // <--- INCORRECT STATE

    //EXPECT_DOUBLE_EQ(data.LowPrice, 700.0);     <--- CORRECT STATE
    //EXPECT_DOUBLE_EQ(data.HighPrice, 700.0);    <--- CORRECT STATE
}

// Low =  (1 - MaxExpandDelta) * CurrentPrice
// High = (1 + maxDelta) * PurchasePrice
TEST(TestMarginPriceBound, Test_MaxExpandDeltaLowBound)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 500.0;
    data.HighPrice = 1000.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 600.0;
    data.CurrentPrice = 800.0;

    MarginCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 720.0, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 824.0, THRESHOLD);
}

// Low = (1 - maxDelta) * PurchasePrice
// High =  (1 + MaxExpandDelta) * CurrentPrice
TEST(TestMarginPriceBound, Test_MaxExpandDeltaHighBound)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 500.0;
    data.HighPrice = 1000.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 900.0;
    data.CurrentPrice = 800.0;

    MarginCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 776.0, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 880.0, THRESHOLD);
}

TEST(TestMarginPriceBound, Test_ConfigExceptions)
{
    InitGlobalLog2Null();

    ForEach(std::make_tuple("max_delta", "max_lower_price_delta", "max_expand_delta", "target_margin"), [](const auto& removedConfig) {
        NJson::TJsonValue config = CreateConfig();
        config.EraseValue(removedConfig);
        TPricingConfig pricingConfig(config);
        EXPECT_THROW(NPricingBounds::CreateCalculator("margin", pricingConfig), NPrivateException::yexception);
    });
}
