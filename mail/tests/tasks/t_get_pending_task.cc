#include "tasks/tasks.h"
#include "callback.h"
#include <boost/format.hpp>
#include <catch.hpp>

namespace fan::tasks {

string TASK_ID = "123";
string ACCOUNT_SLUG = "test_account_slug";
string CAMPAIGN_SLUG = "test_campaign_slug";
string FROM_EMAIL = "yapoptest@yandex.ru";
string RECIPIENT = "yapoptest02@yandex.ru";
string TASK_TEMPLATE = "{\"id\":%1%,\"account_slug\":\"%2%\",\"campaign_slug\":\"%3%\",\"from_"
                       "email\":\"%4%\",\"recipients\":[\"%5%\"]}";
string TASK =
    (boost::format(TASK_TEMPLATE) % TASK_ID % ACCOUNT_SLUG % CAMPAIGN_SLUG % FROM_EMAIL % RECIPIENT)
        .str();
string TASK_LIST = "[" + TASK + "]";

struct t_get_pending_task : t_tasks
{
    using result_type = optional<test_send_task>;
    using callback_type = callback<error_code, result_type>;

    callback_type cb;

    error_code error(callback_type& cb)
    {
        return std::get<0>(cb.args());
    }

    result_type task(callback_type& cb)
    {
        return std::get<1>(cb.args());
    }
};

TEST_CASE_METHOD(t_get_pending_task, "get_pending_task/http_client_error")
{
    prepare_response(500, "");
    tasks->get_pending_task(ctx, cb);
    REQUIRE(error(cb));
}

TEST_CASE_METHOD(t_get_pending_task, "get_pending_task/invalid_json")
{
    prepare_response(200, "{{{");
    tasks->get_pending_task(ctx, cb);
    REQUIRE(error(cb));
}

TEST_CASE_METHOD(t_get_pending_task, "get_pending_task/empty_tasks")
{
    prepare_response(200, "[]");
    tasks->get_pending_task(ctx, cb);
    REQUIRE(!error(cb));
    REQUIRE(!task(cb));
}

TEST_CASE_METHOD(t_get_pending_task, "get_pending_task/task_without_fields")
{
    prepare_response(200, "[{}]");
    tasks->get_pending_task(ctx, cb);
    REQUIRE(error(cb));
}

TEST_CASE_METHOD(t_get_pending_task, "get_pending_task/parse_task")
{
    prepare_response(200, TASK_LIST);
    tasks->get_pending_task(ctx, cb);
    REQUIRE(!error(cb));
    REQUIRE(task(cb));
    REQUIRE(task(cb)->id == TASK_ID);
    REQUIRE(task(cb)->account_slug == ACCOUNT_SLUG);
    REQUIRE(task(cb)->campaign_slug == CAMPAIGN_SLUG);
    REQUIRE(task(cb)->from_email == FROM_EMAIL);
    REQUIRE(task(cb)->recipients.size() == 1);
    REQUIRE(task(cb)->recipients[0] == RECIPIENT);
}

}
