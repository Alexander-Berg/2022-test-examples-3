#include "tasks/tasks.h"
#include "callback.h"
#include <catch.hpp>

namespace fan::tasks {

struct t_get_campaign_recipients : t_tasks
{
    using result_type = vector<recipient_data>;
    using callback_type = callback<error_code, result_type>;

    campaign campaign;
    callback_type cb;

    error_code error(callback_type& cb)
    {
        return std::get<0>(cb.args());
    }

    result_type recipients(callback_type& cb)
    {
        return std::get<1>(cb.args());
    }
};

TEST_CASE_METHOD(t_get_campaign_recipients, "get_campaign_recipients/http_client_error")
{
    prepare_response(500, "");
    tasks->get_campaign_recipients(ctx, campaign, cb);
    REQUIRE(error(cb));
    REQUIRE(recipients(cb).empty());
}

TEST_CASE_METHOD(t_get_campaign_recipients, "get_campaign_recipients/invalid_json")
{
    prepare_response(200, "{{{");
    tasks->get_campaign_recipients(ctx, campaign, cb);
    REQUIRE(error(cb));
    REQUIRE(recipients(cb).empty());
}

TEST_CASE_METHOD(t_get_campaign_recipients, "get_campaign_recipients/empty_recipients")
{
    prepare_response(200, "[]");
    tasks->get_campaign_recipients(ctx, campaign, cb);
    REQUIRE(!error(cb));
    REQUIRE(recipients(cb).empty());
}

TEST_CASE_METHOD(t_get_campaign_recipients, "get_campaign_recipients/parse_recipients")
{
    prepare_response(200, "[\"a@b.c\",\"d@e.f\"]");
    tasks->get_campaign_recipients(ctx, campaign, cb);
    REQUIRE(!error(cb));
    REQUIRE(recipients(cb).size() == 2);
    REQUIRE(recipients(cb)[0].email == "a@b.c");
    REQUIRE(recipients(cb)[1].email == "d@e.f");
}

TEST_CASE_METHOD(
    t_get_campaign_recipients,
    "get_campaign_recipients/parse_recipients_with_user_template_params")
{
    prepare_response(200, R"(
        [
            {
                "email": "a@b.c",
                "user_template_params": {
                    "param": "value"
                }
            },
            {
                "email": "d@e.f",
                "user_template_params": {
                    "параметр": "значение"
                }
            }
        ]
    )");
    tasks->get_campaign_recipients(ctx, campaign, cb);
    REQUIRE(!error(cb));
    REQUIRE(recipients(cb).size() == 2);
    REQUIRE(recipients(cb)[0].email == "a@b.c");
    REQUIRE(recipients(cb)[1].email == "d@e.f");
    REQUIRE(recipients(cb)[0].template_params == map<string, string>{ { "param", "value" } });
    REQUIRE(recipients(cb)[1].template_params == map<string, string>{ { "параметр", "значение" } });
}

}
