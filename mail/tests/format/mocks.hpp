#pragma once

#include <gmock/gmock.h>

namespace ymod_webserver {
namespace helpers {
namespace tests {

using namespace testing;

struct MockedStringStream {
    MOCK_METHOD(void, write, (const std::string&), ());

    MockedStringStream& operator<<(const std::string& value) {
        write(value);
        return *this;
    }
};

struct MockedStringFunction {
    MOCK_METHOD(void, call, (const std::string&), ());

    void operator ()(const std::string& v) {
        call(v);
    }
};

} // namespace tests
} // namespace helpers
} // namespace ymod_webserver
