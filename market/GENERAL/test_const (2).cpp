#include <market/dynamic_pricing/pricing/dynamic_pricing/autostrategy/margin_adjusting/const.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NMarginAdjusting;


// Return same margin as initialized for const adjuster
TEST(TestTrivialAdjuster, Test_Const)
{
    InitGlobalLog2Null();

    auto adjTargetMargin = 0.3;
    auto adjuster = TConstAdjuster(adjTargetMargin);
    auto targetMargin = 0.2;

    EXPECT_DOUBLE_EQ(adjuster.GetAdjusted(targetMargin), adjTargetMargin);
}
