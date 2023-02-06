#include <market/dynamic_pricing/pricing/dynamic_pricing/autostrategy/margin_adjusting/bonus_coupon.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NMarginAdjusting;


// Return same margin as we input
TEST(TestBonusCouponAdjuster, Test_ZeroGmv)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.3;
    TMoney gmv = 0;
    TMoney bonus = 0;
    auto adjuster = TBonusCouponAdjuster(gmv, bonus);

    EXPECT_DOUBLE_EQ(adjuster.GetAdjusted(targetMargin), targetMargin);
}

// Return margin according to bonuses
TEST(TestBonusCouponAdjuster, Test_WithBonus)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.3;
    TMoney gmv = 1000;
    TMoney bonus = 200;
    auto expectedMargin = 0.5;
    auto adjuster = TBonusCouponAdjuster(gmv, bonus);

    EXPECT_DOUBLE_EQ(adjuster.GetAdjusted(targetMargin), expectedMargin);
}

// Return same margin as we input
TEST(TestBonusCouponAdjuster, Test_ZeroBonus)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.3;
    TMoney gmv = 1000;
    TMoney bonus = 0;
    auto adjuster = TBonusCouponAdjuster(gmv, bonus);

    EXPECT_DOUBLE_EQ(adjuster.GetAdjusted(targetMargin), targetMargin);
}
