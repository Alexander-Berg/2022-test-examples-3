#include "settings_http_client_mock.h"

#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <internal/macs/sharpei_httpclient.h>
#include <sharpei_client/http.h>


namespace ymod_httpclient {

static bool operator ==(const request& lhs, const request& rhs) {
    return lhs.method == rhs.method && lhs.url == rhs.url && lhs.headers == rhs.headers &&
            ((lhs.body && rhs.body && *lhs.body == *rhs.body) || (!lhs.body && !rhs.body));
}

static bool operator ==(const timeouts& lhs, const timeouts& rhs) {
    return lhs.connect == rhs.connect && lhs.total == rhs.total;
}

static bool operator ==(const options& lhs, const options& rhs) {
    return lhs.reuse_connection == rhs.reuse_connection
        && lhs.timeouts == rhs.timeouts;
}

}


namespace sharpei::client::http {

static bool operator ==(const Response& lhs, const Response& rhs) {
    return lhs.code == rhs.code && lhs.body == rhs.body;
}

}


namespace {

using namespace settings;
using namespace settings::sharpei;
using namespace settings::test;

using Options = yhttp::options;
using Request = yhttp::request;
using Response = yhttp::response;
using Timeouts = ymod_httpclient::timeouts;
using SharpeiResponse = ::sharpei::client::http::Response;
using Error = boost::system::error_code;

struct MockHandler {
    MOCK_METHOD(void, call, (Error, const SharpeiResponse&), (const));
};

struct TestSharpeiClient: public testing::Test {
    const Address address {"http://sharpei", 80};
    const Timeout timeout {std::chrono::seconds(2)};
    const Timeout connectTimeout {std::chrono::seconds(2)};
    const std::string method {"/conninfo"};
    const Arguments arguments {{"mode", {"write_only"}}, {"uid", {"228"}}};
    const Headers headers {{"X-Request-Id", {"request_id"}}, {"X-Yandex-ClientType", {"client_type"}}};

    const std::shared_ptr<HttpClientMock> httpClient {std::make_shared<HttpClientMock>()};

    const SharpeiHttpClient client {httpClient, connectTimeout};

    const Request getRequest = Request::GET(
        "http://sharpei:80/conninfo?mode=write_only&uid=228",
        "X-Request-Id: request_id\r\n"
        "X-Yandex-ClientType: client_type\r\n"
    );
    const bool keepAlive = true;
    const std::string requestId {"uniq_id"};
    const Response getResponse {200, {}, "body", ""};
    const SharpeiResponse sharpeiResponse {200, "body"};
    const MockHandler handle {};
};

TEST_F(TestSharpeiClient, aget_should_call_http_client_then_call_handler_with_response) {
    Options options;
    options.reuse_connection = keepAlive;
    options.timeouts.connect = connectTimeout;
    options.timeouts.total = timeout;

    EXPECT_CALL(*httpClient, async_run(testing::_, getRequest, options, testing::_))
        .WillOnce(testing::InvokeArgument<3>(yhttp::errc::success, getResponse));
    EXPECT_CALL(handle, call(Error {yhttp::errc::success}, sharpeiResponse))
        .WillOnce(testing::Return());
    client.aget(address, timeout, method, arguments, headers,
        std::bind(&MockHandler::call, &handle, std::placeholders::_1, std::placeholders::_2) , keepAlive, requestId);
}

TEST_F(TestSharpeiClient, apost_should_throw_exception) {
    EXPECT_THROW(
        client.apost(address, timeout, method, arguments, headers, "",
            std::bind(&MockHandler::call, &handle, std::placeholders::_1, std::placeholders::_2) , keepAlive, requestId),
        std::logic_error
    );
}

}
