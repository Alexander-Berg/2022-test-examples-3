#include "nsls_test.h"
#include "mocks/ymod_httpclient.h"
#include "mocks/ymod_tvm.h"
#include <mail/notsolitesrv/tests/unit/fakes/context.h>
#include <mail/notsolitesrv/tests/unit/util/ymod_httpclient.h>
#include <mail/notsolitesrv/src/errors.h>
#include <mail/notsolitesrv/src/http/client.h>

#include <yplatform/zerocopy/streambuf.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

using namespace testing;
using namespace NNotSoLiteSrv;

struct THttpClientTest: public TNslsTest {
    void SetUp() override {
        Ctx = GetContext();

        yplatform::ptree pt;
        pt.add<unsigned int>("attempts", 2);
        pt.add<std::string>("url", "");
        Config = std::make_unique<NConfig::THttpCall>(pt);

        YHttpClientMock = std::make_shared<StrictMock<TYmodHttpClientMock>>();
        HttpClient = NHttp::CreateClient(Ctx, "client", *Config, YHttpClientMock, std::shared_ptr<ymod_tvm::tvm2_module>());
    }

    TContextPtr Ctx;
    NConfig::THttpCallUptr Config;
    NHttp::TClientPtr HttpClient;
    std::shared_ptr<TYmodHttpClientMock> YHttpClientMock;

    const yhttp::response ResponseEmpty{};
    const yhttp::response ResponseSuccess{
        200,
        {},
        "some body",
        "Ok"};
    const yhttp::response ResponseSuccessWithHeaders{
        200,
        {
            {"X-Yandex-Test", "passed"},
            {"X-Yandex-Test", "yeah!"}
        },
        "some body",
        "Ok"};
    const yhttp::response Response500{
        500,
        {},
        "",
        "Internal server error"};
};

struct THttpClientGetMethodTest: public THttpClientTest {
    const yhttp::request Request = yhttp::request::GET("/get_url");
    const yhttp::request RequestWithHeaders = yhttp::request::GET("/get_url", "X-Yandex-Test: in progress\r\n");
};

struct THttpClientGetMethodWithConfigHeadersTest: public THttpClientTest {
    void SetUp() override {
        Ctx = GetContext();

        yplatform::ptree pt;
        pt.add<unsigned int>("attempts", 2);
        pt.add<std::string>("url", "");
        yplatform::ptree addHeaders;
        addHeaders.add("Some-Header", "1");
        addHeaders.add("Another-Header", 2);
        pt.add_child("add_headers", addHeaders);
        Config = std::make_unique<NConfig::THttpCall>(pt);

        YHttpClientMock = std::make_shared<StrictMock<TYmodHttpClientMock>>();
        HttpClient = NHttp::CreateClient(Ctx, "client", *Config, YHttpClientMock, std::shared_ptr<ymod_tvm::tvm2_module>());
    }

    const yhttp::request Request = yhttp::request::GET("/get_url", "Another-Header: 2\r\nSome-Header: 1\r\n");
    const yhttp::request RequestWithHeaders = yhttp::request::GET("/get_url", "Some-Header: 1\r\nAnother-Header: 2\r\nX-Yandex-Test: in progress\r\n");
};

struct THttpClientHeadMethodTest: public THttpClientTest {
    const yhttp::request Request = yhttp::request::HEAD("/head_url");
    const yhttp::request RequestWithHeaders = yhttp::request::HEAD("/head_url", "X-Yandex-Test: in progress\r\n");
};

struct THttpClientPostMethodTest: public THttpClientTest {
    const yhttp::request Request = yhttp::request::POST("/post_url", "here comes the body");
    const yhttp::request RequestWithHeaders = yhttp::request::POST("/post_url", "X-Yandex-Test: in progress\r\n", "here comes the body");
};

