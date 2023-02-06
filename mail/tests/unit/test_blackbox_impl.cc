#include "settings_blackbox_client_mocks.h"
#include "test_with_task_context.h"

#include <internal/blackbox/impl.h>
#include <internal/common/error_code.h>
#include <internal/common/types_reflection.h>

#include <yamail/data/deserialization/json_reader.h>

#include <boost/fusion/include/equal_to.hpp>

namespace settings {

static bool operator == (const Email& lhs, const Email& rhs) {
    return boost::fusion::operator==(lhs, rhs);
}

namespace blackbox {

static bool operator == (const AccountInfo& lhs, const AccountInfo& rhs) {
    return lhs.fromName() == rhs.fromName()
        && lhs.fromNameEng() == rhs.fromNameEng()
        && lhs.emails().items == rhs.emails().items;
}

} // namespace blackbox

} // namespace settings

namespace {

using namespace testing;
using namespace settings;
using namespace settings::test;
using namespace settings::blackbox;

using Error = settings::Error;

struct TestBlackBoxImpl: public TestWithTaskContext {
    std::shared_ptr<MockBlackBox> blackBoxClient = std::make_shared<MockBlackBox>();
    std::shared_ptr<Impl> blackBoxImpl = std::make_shared<Impl>(blackBoxClient);

    const std::string blackboxResponse =
        R"({"users":[{"id":"42","uid":{"value":"42","hosted":false},)"
        R"("login":"ru","have_password":true,"have_hint":true,)"
        R"("karma":{"value":0},"karma_status":{"value":0},)"
        R"("dbfields":{"subscription.suid.2":149,"userinfo.firstname.uid":"Hello",)"
        R"("userinfo.lastname.uid":"Kitty"},)"
        R"("attributes":{"212":"HelloEng","213":"KittyEng"},)"
        R"("address-list":[{"address":"hello@kitty.cat","validated":true,)"
        R"("default":true,"rpop":true,"silent":true,"unsafe":true,"native":true,)"
        R"("born-date":"1974-01-01"}]}]})";

    const std::string blackboxResponseWithoutUid = R"({"users":[{}]})";

    const std::string blackboxResponseWithoutSuid =
        R"({"users":[{"uid":{"value":"42","hosted":false}}]})";

    const std::string blackboxResponseWithoutEmails =
        R"({"users":[{"uid":{"value":"42","hosted":false}}]})";

    const std::string withErrorBlackboxResponse =
        R"({"users":[{"exception":{"id":"9","value":"DB_FETCHFAILED"}}]})";
};

TEST_F(TestBlackBoxImpl, for_isUserExists_response_containing_blackbox_error_should_return_blackBoxError_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        std::string requestUrl = ymod_httpclient::url_encode({
                {"method",      "userinfo"},
                {"format",      "json"},
                {"uid",         "42"},
                {"userip",      "228"},
                {"dbfields",    "subscription.suid.2"}
        }, '?');
        EXPECT_CALL(*blackBoxClient, infoRequest(context, requestUrl))
            .WillOnce(Return(withErrorBlackboxResponse));
        const auto result = blackBoxImpl->isUserExists(context, "42", "228");
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::blackBoxUserError);
    });
}

TEST_F(TestBlackBoxImpl, for_getAccountInfo_response_containing_blackbox_error_should_return_blackBoxError_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        std::string requestUrl = ymod_httpclient::url_encode({
                {"method",      "userinfo"},
                {"format",      "json"},
                {"emails",      "getall"},
                {"uid",         "42"},
                {"userip",      "228"},
                {"dbfields",    "userinfo.firstname.uid,userinfo.lastname.uid"},
                {"attributes",  "212,213"}
        }, '?');
        EXPECT_CALL(*blackBoxClient, infoRequest(context, requestUrl))
            .WillOnce(Return(withErrorBlackboxResponse));
        const auto result = blackBoxImpl->getAccountInfo(context, "42", "228");
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::blackBoxUserError);
    });
}

TEST_F(TestBlackBoxImpl, for_isUserExists_response_not_containing_uid_should_return_blackBoxUserError_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        std::string requestUrl = ymod_httpclient::url_encode({
                {"method",      "userinfo"},
                {"format",      "json"},
                {"uid",         "42"},
                {"userip",      "228"},
                {"dbfields",    "subscription.suid.2"}
        }, '?');
        EXPECT_CALL(*blackBoxClient, infoRequest(context, requestUrl))
            .WillOnce(Return(blackboxResponseWithoutUid));
        const auto result = blackBoxImpl->isUserExists(context, "42", "228");
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::blackBoxUserError);
    });
}

