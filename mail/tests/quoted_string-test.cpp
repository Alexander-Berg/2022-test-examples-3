#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "from_string/quoted_string.hpp"
#include "common.hpp"

namespace {

using namespace testing;
using namespace rcpt_parser;

struct SuccessQSTest: SParserTest {};

TEST_P(SuccessQSTest, no_throw) {
    this->test_parser(&parse_quoted_string);
}

INSTANTIATE_TEST_SUITE_P(full_consume,
        SuccessQSTest, ::testing::Values(
            SParserParams{"\"abc\"", "abc"},

            SParserParams{"\" a b c \""   , " a b c "},
            SParserParams{"\"\r\n abc \"" , " abc "  },  // take_inner_fws
            SParserParams{"\"a\\\\\\\"b\"", "a\\\"b" }
        )
);

struct FailQSTest: SParserTest {};

TEST_P(FailQSTest, fails) {
    this->test_parser_fails(&parse_quoted_string);
}

INSTANTIATE_TEST_SUITE_P(two_crlf,
        FailQSTest, ::testing::Values(
            "\" ",      // unmatched dquote
            "\"\\\""    // backslash in content
        )
);


}
