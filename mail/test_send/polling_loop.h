#pragma once

#include "run_task_op.h"
#include "settings.h"
#include <tasks/test_tasks.h>
#include <delivery/module.h>
#include <typed_log/typed_log.h>
#include <common/test_send_task.h>
#include <common/types.h>
#include <yplatform/coroutine.h>
#include <yplatform/yield.h>

namespace fan::test_send {

template <typename Tasks, typename Delivery, typename Timer>
struct polling_loop_impl : std::enable_shared_from_this<polling_loop_impl<Tasks, Delivery, Timer>>
{
    using yield_context = yplatform::yield_context<polling_loop_impl>;

    io_service& io;
    Timer timer;
    task_context_ptr task_ctx;
    shared_ptr<Tasks> tasks;
    shared_ptr<Delivery> delivery;
    polling_settings settings;
    error_code err;
    optional<test_send_task> task;
    size_t attempt_num = 0;
    duration next_retry_interval;
    bool stopped = false;

    polling_loop_impl(
        io_service& io,
        task_context_ptr task_ctx,
        shared_ptr<Tasks> tasks,
        shared_ptr<Delivery> delivery,
        const polling_settings& settings)
        : io(io)
        , timer(io)
        , task_ctx(task_ctx)
        , tasks(tasks)
        , delivery(delivery)
        , settings(settings)
    {
    }

    void operator()(yield_context yield_ctx)
    {
        reenter(yield_ctx)
        {
            while (!stopped)
            {
                yield wait(settings.interval, yield_ctx.capture(err));
                if (err) break;
                yield tasks->get_pending_task(task_ctx, yield_ctx.capture(err, task));
                if (err)
                {
                    LERR_(task_ctx) << "get pending task error: " << err.message();
                    continue;
                }
                if (!task) continue;
                attempt_num = 0;
                next_retry_interval = settings.interval;
                do
                {
                    if (attempt_num)
                    {
                        next_retry_interval *= settings.backoff_multiplier;
                        yield wait(next_retry_interval, yield_ctx.capture(err));
                    }
                    if (stopped) yield break;
                    yield run_task(*task, yield_ctx.capture(err));
                    ++attempt_num;
                } while (attempt_num <= settings.max_retries && err);
                if (err) // Complete task anyway if retries didn't help
                {
                    yield tasks->complete_task(task_ctx, task->id, yield_ctx.capture(err));
                    // Ignore error
                }
            }
        }
    }

    void operator()(std::exception_ptr exception)
    {
        try
        {
            std::rethrow_exception(exception);
        }
        catch (const std::exception& err)
        {
            LERR_(task_ctx) << "polling_loop exception: " << err.what();
            yplatform::spawn(io, shared_from(this));
        }
    }

    void stop()
    {
        stopped = true;
    }

    template <typename Handler>
    void wait(const duration& interval, const Handler& handler)
    {
        timer.expires_from_now(interval);
        timer.async_wait(handler);
    }

    template <typename Handler>
    void run_task(const test_send_task& task, const Handler& handler)
    {
        auto op = std::make_shared<run_task_op<Tasks, Delivery>>(
            task_ctx, task, tasks, delivery, handler);
        yplatform::spawn(op);
    }
};

using polling_loop = polling_loop_impl<tasks::test_tasks, delivery::sender, timer>;
using polling_loop_ptr = shared_ptr<polling_loop>;
}

#include <yplatform/unyield.h>
