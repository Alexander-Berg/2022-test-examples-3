#pragma once

#include <common/test_send_task.h>
#include <common/types.h>

namespace fan {

struct fake_test_tasks
{
    using pending_task_cb = function<void(error_code, optional<test_send_task>)>;

    error_code get_task_err;
    error_code complete_task_err;
    map<string, error_code> get_eml_recipient_errors;
    optional<test_send_task> task;
    set<string> completed_task_ids;

    void get_pending_task(task_context_ptr, pending_task_cb cb)
    {
        cb(get_task_err, task);
    }

    void complete_task(task_context_ptr, string task_id, without_data_cb cb)
    {
        if (!complete_task_err)
        {
            completed_task_ids.insert(task_id);
        }
        cb(complete_task_err);
    }

    void get_task_eml(task_context_ptr, string, string recipient, string_ptr_cb cb)
    {
        error_code err;
        if (get_eml_recipient_errors.count(recipient))
        {
            err = get_eml_recipient_errors[recipient];
        }
        cb(err, make_shared<string>(""));
    }
};

}
