#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include "../../src/processor/msearch_call.hpp"
#include "http_client_mock.h"
#include "tvm_client_mock.h"
#include <furita/common/context.h>
#include <furita/common/http_headers.h>

namespace furita {

using namespace testing;

class MsearchCallTest: public Test {
public:
    MsearchCallTest()
        : context_(new TContext)
        , httpClientMock_(new HttpClientMock)
        , tvmMock_(new StrictMock<TTvmClientMock>())
        , call_(new MsearchCall(context_, httpClientMock_, "http://msearch-proxy.yandex.net",
                {{"service", "1"}, {"user", "2"}}, ymod_httpclient::timeouts(), "ticket12345", tvmMock_))
    {
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
    std::shared_ptr<MsearchCall> call_;
};

void returnMids(Unused, Unused, Unused, Unused, Unused,
        yhttp::response_handler_ptr handler) {
    handler->set_code(200, "ok");
    const std::string resp = R"({"envelopes":["111","222","333"]})";
    handler->handle_data(resp.c_str(), resp.size());
    handler->handle_data_end();
};

void returnMidsWrongFormat(Unused, Unused, Unused, Unused, Unused,
    yhttp::response_handler_ptr handler) {
    handler->set_code(200, "ok");
    const std::string resp = R"([{"mid":"111")";
    handler->handle_data(resp.c_str(), resp.size());
    handler->handle_data_end();
};

TEST_F(MsearchCallTest, successCall) {
    yplatform::future::promise<void> prom;
    EXPECT_CALL(*httpClientMock_, get(
        context_,
        "http://msearch-proxy.yandex.net",
        _,
        "api/async/mail/furita?request=hdr_subject%3dtext&mdb=pg&suid=123&uid=321&imap=1&first=0&count=100&folder_set=opened_allowed&remote_ip=10.10.10.10&noshared=1&exclude-trash=1&get=mid",
        ymod_httpclient::headers_dict{{HttpHeaderNames::ticket, "ticket12345"}, {"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).WillOnce(DoAll(Invoke(returnMids), Return(prom)));

    prom.set();
    auto future = call_->search("123", "321", "hdr_subject=text", 0, 100, "opened_allowed",
        "10.10.10.10");
    future.get();
    EXPECT_THAT(call_->mids(), ElementsAre("111", "222", "333"));
}

TEST_F(MsearchCallTest, successCallNoSuid) {
    yplatform::future::promise<void> prom;
    EXPECT_CALL(*httpClientMock_, get(
        context_,
        "http://msearch-proxy.yandex.net",
        _,
        "api/async/mail/furita?request=hdr_subject%3dtext&mdb=pg&uid=321&imap=1&first=0&count=100&folder_set=opened_allowed&remote_ip=10.10.10.10&noshared=1&exclude-trash=1&get=mid",
        ymod_httpclient::headers_dict{{HttpHeaderNames::ticket, "ticket12345"}, {"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).WillOnce(DoAll(Invoke(returnMids), Return(prom)));

    prom.set();
    auto future = call_->search("", "321", "hdr_subject=text", 0, 100, "opened_allowed",
        "10.10.10.10");
    future.get();
    EXPECT_THAT(call_->mids(), ElementsAre("111", "222", "333"));
}

TEST_F(MsearchCallTest, successCallNoUid) {
    yplatform::future::promise<void> prom;
    EXPECT_CALL(*httpClientMock_, get(
        context_,
        "http://msearch-proxy.yandex.net",
        _,
        "api/async/mail/furita?request=hdr_subject%3dtext&mdb=pg&suid=123&imap=1&first=0&count=100&folder_set=opened_allowed&remote_ip=10.10.10.10&noshared=1&exclude-trash=1&get=mid",
        ymod_httpclient::headers_dict{{HttpHeaderNames::ticket, "ticket12345"}, {"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).WillOnce(DoAll(Invoke(returnMids), Return(prom)));

    prom.set();
    auto future = call_->search("123", "", "hdr_subject=text", 0, 100, "opened_allowed",
        "10.10.10.10");
    future.get();
    EXPECT_THAT(call_->mids(), ElementsAre("111", "222", "333"));
}

TEST_F(MsearchCallTest, successCallNoLength) {
    yplatform::future::promise<void> prom;
    EXPECT_CALL(*httpClientMock_, get(
        context_,
        "http://msearch-proxy.yandex.net",
        _,
        "api/async/mail/furita?request=hdr_subject%3dtext&mdb=pg&suid=123&uid=321&imap=1&first=0&folder_set=opened_allowed&remote_ip=10.10.10.10&noshared=1&exclude-trash=1&get=mid",
        ymod_httpclient::headers_dict{{HttpHeaderNames::ticket, "ticket12345"}, {"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).WillOnce(DoAll(Invoke(returnMids), Return(prom)));

    prom.set();
    auto future = call_->search("123", "321", "hdr_subject=text", 0, 0, "opened_allowed",
        "10.10.10.10");
    future.get();
    EXPECT_THAT(call_->mids(), ElementsAre("111", "222", "333"));
}

TEST_F(MsearchCallTest, successCallNoFolderSet) {
    yplatform::future::promise<void> prom;
    EXPECT_CALL(*httpClientMock_, get(
        context_,
        "http://msearch-proxy.yandex.net",
        _,
        "api/async/mail/furita?request=hdr_subject%3dtext&mdb=pg&suid=123&uid=321&imap=1&first=0&count=100&remote_ip=10.10.10.10&noshared=1&exclude-trash=1&get=mid",
        ymod_httpclient::headers_dict{{HttpHeaderNames::ticket, "ticket12345"}, {"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).WillOnce(DoAll(Invoke(returnMids), Return(prom)));

    prom.set();
    auto future = call_->search("123", "321", "hdr_subject=text", 0, 100, "",
        "10.10.10.10");
    future.get();
    EXPECT_THAT(call_->mids(), ElementsAre("111", "222", "333"));
}

TEST_F(MsearchCallTest, successCallNoRemoteIp) {
    yplatform::future::promise<void> prom;
    EXPECT_CALL(*httpClientMock_, get(
        context_,
        "http://msearch-proxy.yandex.net",
        _,
        "api/async/mail/furita?request=hdr_subject%3dtext&mdb=pg&suid=123&uid=321&imap=1&first=0&count=100&folder_set=opened_allowed&noshared=1&exclude-trash=1&get=mid",
        ymod_httpclient::headers_dict{{HttpHeaderNames::ticket, "ticket12345"}, {"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).WillOnce(DoAll(Invoke(returnMids), Return(prom)));

    prom.set();
    auto future = call_->search("123", "321", "hdr_subject=text", 0, 100, "opened_allowed",
        "");
    future.get();
    EXPECT_THAT(call_->mids(), ElementsAre("111", "222", "333"));
}

TEST_F(MsearchCallTest, exceptionInPromise) {
    yplatform::future::promise<void> prom;
    EXPECT_CALL(*httpClientMock_, get(
        context_,
        "http://msearch-proxy.yandex.net",
        _,
        "api/async/mail/furita?request=hdr_subject%3dtext&mdb=pg&suid=123&uid=321&imap=1&first=0&count=100&folder_set=opened_allowed&remote_ip=10.10.10.10&noshared=1&exclude-trash=1&get=mid",
        ymod_httpclient::headers_dict{{HttpHeaderNames::ticket, "ticket12345"}, {"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).WillOnce(DoAll(Invoke(returnMids), Return(prom)));

    prom.set_exception(std::make_exception_ptr(std::runtime_error("some error")));
    auto future = call_->search("123", "321", "hdr_subject=text", 0, 100, "opened_allowed",
        "10.10.10.10");
    EXPECT_THROW(future.get(), yplatform::exception);
}

TEST_F(MsearchCallTest, midsWrongFormat) {
    yplatform::future::promise<void> prom;
    EXPECT_CALL(*httpClientMock_, get(
        context_,
        "http://msearch-proxy.yandex.net",
        _,
        "api/async/mail/furita?request=hdr_subject%3dtext&mdb=pg&suid=123&uid=321&imap=1&first=0&count=100&folder_set=opened_allowed&remote_ip=10.10.10.10&noshared=1&exclude-trash=1&get=mid",
        ymod_httpclient::headers_dict{{HttpHeaderNames::ticket, "ticket12345"}, {"service", "1"}, {"user", "2"}, {"x-ya-service-ticket", "ticket"}},
        _)
    ).WillOnce(DoAll(Invoke(returnMidsWrongFormat), Return(prom)));

    prom.set();
    auto future = call_->search("123", "321", "hdr_subject=text", 0, 100, "opened_allowed",
        "10.10.10.10");
    EXPECT_THROW(future.get(), yplatform::exception);
}

} // namespace furita

