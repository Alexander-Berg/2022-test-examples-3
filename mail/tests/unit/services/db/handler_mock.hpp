#pragma once

#include <gmock/gmock.h>

#include <ozo/error.h>

namespace collie::tests {

using namespace testing;

template <class Connection>
struct HandlerMock {
    MOCK_CONST_METHOD2_T(call, void (ozo::error_code, Connection));
};

template <class Connection>
struct Handler {
    StrictMock<HandlerMock<Connection>>* mock = nullptr;

    void operator ()(ozo::error_code ec, Connection conn) {
        mock->call(ec, conn);
    }
};

} // namespace collie::tests
