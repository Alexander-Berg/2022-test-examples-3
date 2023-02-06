#pragma once

#include <functional>

#include <search/meta/scatter/balancer/client_creator.h>
#include <search/meta/scatter/balancer/runner.h>

#include <library/cpp/coroutine/engine/impl.h>

struct TBalancerProxyTestTraits {
    static void Sleep(TDuration duration) {
        RunningCont()->SleepT(duration);
    }
    static NScatter::TSourceOptions SourceOptions() {
        NScatter::TSourceOptions sourceOptions;
        sourceOptions.AllowBalancerDynamic = true;
        return sourceOptions;
    }
};

void RunInBalancerWorker(std::function<void()> f);

class TReplyEvent {
public:
    TReplyEvent();
    ~TReplyEvent();
    void WaitI() noexcept;
    void Signal() noexcept;
private:
    class TImpl;
    THolder<TImpl> Impl;
};

#define Y_SCATTER_UNIT_TEST_IMPL(N) \
    Y_UNIT_TEST(N##BalancerProxy) { \
        RunInBalancerWorker([]() { \
            RunScatterTest##N<NBalancerScatter::TBalancerTaskRunner, TBalancerProxyTestTraits>(); \
        }); \
    }
