#include "shared.h"
#include <market/dynamic_pricing/deprecated/autostrategy/checker/cost_purchase_warning.h>

#include <library/cpp/logger/global/global.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NChecker;
using namespace NMarket::NDynamicPricing::NChecker::NTestShared;

namespace {
    NJson::TJsonValue CreateConfig(double threshold)
    {
        auto config = NJson::TJsonValue();
        config.InsertValue("threshold", threshold);
        return config;
    }
}

TEST(TestCostPurchaseChecker, Test_CostPurchaseBase)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "11", 1000);
    pricesResult.Get("11",  1).SkuData.PurchasePrice = 100;
    AddShopSku(pricesResult, 2, "21", 1000);
    pricesResult.Get("21",  2).SkuData.CostPrice = 100;
    pricesResult.Get("21",  2).SkuData.PurchasePrice = 100;
    AddShopSku(pricesResult, 3, "31", 1000);
    AddShopSku(pricesResult, 3, "32", 1000);
    pricesResult.Get("31",  3).SkuData.CostPrice = 100;
    pricesResult.Get("31",  3).SkuData.PurchasePrice = 100;
    pricesResult.Get("32",  3).SkuData.PurchasePrice = 101;
    AddShopSku(pricesResult, 4, "41", 1000);
    AddShopSku(pricesResult, 4, "42", 1000);
    pricesResult.Get("42",  4).SkuData.CostPrice = 100;
    pricesResult.Get("41",  4).SkuData.PurchasePrice = 100;
    pricesResult.Get("42",  4).SkuData.PurchasePrice = 1000;
    AddShopSku(pricesResult, 5, "51", 1000);
    AddShopSku(pricesResult, 5, "52", 1000);
    pricesResult.Get("51",  5).SkuData.CostPrice = 1000;
    pricesResult.Get("51",  5).SkuData.PurchasePrice = 100;
    pricesResult.Get("52",  5).SkuData.PurchasePrice = 1000;

    TCostPurchaseWarning checker(CreateConfig(5.0f));
    TCheckerResult expectedResult = {
        {4, "42", TCheckerError{
            static_cast<ui8>(ECheckerErrorCode::COST_PURCHASE_WARNING),
            "Purchase price is much lower or greater than cost price (PurchasePrice = 1000; CostPrice = 100)"}
        },
        {5, "51", TCheckerError{
            static_cast<ui8>(ECheckerErrorCode::COST_PURCHASE_WARNING),
            "Purchase price is much lower or greater than cost price (PurchasePrice = 100; CostPrice = 1000)"}
        }
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}
