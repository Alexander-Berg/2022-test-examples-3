#include "mod_hms/mod.h"
#include <catch.hpp>

namespace yxiva::mobile::hms {

using yplatform::json_value;
using std::string;

static constexpr auto DEFAULT_PAYLOAD = "{}";
static constexpr auto DEFAULT_TTL = 3600u;
static constexpr auto DEFAULT_TRANSIT_ID = "abcdef";
static constexpr auto DEFAULT_NOTIFICATION_TITLE = "message title";
static constexpr auto DEFAULT_NOTIFICATION_BODY = "message body";
static constexpr auto DEFAULT_TOKEN = "111111";

auto ttl_to_str(unsigned ttl)
{
    return std::to_string(ttl) + "s";
}

auto make_json(std::initializer_list<std::pair<string, json_value>> params)
{
    json_value payload;
    for (auto&& [key, value] : params)
    {
        payload[key] = value;
    }
    return payload;
}

auto default_click_action()
{
    return make_json({ { "type", 1 }, { "intent", DEFAULT_TRANSIT_ID } });
}

auto default_notification()
{
    return make_json(
        { { "title", DEFAULT_NOTIFICATION_TITLE }, { "body", DEFAULT_NOTIFICATION_BODY } });
}

auto default_android_notification()
{
    auto notification = default_notification();
    notification["click_action"] = default_click_action();
    return notification;
}

auto default_android()
{
    return make_json(
        { { "notification", default_android_notification() }, { "ttl", ttl_to_str(DEFAULT_TTL) } });
}

auto default_notification_message()
{
    auto tokens = json_value(json_type::tarray);
    tokens.push_back(json_value(DEFAULT_TOKEN));
    return make_json({ { "android", default_android() },
                       { "notification", default_notification() },
                       { "data", DEFAULT_PAYLOAD },
                       { "token", tokens } });
}

struct mobile_task_context_fixture
{
    mobile_task_context_fixture()
    {
        init_ctx();
    }

    void init_ctx()
    {
        init_ctx(DEFAULT_TOKEN, DEFAULT_PAYLOAD, DEFAULT_TTL, DEFAULT_TRANSIT_ID, {});
    }

    void init_ctx(
        const string& token,
        const string& payload,
        unsigned ttl,
        const string& transit_id,
        const yhttp::params_initializer_list& platform_params)
    {
        auto webserver_ctx = boost::make_shared<ymod_webserver::context>();
        auto req = boost::make_shared<ymod_webserver::request>(webserver_ctx);
        ctx = boost::make_shared<mobile_task_context>(req);

        ctx->token = token;

        std::tie(ctx->payload, ctx->ttl, ctx->transit_id) = std::tie(payload, ttl, transit_id);
        set_platform_params(platform_params);
    }

    void set_platform_params(const yhttp::params_initializer_list& platform_params)
    {
        make_uri("/push/hms" + yhttp::url_encode(platform_params), ctx->request->url);
    }

