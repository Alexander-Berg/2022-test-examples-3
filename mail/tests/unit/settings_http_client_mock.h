#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <ymod_httpclient/call.h>


namespace settings::test {

using ymod_httpclient::future_void_t;
using yhttp::options;
using ymod_httpclient::post_chunks;
using ymod_httpclient::remote_point_info_ptr;
using yhttp::request;
using yhttp::response;
using yhttp::response_handler_ptr;
using ymod_httpclient::string_ptr;
using ymod_httpclient::timeouts;

struct HttpClientMock : yhttp::call {
    using task_context_ptr = yhttp::call::task_context_ptr;
    using callback_type = yhttp::call::callback_type;

    MOCK_METHOD(response, run, (task_context_ptr, request), (override));
    MOCK_METHOD(response, run, (task_context_ptr, request, const options&), (override));

    MOCK_METHOD(void, async_run, (task_context_ptr, request, callback_type), (override));
    MOCK_METHOD(void, async_run, (task_context_ptr, request, const options&, callback_type), (override));

    MOCK_METHOD(future_void_t, head_url, (task_context_ptr, response_handler_ptr, const remote_point_info_ptr,
            const std::string&, const std::string&), (override));
    MOCK_METHOD(future_void_t, get_url, (task_context_ptr, response_handler_ptr, const remote_point_info_ptr,
            const std::string&, const std::string&), (override));
    MOCK_METHOD(future_void_t, post_url, (task_context_ptr, response_handler_ptr, const remote_point_info_ptr,
            const std::string&, const string_ptr&, const std::string&, bool), (override));
    MOCK_METHOD(future_void_t, mpost_url, (task_context_ptr, response_handler_ptr, const remote_point_info_ptr,
            const std::string&, post_chunks&&, const std::string&, bool), (override));

    MOCK_METHOD(remote_point_info_ptr, make_rm_info, (const std::string&), (override));
    MOCK_METHOD(remote_point_info_ptr, make_rm_info, (const std::string&, bool), (override));
    MOCK_METHOD(remote_point_info_ptr, make_rm_info, (const std::string&, const timeouts&), (override));
    MOCK_METHOD(remote_point_info_ptr, make_rm_info, (const std::string&, const timeouts&, bool), (override));
};

}
