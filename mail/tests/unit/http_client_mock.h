#pragma once

#include <gmock/gmock.h>
#include "../../src/processor/http_client.hpp"
#include <sharpei_client/sharpei_client.h>
#include <furita/common/context.h>

namespace furita {

class HttpClientMock : public HttpClient {
public:
    MOCK_METHOD(ymod_httpclient::future_void_t, post, (
        TContextPtr ctx,
        const std::string& host,
        const ymod_httpclient::timeouts &timeouts,
        const std::string& url,
        ymod_httpclient::string_ptr data,
        const ymod_httpclient::headers_dict& headers,
        yhttp::response_handler_ptr handler)
    , (const, override));

    MOCK_METHOD(ymod_httpclient::future_void_t, get, (
        TContextPtr ctx,
        const std::string& host,
        const ymod_httpclient::timeouts &timeouts,
        const std::string& url,
        const ymod_httpclient::headers_dict& headers,
        yhttp::response_handler_ptr handler)
    , (const, override));
};


struct MockSharpeiClient : ::sharpei::client::SharpeiClient {
    using AsyncHandler = ::sharpei::client::SharpeiClient::AsyncHandler;
    using AsyncMapHandler = ::sharpei::client::SharpeiClient::AsyncMapHandler;

    MOCK_METHOD(void, asyncGetConnInfo, (const ::sharpei::client::ResolveParams&, AsyncHandler), (const, override));
    MOCK_METHOD(void, asyncGetDeletedConnInfo, (const ::sharpei::client::ResolveParams&, AsyncHandler), (const, override));
    MOCK_METHOD(void, asyncGetOrgConnInfo, (const ::sharpei::client::ResolveParams&, AsyncHandler), (const, override));
    MOCK_METHOD(void, asyncStat, (AsyncMapHandler), (const, override));
    MOCK_METHOD(void, asyncStatById, (const ::sharpei::client::Shard::Id&, AsyncHandler), (const, override));

};

using callback_type = ymod_httpclient::simple_call::callback_type;

struct ClusterClientMock : public ymod_httpclient::cluster_call {
    MOCK_METHOD(void, async_run, (task_context_ptr, yhttp::request, callback_type));
    MOCK_METHOD(void, async_run, (task_context_ptr, yhttp::request, const ymod_httpclient::cluster_call::options&, callback_type));
};

}
