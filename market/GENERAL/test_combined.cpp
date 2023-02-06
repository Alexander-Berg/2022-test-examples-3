#include "shared.h"
#include <market/dynamic_pricing/deprecated/autostrategy/checker/combined.h>

#include <library/cpp/logger/global/global.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NChecker;
using namespace NMarket::NDynamicPricing::NChecker::NTestShared;

namespace {
    NJson::TJsonValue CreateCombinedConfig(double coefficient, double lowMarginThreshold, double highMarginThreshold) {
        auto config = NJson::TJsonValue();

        auto cheapSkuCheckerConfig = NJson::TJsonValue();
        auto cheapSkuConfig = NJson::TJsonValue();
        cheapSkuConfig.InsertValue("coefficient", coefficient);
        cheapSkuCheckerConfig.InsertValue("name", "cheap_against_white_sku");
        cheapSkuCheckerConfig.InsertValue("config", cheapSkuConfig);

        auto lowMarginCheckerConfig = NJson::TJsonValue();
        auto lowMarginConfig = NJson::TJsonValue();
        lowMarginConfig.InsertValue("margin_threshold", lowMarginThreshold);
        lowMarginCheckerConfig.InsertValue("name", "low_margin_sku");
        lowMarginCheckerConfig.InsertValue("config", lowMarginConfig);

        auto highMarginCheckerConfig = NJson::TJsonValue();
        auto highMarginConfig = NJson::TJsonValue();
        highMarginConfig.InsertValue("margin_threshold", highMarginThreshold);
        highMarginCheckerConfig.InsertValue("name", "high_margin_sku");
        highMarginCheckerConfig.InsertValue("config", highMarginConfig);

        auto checkersArrayConfig = NJson::TJsonValue();
        checkersArrayConfig.AppendValue(cheapSkuCheckerConfig);
        checkersArrayConfig.AppendValue(lowMarginCheckerConfig);
        checkersArrayConfig.AppendValue(highMarginCheckerConfig);

        config.InsertValue("checkers", checkersArrayConfig);
        return config;
    }
}

// 3 sku with errors (error in 1 rule for sku)
// Ordered as checkers
TEST(TestCombinedChecker, Test_CombinedNoIntersection)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "1", 80, 80, 0.5, 160);
    AddShopSku(pricesResult, 2, "2", 500, 400, 1.8, 420);
    AddShopSku(pricesResult, 3, "3", 500, 650, 3.2, 480);
    AddShopSku(pricesResult, 4, "4", 1000, 1020, 2.1, 900);

    const auto coefficient = 0.8;
    const auto lowMarginThreshold = -0.25;
    const auto highMarginThreshold = 0.1;

    const auto config = CreateCombinedConfig(coefficient, lowMarginThreshold, highMarginThreshold);
    TCombined checker(config);
    TCheckerResult expectedResult = {
        {1, "1", TCheckerError{static_cast<ui8>(ECheckerErrorCode::CHEAPER_THAN_WHITE), "Price is too low comparing to Yandex.Market: 80 VS 160"}},
        {3, "3", TCheckerError{static_cast<ui8>(ECheckerErrorCode::TOO_LOW_MARGIN), "Margin is too low: -0.3"}},
        {2, "2", TCheckerError{static_cast<ui8>(ECheckerErrorCode::TOO_HIGH_MARGIN), "Margin is too high: 0.2"}}
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}

// 3 sku with errors (4 errors total, 1 sku with 2 errors)
// Ordered as checkers
TEST(TestCombinedChecker, Test_CombinedWithIntersection)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "1", 80, 60, 0.5, 160);
    AddShopSku(pricesResult, 2, "2", 500, 650, 3.2, 480);
    AddShopSku(pricesResult, 3, "3", 500, 400, 1.8, 420);
    AddShopSku(pricesResult, 4, "4", 1000, 1020, 2.1, 900);

    const auto coefficient = 0.8;
    const auto lowMarginThreshold = -0.25;
    const auto highMarginThreshold = 0.1;

    const auto config = CreateCombinedConfig(coefficient, lowMarginThreshold, highMarginThreshold);
    TCombined checker(config);
    TCheckerResult expectedResult = {
        {1, "1", TCheckerError{static_cast<ui8>(ECheckerErrorCode::CHEAPER_THAN_WHITE), "Price is too low comparing to Yandex.Market: 80 VS 160"}},
        {2, "2", TCheckerError{static_cast<ui8>(ECheckerErrorCode::TOO_LOW_MARGIN), "Margin is too low: -0.3"}},
        {1, "1", TCheckerError{static_cast<ui8>(ECheckerErrorCode::TOO_HIGH_MARGIN), "Margin is too high: 0.25"}},
        {3, "3", TCheckerError{static_cast<ui8>(ECheckerErrorCode::TOO_HIGH_MARGIN), "Margin is too high: 0.2"}}
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}
