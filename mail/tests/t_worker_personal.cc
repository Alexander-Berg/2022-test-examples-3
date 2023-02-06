#include "../src/worker/worker_procedures.h"
#include "../src/worker/xtask_context.h"
#include "helpers.h"
#include <yxiva/core/types.h>
#include <catch.hpp>
#include <vector>

#define TEST_ID "00001_mail"
#define TEST_UID "00001"
#define TEST_SERVICE "mail"
#define TEST_TTL 1
#define TIME 44000

using namespace yxiva;
using namespace yxiva::hub;
using namespace yxiva::hub::worker;

struct T_WORKER_CONTEXT
{
    T_WORKER_CONTEXT()
    {
        ymod_xtasks::task task = { TEST_ID, TEST_UID, TEST_SERVICE, 0 };
        auto devnull_log = boost::make_shared<mod_log>();
        devnull_log->init(yplatform::ptree{});
        context = boost::make_shared<xtask_context>(task, devnull_log);
    }

    message_builder add_message(
        local_id_t id,
        const string& service = TEST_SERVICE,
        const string& operation = "test")
    {
        context->messages.push_back(make_message(TEST_UID, service, id, operation));
        return message_builder{ *context->messages.back() };
    }

    subscription_builder add_sub(
        const string& subscription_id,
        const string& client,
        local_id_t local_id)
    {
        sub_params_t params(
            subscription_id, "", "http://localhost", "", client, subscription_id, TEST_TTL);
        params.init_local_id = local_id;
        params.ack_local_id = params.init_local_id;
        params.init_time = params.ack_time = 0;
        sub_t sub(params);
        context->subscriptions.push_back(sub);
        return subscription_builder{ context->subscriptions.back() };
    }

    void check_steps(size_t job_index, const std::vector<local_id_t>& ids, local_id_t final_id)
    {
        auto& job = context->jobs[job_index];
        REQUIRE(job.steps.size() == ids.size());
        for (auto i = 0U; i < ids.size(); ++i)
        {
            REQUIRE(job.steps[i].message->local_id == ids[i]);
        }
        REQUIRE(job.final_id == final_id);
    }

    xtask_context_ptr context;
    yplatform::log::source logger;
    string filter_skip_all = R"({
      "vars": {},
      "rules": [ { "do": "skip" } ]
    })";
    string filter_skip_tag1 = R"({
      "vars": { "A": { "$has_tags": ["tag1"] } },
      "rules": [ { "if": "A", "do": "skip" }, { "do": "send_bright" } ]
    })";
};

TEST_CASE_METHOD(T_WORKER_CONTEXT, "worker/prepare_jobs/nothing_to_send_if_ack_equals_last", "")
{
    add_message(1);
    add_message(2);
    add_sub("a", "test", 2);
    prepare_jobs(context, logger);
    REQUIRE(context->jobs.size() == 0);
}

TEST_CASE_METHOD(T_WORKER_CONTEXT, "worker/prepare_jobs/send_all_if_ack_0", "")
{
    add_message(1);
    add_message(2);
    add_sub("a", "test", 0);
    prepare_jobs(context, logger);
    REQUIRE(context->jobs.size() == 1);
    check_steps(0, { 1, 2 }, 0);
}

TEST_CASE_METHOD(T_WORKER_CONTEXT, "worker/prepare_jobs/send_all_if_ack_less_than_min", "")
{
    add_message(3);
    add_message(4);
    add_sub("a", "test", 2);
    prepare_jobs(context, logger);
    REQUIRE(context->jobs.size() == 1);
    check_steps(0, { 3, 4 }, 2);
}

TEST_CASE_METHOD(
    T_WORKER_CONTEXT,
    "worker/prepare_jobs/send_all_but_missing_if_ack_less_than_min",
    "")
{
    add_message(4);
    add_message(6);
    add_sub("a", "test", 2);
    prepare_jobs(context, logger);
    REQUIRE(context->jobs.size() == 1);
    check_steps(0, { 4, 6 }, 2);
}

TEST_CASE_METHOD(T_WORKER_CONTEXT, "worker/prepare_jobs/skip_one_sub_and_send_to_another", "")
{
    add_message(1);
    add_message(2);
    add_sub("a", "test", 0);
    add_sub("b", "test", 2);
    prepare_jobs(context, logger);
    REQUIRE(context->jobs.size() == 1);
    check_steps(0, { 1, 2 }, 0);
}

