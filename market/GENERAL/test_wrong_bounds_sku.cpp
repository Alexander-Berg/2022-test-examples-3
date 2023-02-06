#include "shared.h"
#include <market/dynamic_pricing/deprecated/autostrategy/checker/wrong_bounds_sku.h>

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

    TString Notification = "Полученная цена меньше глобальной нижней границы, установлена цена = нижней границе";
}

// Return error for 3 sku
TEST(TestWrongBoundsChecker, Test_LowBoundAboveHighBound)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "11", 1000, 1251, 10, 1000, 1000, 1000, 1005, 1000);
    AddShopSku(pricesResult, 2, "21", 52, 50, 0.2, 52, 0, 100000, 0, 10000);
    AddShopSku(pricesResult, 2, "22", 52, 50, 0.2, 52, 0, 100000, 0, 10000);
    AddShopSku(pricesResult, 3, "31", 500, 650, 1.3, 590, 500, 500, 500, 590);
    AddShopSku(pricesResult, 3, "32", 500, 650, 1.3, 500, 590, 590, 590, 500);
    AddShopSku(pricesResult, 4, "41", 800, 1200, 7.6, 800, 800, 800, 850, 800);
    AddShopSku(pricesResult, 4, "42", 800, 1200, 7.6, 800, 800, 800, 850, 800);

    TWrongBoundsSku checker(CreateConfig());
    TCheckerResult expectedResult = {
        {1, "11", TCheckerError{
            static_cast<ui8>(ECheckerErrorCode::WRONG_BOUNDS),
            "Low price is above high price (1005 > 1000)",
            Notification}
        },
        {3, "32", TCheckerError{
            static_cast<ui8>(ECheckerErrorCode::WRONG_BOUNDS),
            "Low price is above high price (590 > 500)",
            Notification}
        },
        {4, "41", TCheckerError{
            static_cast<ui8>(ECheckerErrorCode::WRONG_BOUNDS),
            "Low price is above high price (850 > 800)",
            Notification}
        },
        {4, "42", TCheckerError{
            static_cast<ui8>(ECheckerErrorCode::WRONG_BOUNDS),
            "Low price is above high price (850 > 800)",
            Notification}
        }
    };
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}

// ignoring fixed prices
TEST(TestWrongBoundsChecker, Test_FixedPrice)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "1", 1000, 1251, 10, 1000, 1000, 1000, 1005, 1000);
    AddShopSku(pricesResult, 2, "2", 52, 50, 0.2, 52, 0, 100000, 0, 10000);
    AddShopSku(pricesResult, 3, "3", 500, 650, 1.3, 590, 590, 590, 590, 500);
    AddShopSku(pricesResult, 4, "4", 800, 1200, 7.6, 800, 800, 800, 850, 800);

    auto config = CreateConfig();
    config.InsertValue("ignore_fixed", true);
    TWrongBoundsSku checker(config);
    TCheckerResult expectedResult = {};
    auto checkResult = checker.Check(1, pricesResult);

    EXPECT_RANGE_EQ(checkResult, expectedResult);
}
