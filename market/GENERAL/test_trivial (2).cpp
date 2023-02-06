#include <market/dynamic_pricing/pricing/dynamic_pricing/autostrategy/margin_adjusting/trivial.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NMarginAdjusting;


// Return same margin as we input
TEST(TestTrivialAdjuster, Test_Trivial)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.3;
    auto adjuster = TTrivialAdjuster();

    EXPECT_DOUBLE_EQ(adjuster.GetAdjusted(targetMargin), targetMargin);
}
