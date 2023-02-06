#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "from_string/addr_spec.hpp"

#include "common.hpp"

namespace rcpt_parser {
namespace types {

inline std::ostream& operator<<(std::ostream& out, const AddrSpec& email) {
    out << "AddrSpec(\"" << email.login << "\", \"" << email.domain << "\")";
    return out;
}

}
}

namespace {

using namespace testing;
using namespace rcpt_parser;

struct SuccessLocalPartTest : SParserTest {};

TEST_P(SuccessLocalPartTest, ok) {
    this->test_parser(&parse_local_part);
}

INSTANTIATE_TEST_SUITE_P(full_consume,
        SuccessLocalPartTest, ::testing::Values(
                "str.ing",              // dot-atom
                "\" quoted string \"",  // quoted-string, double quotes remain
                "\" qs \\\" with qp \"" // quoted-string with inner quote in quoted-pair
        )
);



struct SuccessDomainTest : SParserTest {};

TEST_P(SuccessDomainTest, ok) {
    this->test_parser(&parse_domain);
}

INSTANTIATE_TEST_SUITE_P(full_consume,
        SuccessDomainTest, ::testing::Values(
                "str.ing",          // dot-atom
                "[.literal]",       // address literal, even with leading dot
                "[ \t spaced ]"     // address literal with fws
        )
);

INSTANTIATE_TEST_SUITE_P(full_consume_part_result,
        SuccessDomainTest, ::testing::Values(
                SParserParams{"\r\n [with_cfws]", "[with_cfws]" }, // trims outer cfws
                SParserParams{"[with_cfws] "    , "[with_cfws]" }, // trims outer cfws
                SParserParams{"[ \r\n with_fws]", "[  with_fws]"}
        )
);

INSTANTIATE_TEST_SUITE_P(partial_consume,
        SuccessDomainTest, ::testing::Values(
                SParserParams{"[first] second"   , "[first]", "second"}, // stops after cfws
                SParserParams{"[word] \r\n \r\n ", "[word]" , "\r\n " } // do not accept second cfws
        )
);

struct FailDomainTest : SParserTest {};

TEST_P(FailDomainTest, fails) {
    this->test_parser_fails(&parse_domain);
}

INSTANTIATE_TEST_SUITE_P(bad_food,
        FailDomainTest, ::testing::Values(
                "[unmatched",               // unmatched square brace
                "[backslash\\]",            // backslash
                "[square[brace]",           // square brace
                "\r\n \r\n [double cwfs]",  // double cfws
                "[ \r\n \r\n double fws]"   // double cfws
        )
);


using Params = ParserParams<types::AddrSpec>;

struct SuccessAddrSpecTest : ParserTest<types::AddrSpec> {};

TEST_P(SuccessAddrSpecTest, basic_testcase) {
    this->test_parser(&parse_addr_spec);
}


INSTANTIATE_TEST_SUITE_P(full_consume,
        SuccessAddrSpecTest, ::testing::Values(
            Params("login@domain.ru", types::AddrSpec("login", "domain.ru")),
            Params("login@[127.0.0.1]", types::AddrSpec("login", "[127.0.0.1]"))
        )
);

struct FailAddrSpecTest : ParserTest<types::AddrSpec> {};

TEST_P(FailAddrSpecTest, fails) {
    this->test_parser_fails(&parse_addr_spec);
}

INSTANTIATE_TEST_SUITE_P(bad_food,
        FailAddrSpecTest, ::testing::Values(
                "no_at",
                "domain_is@.bad_dot_atom_not_in_square_braces",
                "unquoted spaces in local part@domain.ru"
        )
);


}
