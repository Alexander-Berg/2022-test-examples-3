#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <ymod_httpclient/call.h>
#include <ymod_httpclient/request.h>
#include "test_with_yield_context.h"

namespace ymod_httpclient {

static inline bool operator ==(const request& lhs, const request& rhs) {
    return lhs.method == rhs.method && lhs.url == rhs.url && lhs.headers == rhs.headers &&
            ((lhs.body && rhs.body && *lhs.body == *rhs.body) || (!lhs.body && !rhs.body));
}

static inline bool operator ==(const timeouts& lhs, const timeouts& rhs) {
    return lhs.connect == rhs.connect && lhs.total == rhs.total;
}

static inline bool operator ==(const cluster_call::options& lhs, const cluster_call::options& rhs) {
    return lhs.log_post_body == rhs.log_post_body
        && lhs.log_headers == rhs.log_headers
        && lhs.reuse_connection == rhs.reuse_connection
        && lhs.timeouts == rhs.timeouts
        && lhs.max_attempts == rhs.max_attempts;
}

} // namespace ymod_httpclient

namespace msg_body {

using namespace testing;

struct GetClusterClientMock {
    MOCK_METHOD(std::shared_ptr<ymod_httpclient::cluster_call>, call, (), (const));
};

struct GetClusterClientMockWrapper {
    std::shared_ptr<StrictMock<GetClusterClientMock>> impl = std::make_shared<StrictMock<GetClusterClientMock>>();

    std::shared_ptr<ymod_httpclient::cluster_call> operator()(std::string) const {
        return impl->call();
    }
};

struct ClusterClientMock : ymod_httpclient::cluster_call {
    using task_context_ptr = yhttp::simple_call::task_context_ptr;
    using callback_type = yhttp::simple_call::callback_type;

    MOCK_METHOD(void, async_run, (task_context_ptr, yhttp::request, callback_type), (override));
    MOCK_METHOD(void, async_run, (task_context_ptr, yhttp::request, const ymod_httpclient::cluster_call::options&, callback_type), (override));
};

inline std::shared_ptr<ClusterClientMock> GetMockedClusterClient() {
    return std::make_shared<ClusterClientMock>();
}

} // namespace msg_body
