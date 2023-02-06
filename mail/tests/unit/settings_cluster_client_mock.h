#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <ymod_httpclient/call.h>

namespace settings::test {

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

}
