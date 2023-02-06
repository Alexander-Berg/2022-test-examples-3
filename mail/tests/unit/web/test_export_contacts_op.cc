#include <web/handlers/export_contacts_op.h>
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

struct TestExportContactsOp : public TestWithYieldCtx {
    WebContextMockPtr webCtx = std::make_shared<WebContextMock>();
    boost::shared_ptr<StrictMock<MockStream>> stream = boost::make_shared<StrictMock<MockStream>>();
    RequestLogger logger = getLogger("-", "-");
    std::string uid ="123";
};

TEST_F(TestExportContactsOp, should_response_internal_server_error_when_error_occured_in_http_client) {
    withSpawn([&] (YieldCtx yieldCtx) {
        auto webServerRequest = boost::make_shared<ymod_webserver::request>();
        webServerRequest->raw_request_line = "GET /ping HTTP/1.1";
        EXPECT_CALL(*stream, request()).WillOnce(Return(webServerRequest));
        EXPECT_CALL(*(webCtx->collieClient), async_run(_, _, _)).WillOnce(Invoke([](auto, auto, YieldCtx yieldCtx) {
            *yieldCtx.ec_ = ymod_httpclient::http_error::request_timeout;
            return yhttp::response();
        }));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, ""));
        EXPECT_CALL(*stream, result_body("export contacts error: request_timeout"));
        ExportContactsOp()(yieldCtx, webCtx, logger, stream, uid);
    });
}

TEST_F(TestExportContactsOp, should_response_internal_server_error_when_collie_responsed_internal_server_error) {
    withSpawn([&] (YieldCtx yieldCtx) {
        auto webServerRequest = boost::make_shared<ymod_webserver::request>();
        webServerRequest->raw_request_line = "GET /ping HTTP/1.1";
        EXPECT_CALL(*stream, request()).WillOnce(Return(webServerRequest));
        yhttp::response response;
        response.status = 500;
        response.reason = "Internal Server Error";
        EXPECT_CALL(*(webCtx->collieClient), async_run(_, _, _)).WillOnce(Return(response));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, ""));
        EXPECT_CALL(*stream, result_body("export contacts error: Internal Server Error"));
        ExportContactsOp()(yieldCtx, webCtx, logger, stream, uid);
    });
}

TEST_F(TestExportContactsOp, should_response_internal_server_error_when_error_occured_in_python_module) {
    withSpawn([&] (YieldCtx yieldCtx) {
        auto webServerRequest = boost::make_shared<ymod_webserver::request>();
        webServerRequest->raw_request_line = "GET /ping HTTP/1.1";
        EXPECT_CALL(*stream, request()).WillOnce(Return(webServerRequest));
        yhttp::response response;
        response.status = 200;
        response.body = "Success";
        EXPECT_CALL(*(webCtx->collieClient), async_run(_, _, _)).WillOnce(Return(response));
        EXPECT_CALL(*(webCtx->pythonModule), exportContacts(_, _, _)).WillOnce(Invoke([](auto, auto, YieldCtx yieldCtx) {
            *yieldCtx.ec_ = ymod_httpclient::http_error::unknown_error;
            return "";
        }));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::internal_server_error, ""));
        EXPECT_CALL(*stream, result_body("export contacts error: unknown_error"));
        ExportContactsOp()(yieldCtx, webCtx, logger, stream, uid);
    });
}

TEST_F(TestExportContactsOp, should_response_ok) {
    withSpawn([&] (YieldCtx yieldCtx) {
        auto webServerRequest = boost::make_shared<ymod_webserver::request>();
        webServerRequest->raw_request_line = "GET /ping HTTP/1.1";
        EXPECT_CALL(*stream, request()).WillOnce(Return(webServerRequest));
        yhttp::response response;
        response.status = 200;
        response.body = "Success";
        std::string contactResponse = "response";
        EXPECT_CALL(*(webCtx->collieClient), async_run(_, _, _)).WillOnce(Return(response));
        EXPECT_CALL(*(webCtx->pythonModule), exportContacts(_, _, _)).WillOnce(Return(contactResponse));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, ""));
        EXPECT_CALL(*stream, result_body(contactResponse));
        ExportContactsOp()(yieldCtx, webCtx, logger, stream, uid);
    });
}

}