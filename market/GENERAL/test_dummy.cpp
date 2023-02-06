#include "shared.h"
#include <market/dynamic_pricing/deprecated/autostrategy/checker/dummy.h>

#include <library/cpp/logger/global/global.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NChecker;
using namespace NMarket::NDynamicPricing::NChecker::NTestShared;


// Return same empty output
TEST(TestDummyChecker, Test_Dummy)
{
    InitGlobalLog2Null();

    TPricesResult pricesResult;
    AddShopSku(pricesResult, 1, "1", 10);
    AddShopSku(pricesResult, 2, "2", 20);
    AddShopSku(pricesResult, 3, "3", 30);

    TDummyChecker checker;
    EXPECT_EQ(checker.Check(1, pricesResult).size(), 0);
}
