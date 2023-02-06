#include "mail/collectors-ext/src/scheduler/task_index.h"
#include <mocks/mock_manager.h>
#include <mocks/api.h>
#include <mocks/collector_service.h>
#include <mocks/db.h>
#include <mocks/processor.h>
#include <scheduler/dispatcher.h>
#include <stdexcept>
#include <yplatform/find.h>
#include <gtest/gtest.h>

namespace yrpopper::scheduler {

using namespace yrpopper;

namespace {
auto mock_api = std::make_shared<mock::api>();
auto mock_processor = std::make_shared<mock::processor>();
auto mock_collector_service = std::make_shared<mock::collector_service>();
auto mock_db = std::make_shared<mock::db>();

mock::mock_manager mock_manager{ std::static_pointer_cast<mock::mock>(mock_api),
                                 std::static_pointer_cast<mock::mock>(mock_processor),
                                 std::static_pointer_cast<mock::mock>(mock_collector_service),
                                 std::static_pointer_cast<mock::mock>(mock_db) };

host_limit_map create_limits(
    const std::string& host,
    uint64_t max_active_tasks,
    std::time_t min_penalty)
{
    host_limit_map host_limits;
    host_limits[host] = host_limits_t{ max_active_tasks, min_penalty };
    return host_limits;
}

settings_ptr create_settings(const std::string& owner, const std::string& host)
{
    auto settings = boost::make_shared<scheduler::settings>(owner);
    settings->penalty = 0;
    settings->force_penalty = 0;
    settings->finished_penalty_multiplier = 0.0;
    versioned_keys_t mock_dkeys = {};
    settings->dkeys = mock_dkeys;
    settings->host_limits = create_limits(host, 100, 0);
    return settings;
}
}

class test_plan_queue : public ::testing::Test
{
protected:
    const std::string FAKE_OWNER{ "fake_owner" };
    const std::string FAKE_HOST{ "fake_host" };
    const popid_t FAKE_POPID{ 42 };

    boost::shared_ptr<yplatform::task_context> context_;
    settings_ptr settings_;
    plan_queue plan_queue_;
    TaskRunParamsPtr params{ std::shared_ptr<TaskRunParams>() };
    std::map<popid_t, start_result> exec_plan_;
    using trace_type = std::deque<popid_t>;
    trace_type exec_trace_;

    test_plan_queue()
        : context_{ new yplatform::task_context() }
        , settings_{ create_settings(FAKE_OWNER, FAKE_HOST) }
        , plan_queue_{}
    {
        settings_->pool.reset(new yplatform::active::pool());
        settings_->pool->open(8);
        mock_manager.reset();
        mock_manager.init_mock();
        plan_queue_.init(
            [this](task_index ti) {
                auto popid = ti.task()->popid;
                if (!exec_plan_.count(popid))
                {
                    throw std::logic_error("missing popod " + std::to_string(popid) + " exec_plan");
                }
                exec_trace_.push_back(popid);
                auto result = exec_plan_[popid];
                exec_plan_.erase(popid);
                return result;
            },
            settings_);
    }

    ~test_plan_queue()
    {
        settings_->pool->close();
        settings_->pool->clear();
    }

    task_ptr create_task(popid_t popid)
    {
        task_ptr task_ = boost::make_shared<task>();
        task_->popid = popid;
        task_->server = FAKE_HOST;
        task_->last_connect = std::time(0);
        task_index index{ task_ };
        return task_;
    }

    void push_tasks(std::vector<popid_t> ids)
    {
        for (auto id : ids)
        {
            plan_queue_.push(create_task(id));
        }
    }

    void pop_tasks(std::vector<popid_t> ids)
    {
        for (auto id : ids)
        {
            plan_queue_.pop(id);
        }
    }