    mobile_task_context_ptr ctx;
};

string first_key_not_from_whitelist(
    const json_value& json,
    const std::unordered_set<string>& whitelist)
{
    for (auto it = json.members_begin(); it != json.members_end(); ++it)
    {
        auto key = string(it.key());
        if (!whitelist.count(key))
        {
            return key;
        }
    }
    return {};
}

TEST_CASE_METHOD(
    mobile_task_context_fixture,
    "mod_hms/prepare_hms_message/empty_payload_string",
    "")
{
    ctx->payload = "";

    json_value body;
    auto prepared = prepare_hms_message(ctx, body);

    REQUIRE(!prepared.first);
    CHECK(prepared.second == error::invalid_payload);
}

TEST_CASE_METHOD(
    mobile_task_context_fixture,
    "mod_hms/prepare_hms_message/invalid_json_payload",
    "")
{
    ctx->payload = R"({ "a" : "b" )";

    json_value body;
    auto prepared = prepare_hms_message(ctx, body);

    REQUIRE(!prepared.first);
    CHECK(prepared.second == error::invalid_payload);
}

TEST_CASE_METHOD(
    mobile_task_context_fixture,
    "mod_hms/prepare_hms_message/silent/empty_payload",
    "")
{
    json_value body;
    auto prepared = prepare_hms_message(ctx, body);

    REQUIRE("" == prepared.first.error_reason);
    CHECK("" == first_key_not_from_whitelist(body["message"], { "data", "token", "android" }));
    CHECK("" == first_key_not_from_whitelist(body["message"]["android"], { "ttl" }));
    CHECK(body["message"]["data"] == "{}");
    CHECK(body["message"]["android"]["ttl"] == ttl_to_str(DEFAULT_TTL));
}

TEST_CASE_METHOD(mobile_task_context_fixture, "mod_hms/prepare_hms_message/silent", "")
{
    json_value payload;
    payload["key"] = "value";
    ctx->payload = payload.stringify();
    json_value body;
    auto prepared = prepare_hms_message(ctx, body);

    REQUIRE("" == prepared.first.error_reason);
    CHECK("" == first_key_not_from_whitelist(body["message"], { "data", "token", "android" }));
    CHECK("" == first_key_not_from_whitelist(body["message"]["android"], { "ttl" }));
    CHECK(body["message"]["data"] == ctx->payload);
    CHECK(body["message"]["android"]["ttl"] == ttl_to_str(DEFAULT_TTL));
}

TEST_CASE_METHOD(mobile_task_context_fixture, "mod_hms/prepare_hms_message/bright/no_title", "")
{
    set_platform_params({ { "x-notification", R"({ "body": "message body" })" } });
    json_value body;
    auto prepared = prepare_hms_message(ctx, body);

    REQUIRE(!prepared.first);
    CHECK(prepared.second == error::code::invalid_payload);
}

TEST_CASE_METHOD(mobile_task_context_fixture, "mod_hms/prepare_hms_message/bright/no_body", "")
{
    set_platform_params({ { "x-notification", R"({ "title": "message title" })" } });
    json_value body;
    auto prepared = prepare_hms_message(ctx, body);

    REQUIRE(!prepared.first);
    CHECK(prepared.second == error::code::invalid_payload);
}

TEST_CASE_METHOD(
    mobile_task_context_fixture,
    "mod_hms/prepare_hms_message/bright/insert_default_values",
    "")
{
    set_platform_params({ { "x-notification", default_notification().stringify() } });
    json_value body;
    auto prepared = prepare_hms_message(ctx, body);

    REQUIRE("" == prepared.first.error_reason);
    CAPTURE(body["message"].stringify());
    CHECK(body["message"] == default_notification_message());
}

TEST_CASE_METHOD(
    mobile_task_context_fixture,
    "mod_hms/prepare_hms_message/bright/default_click_action/ctx_id_instead_of_empty_transit_id",
    "")
{
    ctx->transit_id = "";
    set_platform_params({ { "x-notification", default_notification().stringify() } });
    json_value body;
    auto prepared = prepare_hms_message(ctx, body);

    REQUIRE("" == prepared.first.error_reason);
    CAPTURE(body["message"].stringify());
    CHECK(body["message"]["android"]["notification"]["click_action"]["intent"] == ctx->uniq_id());
}

TEST_CASE_METHOD(
    mobile_task_context_fixture,
    "mod_hms/prepare_hms_message/bright/not_rewrite_client_click_action",
    "")
{
    json_value custom_click_action;
    custom_click_action["intent"] = "CUSTOM_INTENT";
    custom_click_action["type"] = 3;
    auto notification = default_notification();
    notification["click_action"] = custom_click_action;

    set_platform_params({ { "x-notification", notification.stringify() } });

    json_value body;
    auto prepared = prepare_hms_message(ctx, body);

    REQUIRE("" == prepared.first.error_reason);
    CHECK(body["message"]["android"]["notification"]["click_action"] == custom_click_action);
}

TEST_CASE_METHOD(
    mobile_task_context_fixture,
    "mod_hms/prepare_hms_message/bright/transfer_all_platform_specific",
    "")
{
    auto notification = default_notification();
    notification["channel_id"] = "default_notification_channel";
    auto urgency = "HIGH";
    auto collapse_key = -1;
    set_platform_params({ { "x-notification", notification.stringify() },
                          { "x-urgency", json_value(urgency).stringify() },
                          { "x-collapse_key", json_value(collapse_key).stringify() } });
    json_value body;
    auto prepared = prepare_hms_message(ctx, body);
    REQUIRE("" == prepared.first.error_reason);
    CHECK(body["message"]["android"]["urgency"] == urgency);
    CHECK(body["message"]["android"]["collapse_key"] == collapse_key);
    CHECK(body["message"]["android"]["notification"]["channel_id"] == notification["channel_id"]);
}

}
