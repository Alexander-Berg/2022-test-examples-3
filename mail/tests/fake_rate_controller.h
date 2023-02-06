#pragma once

#include <common/types.h>
#include <ymod_ratecontroller/rate_controller.h>
#include <yplatform/application/repository.h>
#include <yplatform/module.h>

namespace botserver {

struct fake_rate_controller
    : ymod_ratecontroller::rate_controller
    , ymod_ratecontroller::rate_controller_module
    , yplatform::module
{
    using completion_handler = function<void()>;
    using task_type = function<void(error_code, completion_handler)>;

    bool run_tasks = true;

    void post(
        const task_type& task,
        const string& /*task_id*/ = "",
        const time_point& /*deadline*/ = time_point::max()) override
    {
        if (run_tasks)
        {
            task({}, [] {});
        }
    }

    void cancel(const string& /*task_id*/) override
    {
    }

    ymod_ratecontroller::rate_controller_ptr get_controller(
        const std::string& /*resource_path*/) override
    {
        return shared_from(this);
    }

    std::size_t max_concurrency() const override
    {
        return 1;
    }

    std::size_t queue_size() const override
    {
        return 0;
    }

    std::size_t running_tasks_count() const override
    {
        return 0;
    }
};

}