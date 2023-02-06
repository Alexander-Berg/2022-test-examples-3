#include "config.h"

#include <market/dynamic_pricing/pricing/dynamic_pricing/autostrategy/price_bounds/default_price_bound.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <util/generic/maybe.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NPricingBounds::NTestConfig;

namespace {

const auto ExpandedCalculator = NPricingBounds::CreateCalculator("expanded", CreatePricingConfig());

}

// High < Low -> High = Low (sanity check)
TEST(TestExpandedPriceBound, Test_LowAboveHigh)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 800;
    data.HighPrice = 500;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 650;
    data.PreviousPurchasePrice = 700;
    data.CurrentPrice = 770;

    ExpandedCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 800);
    EXPECT_EQ(data.HighPrice, 800);
}


// Low = (1 - maxDelta) * CurrentPrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestExpandedPriceBound, Test_BaseCase)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300;
    data.HighPrice = 1000;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 690;
    data.PreviousPurchasePrice = 700;
    data.CurrentPrice = 735;      // markup 5%

    ExpandedCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 712);
    EXPECT_EQ(data.HighPrice, 757);
}

// Low = (1 - maxDelta) * CurrentPrice
// High = (markup + 1) * PurchasePrice
TEST(TestExpandedPriceBound, Test_BaseCaseExpandHigh)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300;
    data.HighPrice = 1000;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 750;
    data.PreviousPurchasePrice = 700;
    data.CurrentPrice = 735;      // markup 5%

    ExpandedCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 712);
    EXPECT_EQ(data.HighPrice, 787);
}

// Low = (markup + 1) * PurchasePrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestExpandedPriceBound, Test_BaseCaseExpandLow)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300;
    data.HighPrice = 1000;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 600;
    data.PreviousPurchasePrice = 700;
    data.CurrentPrice = 735;      // markup 5%

    ExpandedCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 630);
    EXPECT_EQ(data.HighPrice, 757);
}

// Low = (1 - maxDelta) * CurrentPrice
// High = HighPrice
TEST(TestExpandedPriceBound, Test_HighLimit)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300;
    data.HighPrice = 750;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 690;
    data.PreviousPurchasePrice = 700;
    data.CurrentPrice = 735;      // markup 5%

    ExpandedCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 712);
    EXPECT_EQ(data.HighPrice, 750);
}

// Low =  LowPrice
// High = (1 + markup + maxDelta) * PurchasePrice
TEST(TestExpandedPriceBound, Test_LowLimit)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 720;
    data.HighPrice = 1000;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 690;
    data.PreviousPurchasePrice = 700;
    data.CurrentPrice = 735;      // markup 5%

    ExpandedCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 720);
    EXPECT_EQ(data.HighPrice, 757);
}

// Negative markup
// Low = (1 + markup) * PurchasePrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestExpandedPriceBound, Test_NegativeMarkup)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300;
    data.HighPrice = 1000;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 600;
    data.PreviousPurchasePrice = 700;
    data.CurrentPrice = 630;      // markup -10%

    ExpandedCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 540);
    EXPECT_EQ(data.HighPrice, 648);
}

// Low =  (1 - maxLowDelta) * PurchasePrice
// High = (1 + maxDelta) * CurrentPrice
TEST(TestExpandedPriceBound, Test_MaxLowDeltaWithTooLowHighPrice)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 600;
    data.HighPrice = 650;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 1000;
    data.PreviousPurchasePrice = 1000;
    data.CurrentPrice = 1050;        // markup 5%

    ExpandedCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 650);
    EXPECT_EQ(data.HighPrice, 650);
}

// Low =  CurrentPrice * 0.5
// High = (1 + maxDelta) * CurrentPrice
TEST(TestExpandedPriceBound, Test_HalfCurrentPriceChange)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300;
    data.HighPrice = 1000;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 380;
    data.PreviousPurchasePrice = 800;
    data.CurrentPrice = 800;        // markup 0%

    ExpandedCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 400);
    EXPECT_EQ(data.HighPrice, 824);
}

// Low =  (1 - maxDelta) * CurrentPrice
// High = CurrentPrice * 1.5
TEST(TestExpandedPriceBound, Test_OneAndHalfCurrentPriceChange)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 300;
    data.HighPrice = 1000;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 750;
    data.PreviousPurchasePrice = 500;
    data.CurrentPrice = 550;        // markup 10%

    ExpandedCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 533);
    EXPECT_EQ(data.HighPrice, 825);
}

// Low = Low
// High = Low
TEST(TestExpandedPriceBound, Test_OneAndHalfCurrentPriceChangeTooLow)
{
    InitGlobalLog2Null();

    TShopSkuData data;
    data.LowPrice = 800;
    data.HighPrice = 1000;
    data.BuyPrice = 600;          // not used here
    data.PurchasePrice = 800;
    data.PreviousPurchasePrice = 500;
    data.CurrentPrice = 525;        // markup 5%

    ExpandedCalculator->Calculate(data);

    EXPECT_EQ(data.LowPrice, 800);
    EXPECT_EQ(data.HighPrice, 800);
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
