#include <gtest/gtest.h>
#include <mail/unistat/cpp/include/meters/supervisor_meters.h>

using namespace ::testing;
using namespace ::unistat;

TEST(SupervisorLogRestartMeters, shouldHasNotValueJustAfterInit) {
    SupervisorLogRestartMeters meter("prefix");
    EXPECT_TRUE(meter.get().empty());
}

TEST(SupervisorLogRestartMeters, shouldNotIncrementForEmptyLine) {
    SupervisorLogRestartMeters meter("prefix");
    meter.update("");
    EXPECT_TRUE(meter.get().empty());
}

TEST(SupervisorLogRestartMeters, shouldNotIncrementForUnsuitableString) {
    SupervisorLogRestartMeters meter("prefix");
    meter.update("2019-09-23 15:04:29,124 INFO success: sendbernar entered RUNNING state, process has stayed up for > than 3 seconds (startsecs)");
    EXPECT_TRUE(meter.get().empty());
}

TEST(SupervisorLogRestartMeters, shouldIncrementForStringWithUnexpectedExit) {
    SupervisorLogRestartMeters meter("prefix");
    meter.update("2019-09-23 15:23:01,692 INFO exited: sendbernar (terminated by SIGSEGV (core dumped); not expected)");
    EXPECT_FALSE(meter.get().empty());
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{{"prefix_sendbernar_terminated_by_sigsegv_core_dumped_summ", 1ul}}));
}

TEST(SupervisorLogRestartMeters, shouldIncrementForStringWithNormalExit) {
    SupervisorLogRestartMeters meter("prefix");
    meter.update("2019-10-01 01:10:22,653 INFO stopped: mops (exit status 0)");
    EXPECT_FALSE(meter.get().empty());
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{{"prefix_mops_exit_status_0_summ", 1ul}}));
}

TEST(SupervisorLogRestartMeters, shouldIncrementForServiceNameWithMinus) {
    SupervisorLogRestartMeters meter("prefix");
    meter.update("2019-10-01 01:10:22,653 INFO stopped: push-client-main (exit status 0)");
    EXPECT_FALSE(meter.get().empty());
    EXPECT_EQ(meter.get(), (std::vector<NamedValue<std::size_t>>{{"prefix_push-client-main_exit_status_0_summ", 1ul}}));
}
