#include "config.h"

#include <market/dynamic_pricing/pricing/dynamic_pricing/autostrategy/price_bounds/margin_price_bound.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <util/generic/maybe.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NPricingBounds::NTestConfig;

namespace {

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
    data.LowPrice = 800;
    data.HighPrice = 500;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 650;
    data.PreviousPurchasePrice = 700;
    data.CurrentPrice = 770;

    MarginCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 800);
    EXPECT_EQ(data.HighPrice, 800);
}

// Low = (1 - maxDelta) * CurrentPrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestMarginPriceBound, Test_NoExpansion)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300;
    data.HighPrice = 800;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 550;
    data.CurrentPrice = 620;

    MarginCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 601);
    EXPECT_EQ(data.HighPrice, 638);
}

// Low = PurchasePrice / (1 - targetMargin)
// High = (1 + maxDelta) * CurrentPrice
TEST(TestMarginPriceBound, Test_LowBoundExpansion)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300;
    data.HighPrice = 800;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 540;
    data.CurrentPrice = 650;

    MarginCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 600);
    EXPECT_EQ(data.HighPrice, 669);
}

// Low = (1 - maxDelta) * CurrentPrice
// High = PurchasePrice / (1 - targetMargin)
TEST(TestMarginPriceBound, Test_HighBoundExpansion)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300;
    data.HighPrice = 800;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 630;
    data.CurrentPrice = 650;

    MarginCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 630);
    EXPECT_EQ(data.HighPrice, 700);
}

// Low = (1 - maxDelta) * CurrentPrice
// High = HighPrice
TEST(TestMarginPriceBound, Test_HighLimit)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300;
    data.HighPrice = 690;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 630;
    data.CurrentPrice = 650;

    MarginCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 630);
    EXPECT_EQ(data.HighPrice, 690);
}

// Low = LowPrice
// High = PurchasePrice / (1 - targetMargin)
TEST(TestMarginPriceBound, Test_LowLimit)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 640;
    data.HighPrice = 800;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 630;
    data.CurrentPrice = 650;

    MarginCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 640);
    EXPECT_EQ(data.HighPrice, 700);
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
    data.LowPrice = 300;
    data.HighPrice = 800;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 606;
    data.CurrentPrice = 650;

    const auto calculator = NPricingBounds::CreateCalculator("margin", TPricingConfig(config));
    calculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 600);
    EXPECT_EQ(data.HighPrice, 669);
}

// Low =  (1 - maxLowDelta) * PurchasePrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestMarginPriceBound, Test_MaxLowDeltaWithTooLowHighPrice)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 600;
    data.HighPrice = 650;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 1000;
    data.CurrentPrice = 1050;

    MarginCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 650);     // <--- INCORRECT STATE
    EXPECT_EQ(data.HighPrice, 650);    // <--- INCORRECT STATE
}

// Low =  (1 - MaxExpandDelta) * CurrentPrice
// High = (1 + maxDelta) * PurchasePrice
TEST(TestMarginPriceBound, Test_MaxExpandDeltaLowBound)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 500;
    data.HighPrice = 1000;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 600;
    data.CurrentPrice = 800;

    MarginCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 720);
    EXPECT_EQ(data.HighPrice, 824);
}

// Low = (1 - maxDelta) * PurchasePrice
// High =  (1 + MaxExpandDelta) * CurrentPrice
TEST(TestMarginPriceBound, Test_MaxExpandDeltaHighBound)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 500;
    data.HighPrice = 1000;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 900;
    data.CurrentPrice = 800;

    MarginCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 776);
    EXPECT_EQ(data.HighPrice, 880);
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
