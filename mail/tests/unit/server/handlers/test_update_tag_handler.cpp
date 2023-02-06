#include "utils.hpp"

#include <tests/unit/server/error_code_operators.hpp>
#include <tests/unit/generic_operators.hpp>
#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/server/handlers/update_tag_handler.hpp>
#include <src/logic/interface/types/reflection/updated_tag.hpp>

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
using collie::server::TagId;
using collie::server::Uid;
using collie::server::UpdateTagHandler;
using collie::tests::MockStream;
using collie::tests::makeRequestWithRawBody;
using logic::Revision;
using logic::UpdatedTag;
using logic::UpdateTag;

struct UpdateTagMock : UpdateTag {
    MOCK_METHOD(expected<Revision>, call, (const TaskContextPtr&, const logic::Uid&,
            const logic::TagId, const UpdatedTag&), (const));

    expected<Revision> operator()(const TaskContextPtr& context, const logic::Uid& uid,
            const logic::TagId tagId, const UpdatedTag& updatedTag) const override {
        return call(context, uid, tagId, updatedTag);
    }
};

struct TestServerHandlersUpdateTagHandler : TestWithTaskContext {
    const std::shared_ptr<const StrictMock<UpdateTagMock>> impl {std::make_shared<const StrictMock<UpdateTagMock>>()};
    const UpdateTagHandler handler {impl};
    const Uid uid {"uid"};
    const TagId tagId {42};
    const boost::shared_ptr<StrictMock<MockStream>> stream {boost::make_shared<StrictMock<MockStream>>()};
    const UpdatedTag updatedTag {"tag name", {}, {}, {}, {}, {}};
    yplatform::zerocopy::streambuf buffer;
};

TEST_F(TestServerHandlersUpdateTagHandler, operator_function_call_should_call_impl_operator_function_call_and_write_result_to_stream) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*stream, request())
            .WillOnce(Return(makeRequestWithRawBody(R"json({"name": "tag name"})json", buffer)));
        EXPECT_CALL(*impl, call(context, uid.value, tagId.value, updatedTag)).WillOnce(Return(Revision {13}));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, ""));
        EXPECT_CALL(*stream, set_content_type("application/json"));
        EXPECT_CALL(*stream, result_body(R"json({"revision":13})json"));
        EXPECT_EQ(handler(uid, tagId, stream, context), expected<void>());
    });
}

TEST_F(TestServerHandlersUpdateTagHandler, when_request_body_is_invalid_should_return_error) {
    withSpawn([&] (const auto& context) {
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithRawBody("", buffer)));
        const auto result = handler(uid, tagId, stream, context);
        EXPECT_EQ(result, make_expected_from_error<void>(error_code(Error::invalidRequestBodyFormat)));
    });
}

} // namespace
