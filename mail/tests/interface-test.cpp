#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <recipient_parser/parse.hpp>

#include "common.hpp"

#include <boost/algorithm/string/join.hpp>

namespace rcpt_parser {
namespace types {

inline std::ostream& operator<<(std::ostream& out, const AddrSpec& email) {
    out << "AddrSpec(\"" << email.login << "\", \"" << email.domain << "\")";
    return out;
}

inline std::string joinDisplayName(const Words& words) {
    return boost::algorithm::join(words, " ");
}

inline std::ostream& operator<<(std::ostream& out, const NameAddr& email) {
    static const char* const join_delimiter("\", \"");
    out << "NameAddr(\"";
    if(!email.display_name.empty()) {
        out << joinDisplayName(email.display_name) << join_delimiter;
    }
    out << email.addr_spec << "\")";
    return out;
}

}}

namespace {

using namespace testing;
using namespace rcpt_parser;

struct InterfaceTest : PrinterTest {};


TEST_F(InterfaceTest, address_list) {
    const std::string input("Super Man <man@supermail.com>,Super Woman <woman@supermail.com>");
    auto result = parse_address_list(input);
    ASSERT_TRUE(result);
    auto& vec = *result;
    auto first = boost::get<types::NameAddr>(&vec[0]);
    ASSERT_TRUE(first != nullptr);
    ASSERT_EQ(*first, types::NameAddr(types::Words{"Super", "Man"}, types::AddrSpec("man", "supermail.com")));
    auto second = boost::get<types::NameAddr>(&vec[1]);
    ASSERT_TRUE(second != nullptr);
    ASSERT_EQ(*second, types::NameAddr(types::Words{"Super", "Woman"}, types::AddrSpec("woman", "supermail.com")));
}

}


