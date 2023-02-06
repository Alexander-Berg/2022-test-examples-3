#include "shared.h"
#include <market/dynamic_pricing/deprecated/autostrategy/checker/purchase_price_warning_sku.h>

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
TEST(TestPurchasePriceChecker, Test_TooHighPurchasePrice)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "1", 80, 50, 2.0, 160, 80, 80);
    AddShopSku(pricesResult, 2, "2", 200, 140, 1.1, 250, 100, 350);

    pricesResult.Get("1", 1).SkuData.CostPrice = 98;
    pricesResult.Get("1", 1).SkuData.PurchasePrice = 115;
    pricesResult.Get("1", 1).SkuData.WhiteMedianPrice = 120;

    pricesResult.Get("2", 2).SkuData.CostPrice = 220;
    pricesResult.Get("2", 2).SkuData.PurchasePrice = 312;
    pricesResult.Get("2", 2).SkuData.WhiteMedianPrice = 120;

    TPurchasePriceWarningSku checker(CreateConfig(0.5));
    TCheckerResult expectedResult = {
        {2, "2", TCheckerError{static_cast<ui8>(ECheckerErrorCode::PURCHASE_PRICE_WARNING),
            "Purchase price is suspicious (PurchasePrice = 312; WhiteMedianPrice = 120; CostPrice = 220)"}},
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}

