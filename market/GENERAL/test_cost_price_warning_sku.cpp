#include "shared.h"
#include <market/dynamic_pricing/deprecated/autostrategy/checker/cost_price_warning_sku.h>

#include <library/cpp/logger/global/global.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NChecker;
using namespace NMarket::NDynamicPricing::NChecker::NTestShared;

namespace {
    NJson::TJsonValue CreateConfig(double coefficient)
    {
        auto config = NJson::TJsonValue();
        config.InsertValue("coefficient", coefficient);
        return config;
    }
}

// Return error for 1 sku
TEST(TestCostPriceChecker, Test_NullCostPrice)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "1", 80, 50, 2.0, 160, 80, 80);
    AddShopSku(pricesResult, 2, "2", 200, 140, 1.1, 250, 100, 350);

    pricesResult.Get("1", 1).SkuData.CostPrice = Nothing();
    pricesResult.Get("1", 1).SkuData.PurchasePrice = 115;
    pricesResult.Get("1", 1).SkuData.WhiteMedianPrice = 120;

    pricesResult.Get("2", 2).SkuData.CostPrice = 120;
    pricesResult.Get("2", 2).SkuData.PurchasePrice = 115;
    pricesResult.Get("2", 2).SkuData.WhiteMedianPrice = 120;

    TCostPriceWarningSku checker(CreateConfig(0.5));
    TCheckerResult expectedResult = {
        {1, "1", TCheckerError{static_cast<ui8>(ECheckerErrorCode::COST_PRICE_WARNING), "Cost price is null"}},
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}

// Return error for 1 sku
TEST(TestCostPriceChecker, Test_TooLowCostPrice)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "11", 80, 50, 2.0, 160, 80, 80);
    AddShopSku(pricesResult, 1, "12", 80, 50, 2.0, 160, 80, 80);
    AddShopSku(pricesResult, 2, "21", 200, 140, 1.1, 250, 100, 350);
    AddShopSku(pricesResult, 2, "22", 200, 140, 1.1, 250, 100, 350);

    pricesResult.Get("11",  1).SkuData.CostPrice = 40;
    pricesResult.Get("11",  1).SkuData.PurchasePrice = 115;
    pricesResult.Get("11",  1).SkuData.WhiteMedianPrice = 120;
    pricesResult.Get("12",  1).SkuData.CostPrice = 40;
    pricesResult.Get("12",  1).SkuData.PurchasePrice = 115;
    pricesResult.Get("12",  1).SkuData.WhiteMedianPrice = 120;

    pricesResult.Get("21",  2).SkuData.CostPrice = 120;
    pricesResult.Get("21",  2).SkuData.PurchasePrice = 95;
    pricesResult.Get("21",  2).SkuData.WhiteMedianPrice = 112;
    pricesResult.Get("22",  2).SkuData.CostPrice = 120;
    pricesResult.Get("22",  2).SkuData.PurchasePrice = 95;
    pricesResult.Get("22",  2).SkuData.WhiteMedianPrice = 112;

    TCostPriceWarningSku checker(CreateConfig(0.5));
    TCheckerResult expectedResult = {
        {1, "11", TCheckerError{static_cast<ui8>(ECheckerErrorCode::COST_PRICE_WARNING),
            "Cost price is suspicious (CostPrice = 40; WhiteMedianPrice = 120; PurchasePrice = 115)"}},
        {1, "12", TCheckerError{static_cast<ui8>(ECheckerErrorCode::COST_PRICE_WARNING),
            "Cost price is suspicious (CostPrice = 40; WhiteMedianPrice = 120; PurchasePrice = 115)"}},
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}
