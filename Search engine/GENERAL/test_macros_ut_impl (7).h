#pragma once

#include <search/meta/scatter/phantom/env/phantom_env.h>
#include <search/meta/scatter/phantom/runner.h>

struct TPhantomTestTraits {
    static void Sleep(TDuration duration);
    static NScatter::TSourceOptions SourceOptions() {
        return {};
    }
};

#define Y_SCATTER_UNIT_TEST_IMPL(N) \
    Y_UNIT_TEST(N##Phantom) { \
        RunInPhantom([]() { \
            RunScatterTest##N<TPhantomTaskRunner, TPhantomTestTraits>(); \
        }); \
    }
