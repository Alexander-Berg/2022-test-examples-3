#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/services/utils.hpp>

#include <ymod_httpclient/call.h>

namespace ymod_httpclient {

static inline bool operator ==(const request& lhs, const request& rhs) {
    return lhs.method == rhs.method && lhs.url == rhs.url && lhs.headers == rhs.headers &&
            ((lhs.body && rhs.body && *lhs.body == *rhs.body) || (!lhs.body && !rhs.body));
}

static inline bool operator ==(const timeouts& lhs, const timeouts& rhs) {
    return lhs.connect == rhs.connect && lhs.total == rhs.total;
}

static inline bool operator ==(const options& lhs, const options& rhs) {
    return lhs.log_post_body == rhs.log_post_body
        && lhs.log_headers == rhs.log_headers
        && lhs.reuse_connection == rhs.reuse_connection
        && lhs.timeouts == rhs.timeouts;
}

static inline std::ostream& operator <<(std::ostream& stream, const request& value) {
    return stream << "yhttp::request {"
        << static_cast<int>(value.method) << ", "
        << value.url << ", "
        << value.headers << ", "
        << (value.body ? *value.body : std::string())
        << "}";
}

static inline std::ostream& operator <<(std::ostream& stream, const timeouts& value) {
    return stream << "ymod_httpclient::timeouts {"
        << value.connect.count() << ", "
        << value.total.count()
        << "}";
}

static inline std::ostream& operator <<(std::ostream& stream, const options& value) {
    using namespace std::string_literals;
    return stream << "yhttp::options {"
        << (value.log_post_body ? std::to_string(*value.log_post_body) : "none"s) << ", "
        << (value.log_headers ? std::to_string(*value.log_headers) : "none"s) << ", "
        << (value.reuse_connection ? std::to_string(*value.reuse_connection) : "none"s) << ", "
        << value.timeouts
        << "}";
}

} // namespace ymod_httpclient

namespace collie::tests {

using namespace testing;

struct GetHttpClientMock {
    MOCK_METHOD(std::shared_ptr<yhttp::simple_call>, call, (), (const));
};

struct GetHttpClientMockWrapper {
    std::shared_ptr<StrictMock<GetHttpClientMock>> impl = std::make_shared<StrictMock<GetHttpClientMock>>();

    std::shared_ptr<yhttp::simple_call> operator ()() const {
        return impl->call();
    }
};

struct GetClusterClientMock {
    MOCK_METHOD(std::shared_ptr<ymod_httpclient::cluster_call>, call, (), (const));
};

struct GetClusterClientMockWrapper {
    std::shared_ptr<StrictMock<GetClusterClientMock>> impl = std::make_shared<StrictMock<GetClusterClientMock>>();

    std::shared_ptr<ymod_httpclient::cluster_call> operator()() const {
        return impl->call();
    }
};

using ymod_httpclient::future_void_t;
using yhttp::options;
using ymod_httpclient::remote_point_info_ptr;
using yhttp::request;
using yhttp::response;
using yhttp::response_handler_ptr;
using ymod_httpclient::string_ptr;
using ymod_httpclient::timeouts;

struct HttpClientMock : yhttp::simple_call {
    using task_context_ptr = yhttp::simple_call::task_context_ptr;
    using callback_type = yhttp::simple_call::callback_type;

    MOCK_METHOD(response, run, (task_context_ptr, request), (override));
    MOCK_METHOD(response, run, (task_context_ptr, request, const options&), (override));

    MOCK_METHOD(void, async_run, (task_context_ptr, request, callback_type), (override));
    MOCK_METHOD(void, async_run, (task_context_ptr, request, const options&, callback_type), (override));
};

struct ClusterClientMock : ymod_httpclient::cluster_call {
    using task_context_ptr = yhttp::simple_call::task_context_ptr;
    using callback_type = yhttp::simple_call::callback_type;

    MOCK_METHOD(void, async_run, (task_context_ptr, yhttp::request, callback_type), (override));
    MOCK_METHOD(void, async_run, (task_context_ptr, yhttp::request,
        const ymod_httpclient::cluster_call::options&, callback_type
    ), (override));
};

inline std::shared_ptr<testing::StrictMock<ClusterClientMock>> GetStrictMockedClusterClient() {
    return std::make_shared<testing::StrictMock<ClusterClientMock>>();
}

} // namespace collie::tests
