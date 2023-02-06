#pragma once

#include <search/meta/scatter/ant/runner.h>
#include <adfox/ant/ant/coroutine/spawn.hpp>

struct TAntTestTraits {
    static void Sleep(TDuration duration) {
        ::Sleep(duration);
    }
    static NScatter::TSourceOptions SourceOptions() {
        return {};
    }
};

#define Y_SCATTER_UNIT_TEST_IMPL(N) \
    Y_UNIT_TEST(N##AMACS) { \
        boost::asio::io_service ios; \
        ant::spawn(ios, [](ant::coroutine& coro) { \
            RunScatterTest##N<TAntCoroutineTaskRunner, TAntTestTraits>(&coro); \
        }); \
    }

