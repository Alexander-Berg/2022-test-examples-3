#pragma once

#include "../test_with_spawn.h"
#include "../mocks.h"

#include <internal/task_context.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <boost/asio/spawn.hpp>

namespace sharpei::tests {

using namespace testing;

struct TestWithContext : TestWithSpawn {
    template <class T>
    void withContext(T&& impl) {
        withSpawn([impl = std::forward<T>(impl)] (auto yield) {
            using namespace sharpei;
            using namespace sharpei::services;
            return impl(makeTaskContext(
                "uniq_id",
                "request_id",
                yield
            ));
        });
    }
};

} // namespace sharpei::tests
