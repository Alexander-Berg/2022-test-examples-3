#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "from_string/address.hpp"

#include "common.hpp"

#include <boost/algorithm/string/join.hpp>

namespace rcpt_parser {
namespace types {

inline std::ostream& operator<<(std::ostream& out, const types::AddrSpec& email) {
    out << "AddrSpec(\"" << email.login << "\", \"" << email.domain << "\")";
    return out;
}

inline std::ostream& operator<<(std::ostream& out, const types::Words& words) {
    out << boost::algorithm::join(words, " ");
    return out;
}

inline std::ostream& operator<<(std::ostream& out, const types::NameAddr& email) {
    static const char* const join_delimiter("\", \"");
    out << "NameAddr(\"";
    if(!email.display_name.empty()) {
        out << email.display_name << join_delimiter;
    }
    out << email.addr_spec << "\")";
    return out;
}

inline std::ostream& operator<<(std::ostream& out, const types::MailboxGroup& group) {
    static const char* const join_delimiter("\", \"");
    out << "MailboxGroup(\"";
    if(!group.display_name.empty()) {
        out << group.display_name << join_delimiter;
    }
    out << "[";
    for(const auto& name_addr : group.group) {
        out << name_addr << ", ";
    }
    out << "]\")";
    return out;
}

}}

namespace {

using namespace testing;
using namespace rcpt_parser;


using Params = ParserParams<types::NameAddr>;

struct SuccessNameAddrTest : ParserTest<types::NameAddr> {};

TEST_P(SuccessNameAddrTest, basic_testcase) {
    this->test_parser(&parse_name_addr);
}

INSTANTIATE_TEST_SUITE_P(full_consume,
        SuccessNameAddrTest, ::testing::Values(
            Params("displayname <login@domain.ru>",
                   types::NameAddr("displayname", types::AddrSpec("login", "domain.ru"))
            ),
            Params("display   \r\n name <login@domain.ru>",
                   types::NameAddr(
                           types::Words{"display", "name"},
                           types::AddrSpec("login", "domain.ru"))
            ),
            Params("spaced display name <smth@smw>",
                   types::NameAddr(
                           types::Words{"spaced", "display", "name"},
                           types::AddrSpec("smth", "smw"))
            ),
            Params("<login@domain.ru>",
                   types::NameAddr(types::AddrSpec("login", "domain.ru"))
            ),
            Params("< \r\n login@domain.ru >",
                   types::NameAddr(types::AddrSpec("login", "domain.ru"))
            )
        )
);

INSTANTIATE_TEST_SUITE_P(partial_consume,
        SuccessNameAddrTest, ::testing::Values(
            Params("displayname <login@domain.ru> \r\n ",
                   types::NameAddr("displayname", types::AddrSpec("login", "domain.ru"))
            ),
            Params("displayname <login@domain.ru>   ",
                   types::NameAddr("displayname", types::AddrSpec("login", "domain.ru"))
            )
        )
);

struct FailNameAddrTest : ParserTest<types::NameAddr> {};

TEST_P(FailNameAddrTest, fails) {
    this->test_parser_fails(&parse_name_addr);
}

INSTANTIATE_TEST_SUITE_P(bad_food,
        FailNameAddrTest, ::testing::Values(
                "<unmatched@angle.bracket",
                "no@angle.brackets",
                "<two<angle@brackets>"
        )
);

struct SuccessMailboxTest : ParserTest<types::NameAddr> {};

TEST_P(SuccessMailboxTest, basic_testcase) {
    this->test_parser(&test_parse_mailbox);
}

INSTANTIATE_TEST_SUITE_P(full_consume,
        SuccessMailboxTest, ::testing::Values(
            Params("displayname <login@domain.ru>",
                   types::NameAddr("displayname", types::AddrSpec("login", "domain.ru"))
            ),
            Params("login@domain.ru",
                   types::NameAddr(types::AddrSpec("login", "domain.ru"))
            )
        )
);


using GParams = ParserParams<types::MailboxGroup>;

struct SuccessGroupTest : ParserTest<types::MailboxGroup> {};

TEST_P(SuccessGroupTest, basic_testcase) {
    this->test_parser(&parse_group);
}

INSTANTIATE_TEST_SUITE_P(full_consume,
        SuccessGroupTest, ::testing::Values(
            GParams("groupname:;",
                    types::MailboxGroup(
                            "groupname",
                            {}
                    )
            ),
            GParams("groupname:login@domain.ru; \r\n ",
                    types::MailboxGroup(
                            "groupname",
                            {types::NameAddr(types::AddrSpec("login", "domain.ru"))}
                    )
            ),
            GParams("groupname:login@domain.ru,displayname <my@email.com>, \"quo ted\" <ip@[127.0.0.1]>;",
                    types::MailboxGroup(
                            "groupname",
                            {
                                    types::NameAddr(               types::AddrSpec("login", "domain.ru")),
                                    types::NameAddr("displayname", types::AddrSpec("my", "email.com")   ),
                                    types::NameAddr("quo ted"    , types::AddrSpec("ip", "[127.0.0.1]") ),
                            }
                    )
            )
        )
);

struct FailGroupTest : ParserTest<types::MailboxGroup> {};

TEST_P(FailGroupTest, fails) {
    this->test_parser_fails(&parse_group);
}

INSTANTIATE_TEST_SUITE_P(bad_food,
        FailGroupTest, ::testing::Values(
                "no colon",
                "no semicolon after colon:",
                "mailbox-list ended with comma:login@yandex.ru,;",
                "double comma::",
                "comma-login-comma:login@yandex.ru:"
        )
);


using AParams = ParserParams<types::Address>;

struct SuccessAddressTest : ParserTest<types::Address> {};

TEST_P(SuccessAddressTest, basic_testcase) {
    this->test_parser(&test_parse_address);
}

INSTANTIATE_TEST_SUITE_P(full_consume,
        SuccessAddressTest, ::testing::Values(
            AParams("displayname <login@domain.ru>",
                    types::NameAddr("displayname", types::AddrSpec("login", "domain.ru"))
            ),
            AParams("groupname:;",
                    types::MailboxGroup("groupname", {})
            )
        )
);


}
