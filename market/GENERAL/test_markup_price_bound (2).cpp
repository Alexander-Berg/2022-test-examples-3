#include "config.h"

#include <market/dynamic_pricing/pricing/dynamic_pricing/autostrategy/price_bounds/markup_price_bound.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <util/generic/maybe.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NPricingBounds::NTestConfig;

namespace {

const auto MarkupCalculator = NPricingBounds::CreateCalculator("markup", CreatePricingConfig());

}

// High < Low -> High = Low (sanity check)
TEST(TestMarkupPriceBound, Test_LowAboveHigh)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 800;
    data.HighPrice = 500;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 650;
    data.PreviousPurchasePrice = 700;
    data.CurrentPrice = 770;

    MarkupCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 800);
    EXPECT_EQ(data.HighPrice, 800);
}

// Low = (1 + markup - maxDelta) * PurchasePrice
// High = (1 + markup + maxDelta) * PurchasePrice
TEST(TestMarkupPriceBound, Test_BaseCase)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300;
    data.HighPrice = 800;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 500;
    data.PreviousPurchasePrice = 700;
    data.CurrentPrice = 770;      // markup 10%

    MarkupCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 535);
    EXPECT_EQ(data.HighPrice, 565);
}

// Low = (1 + markup - maxDelta) * PurchasePrice
// High = HighPrice
TEST(TestMarkupPriceBound, Test_HighLimit)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300;
    data.HighPrice = 550;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 500;
    data.PreviousPurchasePrice = 700;
    data.CurrentPrice = 770;      // markup 10%

    MarkupCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 535);
    EXPECT_EQ(data.HighPrice, 550);
}

// Low =  LowPrice
// High = (1 + markup + maxDelta) * PurchasePrice
TEST(TestMarkupPriceBound, Test_LowLimit)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 550;
    data.HighPrice = 700;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 500;
    data.PreviousPurchasePrice = 700;
    data.CurrentPrice = 770;      // markup 10%

    MarkupCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 550);
    EXPECT_EQ(data.HighPrice, 565);
}

// Negative markup
// Low = (1 + markup - maxDelta) * PurchasePrice
// High = (1 + markup + maxDelta) * PurchasePrice
TEST(TestMarkupPriceBound, Test_NegativeMarkup)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300;
    data.HighPrice = 1000;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 1000;
    data.PreviousPurchasePrice = 700;
    data.CurrentPrice = 630;      // markup -10%

    MarkupCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 870);
    EXPECT_EQ(data.HighPrice, 930);
}

// Too negative markup (-markup + maxDelta > 0.2)
// Low = (1 - maxLowDelta) * PurchasePrice
// High = (1 + markup + maxDelta) * PurchasePrice
TEST(TestMarkupPriceBound, Test_TooNegativeMarkup)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300;
    data.HighPrice = 1000;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 800;
    data.PreviousPurchasePrice = 1000;
    data.CurrentPrice = 720;      // markup -28%

    MarkupCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 560);
    EXPECT_EQ(data.HighPrice, 600);
}

// Low =  (1 - maxLowDelta) * PurchasePrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestMarkupPriceBound, Test_MaxLowDeltaWithTooLowHighPrice)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 600;
    data.HighPrice = 650;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 1000;
    data.PreviousPurchasePrice = 1000;
    data.CurrentPrice = 1050;        // markup 5%

    MarkupCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 650);     // <--- INCORRECT STATE
    EXPECT_EQ(data.HighPrice, 650);    // <--- INCORRECT STATE
}

// Low =  CurrentPrice * 0.5
// High = (1 + markup + maxDelta) * PurchasePrice
TEST(TestMarkupPriceBound, Test_HalfCurrentPriceChange)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300;
    data.HighPrice = 1000;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 400;
    data.PreviousPurchasePrice = 800;
    data.CurrentPrice = 800;        // markup 0%

    MarkupCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 400);
    EXPECT_EQ(data.HighPrice, 412);
}

// Low =  (1 + markup - maxDelta) * PurchasePrice
// High = CurrentPrice * 1.5
TEST(TestMarkupPriceBound, Test_OneAndHalfCurrentPriceChange)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300;
    data.HighPrice = 1000;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 750;
    data.PreviousPurchasePrice = 500;
    data.CurrentPrice = 550;        // markup 10%

    MarkupCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 802);
    EXPECT_EQ(data.HighPrice, 825);
}

// Low =  CurrentPrice * 1.5
// High = CurrentPrice * 1.5
TEST(TestMarkupPriceBound, Test_OneAndHalfCurrentPriceChangeBelowLow)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300;
    data.HighPrice = 1000;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 800;
    data.PreviousPurchasePrice = 500;
    data.CurrentPrice = 525;        // markup 5%

    MarkupCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 787);
    EXPECT_EQ(data.HighPrice, 787);
}

// Low = Low
// High = Low
TEST(TestMarkupPriceBound, Test_OneAndHalfCurrentPriceChangeTooLow)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 800;
    data.HighPrice = 1000;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 800;
    data.PreviousPurchasePrice = 500;
    data.CurrentPrice = 525;        // markup 5%

    MarkupCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 800);       // <--- IS IT CORRECT?
    EXPECT_EQ(data.HighPrice, 800);      // <--- IS IT CORRECT?
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
