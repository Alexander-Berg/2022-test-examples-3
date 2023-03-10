#pragma once

#include <gtest/gtest.h>
#include <gmock/gmock.h>


#include <boost/thread.hpp>


struct BaseTest : public testing::TestWithParam<int> {
    boost::asio::io_context io{GetParam()};

    template <class Coroutine>
    void spawn(Coroutine&& coroutine) {
        boost::asio::spawn(io, std::forward<Coroutine>(coroutine));
    }


    void runIO() {
        io.reset();
        const int threadCount = GetParam();
        boost::thread_group tg;
        for (int i = 0; i < threadCount; ++i) {
            tg.create_thread([this] () { io.run(); });
        }
        tg.join_all();
    }
};



template<typename ValueType>
inline ValueType sleepAndReturn(ValueType v, boost::asio::yield_context yield) {
    boost::asio::deadline_timer t(coro::getIoContext(yield));
    t.expires_from_now(boost::posix_time::milliseconds(10));
    t.async_wait(yield);
    return v;
}


template<int V>
inline int returnInt() {
    return V;
}

inline auto returnInt(int r) {
    return [=]() {
        return r;
    };
}

