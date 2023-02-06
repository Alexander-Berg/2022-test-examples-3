#include "shared.h"
#include <market/dynamic_pricing/deprecated/autostrategy/checker/out_off_stock_sku.h>

#include <library/cpp/logger/global/global.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NChecker;
using namespace NMarket::NDynamicPricing::NChecker::NTestShared;

namespace {
    NJson::TJsonValue CreateConfig()
    {
        auto config = NJson::TJsonValue();
        return config;
    }
}

// Return error for 3 sku
TEST(TestOutOffStockChecker, Test_HighDemand)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "1", 1000, 1251, 10, 1000, 0, 10000, 0, 10000, 7);
    AddShopSku(pricesResult, 2, "2", 52, 50, 0.2, 52, 0, 100000, 0, 10000, 1);
    AddShopSku(pricesResult, 3, "3", 500, 650, 1.3, 500, 0, 10000, 0, 10000, 1.2);
    AddShopSku(pricesResult, 4, "4", 800, 1200, 7.6, 800, 0, 100000, 0, 10000, 4.1);

    TOutOffStockSku checker(CreateConfig());
    TCheckerResult expectedResult = {
        {1, "1", TCheckerError{static_cast<ui8>(ECheckerErrorCode::OUT_OFF_STOCK), "Demand is too high, possible out off stock (10 > 7)"}},
        {3, "3", TCheckerError{static_cast<ui8>(ECheckerErrorCode::OUT_OFF_STOCK), "Demand is too high, possible out off stock (1.3 > 1.2)"}},
        {4, "4", TCheckerError{static_cast<ui8>(ECheckerErrorCode::OUT_OFF_STOCK), "Demand is too high, possible out off stock (7.6 > 4.1)"}}
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}

// ignoring fixed prices
TEST(TestOutOffStockChecker, Test_FixedPrice)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "1", 1000, 1251, 10, 1000, 0, 10000, 0, 10000, 7);
    AddShopSku(pricesResult, 2, "2", 52, 50, 0.2, 52, 0, 100000, 0, 10000, 1);
    AddShopSku(pricesResult, 3, "3", 500, 650, 1.3, 500, 0, 10000, 0, 10000, 1.2);
    AddShopSku(pricesResult, 4, "4", 800, 1200, 7.6, 800, 800, 800, 0, 10000, 4.1);

    auto config = CreateConfig();
    config.InsertValue("ignore_fixed", true);

    TOutOffStockSku checker(config);
    TCheckerResult expectedResult = {
        {1, "1", TCheckerError{static_cast<ui8>(ECheckerErrorCode::OUT_OFF_STOCK), "Demand is too high, possible out off stock (10 > 7)"}},
        {3, "3", TCheckerError{static_cast<ui8>(ECheckerErrorCode::OUT_OFF_STOCK), "Demand is too high, possible out off stock (1.3 > 1.2)"}}
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}
