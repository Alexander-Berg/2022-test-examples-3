#include <gtest/gtest.h>

#include <mail/notsolitesrv/src/meta_save_op/types/request.h>

#include <mail/notsolitesrv/tests/unit/util/email_address.h>

using namespace testing;
using namespace NNotSoLiteSrv;
using namespace NNotSoLiteSrv::NMetaSaveOp;

namespace NNotSoLiteSrv {

std::ostream& operator<<(std::ostream& os, const TEmailAddress& email) {
    os << "(\"" << email.DisplayName << "\",\"" << email.Local << "\",\"" << email.Domain << "\")";
    return os;
}

}

TEST(TMetaSaveOpRequest, ToEmailAddressRfc2822) {
    using TAddrPair = std::pair<std::string, std::string>;
    EXPECT_EQ(ToEmailAddress(TAddrPair{"Display", "local@domain"}),
        (TEmailAddress{"local", "domain", "Display"}));
    EXPECT_EQ(ToEmailAddress(TAddrPair{"Display", "only_local_part"}),
        (TEmailAddress{"only_local_part", "", "Display"}));
}

TEST(TMetaSaveOpRequest, ToEmailAddressString) {
    EXPECT_EQ(ToEmailAddress(R"("Display name" <local@domain>)"),
        (TEmailAddress{"local", "domain", "Display name"}));
    EXPECT_EQ(ToEmailAddress(R"(<only_local_part>)"),
        (TEmailAddress{"only_local_part", "", ""}));
    EXPECT_EQ(ToEmailAddress(R"(<only_local_part@>)"),
        (TEmailAddress{"only_local_part", "", ""}));
    EXPECT_EQ(ToEmailAddress(R"(only_local_part)"),
        (TEmailAddress{"only_local_part", "", ""}));
    EXPECT_EQ(ToEmailAddress(R"(Имя Фамилия <local@domain>)"),
        (TEmailAddress{"local", "domain", "Имя Фамилия"}));
    EXPECT_EQ(ToEmailAddress(R"((st) ssart <local@domain>)"),
        (TEmailAddress{"local", "domain", "(st) ssart"}));
    EXPECT_EQ(ToEmailAddress(R"(<Печкин@почта.рф>)"),
        (TEmailAddress{"Печкин", "почта.рф", ""}));

    // mimeparser bug, we should be bug2bug compatible for many reasons
    EXPECT_EQ(ToEmailAddress("<@domain>"), (TEmailAddress{"", "domain", ""}));

    // incorrect email addresses
    EXPECT_EQ(ToEmailAddress("<>"), boost::none);
    EXPECT_EQ(ToEmailAddress(R"("" <>)"), boost::none);

    EXPECT_EQ(ToEmailAddress("<@>"), boost::none);
    EXPECT_EQ(ToEmailAddress(R"("" <@>)"), boost::none);

    EXPECT_EQ(ToEmailAddress(""), boost::none);
    EXPECT_EQ(ToEmailAddress("><"), boost::none);
}

TEST(TMetaSaveOpRequest, ToEmailAddressStringWithManyAddresses) {
    EXPECT_EQ(ToEmailAddress(R"(one <local@domain>, two <local2@domain2>)"),
        (TEmailAddress{"local", "domain", "one"}));
    EXPECT_EQ(ToEmailAddress(R"(<>, two <local2@domain2>)"),
        (TEmailAddress{"local2", "domain2", "two"}));
}

TEST(TMetaSaveOpRequest, FillAddressListRfc2822) {
    std::vector<TEmailAddress> actual;
    std::vector<TEmailAddress> expected{
        {"local", "", ""},
        {"local", "domain", "display"},
        {"печкин", "почта.рф", ""}
    };
    FillAddressList(
        rfc2822::old_rfc2822address(R"(local, display <local@domain>, <печкин@почта.рф>)"),
        std::back_inserter(actual));

    EXPECT_EQ(expected, actual);
}

TEST(TMetaSaveOpRequest, FillAddressListFromStrings) {
    std::vector<TEmailAddress> actual;
    std::vector<TEmailAddress> expected{
        {"local", "", ""},
        {"local", "domain", "display"},
        {"печкин", "почта.рф", ""}
    };
    FillAddressList(
        std::vector<std::string>{"local", "display <local@domain>", "<печкин@почта.рф>"},
        std::back_inserter(actual));

    EXPECT_EQ(expected, actual);
}

TEST(TMetaSaveOpRequest, FillAddressListFromStringsWithIncorrectEmail) {
    std::vector<TEmailAddress> actual;
    std::vector<TEmailAddress> expected{
        {"local", "", ""},
        {"local", "domain", "display"},
        {"печкин", "почта.рф", ""}
    };
    FillAddressList(
        std::vector<std::string>{"local", "display <local@domain>", "><", "<печкин@почта.рф>"},
        std::back_inserter(actual));

    EXPECT_EQ(expected, actual);
}
