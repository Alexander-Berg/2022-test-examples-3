#pragma once

#include <gmock/gmock.h>
#include <functional>

namespace doberman {
namespace testing {

struct SpawnMock {
    using Func = std::function<void()>;
    MOCK_METHOD(void, spawn, (Func), (const));
};

} // namespace testing
} // namespace doberman


