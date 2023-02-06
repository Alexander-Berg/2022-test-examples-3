#include <tests/unit/generic_operators.hpp>
#include <tests/unit/http_client_mock.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/services/sharpei/sharpei_http_client.hpp>

namespace sharpei::client::http {

static inline bool operator ==(const Response& lhs, const Response& rhs) {
    return lhs.code == rhs.code && lhs.body == rhs.body;
}

} // namespace sharpei::client::http

namespace {

using namespace testing;

using collie::services::sharpei::SharpeiHttpClient;
using collie::services::sharpei::Address;
using collie::services::sharpei::Arguments;
using collie::services::sharpei::Headers;
using collie::services::sharpei::ResponseHandler;
using collie::services::sharpei::Timeout;
using collie::tests::GetHttpClientMockWrapper;
using collie::tests::HttpClientMock;
using sharpei::client::ErrorCode;

using Options = yhttp::options;
using Request = yhttp::request;
using Response = yhttp::response;
using Timeouts = ymod_httpclient::timeouts;
using SharpeiResponse = sharpei::client::http::Response;

Timeouts makeTimeouts() {
    Timeouts result;
    result.connect = std::chrono::seconds(1);
    result.total = std::chrono::seconds(2);
    return result;
}

Options makeOptions() {
    Options result;
    result.reuse_connection = true;
    result.timeouts = makeTimeouts();
    return result;
}

struct HandlerMock {
    struct Impl {
        MOCK_METHOD(void, call, (ErrorCode, SharpeiResponse), (const));
    };

    std::shared_ptr<StrictMock<Impl>> impl = std::make_shared<StrictMock<Impl>>();

    template <class ... Args>
    void operator ()(Args&& ... args) const {
        return impl->call(std::forward<Args>(args) ...);
    }
};

struct TestServicesSharpeiHttpClient : Test {
    const Address address {"sharpei", 80};
    const Timeout timeout {std::chrono::seconds(2)};
    const std::string method {"/conninfo"};
    const Arguments arguments {{"mode", {"write_only"}}, {"uid", {"42"}}};
    const Headers headers {{"X-Request-Id", {"request_id"}}, {"X-Yandex-ClientType", {"client_type"}}};
    const std::shared_ptr<StrictMock<HttpClientMock>> httpClient {std::make_shared<StrictMock<HttpClientMock>>()};
    const GetHttpClientMockWrapper getHttpClient {};
    const SharpeiHttpClient client {getHttpClient, std::chrono::seconds(1)};
    const Options options {makeOptions()};
    const Request getRequest = Request::GET(
        "http://sharpei:80/conninfo?mode=write_only&uid=42",
        "X-Request-Id: request_id\r\n"
        "X-Yandex-ClientType: client_type\r\n"
    );
    const HandlerMock handler {};
    const bool keepAlive = true;
    const std::string requestId {"uniq_id"};
    const Response getResponse {200, {}, "body", ""};
    const SharpeiResponse expectedSharpeiResponse {200, "body"};
};

TEST_F(TestServicesSharpeiHttpClient, aget_should_call_http_client_then_call_handler_with_response) {
    const InSequence s;

    EXPECT_CALL(*getHttpClient.impl, call()).WillOnce(Return(httpClient));
    EXPECT_CALL(*httpClient, async_run(_, getRequest, options, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, getResponse));
    EXPECT_CALL(*handler.impl, call(ErrorCode(yhttp::errc::success), expectedSharpeiResponse))
        .WillOnce(Return());

    client.aget(address, timeout, method, arguments, headers, handler, keepAlive, requestId);

    EXPECT_TRUE(Mock::VerifyAndClearExpectations(httpClient.get()));
}

TEST_F(TestServicesSharpeiHttpClient, apost_should_throw_exception) {
    EXPECT_THROW(
        client.apost(address, timeout, method, arguments, headers, "", handler, keepAlive, requestId),
        std::logic_error
    );
}

} // namespace
