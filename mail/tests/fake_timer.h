#pragma once

#include <multipaxos/types.h>
#include <sintimers/timers_queue_interface.h>

using namespace multipaxos;

struct FakeTimer : public timers::timer_interface
{
    time_duration interval;
    timers::timer_callback callback;
    bool canceled;

    FakeTimer() : canceled(false)
    {
    }

    void async_wait(const time_duration& _interval, const timers::timer_callback& _callback)
    {
        interval = _interval;
        callback = _callback;
        canceled = false;
    }

    virtual void cancel()
    {
        canceled = true;
    }
};

struct FakeTimersQueue : public timers::queue_interface
{
    std::vector<shared_ptr<FakeTimer>> timers;

    timers::timer_ptr create_timer()
    {
        shared_ptr<FakeTimer> timer(new FakeTimer());
        timers.push_back(timer);
        return timer;
    }
};
