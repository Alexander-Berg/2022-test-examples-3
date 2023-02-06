#pragma once

#include <mutex>

#include <ymod_paxos/caller.h>
#include <ymod_paxos/error.h>

using ymod_paxos::mutex_t;
using ymod_paxos::lock_t;

class caller_counter
{
public:
    int results;
    int errors;
    mutex_t mutex;

    caller_counter() : results(0), errors(0)
    {
    }

    void on_result()
    {
        lock_t lock(mutex);
        results++;
    }

    void on_error()
    {
        lock_t lock(mutex);
        errors++;
    }
};

class test_caller : public ymod_paxos::icaller
{
    caller_counter* counter_;

public:
    test_caller(caller_counter* counter) : counter_(counter)
    {
    }

    void set_attribute(const string& /*name*/, const string& /*value*/)
    {
    }

    void set_result(const string& /*task_id*/, const string& /*data*/)
    {
        counter_->on_result();
        //        std::cout << "test_caller result for task " << task_id
        //                << ": " << data
        //                << std::endl;
    }

    void set_error(const string& /*task_id*/, const ymod_paxos::error& /*e*/)
    {
        //      std::cout << "** error: " << e.what() << std::endl;
        counter_->on_error();
        //        std::cerr << "test_caller error for task " << task_id
        //                << ": [code: "<< e.code() << "] "
        //                << e.what()
        //                << std::endl;
    }
};
