#include "test_with_yield_context.h"
#include "http_client_mock.h"
#include "tvm_client.h"
#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <internal/sanitizer.h>


namespace mail_getter {
    static bool operator ==(const SanitizerParsedResponse& lhs, const SanitizerParsedResponse& rhs) {
        return lhs.html == rhs.html;
    }
}


namespace {

using namespace testing;
using namespace msg_body;
using task_context_ptr = yhttp::simple_call::task_context_ptr;

using Options = ymod_httpclient::cluster_call::options;
using Request = yhttp::request;
using Response = yhttp::response;
using Timeouts = ymod_httpclient::timeouts;
using mail_getter::SanitizerParsedResponse;

std::string getFakeServiceTicket(const std::string&) {
    return "service";
}

std::string getBadFakeServiceTicket(const std::string&) {
    throw std::runtime_error("");
}

Timeouts makeTimeouts() {
    Timeouts result;
    result.connect = std::chrono::seconds(0);
    result.total = std::chrono::seconds(0);
    return result;
}

struct TestSanitizer : public TestWithYieldContext {
    std::string requestBody = "\r\n--ccc\r\n"
                "Content-Type: application/json; charset=utf-8\r\n\r\n"
                "null\r\n"
                "--ccc\r\n"
                "Content-Type: text/html; charset=utf-8\r\n\r\n"
                "<div>Hola</div>\r\n"
                "--ccc--";
    const Request postRequest = Request::POST(
        "sanitizer.yandex.ru?id=&mid=2&mimetype=text%2Fhtml&s=&stid=3&uid=1",
        "X-Request-Id: \r\n"
        "X-Ya-Service-Ticket: service\r\n",
        "\r\n--ccc\r\n"
        "Content-Type: application/json; charset=utf-8\r\n\r\n"
        "null\r\n"
        "--ccc\r\n"
        "Content-Type: text/html; charset=utf-8\r\n\r\n"
        "<div>Hola</div>\r\n"
        "--ccc--"
    );
    std::string requestBadBody = "bad data";
    const Request postBadRequest = Request::POST(
        "sanitizer.yandex.ru?from=vasyapupkin%40yandex.ru&id=&mid=2&mimetype=text%2Fplain&s=&stid=3&system=yes&uid=1",
        "X-Request-Id: \r\n"
        "X-Ya-Service-Ticket: service\r\n",
        "bad data"
    );

    const PaLog paLog = PaLog {"Sanitizer"};

    const std::string uid = "1";
    const std::string mid = "2";
    const std::string stid = "3";

    const std::shared_ptr<ClusterClientMock> clusterClient = GetMockedClusterClient();
    const std::shared_ptr<TvmClientMock> tvmClient = std::make_shared<TvmClientMock>();
    const GetClusterClientMockWrapper getClusterClient {};
    GetServiceTicket getServiceTicket = [this](const std::string& name) {
        return this->tvmClient->invoke(name); };

    const Options options {{false, {}, true, makeTimeouts()}, {}};
};

TEST_F(TestSanitizer, for_successful_request_should_return_parse_response) {
    withSpawn([&] (YieldCtx yc) {
        Response sanitizerResponse {200, {{"Content-Type", "multipart/mixed; boundary=ccc"}}, requestBody, {}};
        const auto parseResponse = SanitizerParsedResponse {"<div>Hola</div>", {}};
        EXPECT_CALL(*tvmClient, invoke("sanitizer")).WillOnce(Invoke(getFakeServiceTicket));
        EXPECT_CALL(*getClusterClient.impl, call()).WillOnce(Return(clusterClient));
        EXPECT_CALL(*clusterClient, async_run(_, postRequest, options, _))
            .WillOnce(InvokeArgument<3>(
                yhttp::errc::success,
                sanitizerResponse
            ));
        Sanitizer sanitizer_with_retries {{"sanitizer.yandex.ru", "", "", "", "", {}, {}, true, 1, false}, false, false, "", {}, std::move(getServiceTicket), yc, getClusterClient};
        EXPECT_EQ(sanitizer_with_retries.sanitizeEnd(requestBody, uid, mid, stid, "text/html", std::nullopt, std::nullopt, paLog),
            parseResponse);
    });
}

TEST_F(TestSanitizer, for_bad_data_in_response_should_throw_exception) {
    withSpawn([&] (YieldCtx yc) {
        Response sanitizerResponse {200, {{"Content-Type", "multipart/mixed; boundary=ccc"}}, requestBadBody, {}};
        EXPECT_CALL(*tvmClient, invoke("sanitizer")).WillOnce(Invoke(getFakeServiceTicket));
        EXPECT_CALL(*getClusterClient.impl, call()).WillOnce(Return(clusterClient));
        EXPECT_CALL(*clusterClient, async_run(_, postBadRequest, options, _))
            .WillOnce(InvokeArgument<3>(
                yhttp::errc::success,
                sanitizerResponse
            ));
        Sanitizer sanitizer_with_retries {{"sanitizer.yandex.ru", "", "", "", "", {}, {}, true, 1, false}, false, false, "", {}, std::move(getServiceTicket), yc, getClusterClient};
        EXPECT_THROW(sanitizer_with_retries.sanitizeEnd(requestBadBody, uid, mid, stid, "text/plain", "vasyapupkin@yandex.ru", "yes", paLog),
            std::runtime_error);
    });
}

TEST_F(TestSanitizer, for_unsuccessful_request_should_throw_exception) {
    withSpawn([&] (YieldCtx yc) {
        Response sanitizerResponseWithError {500, {{"Content-Type", "multipart/mixed; boundary=ccc"}}, requestBody, {}};
        EXPECT_CALL(*tvmClient, invoke("sanitizer")).WillOnce(Invoke(getFakeServiceTicket));
        EXPECT_CALL(*getClusterClient.impl, call()).WillOnce(Return(clusterClient));
        EXPECT_CALL(*clusterClient, async_run(_, postRequest, options, _))
            .WillOnce(InvokeArgument<3>(
                yhttp::errc::success,
                sanitizerResponseWithError
            ));
        Sanitizer sanitizer_with_retries {{"sanitizer.yandex.ru", "", "", "", "", {}, {}, true, 0, false}, false, false, "", {}, std::move(getServiceTicket), yc, getClusterClient};
        EXPECT_THROW(sanitizer_with_retries.sanitizeEnd(requestBody, uid, mid, stid, "text/html", std::nullopt, std::nullopt, paLog),
            std::runtime_error);
    });
}

TEST_F(TestSanitizer, sanitizer_should_throw_an_exception_in_case_of_tvm_client_error) {
    withSpawn([&] (YieldCtx yc) {
        EXPECT_CALL(*tvmClient, invoke("sanitizer")).WillOnce(Invoke(getBadFakeServiceTicket));
        Sanitizer sanitizer_with_retries {{"sanitizer.yandex.ru", "", "", "", "", {}, {}, true, 1, false}, false, false, "", {}, std::move(getServiceTicket), yc, getClusterClient};
        EXPECT_THROW(sanitizer_with_retries.sanitizeEnd(requestBody, uid, mid, stid, "", std::nullopt, std::nullopt, paLog),
            std::runtime_error);
    });
}

} // namespace
