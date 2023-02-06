#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <common/spawn.h>
#include <common/log.h>
#include <web/ymod_webserver_mocks.h>
#include <web/http_client_mock.h>
#include <web/python_module_mock.h>

namespace sheltie::tests {

using namespace testing;
using namespace sheltie::web;
using tvm_guard::Action;
using tvm_guard::Reason;
using tvm_guard::Response;

struct TvmGuardMock {
    MOCK_METHOD(Response, check, (const std::string&, const std::string&,
        const std::optional<std::string_view>&, const std::optional<std::string_view>&), (const));
};
using TvmGuardMockPtr = std::shared_ptr<TvmGuardMock>;

struct WebContextMock {
    TvmGuardMockPtr tvmGuard = std::make_shared<TvmGuardMock>();
    std::shared_ptr<StrictMock<HttpClientMock>> collieClient {std::make_shared<StrictMock<HttpClientMock>>()};
    std::shared_ptr<StrictMock<PythonModuleMock>> pythonModule {std::make_shared<StrictMock<PythonModuleMock>>()};
};
using WebContextMockPtr = std::shared_ptr<WebContextMock>;

}
