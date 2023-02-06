#include <common/helpers/helpers.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

TEST(HELPERS, makePoweredByTwo)
{
    EXPECT_EQ(1024, yimap::makePoweredByTwo(555));
    EXPECT_EQ(1024, yimap::makePoweredByTwo(1024));
    EXPECT_EQ(4096, yimap::makePoweredByTwo(3000));
    EXPECT_EQ(2048, yimap::makePoweredByTwo(2048));
    EXPECT_EQ(2048, yimap::makePoweredByTwo(1024, 11));
}