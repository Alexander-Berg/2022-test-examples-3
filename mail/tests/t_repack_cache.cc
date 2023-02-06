#include <yxiva/core/message.h>
#include <yxiva/core/repacker.h>
#include <catch.hpp>

using namespace yxiva;

struct repack_cache_test
{
    static const string x_param;
    message msg;
    sub_t sub;
    push_subscription_params sub_mob;
    push_request_cache cache;

    repack_cache_test()
    {
        msg.bright = true;
        msg.uid = "123";
        msg.service = "qwerty";
        msg.repacking_rules["apns"] =
            R"({"aps":{"alert": "New resource found","content-available": 0}})";

        sub.platform = "apns";

        sub_mob = push_subscription_params(sub);
        sub_mob.app_name = "test_app";
    }
};

const string repack_cache_test::x_param = "&x-aps=%7b%22alert%22%3a%22New+resource+"
                                          "found%22%2c%22content-available%22%3a0%7d";

TEST_CASE_METHOD(repack_cache_test, "repack_cache/repacking_not_broken", "")
{
    push_requests_queue messages;
    auto repacked = repack_message_if_needed(packet(nullptr, msg, sub), sub_mob, messages, cache);
    REQUIRE(repacked.error_reason == "");
    auto m = messages.pop_front();
    CHECK(m.payload() == "");
    CHECK(m.http_url_params() == x_param);
}

TEST_CASE_METHOD(repack_cache_test, "repack_cache/result_is_cached", "")
{
    push_requests_queue messages;
    auto repacked = repack_message_if_needed(packet(nullptr, msg, sub), sub_mob, messages, cache);
    REQUIRE(repacked.error_reason == "");
    auto m = messages.pop_front();

    msg.repacking_rules.clear();
    push_requests_queue messages2;
    auto repacked2 = repack_message_if_needed(packet(nullptr, msg, sub), sub_mob, messages2, cache);
    REQUIRE(repacked2.error_reason == "");
    auto m2 = messages2.pop_front();
    CHECK(m2.payload() == m.payload());
    CHECK(m2.http_url_params() == m.http_url_params());
}

TEST_CASE_METHOD(repack_cache_test, "repack_cache/different_result_for_different_bright", "")
{
    push_requests_queue messages;
    auto repacked = repack_message_if_needed(packet(nullptr, msg, sub), sub_mob, messages, cache);
    REQUIRE(repacked.error_reason == "");
    auto m = messages.pop_front();

    msg.repacking_rules.clear();
    msg.bright = false;
    push_requests_queue messages2;
    auto repacked2 = repack_message_if_needed(packet(nullptr, msg, sub), sub_mob, messages2, cache);
    REQUIRE(repacked2.error_reason == "");
    auto m2 = messages2.pop_front();
    CHECK(m2.http_url_params() != m.http_url_params());
}

TEST_CASE_METHOD(repack_cache_test, "repack_cache/different_result_for_different_app", "")
{
    push_requests_queue messages;
    auto repacked = repack_message_if_needed(packet(nullptr, msg, sub), sub_mob, messages, cache);
    REQUIRE(repacked.error_reason == "");
    auto m = messages.pop_front();

    msg.repacking_rules.clear();
    auto s2 = sub_mob;
    s2.app_name += "2";
    push_requests_queue messages2;
    auto repacked2 = repack_message_if_needed(packet(nullptr, msg, sub), s2, messages2, cache);
    REQUIRE(repacked2.error_reason == "");
    auto m2 = messages2.pop_front();
    CHECK(m2.http_url_params() != m.http_url_params());
}

TEST_CASE_METHOD(repack_cache_test, "repack_cache/error_is_stored", "")
{
    msg.repacking_rules["apns"][0] = ' ';
    push_requests_queue messages;
    auto repacked = repack_message_if_needed(packet(nullptr, msg, sub), sub_mob, messages, cache);
    CHECK(repacked.error_reason != "");

    msg.repacking_rules.clear();
    auto repacked2 = repack_message_if_needed(packet(nullptr, msg, sub), sub_mob, messages, cache);
    CHECK(repacked2.error_reason == repacked.error_reason);
}
