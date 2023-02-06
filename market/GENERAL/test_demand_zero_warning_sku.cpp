#include "shared.h"
#include <market/dynamic_pricing/deprecated/autostrategy/checker/demand_zero_warning_sku.cpp>

#include <library/cpp/logger/global/global.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NChecker;
using namespace NMarket::NDynamicPricing::NChecker::NTestShared;

namespace {
    NJson::TJsonValue CreateConfig(double demandThreshold)
    {
        auto config = NJson::TJsonValue();
        config.InsertValue("demand_threshold", demandThreshold);
        return config;
    }
}

// Return error on non zero stock
TEST(TestDemandZeroChecker, Test_DemandZero)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "1", 1000, 1200, 0.0, 0, 0, Max<TPrice>(), 0, Max<TPrice>(), Max<TDemand>(), 0);
    AddShopSku(pricesResult, 2, "2", 1000, 1200, 0, 0, 0, Max<TPrice>(), 0, Max<TPrice>(), Max<TDemand>(), 50);
    AddShopSku(pricesResult, 3, "3", 1000, 1200, 0.1, 0, 0, Max<TPrice>(), 0, Max<TPrice>(), Max<TDemand>(), 51);
    AddShopSku(pricesResult, 4, "4", 1000, 1200, 10.1, 0, 0, Max<TPrice>(), 0, Max<TPrice>(), Max<TDemand>(), 555);
    AddShopSku(pricesResult, 5, "5", 1000, 1200, 5, 0, 0, Max<TPrice>(), 0, Max<TPrice>(), Max<TDemand>(), 0);

    TDemandZeroWarningSku checker(CreateConfig(0.1));
    TCheckerResult expectedResult = {
        {2, "2", TCheckerError{static_cast<ui8>(ECheckerErrorCode::DEMAND_ZERO_WARNING), "For not empty stock demand is too low: 0"}},
        {3, "3", TCheckerError{static_cast<ui8>(ECheckerErrorCode::DEMAND_ZERO_WARNING), "For not empty stock demand is too low: 0.1"}},
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}

// ignoring fixed prices
TEST(TestDemandZeroChecker, Test_FixedPrice)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "1", 1000, 1200, 0.0, 0, 0, Max<TPrice>(), 0, Max<TPrice>(), Max<TDemand>(), 0);
    AddShopSku(pricesResult, 2, "2", 1000, 1200, 0, 0, 1200, 1200, Max<TPrice>(), Max<TDemand>(), 50);
    AddShopSku(pricesResult, 3, "3", 1000, 1200, 0.1, 0, 0, Max<TPrice>(), 0, Max<TPrice>(), Max<TDemand>(), 51);
    AddShopSku(pricesResult, 4, "4", 1000, 1200, 10.1, 0, 0, Max<TPrice>(), 0, Max<TPrice>(), Max<TDemand>(), 555);
    AddShopSku(pricesResult, 5, "5", 1000, 1200, 5, 0, 0, Max<TPrice>(), 0, Max<TPrice>(), Max<TDemand>(), 0);

    auto config = CreateConfig(0.1);
    config.InsertValue("ignore_fixed", true);

    TDemandZeroWarningSku checker(config);
        TCheckerResult expectedResult = {
        {3, "3", TCheckerError{static_cast<ui8>(ECheckerErrorCode::DEMAND_ZERO_WARNING), "For not empty stock demand is too low: 0.1"}},
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}

// Return error on non zero stock
TEST(TestDemandZeroChecker, Test_AbcStatus)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "1", 1000, 1200, 0, 0, 0, Max<TPrice>(), 0, Max<TPrice>(), Max<TDemand>(), 50);
    pricesResult.Get("1", 1).SkuData.AbcStatus = EAbcStatus::DeadStock;
    AddShopSku(pricesResult, 2, "2", 1000, 1200, 0, 0, 0, Max<TPrice>(), 0, Max<TPrice>(), Max<TDemand>(), 50);
    pricesResult.Get("2", 2).SkuData.AbcStatus = EAbcStatus::New;
    AddShopSku(pricesResult, 3, "3", 1000, 1200, 0, 0, 0, Max<TPrice>(), 0, Max<TPrice>(), Max<TDemand>(), 50);
    pricesResult.Get("3", 3).SkuData.AbcStatus = Nothing();
    AddShopSku(pricesResult, 4, "4", 1000, 1200, 0, 0, 0, Max<TPrice>(), 0, Max<TPrice>(), Max<TDemand>(), 50);
    pricesResult.Get("4", 4).SkuData.AbcStatus = EAbcStatus::A;
    AddShopSku(pricesResult, 5, "5", 1000, 1200, 0, 0, 0, Max<TPrice>(), 0, Max<TPrice>(), Max<TDemand>(), 50);
    pricesResult.Get("5", 5).SkuData.AbcStatus = EAbcStatus::Unknown;

    TDemandZeroWarningSku checker(CreateConfig(0.1));
    TCheckerResult expectedResult = {
        {3, "3", TCheckerError{static_cast<ui8>(ECheckerErrorCode::DEMAND_ZERO_WARNING), "For not empty stock demand is too low: 0"}},
        {4, "4", TCheckerError{static_cast<ui8>(ECheckerErrorCode::DEMAND_ZERO_WARNING), "For not empty stock demand is too low: 0"}},
        {5, "5", TCheckerError{static_cast<ui8>(ECheckerErrorCode::DEMAND_ZERO_WARNING), "For not empty stock demand is too low: 0"}},
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}
