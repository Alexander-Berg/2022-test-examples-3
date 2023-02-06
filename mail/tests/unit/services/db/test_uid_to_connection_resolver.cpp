#include "connection_pool_mock.hpp"
#include "endpoint_pool_mock.hpp"
#include "uid_resolver_mock.hpp"
#include "handler_mock.hpp"

#include <tests/unit/error_category.hpp>
#include <tests/unit/test_with_task_context.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <src/services/db/contacts/uid_to_connection_resolver.hpp>

namespace {

using namespace testing;
using namespace collie::tests;

using collie::TaskContextPtr;

using Connection = ConnectionPoolMock::connection_type;
using HandlerMock = collie::tests::HandlerMock<Connection>;
using Handler = collie::tests::Handler<Connection>;
using UidToConnectionResolver = collie::services::db::contacts::UidToConnectionResolver<EndpointPool>;

struct TestUidToConnectionResolver : TestWithTaskContext {
    StrictMock<EndpointPoolMock> pool;
    StrictMock<UidResolverMock> uidResolver;
    StrictMock<ConnectionPoolMock> connPool;
    StrictMock<HandlerMock> handler;

    UidToConnectionResolver resolver(TaskContextPtr ctx) {
        return {
            42,
            std::make_shared<EndpointPool>(pool),
            ozo::connection_pool_timeouts {},
            ctx,
            std::make_shared<UidResolver>(uidResolver)
        };
    }
};

TEST_F(TestUidToConnectionResolver, should_callback_with_error_if_asyncGetConnInfo_provides_error) {
    withSpawn([&] (const auto& ctx) {
        const InSequence s;

        EXPECT_CALL(uidResolver, asyncGetConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(ozo::error_code{Error::fail}));
        EXPECT_CALL(handler, call(ozo::error_code{Error::fail}, _)).WillOnce(Return());

        resolver(ctx)(Handler{&handler});
    });
}

TEST_F(TestUidToConnectionResolver, should_callback_with_error_if_asyncGetConnInfo_provides_empty_connInfo) {
    withSpawn([&] (const auto& ctx) {
        const InSequence s;

        EXPECT_CALL(uidResolver, asyncGetConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(std::vector<std::string>{}));
        EXPECT_CALL(handler, call(ozo::error_code{collie::services::db::Error::userShardUnavailable}, _))
            .WillOnce(Return());

        resolver(ctx)(Handler{&handler});
    });
}

TEST_F(TestUidToConnectionResolver, should_ask_for_pool_with_first_connstr_and_callback_with_error_if_no_pool_returned) {
    withSpawn([&] (const auto& ctx) {
        const InSequence s;

        EXPECT_CALL(uidResolver, asyncGetConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(std::vector<std::string>{"aaa", "bbb"}));
        EXPECT_CALL(pool, getConnectionPool("aaa"))
            .WillOnce(Return(nullptr));
        EXPECT_CALL(handler, call(ozo::error_code{collie::services::db::Error::endpointsOverflow}, _))
            .WillOnce(Return());

        resolver(ctx)(Handler{&handler});
    });
}

TEST_F(TestUidToConnectionResolver, should_ask_for_pool_with_first_connstr_and_obtain_connection_from_pool) {
    withSpawn([&] (const auto& ctx) {
        const InSequence s;

        EXPECT_CALL(uidResolver, asyncGetConnInfo(_, _))
            .WillOnce(InvokeArgument<1>(std::vector<std::string>{"aaa", "bbb"}));
        EXPECT_CALL(pool, getConnectionPool("aaa"))
            .WillOnce(Return(&connPool));
        EXPECT_CALL(connPool, async_get_connection(_))
            .WillOnce(Return());

        resolver(ctx)([&](ozo::error_code, auto) {});
    });
}

} // namespace
