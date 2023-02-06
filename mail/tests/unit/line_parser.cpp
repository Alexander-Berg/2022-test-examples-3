#include <gtest/gtest.h>

#include <parser/line.h>

#include <boost/algorithm/string.hpp>

using namespace ymod_smtpserver;
using namespace testing;

struct TParseLineCheckSuccess : TestWithParam<std::tuple<std::string, std::string>> {
};

TEST_P(TParseLineCheckSuccess, ParseShouldEndWithSuccess) {
    std::string str = std::get<0>(GetParam());
    std::string shouldBe = std::get<1>(GetParam());
    auto lineEnd = str.begin();
    ASSERT_TRUE(parser::parse_line(lineEnd, str.end()));
    ASSERT_EQ(shouldBe, std::string(str.begin(), lineEnd));
}

INSTANTIATE_TEST_SUITE_P(ParseLineCheckSuccess, TParseLineCheckSuccess,
    Values(
        std::make_tuple("FooBar\r\n", "FooBar\r\n"),
        std::make_tuple("   \t FooBar   \r\n  Buzz", "   \t FooBar   \r\n"),
        std::make_tuple("FooBar\n", "FooBar\n"),
        std::make_tuple("FooBar\r\nZooBar\r\n", "FooBar\r\n"),
        std::make_tuple("FooBar\n ZooBar\n", "FooBar\n"),
        std::make_tuple("FooBar \t\nZooBar\n", "FooBar \t\n"),
        std::make_tuple("\r\nFooBar", "\r\n"),
        std::make_tuple("\nFooBar", "\n")
    )
);

struct TParseLineCheckFail : TestWithParam<std::string> {
};

TEST_P(TParseLineCheckFail, ParseShouldEndWithFail) {
    std::string str = GetParam();
    auto lineEnd = str.begin();
    ASSERT_FALSE(parser::parse_line(lineEnd, str.end()));
}

INSTANTIATE_TEST_SUITE_P(ParseLineCheckFail, TParseLineCheckFail,
    Values(
        "FooBar",
        "   \tFooBar      ",
        "FooBar\tZoo",
        "FooBar\r",
        "\rFooBar\rZooBar"
    )
);
