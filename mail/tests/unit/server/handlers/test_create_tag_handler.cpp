#include "utils.hpp"

#include <tests/unit/server/error_code_operators.hpp>
#include <tests/unit/generic_operators.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>
#include <tests/unit/test_with_task_context.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/server/handlers/create_tag_handler.hpp>
#include <src/logic/interface/types/reflection/new_tag.hpp>

namespace collie::logic {

using collie::tests::operator ==;
using collie::tests::operator <<;

} // namespace collie::logic

namespace {

using namespace testing;

namespace logic = collie::logic;

using collie::error_code;
using collie::expected;
using collie::make_expected_from_error;
using collie::TaskContextPtr;
using collie::server::Error;
using collie::server::CreateTagHandler;
using collie::server::Uid;
using collie::tests::MockStream;
using collie::tests::makeRequestWithRawBody;
using logic::CreateTag;
using logic::CreatedTag;
using logic::NewTag;
using logic::Revision;
using logic::TagId;

struct CreateTagMock : CreateTag {
    MOCK_METHOD(expected<CreatedTag>, call, (const TaskContextPtr&, const logic::Uid&,
            const NewTag&), (const));

    expected<CreatedTag> operator()(const TaskContextPtr& context, const logic::Uid& uid,
            const NewTag& newTag) const override {
        return call(context, uid, newTag);
    }
};

struct TestServerHandlersCreateTagHandler : TestWithTaskContext {
    const std::shared_ptr<const StrictMock<CreateTagMock>> impl {std::make_shared<const StrictMock<CreateTagMock>>()};
    const CreateTagHandler handler {impl};
    const Uid uid {"uid"};
    const boost::shared_ptr<StrictMock<MockStream>> stream {boost::make_shared<StrictMock<MockStream>>()};
    const NewTag newTag {"tag name", {}};
    const CreatedTag result {TagId {13}, Revision {42}};
    yplatform::zerocopy::streambuf buffer;
};

TEST_F(TestServerHandlersCreateTagHandler, operator_function_call_should_call_impl_operator_function_call_and_write_result_to_stream) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*stream, request())
            .WillOnce(Return(makeRequestWithRawBody(R"json({"name": "tag name"})json", buffer)));
        EXPECT_CALL(*impl, call(context, uid.value, newTag)).WillOnce(Return(result));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, ""));
        EXPECT_CALL(*stream, set_content_type("application/json"));
        EXPECT_CALL(*stream, result_body(R"json({"tag_id":13,"revision":42})json"));
        EXPECT_EQ(handler(uid, stream, context), expected<void>());
    });
}

TEST_F(TestServerHandlersCreateTagHandler, when_request_body_is_invalid_should_return_error) {
    withSpawn([&] (const auto& context) {
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithRawBody("", buffer)));
        EXPECT_EQ(handler(uid, stream, context), make_expected_from_error<void>(error_code(Error::invalidRequestBodyFormat)));
    });
}

} // namespace