TEST_F(TestBlackBoxImpl, for_getAccountInfo_response_not_containing_uid_should_return_blackBoxUserError_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        std::string requestUrl = ymod_httpclient::url_encode({
                {"method",      "userinfo"},
                {"format",      "json"},
                {"emails",      "getall"},
                {"uid",         "42"},
                {"userip",      "228"},
                {"dbfields",    "userinfo.firstname.uid,userinfo.lastname.uid"},
                {"attributes",  "212,213"}
        }, '?');
        EXPECT_CALL(*blackBoxClient, infoRequest(context, requestUrl))
            .WillOnce(Return(blackboxResponseWithoutUid));
        const auto result = blackBoxImpl->getAccountInfo(context, "42", "228");
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::blackBoxUserError);
    });
}

TEST_F(TestBlackBoxImpl, for_isUserExists_response_not_containing_suid_should_return_blackBoxUserError_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        std::string requestUrl = ymod_httpclient::url_encode({
                {"method",      "userinfo"},
                {"format",      "json"},
                {"uid",         "42"},
                {"userip",      "228"},
                {"dbfields",    "subscription.suid.2"}
        }, '?');
        EXPECT_CALL(*blackBoxClient, infoRequest(context, requestUrl))
            .WillOnce(Return(blackboxResponseWithoutSuid));
        const auto result = blackBoxImpl->isUserExists(context, "42", "228");
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::blackBoxUserError);
    });
}

TEST_F(TestBlackBoxImpl, for_isUserExists_good_response_should_not_return_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        std::string requestUrl = ymod_httpclient::url_encode({
                {"method",      "userinfo"},
                {"format",      "json"},
                {"uid",         "42"},
                {"userip",      "228"},
                {"dbfields",    "subscription.suid.2"}
        }, '?');
        EXPECT_CALL(*blackBoxClient, infoRequest(context, requestUrl))
            .WillOnce(Return(blackboxResponse));
        const auto result = blackBoxImpl->isUserExists(context, "42", "228");
        ASSERT_TRUE(result);
    });
}

TEST_F(TestBlackBoxImpl, for_getAccountInfo_response_not_containing_emails_return_blackBoxUserError_error) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        std::string requestUrl = ymod_httpclient::url_encode({
                {"method",      "userinfo"},
                {"format",      "json"},
                {"emails",      "getall"},
                {"uid",         "42"},
                {"userip",      "228"},
                {"dbfields",    "userinfo.firstname.uid,userinfo.lastname.uid"},
                {"attributes",  "212,213"}
        }, '?');
        EXPECT_CALL(*blackBoxClient, infoRequest(context, requestUrl))
            .WillOnce(Return(blackboxResponseWithoutEmails));
        const auto result = blackBoxImpl->getAccountInfo(context, "42", "228");
        ASSERT_FALSE(result);
        EXPECT_EQ(result.error(), Error::blackBoxUserError);
    });
}

TEST_F(TestBlackBoxImpl, for_getAccountInfo_good_response_return_account_info_result) {
    withSpawn([this](const auto& context) {
        const InSequence s;
        std::string requestUrl = ymod_httpclient::url_encode({
                {"method",      "userinfo"},
                {"format",      "json"},
                {"emails",      "getall"},
                {"uid",         "42"},
                {"userip",      "228"},
                {"dbfields",    "userinfo.firstname.uid,userinfo.lastname.uid"},
                {"attributes",  "212,213"}
        }, '?');
        EXPECT_CALL(*blackBoxClient, infoRequest(context, requestUrl))
            .WillOnce(Return(blackboxResponse));
        const auto result = blackBoxImpl->getAccountInfo(context, "42", "228");
        ASSERT_TRUE(result);
        AddressList addresses = {{"hello@kitty.cat", "1974-01-01", 1, 1, 1, 1}};
        AccountInfo accountInfo {"Hello", "Kitty", "HelloEng", "KittyEng", addresses};
        EXPECT_EQ(*result.value(), accountInfo);
    });
}

}
