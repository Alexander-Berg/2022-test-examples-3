#include "shared.h"
#include <market/dynamic_pricing/deprecated/autostrategy/checker/cheap_against_white_sku.h>

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

// Return error for 2 sku
TEST(TestCheapAgainstWhiteSkuChecker, Test_Cheap)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "1", 80, 50, 2.0, 160, 80, 80);
    AddShopSku(pricesResult, 2, "2", 200, 140, 1.1, 250, 100, 350);
    AddShopSku(pricesResult, 3, "3", 310, 120, 0.2, 300, 200, 400);
    AddShopSku(pricesResult, 4, "4", 100, 120, 4, 150, 50, 670);

    TCheapAgainstWhiteSku checker(CreateConfig(0.8));
    TCheckerResult expectedResult = {
        {1, "1", TCheckerError{static_cast<ui8>(ECheckerErrorCode::CHEAPER_THAN_WHITE), "Price is too low comparing to Yandex.Market: 80 VS 160"}},
        {4, "4", TCheckerError{static_cast<ui8>(ECheckerErrorCode::CHEAPER_THAN_WHITE), "Price is too low comparing to Yandex.Market: 100 VS 150"}}
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}

// Check zero demand handling
TEST(TestCheapAgainstWhiteSkuChecker, Test_ZeroDemand)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "1", 80, 50, 0, 160);
    AddShopSku(pricesResult, 2, "2", 200, 140, 0, 250);
    AddShopSku(pricesResult, 3, "3", 310, 120, 2.2, 300);
    AddShopSku(pricesResult, 4, "4", 100, 120, 0, 150);

    TCheapAgainstWhiteSku checker(CreateConfig(0.8));
    EXPECT_EQ(checker.Check(1, pricesResult).size(), 0);
}

// ignoring fixed prices
TEST(TestCheapAgainstWhiteSkuChecker, Test_FixedPrice)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "1", 80, 50, 2.0, 160, 80, 80);
    AddShopSku(pricesResult, 2, "2", 200, 140, 1.1, 250, 100, 350);
    AddShopSku(pricesResult, 3, "3", 310, 120, 0.2, 300, 200, 400);
    AddShopSku(pricesResult, 4, "4", 100, 120, 4, 150, 50, 670);

    auto config = CreateConfig(0.8);
    config.InsertValue("ignore_fixed", true);

    TCheapAgainstWhiteSku checker(config);
    TCheckerResult expectedResult = {
        {4, "4", TCheckerError{static_cast<ui8>(ECheckerErrorCode::CHEAPER_THAN_WHITE), "Price is too low comparing to Yandex.Market: 100 VS 150"}}
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}

TEST(TestCheapAgainstWhiteSkuChecker, Test_CheapSsku)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "11", 80, 50, 2.0, 160, 80, 80);
    AddShopSku(pricesResult, 1, "12", 80, 50, 2.0, 160, 80, 80);
    AddShopSku(pricesResult, 2, "21", 200, 140, 1.1, 250, 100, 350);
    AddShopSku(pricesResult, 2, "22", 200, 140, 1.1, 250, 100, 350);
    AddShopSku(pricesResult, 3, "31", 310, 120, 0.2, 300, 200, 400);
    AddShopSku(pricesResult, 3, "32", 310, 120, 0.2, 300, 200, 400);

    TCheapAgainstWhiteSku checker(CreateConfig(0.8));
    TCheckerResult expectedResult = {
        {1, "11", TCheckerError{static_cast<ui8>(ECheckerErrorCode::CHEAPER_THAN_WHITE), "Price is too low comparing to Yandex.Market: 80 VS 160"}},
        {1, "12", TCheckerError{static_cast<ui8>(ECheckerErrorCode::CHEAPER_THAN_WHITE), "Price is too low comparing to Yandex.Market: 80 VS 160"}},
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}
