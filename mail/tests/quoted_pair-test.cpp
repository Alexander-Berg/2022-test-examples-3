#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "from_string/quoted_pair.hpp"
#include "common.hpp"

namespace {

using namespace testing;
using namespace rcpt_parser;

struct SuccessQPTest : SParserTest {};


TEST_P(SuccessQPTest, no_throw_on_parse) {
    this->test_parser(&parse_quoted_pair);
}

INSTANTIATE_TEST_SUITE_P(strip_leading_backslash,
        SuccessQPTest, ::testing::Values(
            SParserParams{"\\0" , "0" },    // Number
            SParserParams{"\\z" , "z" },    // Letter
            SParserParams{"\\ " , " " },    // space
            SParserParams{"\\\\", "\\"},    // backslash
            SParserParams{"\\\"", "\""}     // dquote
        )
);

}
