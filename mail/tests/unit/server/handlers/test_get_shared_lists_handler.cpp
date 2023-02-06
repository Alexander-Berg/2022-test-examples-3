#include <src/logic/interface/get_shared_lists.hpp>
#include <src/logic/interface/types/existing_shared_list.hpp>
#include <src/logic/interface/types/existing_shared_lists.hpp>
#include <src/logic/interface/types/list_id.hpp>
#include <src/logic/interface/types/revision.hpp>
#include <src/server/handlers/get_shared_lists_handler.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>
#include <tests/unit/test_with_task_context.hpp>
#include <ymod_webserver/codes.h>
#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <boost/shared_ptr.hpp>
#include <boost/smart_ptr/make_shared.hpp>
#include <memory>

namespace {

using namespace testing;
namespace logic = collie::logic;
using collie::expected;
using collie::TaskContextPtr;
using collie::logic::ExistingSharedList;
using collie::logic::ExistingSharedLists;
using collie::logic::GetSharedLists;
using collie::logic::ListId;
using collie::server::GetSharedListsHandler;
using collie::server::Uid;
using collie::tests::MockStream;

struct GetSharedListsMock : GetSharedLists {
    MOCK_METHOD(expected<ExistingSharedLists>, call, (const TaskContextPtr&, const logic::Uid&), (const));
    expected<ExistingSharedLists> operator()(const TaskContextPtr& context,
            const logic::Uid& uid) const override {
        return call(context, uid);
    }
};

struct TestServerHandlersGetListsHandler : TestWithTaskContext {
    const std::shared_ptr<const StrictMock<GetSharedListsMock>> impl {std::make_shared<const StrictMock<GetSharedListsMock>>()};
    const GetSharedListsHandler handler {impl};
    const Uid uid {"uid"};
    const boost::shared_ptr<StrictMock<MockStream>> stream {boost::make_shared<StrictMock<MockStream>>()};
    const ExistingSharedLists result{{ExistingSharedList{ListId{1}, "list name"}}};
};

TEST_F(TestServerHandlersGetListsHandler, operator_function_call_should_call_impl_operator_function_call_and_write_result_to_stream) {
    withSpawn([&] (const auto& context) {
        const InSequence s;
        EXPECT_CALL(*impl, call(context, uid.value)).WillOnce(Return(result));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, "")).WillOnce(Return());
        EXPECT_CALL(*stream, set_content_type("application/json")).WillOnce(Return());
        EXPECT_CALL(*stream, result_body(R"({"lists":[{"list_id":1,"name":"list name"}]})"));
        EXPECT_EQ(handler(uid, stream, context), expected<void>());
    });
}

} // namespace
