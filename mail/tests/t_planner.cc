#include "catch.hpp"

#include <streamer/planner.h>

#include <atomic>
#include <chrono>
#include <thread>

using namespace collectors;
using namespace collectors::streamer;
using namespace yplatform::time_traits;

using planner_type = streamer::planner<int>;
using planner_ptr = std::shared_ptr<planner_type>;

class planner_test
{
public:
    planner_test()
    {
        settings->max_concurrency = 2;
        settings->task_penalty = milliseconds(100);
        settings->task_timeout = milliseconds(100);
        planner = std::make_shared<planner_type>(settings, &main_io);

        task_thread = std::thread([this]() { task_io.run(); });
    }

    ~planner_test()
    {
        work_guard.reset();
        task_thread.join();
    }

    void wait_task_io()
    {
        std::atomic_bool ready{ false };
        task_io.post([&ready]() { ready = true; });
        while (!ready)
        {
        } // spin lock
    }

    void run_main_io()
    {
        main_io.reset();
        main_io.run();
    }

    boost::asio::io_context main_io;
    boost::asio::io_context task_io;
    std::thread task_thread;
    using io_context_work_guard =
        boost::asio::executor_work_guard<boost::asio::io_context::executor_type>;
    io_context_work_guard work_guard = boost::asio::make_work_guard(task_io);
    planner_settings_ptr settings = std::make_shared<planner_settings>();
    planner_ptr planner;
    std::vector<int> data;
};

TEST_CASE_METHOD(planner_test, "simple_task_run")
{
    planner->add(0, task_io.wrap([this](auto /*ctx*/, auto handler) {
        data.push_back(0);
        handler(code::ok);
    }));
    run_main_io();

    wait_task_io();
    REQUIRE(data == std::vector{ 0 });
}

TEST_CASE_METHOD(planner_test, "parallelization_limit")
{
    for (int i = 0; i < 3; ++i)
    {
        planner->add(i, task_io.wrap([i, this](auto /*ctx*/, auto handler) {
            data.push_back(i);
            std::this_thread::sleep_for(std::chrono::milliseconds(20));
            handler(code::ok);
        }));
    }
    run_main_io();
    wait_task_io();
    REQUIRE(data == (std::vector{ 0, 1 }));

    run_main_io();
    wait_task_io();
    REQUIRE(data == (std::vector{ 0, 1, 2, 0, 1 }));
}

TEST_CASE_METHOD(planner_test, "run_penalty")
{
    planner->add(0, task_io.wrap([this](auto /*ctx*/, auto handler) {
        data.push_back(0);
        handler(code::ok);
    }));
    run_main_io();
    wait_task_io();
    REQUIRE(data == std::vector{ 0 });
    auto first_run_ts = clock::now();

    run_main_io();
    wait_task_io();
    REQUIRE(clock::now() - first_run_ts >= settings->task_penalty);
    REQUIRE(data == (std::vector{ 0, 0 }));
}

TEST_CASE_METHOD(planner_test, "timeout")
{
    planner->add(0, task_io.wrap([this](auto ctx, auto handler) {
        data.push_back(0);

        std::this_thread::sleep_for(std::chrono::milliseconds(110));
        REQUIRE(ctx->is_cancelled());
        handler(code::ok);
    }));
    run_main_io();
    wait_task_io();
}

TEST_CASE_METHOD(planner_test, "task_aborted")
{
    planner->add(0, task_io.wrap([this](auto /*ctx*/, auto handler) {
        data.push_back(10);
        handler(code::deleted_collector);
    }));
    run_main_io();
    wait_task_io();

    std::this_thread::sleep_for(std::chrono::milliseconds(110));
    run_main_io();
    wait_task_io();
    REQUIRE(data == (std::vector{ 10 }));
}
