#include "loop_timer.h"

#include <library/cpp/testing/gtest/gtest.h>

namespace NPlutonium {
    TEST(TLoopTimerSuite, CheckAndUpdateLastTrigger) {
        TLoopTimer timer(TDuration::MilliSeconds(1));
        EXPECT_TRUE(timer.CheckAndUpdateLastTrigger());
        EXPECT_FALSE(timer.CheckAndUpdateLastTrigger());
        Sleep(TDuration::MilliSeconds(1));
        EXPECT_TRUE(timer.CheckAndUpdateLastTrigger());
    }

    TEST(TLoopTimerSuite, CheckAndUpdateLastTrigger2) {
        TLoopTimer timer(TDuration::Seconds(10), TDuration::Zero(), TInstant::Seconds(1));
        EXPECT_TRUE(timer.CheckAndUpdateLastTrigger(TInstant::Seconds(5)));
        EXPECT_FALSE(timer.CheckAndUpdateLastTrigger(TInstant::Seconds(5)));
        EXPECT_FALSE(timer.CheckAndUpdateLastTrigger(TInstant::Seconds(6)));
        EXPECT_FALSE(timer.CheckAndUpdateLastTrigger(TInstant::Seconds(10)));
        EXPECT_TRUE(timer.CheckAndUpdateLastTrigger(TInstant::Seconds(11)));
    }

    TEST(TLoopTimerSuite, Triggered_SetLastTrigger) {
        TLoopTimer timer(TDuration::MilliSeconds(1));
        Y_ENSURE(timer.Triggered());
        timer.SetLastTrigger();
        EXPECT_FALSE(timer.Triggered());
        Sleep(TDuration::MilliSeconds(1));
        EXPECT_TRUE(timer.Triggered());
    }

    TEST(TLoopTimerSuite, GetIntervalLimits) {
        TLoopTimer timer(TDuration::Seconds(10), TDuration::Zero(), TInstant::Seconds(3));

        auto l1 = timer.GetIntervalLimits(TInstant::Seconds(2));
        EXPECT_EQ(l1.first, TInstant::Zero());
        EXPECT_EQ(l1.second, TInstant::Seconds(3));

        auto l2 = timer.GetIntervalLimits(TInstant::Seconds(10));
        EXPECT_EQ(l2.first, TInstant::Seconds(3));
        EXPECT_EQ(l2.second, TInstant::Seconds(13));

        auto l3 = timer.GetIntervalLimits(TInstant::Seconds(23));
        EXPECT_EQ(l3.first, TInstant::Seconds(23));
        EXPECT_EQ(l3.second, TInstant::Seconds(33));
    }

    TEST(TLoopTimerSuite, GetPreviousIntervalLimits) {
        TLoopTimer timer(TDuration::Seconds(10), TDuration::Zero(), TInstant::Seconds(3));
        auto l1 = timer.GetPreviousIntervalLimits(TInstant::Seconds(30));
        EXPECT_EQ(l1.first, TInstant::Seconds(13));
        EXPECT_EQ(l1.second, TInstant::Seconds(23));
    }

    TEST(TLoopTimerSuite, RandomizePhase) {
        EXPECT_EQ(RandomizePhase(TInstant::Seconds(33), TDuration::Zero()), TInstant::Seconds(33));

        auto phase = RandomizePhase(TInstant::Seconds(43), TDuration::Seconds(10));
        EXPECT_TRUE(TInstant::Seconds(43) <= phase);
        EXPECT_TRUE(phase < TInstant::Seconds(53));
    }

    TEST(TLoopTimerSuite, IsTooEarlyEmptyMinPeriod) {
        TLoopTimer timer(TDuration::Seconds(10));
        timer.SetLastTrigger();
        EXPECT_FALSE(timer.IsTooEarly());
    }

    TEST(TLoopTimerSuite, IsTooEarlyEmpty) {
        TLoopTimer timer(TDuration::Seconds(10), TDuration::MilliSeconds(1));
        timer.SetLastTrigger();
        EXPECT_TRUE(timer.IsTooEarly());
        Sleep(TDuration::MilliSeconds(1));
        EXPECT_FALSE(timer.IsTooEarly());
    }
}
