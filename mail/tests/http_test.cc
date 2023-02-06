#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/http_getter/client/include/client.h>

using namespace testing;

namespace http_getter::tests {

constexpr unsigned firstTry = 0;
constexpr unsigned notFirstTry = 228;

TEST(HttpTest, shouldReturnPrimaryUrlOnEndpointWithoutFallback) {
    Endpoint endpoint({
        .url = "url",
        .method = "/method",
        .tries = 1
    });

    const std::string expected = endpoint.url() + endpoint.method();

    EXPECT_EQ(helpers::getUrlByTryNumber(endpoint, firstTry), expected);
    EXPECT_EQ(helpers::getUrlByTryNumber(endpoint, notFirstTry), expected);
}

TEST(HttpTest, shouldReturnPrimaryUrlFirstTimeOnEndpointWithFallback) {
    Endpoint endpoint({
        .url = "url",
        .fallback = "fallback",
        .method = "/method",
        .tries = 2
    });
    const std::string expected = endpoint.url() + endpoint.method();

    EXPECT_EQ(helpers::getUrlByTryNumber(endpoint, firstTry), expected);
}

TEST(HttpTest, shouldReturnFallbackUrlOnEndpointWithFallback) {
    Endpoint endpoint({
        .url = "url",
        .fallback = "fallback",
        .method = "/method",
        .tries = 2
    });
    const std::string expected = *endpoint.fallback() + endpoint.method();

    EXPECT_EQ(helpers::getUrlByTryNumber(endpoint, notFirstTry), expected);
}

TEST(EndpointInitTest, shouldThrowAnExceptionIfEndpointIsUninitialized) {
    EXPECT_THROW(Endpoint().url(), EndpointException);
}

TEST(EndpointInitTest, shouldNotThrowAnExceptionIfMethodIsEmpty) {
    Endpoint::Data data = {
        .url = "url",
        .method = "",
        .tries = 1
    };

    EXPECT_NO_THROW({Endpoint().setData(data);});
    EXPECT_NO_THROW({Endpoint(std::move(data));});
}

TEST(EndpointInitTest, shouldThrowAnExceptionIfThereIsNoStartingSlashInMethod) {
    Endpoint::Data data = {
        .method = "without_slash",
    };

    EXPECT_THROW(Endpoint().setData(data), EndpointException);
    EXPECT_THROW(Endpoint(std::move(data)), EndpointException);
}

TEST(EndpointInitTest, shouldThrowAnExceptionIfUrlIsEmpty) {
    Endpoint::Data data = {
        .url = "",
        .method = "/method",
    };

    EXPECT_THROW(Endpoint().setData(data), EndpointException);
    EXPECT_THROW(Endpoint(std::move(data)), EndpointException);
}

TEST(EndpointInitTest, shouldThrowAnExceptionIfFallbackIsEmpty) {
    Endpoint::Data data = {
        .url = "url",
        .fallback = "",
        .method = "/method",
    };

    EXPECT_THROW(Endpoint().setData(data), EndpointException);
    EXPECT_THROW(Endpoint(std::move(data)), EndpointException);
}

TEST(EndpointInitTest, shouldRemoveDoubleSlashFromUrlAndFallback) {
    Endpoint e({
        .url = "url/",
        .fallback = "fallback/",
        .method = "/method",
        .tries = 2
    });

    EXPECT_EQ(e.url(), "url");
    EXPECT_EQ(e.fallback(), "fallback");
}

TEST(EndpointTest, emptyTest) {
    EXPECT_TRUE(Endpoint().empty());
    EXPECT_FALSE(Endpoint({.url = "url", .method = "/method", .tries = 1}).empty());
}

TEST(EndpointFormatTest, shouldChangeMethod) {
    Endpoint e({
        .url = "url",
        .method = "/{arg_1}",
        .tries = 1
    });

    EXPECT_EQ(e.method(), "/{arg_1}");
    EXPECT_EQ(e.format(fmt::arg("arg_1", "save_draft")).method(), "/save_draft");

    e.setData({
        .url = "url",
        .method = "/{}",
        .tries = 1
    });

    EXPECT_EQ(e.method(), "/{}");
    EXPECT_EQ(e.format("save_draft").method(), "/save_draft");
}

TEST(EndpointTransformTest, shouldTransformEndpointToRequest) {
    Endpoint e({
        .url="http://yandex.ru",
        .fallback="http://google.com",
        .method="/mail",
        .tvm_service="tvm_service",
        .keep_alive=true,
        .log_response_body=true,
        .log_post_body=true,
        .timeout_ms={
            .connect=std::chrono::seconds{5},
            .total=std::chrono::seconds{5}
        },
        .tries=3
    });

    const auto check = [&](const http_getter::Request& req, const std::string& url) {
        EXPECT_EQ(url, req.request.url);
        EXPECT_EQ(e.keepAlive(), *req.options.keepAlive);
        EXPECT_EQ(e.logResponseBody(), *req.options.logResponseBody);
        EXPECT_EQ(e.logPostBody(), *req.options.logPostArgs);
        EXPECT_EQ(e.totalTimeout(), req.options.timeouts.total);
        EXPECT_EQ(e.connectTimeout(), req.options.timeouts.connect);
        EXPECT_EQ(e.tries(), *req.options.maxAttempts);
    };

    auto builder = Client(nullptr, http::headers(), [](auto, auto) { }, nullptr).toPOST(e);

    check(builder.primary(), e.url() + e.method());
    check(builder.fallback(), *e.fallback() + e.method());
    check(builder.primary(), e.url() + e.method());
    check(builder.body("").primary(), e.url() + e.method());
}

}
