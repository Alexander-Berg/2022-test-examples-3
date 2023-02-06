#include "../src/rate_controller_impl.h"
#include <catch.hpp>

const std::size_t MAX_CONCURRENCY = 5;
const std::size_t MAX_QUEUE_SIZE = 15;

namespace ymod_ratecontroller {

struct rate_controller_test
{
    rate_controller_test()
        : rc(std::make_shared<rate_controller_impl>(io, MAX_CONCURRENCY, MAX_QUEUE_SIZE))
    {
    }

    void run_io()
    {
        io.run();
        io.reset();
    }

    void call_with_delay(std::function<void()> handler, time_traits::duration delay)
    {
        auto timer = std::make_shared<time_traits::timer>(io);
        timer->expires_from_now(delay);
        timer->async_wait([timer, handler](error_code err) {
            REQUIRE(!err);
            handler();
        });
    }

    std::vector<yplatform::task_context_ptr> make_contexts(std::size_t ctxs_num)
    {
        std::vector<yplatform::task_context_ptr> ctxs(ctxs_num);
        for (std::size_t i = 0; i < ctxs_num; ++i)
        {
            auto ctx = boost::make_shared<yplatform::task_context>(std::to_string(i));
            ctxs[i] = ctx;
        }
        return ctxs;
    }

    rate_controller_ptr rc;
    boost::asio::io_service io;
};

TEST_CASE_METHOD(rate_controller_test, "should run tasks")
{
    std::size_t completed_count = 0;
    for (std::size_t i = 0; i < 2 * MAX_CONCURRENCY; ++i)
    {
        rc->post([&completed_count](error_code err, completion_handler handler) {
            REQUIRE(!err);
            ++completed_count;
            handler();
        });
    }
    run_io();
    REQUIRE(completed_count == 2 * MAX_CONCURRENCY);
}

TEST_CASE_METHOD(rate_controller_test, "should abort tasks on deadline")
{
    // post 2 * MAX_CONCURRENCY tasks, each run 2ms and have deadline 1ms
    // half of them should be aborted due to deadline
    std::size_t completed_count = 0, aborted_count = 0;
    auto task =
        [this, &completed_count, &aborted_count](error_code err, completion_handler handler) {
            if (!err)
            {
                ++completed_count;
            }
            else if (err == error::task_aborted)
            {
                ++aborted_count;
            }
            else
            {
                throw std::runtime_error("unexpected error: " + err.message());
            }
            call_with_delay(handler, time_traits::milliseconds(2));
        };
    for (std::size_t i = 0; i < 2 * MAX_CONCURRENCY; ++i)
    {
        rc->post(task, "", time_traits::clock::now() + time_traits::milliseconds(1));
    }
    run_io();
    REQUIRE(completed_count == MAX_CONCURRENCY);
    REQUIRE(aborted_count == MAX_CONCURRENCY);
}

TEST_CASE_METHOD(rate_controller_test, "should abort tasks if queue capacity exceeded")
{
    std::size_t completed_count = 0, aborted_count = 0;
    auto task =
        [this, &completed_count, &aborted_count](error_code err, completion_handler handler) {
            if (!err)
            {
                ++completed_count;
            }
            else if (err == error::capacity_exceeded)
            {
                ++aborted_count;
            }
            else
            {
                throw std::runtime_error("unexpected error: " + err.message());
            }
            call_with_delay(handler, time_traits::milliseconds(1));
        };
    for (std::size_t i = 0; i < 2 * MAX_QUEUE_SIZE; ++i)
    {
        rc->post(task);
    }
    run_io();
    REQUIRE(completed_count == MAX_QUEUE_SIZE + MAX_CONCURRENCY);
    REQUIRE(aborted_count == MAX_QUEUE_SIZE - MAX_CONCURRENCY);
}

TEST_CASE_METHOD(rate_controller_test, "should cancel tasks")
{
    // post 2 * MAX_CONCURRENCY tasks and cancel last one
    std::size_t completed_count = 0, aborted_count = 0;
    std::size_t cancelled_task_id = 2 * MAX_CONCURRENCY - 1;
    for (std::size_t i = 0; i < 2 * MAX_CONCURRENCY; ++i)
    {
        rc->post(
            [this, &completed_count, &aborted_count, i, cancelled_task_id](
                error_code err, completion_handler handler) {
                call_with_delay(handler, time_traits::milliseconds(2));
                if (!err)
                {
                    ++completed_count;
                }
                else if (err == error::task_aborted)
                {
                    REQUIRE(i == cancelled_task_id);
                    ++aborted_count;
                }
                else
                {
                    throw std::runtime_error("unexpected error: " + err.message());
                }
            },
            std::to_string(i));
    }
    rc->cancel(std::to_string(cancelled_task_id));
    run_io();
    REQUIRE(completed_count == 2 * MAX_CONCURRENCY - 1);
    REQUIRE(aborted_count == 1);
}

TEST_CASE_METHOD(rate_controller_test, "should run task using task context")
{
    bool is_completed = false;
    auto ctx = boost::make_shared<yplatform::task_context>();

    rc->post(ctx, [&is_completed](error_code err, completion_handler handler) {
        REQUIRE(!err);
        is_completed = true;
        handler();
    });
    run_io();
    REQUIRE(is_completed);
}

TEST_CASE_METHOD(rate_controller_test, "should cancel task using task context")
{
    // post 2 * MAX_CONCURRENCY tasks and cancel last one
    std::size_t completed_count = 0, aborted_count = 0;
    std::size_t cancelled_task_id = 2 * MAX_CONCURRENCY - 1;
    auto ctxs = make_contexts(2 * MAX_CONCURRENCY);

    for (std::size_t i = 0; i < 2 * MAX_CONCURRENCY; ++i)
    {
        rc->post(
            ctxs[i],
            [this, &completed_count, &aborted_count, i, cancelled_task_id](
                error_code err, completion_handler handler) {
                call_with_delay(handler, time_traits::milliseconds(2));
                if (!err)
                {
                    ++completed_count;
                }
                else if (err == error::task_aborted)
                {
                    REQUIRE(i == cancelled_task_id);
                    ++aborted_count;
                }
                else
                {
                    throw std::runtime_error("unexpected error: " + err.message());
                }
            });
    }
    rc->cancel(ctxs[cancelled_task_id]);
    run_io();
    REQUIRE(completed_count == 2 * MAX_CONCURRENCY - 1);
    REQUIRE(aborted_count == 1);
}

TEST_CASE_METHOD(rate_controller_test, "should throw 'invalid_argument' exception")
{
    yplatform::task_context_ptr ctx;

    REQUIRE_THROWS_WITH(
        rc->post(ctx, [](error_code, completion_handler handler) { handler(); }),
        "task_context_ptr parameter is nullptr");
    run_io();
}

}
