#include <tests/unit/server/error_code_operators.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>
#include <tests/unit/test_with_task_context.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/server/handlers/remove_tag_handler.hpp>

namespace {

using namespace testing;

namespace logic = collie::logic;

using collie::expected;
using collie::error_code;
using collie::TaskContextPtr;
using collie::server::Error;
using collie::server::RemoveTagHandler;
using collie::server::TagId;
using collie::server::Uid;
using collie::tests::MockStream;
using logic::RemoveTag;
using logic::Revision;

struct RemoveTagMock : RemoveTag {
    MOCK_METHOD(expected<Revision>, call, (const TaskContextPtr&, const logic::Uid&,
            const logic::TagId), (const));

    expected<Revision> operator()(const TaskContextPtr& context, const logic::Uid& uid,
            const logic::TagId tagId) const override {
        return call(context, uid, tagId);
    }
};

struct TestServerHandlersRemoveTagHandler : TestWithTaskContext {
    const std::shared_ptr<const StrictMock<RemoveTagMock>> impl {std::make_shared<const StrictMock<RemoveTagMock>>()};
    const RemoveTagHandler handler {impl};
    const Uid uid {"uid"};
    const TagId tagId {13};
    const boost::shared_ptr<StrictMock<MockStream>> stream {boost::make_shared<StrictMock<MockStream>>()};
};

TEST_F(TestServerHandlersRemoveTagHandler, operator_function_call_should_call_impl_operator_function_call_and_write_result_to_stream) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*impl, call(context, uid.value, tagId.value)).WillOnce(Return(Revision {42}));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, ""));
        EXPECT_CALL(*stream, set_content_type("application/json"));
        EXPECT_CALL(*stream, result_body(R"json({"revision":42})json"));
        EXPECT_EQ(handler(uid, tagId, stream, context), expected<void>());
    });
}

} // namespace
