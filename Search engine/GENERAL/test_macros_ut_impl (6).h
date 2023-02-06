#pragma once

#include <search/meta/scatter/fiber/runner.h>
#include <search/meta/scatter/fiber/env/fiber_env.h>

struct TFiberTestTraits {
    static void Sleep(TDuration duration) {
        NScatter::FiberSleep(duration);
    }
    static NScatter::TSourceOptions SourceOptions() {
        return {};
    }
};

#define Y_SCATTER_UNIT_TEST_IMPL(N) \
    Y_UNIT_TEST(N##Fiber) {\
        RunInFiber([]() {\
              RunScatterTest##N<TFiberTaskRunner, TFiberTestTraits>();\
        });\
    }
