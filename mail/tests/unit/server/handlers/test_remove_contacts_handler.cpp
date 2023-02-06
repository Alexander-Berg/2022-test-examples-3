#include "utils.hpp"

#include <tests/unit/server/error_code_operators.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>
#include <tests/unit/test_with_task_context.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/server/handlers/remove_contacts_handler.hpp>

namespace {

using namespace testing;

namespace logic = collie::logic;

using collie::expected;
using collie::error_code;
using collie::TaskContextPtr;
using collie::server::Error;
using collie::server::ContactIds;
using collie::server::RemoveContactsHandler;
using collie::server::Uid;
using collie::tests::MockStream;
using logic::ContactId;
using logic::RemoveContacts;
using logic::Revision;

struct RemoveContactsMock : RemoveContacts {
    MOCK_METHOD(expected<Revision>, call, (const TaskContextPtr&, const logic::Uid&,
            const std::vector<ContactId>&), (const));

    expected<Revision> operator()(const TaskContextPtr& context, const logic::Uid& uid,
            const std::vector<ContactId>& contactIds) const override {
        return call(context, uid, contactIds);
    }
};

struct TestServerHandlersRemoveContactsHandler : TestWithTaskContext {
    const std::shared_ptr<const StrictMock<RemoveContactsMock>> impl {std::make_shared<const StrictMock<RemoveContactsMock>>()};
    const RemoveContactsHandler handler {impl};
    const Uid uid {"uid"};
    const ContactIds contactIds {{1, 2}};
    const boost::shared_ptr<StrictMock<MockStream>> stream {boost::make_shared<StrictMock<MockStream>>()};
    const Revision result {42};
};

TEST_F(TestServerHandlersRemoveContactsHandler, operator_function_call_should_call_impl_operator_function_call_and_write_result_to_stream) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*impl, call(context, uid.value, contactIds.value)).WillOnce(Return(result));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, ""));
        EXPECT_CALL(*stream, set_content_type("application/json"));
        EXPECT_CALL(*stream, result_body(R"json({"revision":42})json"));
        EXPECT_EQ(handler(uid, contactIds, stream, context), expected<void>());
    });
}

} // namespace
