#pragma once

#include <stdint.h>

class ntimer_t
{
public:
    ntimer_t() : has_run_(0), running_(false)
    {
        start();
    }
    void start()
    {
        clock_gettime(CLOCK_MONOTONIC, &initiated_);
        running_ = true;
    }
    uint64_t stop() // milliseconds
    {
        if (running_)
        {
            has_run_ = shot();
            running_ = false;
        }
        return has_run_;
    }
    uint64_t shot() // milliseconds
    {
        timespec now;
        clock_gettime(CLOCK_MONOTONIC, &now);
        return get_nanoseconds(now) - get_nanoseconds(initiated_);
    }

protected:
    uint64_t get_nanoseconds(const timespec& time)
    {
        return static_cast<uint64_t>(time.tv_sec) * 1000000000UL + time.tv_nsec;
    }

    timespec initiated_;
    uint64_t has_run_;
    bool running_;
};
