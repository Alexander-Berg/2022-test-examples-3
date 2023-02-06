#include <tests/unit/server/error_code_operators.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>
#include <tests/unit/test_with_task_context.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/server/handlers/get_tags_handler.hpp>
#include <src/logic/interface/types/existing_tag.hpp>
#include <src/server/error_category.hpp>

namespace {

using namespace testing;

namespace logic = collie::logic;

using collie::expected;
using collie::TaskContextPtr;
using collie::logic::ExistingTag;
using collie::logic::ExistingTags;
using collie::logic::GetTags;
using collie::logic::Revision;
using collie::logic::TagId;
using collie::logic::TagType;
using collie::server::Error;
using collie::server::GetTagsHandler;
using collie::server::Uid;
using collie::tests::MockStream;
using collie::error_code;

struct GetTagsMock : GetTags {
    MOCK_METHOD(expected<ExistingTags>, call, (const TaskContextPtr&, const logic::Uid&), (const));
    expected<ExistingTags> operator()(const TaskContextPtr& context,
            const logic::Uid& uid) const override {
        return call(context, uid);
    }
};

struct TestServerHandlersGetTagsHandler : TestWithTaskContext {
    const std::shared_ptr<const StrictMock<GetTagsMock>> impl {std::make_shared<const StrictMock<GetTagsMock>>()};
    const GetTagsHandler handler {impl};
    const Uid uid {"uid"};
    const boost::shared_ptr<StrictMock<MockStream>> stream {boost::make_shared<StrictMock<MockStream>>()};
    const ExistingTags result{{ExistingTag{TagId{1}, TagType::system, "tag name", 13, Revision{42}}}};
};

TEST_F(TestServerHandlersGetTagsHandler, operator_function_call_should_call_impl_operator_function_call_and_write_result_to_stream) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*impl, call(context, uid.value)).WillOnce(Return(result));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, "")).WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application/json")).WillOnce(Return());
        EXPECT_CALL(*stream, result_body(R"({"tags":[{"tag_id":1,"type":"system","name":"tag name",)"
                R"("contacts_count":13,"revision":42}]})"));
        EXPECT_EQ(handler(uid, stream, context), expected<void>());
    });
}

} // namespace
