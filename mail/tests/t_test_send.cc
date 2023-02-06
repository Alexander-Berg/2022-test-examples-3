#include "fakes/delivery.h"
#include "fakes/test_tasks.h"
#include "fakes/timer.h"
#include <test_send/polling_loop.h>
#include <common/errors.h>
#include <catch.hpp>

namespace fan::test_send {

string RECIPIENT = "test_recipient";
string ANOTHER_RECIPIENT = "another_test_recipient";

struct t_polling_loop
{
    using loop_type = polling_loop_impl<fake_test_tasks, fake_delivery, fake_timer>;

    boost::asio::io_service io;
    task_context_ptr ctx = boost::make_shared<task_context>();
    polling_settings settings{ .interval = nanoseconds(1), .max_retries = 0 };
    shared_ptr<fake_test_tasks> tasks = make_shared<fake_test_tasks>();
    shared_ptr<fake_delivery> delivery = make_shared<fake_delivery>();
    shared_ptr<loop_type> loop = std::make_shared<loop_type>(io, ctx, tasks, delivery, settings);

    void add_task()
    {
        tasks->task = test_send_task();
        tasks->task->recipients.push_back(RECIPIENT);
        tasks->task->recipients.push_back(ANOTHER_RECIPIENT);
    }

    void set_error_on_get_pending_task()
    {
        tasks->get_task_err = boost::asio::error::operation_aborted;
    }

    void set_error_on_complete_task()
    {
        tasks->complete_task_err = boost::asio::error::operation_aborted;
    }

    void set_error_on_get_task_eml(string recipient)
    {
        tasks->get_eml_recipient_errors[recipient] = boost::asio::error::operation_aborted;
    }

    void set_error_on_delivery(string recipient)
    {
        delivery->recipient_errors[recipient] = boost::asio::error::operation_aborted;
    }

    void reset_get_pending_task_errors()
    {
        tasks->get_task_err = {};
    }

    auto&& sent_recipients()
    {
        return delivery->sent_recipients;
    }

    auto&& completed_tasks()
    {
        return tasks->completed_task_ids;
    }

    void run_loop()
    {
        yplatform::spawn(loop);
        loop->timer.fire();
    }
};

TEST_CASE_METHOD(t_polling_loop, "no_tasks")
{
    run_loop();
    REQUIRE(sent_recipients().size() == 0);
}

TEST_CASE_METHOD(t_polling_loop, "complete_task")
{
    add_task();
    run_loop();
    REQUIRE(completed_tasks().size());
}

TEST_CASE_METHOD(t_polling_loop, "send_to_recipients")
{
    add_task();
    run_loop();
    REQUIRE(sent_recipients().count(RECIPIENT));
    REQUIRE(sent_recipients().count(ANOTHER_RECIPIENT));
}

TEST_CASE_METHOD(t_polling_loop, "dont_send_on_mark_completed_error")
{
    add_task();
    set_error_on_complete_task();
    run_loop();
    REQUIRE(sent_recipients().size() == 0);
}

TEST_CASE_METHOD(t_polling_loop, "dont_send_on_render_error")
{
    add_task();
    set_error_on_get_task_eml(ANOTHER_RECIPIENT);
    run_loop();
    REQUIRE(sent_recipients().size() == 0);
}

TEST_CASE_METHOD(t_polling_loop, "complete_task_on_errors")
{
    add_task();
    set_error_on_get_task_eml(ANOTHER_RECIPIENT);
    run_loop();
    REQUIRE(completed_tasks().size());
}

TEST_CASE_METHOD(t_polling_loop, "ignore_send_errors")
{
    add_task();
    set_error_on_delivery(RECIPIENT);
    run_loop();
    REQUIRE(sent_recipients().count(ANOTHER_RECIPIENT));
}

TEST_CASE_METHOD(t_polling_loop, "resume_after_error")
{
    set_error_on_get_pending_task();
    run_loop();

    reset_get_pending_task_errors();
    add_task();
    run_loop();

    REQUIRE(sent_recipients().count(RECIPIENT));
}

}
