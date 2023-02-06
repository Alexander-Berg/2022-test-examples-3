#include <mocks/mock_manager.h>
#include <mocks/query_dispatcher.h>
#include <mocks/api.h>
#include <mocks/collector_service.h>
#include <mocks/db.h>
#include <mocks/processor.h>
#include <scheduler/dispatcher.h>
#include <yplatform/find.h>
#include <gtest/gtest.h>

namespace yrpopper::scheduler {

using namespace yrpopper;

auto init_query_dispatcher()
{
    auto mock_query_dispatcher = std::make_shared<mock::query_dispatcher>();
    mock_query_dispatcher->init_mock();
    return mock_query_dispatcher;
}

auto mock_query_dispatcher = init_query_dispatcher();
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

class dispatcher_test : public ::testing::Test
{
protected:
    const std::string FAKE_OWNER{ "fake_owner" };
    const std::string FAKE_HOST{ "fake_host" };
    const popid_t FAKE_POPID{ 42 };

    boost::shared_ptr<yplatform::task_context> context_;
    settings_ptr settings_;
    dispatcher<mock::db> dispatcher_;

    dispatcher_test()
        : context_{ new yplatform::task_context() }
        , settings_{ create_settings(FAKE_OWNER, FAKE_HOST) }
        , dispatcher_{ mock_db }
    {
        settings_->pool.reset(new yplatform::active::pool());
        settings_->pool->open(8);
        mock_manager.reset();
        mock_manager.init_mock();
        dispatcher_.init(settings_);
        dispatcher_.start();
    }

    ~dispatcher_test()
    {
        dispatcher_.stop();
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
        mock_db->create_cb(index);
        return task_;
    }

    future_void_t run(popid_t popid)
    {
        std::set<TaskRunParams::RunFlag> flags{ TaskRunParams::RunFlag::Syncronized };
        auto params = std::make_shared<TaskRunParams>(flags);
        auto res = dispatcher_.manual_run(context_, popid, params);
        return res;
    }
};

TEST_F(dispatcher_test, db_did_not_load_task_to_queue)
{
    auto res = run(0);

    EXPECT_THROW(res.get(), task_not_exists_error);
}

TEST_F(dispatcher_test, valid_task_pop3_succeeds)
{
    auto task = create_task(FAKE_POPID);
    dispatcher_.push_task(task);
    auto res = run(FAKE_POPID);

    EXPECT_NO_THROW(res.get());
    EXPECT_EQ(mock_processor->process_called_count, 1);
    EXPECT_EQ(mock_processor->process_requested_id, FAKE_POPID);
}

TEST_F(dispatcher_test, valid_task_imap_succeeds)
{
    auto task = create_task(FAKE_POPID);
    task->use_imap = true;
    dispatcher_.push_task(task);
    auto res = run(FAKE_POPID);

    EXPECT_NO_THROW(res.get());
    EXPECT_NE(mock_collector_service->collector->step_called_count, 0);
    EXPECT_EQ(mock_collector_service->collector->requested_popid, FAKE_POPID);
}

TEST_F(dispatcher_test, handles_exception_from_collector_step)
{
    mock_collector_service->collector->set_step_exception(std::runtime_error("random error"));

    auto task = create_task(FAKE_POPID);
    task->use_imap = true;
    dispatcher_.push_task(task);
    auto res = run(FAKE_POPID);

    EXPECT_NO_THROW(res.get());
    EXPECT_NE(mock_collector_service->collector->step_called_count, 0);
}

TEST_F(dispatcher_test, error_if_time_till_last_connect_not_reached)
{
    settings_->force_penalty = 1'000'000;

    auto task = create_task(FAKE_POPID);
    dispatcher_.push_task(task);
    auto res = run(FAKE_POPID);

    EXPECT_THROW(res.get(), frequent_run_error);
}

TEST_F(dispatcher_test, error_if_maximum_tasks_limit_exceeded)
{
    settings_->max_tasks = 0;

    auto task = create_task(FAKE_POPID);
    dispatcher_.push_task(task);
    auto res = run(FAKE_POPID);

    EXPECT_THROW(res.get(), task_limits_reached_error);
}

TEST_F(dispatcher_test, error_if_max_active_tasks_limit_exceeded)
{
    settings_->host_limits = create_limits(FAKE_HOST, 0, 0);

    auto task = create_task(FAKE_POPID);
    dispatcher_.push_task(task);
    auto res = run(FAKE_POPID);

    EXPECT_THROW(res.get(), host_limits_reached_error);
}

TEST_F(dispatcher_test, handles_task_validity_bad_task)
{
    mock_api->set_check_task_validity_result(api::task_validity::task_bad);

    auto task = create_task(FAKE_POPID);
    dispatcher_.push_task(task);
    auto res = run(FAKE_POPID);

    EXPECT_NO_THROW(res.get());
    EXPECT_EQ(mock_api->enable_called_count, 1);
    EXPECT_EQ(mock_api->enable_requested_id, FAKE_POPID);
    EXPECT_EQ(mock_db->update_task_called_count, 1);
}

TEST_F(dispatcher_test, handles_exception_from_task_validity)
{
    mock_api->set_check_task_validity_exception(std::runtime_error("random error"));

    auto task = create_task(FAKE_POPID);
    dispatcher_.push_task(task);
    auto res = run(FAKE_POPID);

    EXPECT_NO_THROW(res.get());
    EXPECT_EQ(mock_db->update_task_called_count, 1);
}

}
