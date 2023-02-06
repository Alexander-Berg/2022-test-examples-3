#include "utils.hpp"

#include <src/server/handlers/add_directory_event_handler.hpp>

#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>

namespace collie::logic {

static bool operator==(const NewEvent& left, const NewEvent& right) {
    return left.org_id == right.org_id && left.event == right.event;
}

} // namespace collie::logic

namespace {

namespace logic = collie::logic;

using collie::expected;
using collie::server::AddDirectoryEventHandler;
using collie::TaskContextPtr;
using collie::tests::makeRequestWithRawBody;
using collie::tests::MockStream;
using logic::AddDirectoryEvent;
using logic::NewEvent;
using collie::logic::EventType;
using logic::OrgId;
using logic::Revision;

struct AddDirectoryEventMock : AddDirectoryEvent {
    MOCK_METHOD(expected<void>, call, (const TaskContextPtr&, const NewEvent& event), (const));

    expected<void> operator()(const TaskContextPtr& context, NewEvent event) const override {
        return call(context, event);
    }
};

class TestServerHandlersAddDirectoryEventHandler : public TestWithTaskContext {
protected:
    const std::shared_ptr<const StrictMock<AddDirectoryEventMock>> impl{std::make_shared<const StrictMock<
        AddDirectoryEventMock>>()};
    const AddDirectoryEventHandler handler{impl};
    const boost::shared_ptr<StrictMock<MockStream>> stream{boost::make_shared<StrictMock<MockStream>>()};
    yplatform::zerocopy::streambuf buffer;
};

TEST_F(TestServerHandlersAddDirectoryEventHandler,
        operator_call_must_call_impl_operator_call_and_write_result_to_stream) {
    withSpawn([&](const auto& context) {
        const InSequence sequence;

        const OrgId orgId{2};
        const auto event{EventType::updateOrg};
        const Revision revision{0};
        const std::string requestBody{R"({"org_id": )" + std::to_string(orgId) + R"(, "event": "user_added", "revision": ")" + std::to_string(revision) + R"("})"};
        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithRawBody(requestBody, buffer)));
        EXPECT_CALL(*impl, call(context, NewEvent{orgId, event, revision})).WillOnce(Return(expected<void>()));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, ""));
        EXPECT_CALL(*stream, set_content_type("application/json"));
        EXPECT_CALL(*stream, result_body(R"({"status":"ok"})"));
        EXPECT_EQ(expected<void>{}, handler(stream, context));
    });
}

} // namespace
