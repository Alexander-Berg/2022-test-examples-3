#include "utils.hpp"

#include <tests/unit/server/error_code_operators.hpp>
#include <tests/unit/error_category.hpp>
#include <tests/unit/generic_operators.hpp>
#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/server/handlers/update_contacts_handler.hpp>
#include <src/logic/interface/types/reflection/updated_contacts.hpp>

namespace collie::logic {

using collie::tests::operator ==;
using collie::tests::operator <<;

} // namespace collie::logic

namespace {

using namespace testing;

namespace logic = collie::logic;
namespace tests = collie::tests;

using collie::TaskContextPtr;
using collie::error_code;
using collie::expected;
using collie::make_expected_from_error;
using collie::make_unexpected;
using collie::logic::ContactId;
using collie::logic::Revision;
using collie::logic::UpdateContacts;
using collie::logic::UpdatedContact;
using collie::logic::UpdatedContacts;
using collie::logic::Vcard;
using collie::server::Error;
using collie::server::Uid;
using collie::server::UpdateContactsHandler;
using collie::tests::MockStream;
using collie::tests::makeRequestWithRawBody;

struct UpdateContactsMock : UpdateContacts {
    MOCK_METHOD(expected<Revision>, call, (const TaskContextPtr&, const logic::Uid&, UpdatedContacts), (const));

    expected<Revision> operator ()(const TaskContextPtr& context, const logic::Uid& uid,
            UpdatedContacts updatedContacts) const override {
        return call(context, uid, std::move(updatedContacts));
    }
};

struct TestServerHandlersUpdateContactsHandler : TestWithTaskContext {
    const std::shared_ptr<const StrictMock<UpdateContactsMock>> impl {std::make_shared<const StrictMock<UpdateContactsMock>>()};
    const UpdateContactsHandler handler {impl};
    const Uid uid {"uid"};
    const boost::shared_ptr<StrictMock<MockStream>> stream {boost::make_shared<StrictMock<MockStream>>()};
    UpdatedContacts updatedContacts;
    const Revision result {42};
    const expected<Revision> error {make_unexpected(error_code(tests::Error::fail))};
    yplatform::zerocopy::streambuf buffer;

    TestServerHandlersUpdateContactsHandler() {
        const Vcard vcard;
        UpdatedContact updatedContact;
        updatedContact.contact_id = ContactId {13};
        updatedContact.vcard = vcard;
        updatedContact.uri = "uri";
        updatedContacts.updated_contacts.push_back(updatedContact);
    }
};

TEST_F(TestServerHandlersUpdateContactsHandler, operator_function_call_should_call_impl_operator_function_call_and_write_result_to_stream) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*stream, request())
            .WillOnce(Return(makeRequestWithRawBody(
                R"json({"updated_contacts": [{"contact_id": 13, "vcard": {}, "uri": "uri"}]})json",
                buffer
            )));
        EXPECT_CALL(*impl, call(context, uid.value, updatedContacts)).WillOnce(Return(result));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, "")).WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application/json")).WillOnce(Return());
        EXPECT_CALL(*stream, result_body(R"json({"revision":42})json")).WillOnce(Return());
        EXPECT_EQ(handler(uid, stream, context), expected<void>());
    });
}

TEST_F(TestServerHandlersUpdateContactsHandler, when_request_body_is_invalid_should_return_error) {
    withSpawn([&] (const auto& context) {
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithRawBody("", buffer)));
        EXPECT_EQ(handler(uid, stream, context), make_expected_from_error<void>(error_code(Error::invalidRequestBodyFormat)));
    });
}

TEST_F(TestServerHandlersUpdateContactsHandler, when_impl_return_error_should_return_error) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*stream, request())
            .WillOnce(Return(makeRequestWithRawBody(
                R"json({"updated_contacts": [{"contact_id": 13, "vcard": {}, "uri": "uri"}]})json",
                buffer
            )));
        EXPECT_CALL(*impl, call(context, uid.value, updatedContacts)).WillOnce(Return(error));
        EXPECT_EQ(handler(uid, stream, context), make_expected_from_error<void>(error_code(tests::Error::fail)));
    });
}

} // namespace
