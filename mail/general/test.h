#pragma once

#include <boost/make_shared.hpp>
#include <sintimers/timers_queue_interface.h>
#include <sintimers/timer.h>

namespace timers {

class test_queue;

class test_timer : public timer_interface
{
public:
    test_timer (test_queue * owner);
    void async_wait(const std::chrono::steady_clock::duration & ainterval,
        const timer_callback & cb);
    void cancel();
private:
    test_queue * owner_;
};

class test_queue : public timers::queue_interface
{
public:
    test_queue () {}
    timers::timer_ptr create_timer();
    size_t size();

    typedef std::pair<std::chrono::steady_clock::duration, timers::timer_callback> pair_t;
    std::vector<pair_t> timers;
};

}