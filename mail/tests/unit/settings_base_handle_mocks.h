#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <internal/common/context.h>
#include <internal/http/detail/handlers/base.h>


namespace settings::test {

using BaseHandle = http::detail::handlers::Base;
using RequestPtr = http::detail::RequestPtr;
using ResponsePtr = http::detail::ResponsePtr;

struct MockBaseHandle: BaseHandle {
    MockBaseHandle(std::string logic_name)
            : BaseHandle(logic_name) {
    }

    MOCK_METHOD(std::string, uri, (), (const, override));
    MOCK_METHOD(ymod_webserver::methods::http_method, method, (), (const, override));
    MOCK_METHOD(expected<void>, invoke, (RequestPtr, ResponsePtr, ContextPtr ctx), (const, override));
    MOCK_METHOD(expected<void>, parameters, (RequestPtr, ContextPtr ctx), (const, override));
};

} //namespace settings::test
