#pragma once

#include "connection_mock.hpp"

#include <gmock/gmock.h>

#include <ozo/connection_pool.h>

namespace collie::tests {

using namespace testing;

struct ConnectionPoolMock {
    using connection_type = ConnectionMock<>*;

    MOCK_METHOD(void, async_get_connection, (std::function<void (ozo::error_code, connection_type)>), (const));

    template <typename IoContext>
    const ConnectionPoolMock& operator[] (IoContext&) const {
        return *this;
    }

    template <typename TimeConstraint, typename Handler>
    friend void async_get_connection(const ConnectionPoolMock& self, TimeConstraint, Handler h) {
        self.async_get_connection(h);
    }
};

// using Connection = ConnectionPoolMock::connection_type;
using ConnectionPoolPtr = ConnectionPoolMock*;

} // namespace collie::tests
