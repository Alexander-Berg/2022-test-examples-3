#include <tests/unit/server/error_code_operators.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>
#include <tests/unit/test_with_task_context.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/error_code.hpp>
#include <src/server/handlers/get_changes_handler.hpp>
#include <src/logic/interface/types/change.hpp>

namespace {

using namespace testing;

namespace logic = collie::logic;

using collie::expected;
using collie::TaskContextPtr;
using collie::logic::Change;
using collie::logic::Changes;
using collie::logic::ChangeType;
using collie::logic::GetChanges;
using collie::logic::Revision;
using collie::server::Error;
using collie::server::GetChangesHandler;
using collie::server::Uid;
using collie::tests::MockStream;

struct GetChangesMock : GetChanges {
    MOCK_METHOD(expected<Changes>, call, (const TaskContextPtr&, const logic::Uid&), (const));
    expected<Changes> operator()(const TaskContextPtr& context,
            const logic::Uid& uid) const override {
        return call(context, uid);
    }
};

struct TestServerHandlersGetChangesHandler : TestWithTaskContext {
    const std::shared_ptr<const StrictMock<GetChangesMock>> impl {std::make_shared<const StrictMock<GetChangesMock>>()};
    const GetChangesHandler handler {impl};
    const Uid uid {"uid"};
    const boost::shared_ptr<StrictMock<MockStream>> stream {boost::make_shared<StrictMock<MockStream>>()};
    const Changes result{{Change{Revision{42}, std::time_t{100500}, ChangeType::createContacts}}};
};

TEST_F(TestServerHandlersGetChangesHandler, operator_function_call_should_call_impl_operator_function_call_and_write_result_to_stream) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*impl, call(context, uid.value)).WillOnce(Return(expected(result)));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, "")).WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application/json")).WillOnce(Return());
        EXPECT_CALL(*stream, result_body(
                R"({"changes":[{"revision":42,"time":100500,"type":"create_contacts"}]})"));
        EXPECT_EQ(handler(uid, stream, context), expected<void>());
    });
}

} // namespace
