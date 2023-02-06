#pragma once

#include <common/types.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace sheltie::tests {

using namespace testing;

struct HttpClientMock {
    using task_context_ptr = yhttp::simple_call::task_context_ptr;
    MOCK_METHOD(yhttp::response, async_run, (task_context_ptr, yhttp::request, sheltie::YieldCtx), ());
};

}
