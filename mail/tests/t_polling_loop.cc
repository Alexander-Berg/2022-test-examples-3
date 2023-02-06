#include "fakes/send_campaign_op.h"
#include "fakes/send_tasks.h"
#include "fakes/delivery.h"
#include "fakes/timer.h"
#include <send/polling_loop.h>
#include <common/errors.h>
#include <catch.hpp>

namespace fan::send {

struct t_polling_loop
{
    using send_op_type = fake_send_campaign_op<fake_send_tasks, fake_delivery>;
    using loop_type = polling_loop_impl<send_op_type, fake_send_tasks, fake_delivery, fake_timer>;

    recipient_data RECIPIENT{ "yapoptest@yandex.ru", {} };

    boost::asio::io_service io;
    task_context_ptr ctx = boost::make_shared<task_context>();
    polling_settings settings;
    shared_ptr<fake_send_tasks> tasks = make_shared<fake_send_tasks>();
    shared_ptr<fake_delivery> delivery = make_shared<fake_delivery>();
    shared_ptr<loop_type> loop;

    t_polling_loop()
    {
        settings.interval = nanoseconds(1);
        settings.max_retries = 0;
        loop = std::make_shared<loop_type>(io, ctx, 0, 1, tasks, delivery, settings);
    }

    void add_campaign()
    {
        tasks->campaigns.emplace_back(campaign{});
        tasks->recipients.emplace_back(RECIPIENT);
    }

    auto&& sent_recipients()
    {
        return delivery->sent_recipients;
    }

    string campaign_state()
    {
        return tasks->state;
    }

    void set_error_on_get_pending_campaigns()
    {
        tasks->get_campaigns_err = boost::asio::error::operation_aborted;
    }

    void reset_get_pending_campaigns_error()
    {
        tasks->get_campaigns_err = {};
    }

    void set_error_on_get_recipients()
    {
        tasks->get_recipients_err = boost::asio::error::operation_aborted;
    }

    void run_loop()
    {
        yplatform::spawn(loop);
        loop->timer.fire();
    }
};

TEST_CASE_METHOD(t_polling_loop, "polling_loop/no_pending_campaigns")
{
    run_loop();
    REQUIRE(sent_recipients().size() == 0);
}

TEST_CASE_METHOD(t_polling_loop, "polling_loop/run_send_op")
{
    add_campaign();
    run_loop();
    REQUIRE(sent_recipients().size());
}

TEST_CASE_METHOD(t_polling_loop, "polling_loop/dont_run_send_op_on_get_campaigns_error")
{
    add_campaign();
    set_error_on_get_pending_campaigns();
    run_loop();
    REQUIRE(sent_recipients().size() == 0);
}

TEST_CASE_METHOD(t_polling_loop, "polling_loop/mark_failed")
{
    add_campaign();
    set_error_on_get_recipients();
    run_loop();
    REQUIRE(campaign_state() == "failed");
}

TEST_CASE_METHOD(t_polling_loop, "polling_loop/resume_after_error")
{
    add_campaign();
    set_error_on_get_pending_campaigns();
    run_loop();

    reset_get_pending_campaigns_error();
    run_loop();
    REQUIRE(sent_recipients().size());
}

}
