#pragma once

#include <tasks/test_tasks.h>
#include <delivery/module.h>
#include <typed_log/typed_log.h>
#include <common/test_send_task.h>
#include <common/errors.h>
#include <common/types.h>
#include <yplatform/coroutine.h>
#include <yplatform/yield.h>

namespace fan::test_send {

template <typename Tasks, typename Delivery>
struct run_task_op : std::enable_shared_from_this<run_task_op<Tasks, Delivery>>
{
    using yield_context = yplatform::yield_context<run_task_op>;

    task_context_ptr task_ctx;
    test_send_task task;
    shared_ptr<Tasks> tasks;
    shared_ptr<Delivery> delivery;
    without_data_cb cb;

    error_code err;
    vector<string_ptr> emls;
    string_ptr cur_eml;
    size_t recipient_num = 0;
    size_t sent_recipients = 0;

    run_task_op(
        task_context_ptr task_ctx,
        const test_send_task& task,
        shared_ptr<Tasks> tasks,
        shared_ptr<Delivery> delivery,
        const without_data_cb& cb)
        : task_ctx(task_ctx), task(task), tasks(tasks), delivery(delivery), cb(cb)
    {
    }

    void operator()(yield_context yield_ctx)
    {
        reenter(yield_ctx)
        {
            for (recipient_num = 0; recipient_num < task.recipients.size(); ++recipient_num)
            {
                yield tasks->get_task_eml(
                    task_ctx,
                    task.id,
                    task.recipients[recipient_num],
                    yield_ctx.capture(err, cur_eml));
                if (err) break;
                emls.push_back(cur_eml);
            }
            if (err)
            {
                log_error("get eml error: " + err.message());
                yield break;
            }
            yield tasks->complete_task(task_ctx, task.id, yield_ctx.capture(err));
            if (err)
            {
                log_error("complete task error: " + err.message());
                yield break;
            }
            for (recipient_num = 0; recipient_num < task.recipients.size(); ++recipient_num)
            {
                yield send(
                    task,
                    task.recipients[recipient_num],
                    emls[recipient_num],
                    yield_ctx.capture(err));
                // Ignore error
                if (!err) ++sent_recipients;
            }
            err = error_code();
            log_success();
        }
        if (yield_ctx.is_complete())
        {
            cb(err);
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
            log_error(err.what());
        }
        cb(error::test_send_error);
    }

    template <typename Handler>
    void send(
        const test_send_task& task,
        const string& recipient,
        string_ptr eml,
        const Handler& handler)
    {
        mail_message message = { .from_email = task.from_email,
                                 .recipient = recipient,
                                 .eml = eml,
                                 .account_slug = task.account_slug,
                                 .campaign_slug = task.campaign_slug };
        delivery->send(task_ctx, message, handler);
    }

    void log_error(const string& reason)
    {
        typed::log_test_send_task_processed(task_ctx, task, "error", reason);
    }

    void log_success()
    {
        typed::log_test_send_task_processed(
            task_ctx,
            task,
            "success",
            "",
            { { "sent_recipients", std::to_string(sent_recipients) } });
    }
};

}

#include <yplatform/unyield.h>
