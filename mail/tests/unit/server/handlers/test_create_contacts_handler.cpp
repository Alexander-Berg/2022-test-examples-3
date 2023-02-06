#include "utils.hpp"

#include <tests/unit/server/error_code_operators.hpp>
#include <tests/unit/create_contacts_mock.hpp>
#include <tests/unit/error_category.hpp>
#include <tests/unit/generic_operators.hpp>
#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/server/handlers/create_contacts_handler.hpp>
#include <src/logic/interface/types/reflection/new_contact.hpp>

namespace collie::logic {

using collie::tests::operator ==;
using collie::tests::operator <<;

} // namespace collie::logic

namespace {

using namespace testing;
using namespace std::string_literals;

namespace logic = collie::logic;
namespace tests = collie::tests;

using collie::TaskContextPtr;
using collie::error_code;
using collie::expected;
using collie::make_expected_from_error;
using collie::make_unexpected;
using collie::logic::ContactId;
using collie::logic::CreatedContacts;
using collie::logic::ListId;
using collie::logic::NewContact;
using collie::logic::Revision;
using collie::logic::Vcard;
using collie::server::CreateContactsHandler;
using collie::server::Error;
using collie::server::Uid;
using tests::CreateContactsMock;
using tests::MockStream;
using tests::makeRequestWithRawBody;

struct TestServerHandlersCreateContactsHandler : TestWithTaskContext {
    const std::shared_ptr<const StrictMock<CreateContactsMock>> impl {std::make_shared<const StrictMock<CreateContactsMock>>()};
    const CreateContactsHandler handler {impl};
    const Uid uid {"uid"};
    const boost::shared_ptr<StrictMock<MockStream>> stream {boost::make_shared<StrictMock<MockStream>>()};
    std::vector<NewContact> newContacts;
    const CreatedContacts result {{ContactId {13}}, Revision {42}};
    const expected<CreatedContacts> error {make_unexpected(error_code(tests::Error::fail))};
    yplatform::zerocopy::streambuf buffer;

    TestServerHandlersCreateContactsHandler() {
        const Vcard vcard;
        newContacts.push_back(NewContact {ListId {0}, vcard, {}, "uri"s});
    }
};

TEST_F(TestServerHandlersCreateContactsHandler, operator_function_call_should_call_impl_operator_function_call_and_write_result_to_stream) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*stream, request())
            .WillOnce(Return(makeRequestWithRawBody(R"json([{"list_id": 0, "vcard": {}, "uri": "uri"}])json", buffer)));
        EXPECT_CALL(*impl, call(context, uid.value, newContacts)).WillOnce(Return(result));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, "")).WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application/json")).WillOnce(Return());
        EXPECT_CALL(*stream, result_body(R"json({"contact_ids":[13],"revision":42})json")).WillOnce(Return());
        EXPECT_EQ(handler(uid, stream, context), expected<void>());
    });
}

TEST_F(TestServerHandlersCreateContactsHandler, when_request_body_is_invalid_should_return_error) {
    withSpawn([&] (const auto& context) {
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithRawBody("", buffer)));
        EXPECT_EQ(handler(uid, stream, context), make_expected_from_error<void>(error_code(Error::invalidRequestBodyFormat)));
    });
}

TEST_F(TestServerHandlersCreateContactsHandler, when_impl_return_error_should_return_error) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*stream, request())
            .WillOnce(Return(makeRequestWithRawBody(R"json([{"list_id": 0, "vcard": {}, "uri": "uri"}])json", buffer)));
        EXPECT_CALL(*impl, call(context, uid.value, newContacts)).WillOnce(Return(error));
        EXPECT_EQ(handler(uid, stream, context), make_expected_from_error<void>(error_code(tests::Error::fail)));
    });
}

} // namespace
