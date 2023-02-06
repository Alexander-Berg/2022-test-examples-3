#include "tasks/tasks.h"
#include "callback.h"
#include <catch.hpp>

namespace fan::tasks {

struct t_get_campaign_template_params : t_tasks
{
    using result_type = vector<recipient_data>;
    using callback_type = callback<error_code, result_type>;

    vector<recipient_data> RECIPIENTS{ { "a@b.c", {} }, { "d@e.f", {} } };
    vector<recipient_data> RECIPIENTS_WITH_USER_TEMPLATE_PARAMS{
        { "a@b.c", { { "param", "value" } } },
        { "d@e.f", { { "параметр", "значение" } } }
    };

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

TEST_CASE_METHOD(t_get_campaign_template_params, "get_campaign_template_params/http_client_error")
{
    prepare_response(500, "");
    tasks->get_campaign_template_params(ctx, campaign, RECIPIENTS, cb);
    REQUIRE(error(cb));
    REQUIRE(recipients(cb).size() == RECIPIENTS.size());
    REQUIRE(recipients(cb)[0].email == RECIPIENTS[0].email);
    REQUIRE(recipients(cb)[1].email == RECIPIENTS[1].email);
}

TEST_CASE_METHOD(t_get_campaign_template_params, "get_campaign_template_params/invalid_json")
{
    prepare_response(200, "{{{");
    tasks->get_campaign_template_params(ctx, campaign, RECIPIENTS, cb);
    REQUIRE(error(cb));
    REQUIRE(recipients(cb).size() == RECIPIENTS.size());
    REQUIRE(recipients(cb)[0].email == RECIPIENTS[0].email);
    REQUIRE(recipients(cb)[1].email == RECIPIENTS[1].email);
}

TEST_CASE_METHOD(
    t_get_campaign_template_params,
    "get_campaign_template_params/mismatched_params_count")
{
    prepare_response(200, "[{\"p1\":\"v1\",\"p2\":\"v2\"}]");
    tasks->get_campaign_template_params(ctx, campaign, RECIPIENTS, cb);
    REQUIRE(error(cb));
    REQUIRE(recipients(cb).size() == RECIPIENTS.size());
    REQUIRE(recipients(cb)[0].email == RECIPIENTS[0].email);
    REQUIRE(recipients(cb)[1].email == RECIPIENTS[1].email);
}

TEST_CASE_METHOD(
    t_get_campaign_template_params,
    "get_campaign_template_params/parse_template_params")
{
    prepare_response(200, "[{\"p1\":\"v1a\",\"p2\":\"v2a\"},{\"p1\":\"v1b\",\"p2\":\"v2b\"}]");
    tasks->get_campaign_template_params(ctx, campaign, RECIPIENTS, cb);
    REQUIRE(!error(cb));
    REQUIRE(recipients(cb).size() == RECIPIENTS.size());
    REQUIRE(recipients(cb)[0].email == RECIPIENTS[0].email);
    REQUIRE(recipients(cb)[1].email == RECIPIENTS[1].email);
    REQUIRE(
        recipients(cb)[0].template_params ==
        map<string, string>{ { "p1", "v1a" }, { "p2", "v2a" } });
    REQUIRE(
        recipients(cb)[1].template_params ==
        map<string, string>{ { "p1", "v1b" }, { "p2", "v2b" } });
}

TEST_CASE_METHOD(
    t_get_campaign_template_params,
    "get_campaign_template_params/keeps_user_template_params")
{
    prepare_response(200, "[{\"p1\":\"v1a\",\"p2\":\"v2a\"},{\"p1\":\"v1b\",\"p2\":\"v2b\"}]");
    tasks->get_campaign_template_params(ctx, campaign, RECIPIENTS_WITH_USER_TEMPLATE_PARAMS, cb);
    REQUIRE(!error(cb));
    REQUIRE(recipients(cb).size() == RECIPIENTS.size());
    REQUIRE(
        recipients(cb)[0].template_params ==
        map<string, string>{ { "param", "value" }, { "p1", "v1a" }, { "p2", "v2a" } });
    REQUIRE(
        recipients(cb)[1].template_params ==
        map<string, string>{ { "параметр", "значение" }, { "p1", "v1b" }, { "p2", "v2b" } });
}

}
