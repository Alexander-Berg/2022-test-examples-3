#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include "../../src/processor/mops_call.hpp"
#include "http_client_mock.h"
#include "tvm_client_mock.h"
#include <furita/common/context.h>
#include <furita/common/http_headers.h>

namespace furita {

using namespace testing;

void set200(Unused, Unused, Unused, Unused, Unused, Unused,
        yhttp::response_handler_ptr handler) {
    handler->set_code(200, "ok");
};

void set500(Unused, Unused, Unused, Unused, Unused, Unused,
    yhttp::response_handler_ptr handler) {
    handler->set_code(500, "internal server error");
};

class MopsCallTest: public Test {
public:
    MopsCallTest()
        : context_(new TContext)
        , httpClientMock_(new HttpClientMock)
        , tvmMock_(new StrictMock<TTvmClientMock>())
        , call_(context_, httpClientMock_, "ticket12345", {{"service", "1"}, {"user", "2"}}, tvmMock_)
    {
        opts_.primary_host = "http://mops-primary.yandex.ru:8814";
        opts_.fallback_host = "http://mops-fallback.yandex.ru:8814";
        opts_.attempts = 2;

        EXPECT_CALL(*tvmMock_, get_service_ticket(_, _))
            .WillRepeatedly(DoAll(
                SetArgReferee<1>("ticket"),
                Return(boost::system::error_code{})
            ));
    }

protected:
    TContextPtr context_;
    std::shared_ptr<HttpClientMock> httpClientMock_;
    std::shared_ptr<TTvmClientMock> tvmMock_;
    mops_call call_;
    mops_call::options opts_;
};

TEST_F(MopsCallTest, successMoveCall) {
    yplatform::future::promise<void> prom;
    EXPECT_CALL(*httpClientMock_, post(
        context_,
        "http://mops-primary.yandex.ru:8814",
        _,
        "complex_move",
        Pointee(StrEq("uid=123&mdb=pg&ora_dsn=mail_dsn&source=furita&mids=111,222,333&dest_fid=1")),
        ymod_httpclient::headers_dict{{HttpHeaderNames::ticket, "ticket12345"}, {"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).WillOnce(DoAll(Invoke(set200), Return(prom)));

    prom.set();
    auto future = call_.move(opts_, 123, {"111", "222", "333"}, "1");
    EXPECT_NO_THROW(future.get());
}

TEST_F(MopsCallTest, emptyHostGetError) {
    opts_.primary_host = "";
    auto future = call_.move(opts_, 123, {"111", "222", "333"}, "1");
    EXPECT_THROW(future.get(), yplatform::exception);
}

TEST_F(MopsCallTest, primary500AndRetry) {
    yplatform::future::promise<void> prom;
    EXPECT_CALL(*httpClientMock_, post(
        context_,
        "http://mops-primary.yandex.ru:8814",
        _,
        "complex_move",
        Pointee(StrEq("uid=123&mdb=pg&ora_dsn=mail_dsn&source=furita&mids=111,222,333&dest_fid=1")),
        ymod_httpclient::headers_dict{{HttpHeaderNames::ticket, "ticket12345"}, {"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).WillOnce(DoAll(Invoke(set500), Return(prom)))
        .WillOnce(DoAll(Invoke(set200), Return(prom)));

    prom.set();
    auto future = call_.move(opts_, 123, {"111", "222", "333"}, "1");
    EXPECT_NO_THROW(future.get());
}

TEST_F(MopsCallTest, primaryAlways500AndRetryFallback) {
    yplatform::future::promise<void> prom;
    EXPECT_CALL(*httpClientMock_, post(
        context_,
        "http://mops-primary.yandex.ru:8814",
        _,
        "complex_move",
        Pointee(StrEq("uid=123&mdb=pg&ora_dsn=mail_dsn&source=furita&mids=111,222,333&dest_fid=1")),
        ymod_httpclient::headers_dict{{HttpHeaderNames::ticket, "ticket12345"}, {"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).Times(2).WillRepeatedly(DoAll(Invoke(set500), Return(prom)));

    EXPECT_CALL(*httpClientMock_, post(
        context_,
        "http://mops-fallback.yandex.ru:8814",
        _,
        "complex_move",
        Pointee(StrEq("uid=123&mdb=pg&ora_dsn=mail_dsn&source=furita&mids=111,222,333&dest_fid=1")),
        ymod_httpclient::headers_dict{{HttpHeaderNames::ticket, "ticket12345"}, {"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).WillOnce(DoAll(Invoke(set200), Return(prom)));

    prom.set();
    auto future = call_.move(opts_, 123, {"111", "222", "333"}, "1");
    EXPECT_NO_THROW(future.get());
}

TEST_F(MopsCallTest, always500GetError) {
    yplatform::future::promise<void> prom;
    EXPECT_CALL(*httpClientMock_, post(
        context_,
        "http://mops-primary.yandex.ru:8814",
        _,
        "complex_move",
        Pointee(StrEq("uid=123&mdb=pg&ora_dsn=mail_dsn&source=furita&mids=111,222,333&dest_fid=1")),
        ymod_httpclient::headers_dict{{HttpHeaderNames::ticket, "ticket12345"}, {"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).Times(2).WillRepeatedly(DoAll(Invoke(set500), Return(prom)));

    EXPECT_CALL(*httpClientMock_, post(
        context_,
        "http://mops-fallback.yandex.ru:8814",
        _,
        "complex_move",
        Pointee(StrEq("uid=123&mdb=pg&ora_dsn=mail_dsn&source=furita&mids=111,222,333&dest_fid=1")),
        ymod_httpclient::headers_dict{{HttpHeaderNames::ticket, "ticket12345"}, {"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).Times(2).WillRepeatedly(DoAll(Invoke(set500), Return(prom)));

    prom.set();
    auto future = call_.move(opts_, 123, {"111", "222", "333"}, "1");
    EXPECT_THROW(future.get(), yplatform::exception);
}

TEST_F(MopsCallTest, successRemoveCall) {
    yplatform::future::promise<void> prom;
    EXPECT_CALL(*httpClientMock_, post(
        context_,
        "http://mops-primary.yandex.ru:8814",
        _,
        "remove",
        Pointee(StrEq("uid=123&mdb=pg&ora_dsn=mail_dsn&source=furita&mids=111,222,333&nopurge=1")),
        ymod_httpclient::headers_dict{{HttpHeaderNames::ticket, "ticket12345"}, {"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).WillOnce(DoAll(Invoke(set200), Return(prom)));

    prom.set();
    auto future = call_.remove(opts_, 123, {"111", "222", "333"});
    EXPECT_NO_THROW(future.get());
}

TEST_F(MopsCallTest, successMarkROCall) {
    yplatform::future::promise<void> prom;
    EXPECT_CALL(*httpClientMock_, post(
        context_,
        "http://mops-primary.yandex.ru:8814",
        _,
        "mark",
        Pointee(StrEq("uid=123&mdb=pg&ora_dsn=mail_dsn&source=furita&mids=111,222,333&status=read")),
        ymod_httpclient::headers_dict{{HttpHeaderNames::ticket, "ticket12345"}, {"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).WillOnce(DoAll(Invoke(set200), Return(prom)));

    prom.set();
    auto future = call_.mark(opts_, 123, {"111", "222", "333"}, "RO");
    EXPECT_NO_THROW(future.get());
}

TEST_F(MopsCallTest, successMarkNewCall) {
    yplatform::future::promise<void> prom;
    EXPECT_CALL(*httpClientMock_, post(
        context_,
        "http://mops-primary.yandex.ru:8814",
        _,
        "mark",
        Pointee(StrEq("uid=123&mdb=pg&ora_dsn=mail_dsn&source=furita&mids=111,222,333&status=not_read")),
        ymod_httpclient::headers_dict{{HttpHeaderNames::ticket, "ticket12345"}, {"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).WillOnce(DoAll(Invoke(set200), Return(prom)));

    prom.set();
    auto future = call_.mark(opts_, 123, {"111", "222", "333"}, "New");
    EXPECT_NO_THROW(future.get());
}

TEST_F(MopsCallTest, successLabelCall) {
    yplatform::future::promise<void> prom;
    EXPECT_CALL(*httpClientMock_, post(
        context_,
        "http://mops-primary.yandex.ru:8814",
        _,
        "label",
        Pointee(StrEq("uid=123&mdb=pg&ora_dsn=mail_dsn&source=furita&mids=111,222,333&lids=1,2")),
        ymod_httpclient::headers_dict{{HttpHeaderNames::ticket, "ticket12345"}, {"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).WillOnce(DoAll(Invoke(set200), Return(prom)));

    prom.set();
    auto future = call_.label(opts_, 123, {"111", "222", "333"}, {"1", "2"});
    EXPECT_NO_THROW(future.get());
}

TEST_F(MopsCallTest, emptyTvmTicket) {
    yplatform::future::promise<void> prom;
    EXPECT_CALL(*httpClientMock_, post(
        context_,
        "http://mops-primary.yandex.ru:8814",
        _,
        "label",
        Pointee(StrEq("uid=123&mdb=pg&ora_dsn=mail_dsn&source=furita&mids=111,222,333&lids=1,2")),
        ymod_httpclient::headers_dict{{"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).WillOnce(DoAll(Invoke(set200), Return(prom)));

    prom.set();
    mops_call call(context_, httpClientMock_, "", {{"service", "1"}, {"user", "2"}}, tvmMock_);
    auto future = call.label(opts_, 123, {"111", "222", "333"}, {"1", "2"});
    EXPECT_NO_THROW(future.get());
}

} // namespace furita
