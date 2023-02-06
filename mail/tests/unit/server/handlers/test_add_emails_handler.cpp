#include "add_emails_mock.hpp"
#include "utils.hpp"

#include <src/server/handlers/add_emails_handler.hpp>

#include <tests/unit/error_category.hpp>
#include <tests/unit/logic/interface/types/recipients.hpp>
#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>

namespace {

namespace logic = collie::logic;
namespace server = collie::server;
namespace tests = collie::tests;

using namespace std::string_literals;

using collie::error_code;
using collie::expected;
using collie::make_expected_from_error;
using collie::TaskContextPtr;
using logic::ContactId;
using logic::CreatedContacts;
using logic::Recipients;
using logic::Revision;
using server::AddEmailsHandler;
using server::Error;
using server::Uid;
using tests::AddEmailsMock;
using tests::makeRequestWithRawBody;
using tests::MockStream;

struct TestServerHandlersAddEmailsHandler : TestWithTaskContext {
    std::string makeRequestBody() const {
        return R"({"to":["local0@domain0.com"],"cc":["local1@domain1.com","local2@domain2.com"],)"
                R"("bcc":["local3@domain3.com","local4@domain4.com","local5@domain5.com"]})";
    }

    const boost::shared_ptr<StrictMock<MockStream>> stream{boost::make_shared<StrictMock<MockStream>>()};
    yplatform::zerocopy::streambuf buffer;
    const std::shared_ptr<const StrictMock<AddEmailsMock>> impl{std::make_shared<const StrictMock<
            AddEmailsMock>>()};
    const AddEmailsHandler handler{impl};
    const Uid uid{"uid"};
    const Recipients recipients {
        std::vector<std::string>{"local0@domain0.com"},
        std::vector<std::string>{"local1@domain1.com", "local2@domain2.com"},
        std::vector<std::string>{"local3@domain3.com", "local4@domain4.com", "local5@domain5.com"}
    };

    const CreatedContacts createdContacts{{ContactId{1}}, Revision{2}};
};

TEST_F(TestServerHandlersAddEmailsHandler,
        operator_call_must_call_impl_operator_call_and_write_result_to_stream) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithRawBody(makeRequestBody(), buffer)));
        EXPECT_CALL(*impl, call(context, uid.value, recipients)).WillOnce(Return(createdContacts));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, std::string{}));
        EXPECT_CALL(*stream, set_content_type("application/json"));
        EXPECT_CALL(*stream, result_body(R"({"contact_ids":[1],"revision":2})"));
        EXPECT_EQ(expected<void>{}, handler(uid, stream, context));
    });
}

TEST_F(TestServerHandlersAddEmailsHandler, operator_call_must_return_error_when_invalid_request) {
    withSpawn([&](const auto& context) {
        const std::string body;
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithRawBody(body, buffer)));
        EXPECT_EQ(make_expected_from_error<void>(error_code(Error::invalidRequestBodyFormat)),
                handler(uid, stream, context));
    });
}

TEST_F(TestServerHandlersAddEmailsHandler, operator_call_must_return_error_when_impl_returns_error) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithRawBody(makeRequestBody(), buffer)));
        const error_code error{tests::Error::fail};
        EXPECT_CALL(*impl, call(context, uid.value, recipients)).WillOnce(Return(make_expected_from_error<
                CreatedContacts>(error)));
        EXPECT_EQ(make_expected_from_error<void>(error), handler(uid, stream, context));
    });
}

} // namespace
