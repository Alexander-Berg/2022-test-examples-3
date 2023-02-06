#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <mail/unistat/cpp/include/meters/count_by_condition.h>

using namespace ::testing;
using namespace ::unistat::CountByCondition;


using MatchTestParam = std::tuple<bool, Condition, std::string, std::string>;
class MatchTestFixture : public TestWithParam<MatchTestParam> {
};


TEST_P(MatchTestFixture, shouldMatch) {
    const auto [result, cond, needle, haystack] = GetParam();
    EXPECT_EQ(result, match(cond, needle, haystack));
}

INSTANTIATE_TEST_SUITE_P(
        MatchTests,
        MatchTestFixture,
        Values(
            MatchTestParam{true,  unistat::CountByCondition::Equals, "str", "str"},
            MatchTestParam{false, unistat::CountByCondition::Equals, "str", "another_str"},

            MatchTestParam{true,  unistat::CountByCondition::Contains, "str", "another_str"},
            MatchTestParam{false, unistat::CountByCondition::Contains, "42", "another_str"},

            MatchTestParam{true,  unistat::CountByCondition::AnyStr, "str", "str1"}
        )
);

TEST(Match, shouldThrowWhenConditionInvalid) {
    EXPECT_THROW(match(static_cast<Condition>(42), "", ""), std::invalid_argument);
}
