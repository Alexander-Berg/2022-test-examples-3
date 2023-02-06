#include <gtest/gtest.h>

#include <src/services/db/utils.hpp>

namespace {

using namespace testing;

using collie::services::db::expectSingleRow;

struct TestLogicDbExpectSingleRow : Test {};

TEST_F(TestLogicDbExpectSingleRow, for_range_with_single_element_should_return_this_element) {
    EXPECT_EQ(expectSingleRow(std::vector<int>({42})), 42);
}

TEST_F(TestLogicDbExpectSingleRow, for_range_with_more_than_one_element_should_throw_exception) {
    EXPECT_THROW(expectSingleRow(std::vector<int>({42, 13})), std::runtime_error);
}

TEST_F(TestLogicDbExpectSingleRow, for_empty_range_should_throw_exception) {
    EXPECT_THROW(expectSingleRow(std::vector<int>({})), std::runtime_error);
}

} // namespace
