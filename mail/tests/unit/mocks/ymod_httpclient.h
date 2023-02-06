#pragma once

#include <ymod_httpclient/call.h>
#include <gtest/gtest.h>
#include <gmock/gmock.h>

struct TYmodHttpClientMock: public yhttp::call {
    using TFutureVoid = ymod_httpclient::future_void_t;
    using TOptions = yhttp::options;
    using TPostChunks = ymod_httpclient::post_chunks;
    using TRemotePointInfoPtr = ymod_httpclient::remote_point_info_ptr;
    using TResponseHandlerPtr = yhttp::response_handler_ptr;
    using TStdStringPtr = ymod_httpclient::string_ptr;
    using TTimeouts = ymod_httpclient::timeouts;
    using TResponse = yhttp::response;
    using TRequest = yhttp::request;
    using TTaskContextPtr = yhttp::call::task_context_ptr;
    using TCallback = yhttp::call::callback_type;

    MOCK_METHOD(TResponse, run, (TTaskContextPtr, TRequest), (override));
    MOCK_METHOD(TResponse, run, (TTaskContextPtr, TRequest, const TOptions&), (override));

    MOCK_METHOD(void, async_run, (TTaskContextPtr ctx, TRequest req, TCallback), (override));
    MOCK_METHOD(void, async_run, (TTaskContextPtr ctx, TRequest req, const TOptions&, TCallback), (override));

    MOCK_METHOD(TFutureVoid, head_url, (
            TTaskContextPtr,
            TResponseHandlerPtr,
            const TRemotePointInfoPtr,
            const std::string&,
            const std::string&)
        , (override));
    MOCK_METHOD(TFutureVoid, get_url, (
            TTaskContextPtr,
            TResponseHandlerPtr,
            const TRemotePointInfoPtr,
            const std::string&,
            const std::string&)
        , (override));
    MOCK_METHOD(TFutureVoid, post_url, (
            TTaskContextPtr,
            TResponseHandlerPtr,
            const TRemotePointInfoPtr,
            const std::string&,
            const TStdStringPtr&,
            const std::string&,
            bool)
        , (override));
    MOCK_METHOD(TFutureVoid, mpost_url, (
            TTaskContextPtr,
            TResponseHandlerPtr,
            const TRemotePointInfoPtr,
            const std::string&,
            TPostChunks&&,
            const std::string&,
            bool)
        , (override));

    MOCK_METHOD(TRemotePointInfoPtr, make_rm_info, (const std::string&), (override));
    MOCK_METHOD(TRemotePointInfoPtr, make_rm_info, (const std::string&, bool), (override));
    MOCK_METHOD(TRemotePointInfoPtr, make_rm_info, (const std::string&, const TTimeouts&), (override));
    MOCK_METHOD(TRemotePointInfoPtr, make_rm_info, (const std::string&, const TTimeouts&, bool), (override));
};
