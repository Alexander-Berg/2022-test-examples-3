#include "utils.hpp"

#include <src/server/handlers/carddav_multiget_handler.hpp>

#include <tests/unit/test_with_task_context.hpp>
#include <tests/unit/ymod_webserver_mocks.hpp>

namespace {

namespace logic = collie::logic;

using collie::error_code;
using collie::expected;
using collie::server::CarddavMultigetHandler;
using collie::server::Error;
using collie::server::make_error_code;
using collie::server::Uid;
using collie::TaskContextPtr;
using collie::tests::MockStream;
using collie::tests::makeRequestWithRawBody;
using logic::CarddavMultiget;
using logic::CarddavMultigetResult;

struct CarddavMultigetMock : CarddavMultiget {
    MOCK_METHOD(expected<CarddavMultigetResult>, call, (const TaskContextPtr&, const logic::Uid&,
            std::vector<std::string>), (const));

    expected<CarddavMultigetResult> operator()(const TaskContextPtr& context, const logic::Uid& uid,
            std::vector<std::string> uris) const override {
        return call(context, uid, std::move(uris));
    }
};

class TestServerHandlersCarddavMultigetHandler : public TestWithTaskContext {
protected:
    const std::shared_ptr<const StrictMock<CarddavMultigetMock>> impl{
            std::make_shared<const StrictMock<CarddavMultigetMock>>()};
    const CarddavMultigetHandler handler{impl};
    const Uid uid{"uid"};
    const boost::shared_ptr<StrictMock<MockStream>> stream{boost::make_shared<StrictMock<MockStream>>()};
    yplatform::zerocopy::streambuf buffer;
};

TEST_F(TestServerHandlersCarddavMultigetHandler,
        operator_call_should_call_impl_operator_call_and_write_result_to_stream) {
    withSpawn([this](const auto& context) {
        const InSequence sequence;

        std::vector<std::string> uris{"a.vcf", "b.vcf"};
        const std::string requestBody {
            R"(<?xml version="1.0" encoding="UTF-8"?>)"
            "\n"
            R"(<request>)"
            R"(<href>)" + uris[0] + R"(</href>)"
            R"(<href>)" + uris[1] + R"(</href>)"
            R"(</request>)"
            "\n"
        };

        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithRawBody(requestBody, buffer)));

        const int statusOk{200};
        const int statusResourceNotFound{404};
        const std::string tag{"68-63"};
        const std::string vcard{"vcard"};
        const CarddavMultigetResult result {
            {{uris[0], statusOk, R"(")" + tag + R"(")", vcard}, {uris[1], statusResourceNotFound, {}, {}}}
        };

        EXPECT_CALL(*impl, call(context, uid.value, uris)).WillOnce(Return(result));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, ""));
        EXPECT_CALL(*stream, set_content_type("application/xml"));

        const std::string responseBody {
            R"(<?xml version="1.0" encoding="UTF-8"?>)"
            "\n"
            R"(<multiget-result found="2">)"
            R"(<contact>)"
            R"(<uri>)" + result.contact[0].uri + R"(</uri>)"
            R"(<status>)" + std::to_string(result.contact[0].status) + R"(</status>)"
            R"(<etag>&quot;)" + tag + R"(&quot;</etag>)"
            R"(<vcard>)" + *result.contact[0].vcard + R"(</vcard>)"
            R"(</contact>)"
            R"(<contact>)"
            R"(<uri>)" + result.contact[1].uri + R"(</uri>)"
            R"(<status>)" + std::to_string(result.contact[1].status) + R"(</status>)"
            R"(</contact>)"
            R"(</multiget-result>)"
            "\n"
        };

        EXPECT_CALL(*stream, result_body(responseBody));
        EXPECT_EQ(expected<void>{}, handler(uid, stream, context));
    });
}

TEST_F(TestServerHandlersCarddavMultigetHandler, operator_call_must_return_error_when_invalid_request) {
    withSpawn([this](const auto& context) {
        std::string uri{"a.vcf"};
        const std::string requestBody {
            R"(<?xml version="1.0" encoding="UTF-8"?>)"
            "\n"
            R"(<incorrect_tag>)"
            R"(<href>)" + uri + R"(</href>)"
            R"(</incorrect_tag>)"
            "\n"
        };

        EXPECT_CALL(*stream, request()).WillOnce(Return(makeRequestWithRawBody(requestBody, buffer)));

        const auto result{handler(uid, stream, context)};
        ASSERT_FALSE(result);
        EXPECT_EQ((error_code{make_error_code(Error::invalidRequestBodyFormat), {}}), result.error());
    });
}

} // namespace
