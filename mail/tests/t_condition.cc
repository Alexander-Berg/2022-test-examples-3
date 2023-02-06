#include <yxiva/core/filter/filter_set.h>
#include <yxiva/core/message.h>
#include <yxiva/core/packing.hpp>
#include <catch.hpp>

using namespace yxiva;

TEST_CASE("condition/matches/message/eq", "")
{
    using cond_t = filter::message_condition_type;
    message m;
    filter::condition cond = { cond_t::data_field_equals, "f", { "v1", "v2" } };
    REQUIRE(!cond.matches(m));
    m.data["f"] = "v3";
    REQUIRE(!cond.matches(m));
    m.data["f"] = "v1";
    REQUIRE(cond.matches(m));
    m.data["f"] = "v2";
    REQUIRE(cond.matches(m));
}

TEST_CASE("condition/matches/message/has_tags", "")
{
    using cond_t = filter::message_condition_type;
    message m;
    filter::condition cond = { cond_t::has_tags, "", { "v1", "v2" } };
    REQUIRE(!cond.matches(m));
    m.tags = { "v3" };
    REQUIRE(!cond.matches(m));
    m.tags = { "v1", "v3" };
    REQUIRE(!cond.matches(m));
    m.tags = { "v1", "v2" };
    REQUIRE(cond.matches(m));
    m.tags = { "v1", "v2", "v3" };
    REQUIRE(cond.matches(m));
}

TEST_CASE("condition/matches/message/has", "")
{
    using cond_t = filter::message_condition_type;
    message m;
    filter::condition cond = { cond_t::has_least_one, "f", { "v1", "v2" } };
    REQUIRE(!cond.matches(m));
    m.data["f"] = pack(std::vector<string>{ "v3" });
    REQUIRE(!cond.matches(m));
    m.data["f"] = pack(std::vector<string>{ "v3", "v4", "v4" });
    REQUIRE(!cond.matches(m));
    m.data["f"] = "something-that-is-not-msgpack";
    REQUIRE(!cond.matches(m));
    m.data["f"] = pack(std::vector<string>{ "v1", "v3" });
    REQUIRE(cond.matches(m));
    m.data["f"] = pack(std::vector<string>{ "v1", "v2" });
    REQUIRE(cond.matches(m));
    m.data["f"] = pack(std::vector<string>{ "v1", "v2", "v3" });
    REQUIRE(cond.matches(m));
}

TEST_CASE("condition/matches/message/event", "")
{
    using cond_t = filter::message_condition_type;
    message m;
    filter::condition cond = { cond_t::event, "", { "v1", "v2" } };
    REQUIRE(!cond.matches(m));
    m.operation = "v3";
    REQUIRE(!cond.matches(m));
    m.operation = "v1";
    REQUIRE(cond.matches(m));
    m.operation = "v2";
    REQUIRE(cond.matches(m));
}

TEST_CASE("condition/matches/subscription/platform", "")
{
    using cond_t = filter::subscription_condition_type;
    sub_t s;
    filter::subscription_condition cond = { cond_t::platform,
                                            "",
                                            { "apns", "gcm" } }; // gcm_compatibility
    REQUIRE_FALSE(cond.matches(s));
    s.platform = "apns";
    REQUIRE(cond.matches(s));
    s.platform = "gcm"; // gcm_compatibility
    REQUIRE(cond.matches(s));
    s.platform = "fcm";
    REQUIRE(cond.matches(s));
    s.platform = "wns";
    REQUIRE_FALSE(cond.matches(s));
    s.platform = "mpns";
    REQUIRE_FALSE(cond.matches(s));
    s.platform = "hms";
    REQUIRE_FALSE(cond.matches(s));
}

TEST_CASE("condition/matches/subscription/platform_resolved", "")
{
    using cond_t = filter::subscription_condition_type;
    sub_t s;
    filter::subscription_condition cond = { cond_t::platform, "", { "fcm" } };
    s.platform = "gcm"; // gcm_compatibility
    REQUIRE(cond.matches(s));
    s.platform = "fcm";
    REQUIRE(cond.matches(s));
}

TEST_CASE("condition/matches/subscription/subscription_id", "")
{
    using cond_t = filter::subscription_condition_type;
    sub_t s;
    filter::subscription_condition cond = { cond_t::subscription_id, "", { "123", "abc" } };
    REQUIRE_FALSE(cond.matches(s));
    s.id = "qwe";
    REQUIRE_FALSE(cond.matches(s));
    s.id = "1234";
    REQUIRE_FALSE(cond.matches(s));
    s.id = "123";
    REQUIRE(cond.matches(s));
    s.id = "abc";
    REQUIRE(cond.matches(s));
}

TEST_CASE("condition/matches/subscription/device", "")
{
    using cond_t = filter::subscription_condition_type;
    sub_t s;
    filter::subscription_condition cond = { cond_t::device, "", { "123", "abc" } };
    REQUIRE_FALSE(cond.matches(s));
    s.device = "qwe";
    REQUIRE_FALSE(cond.matches(s));
    s.device = "1234";
    REQUIRE_FALSE(cond.matches(s));
    s.device = "123";
    REQUIRE(cond.matches(s));
    s.device = "abc";
    REQUIRE(cond.matches(s));
}

