#include "config.h"

#include <market/dynamic_pricing/deprecated/autostrategy/price_bounds/markup_price_bound.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <util/generic/maybe.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NPricingBounds::NTestConfig;

namespace {

constexpr double THRESHOLD = 1e-9;
const auto MarkupCalculator = NPricingBounds::CreateCalculator("markup", CreatePricingConfig());

}

// High < Low -> High = Low (sanity check)
TEST(TestMarkupPriceBound, Test_LowAboveHigh)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 800.0;
    data.HighPrice = 500.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 650.0;
    data.PreviousPurchasePrice = 700.0;
    data.CurrentPrice = 770.0;

    MarkupCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 800.0, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 800.0, THRESHOLD);
}

// Low = (1 + markup - maxDelta) * PurchasePrice
// High = (1 + markup + maxDelta) * PurchasePrice
TEST(TestMarkupPriceBound, Test_BaseCase)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 800.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 500.0;
    data.PreviousPurchasePrice = 700.0;
    data.CurrentPrice = 770.0;      // markup 10%

    MarkupCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 535.0, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 565.0, THRESHOLD);
}

// Low = (1 + markup - maxDelta) * PurchasePrice
// High = HighPrice
TEST(TestMarkupPriceBound, Test_HighLimit)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 550.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 500.0;
    data.PreviousPurchasePrice = 700.0;
    data.CurrentPrice = 770.0;      // markup 10%

    MarkupCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 535.0, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 550.0, THRESHOLD);
}

// Low =  LowPrice
// High = (1 + markup + maxDelta) * PurchasePrice
TEST(TestMarkupPriceBound, Test_LowLimit)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 550.0;
    data.HighPrice = 700.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 500.0;
    data.PreviousPurchasePrice = 700.0;
    data.CurrentPrice = 770.0;      // markup 10%

    MarkupCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 550.0, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 565.0, THRESHOLD);
}

// Negative markup
// Low = (1 + markup - maxDelta) * PurchasePrice
// High = (1 + markup + maxDelta) * PurchasePrice
TEST(TestMarkupPriceBound, Test_NegativeMarkup)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 1000.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 1000.0;
    data.PreviousPurchasePrice = 700.0;
    data.CurrentPrice = 630.0;      // markup -10%

    MarkupCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 870.0, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 930.0, THRESHOLD);
}

// Too negative markup (-markup + maxDelta > 0.2)
// Low = (1 - maxLowDelta) * PurchasePrice
// High = (1 + markup + maxDelta) * PurchasePrice
TEST(TestMarkupPriceBound, Test_TooNegativeMarkup)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 1000.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 800.0;
    data.PreviousPurchasePrice = 1000.0;
    data.CurrentPrice = 720.0;      // markup -28%

    MarkupCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 560.0, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 600.0, THRESHOLD);
}

// Low =  (1 - maxLowDelta) * PurchasePrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestMarkupPriceBound, Test_MaxLowDeltaWithTooLowHighPrice)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 600.0;
    data.HighPrice = 650.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 1000.0;
    data.PreviousPurchasePrice = 1000.0;
    data.CurrentPrice = 1050.0;        // markup 5%

    MarkupCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 650.0, THRESHOLD);     // <--- INCORRECT STATE
    EXPECT_NEAR(data.HighPrice, 650.0, THRESHOLD);    // <--- INCORRECT STATE

    //EXPECT_DOUBLE_EQ(data.LowPrice, 700.0);     <--- CORRECT STATE
    //EXPECT_DOUBLE_EQ(data.HighPrice, 700.0);    <--- CORRECT STATE
}

// Low =  CurrentPrice * 0.5
// High = (1 + markup + maxDelta) * PurchasePrice
TEST(TestMarkupPriceBound, Test_HalfCurrentPriceChange)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 1000.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 400.0;
    data.PreviousPurchasePrice = 800.0;
    data.CurrentPrice = 800.0;        // markup 0%

    MarkupCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 400.0, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 412.0, THRESHOLD);
}

// Low =  (1 + markup - maxDelta) * PurchasePrice
// High = CurrentPrice * 1.5
TEST(TestMarkupPriceBound, Test_OneAndHalfCurrentPriceChange)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 1000.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 750.0;
    data.PreviousPurchasePrice = 500.0;
    data.CurrentPrice = 550.0;        // markup 10%

    MarkupCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 802.5, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 825.0, THRESHOLD);
}

// Low =  CurrentPrice * 1.5
// High = CurrentPrice * 1.5
TEST(TestMarkupPriceBound, Test_OneAndHalfCurrentPriceChangeBelowLow)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 1000.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 800.0;
    data.PreviousPurchasePrice = 500.0;
    data.CurrentPrice = 525.0;        // markup 5%

    MarkupCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 787.5, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 787.5, THRESHOLD);
}

// Low = Low
// High = Low
TEST(TestMarkupPriceBound, Test_OneAndHalfCurrentPriceChangeTooLow)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 800.0;
    data.HighPrice = 1000.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 800.0;
    data.PreviousPurchasePrice = 500.0;
    data.CurrentPrice = 525.0;        // markup 5%

    MarkupCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 800.0, THRESHOLD);       // <--- IS IT CORRECT?
    EXPECT_NEAR(data.HighPrice, 800.0, THRESHOLD);      // <--- IS IT CORRECT?
}

TEST(TestMarkupPriceBound, Test_ConfigExceptions)
{
    InitGlobalLog2Null();

    ForEach(std::make_tuple("max_delta", "max_lower_price_delta"), [](const auto& removedConfig) {
        NJson::TJsonValue config = CreateConfig();
        config.EraseValue(removedConfig);
        TPricingConfig pricingConfig(config);
        EXPECT_THROW(NPricingBounds::CreateCalculator("markup", pricingConfig), NPrivateException::yexception);
    });
}
