#pragma once

#include <mocks/mock.h>
#include <scheduler/settings.h>
#include <scheduler/task_index.h>
#include <ymod_pq/bind_array.h>
#include <functional>
#include <memory>

namespace yrpopper::mock {

using namespace yrpopper::scheduler;

class db
    : public std::enable_shared_from_this<db>
    , public yrpopper::mock::mock
{
public:
    mutable int request_task_called_count = 0;
    mutable int update_task_called_count = 0;
    promise_void_t request_task_result;
    promise_bool_t update_task_result;

    using create_callback = std::function<void(const task_index&)>;
    using erase_callback = std::function<void(popid_t)>;

    create_callback create_cb;
    erase_callback erase_cb;

    db()
    {
    }

    void set_request_task_result(const VoidResult& result)
    {
        request_task_result.reset();
        request_task_result.set(result);
    }

    void set_request_task_exception(const std::exception& exception)
    {
        request_task_result.reset();
        request_task_result.set_exception(exception);
    }

    void set_update_task_result(bool result)
    {
        update_task_result.reset();
        update_task_result.set(result);
    }

    void set_update_task_exception(const std::exception& exception)
    {
        update_task_result.reset();
        update_task_result.set_exception(exception);
    }

    void init_mock() override
    {
        request_task_result.set(VoidResult{});
        update_task_result.set(true);
    }

    void reset() override
    {
        request_task_called_count = 0;
        update_task_called_count = 0;
        request_task_result.reset();
        update_task_result.reset();
    }

    void start()
    {
    }

    void stop()
    {
    }

    void set_task_callbacks(const create_callback& create_cb, const erase_callback& erase_cb)
    {
        this->create_cb = create_cb;
        this->erase_cb = erase_cb;
    }

    future_bool_t update_task(const rpop_context_ptr, const task_status_ptr, int) const
    {
        ++update_task_called_count;
        return update_task_result;
    }
};

}