TEST_F(THttpClientGetMethodTest, NoHeadersSuccess) {
    EXPECT_CALL(*YHttpClientMock, async_run(_, Request, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, ResponseSuccess));

    ExpectCallbackCalled(
        [](auto ec, auto res) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(res.status, 200);
            EXPECT_EQ(res.body, "some body");
        },
        1,
        [this](auto cb) { HttpClient->Get("/get_url", cb); }
    );
}

TEST_F(THttpClientGetMethodTest, WithHeadersSuccess) {
    EXPECT_CALL(*YHttpClientMock, async_run(_, RequestWithHeaders, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, ResponseSuccessWithHeaders));

    NHttp::THeaders hdr{{"X-Yandex-Test", "in progress"}};
    ExpectCallbackCalled(
        [](auto ec, auto res) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(res.status, 200);

            NHttp::THeaders headers{
                { "X-Yandex-Test", "passed" },
                { "X-Yandex-Test", "yeah!" }
            };
            EXPECT_EQ(res.headers, headers);
            EXPECT_EQ(res.body, "some body");
        },
        1,
        [this, hdr](auto cb) { HttpClient->Get("/get_url", hdr, cb); }
    );
}

TEST_F(THttpClientHeadMethodTest, NoHeadersSuccess) {
    EXPECT_CALL(*YHttpClientMock, async_run(_, Request, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, ResponseSuccess));

    ExpectCallbackCalled(
        [](auto ec, auto res) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(res.status, 200);
            EXPECT_EQ(res.body, "some body");
        },
        1,
        [this](auto cb) { HttpClient->Head("/head_url", cb); }
    );
}

TEST_F(THttpClientHeadMethodTest, WithHeadersSuccess) {
    EXPECT_CALL(*YHttpClientMock, async_run(_, RequestWithHeaders, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, ResponseSuccessWithHeaders));

    NHttp::THeaders hdr{{"X-Yandex-Test", "in progress"}};
    ExpectCallbackCalled(
        [](auto ec, auto res) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(res.status, 200);

            NHttp::THeaders headers{
                { "X-Yandex-Test", "passed" },
                { "X-Yandex-Test", "yeah!" }
            };
            EXPECT_EQ(res.headers, headers);
            EXPECT_EQ(res.body, "some body");
        },
        1,
        [this, hdr](auto cb) { HttpClient->Head("/head_url", hdr, cb); }
    );
}

TEST_F(THttpClientPostMethodTest, StringBodySuccess) {
    EXPECT_CALL(*YHttpClientMock, async_run(_, Request, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, ResponseSuccess));

    ExpectCallbackCalled(
        [](auto ec, auto res) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(res.status, 200);
            EXPECT_EQ(res.body, "some body");
        },
        1,
        [this](auto cb) { HttpClient->Post("/post_url", "here comes the body", cb); }
    );
}

TEST_F(THttpClientPostMethodTest, StringBodyWithHeadersSuccess) {
    EXPECT_CALL(*YHttpClientMock, async_run(_, RequestWithHeaders, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, ResponseSuccessWithHeaders));

    NHttp::THeaders hdr{{"X-Yandex-Test", "in progress"}};
    ExpectCallbackCalled(
        [](auto ec, auto res) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(res.status, 200);

            NHttp::THeaders headers{
                { "X-Yandex-Test", "passed" },
                { "X-Yandex-Test", "yeah!" }
            };
            EXPECT_EQ(res.headers, headers);
            EXPECT_EQ(res.body, "some body");
        },
        1,
        [this, hdr](auto cb) { HttpClient->Post("/post_url", "here comes the body", hdr, cb); }
    );
}

