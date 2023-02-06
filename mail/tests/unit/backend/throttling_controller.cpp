#include <backend/throttling_controller.h>
#include <common/chrono.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

using namespace yimap;
using namespace yimap::backend;

#define TIMEPOINT_DELTA FloatSeconds(0.001)
#define ASSERT_CHRONO_NEAR(val1, val2)                                                             \
    {                                                                                              \
        auto diff = std::chrono::abs((val1) - (val2));                                             \
        ASSERT_LE(diff, duration_cast<decltype(diff)>(TIMEPOINT_DELTA));                           \
    }

struct ThrottlingTest : ::testing::Test
{
    ThrottlingControllerPtr controller;
    ThrottlingSettings settings;

    size_t limit = 100;
    Duration windowLength = 10ms;
    int maxDelayMultiplier = 10;

    int maxDelayLimit()
    {
        return limit * maxDelayMultiplier;
    }

    ThrottlingTest()
    {
        settings.limit = limit;
        settings.windowLength = windowLength;
        settings.maxDelay = windowLength * maxDelayMultiplier;
        controller = std::make_shared<ThrottlingController>(settings);
    }
};

TEST_F(ThrottlingTest, fullLimitOnCreation)
{
    ASSERT_EQ(controller->limit(), limit);
}

TEST_F(ThrottlingTest, zeroDelayOnFullLimit)
{
    ASSERT_EQ(controller->recoveryDelay(), Milliseconds(0));
}

TEST_F(ThrottlingTest, limitsCalculatesCorrectly)
{
    controller->consume(limit - 1);
    ASSERT_EQ(controller->limit(), 1);
}

TEST_F(ThrottlingTest, zeroDelayOnNonZeroLimit)
{
    controller->consume(limit - 1);
    ASSERT_EQ(controller->recoveryDelay(), Milliseconds(0));
}

TEST_F(ThrottlingTest, zeroLimit)
{
    controller->consume(limit);
    ASSERT_EQ(controller->limit(), 0);
}

TEST_F(ThrottlingTest, nonZeroDelayOnZeroLimit)
{
    controller->consume(limit);
    ASSERT_CHRONO_NEAR(controller->recoveryDelay(), windowLength);
}

TEST_F(ThrottlingTest, limitRemainsZeroAfterConsumingMoreThanLimit)
{
    controller->consume(limit * 2);
    ASSERT_EQ(controller->limit(), 0);
}

TEST_F(ThrottlingTest, delayGrowsLinearlyAfterLimit)
{
    controller->consume(limit * 2);
    ASSERT_CHRONO_NEAR(controller->recoveryDelay(), windowLength * 2);
}

TEST_F(ThrottlingTest, delayNeverExceedMaxDelay)
{
    controller->consume(maxDelayLimit() * 10);
    ASSERT_CHRONO_NEAR(controller->recoveryDelay(), settings.maxDelay);
}

TEST_F(ThrottlingTest, limitResetsAfterDelay)
{
    controller->consume(limit * 2);
    std::this_thread::sleep_for(controller->recoveryDelay());
    ASSERT_EQ(controller->limit(), limit);
}
