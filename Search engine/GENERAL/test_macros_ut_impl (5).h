#pragma once

#include <search/meta/scatter/coroutine/runner.h>

#include <library/cpp/coroutine/engine/impl.h>

struct TCoroutineTestTraits {
    static void Sleep(TDuration duration) {
        RunningCont()->SleepT(duration);
    }
    static NScatter::TSourceOptions SourceOptions() {
        return {};
    }
};

#define Y_SCATTER_UNIT_TEST_IMPL(N) \
    Y_UNIT_TEST(N##Coroutine) {\
        TContExecutor executor(128 * 1024);\
        executor.Execute([](TCont*, void*) {\
            RunScatterTest##N<TCoroutineTaskRunner, TCoroutineTestTraits>();\
        });\
    }
