#include <internal/mail/registration.h>

#include <gtest/gtest.h>

namespace sharpei {
namespace mail {
namespace registration {
namespace detail {

bool operator ==(const UserInfo& lhs, const UserInfo& rhs) {
    return lhs.country == rhs.country
        && lhs.lang == rhs.lang
        && lhs.domainId == rhs.domainId
        && lhs.hasMailishAlias == rhs.hasMailishAlias;
}

std::ostream& operator <<(std::ostream& stream, const UserInfo& value) {
    return stream << "sharpei::mail::registration::detail::UserInfo {"
                  << '"' << value.country << "\", "
                  << '"' << value.lang << "\", "
                  << value.domainId << ", "
                  << value.hasMailishAlias
                  << "}";
}

namespace tests {

using namespace testing;

struct ParseUserInfoTest : public Test {};

using Response = yhttp::response;

TEST(ParseUserInfoTest, with_http_code_not_200_should_return_http_error) {
    ExplainedError ec;
    UserInfo userInfo;
    std::tie(ec, userInfo) = parseUserInfo(Response {500, {}, "", ""});
    EXPECT_EQ(ec, ExplainedError(Error::blackBoxHttpError));
}

TEST(ParseUserInfoTest, with_invalid_blackbox_response_should_return_fatal_error) {
    ExplainedError ec;
    UserInfo userInfo;
    std::tie(ec, userInfo) = parseUserInfo(Response {200, {}, "", ""});
    EXPECT_EQ(ec, ExplainedError(Error::blackBoxFatalError));
}

TEST(ParseUserInfoTest, with_empty_uid_in_blackbox_response_should_return_uid_not_found_error) {
    const std::string body =
        R"xml(<?xml version="1.0" encoding="UTF-8"?>
            <doc>
                <uid hosted="0"></uid>
                <karma confirmed="0">0</karma>
                <karma_status>0</karma_status>
            </doc>
        )xml";
    ExplainedError ec;
    UserInfo userInfo;
    std::tie(ec, userInfo) = parseUserInfo(Response {200, {}, body, ""});
    EXPECT_EQ(ec, ExplainedError(Error::uidNotFound));
}

TEST(ParseUserInfoTest, without_domain_id_in_blackbox_response_should_return_zero_domain_id) {
    const std::string body =
        R"xml(<?xml version="1.0" encoding="UTF-8"?>
            <doc>
                <uid hosted="0">13</uid>
                <login>login</login>
                <have_password>1</have_password>
                <have_hint>1</have_hint>
                <karma confirmed="0">0</karma>
                <karma_status>0</karma_status>
                <dbfield id="account_info.country.uid">country</dbfield>
                <dbfield id="userinfo.lang.uid">lang</dbfield>
                <dbfield id="subscription.suid.2">42</dbfield>
                <dbfield id="hosts.db_id.2">pg</dbfield>"
                <aliases>
                    <alias type="1">login</alias>
                </aliases>
            </doc>
        )xml";
    ExplainedError ec;
    UserInfo userInfo;
    std::tie(ec, userInfo) = parseUserInfo(Response {200, {}, body, ""});
    ASSERT_EQ(ec, ExplainedError(Error::ok));
    const UserInfo expected {"country", "lang", 0, false};
    EXPECT_EQ(userInfo, expected);
}

TEST(ParseUserInfoTest, with_domain_id_in_blackbox_response_should_return_equal_domain_id) {
    const std::string body =
        R"xml(<?xml version="1.0" encoding="UTF-8"?>
            <doc>
                <uid hosted="1" domid="146" domain="domain" mx="1" domain_ena="1" catch_all="0">13</uid>
                <login>login</login>
                <have_password>1</have_password>
                <have_hint>1</have_hint>
                <karma confirmed="0">0</karma>
                <karma_status>0</karma_status>
                <dbfield id="account_info.country.uid">country</dbfield>
                <dbfield id="userinfo.lang.uid">lang</dbfield>
                <dbfield id="subscription.suid.2">42</dbfield>
                <dbfield id="hosts.db_id.2">pg</dbfield>"
                <aliases>
                    <alias type="1">login</alias>
                </aliases>
            </doc>
        )xml";
    ExplainedError ec;
    UserInfo userInfo;
    std::tie(ec, userInfo) = parseUserInfo(Response {200, {}, body, ""});
    ASSERT_EQ(ec, ExplainedError(Error::ok));
    const UserInfo expected {"country", "lang", 146, false};
    EXPECT_EQ(userInfo, expected);
}

TEST(ParseUserInfoTest, with_not_numeric_domain_id_in_blackbox_response_should_return_parse_error) {
    const std::string body =
        R"xml(<?xml version="1.0" encoding="UTF-8"?>
            <doc>
                <uid hosted="1" domid="domid" domain="domain" mx="1" domain_ena="1" catch_all="0">13</uid>
                <login>login</login>
                <have_password>1</have_password>
                <have_hint>1</have_hint>
                <karma confirmed="0">0</karma>
                <karma_status>0</karma_status>
                <dbfield id="account_info.country.uid">country</dbfield>
                <dbfield id="userinfo.lang.uid">lang</dbfield>
                <dbfield id="subscription.suid.2">42</dbfield>
                <dbfield id="hosts.db_id.2">pg</dbfield>"
                <aliases>
                    <alias type="1">login</alias>
                </aliases>
            </doc>
        )xml";
    ExplainedError ec;
    UserInfo userInfo;
    std::tie(ec, userInfo) = parseUserInfo(Response {200, {}, body, ""});
    EXPECT_EQ(ec, ExplainedError(Error::blackBoxParseError));
}

TEST(ParseUserInfoTest, with_invalid_params_exception_should_return_fatal_error) {
    const std::string body =
        R"xml(<?xml version="1.0" encoding="UTF-8"?>
            <doc>
                <exception id=")xml" + std::to_string(bb::NSessionCodes::INVALID_PARAMS) + R"xml(">INVALID_PARAMS</exception>
                <error></error>
            </doc>
        )xml";
    ExplainedError ec;
    UserInfo userInfo;
    std::tie(ec, userInfo) = parseUserInfo(Response {200, {}, body, ""});
    EXPECT_EQ(ec, ExplainedError(Error::blackBoxFatalError));
}

