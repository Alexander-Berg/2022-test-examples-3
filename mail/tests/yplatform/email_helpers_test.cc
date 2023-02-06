#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/hound/include/internal/wmi/yplatform/helpers/email.h>

namespace {

using namespace ::testing;

TEST(YplatformHelpersEmailTest, setDisplayName_emptyDN_joinsLocalAndDomain) {
    auto good = [](const std::string& s){ return s; };
    hound::helpers::Email email("l", "d", "");
    hound::helpers::details::setDisplayName(email, "", good);
    ASSERT_EQ("l@d", email.displayName);
}

TEST(YplatformHelpersEmailTest, setDisplayName_goodDN_setsDN) {
    auto good = [](const std::string& s){ return s; };
    hound::helpers::Email email("l", "d", "");
    hound::helpers::details::setDisplayName(email, "dn", good);
    ASSERT_EQ("dn", email.displayName);
}

TEST(YplatformHelpersEmailTest, setDisplayName_badDN_joinsLocalAndDomain) {
    auto bad = [](const std::string&)->std::string { throw std::exception(); };
    hound::helpers::Email email("l", "d", "");
    hound::helpers::details::setDisplayName(email, "bad", bad);
    ASSERT_EQ("l@d", email.displayName);
}

} //anonimous namespace
