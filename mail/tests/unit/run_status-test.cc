#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/service_control/run_status.h>

using namespace testing;
using namespace doberman::service_control;

struct RunStatusTest : Test {
    RunStatus status;
};

TEST_F(RunStatusTest, testStatusBeforeReset) {
    EXPECT_TRUE(status);
}

TEST_F(RunStatusTest, testStatusAfterReset) {
    status.reset();
    EXPECT_FALSE(status);
}
