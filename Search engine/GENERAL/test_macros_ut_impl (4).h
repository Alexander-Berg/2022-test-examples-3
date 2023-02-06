#pragma once

#include <search/meta/scatter/async/runner.h>
#include <search/meta/scatter/balancer/client_creator.h>

struct TAsyncBalancerTestTraits {
    static void Sleep(TDuration duration) {
        ::Sleep(duration);
    }
    static NScatter::TSourceOptions SourceOptions() {
        NScatter::TSourceOptions sourceOptions;
        sourceOptions.AllowBalancerDynamic = true;
        return sourceOptions;
    }
};

#define Y_SCATTER_UNIT_TEST_IMPL(N) \
    Y_UNIT_TEST(N##AsyncBalancer) { \
        NScatter::AllowBalancerSources(true); \
        RunScatterTest##N<TAsyncTaskRunner, TAsyncBalancerTestTraits>(); \
    }
