#pragma once

#include <internal/server/context.h>
#include <internal/server/handlers/helpers.h>
#include <internal/server/handlers/create_list.h>
#include <internal/server/handlers/subscribe.h>
#include <internal/server/handlers/unsubscribe.h>
#include <internal/server/handlers/set_archivation_rule.h>
#include <internal/server/handlers/remove_archivation_rule.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace york {
namespace tests {

using namespace server::handlers;

struct ResponseMock {
    MOCK_METHOD(void, badRequest, (server::handlers::Error), (const));
    MOCK_METHOD(void, internalError, (server::handlers::Error), (const));
    MOCK_METHOD(void, ok, (server::handlers::CreateListResult), (const));
    MOCK_METHOD(void, ok, (server::handlers::SubscribeResult), (const));
    MOCK_METHOD(void, ok, (server::handlers::UnsubscribeResult), (const));
    MOCK_METHOD(void, ok, (server::handlers::SetArchivationRuleResult), (const));
    MOCK_METHOD(void, ok, (server::handlers::RemoveArchivationRuleResult), (const));

    template <typename Logger, typename ... Args>
    void badRequest(server::handlers::Error e, Logger, Args&& ...) const { badRequest(e); }

    template <typename Logger, typename ... Args>
    void internalError(server::handlers::Error e, Logger, Args&& ...) const { internalError(e); }
};

struct ContextMock {
    MOCK_METHOD(boost::optional<std::string>, getOptionalArg, (const std::string&), (const));
    MOCK_METHOD(std::string, getHeader, (const std::string&), (const));
    MOCK_METHOD(std::string, requestId, (), (const));
    ResponseMock resp;
    ResponseMock& response() {
        return resp;
    }
};

} //namespace tests
} //namespace york
