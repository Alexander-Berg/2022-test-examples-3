#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "from_string/text.hpp"

#include "common.hpp"

#include <boost/algorithm/string/join.hpp>

namespace std {

inline std::ostream& operator<<(std::ostream& out, const std::vector<std::string>& words) {
    out << boost::algorithm::join(words, " ");
    return out;
}

}

namespace {

using namespace testing;
using namespace rcpt_parser;

struct PhraseTest : ParserTest<types::Words> {};

using WParams = ParserParams<types::Words>;

TEST_P(PhraseTest, no_throw) {
    this->test_parser(&parse_phrase);
}

INSTANTIATE_TEST_SUITE_P(full_consume_no_trim,
        PhraseTest, ::testing::Values(
            WParams{"word", types::Words{"word"}},                         // single atom word
            WParams{"two words"        , types::Words{"two", "words"}},                    // two atom words
            WParams{"\"quoted string\"", types::Words{"quoted string"}},            // single quoted string
            WParams{"atom \"and quoted\" string", types::Words{"atom", "and quoted", "string"}}    // two quoted strings
        )
);

INSTANTIATE_TEST_SUITE_P(full_consume_trim_spaces,
        PhraseTest, ::testing::Values(
                WParams{" word", types::Words{"word"}},
                WParams{"word ", types::Words{"word"}},
                WParams{" first second ", types::Words{"first", "second"}},
                WParams{"\"quo ted\"\"str ing\"", types::Words{"quo ted", "str ing"}}
                                                                // adds space between two quoted strings
        )
);

INSTANTIATE_TEST_SUITE_P(partial_consume,
        PhraseTest, ::testing::Values(
            WParams{"foo\"bar", types::Words{"foo"}   , "\"bar"},  //stops on dquote, excluding
            WParams{"string." , types::Words{"string"}, "."    }   //stops on dot as last symbol, excluding
        )
);

TEST_F(PhraseTest, fails_on_leading_dot) {
    this->test_parser_fails(&parse_phrase, WParams{".string"});
}

}
