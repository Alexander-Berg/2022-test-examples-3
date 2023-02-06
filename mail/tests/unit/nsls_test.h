#pragma once

#include <gtest/gtest.h>

class TNslsTest: public testing::Test {
public:
    template <typename TCallback, typename TCallable, typename TThis, typename ... TArgs>
    void ExpectCallbackCalled(TCallback&& cb, int count, TCallable&& call, TThis* self, TArgs&&... args) {
        int called = 0;
        (self->*call)(
            std::forward<TArgs>(args)...,
            [cb = std::move(cb), &called](auto ... cbArgs) {
                ++called;
                cb(cbArgs...);
            });
        EXPECT_EQ(called, count);
    }

    template <typename TCallback, typename TCallable, typename ... TArgs>
    void ExpectCallbackCalled(TCallback&& cb, int count, TCallable&& call, TArgs&&... args) {
        int called = 0;
        call(
            std::forward<TArgs>(args)...,
            [cb = std::move(cb), &called](auto ... cbArgs) {
                ++called;
                cb(cbArgs...);
            });
        EXPECT_EQ(called, count);
    }
};

namespace std {

template <typename T>
ostream& operator<<(ostream& os, const vector<T>& v) {
    bool first = true;
    for (const auto& e: v) {
        if (!first) {
            os << ", ";
        }
        first = false;
        os << e;
    }
    return os;
}

} // namespace std
