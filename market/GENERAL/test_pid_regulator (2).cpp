#include <market/dynamic_pricing/pricing/dynamic_pricing/autostrategy/margin_adjusting/pid_regulator.h>

#include <library/cpp/logger/global/global.h>
#include <library/cpp/testing/unittest/gtest.h>

using namespace NMarket::NDynamicPricing;
using namespace NMarket::NDynamicPricing::NMarginAdjusting;

namespace {
    TMargin HISTORY[3] = { 0.2, 0.3, 0.25 };
}

// Return margin without pid correction
TEST(TestPidRegulator, Test_ZeroPidCorrection)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.3;
    TMaybe<TMargin> previousTarget = 0.2;
    double kp = 0;
    double ki = 0;
    double kd = 0;
    auto adjuster = TPidRegulatorAdjuster(kp, ki, kd, previousTarget, HISTORY);

    EXPECT_DOUBLE_EQ(adjuster.GetAdjusted(targetMargin), *previousTarget);
}

// Return PID adjusted margin
TEST(TestPidRegulator, Test_BaseCase)
{
    InitGlobalLog2Null();

    auto targetMargin = 0.3;
    TMaybe<TMargin> previousTarget = 0.2;
    TMargin expectedMargin = 0.275;
    double kp = 0.5;
    double ki = -0.5;
    double kd = -0.5;
    auto adjuster = TPidRegulatorAdjuster(kp, ki, kd, previousTarget, HISTORY);

    EXPECT_DOUBLE_EQ(adjuster.GetAdjusted(targetMargin), expectedMargin);
}

