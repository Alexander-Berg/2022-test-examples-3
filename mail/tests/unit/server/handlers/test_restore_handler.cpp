#include <tests/unit/server/error_code_operators.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>
#include <tests/unit/test_with_task_context.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/server/handlers/restore_handler.hpp>

namespace {

using namespace testing;

namespace logic = collie::logic;

using collie::TaskContextPtr;
using collie::error_code;
using collie::expected;
using collie::make_expected_from_error;
using collie::logic::Restore;
using collie::server::Error;
using collie::server::RestoreHandler;
using collie::server::Revision;
using collie::server::Uid;
using collie::tests::MockStream;

struct RestoreMock : Restore {
    MOCK_METHOD(expected<void>, call, (const TaskContextPtr&, const logic::Uid&, const logic::Revision), (const));

    expected<void> operator ()(const TaskContextPtr& context, const logic::Uid& uid,
            const logic::Revision revision) const override {
        return call(context, uid, revision);
    }
};

struct TestServerHandlersRestoreHandler : TestWithTaskContext {
    const std::shared_ptr<const StrictMock<RestoreMock>> impl {std::make_shared<const StrictMock<RestoreMock>>()};
    const RestoreHandler handler {impl};
    const Uid uid {"uid"};
    const Revision revision {42};
    const boost::shared_ptr<StrictMock<MockStream>> stream {boost::make_shared<StrictMock<MockStream>>()};
};

TEST_F(TestServerHandlersRestoreHandler, operator_function_call_should_call_impl_operator_function_call_and_write_result_to_stream) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*impl, call(context, uid.value, revision.value))
            .WillOnce(Return(make_expected_from_error<void>(error_code())));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, "")).WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application/json")).WillOnce(Return());
        EXPECT_CALL(*stream, result_body(R"json({"status":"ok"})json")).WillOnce(Return());
        EXPECT_EQ(handler(uid, revision, stream, context), expected<void>());
    });
}

} // namespace
