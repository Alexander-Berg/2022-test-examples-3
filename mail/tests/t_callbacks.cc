#include <yxiva/core/callbacks.h>
#include <catch.hpp>

namespace yxiva { namespace callback_uri {

TEST_CASE("callback_uri/parse_uri/extract_app_name_and_push_token_from_mobile_uri", "")
{
    std::string app, token;
    auto result = parse_mobile_uri("xivamob:ru.yandex.push.app/p6u4s1h4t2o5k6e7n", app, token);
    REQUIRE(result);
    REQUIRE(app == "ru.yandex.push.app");
    REQUIRE(token == "p6u4s1h4t2o5k6e7n");
}

TEST_CASE("callback_uri/parse_uri/incorrect_mobile_uri_format", "")
{
    std::string app, token;
    CHECK(!parse_mobile_uri("xivamob:", app, token));
    CHECK(!parse_mobile_uri("xivamob:/", app, token));
    CHECK(!parse_mobile_uri("xivamob:ru.yandex.push.app/", app, token));
    CHECK(!parse_mobile_uri("xivamob:/p6u4s1h4t2o5k6e7n", app, token));
    CHECK(!parse_mobile_uri("xivamob:/:p6u4s1h4t2o5k6e7n", app, token));
    CHECK(!parse_mobile_uri("xivamob:/:p6u4s1h4t2o5k6e7n/", app, token));
    CHECK(!parse_mobile_uri(":app:token", app, token));
    CHECK(!parse_mobile_uri(":app/", app, token));
    CHECK(!parse_mobile_uri("/app:", app, token));
    CHECK(!parse_mobile_uri(":/token/", app, token));
    CHECK(!parse_mobile_uri("//token:", app, token));
    CHECK(!parse_mobile_uri("/", app, token));
    CHECK(!parse_mobile_uri("//", app, token));
    CHECK(!parse_mobile_uri(":/", app, token));
    CHECK(!parse_mobile_uri("/:", app, token));
    CHECK(!parse_mobile_uri("::", app, token));
    CHECK(!parse_mobile_uri("", app, token));
}

TEST_CASE("callback_uri/parse_uri/extract_data_from_webpush_uri", "")
{
    std::string data;
    auto result = parse_webpush_uri("webpush:p6u4s1h4t2o5k6e7n", data);
    REQUIRE(result);
    REQUIRE(data == "p6u4s1h4t2o5k6e7n");
}

TEST_CASE("callback_uri/parse_uri/incorrect_webpush_uri_format", "")
{
    std::string data;
    CHECK(!parse_webpush_uri("webpush:", data));
    CHECK(!parse_webpush_uri("webpush", data));
    CHECK(!parse_webpush_uri("webp", data));
    CHECK(!parse_webpush_uri("", data));
}

TEST_CASE("callback_uri/is_active_uri", "")
{
    CHECK(is_active_uri("http://inactive"));
    CHECK(!is_active_uri("inactive://host"));
}

TEST_CASE("callback_uri/inactive_uri", "")
{
    CHECK(inactive_uri("http://something") == "inactive://something");
    CHECK(inactive_uri("something") == "inactive:something");
}

TEST_CASE("callback_uri/websocket_uri", "")
{
    std::string url = "https://xiva-server.yandex.ru/notify?subid=1234wesfdgh";
    std::string ws_uri = SCHEME_XIVA_WEBSOCKET + url;
    CHECK(xiva_websocket_uri(url) == ws_uri);
    CHECK_FALSE(is_xiva_websocket_uri(url));
    CHECK(is_xiva_websocket_uri(ws_uri));
    CHECK_FALSE(parse_xiva_websocket_uri(url).first);
    CHECK(parse_xiva_websocket_uri(ws_uri) == std::pair(operation::success, url));
}

}}
