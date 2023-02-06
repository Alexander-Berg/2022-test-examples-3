#include "web/webui/json_helpers.h"
#include "idm_helpers.h"
#include <catch.hpp>

namespace yxiva::web::webui {

struct t_write_service
{
    json_value_ref result()
    {
        return json["result"];
    }

    bool json_contains_app(const json_value& json_apps, const tvm_app_info& app)
    {
        for (auto&& json_app : json_apps.array_items())
        {
            tvm_app_info app_from_json{ static_cast<uint32_t>(json_app["id"].to_uint64()),
                                        json_app["name"].to_string(),
                                        json_app["suspended"].to_bool() };
            if (app_from_json == app) return true;
        }
        return false;
    }

    json_value json;
};

TEST_CASE_METHOD(t_write_service, "write_service/tvm_roles/empty")
{
    auto service = make_service_data("", "");

    write_service(result(), *service);

    REQUIRE(result()["tvm_publishers"].empty());
    REQUIRE(result()["tvm_subscribers"].empty());
}

TEST_CASE_METHOD(t_write_service, "write_service/tvm_roles/one_app")
{
    auto services = make_service_data("", "", { { "sandbox", { 1, "app1", true } } });

    write_service(result(), *services);

    REQUIRE(result()["tvm_publishers"].size() == 1);
    REQUIRE(result()["tvm_publishers"]["sandbox"].size() == 1);
    REQUIRE(json_contains_app(result()["tvm_publishers"]["sandbox"], { 1, "app1", true }));
    REQUIRE(result()["tvm_subscribers"].empty());
}

TEST_CASE_METHOD(t_write_service, "write_service/tvm_roles/many_apps")
{
    auto services = make_service_data(
        "",
        "",
        { { "sandbox", { 1, "app1" } }, { "sandbox", { 2, "app2" } }, { "corp", { 3, "app3" } } },
        { { "production", { 4, "app4" } }, { "production", { 5, "app5", true } } });

    write_service(result(), *services);

    REQUIRE(result()["tvm_publishers"].size() == 2);
    REQUIRE(result()["tvm_publishers"]["sandbox"].size() == 2);
    REQUIRE(json_contains_app(result()["tvm_publishers"]["sandbox"], { 1, "app1", false }));
    REQUIRE(json_contains_app(result()["tvm_publishers"]["sandbox"], { 2, "app2", false }));
    REQUIRE(result()["tvm_publishers"]["corp"].size() == 1);
    REQUIRE(json_contains_app(result()["tvm_publishers"]["corp"], { 3, "app3", false }));
    REQUIRE(result()["tvm_subscribers"].size() == 1);
    REQUIRE(result()["tvm_subscribers"]["production"].size() == 2);
    REQUIRE(json_contains_app(result()["tvm_subscribers"]["production"], { 4, "app4", false }));
    REQUIRE(json_contains_app(result()["tvm_subscribers"]["production"], { 5, "app5", true }));
}

struct t_parse_service_properties
{
    t_parse_service_properties()
        : json(json_parse_no_type_check(R"({
            "name": "appmetrica",
            "description": "",
            "is_passport": false,
            "is_stream": false,
            "queued_delivery_by_default": true,
            "stream_count": 0,
            "revoked": false,
            "owner": "",
            "auth_disabled": false,
            "oauth_scopes": [],
            "send_tokens": {},
            "listen_tokens": {},
            "apps": []
        })"))
    {
    }

    void append_app_to_json(
        const string& role_name,
        const string& environment,
        const tvm_app_info& app)
    {
        auto&& json_app = json[role_name][environment].push_back();
        json_app["id"] = app.id;
        json_app["name"] = app.name;
        json_app["suspended"] = app.suspended;
    }

    bool contains_app(const std::set<tvm_app_info>& apps, const tvm_app_info& app)
    {
        return std::count(apps.begin(), apps.end(), app);
    }

    json_value json;
    service_properties data;
};

TEST_CASE_METHOD(
    t_parse_service_properties,
    "parse_json_properties/service_properties/tvm_roles/no_json_members")
{
    auto res = parse_json_properties(json, data);

    REQUIRE(res.success());
    REQUIRE(data.tvm_publishers.empty());
    REQUIRE(data.tvm_subscribers.empty());
}

TEST_CASE_METHOD(
    t_parse_service_properties,
    "parse_json_properties/service_properties/tvm_roles/one_app")
{
    append_app_to_json("tvm_publishers", "sandbox", { 1, "app1", false });

    auto res = parse_json_properties(json, data);

    REQUIRE(res.success());
    REQUIRE(data.tvm_publishers["sandbox"].size() == 1);
    REQUIRE(contains_app(data.tvm_publishers["sandbox"], { 1, "app1", false }));
    REQUIRE(data.tvm_subscribers.empty());
}

TEST_CASE_METHOD(
    t_parse_service_properties,
    "parse_json_properties/service_properties/tvm_roles/many_apps")
{
    append_app_to_json("tvm_publishers", "sandbox", { 1, "app1", false });
    append_app_to_json("tvm_publishers", "sandbox", { 2, "app2", true });
    append_app_to_json("tvm_publishers", "corp", { 3, "app3", false });
    append_app_to_json("tvm_subscribers", "production", { 4, "app4", false });
    append_app_to_json("tvm_subscribers", "production", { 5, "app5", false });

    auto res = parse_json_properties(json, data);

    REQUIRE(res.success());
    REQUIRE(data.tvm_publishers["sandbox"].size() == 2);
    REQUIRE(contains_app(data.tvm_publishers["sandbox"], { 1, "app1", false }));
    REQUIRE(contains_app(data.tvm_publishers["sandbox"], { 2, "app2", true }));
    REQUIRE(data.tvm_publishers["corp"].size() == 1);
    REQUIRE(contains_app(data.tvm_publishers["corp"], { 3, "app3", false }));
    REQUIRE(data.tvm_subscribers["production"].size() == 2);
    REQUIRE(contains_app(data.tvm_subscribers["production"], { 4, "app4", false }));
    REQUIRE(contains_app(data.tvm_subscribers["production"], { 5, "app5", false }));
}

}
