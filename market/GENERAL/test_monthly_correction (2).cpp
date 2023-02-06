#include <market/dynamic_pricing/pricing/dynamic_pricing/autostrategy/margin_adjusting/monthly_correction.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NMarginAdjusting;

namespace {
    size_t DAYS_IN_MONTH =  30;
    double THRESHOLD = 1e-6;
}

// Return same margin as we input
TEST(TestMonthlyAdjuster, Test_FirstDayMarginEqual)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.3;
    auto currentMargin = 0.3;
    auto daysFromBegin = 0;
    auto daysToEnd = DAYS_IN_MONTH - daysFromBegin;
    auto adjuster = TMonthlyCorrectionAdjuster(currentMargin, daysFromBegin, daysToEnd);

    EXPECT_DOUBLE_EQ(adjuster.GetAdjusted(targetMargin), targetMargin);
}

// Return same margin as we input
TEST(TestMonthlyAdjuster, Test_FirstDayMarginDiff)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.3;
    auto currentMargin = 0.2;
    auto daysFromBegin = 0;
    auto daysToEnd = DAYS_IN_MONTH - daysFromBegin;
    auto adjuster = TMonthlyCorrectionAdjuster(currentMargin, daysFromBegin, daysToEnd);

    EXPECT_DOUBLE_EQ(adjuster.GetAdjusted(targetMargin), targetMargin);
}

// Return same margin as we input
TEST(TestMonthlyAdjuster, Test_FirstSevenDaysMarginEqual)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.3;
    auto currentMargin = 0.3;
    auto daysFromBegin = 5;
    auto daysToEnd = DAYS_IN_MONTH - daysFromBegin;
    auto adjuster = TMonthlyCorrectionAdjuster(currentMargin, daysFromBegin, daysToEnd);

    EXPECT_DOUBLE_EQ(adjuster.GetAdjusted(targetMargin), targetMargin);
}

// Return same margin as we input
TEST(TestMonthlyAdjuster, Test_FirstSevenDaysMarginDiff)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.3;
    auto currentMargin = 0.2;
    auto daysFromBegin = 5;
    auto daysToEnd = DAYS_IN_MONTH - daysFromBegin;
    auto adjuster = TMonthlyCorrectionAdjuster(currentMargin, daysFromBegin, daysToEnd);

    EXPECT_DOUBLE_EQ(adjuster.GetAdjusted(targetMargin), targetMargin);
}

// Return same margin as we input
TEST(TestMonthlyAdjuster, Test_SeventhDayMarginEqual)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.3;
    auto currentMargin = 0.3;
    auto daysFromBegin = 6;
    auto daysToEnd = DAYS_IN_MONTH - daysFromBegin;
    auto adjuster = TMonthlyCorrectionAdjuster(currentMargin, daysFromBegin, daysToEnd);

    EXPECT_DOUBLE_EQ(adjuster.GetAdjusted(targetMargin), targetMargin);
}

// Return same margin as we input
TEST(TestMonthlyAdjuster, Test_SeventhDayMarginDiff)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.3;
    auto currentMargin = 0.2;
    auto daysFromBegin = 6;
    auto daysToEnd = DAYS_IN_MONTH - daysFromBegin;
    auto adjuster = TMonthlyCorrectionAdjuster(currentMargin, daysFromBegin, daysToEnd);

    EXPECT_DOUBLE_EQ(adjuster.GetAdjusted(targetMargin), targetMargin);
}

// First day with adjusted margin
TEST(TestMonthlyAdjuster, Test_FirstBoundDay)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.3;
    auto currentMargin = 0.2;
    auto daysFromBegin = 7;
    auto daysToEnd = DAYS_IN_MONTH - daysFromBegin;
    double expectedMargin = 0.3304347826086957;
    auto adjuster = TMonthlyCorrectionAdjuster(currentMargin, daysFromBegin, daysToEnd);

    EXPECT_NEAR(adjuster.GetAdjusted(targetMargin), expectedMargin, THRESHOLD);
}

// Base case margin adjusting
TEST(TestMonthlyAdjuster, Test_BaseCase)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.3;
    auto currentMargin = 0.2;
    auto daysToEnd = 15;
    auto daysFromBegin = DAYS_IN_MONTH - daysToEnd;

    double expectedMargin = 0.4;
    auto adjuster = TMonthlyCorrectionAdjuster(currentMargin, daysFromBegin, daysToEnd);

    EXPECT_NEAR(adjuster.GetAdjusted(targetMargin), expectedMargin, THRESHOLD);
}

// Last day with adjusted margin
TEST(TestMonthlyAdjuster, Test_LastBoundDay)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.3;
    auto currentMargin = 0.2;
    auto daysToEnd = 7;
    auto daysFromBegin = DAYS_IN_MONTH - daysToEnd;

    double expectedMargin = 0.628571429;
    auto adjuster = TMonthlyCorrectionAdjuster(currentMargin, daysFromBegin, daysToEnd);

    EXPECT_NEAR(adjuster.GetAdjusted(targetMargin), expectedMargin, THRESHOLD);
}

// Equal to last bound adjusted margin
TEST(TestMonthlyAdjuster, Test_LastSevenDays)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.3;
    auto currentMargin = 0.2;
    auto daysToEnd = 4;
    auto daysFromBegin = DAYS_IN_MONTH - daysToEnd;

    double expectedMargin = 0.628571429;
    auto adjuster = TMonthlyCorrectionAdjuster(currentMargin, daysFromBegin, daysToEnd);

    EXPECT_NEAR(adjuster.GetAdjusted(targetMargin), expectedMargin, THRESHOLD);
}

// Equal to last bound adjusted margin
TEST(TestMonthlyAdjuster, LastDay)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.3;
    auto currentMargin = 0.2;
    auto daysToEnd = 0;
    auto daysFromBegin = DAYS_IN_MONTH - daysToEnd;

    double expectedMargin = 0.628571429;
    auto adjuster = TMonthlyCorrectionAdjuster(currentMargin, daysFromBegin, daysToEnd);

    EXPECT_NEAR(adjuster.GetAdjusted(targetMargin), expectedMargin, THRESHOLD);
}
