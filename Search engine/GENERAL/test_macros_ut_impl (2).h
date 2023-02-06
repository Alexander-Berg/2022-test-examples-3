#pragma once

#include <search/meta/scatter/async/runner.h>

struct TAsyncTestTraits {
    static void Sleep(TDuration duration) {
        ::Sleep(duration);
    }
    static NScatter::TSourceOptions SourceOptions() {
        NScatter::TSourceOptions sourceOptions;
        sourceOptions.AllowBalancerDynamic = false;
        return sourceOptions;
    }
};

#define Y_SCATTER_UNIT_TEST_IMPL(N) \
    Y_UNIT_TEST(N##Async) { \
        RunScatterTest##N<TAsyncTaskRunner, TAsyncTestTraits>(); \
    }
