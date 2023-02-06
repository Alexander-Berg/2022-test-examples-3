#include <src/server/handlers/carddav_delete_handler.hpp>

#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>

namespace {

namespace logic = collie::logic;

using collie::expected;
using collie::server::CarddavDeleteHandler;
using collie::server::Uid;
using collie::server::Uri;
using collie::TaskContextPtr;
using collie::tests::MockStream;
using logic::CarddavDelete;

struct CarddavDeleteMock : CarddavDelete {
    MOCK_METHOD(expected<void>, call, (const TaskContextPtr&, const logic::Uid&, const std::string&), (const));

    expected<void> operator()(const TaskContextPtr& context, const logic::Uid& uid,
            const std::string& uri) const override {
        return call(context, uid, uri);
    }
};

class TestServerHandlersCarddavDeleteHandler : public TestWithTaskContext {
protected:
    const std::shared_ptr<const StrictMock<CarddavDeleteMock>> impl{
            std::make_shared<const StrictMock<CarddavDeleteMock>>()};
    const CarddavDeleteHandler handler{impl};
    const Uid queryUid{"uid"};
    const Uri queryUri{"a.vcf"};
    const boost::shared_ptr<StrictMock<MockStream>> stream{boost::make_shared<StrictMock<MockStream>>()};
};

TEST_F(TestServerHandlersCarddavDeleteHandler,
        operator_call_must_call_impl_operator_call_and_write_result_to_stream) {
    withSpawn([this](const auto& context) {
        const InSequence sequence;
        EXPECT_CALL(*impl, call(context, queryUid.value, queryUri.value)).WillOnce(Return(expected<void>{}));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, ""));
        ASSERT_TRUE(handler(queryUid, queryUri, stream, context));
    });
}

} // namespace
