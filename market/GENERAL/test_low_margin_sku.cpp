#include "shared.h"
#include <market/dynamic_pricing/deprecated/autostrategy/checker/low_margin_sku.h>

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

// Return error for 2 sku
TEST(TestLowMarginChecker, Test_LowMargin)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "1", 1000, 1251);
    AddShopSku(pricesResult, 2, "2", 52, 50);
    AddShopSku(pricesResult, 3, "3", 500, 650);
    AddShopSku(pricesResult, 4, "4", 800, 1200);

    TLowMarginSku checker(CreateConfig(-0.25));
    TCheckerResult expectedResult = {
        {1, "1", TCheckerError{static_cast<ui8>(ECheckerErrorCode::TOO_LOW_MARGIN), "Margin is too low: -0.251"}},
        {3, "3", TCheckerError{static_cast<ui8>(ECheckerErrorCode::TOO_LOW_MARGIN), "Margin is too low: -0.3"}},
        {4, "4", TCheckerError{static_cast<ui8>(ECheckerErrorCode::TOO_LOW_MARGIN), "Margin is too low: -0.5"}}
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}

// ignoring fixed prices
TEST(TestLowMarginChecker, Test_FixedPrice)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "1", 1000, 1251, 0, 0, 1000, 1230);
    AddShopSku(pricesResult, 2, "2", 52, 50, 0, 0, 0, 5000);
    AddShopSku(pricesResult, 3, "3", 500, 650, 0, 0, 200, 590);
    AddShopSku(pricesResult, 4, "4", 800, 1200, 0, 0, 800, 800);

    auto config = CreateConfig(-0.25);
    config.InsertValue("ignore_fixed", true);

    TLowMarginSku checker(config);
    TCheckerResult expectedResult = {
        {1, "1", TCheckerError{static_cast<ui8>(ECheckerErrorCode::TOO_LOW_MARGIN), "Margin is too low: -0.251"}},
        {3, "3", TCheckerError{static_cast<ui8>(ECheckerErrorCode::TOO_LOW_MARGIN), "Margin is too low: -0.3"}}
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}
