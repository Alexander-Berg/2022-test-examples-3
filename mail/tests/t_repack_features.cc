#include <yxiva/core/json.h>
#include <yxiva/core/repacker.h>
#include <yplatform/encoding/url_encode.h>
#include <catch.hpp>
#include <boost/regex.hpp>

using namespace yxiva;

inline auto make_message(string payload)
{
    struct message message;
    message.operation = "eventname";
    message.transit_id = "test";
    message.raw_data = payload;
    return message;
}

template <typename Repacker>
auto repack_impl(Repacker repacker, struct message message, repack_features features)
{
    task_context_ptr ctx{ new task_context() };
    push_subscription_params sub;
    sub.platform = "apns";
    sub.repack_features = features;
    sub.service = "test";
    struct packet packet(ctx, message, sub);
    return repacker.repack(packet, sub);
}

template <typename Repacker>
string repack(Repacker repacker, string payload, repack_features features)
{
    auto [error, req_queue] = repack_impl(repacker, make_message(payload), features);
    if (error) return error;
    if (req_queue.size() == 0) return "no requests generated"s;
    if (req_queue.size() > 1) return "too many requests: "s;
    return req_queue.pop_front().payload();
}

template <typename Repacker>
string repack_url_params(Repacker repacker, struct message message, repack_features features)
{
    auto [error, req_queue] = repack_impl(repacker, message, features);
    if (error) return error;
    if (req_queue.size() == 0) return "no requests generated"s;
    if (req_queue.size() > 1) return "too many requests: "s;
    return req_queue.pop_front().http_url_params();
}

template <typename Repacker>
string repack_url_params(Repacker repacker, string payload, repack_features features)
{
    return repack_url_params(repacker, make_message(payload), features);
}

string default_repack_rules = R"(
{
    "repack_payload": ["*"]
}
)";

auto make_custom_repacker(string repack_rules = default_repack_rules)
{
    custom_repacker result;
    if (auto ret = custom_repacker::from_json_string(repack_rules, result); !ret)
    {
        throw std::runtime_error("failed to create custom repacker: " + ret.error_reason);
    }
    return result;
}

TEST_CASE("repack/features/default")
{
    repack_features features;
    string payload = "{}";

    SECTION("bypass_repacker")
    {
        auto result = repack(bypass_repacker(), payload, features);
        REQUIRE(result == R"({})");
    }

    SECTION("custom_repacker")
    {
        auto result = repack(make_custom_repacker(), payload, features);
        REQUIRE(result == R"({})");
    }
}

TEST_CASE("repack/features/transit_id_injection/xiva")
{
    repack_features features;
    features.inject_transit_id = true;
    string payload = "{}";

    SECTION("bypass_repacker")
    {
        auto result = repack(bypass_repacker(), payload, features);
        REQUIRE(result == R"({"xiva":{"transit_id":"test"}})");
    }

    SECTION("custom_repacker")
    {
        auto result = repack(make_custom_repacker(), payload, features);
        REQUIRE(result == R"({"xiva":{"transit_id":"test"}})");
    }
}

TEST_CASE("repack/features/transit_id_injection/yamp")
{
    repack_features features;
    features.inject_transit_id = true;
    string payload = R"({"yamp":{}})";

    SECTION("bypass_repacker")
    {
        auto result = repack(bypass_repacker(), payload, features);
        REQUIRE(result == R"({"yamp":{"i":"test"}})");
    }

    SECTION("custom_repacker")
    {
        auto result = repack(make_custom_repacker(), payload, features);
        REQUIRE(result == R"({"yamp":{"i":"test"}})");
    }
}

TEST_CASE("repack/features/transit_id_injection_appmetrica/yamp")
{
    repack_features features;
    features.inject_transit_id = true;
    features.transit_id_appmetrica_format = true;
    string payload = R"({"yamp":{}})";

    SECTION("bypass_repacker")
    {
        auto result = repack(bypass_repacker(), payload, features);
        REQUIRE(result == R"({"yamp":{"i":"xt=test&xe=eventname"}})");
    }

    SECTION("custom_repacker")
    {
        auto result = repack(make_custom_repacker(), payload, features);
        REQUIRE(result == R"({"yamp":{"i":"xt=test&xe=eventname"}})");
    }
}

TEST_CASE("repack/features/inject_collapse_id")
{
    repack_features features;
    features.inject_collapse_id = true;
    string payload = "{}";

    SECTION("bypass_repacker")
    {
        auto url_params = repack_url_params(bypass_repacker(), payload, features);
        REQUIRE(url_params == R"(&x-collapse-id=test)");
    }

    SECTION("custom_repacker")
    {
        auto url_params = repack_url_params(make_custom_repacker(), payload, features);
        REQUIRE(url_params == R"(&x-collapse-id=test)");
    }

    SECTION("apns_queue_repacker")
    {
        auto url_params = repack_url_params(apns_queue_repacker(), payload, features);
        REQUIRE(url_params == R"(&x-collapse-id=test)");
    }
}

TEST_CASE("repack/features/inject_collapse_id custom collapse-id")
{
    repack_features features;
    features.inject_collapse_id = true;
    string payload = "{}";
    string repack_rules = R"(
        {
            "collapse-id": "custom"
        }
    )";
    SECTION("custom_repacker custom collapse-id")
    {
        auto url_params = repack_url_params(make_custom_repacker(repack_rules), payload, features);
        REQUIRE(url_params == R"(&x-collapse-id=%22custom%22)");
    }

    SECTION("apns_queue_repacker")
    {
        auto message = make_message(payload);
        message.repacking_rules["apns"] = repack_rules;
        auto url_params = repack_url_params(apns_queue_repacker(), message, features);
        REQUIRE(url_params == R"(&x-collapse-id=%22custom%22)");
    }
}