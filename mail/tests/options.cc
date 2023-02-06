#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <yamail/data/reflection/options.h>

namespace {

using namespace yamail::data::reflection;

struct TestOption {};

TEST(findOption, should_return_end_iterator_for_option_not_found) {
    const auto opts = options();
    EXPECT_EQ(findOption<TestOption>(opts), boost::fusion::end(opts));
}

TEST(findOption, should_return_iterator_for_option_found) {
    const auto opts = options(TestOption{});
    EXPECT_TRUE(findOption<TestOption>(opts) != boost::fusion::end(opts));
}

TEST(hasOption, should_return_std_false_type_for_option_not_found) {
    const auto opts = options();
    static_assert(!hasOption<TestOption>(opts));
}

TEST(findOption, should_return_std_true_type_for_option_found) {
    const auto opts = options(TestOption{});
    static_assert(hasOption<TestOption>(opts));
}

}
