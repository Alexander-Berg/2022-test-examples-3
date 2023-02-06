#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <io_result/hooks.h>

using namespace io_result;

using Opt = Optional<int>;

TEST(optionalTest, empty_optional_test) {
    Opt opt;

    EXPECT_FALSE(opt);
    EXPECT_FALSE(opt.isInitialized());
    EXPECT_EQ(opt, Opt());

    EXPECT_EQ(opt.getValueOr(0), 0);

    opt = 42;
    EXPECT_TRUE(opt);
    EXPECT_EQ(opt, 42);
}

TEST(optionalTest, non_empty_optional_test) {
    Opt opt(42);

    EXPECT_TRUE(opt);
    EXPECT_TRUE(opt.isInitialized());
    EXPECT_NE(opt, Opt());
    EXPECT_EQ(opt, 42);
    EXPECT_EQ(*opt, 42);
    EXPECT_EQ(opt.getValueOr(0), 42);

    opt = boost::make_optional(10);
    EXPECT_EQ(opt, 10);

    opt = boost::none;
    EXPECT_FALSE(opt);
}

TEST(optionalTest, flatten_test) {
    Optional<boost::optional<int>> opt;
    EXPECT_FALSE(opt);
    EXPECT_FALSE(opt.flatten());

    opt = makeOptional(boost::optional<int>());
    EXPECT_TRUE(opt);
    EXPECT_FALSE(opt.flatten());

    opt = makeOptional(boost::make_optional(42));
    EXPECT_EQ(opt.flatten(), 42);
}
