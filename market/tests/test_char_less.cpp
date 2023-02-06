#include <market/qpipe/qbid/qbidengine/qbid_utils.h>

#include <library/cpp/testing/unittest/gtest.h>


TEST(TCharLess, SquishesSpaces) {
    NQBid::NPrivate::TCharLess charLess;

    EXPECT_EQ(0, charLess.strspcmp("Hello, world!", "Hello,   world!"));
    EXPECT_NE(0, charLess.strspcmp("Hello, world!", "Hello,world!"));
}
