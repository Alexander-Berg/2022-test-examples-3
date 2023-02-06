#ifndef DOBERMAN_TESTS_TIMER_H_
#define DOBERMAN_TESTS_TIMER_H_

#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include "wrap_yield.h"
#include "mock_extentions.h"

namespace doberman {
namespace testing {

struct TimerMock : MockSingleton<TimerMock> {
    MOCK_METHOD(void, wait_, (int), (const));
    PROXY_TO_MOCK_SINGLETON(wait)
};

} // namespace testing

namespace access_impl {
namespace timer {

template <typename ... Args>
inline void wait(std::chrono::duration<Args...>, testing::Yield) {}

inline void wait(int dur, testing::Yield) { testing::TimerMock::wait(dur); }

} // namespace timer
} // namespace access_impl

namespace testing {

using namespace ::testing;

struct Duration {
    struct Mock {
        MOCK_METHOD(void, wait, (const Duration&), (const));
    };
    Mock& mock;
    Duration(Mock& m) : mock(m) {}
    friend void wait(const Duration& d, Yield) { d.mock.wait(d); }
    template <typename T>
    Duration operator / (T&&) const { return *this; }
    // Dummy for logger attribute compatibility
    operator std::chrono::steady_clock::duration () const {
        return std::chrono::steady_clock::duration(0);
    }
};

} // namespace testing
} // namespace doberman
#endif /* DOBERMAN_TESTS_TIMER_H_ */
