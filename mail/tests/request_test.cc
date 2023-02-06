#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <http_getter/http_request.h>

namespace ymod_httpclient {

static inline bool operator ==(const request& lhs, const request& rhs) {
    return lhs.method == rhs.method && lhs.url == rhs.url && lhs.headers == rhs.headers &&
            ((lhs.body && rhs.body && *lhs.body == *rhs.body) || (!lhs.body && !rhs.body));
}

static inline bool operator ==(const timeouts& lhs, const timeouts& rhs) {
    return lhs.connect == rhs.connect && lhs.total == rhs.total;
}

} // namespace ymod_httpclient

namespace http_getter::detail {

static inline bool operator ==(const Options& lhs, const Options& rhs) {
    return (lhs.logPostArgs == rhs.logPostArgs) &&
           (lhs.logHeaders == rhs.logHeaders) &&
           (lhs.keepAlive == rhs.keepAlive) &&
           (lhs.timeouts == rhs.timeouts) &&
           (lhs.maxAttempts == rhs.maxAttempts);
}

static inline bool operator ==(const Request& lhs, const Request& rhs) {
    return lhs.request == rhs.request && lhs.options == rhs.options;
}

} // namespace http_getter::detail

struct TestRequest: public ::testing::Test {
    TestRequest()
        : ::testing::Test() { }

    virtual ~TestRequest() { }

    void SetUp() override { 
        url = "mail.yandex.ru/heh?i=super";
        body = "body";
        getArguments.add("get_uid", "13");
        getArguments.add("get_mid", "42");
        postArguments.add("post_uid", "13");
        postArguments.add("post_mid", "42");
        headers.add("X-Header-1", "1");
        headers.add("X-Header-2", "2");
    }

    std::string url;
    HttpArguments getArguments;
    HttpArguments postArguments;
    http::headers headers;
    std::string body;
};

