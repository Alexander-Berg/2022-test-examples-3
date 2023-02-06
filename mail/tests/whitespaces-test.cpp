#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "from_string/whitespaces.hpp"
#include "common.hpp"

namespace {

using namespace testing;
using namespace rcpt_parser;

struct SuccessFWSTest : SParserTest {};

TEST_P(SuccessFWSTest, no_throw) {
    this->test_parser(&parse_fws);
}

INSTANTIATE_TEST_SUITE_P(full_consume_full_result,
        SuccessFWSTest, ::testing::Values(
            " ",     // only whitespace
            "\t"     // only tab
        )
);

INSTANTIATE_TEST_SUITE_P(full_consume_remove_crlf,
        SuccessFWSTest, ::testing::Values(
            SParserParams{" \r\n\t", " \t"},    // spaces and tabs before CRLF
            SParserParams{"\r\n \t", " \t"},    // spaces and tabs around CRLF
            SParserParams{"\r "    , " "},      // only CR (non-RFC but probably useful)
            SParserParams{"\n "    , " "}       // only LF (non-RFC but probably useful)
        )
);

INSTANTIATE_TEST_SUITE_P(partial_consume,
        SuccessFWSTest, ::testing::Values(
            SParserParams{" \r\n"     , " ", "\r\n" },  // stops on trailing CRLF
            SParserParams{"\r\n \r\n ", " ", "\r\n "}   // stops on second CRLF
        )
);

struct FailFWSTest: SParserTest {};

TEST_P(FailFWSTest, fails) {
    this->test_parser_fails(&parse_fws);
}

INSTANTIATE_TEST_SUITE_P(two_crlf,
        FailFWSTest, ::testing::Values(
            "\r\n\r\n ",       // nothing in between CRLF
            "\n\n "            // nothing in between LF+LF
        )
);

INSTANTIATE_TEST_SUITE_P(crlf_only,
        FailFWSTest, ::testing::Values(
            "\r\n",  // just CRLF
            "\r",    // just CRLF
            "\n"     // just CRLF
        )
);

}
