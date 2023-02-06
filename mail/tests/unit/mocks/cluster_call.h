#pragma once

#include <ymod_httpclient/call.h>

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <memory>

namespace {

class TClusterCallMock : public ymod_httpclient::cluster_call {
public:
    MOCK_METHOD(void, async_run, (task_context_ptr, yhttp::request, callback_type));
    MOCK_METHOD(void,
                async_run,
                (task_context_ptr,
                 yhttp::request,
                 const ymod_httpclient::cluster_call::options&,
                 callback_type));
};

}