namespace http_getter::testing {

using namespace std::string_literals;

TEST_F(TestRequest, must_merge_get_arguments) {
    const yhttp::request expectedRequest = yhttp::request::POST(
        "mail.yandex.ru/heh?i=super&WE=x100500_super&get_mid=42&get_uid=13&you=x2_super",
        "body"
    );
    const Request postRequest = post(url)
            .getArgs("you"_arg="x2_super"s, "args"_arg=getArguments)
            .getArgs("WE"_arg="x100500_super"s)
            .body(body)
            .make();

    ASSERT_EQ(postRequest.request, expectedRequest);
}

TEST_F(TestRequest, must_merge_optional_non_empty_get_arguments) {
    const yhttp::request expectedRequest = yhttp::request::POST(
        "mail.yandex.ru/heh?i=super&opt1=value1&opt2=",
        "body"
    );

    const Request postRequest = post(url)
            .getArgs(
                "opt1"_arg=std::make_optional(std::string("value1")),
                "opt2"_arg=std::make_optional(std::string())
            )
            .body(body)
            .make();

    ASSERT_EQ(postRequest.request, expectedRequest);
}

TEST_F(TestRequest, must_skip_optional_empty_get_arguments) {
    const yhttp::request expectedRequest = yhttp::request::POST(
        "mail.yandex.ru/heh?i=super",
        "body"
    );

    const Request postRequest = post(url)
            .getArgs(
                "opt1"_arg=std::optional<std::string>(),
                "opt2"_arg=std::nullopt
            )
            .body(body)
            .make();

    ASSERT_EQ(postRequest.request, expectedRequest);
}

TEST_F(TestRequest, must_merge_empty_get_arguments) {
    HttpArguments emptyGetArguments;
    const yhttp::request expectedRequest = yhttp::request::POST(
        "mail.yandex.ru/heh?i=super",
        "body"
    );
    const Request postRequest = post(url)
            .getArgs("args"_arg=emptyGetArguments)
            .body(body)
            .make();

    ASSERT_EQ(postRequest.request, expectedRequest);
}

TEST_F(TestRequest, must_throw_exception_when_merging_get_arguments_with_common_element) {
    EXPECT_THROW(
        post(url).getArgs("get_uid"_arg="13*1"s, "we"_arg="x100500_super"s, "args"_arg=getArguments),
        std::logic_error
    );
}

TEST_F(TestRequest, must_throw_exception_when_adding_argument_twice) {
    EXPECT_THROW(
        post(url).getArgs("you"_arg="x2_super"s, "we"_arg="x100500_super"s, "args"_arg=getArguments, "you"_arg="11/10_super"s),
        std::logic_error
    );
}

TEST_F(TestRequest, must_merge_headers) {
    const yhttp::request expectedRequest = yhttp::request::POST(
        "mail.yandex.ru/heh?i=super&get_mid=42&get_uid=13",
        "X-Header-1: 1\r\n"
        "X-Header-2: 2\r\n"
        "X-Request-Id: requestId\r\n"
        "X-XXX: YYY\r\n",
        "body"
    );
    const Request postRequest = post(url)
            .getArgs("args"_arg=getArguments)
            .headers(requestId="requestId"s, "hdrs"_hdr=headers)
            .body(body)
            .headers("X-XXX"_hdr = "YYY"s)
            .make();

    ASSERT_EQ(postRequest.request, expectedRequest);
}

TEST_F(TestRequest, must_throw_exception_when_merging_headers_with_common_element) {
    EXPECT_THROW(
        post(url).getArgs("args"_arg=getArguments).headers("request_id"_hdr="requestId"s, "X-Header-1"_hdr="2"s, "hdrs"_hdr=headers),
        std::logic_error
    );
}

TEST_F(TestRequest, must_throw_exception_when_adding_header_twice) {
    EXPECT_THROW(
        post(url).getArgs("args"_arg=getArguments).headers("header"_hdr="YYY"s, "request_id"_hdr="requestId"s, "hdrs"_hdr=headers, "header"_hdr="YY+Y"s),
        std::logic_error
    );
}

TEST_F(TestRequest, must_put_post_arguments_in_body) {
    const yhttp::request expectedRequest = yhttp::request::POST(
        "mail.yandex.ru/heh?i=super&get_mid=42&get_uid=13",
        "X-Header-1: 1\r\n"
        "X-Header-2: 2\r\n",
        "post_mid=42&post_uid=13"
    );
    const Request postRequest = post(url)
            .getArgs("args"_arg=getArguments)
            .headers("hdrs"_hdr=headers)
            .postArgs(postArguments)
            .make();

    ASSERT_EQ(postRequest.request, expectedRequest);
}

TEST_F(TestRequest, must_make_post_request) {
    const yhttp::request expectedRequest = yhttp::request::POST(
        "mail.yandex.ru/heh?i=super&WE=x100500_super&X-XXX=heh&get_mid=42&get_uid=13&you=x2_super",
        "X-Header-1: 1\r\n"
        "X-Header-2: 2\r\n"
        "X-Request-Id: requestId\r\n"
        "X-XXX: YYY\r\n",
        "body"
    );
    const Request postRequest = post(url)
            .getArgs("X-XXX"_arg="heh"s, "you"_arg="x2_super"s, "WE"_arg="x100500_super"s, "args"_arg=getArguments)
            .headers(requestId="requestId"s, "X-XXX"_hdr="YYY"s, "hdrs"_hdr=headers)
            .body(body)
            .make();

    ASSERT_EQ(postRequest.request.url, expectedRequest.url);
    ASSERT_EQ(postRequest.request, expectedRequest);
}

TEST_F(TestRequest, must_make_get_request) {
    const yhttp::request expectedRequest = yhttp::request::GET(
        "mail.yandex.ru/heh?i=super&WE=x100500_super&X-XXX=heh&get_mid=42&get_uid=13&you=x2_super",
        "X-Header-1: 1\r\n"
        "X-Header-2: 2\r\n"
        "X-Request-Id: requestId\r\n"
        "X-XXX: YYY\r\n"
    );
    const Request getRequest = get(url)
            .getArgs("X-XXX"_arg="heh"s, "you"_arg="x2_super"s, "WE"_arg="x100500_super"s, "args"_arg=getArguments)
            .headers(requestId="requestId"s, "X-XXX"_hdr="YYY"s, "hdrs"_hdr=headers)
            .make();

    ASSERT_EQ(getRequest.request, expectedRequest);
}

TEST_F(TestRequest, must_make_head_request) {
    const yhttp::request expectedRequest = yhttp::request::HEAD(
        "mail.yandex.ru/heh?i=super&WE=x100500_super&X-XXX=heh&get_mid=42&get_uid=13&you=x2_super",
        "X-Header-1: 1\r\n"
        "X-Header-2: 2\r\n"
        "X-Request-Id: requestId\r\n"
        "X-XXX: YYY\r\n"
    );
    const Request headRequest = head(url)
            .getArgs("X-XXX"_arg="heh"s, "you"_arg="x2_super"s, "WE"_arg="x100500_super"s, "args"_arg=getArguments)
            .headers(requestId="requestId"s, "X-XXX"_hdr="YYY"s, "hdrs"_hdr=headers)
            .make();

    ASSERT_EQ(headRequest.request, expectedRequest);
}

TEST_F(TestRequest, must_make_post_request_without_body) {
    const yhttp::request expectedRequest = yhttp::request::POST(
        "mail.yandex.ru/heh?i=super&WE=x100500_super&X-XXX=heh&get_mid=42&get_uid=13&you=x2_super",
        "X-Header-1: 1\r\n"
        "X-Header-2: 2\r\n"
        "X-Request-Id: requestId\r\n"
        "X-XXX: YYY\r\n",
        ""
    );
    const Request postRequest = post(url)
            .getArgs("X-XXX"_arg="heh"s, "you"_arg="x2_super"s, "WE"_arg="x100500_super"s, "args"_arg=getArguments)
            .headers(requestId="requestId"s, "X-XXX"_hdr="YYY"s, "hdrs"_hdr=headers)
            .make();

    ASSERT_EQ(postRequest.request, expectedRequest);
}

TEST_F(TestRequest, must_make_post_request_with_options) {
    yhttp::request request = yhttp::request::POST(
        "mail.yandex.ru/heh?i=super&WE=x100500_super&X-XXX=heh&get_mid=42&get_uid=13&you=x2_super",
        "X-Header-1: 1\r\n"
        "X-Header-2: 2\r\n"
        "X-Request-Id: requestId\r\n"
        "X-XXX: YYY\r\n",
        "body"
    );
    ymod_httpclient::timeouts timeouts;
    timeouts.total = std::chrono::seconds(2);
    timeouts.connect = std::chrono::seconds(1);
    Options option {{true}, {false}, {}, {timeouts}, {}, {true}};
    Request expectedRequest {std::move(request), std::move(option)};
    const Request postRequest = post(url)
            .getArgs("X-XXX"_arg="heh"s, "you"_arg="x2_super"s, "WE"_arg="x100500_super"s, "args"_arg=getArguments)
            .headers(requestId="requestId"s, "X-XXX"_hdr="YYY"s, "hdrs"_hdr=headers)
            .logPostArgs(true)
            .logHeaders(false)
            .timeouts(std::chrono::seconds(2), std::chrono::seconds(1))
            .body(body)
            .make();

    ASSERT_EQ(postRequest, expectedRequest);
}

TEST_F(TestRequest, must_set_shared_ptr_as_body) {
    auto body = boost::make_shared<std::string>("body");
    const yhttp::request expectedRequest = yhttp::request {
        .method=yhttp::request::method_t::POST,
        .url=url,
        .headers="",
        .body=body,
        .attempt=0
    };
    const Request postRequest = post(url)
            .body(body)
            .make();

    ASSERT_EQ(postRequest.request, expectedRequest);
}

} // namespace http_getter::testing
