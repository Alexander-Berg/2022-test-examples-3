#include "shared.h"
#include <market/dynamic_pricing/deprecated/autostrategy/checker/high_margin_sku.h>

#include <library/cpp/logger/global/global.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NChecker;
using namespace NMarket::NDynamicPricing::NChecker::NTestShared;

namespace {
    NJson::TJsonValue CreateConfig(double marginThreshold)
    {
        auto config = NJson::TJsonValue();
        config.InsertValue("margin_threshold", marginThreshold);
        return config;
    }
}

// Return error for 1 sku
TEST(TestHighMarginChecker, Test_HighMargin)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "1", 1000, 900);
    AddShopSku(pricesResult, 2, "2", 52, 50);
    AddShopSku(pricesResult, 3, "3", 500, 400);
    AddShopSku(pricesResult, 4, "4", 1000, 1020);

    THighMarginSku checker(CreateConfig(0.1));
    TCheckerResult expectedResult = {
        {3, "3", TCheckerError{static_cast<ui8>(ECheckerErrorCode::TOO_HIGH_MARGIN), "Margin is too high: 0.2"}}
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}

// ignoring fixed prices
TEST(TestHighMarginChecker, Test_FixedPrice)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "1", 1000, 900, 0, 0, 520, 1090);
    AddShopSku(pricesResult, 2, "2", 52, 50, 0, 0, 52, 52);
    AddShopSku(pricesResult, 3, "3", 500, 400, 0, 0, 500, 500);
    AddShopSku(pricesResult, 4, "4", 1000, 1020, 0, 0, 800, 1200);

    auto config = CreateConfig(0.1);
    config.InsertValue("ignore_fixed", true);

    THighMarginSku checker(config);
    TCheckerResult expectedResult = {};
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}
