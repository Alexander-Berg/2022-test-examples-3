#include "shared.h"
#include <market/dynamic_pricing/deprecated/autostrategy/checker/empty_bounds_sku.h>

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
TEST(TestEmptyBoundsChecker, Test_DifferentBounds)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "11", 1000);
    pricesResult.Get("11",  1).SkuData.RawLowPrice = 0;
    pricesResult.Get("11",  1).SkuData.RawHighPrice = Max<TPrice>();
    AddShopSku(pricesResult, 2, "21", 1100);
    AddShopSku(pricesResult, 2, "22", 1100);
    pricesResult.Get("21",  2).SkuData.RawLowPrice = 110;
    pricesResult.Get("21",  2).SkuData.RawHighPrice = 1100;
    pricesResult.Get("22",  2).SkuData.RawLowPrice = 0;
    AddShopSku(pricesResult, 3, "31", 1000);
    AddShopSku(pricesResult, 3, "32", 1000);
    pricesResult.Get("31",  3).SkuData.RawLowPrice = 110;
    pricesResult.Get("31",  3).SkuData.RawHighPrice = 1102;
    pricesResult.Get("32",  3).SkuData.RawLowPrice = 110;
    pricesResult.Get("32",  3).SkuData.RawHighPrice = 1110;
    AddShopSku(pricesResult, 4, "41", 1000);
    pricesResult.Get("41",  4).SkuData.RawHighPrice = 5500;

    TEmptyBoundsSku checker(CreateConfig());
    TCheckerResult expectedResult = {
        {1, "11", TCheckerError{
            static_cast<ui8>(ECheckerErrorCode::EMPTY_BOUNDS),
            "Low price or high price are empty"}
        },
        {2, "22", TCheckerError{
            static_cast<ui8>(ECheckerErrorCode::EMPTY_BOUNDS),
            "Low price or high price are empty"}
        },
        {4, "41", TCheckerError{
            static_cast<ui8>(ECheckerErrorCode::EMPTY_BOUNDS),
            "Low price or high price are empty"}
        }
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}
