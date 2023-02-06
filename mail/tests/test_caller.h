#pragma once

#include <mutex>

#include <ymod_paxos/task.h>
#include <ymod_paxos/event.h>
#include <ymod_paxos/caller.h>
#include <ymod_paxos/exception.h>

using ymod_paxos::Task;
using ymod_paxos::event_t;

class caller_counter
{
private:
    typedef scoped_lock lock_guard;

public:
    int results;
    int errors;
    mutex mutex;

    caller_counter() : results(0), errors(0)
    {
    }

    void on_result()
    {
        lock_guard lock(mutex);
        results++;
    }

    void on_error()
    {
        lock_guard lock(mutex);
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

    void set_attribute(string const& name, string const& value)
    {
    }

    void set_result(string const& task_id, string const& data)
    {
        counter_->on_result();
//        std::cout << "test_caller result for task " << task_id
//                << ": " << data
//                << std::endl;
    }

    void set_error(string const& task_id, ymod_paxos::error const& e)
    {
        counter_->on_error();
//        std::cerr << "test_caller error for task " << task_id
//                << ": [code: "<< e.code() << "] "
//                << e.what()
//                << std::endl;
    }
};
