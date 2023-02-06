#include <market/dynamic_pricing/pricing/dynamic_pricing/autostrategy/margin_adjusting/bias.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NMarginAdjusting;


// margin = targetMargin + bias + overcomeMargin
TEST(TestBiasAdjuster, Test_BaseCase)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.1;
    TMargin overcomeMargin = 0.01;
    TMargin minMargin = -0.01;
    TMargin maxMargin = 0.2;
    TMargin bias = 0.05;
    auto adjuster = TBiasAdjuster(
        overcomeMargin,
        minMargin,
        maxMargin,
        bias
    );
    TMargin expectedMargin = 0.16;

    EXPECT_DOUBLE_EQ(adjuster.GetAdjusted(targetMargin), expectedMargin);
}

// margin = maxMargin
TEST(TestBiasAdjuster, Test_MarginAboveMax)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.1;
    TMargin overcomeMargin = 0.01;
    TMargin minMargin = -0.01;
    TMargin maxMargin = 0.15;
    TMargin bias = 0.05;
    auto adjuster = TBiasAdjuster(
        overcomeMargin,
        minMargin,
        maxMargin,
        bias
    );
    TMargin expectedMargin = 0.15;

    EXPECT_DOUBLE_EQ(adjuster.GetAdjusted(targetMargin), expectedMargin);
}

TEST(TestBiasAdjuster, Test_MarginBelowMin)
{
    InitGlobalLog2Null();

    auto targetMargin = -0.05;
    TMargin overcomeMargin = 0.01;
    TMargin minMargin = -0.01;
    TMargin maxMargin = 0.15;
    TMargin bias = 0;
    auto adjuster = TBiasAdjuster(
        overcomeMargin,
        minMargin,
        maxMargin,
        bias
    );
    TMargin expectedMargin = -0.01;

    EXPECT_DOUBLE_EQ(adjuster.GetAdjusted(targetMargin), expectedMargin);
}

TEST(TestBiasAdjuster, Test_NegativeOvercome)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.05;
    TMargin overcomeMargin = -0.01;
    TMargin minMargin = -0.1;
    TMargin maxMargin = 0.25;
    TMargin bias = 0.02;
    auto adjuster = TBiasAdjuster(
        overcomeMargin,
        minMargin,
        maxMargin,
        bias
    );
    TMargin expectedMargin = 0.06;

    EXPECT_DOUBLE_EQ(adjuster.GetAdjusted(targetMargin), expectedMargin);
}
