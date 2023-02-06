#include <src/server/handlers/carddav_propfind_handler.hpp>

#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>

namespace {

namespace logic = collie::logic;

using collie::expected;
using collie::server::CarddavPropfindHandler;
using collie::server::Uid;
using collie::TaskContextPtr;
using collie::tests::MockStream;
using logic::CarddavPropfind;
using logic::CarddavPropfindResult;

struct CarddavPropfindMock : CarddavPropfind {
    MOCK_METHOD(expected<CarddavPropfindResult>, call, (const TaskContextPtr&, const logic::Uid&), (const));

    expected<CarddavPropfindResult> operator()(const TaskContextPtr& context,
            const logic::Uid& uid) const override {
        return call(context, uid);
    }
};

class TestServerHandlersCarddavPropfindHandler : public TestWithTaskContext {
protected:
    const std::shared_ptr<const StrictMock<CarddavPropfindMock>> impl{
            std::make_shared<const StrictMock<CarddavPropfindMock>>()};
    const CarddavPropfindHandler handler{impl};
    const Uid uid{"uid"};
    const boost::shared_ptr<StrictMock<MockStream>> stream{boost::make_shared<StrictMock<MockStream>>()};
};

TEST_F(TestServerHandlersCarddavPropfindHandler,
        operator_call_should_call_impl_operator_call_and_write_result_to_stream) {
    withSpawn([this](const auto& context) {
        const InSequence sequence;

        const std::string tag0{"68-63"};
        const std::string tag1{"69-64"};
        const CarddavPropfindResult result {
            {{"name0.vcf", "Name0", R"(")" + tag0 + R"(")"}, {"name1.vcf", "Name1", R"(")" + tag1 + R"(")"}}
        };

        EXPECT_CALL(*impl, call(context, uid.value)).WillOnce(Return(result));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, ""));
        EXPECT_CALL(*stream, set_content_type("application/xml"));

        const std::string body {
            R"(<?xml version="1.0" encoding="UTF-8"?>)"
            "\n"
            R"(<response>)"
            R"(<contact>)"
            R"(<uri>)" + *result.contact[0].uri + R"(</uri>)"
            R"(<displayname>)" + result.contact[0].displayname + R"(</displayname>)"
            R"(<etag>&quot;)" + tag0 + R"(&quot;</etag>)"
            R"(</contact>)"
            R"(<contact>)"
            R"(<uri>)" + *result.contact[1].uri + R"(</uri>)"
            R"(<displayname>)" + result.contact[1].displayname + R"(</displayname>)"
            R"(<etag>&quot;)" + tag1 + R"(&quot;</etag>)"
            R"(</contact>)"
            R"(</response>)"
            "\n"
        };

        EXPECT_CALL(*stream, result_body(body));
        EXPECT_EQ(expected<void>{}, handler(uid, stream, context));
    });
}

} // namespace
