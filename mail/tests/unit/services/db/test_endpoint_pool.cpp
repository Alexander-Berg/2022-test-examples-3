#include <gtest/gtest.h>

#include <src/services/db/endpoint_pool.hpp>

namespace {

using namespace testing;

using ConnectionPool = collie::services::db::EndpointPool<>::ConnectionPool;
using ConnectionPoolPtr = collie::services::db::EndpointPool<>::ConnectionPoolPtr;

struct TestServicesDbEndpointPool : Test {
    std::size_t maxTotalPoolsCapacity = 5;
    ozo::connection_pool_config poolConfig {2, 0, std::chrono::seconds(0)};
    collie::services::db::EndpointPool<> endpointPool{maxTotalPoolsCapacity, poolConfig};
};

TEST_F(TestServicesDbEndpointPool, for_max_total_pools_capacilty_less_than_pool_capacity_constructor_should_throw_exception) {
    maxTotalPoolsCapacity = 1;
    poolConfig.capacity = 2;
    EXPECT_THROW(collie::services::db::EndpointPool<>(maxTotalPoolsCapacity, poolConfig), std::logic_error);
}

TEST_F(TestServicesDbEndpointPool, get_connection_pool_should_return_connection_pool_ptr) {
    EXPECT_NE(endpointPool.getConnectionPool("host=foo"), nullptr);
}

TEST_F(TestServicesDbEndpointPool, get_connection_pool_for_same_connection_info_should_return_existing_connection_pool_ptr) {
    const auto pool = endpointPool.getConnectionPool("host=foo");
    ASSERT_NE(pool, ConnectionPoolPtr());
    EXPECT_EQ(endpointPool.getConnectionPool("host=foo"), pool);
}

TEST_F(TestServicesDbEndpointPool, get_connection_pool_for_different_connection_info_should_return_new_connection_pool_ptr) {
    const auto foo = endpointPool.getConnectionPool("host=foo");
    ASSERT_NE(foo, ConnectionPoolPtr());
    const auto bar = endpointPool.getConnectionPool("host=bar");
    ASSERT_NE(bar, ConnectionPoolPtr());
    EXPECT_NE(foo, bar);
}

TEST_F(TestServicesDbEndpointPool, get_connection_pool_should_return_nullptr_when_overflow_max_total_pools_capacity) {
    const auto first = endpointPool.getConnectionPool("host=first");
    const auto second = endpointPool.getConnectionPool("host=second");
    EXPECT_EQ(endpointPool.getConnectionPool("host=third"), ConnectionPoolPtr());
}

TEST_F(TestServicesDbEndpointPool, get_connection_pool_should_replace_unused_pool_when_overflow) {
    const auto first = endpointPool.getConnectionPool("host=first");
    const std::weak_ptr<ConnectionPool> second = endpointPool.getConnectionPool("host=second");
    EXPECT_NE(second.lock(), ConnectionPoolPtr());
    EXPECT_NE(endpointPool.getConnectionPool("host=third"), ConnectionPoolPtr());
    EXPECT_EQ(second.lock(), ConnectionPoolPtr());
}

} // namespace
