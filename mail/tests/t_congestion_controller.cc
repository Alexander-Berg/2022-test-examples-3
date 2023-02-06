#include <yplatform/algorithm/congestion_controller.h>
#include <catch.hpp>
#include <functional>
#include <string>

using namespace yplatform;

struct t_congestion_controller
{
    using test = t_congestion_controller;

    template <typename F, typename... Args>
    auto repeat(std::size_t n, F&& f, const Args&... args)
    {
        std::vector<decltype(std::mem_fn(f)(this, args...))> ret;
        for (std::size_t i = 0; i < n; ++i)
        {
            ret.emplace_back(std::mem_fn(f)(this, args...));
        }
        return ret;
    }

    template <typename F, typename... Args>
    auto x3(F&& f, const Args&... args)
    {
        return repeat(3, f, args...);
    }

    template <typename F, typename... Args>
    auto x5(F&& f, const Args&... args)
    {
        return repeat(5, f, args...);
    }

    std::size_t run_tasks_with_congestion(std::size_t count)
    {
        std::size_t tasks_count = 0;
        for (std::size_t i = 0; i < count; ++i)
        {
            if (controller.try_run())
            {
                ++tasks_count;
            }
        }
        for (std::size_t i = 0; i < tasks_count; ++i)
        {
            controller.finish_with_congestion();
        }
        return tasks_count;
    }

    std::size_t run_tasks_without_congestion(std::size_t count)
    {
        std::size_t tasks_count = 0;
        for (std::size_t i = 0; i < count; ++i)
        {
            if (controller.try_run())
            {
                ++tasks_count;
            }
        }
        for (std::size_t i = 0; i < tasks_count; ++i)
        {
            controller.finish_without_congestion();
        }
        return tasks_count;
    }

    std::size_t run_max_allowed_tasks_without_congestion()
    {
        return run_tasks_without_congestion(controller.limit());
    }

    static constexpr auto run_max_allowed_tasks_without_congestion_ptr =
        &test::run_max_allowed_tasks_without_congestion;

    congestion_controller controller;
};

TEST_CASE_METHOD(t_congestion_controller, "run_task", "")
{
    REQUIRE(controller.try_run());
}

TEST_CASE_METHOD(t_congestion_controller, "finish_not_runned_task_without_congestion", "")
{
    REQUIRE_THROWS(controller.finish_without_congestion());
}

TEST_CASE_METHOD(t_congestion_controller, "finish_not_runned_task_with_congestion", "")
{
    REQUIRE_THROWS(controller.finish_with_congestion());
}

TEST_CASE_METHOD(t_congestion_controller, "run_after_finish_throwed", "")
{
    REQUIRE_THROWS(controller.finish_with_congestion());
    REQUIRE(controller.try_run());
}

TEST_CASE_METHOD(t_congestion_controller, "double_finish", "")
{
    controller.try_run();
    controller.finish_without_congestion();
    REQUIRE_THROWS(controller.finish_without_congestion());
}

TEST_CASE_METHOD(t_congestion_controller, "dont_run_tasks_over_limit", "")
{
    auto limit = controller.limit();
    REQUIRE(run_tasks_without_congestion(limit + 1) == limit);
}

TEST_CASE_METHOD(t_congestion_controller, "limit_dont_grows_when_tasks_finish_with_congestion", "")
{
    auto limit = controller.limit();
    run_tasks_with_congestion(controller.limit());
    REQUIRE(controller.limit() == limit);
}

TEST_CASE_METHOD(
    t_congestion_controller,
    "limit_grows_exponentially_when_tasks_finish_without_congestion",
    "")
{
    auto tasks_count = x3(run_max_allowed_tasks_without_congestion_ptr);
    REQUIRE(tasks_count == std::vector({ tasks_count[0], 2 * tasks_count[0], 4 * tasks_count[0] }));
}

TEST_CASE_METHOD(t_congestion_controller, "limit_grows_no_more_than_double_actual_concurrency", "")
{
    std::size_t concurrency = 1;
    x3(&test::run_tasks_without_congestion, concurrency);
    REQUIRE(controller.limit() <= 2 * concurrency);
}

TEST_CASE_METHOD(t_congestion_controller, "limit_resets_when_task_finish_with_congestion", "")
{
    auto tasks_count_before_congestion = run_max_allowed_tasks_without_congestion();
    run_tasks_with_congestion(1);
    auto tasks_count_after_congestion = run_max_allowed_tasks_without_congestion();
    REQUIRE(tasks_count_before_congestion == tasks_count_after_congestion);
}

TEST_CASE_METHOD(t_congestion_controller, "limit_grows_linearly_after_congestion", "")
{
    run_tasks_with_congestion(1);
    auto tasks_count = x3(run_max_allowed_tasks_without_congestion_ptr);
    REQUIRE(tasks_count == std::vector({ tasks_count[0], tasks_count[0] + 1, tasks_count[0] + 2 }));
}

TEST_CASE_METHOD(
    t_congestion_controller,
    "limit_grows_no_more_than_double_actual_concurrency_after_congestion",
    "")
{
    x3(run_max_allowed_tasks_without_congestion_ptr);
    run_tasks_with_congestion(1);
    std::size_t concurrency = 1;
    x3(&test::run_tasks_without_congestion, concurrency);
    REQUIRE(controller.limit() <= 2 * concurrency);
}

TEST_CASE_METHOD(t_congestion_controller, "limit_recover_exponentially_then_grows_linearly", "")
{
    x3(run_max_allowed_tasks_without_congestion_ptr);
    run_tasks_with_congestion(controller.limit());
    auto tasks_count = x5(run_max_allowed_tasks_without_congestion_ptr);
    REQUIRE(
        tasks_count ==
        std::vector({ tasks_count[0],
                      2 * tasks_count[0],
                      4 * tasks_count[0],
                      4 * tasks_count[0] + 1,
                      4 * tasks_count[0] + 2 }));
}
