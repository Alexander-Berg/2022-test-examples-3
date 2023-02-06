#pragma once

#include "connection_pool_mock.hpp"

#include <gmock/gmock.h>

namespace collie::tests {

using namespace testing;

struct EndpointPoolMock {
    MOCK_METHOD(ConnectionPoolPtr, getConnectionPool, (const std::string&), ());
};

struct EndpointPool {
    EndpointPoolMock* mock_ = nullptr;

    EndpointPool(EndpointPoolMock& mock) : mock_(&mock) {}

    using ConnectionPool = ConnectionPoolMock;

    ConnectionPoolPtr getConnectionPool(const std::string& conninfo) {
        return mock_->getConnectionPool(conninfo);
    }
};

} // namespace collie::tests
