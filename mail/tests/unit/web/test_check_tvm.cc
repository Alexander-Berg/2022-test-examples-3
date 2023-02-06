#include <web/check_tvm.h>
#include <with_spawn.h>
#include <web/common_mocks.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace {

using namespace testing;
using namespace sheltie;
using namespace sheltie::web;
using namespace sheltie::tests;
using TvmAction = tvm_guard::Action;
using TvmResponse = tvm_guard::Response;

struct OperationHandlerMock {
    void operator()(
        YieldCtx /*yieldCtx*/,
        WebContextMockPtr /*webCtx*/,
        RequestLogger /*logger*/,
        StreamPtr stream,
        const std::string& /*uid*/)
    {
        stream->result(ymod_webserver::codes::ok, "");
    }
};

struct TestCheckTvmOp : public TestWithYieldCtx {
    TestCheckTvmOp() {
        std::string serviceTicketHeader;
        boost::to_lower_copy(std::back_inserter(serviceTicketHeader), tvm_guard::header::serviceTicket());
        request->headers[serviceTicketHeader] = serviceTicket;

        std::string userTicketHeader;
        boost::to_lower_copy(std::back_inserter(userTicketHeader), tvm_guard::header::userTicket());
        request->headers[userTicketHeader] = userTicket;

        request->url.path = {path};
    }

    CheckTvmOp op{};
    WebContextMockPtr webCtx = std::make_shared<WebContextMock>();
    boost::shared_ptr<StrictMock<MockStream>> stream{boost::make_shared<StrictMock<MockStream>>()};
    boost::shared_ptr<ymod_webserver::request> request{boost::make_shared<ymod_webserver::request>()};
    boost::shared_ptr<ymod_webserver::context> context = boost::make_shared<ymod_webserver::context>();
    std::string serviceTicket{"service_ticket"};
    std::string userTicket{"user_ticket"};
    std::string path{"path"};
    std::string uid{"123"};
};

TEST_F(TestCheckTvmOp, should_return_bad_tvm_ticket) {
    withSpawn([&] (YieldCtx yieldCtx) {
        EXPECT_CALL(*stream, ctx()).WillOnce(Return(context));
        EXPECT_CALL(*stream, request()).WillOnce(Return(request));
        TvmResponse response;
        response.action = TvmAction::reject;
        EXPECT_CALL(*webCtx->tvmGuard, check(
            std::string("/" + path),
            uid,
            std::optional<std::string_view>(serviceTicket),
            std::optional<std::string_view>(userTicket))).WillOnce(Return(response));
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::unauthorized, ""));
        EXPECT_CALL(*stream, result_body("bad tvm ticket"));
        op(yieldCtx, webCtx, stream, uid, OperationHandlerMock{});
    });
}

TEST_F(TestCheckTvmOp, should_call_handler) {
    withSpawn([&] (YieldCtx yieldCtx) {
        EXPECT_CALL(*stream, ctx()).WillOnce(Return(context));
        EXPECT_CALL(*stream, request()).WillOnce(Return(request));
        TvmResponse response;
        response.action = TvmAction::accept;
        EXPECT_CALL(*webCtx->tvmGuard, check(
            std::string("/" + path),
            uid,
            std::optional<std::string_view>(serviceTicket),
            std::optional<std::string_view>(userTicket))).WillOnce(Return(response));
        auto handler = OperationHandlerMock{};
        EXPECT_CALL(*stream, set_code(ymod_webserver::codes::ok, ""));
        EXPECT_CALL(*stream, result_body(""));
        op(yieldCtx, webCtx, stream, uid, handler);
    });
}

}
