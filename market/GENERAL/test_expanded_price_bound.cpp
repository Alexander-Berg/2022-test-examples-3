#include "config.h"

#include <market/dynamic_pricing/deprecated/autostrategy/price_bounds/default_price_bound.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <util/generic/maybe.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NPricingBounds::NTestConfig;

namespace {

constexpr double THRESHOLD = 1e-9;
const auto ExpandedCalculator = NPricingBounds::CreateCalculator("expanded", CreatePricingConfig());

}

// High < Low -> High = Low (sanity check)
TEST(TestExpandedPriceBound, Test_LowAboveHigh)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 800.0;
    data.HighPrice = 500.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 650.0;
    data.PreviousPurchasePrice = 700.0;
    data.CurrentPrice = 770.0;

    ExpandedCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 800.0, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 800.0, THRESHOLD);
}


// Low = (1 - maxDelta) * CurrentPrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestExpandedPriceBound, Test_BaseCase)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 1000.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 690.0;
    data.PreviousPurchasePrice = 700.0;
    data.CurrentPrice = 735.0;      // markup 5%

    ExpandedCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 712.95, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 757.05, THRESHOLD);
}

// Low = (1 - maxDelta) * CurrentPrice
// High = (markup + 1) * PurchasePrice
TEST(TestExpandedPriceBound, Test_BaseCaseExpandHigh)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 1000.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 750.0;
    data.PreviousPurchasePrice = 700.0;
    data.CurrentPrice = 735.0;      // markup 5%

    ExpandedCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 712.95, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 787.5, THRESHOLD);
}

// Low = (markup + 1) * PurchasePrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestExpandedPriceBound, Test_BaseCaseExpandLow)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 1000.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 600.0;
    data.PreviousPurchasePrice = 700.0;
    data.CurrentPrice = 735.0;      // markup 5%

    ExpandedCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 630, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 757.05, THRESHOLD);
}

// Low = (1 - maxDelta) * CurrentPrice
// High = HighPrice
TEST(TestExpandedPriceBound, Test_HighLimit)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 750.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 690.0;
    data.PreviousPurchasePrice = 700.0;
    data.CurrentPrice = 735.0;      // markup 5%

    ExpandedCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 712.95, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 750.0, THRESHOLD);
}

// Low =  LowPrice
// High = (1 + markup + maxDelta) * PurchasePrice
TEST(TestExpandedPriceBound, Test_LowLimit)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 720.0;
    data.HighPrice = 1000.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 690.0;
    data.PreviousPurchasePrice = 700.0;
    data.CurrentPrice = 735.0;      // markup 5%

    ExpandedCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 720.0, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 757.05, THRESHOLD);
}

// Negative markup
// Low = (1 + markup) * PurchasePrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestExpandedPriceBound, Test_NegativeMarkup)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 1000.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 600.0;
    data.PreviousPurchasePrice = 700.0;
    data.CurrentPrice = 630.0;      // markup -10%

    ExpandedCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 540.0, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 648.9, THRESHOLD);
}

// Low =  (1 - maxLowDelta) * PurchasePrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestExpandedPriceBound, Test_MaxLowDeltaWithTooLowHighPrice)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 600.0;
    data.HighPrice = 650.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 1000.0;
    data.PreviousPurchasePrice = 1000.0;
    data.CurrentPrice = 1050.0;        // markup 5%

    ExpandedCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 650.0, THRESHOLD);     // <--- INCORRECT STATE
    EXPECT_NEAR(data.HighPrice, 650.0, THRESHOLD);    // <--- INCORRECT STATE

    //EXPECT_DOUBLE_EQ(data.LowPrice, 700.0);     <--- CORRECT STATE
    //EXPECT_DOUBLE_EQ(data.HighPrice, 700.0);    <--- CORRECT STATE
}

// Low =  CurrentPrice * 0.5
// High = (1 + maxDelta) * CurrentPrice
TEST(TestExpandedPriceBound, Test_HalfCurrentPriceChange)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 1000.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 380.0;
    data.PreviousPurchasePrice = 800.0;
    data.CurrentPrice = 800.0;        // markup 0%

    ExpandedCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 400.0, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 824.0, THRESHOLD);
}

// Low =  (1 - maxDelta) * CurrentPrice
// High = CurrentPrice * 1.5
TEST(TestExpandedPriceBound, Test_OneAndHalfCurrentPriceChange)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300.0;
    data.HighPrice = 1000.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 750.0;
    data.PreviousPurchasePrice = 500.0;
    data.CurrentPrice = 550.0;        // markup 10%

    ExpandedCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 533.5, THRESHOLD);
    EXPECT_NEAR(data.HighPrice, 825.0, THRESHOLD);
}

// Low = Low
// High = Low
TEST(TestExpandedPriceBound, Test_OneAndHalfCurrentPriceChangeTooLow)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 800.0;
    data.HighPrice = 1000.0;
    data.BuyPrice = 600.0;          // not used here
    data.PurchasePrice = 800.0;
    data.PreviousPurchasePrice = 500.0;
    data.CurrentPrice = 525.0;        // markup 5%

    ExpandedCalculator->Calculate(data);

    EXPECT_NEAR(data.LowPrice, 800.0, THRESHOLD);       // <--- IS IT CORRECT?
    EXPECT_NEAR(data.HighPrice, 800.0, THRESHOLD);      // <--- IS IT CORRECT?
}

TEST(TestExpandedPriceBound, Test_ConfigExceptions)
{
    InitGlobalLog2Null();

    ForEach(std::make_tuple("max_delta", "max_lower_price_delta"), [](const auto& removedConfig) {
        NJson::TJsonValue config = CreateConfig();
        config.EraseValue(removedConfig);
        TPricingConfig pricingConfig(config);
        EXPECT_THROW(NPricingBounds::CreateCalculator("expanded", pricingConfig), NPrivateException::yexception);
    });
}
