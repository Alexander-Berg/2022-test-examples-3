#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "from_string/atoms.hpp"
#include "common.hpp"

namespace {

using namespace testing;
using namespace rcpt_parser;

struct SuccessDotAtomTest : SParserTest {};

TEST_P(SuccessDotAtomTest, no_throw) {
    this->test_parser(&parse_dot_atom);
}

INSTANTIATE_TEST_SUITE_P(full_consume_no_trim,
        SuccessDotAtomTest, ::testing::Values(
            "!#$%&'*+|=?^_`{|}~-",  // allow specials
            "str.ing"               // allow dot inside word
        )
);

INSTANTIATE_TEST_SUITE_P(partial_consume,
        SuccessDotAtomTest, ::testing::Values(
            SParserParams{"first second", "first" , "second"},  //stops after whitespaces (and trim them)
            SParserParams{"foo\"bar"    , "foo"   , "\"bar" },  //stops on dquote, excluding
            SParserParams{"string."     , "string", "."     }   //stops on dot as last symbol, excluding
        )
);

struct FailDotAtomTest : SParserTest {};

TEST_F(FailDotAtomTest, fails_on_leading_dot) {
    this->test_parser_fails(&parse_dot_atom, SParserParams{".string"});
}

TEST_F(FailDotAtomTest, fails_on_non_ascii) {
    this->test_parser_fails(&parse_dot_atom, SParserParams{"\xd0\xae\xd0\xa2\xd0\xa4"}); //UTF-8 cyrillic
    this->test_parser_fails(&parse_dot_atom, SParserParams{"\xde\xd2\xd4"});             //CP1251 cyrillic
}

}