    void run()
    {
        plan_queue_.run_once();
    }
};

TEST_F(test_plan_queue, empty_does_nothing)
{
    EXPECT_NO_THROW(plan_queue_.run_once());
    EXPECT_EQ(plan_queue_.pending_tasks_count(), 0);
    EXPECT_EQ(plan_queue_.total_tasks_count(), 0);
}

TEST_F(test_plan_queue, push_single_task_enqueues_that_task)
{
    push_tasks({ 1 });
    EXPECT_EQ(plan_queue_.pending_tasks_count(), 1);
    EXPECT_EQ(plan_queue_.total_tasks_count(), 1);
}

TEST_F(test_plan_queue, push_multiple_tasks_enqueues_those_tasks)
{
    push_tasks({ 1, 2, 3 });
    EXPECT_EQ(plan_queue_.pending_tasks_count(), 3);
    EXPECT_EQ(plan_queue_.total_tasks_count(), 3);
}

TEST_F(test_plan_queue, push_does_not_add_same_task_twice)
{
    push_tasks({ 1, 1 });
    EXPECT_EQ(plan_queue_.pending_tasks_count(), 1);
    EXPECT_EQ(plan_queue_.total_tasks_count(), 1);
}

TEST_F(test_plan_queue, find_returns_previously_enqueued_task)
{
    push_tasks({ 1 });
    EXPECT_EQ(plan_queue_.find(1)->task()->popid, 1);
    EXPECT_EQ(plan_queue_.total_tasks_count(), 1);
}

TEST_F(test_plan_queue, find_returns_empty_optional_if_task_was_not_previously_added)
{
    EXPECT_FALSE(plan_queue_.find(1).has_value());
}

TEST_F(test_plan_queue, run_single_task_ok)
{
    push_tasks({ 1 });
    exec_plan_ = { { 1, start_result_ok } };
    plan_queue_.run_once();
    EXPECT_EQ(exec_trace_, trace_type({ 1 }));
    EXPECT_EQ(plan_queue_.pending_tasks_count(), 0);
    EXPECT_EQ(plan_queue_.total_tasks_count(), 1);
}

TEST_F(test_plan_queue, new_tasks_are_executed_in_push_order)
{
    push_tasks({ 1, 2, 3 });
    exec_plan_ = { { 1, start_result_ok }, { 2, start_result_ok }, { 3, start_result_ok } };
    plan_queue_.run_once();
    EXPECT_EQ(exec_trace_, trace_type({ 1, 2, 3 }));
}

TEST_F(test_plan_queue, run_multiple_tasks)
{
    push_tasks({ 1, 2, 3 });
    exec_plan_ = { { 1, start_result_ok }, { 2, start_result_ok }, { 3, start_result_ok } };
    plan_queue_.run_once();
    EXPECT_EQ(exec_trace_, trace_type({ 1, 2, 3 }));
    EXPECT_EQ(plan_queue_.pending_tasks_count(), 0);
    EXPECT_EQ(plan_queue_.total_tasks_count(), 3);
}

TEST_F(test_plan_queue, tasks_popped_before_run_are_not_executed)
{
    push_tasks({ 1, 2, 3 });
    pop_tasks({ 1, 2 });
    exec_plan_ = { { 3, start_result_ok } };
    EXPECT_NO_THROW(plan_queue_.run_once());
    EXPECT_EQ(exec_trace_, trace_type({ 3 }));
}

TEST_F(test_plan_queue, tasks_popped_after_execution_are_not_reenqueued)
{
    push_tasks({ 1, 2, 3 });
    exec_plan_ = { { 1, start_result_ok }, { 2, start_result_ok }, { 3, start_result_ok } };
    plan_queue_.run_once();
    pop_tasks({ 1, 2 });
    plan_queue_.on_task_finished(1);
    plan_queue_.on_task_finished(2);
    EXPECT_EQ(plan_queue_.pending_tasks_count(), 0);
    EXPECT_EQ(plan_queue_.total_tasks_count(), 1);
}

TEST_F(test_plan_queue, popping_non_enqueued_task_does_not_dequeue_previously_added_tasks)
{
    push_tasks({ 1 });
    pop_tasks({ 2 });
    EXPECT_EQ(plan_queue_.pending_tasks_count(), 1);
    EXPECT_EQ(plan_queue_.total_tasks_count(), 1);
}

TEST_F(test_plan_queue, tasks_over_limit_are_delayed)
{
    push_tasks({ 1, 2, 3 });
    exec_plan_ = { { 1, start_result_ok },
                   { 2, start_result_ok },
                   { 3, start_result_err_all_limit } };

    plan_queue_.run_once();
    EXPECT_EQ(exec_trace_, trace_type({ 1, 2, 3 }));
    EXPECT_EQ(plan_queue_.pending_tasks_count(), 1);
    EXPECT_EQ(plan_queue_.total_tasks_count(), 3);
}

TEST_F(test_plan_queue, delayed_tasks_are_retried)
{
    push_tasks({ 1, 2, 3 });
    exec_plan_ = { { 1, start_result_ok },
                   { 2, start_result_ok },
                   { 3, start_result_err_all_limit } };
    plan_queue_.run_once();
    exec_plan_[3] = start_result_ok;
    plan_queue_.run_once();
    EXPECT_EQ(exec_trace_, trace_type({ 1, 2, 3, 3 }));
    EXPECT_EQ(plan_queue_.pending_tasks_count(), 0);
    EXPECT_EQ(plan_queue_.total_tasks_count(), 3);
}

TEST_F(test_plan_queue, run_does_nothing_when_all_active)
{
    push_tasks({ 1, 2, 3 });
    exec_plan_ = { { 1, start_result_ok }, { 2, start_result_ok }, { 3, start_result_ok } };
    plan_queue_.run_once();
    plan_queue_.run_once();
    EXPECT_EQ(exec_trace_, trace_type({ 1, 2, 3 }));
    EXPECT_EQ(plan_queue_.pending_tasks_count(), 0);
    EXPECT_EQ(plan_queue_.total_tasks_count(), 3);
}

TEST_F(test_plan_queue, already_running_are_reenqueued)
{
    push_tasks({ 1, 2, 3 });
    exec_plan_ = { { 1, start_result_ok },
                   { 2, start_result_err_already },
                   { 3, start_result_ok } };
    plan_queue_.run_once();
    EXPECT_EQ(exec_trace_, trace_type({ 1, 2, 3 }));
    EXPECT_EQ(plan_queue_.pending_tasks_count(), 1);
    EXPECT_EQ(plan_queue_.total_tasks_count(), 3);
}

TEST_F(test_plan_queue, reenqueued_are_handled_next_step)
{
    push_tasks({ 1, 2, 3 });
    exec_plan_ = { { 1, start_result_ok },
                   { 2, start_result_err_already },
                   { 3, start_result_ok } };
    plan_queue_.run_once();
    exec_plan_ = { { 2, start_result_ok } };
    plan_queue_.run_once();
    EXPECT_EQ(exec_trace_, trace_type({ 1, 2, 3, 2 }));
    EXPECT_EQ(plan_queue_.pending_tasks_count(), 0);
    EXPECT_EQ(plan_queue_.total_tasks_count(), 3);
}

TEST_F(test_plan_queue, finished_are_reenqueued)
{
    push_tasks({ 1, 2, 3 });
    exec_plan_ = { { 1, start_result_ok }, { 2, start_result_ok }, { 3, start_result_ok } };
    plan_queue_.run_once();
    plan_queue_.on_task_finished(2);
    EXPECT_EQ(plan_queue_.pending_tasks_count(), 1);
    EXPECT_EQ(plan_queue_.total_tasks_count(), 3);
}

TEST_F(test_plan_queue, one_finished_is_executed_repeatedly)
{
    push_tasks({ 1, 2 });
    exec_plan_ = { { 1, start_result_ok }, { 2, start_result_ok } };
    plan_queue_.run_once();
    plan_queue_.on_task_finished(1);
    exec_plan_ = { { 1, start_result_ok } };
    plan_queue_.run_once();
    EXPECT_EQ(exec_trace_, trace_type({ 1, 2, 1 }));
    EXPECT_EQ(plan_queue_.pending_tasks_count(), 0);
    EXPECT_EQ(plan_queue_.total_tasks_count(), 2);
}

TEST_F(test_plan_queue, all_finished_are_executed_repeatedly)
{
    push_tasks({ 1, 2 });
    exec_plan_ = { { 1, start_result_ok }, { 2, start_result_ok } };
    plan_queue_.run_once();
    plan_queue_.on_task_finished(1);
    plan_queue_.on_task_finished(2);
    exec_plan_ = { { 1, start_result_ok }, { 2, start_result_ok } };
    plan_queue_.run_once();
    EXPECT_EQ(exec_trace_, trace_type({ 1, 2, 1, 2 }));
    EXPECT_EQ(plan_queue_.pending_tasks_count(), 0);
    EXPECT_EQ(plan_queue_.total_tasks_count(), 2);
}

TEST_F(test_plan_queue, all_finished_are_executed_in_fin_order)
{
    push_tasks({ 1, 2 });
    exec_plan_ = { { 1, start_result_ok }, { 2, start_result_ok } };
    plan_queue_.run_once();
    plan_queue_.on_task_finished(2);
    plan_queue_.on_task_finished(1);
    exec_plan_ = { { 1, start_result_ok }, { 2, start_result_ok } };
    plan_queue_.run_once();
    EXPECT_EQ(exec_trace_, trace_type({ 1, 2, 2, 1 }));
}

// task->use_imap = true;

}
