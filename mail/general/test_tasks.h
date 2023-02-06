#pragma once

#include <common/test_send_task.h>
#include <common/types.h>

namespace fan::tasks {

struct test_tasks
{
    using pending_task_cb = function<void(error_code, const optional<test_send_task>&)>;

    virtual ~test_tasks() = default;

    virtual void get_pending_task(task_context_ptr ctx, const pending_task_cb& cb) = 0;

    virtual void get_task_eml(
        task_context_ptr ctx,
        const string& task_id,
        const string& recipient,
        const string_ptr_cb& cb) = 0;

    virtual void complete_task(
        task_context_ptr ctx,
        const string& task_id,
        const without_data_cb& cb) = 0;
};

}
