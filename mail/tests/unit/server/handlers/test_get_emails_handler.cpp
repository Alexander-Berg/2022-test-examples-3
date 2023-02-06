#include "utils.hpp"

#include <src/server/handlers/get_emails_handler.hpp>

#include <tests/unit/error_category.hpp>
#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>

namespace {

namespace logic = collie::logic;
namespace server = collie::server;
namespace tests = collie::tests;

using collie::error_code;
using collie::expected;
using collie::make_expected_from_error;
using collie::TaskContextPtr;
using logic::ContactId;
using logic::EmailId;
using logic::ExistingEmails;
using logic::GetEmailsResult;
using logic::GetEmails;
using logic::TagId;
using server::GetEmailsHandler;
using server::TagIds;
using server::Uid;
using tests::Error;
using tests::makeRequestWithUriParams;
using tests::MockStream;

struct GetEmailsMock : GetEmails {
    MOCK_METHOD(expected<GetEmailsResult>, call, (
            const TaskContextPtr&,
            const logic::Uid&,
            const std::vector<logic::TagId>&,
            const std::optional<std::string_view>&), (const));

    expected<GetEmailsResult> operator()(
            const TaskContextPtr& context,
            const logic::Uid& uid,
            const std::vector<logic::TagId>& tagIds,
            const std::optional<std::string_view>& mixin) const override {
        return call(context, uid, tagIds, mixin);
    }
};

struct TestServerHandlersGetEmailsHandler : TestWithTaskContext {
    ymod_webserver::param_map_t makeUriParams() const {
        return {{std::string{*mixin}, std::string{*mixin}}};
    }

    std::string makeResultBody() const {
        return R"({"emails":[)"
                R"({"tag_id":1,"count":2,"emails":[)"
                R"({"contact_id":4,"email":"local0@domain0.com"},)"
                R"({"email_id":1,"contact_id":5,"email":"local1@domain1.com"}]},)"
                R"({"tag_id":2,"count":1,"emails":[)"
                R"({"contact_id":5,"email":"local2@domain2.com"}]}]})";
    }

    const boost::shared_ptr<StrictMock<MockStream>> stream{boost::make_shared<StrictMock<MockStream>>()};
    const std::shared_ptr<const StrictMock<GetEmailsMock>> impl{std::make_shared<const StrictMock<
            GetEmailsMock>>()};
    const GetEmailsHandler handler{impl};
    const Uid uid{"uid"};
    const TagId tagId{1};
    const TagIds tagIds{{tagId - 1, tagId, tagId + 1}};
    const std::optional<std::string_view> mixin{"mixin"};
    const ContactId contactId{4};
    const EmailId emailId{1};
    const GetEmailsResult getEmailsResult {{
        ExistingEmails {
            tagId,
            2,
            {{{}, contactId, {}, "local0@domain0.com", {}, {}},
             {emailId, contactId + 1, {}, "local1@domain1.com", {}, {}}}
        },
        ExistingEmails {
            tagId + 1,
            1,
            {{{}, contactId + 1, {}, "local2@domain2.com", {}, {}}}
        }
    }};
};

TEST_F(TestServerHandlersGetEmailsHandler,
        operator_call_must_call_impl_operator_call_and_write_result_to_stream) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithUriParams(makeUriParams())));
        EXPECT_CALL(*impl, call(context, uid.value, tagIds.value, mixin)).WillOnce(Return(getEmailsResult));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, std::string{}));
        EXPECT_CALL(*stream, set_content_type("application/json"));
        EXPECT_CALL(*stream, result_body(makeResultBody()));
        EXPECT_EQ(expected<void>{}, handler(uid, tagIds, stream, context));
    });
}

TEST_F(TestServerHandlersGetEmailsHandler, operator_call_must_return_error_when_impl_returns_error) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithUriParams(makeUriParams())));
        const error_code error{Error::fail};
        EXPECT_CALL(*impl, call(context, uid.value, tagIds.value, mixin)).WillOnce(Return(
                make_expected_from_error<GetEmailsResult>(error)));
        EXPECT_EQ(make_expected_from_error<void>(error), handler(uid, tagIds, stream, context));
    });
}

} // namespace
