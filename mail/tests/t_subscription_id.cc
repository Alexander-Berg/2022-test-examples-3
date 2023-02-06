#include <yxiva/core/subscription_id.h>
#include <catch.hpp>

namespace yxiva {

sub_t test_subscription(int idx)
{
    sub_t sub;

    sub.uid = "uid" + std::to_string(idx);
    sub.service = "service" + std::to_string(idx);

    sub.id = "id" + std::to_string(idx);
    sub.filter = "filter" + std::to_string(idx);
    sub.callback_url = "callback_url" + std::to_string(idx);
    sub.extra_data = "extra_data" + std::to_string(idx);
    sub.client = "client" + std::to_string(idx);
    sub.session_key = "session_key" + std::to_string(idx);
    sub.ttl = 123 + idx;

    sub.init_local_id = 456 + idx;
    sub.init_time = 789 + idx;
    sub.ack_local_id = 987 + idx;
    sub.ack_time = 654 + idx;
    sub.smart_notify = idx % 2;
    sub.platform = "platform" + std::to_string(idx);
    sub.device = "device" + std::to_string(idx);
    sub.bb_connection_id = "bb_connection_id" + std::to_string(idx);
    sub.uidset = "uidset" + std::to_string(idx);
    sub.next_retry_time = 543 + idx;
    sub.retry_interval = 432 + idx;
    sub.ack_event_ts = 321 + idx;

    return sub;
}

TEST_CASE("subscription_id/make_mobile_subscription_id/simple", "")
{
    REQUIRE(make_mobile_subscription_id("DEVICE-123_uuid") == "mob:device123_uuid");
}

TEST_CASE("subscription_id/make_subscription_id/from_sub", "")
{
    auto sub = test_subscription(0);
    REQUIRE(make_subscription_id(sub) == "51259b75d70e2b22c914682ac53abf30e122c6e3");
}

TEST_CASE("subscription_id/make_subscription_id/from_args", "")
{
    auto sub = test_subscription(0);
    REQUIRE(
        make_subscription_id(
            sub.uid,
            sub.service,
            sub.callback_url,
            sub.client,
            sub.session_key,
            sub.filter,
            sub.extra_data) == "51259b75d70e2b22c914682ac53abf30e122c6e3");
}

TEST_CASE("subscription_id/make_subscription_id/depends_only_on", "")
{
    auto sub1 = test_subscription(1);
    auto sub2 = test_subscription(2);
    sub1.uid = sub2.uid = "same_uid";
    sub1.service = sub2.service = "same_service";
    sub1.callback_url = sub2.callback_url = "same_callback_url";
    sub1.extra_data = sub2.extra_data = "same_extra_data";
    sub1.client = sub2.client = "same_client";
    sub1.session_key = sub2.session_key = "same_session_key";
    sub1.filter = sub2.filter = "same_filter";
    REQUIRE(make_subscription_id(sub1) == make_subscription_id(sub2));
}

TEST_CASE("subscription_id/make_webpush_subscription_id/simple", "")
{
    REQUIRE(make_webpush_subscription_id("session-123-XYZ") == "webpush:session-123-XYZ");
}

}