TEST(ParseUserInfoTest, with_db_exception_should_return_temp_error) {
    const std::string body =
        R"xml(<?xml version="1.0" encoding="UTF-8"?>
            <doc>
                <exception id=")xml" + std::to_string(bb::NSessionCodes::DB_EXCEPTION) + R"xml(">DB_EXCEPTION</exception>
                <error></error>
            </doc>
        )xml";
    ExplainedError ec;
    UserInfo userInfo;
    std::tie(ec, userInfo) = parseUserInfo(Response {200, {}, body, ""});
    EXPECT_EQ(ec, ExplainedError(Error::blackBoxTempError));
}

TEST(ParseUserInfoTest, with_empty_subscription_suid_2_should_return_uid_not_found_error) {
    const std::string body =
        R"xml(<?xml version="1.0" encoding="UTF-8"?>
            <doc>
                <uid hosted="1" domid="146" domain="domain" mx="1" domain_ena="1" catch_all="0">13</uid>
                <login>login</login>
                <have_password>1</have_password>
                <have_hint>1</have_hint>
                <karma confirmed="0">0</karma>
                <karma_status>0</karma_status>
                <dbfield id="account_info.country.uid">country</dbfield>
                <dbfield id="userinfo.lang.uid">lang</dbfield>
                <dbfield id="subscription.suid.2"></dbfield>"
                <aliases>
                    <alias type="1">login</alias>
                </aliases>
            </doc>
        )xml";
    ExplainedError ec;
    UserInfo userInfo;
    std::tie(ec, userInfo) = parseUserInfo(Response {200, {}, body, ""});
    EXPECT_EQ(ec, ExplainedError(Error::uidNotFound));
}

TEST(ParseUserInfoTest, with_not_pg_hosts_db_id_2_should_return_not_pg_user_error) {
    const std::string body =
        R"xml(<?xml version="1.0" encoding="UTF-8"?>
            <doc>
                <uid hosted="1" domid="146" domain="domain" mx="1" domain_ena="1" catch_all="0">13</uid>
                <login>login</login>
                <have_password>1</have_password>
                <have_hint>1</have_hint>
                <karma confirmed="0">0</karma>
                <karma_status>0</karma_status>
                <dbfield id="account_info.country.uid">country</dbfield>
                <dbfield id="userinfo.lang.uid">lang</dbfield>
                <dbfield id="subscription.suid.2">42</dbfield>
                <dbfield id="hosts.db_id.2">not_pg</dbfield>"
                <aliases>
                    <alias type="1">login</alias>
                </aliases>
            </doc>
        )xml";
    ExplainedError ec;
    UserInfo userInfo;
    std::tie(ec, userInfo) = parseUserInfo(Response {200, {}, body, ""});
    EXPECT_EQ(ec, ExplainedError(Error::notPgUser));
}

TEST(ParseUserInfoTest, with_mailish_alias_should_return_with_has_mailish_alias_true) {
    const std::string body =
        R"xml(<?xml version="1.0" encoding="UTF-8"?>
            <doc>
                <uid hosted="0">13</uid>
                <login>login</login>
                <have_password>1</have_password>
                <have_hint>1</have_hint>
                <karma confirmed="0">0</karma>
                <karma_status>0</karma_status>
                <dbfield id="account_info.country.uid">country</dbfield>
                <dbfield id="userinfo.lang.uid">lang</dbfield>
                <dbfield id="subscription.suid.2">42</dbfield>
                <dbfield id="hosts.db_id.2">pg</dbfield>"
                <aliases>
                    <alias type="12">mailish_login</alias>
                </aliases>
            </doc>
        )xml";
    ExplainedError ec;
    UserInfo userInfo;
    std::tie(ec, userInfo) = parseUserInfo(Response {200, {}, body, ""});
    ASSERT_EQ(ec, ExplainedError(Error::ok));
    const UserInfo expected {"country", "lang", 0, true};
    EXPECT_EQ(userInfo, expected);
}

} // namespace tests
} // namespace detail
} // namespace registration
} // namespace mail
} // namespace sharpei