TEST_CASE_METHOD(
    T_WORKER_CONTEXT,
    "worker/prepare_jobs/send_all_in_one_sub_and_send_partial_to_another",
    "")
{
    add_message(1);
    add_message(2);
    add_sub("a", "test", 0);
    add_sub("b", "test", 1);
    prepare_jobs(context, logger);
    REQUIRE(context->jobs.size() == 2);
    check_steps(0, { 1, 2 }, 0);
    check_steps(1, { 2 }, 1);
}

TEST_CASE_METHOD(T_WORKER_CONTEXT, "worker/prepare_jobs/skip_all_by_filter_updates_position", "")
{
    add_message(1);
    add_message(2);
    add_sub("a", "test", 0).filter(filter_skip_all);
    prepare_jobs(context, logger);
    REQUIRE(context->jobs.size() == 1);
    check_steps(0, {}, 2);
}

TEST_CASE_METHOD(T_WORKER_CONTEXT, "worker/prepare_jobs/skip_one_by_tag", "")
{
    add_message(2).tag("tag1");
    add_message(3);
    add_sub("a", "test", 0).filter(filter_skip_tag1);
    prepare_jobs(context, logger);
    REQUIRE(context->jobs.size() == 1);
    check_steps(0, { 3 }, 0);
}

TEST_CASE_METHOD(T_WORKER_CONTEXT, "worker/prepare_jobs/if_all_filtered_update_position", "")
{
    add_message(2).tag("tag1");
    add_message(3).tag("tag1");
    add_sub("a", "test", 0).filter(filter_skip_tag1);
    prepare_jobs(context, logger);
    REQUIRE(context->jobs.size() == 1);
    check_steps(0, {}, 3);
}

TEST_CASE_METHOD(
    T_WORKER_CONTEXT,
    "worker/prepare_jobs/skip_too_old_messages_for_new_subscription",
    "")
{
    add_message(1).ts(100);
    add_sub("a", "test", 0).init_time(200);
    prepare_jobs(context, logger);
    REQUIRE(context->jobs.size() == 1);
    check_steps(0, {}, 1);
}

TEST_CASE_METHOD(T_WORKER_CONTEXT, "worker/prepare_jobs/slip_message_with_zero_ts", "")
{
    add_message(1).ts(0);
    add_sub("a", "test", 0).init_time(100);
    prepare_jobs(context, logger);
    REQUIRE(context->jobs.size() == 1);
    check_steps(0, {}, 1);
}

TEST_CASE_METHOD(T_WORKER_CONTEXT, "worker/prepare_jobs/send_fresh_message", "")
{
    add_message(1).ts(101);
    add_sub("a", "test", 0).init_time(100);
    prepare_jobs(context, logger);
    REQUIRE(context->jobs.size() == 1);
    check_steps(0, { 1 }, 0);
}

TEST_CASE_METHOD(T_WORKER_CONTEXT, "worker/prepare_jobs/send_fresh_message_with_zero_delta", "")
{
    add_message(1).ts(100);
    add_sub("a", "test", 0).init_time(100);
    prepare_jobs(context, logger);
    REQUIRE(context->jobs.size() == 1);
    check_steps(0, { 1 }, 0);
}

TEST_CASE_METHOD(T_WORKER_CONTEXT, "worker/prepare_jobs/send_fresh_message_with_1s_delta", "")
{
    add_message(1).ts(100);
    add_sub("a", "test", 0).init_time(101);
    prepare_jobs(context, logger);
    REQUIRE(context->jobs.size() == 1);
    check_steps(0, { 1 }, 0);
}

TEST_CASE_METHOD(
    T_WORKER_CONTEXT,
    "worker/prepare_jobs/"
    "no_notifications_if_too_old_messages_for_new_subscription_and_all_filtered_for_old_one",
    "")
{
    add_message(1).tag("tag1").ts(100);
    add_message(2).ts(100);
    add_sub("a", "test", 0).filter(filter_skip_tag1).init_time(200);
    add_sub("b", "test", 1).filter(filter_skip_all).init_time(200);
    prepare_jobs(context, logger);
    REQUIRE(context->jobs.size() == 2);
    check_steps(0, {}, 2);
    check_steps(1, {}, 2);
}