TEST_CASE("condition/matches/subscription/session", "")
{
    using cond_t = filter::subscription_condition_type;
    sub_t s;
    filter::subscription_condition cond = { cond_t::session, "", { "123", "abc" } };
    REQUIRE_FALSE(cond.matches(s));
    s.session_key = "qwe";
    REQUIRE_FALSE(cond.matches(s));
    s.session_key = "1234";
    REQUIRE_FALSE(cond.matches(s));
    s.session_key = "123";
    REQUIRE(cond.matches(s));
    s.session_key = "abc";
    REQUIRE(cond.matches(s));
}

TEST_CASE("condition/matches/subscription/transport", "")
{
    using cond_t = filter::subscription_condition_type;
    sub_t s;
    filter::subscription_condition cond_http = { cond_t::transport, "", { "http" } };
    filter::subscription_condition cond_mobile = { cond_t::transport, "", { "mobile" } };
    filter::subscription_condition cond_webpush = { cond_t::transport, "", { "webpush" } };
    filter::subscription_condition cond_websocket = { cond_t::transport, "", { "websocket" } };
    filter::subscription_condition cond_http_or_webpush = { cond_t::transport,
                                                            "",
                                                            { "http", "webpush" } };
    filter::subscription_condition cond_wrong_transport = { cond_t::transport, "", { "fake" } };

    s.callback_url = "http://test";
    REQUIRE(cond_http.matches(s));
    REQUIRE_FALSE(cond_mobile.matches(s));
    REQUIRE_FALSE(cond_webpush.matches(s));
    REQUIRE_FALSE(cond_websocket.matches(s));
    REQUIRE(cond_http_or_webpush.matches(s));
    REQUIRE_FALSE(cond_wrong_transport.matches(s));
    s.callback_url = "xivamob:test";
    REQUIRE_FALSE(cond_http.matches(s));
    REQUIRE(cond_mobile.matches(s));
    REQUIRE_FALSE(cond_webpush.matches(s));
    REQUIRE_FALSE(cond_websocket.matches(s));
    REQUIRE_FALSE(cond_http_or_webpush.matches(s));
    REQUIRE_FALSE(cond_wrong_transport.matches(s));
    s.callback_url = "webpush:test";
    REQUIRE_FALSE(cond_http.matches(s));
    REQUIRE_FALSE(cond_mobile.matches(s));
    REQUIRE_FALSE(cond_websocket.matches(s));
    REQUIRE(cond_webpush.matches(s));
    REQUIRE(cond_http_or_webpush.matches(s));
    REQUIRE_FALSE(cond_wrong_transport.matches(s));
    s.callback_url = "xivaws:test";
    REQUIRE(cond_http.matches(s));
    REQUIRE_FALSE(cond_mobile.matches(s));
    REQUIRE(cond_websocket.matches(s));
    REQUIRE_FALSE(cond_webpush.matches(s));
    REQUIRE(cond_http_or_webpush.matches(s));
    REQUIRE_FALSE(cond_wrong_transport.matches(s));
}

TEST_CASE("condition/matches/subscription/uuid", "")
{
    using cond_t = filter::subscription_condition_type;
    sub_t s;
    filter::subscription_condition cond1 = { cond_t::uuid, "", { "X-y-Z" } };
    filter::subscription_condition cond2 = { cond_t::uuid, "", { "xyz" } };

    s.session_key = "123";
    REQUIRE_FALSE(cond1.matches(s));
    REQUIRE_FALSE(cond2.matches(s));
    s.session_key = "xyz";
    REQUIRE(cond1.matches(s));
    REQUIRE(cond2.matches(s));
    s.session_key = "X-y-Z";
    REQUIRE_FALSE(cond1.matches(s));
    REQUIRE_FALSE(cond2.matches(s));
}

TEST_CASE("condition/matches/subscription/app", "")
{
    using cond_t = filter::subscription_condition_type;
    sub_t s;
    filter::subscription_condition cond1 = { cond_t::app, "", { "ru.test.ya" } };
    filter::subscription_condition cond2 = { cond_t::app, "", { "abcdefg" } };

    s.callback_url = "http://test";
    REQUIRE_FALSE(cond1.matches(s));
    REQUIRE_FALSE(cond2.matches(s));
    s.callback_url = "webpush:test";
    REQUIRE_FALSE(cond1.matches(s));
    REQUIRE_FALSE(cond2.matches(s));
    s.callback_url = "xivaws:test";
    REQUIRE_FALSE(cond1.matches(s));
    REQUIRE_FALSE(cond2.matches(s));
    s.callback_url = "xivamob:app.othrer.com/AFGDFVDSFGDSF";
    REQUIRE_FALSE(cond1.matches(s));
    REQUIRE_FALSE(cond2.matches(s));
    s.callback_url = "xivamob:ru.test.ya/GNGFBSDF";
    REQUIRE(cond1.matches(s));
    REQUIRE_FALSE(cond2.matches(s));
    s.callback_url = "xivamob:abcdefg/kjhkjhk";
    REQUIRE_FALSE(cond1.matches(s));
    REQUIRE(cond2.matches(s));
    s.callback_url = "xivamob:ru.test.ya.abcdefg/3452345435345";
    REQUIRE_FALSE(cond1.matches(s));
    REQUIRE_FALSE(cond2.matches(s));
    s.callback_url = "xivamob:abcdefg/";
    REQUIRE_FALSE(cond2.matches(s));
    s.callback_url = "xivamob:abcdefg";
    REQUIRE_FALSE(cond2.matches(s));
    s.callback_url = "webpush:abcdefg/kjhkjhk";
    REQUIRE_FALSE(cond2.matches(s));
}