TEST_F(THttpClientPostMethodTest, ZeroCopyBodySuccess) {
    EXPECT_CALL(*YHttpClientMock, async_run(_, Request, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, ResponseSuccess));

    yplatform::zerocopy::segment zc;
    yplatform::zerocopy::streambuf buf;
    std::ostream os(&buf);
    os << "here " << std::flush;
    zc.append(buf.detach(buf.end()));
    os << "comes " << std::flush;
    zc.append(buf.detach(buf.end()));
    os << "the body" << std::flush;
    zc.append(buf.detach(buf.end()));
    EXPECT_EQ(std::distance(zc.begin_fragment(), zc.end_fragment()), 3);

    ExpectCallbackCalled(
        [](auto ec, auto res) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(res.status, 200);
            EXPECT_EQ(res.body, "some body");
        },
        1,
        [this, zc](auto cb) { HttpClient->Post("/post_url", zc, cb); }
    );
}

TEST_F(THttpClientPostMethodTest, ZeroCopyBodyWithHeadersSuccess) {
    EXPECT_CALL(*YHttpClientMock, async_run(_, RequestWithHeaders, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, ResponseSuccessWithHeaders));

    yplatform::zerocopy::segment zc;
    yplatform::zerocopy::streambuf buf;
    std::ostream os(&buf);
    os << "here " << std::flush;
    zc.append(buf.detach(buf.end()));
    os << "comes " << std::flush;
    zc.append(buf.detach(buf.end()));
    os << "the body" << std::flush;
    zc.append(buf.detach(buf.end()));
    EXPECT_EQ(std::distance(zc.begin_fragment(), zc.end_fragment()), 3);

    NHttp::THeaders hdr{{"X-Yandex-Test", "in progress"}};
    ExpectCallbackCalled(
        [](auto ec, auto res) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(res.status, 200);

            NHttp::THeaders headers{
                { "X-Yandex-Test", "passed" },
                { "X-Yandex-Test", "yeah!" }
            };
            EXPECT_EQ(res.headers, headers);
            EXPECT_EQ(res.body, "some body");
        },
        1,
        [this, zc, hdr](auto cb) { HttpClient->Post("/post_url", zc, hdr, cb); }
    );
}

TEST_F(THttpClientGetMethodTest, RetryOnTimeoutThenFailOnTimeout) {
    EXPECT_CALL(*YHttpClientMock, async_run(_, _, _, _))
        .Times(2)
        .WillRepeatedly(InvokeArgument<3>(yhttp::errc::request_timeout, yhttp::response()));

    ExpectCallbackCalled(
        [](auto ec, auto res) {
            EXPECT_EQ(ec, yhttp::errc::request_timeout);
            EXPECT_NE(res.status, 200);
            EXPECT_NE(res.body, "some body");
        },
        1,
        [this](auto cb) { HttpClient->Get("/request_timeout", cb); }
    );
}

TEST_F(THttpClientGetMethodTest, RetryOn500ThenFailOn500) {
    EXPECT_CALL(*YHttpClientMock, async_run(_, _, _, _))
        .Times(2)
        .WillRepeatedly(InvokeArgument<3>(yhttp::errc::success, Response500));

    ExpectCallbackCalled(
        [](auto ec, auto res) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(res.status, 500);
            EXPECT_NE(res.body, "some body");
            EXPECT_EQ(res.reason, "Internal server error");
        },
        1,
        [this](auto cb) { HttpClient->Get("/get", cb); }
    );
}

TEST_F(THttpClientGetMethodTest, Code2xxIsSuccess) {
    yhttp::response response{203, {}, "some body", "Non-Authoritative Information"};

    EXPECT_CALL(*YHttpClientMock, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, response));

    ExpectCallbackCalled(
        [](auto ec, auto res) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(res.status, 203);
            EXPECT_EQ(res.body, "some body");
        },
        1,
        [this](auto cb) { HttpClient->Get("/get", cb); }
    );
}

TEST_F(THttpClientGetMethodTest, DontRetryOn4xx) {
    const yhttp::response response{403, {}, "", "Forbidden"};

    EXPECT_CALL(*YHttpClientMock, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, response));

    ExpectCallbackCalled(
        [](auto ec, auto res) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(res.status, 403);
        },
        1,
        [this](auto cb) { HttpClient->Get("/get", cb); }
    );
}

