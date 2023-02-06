#ifndef DOBERMAN_TESTS_CACHE_MOCKS_H_
#define DOBERMAN_TESTS_CACHE_MOCKS_H_

#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include "wrap_yield.h"
#include "mock_extentions.h"

namespace {

using namespace ::testing;
using namespace ::doberman::testing;

struct MutexMock : MockSingleton<MutexMock> {
    using YieldContext = Yield;
    using LockHandle = const void*;
    MOCK_METHOD(LockHandle, lock_, (YieldContext, int), ());
    MOCK_METHOD(void, unlock_, (LockHandle), ());
    PROXY_TO_MOCK_SINGLETON(lock)
    static auto lock(YieldContext y) { return instance->lock_(y, 0); }
    PROXY_TO_MOCK_SINGLETON(unlock)
};

struct ClockMock : MockSingleton<ClockMock>{
    using duration = int;
    using time_point = int;
    MOCK_METHOD(time_point, now_, (), (const));
    PROXY_TO_MOCK_SINGLETON(now)
};

}

#endif // DOBERMAN_TESTS_CACHE_MOCKS_H_
