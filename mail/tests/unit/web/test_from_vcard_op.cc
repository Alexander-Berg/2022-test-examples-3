#include <web/handlers/from_vcard_op.h>
#include <with_spawn.h>
#include <web/common_mocks.h>
#include <yplatform/zerocopy/streambuf.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace {

using namespace testing;
using namespace sheltie;
using namespace sheltie::web;
using namespace sheltie::tests;

struct TestFromVcardOp : public TestWithYieldCtx {
    WebContextMockPtr webCtx = std::make_shared<WebContextMock>();
    boost::shared_ptr<StrictMock<MockStream>> stream =boost::make_shared<StrictMock<MockStream>>();
    RequestLogger logger{getLogger("-", "-")};
    std::string uid{"123"};
};

TEST_F(TestFromVcardOp, should_response_internal_server_error_when_error_occured_in_python_module) {
    withSpawn([&] (YieldCtx yieldCtx) {
        auto webServerRequest = boost::make_shared<ymod_webserver::request>();
        webServerRequest->raw_request_line = "GET /ping HTTP/1.1";
        EXPECT_CALL(*stream, request()).WillOnce(Return(webServerRequest));
        EXPECT_CALL(*(webCtx->pythonModule), transformFromVcard(_, _, _)).WillOnce(Invoke([](auto, auto, YieldCtx yieldCtx) {
            *yieldCtx.ec_ = ymod_httpclient::http_error::unknown_error;
            return "";
        }));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, ""));
        EXPECT_CALL(*stream, result_body("transform from vcard error: unknown_error"));
        FromVcardOp()(yieldCtx, webCtx, logger, stream, uid);
    });
}

TEST_F(TestFromVcardOp, should_response_ok) {
    withSpawn([&] (YieldCtx yieldCtx) {
        auto webServerRequest = boost::make_shared<ymod_webserver::request>();
        webServerRequest->raw_request_line = "GET /ping HTTP/1.1";
        EXPECT_CALL(*stream, request()).WillOnce(Return(webServerRequest));
        std::string response = "response";
        EXPECT_CALL(*(webCtx->pythonModule), transformFromVcard(_, _, _)).WillOnce(Return(response));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, ""));
        EXPECT_CALL(*stream, result_body(response));
        FromVcardOp()(yieldCtx, webCtx, logger, stream, uid);
    });
}

}