TEST_F(THttpClientPostMethodTest, RetryOn500ThenSuccess) {
    EXPECT_CALL(*YHttpClientMock, async_run(_, Request, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, Response500))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, ResponseSuccess));

    ExpectCallbackCalled(
        [](auto ec, auto res) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(res.status, 200);
            EXPECT_EQ(res.body, "some body");
        },
        1,
        [this](auto cb) { HttpClient->Post("/post_url", "here comes the body", cb); }
    );
}

TEST_F(THttpClientGetMethodTest, FailOnException) {
    EXPECT_CALL(*YHttpClientMock, async_run(_, Request, _, _))
        .WillOnce(Throw(std::runtime_error("error")));

    ExpectCallbackCalled(
        [](auto ec, auto) {
            EXPECT_EQ(ec, EError::DeliveryInternal);
        },
        1,
        [this](auto cb) { HttpClient->Get("/get_url", cb); }
    );
}

struct THttpClientWithTvm2Test: public TNslsTest {
    void SetUp() override {
        Ctx = GetContext();

        YHttpClientMock = std::make_shared<StrictMock<TYmodHttpClientMock>>();
        YTvmClientMock = std::make_shared<StrictMock<TYmodTvmMock>>();
    }

    void CreateClient(bool useTvm) {
        yplatform::ptree pt;
        pt.add<unsigned int>("attempts", 2);
        pt.add<std::string>("url", "");
        if (useTvm) {
            pt.add<std::string>("tvm_service_name", "tvm_service_name");
        }
        Config = std::make_unique<NConfig::THttpCall>(pt);

        HttpClient = NHttp::CreateClient(Ctx, "client", *Config, YHttpClientMock, YTvmClientMock);
    }

    TContextPtr Ctx;
    NConfig::THttpCallUptr Config;
    NHttp::TClientPtr HttpClient;
    std::shared_ptr<TYmodHttpClientMock> YHttpClientMock;
    std::shared_ptr<TYmodTvmMock> YTvmClientMock;

    const yhttp::response ResponseEmpty{};
    const yhttp::response ResponseSuccess{
        200,
        {},
        "some body",
        "Ok"};
};

TEST_F(THttpClientWithTvm2Test, UndefinedTvmServiceName) {
    EXPECT_CALL(*YHttpClientMock, async_run(_, _, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, ResponseSuccess));
    EXPECT_CALL(*YTvmClientMock, get_service_ticket(_, _))
        .Times(0);

    CreateClient(false);
    ExpectCallbackCalled(
        [](auto ec, auto res) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(res.status, 200);
            EXPECT_EQ(res.body, "some body");
        },
        1,
        [this](auto cb) { HttpClient->Get("/get_url", cb); }
    );
}

TEST_F(THttpClientWithTvm2Test, SuccessfullyGotTicket) {
    EXPECT_CALL(*YHttpClientMock, async_run(_, Field(&TYmodHttpClientMock::TRequest::headers, "X-Ya-Service-Ticket: Ticket\r\n"), _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, ResponseSuccess));
    EXPECT_CALL(*YTvmClientMock, get_service_ticket("tvm_service_name", _))
        .WillOnce(DoAll(
            SetArgReferee<1>("Ticket"),
            Return(boost::system::error_code())
        ));

    CreateClient(true);
    ExpectCallbackCalled(
        [](auto ec, auto res) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(res.status, 200);
            EXPECT_EQ(res.body, "some body");
        },
        1,
        [this](auto cb) { HttpClient->Get("/get_url", cb); }
    );
}

TEST_F(THttpClientWithTvm2Test, FailToGetTicket) {
    EXPECT_CALL(*YHttpClientMock, async_run(_, Field(&TYmodHttpClientMock::TRequest::headers, "X-Ya-Service-Ticket: Ticket\r\n"), _, _))
        .Times(0);
    EXPECT_CALL(*YTvmClientMock, get_service_ticket("tvm_service_name", _))
        .WillOnce(
            Return(ymod_tvm::error::tickets_not_loaded)
        );

    CreateClient(true);
    ExpectCallbackCalled(
        [](auto ec, auto) {
            EXPECT_EQ(ec, EError::DeliveryInternal);
        },
        1,
        [this](auto cb) { HttpClient->Get("/get_url", cb); }
    );
}

TEST_F(THttpClientWithTvm2Test, ThrowsWhileGettingTicket) {
    EXPECT_CALL(*YHttpClientMock, async_run(_, Field(&TYmodHttpClientMock::TRequest::headers, "X-Ya-Service-Ticket: Ticket\r\n"), _, _))
        .Times(0);
    EXPECT_CALL(*YTvmClientMock, get_service_ticket("tvm_service_name", _))
        .WillOnce(
            Throw(std::runtime_error("error"))
        );

    CreateClient(true);
    ExpectCallbackCalled(
        [](auto ec, auto) {
            EXPECT_EQ(ec, EError::DeliveryInternal);
        },
        1,
        [this](auto cb) { HttpClient->Get("/get_url", cb); }
    );
}

TEST_F(THttpClientGetMethodWithConfigHeadersTest, NoHeadersSuccess) {
    EXPECT_CALL(*YHttpClientMock, async_run(_, Request, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, ResponseSuccess));

    ExpectCallbackCalled(
        [](auto ec, auto res) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(res.status, 200);
            EXPECT_EQ(res.body, "some body");
        },
        1,
        [this](auto cb) { HttpClient->Get("/get_url", cb); }
    );
}

TEST_F(THttpClientGetMethodWithConfigHeadersTest, WithHeadersSuccess) {
    EXPECT_CALL(*YHttpClientMock, async_run(_, RequestWithHeaders, _, _))
        .WillOnce(InvokeArgument<3>(yhttp::errc::success, ResponseSuccessWithHeaders));

    NHttp::THeaders hdr{{"X-Yandex-Test", "in progress"}};
    ExpectCallbackCalled(
        [](auto ec, auto res) {
            EXPECT_FALSE(ec);
            EXPECT_EQ(res.status, 200);

            NHttp::THeaders headers{
                { "X-Yandex-Test", "passed" },
                { "X-Yandex-Test", "yeah!" }
            };
            EXPECT_EQ(res.headers, headers);
            EXPECT_EQ(res.body, "some body");
        },
        1,
        [this, hdr](auto cb) { HttpClient->Get("/get_url", hdr, cb); }
    );
}

TEST(AppendUriParams, TestBasic) {
    struct TTestCases {
        std::string Uri;
        std::string Params;
        std::string ExpectedResult;
    };
    TTestCases testCases[] = {
        {"domain",         "",           "domain"},
        {"domain",         "p1=1",       "domain?p1=1"},
        {"domain",         "p1=1&p2=v2", "domain?p1=1&p2=v2"},
        {"domain?",        "p1=1&p2=v2", "domain?p1=1&p2=v2"},
        {"domain?o=val",   "p1=1&p2=v2", "domain?o=val&p1=1&p2=v2"},
        {"domain?o=val&",  "p1=1&p2=v2", "domain?o=val&p1=1&p2=v2"},
        {"domain?o=v&i=0", "p1=1&p2=v2", "domain?o=v&i=0&p1=1&p2=v2"}
    };
    for (const auto& test: testCases) {
        auto uri = test.Uri;
        NHttp::AppendUriParams(uri, test.Params);
        EXPECT_EQ(uri, test.ExpectedResult);
    }
}

TEST(UrlEncode, TestBasic) {
    EXPECT_EQ(NHttp::UrlEncode({{"test", "need encode"}}), "test=need+encode");
    EXPECT_EQ(NHttp::UrlEncode({{"test", "need=encode"}}, '?'), "?test=need%3dencode");
}
