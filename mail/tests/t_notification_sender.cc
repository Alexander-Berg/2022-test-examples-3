#include "helpers.h"
#include <processor/notification_sender.h>
#include <yplatform/coroutine.h>
#include <catch.hpp>
#include <boost/algorithm/string/predicate.hpp>

struct fake_notification_factory
{
    std::vector<notification> notifications;
    auto make(const shared_ptr<task>&, const event&, const settings&) const
    {
        return notifications;
    }
};

using notification_sender_t = notification_sender<fake_http_client, fake_notification_factory>;
static const auto direct = notification::delivery_mode::direct;
static const auto queued = notification::delivery_mode::queued;

struct notification_sender_fixture
{
    shared_ptr<task> task;
    fake_http_client http;
    std::shared_ptr<fake_rate_controller> rc;
    struct settings settings;
    error_code sender_ec;
    std::function<void(const error_code&)> cb = [&](auto& ec) { sender_ec = ec; };
    fake_notification_factory fake_factory;

    notification_sender_fixture()
    {
        rc = std::make_shared<fake_rate_controller>();
        fake_factory.notifications.emplace_back(notification{ {}, {}, {}, {}, {}, {} });
        http = fake_http_client({ yhttp::response{ 200, {}, "", "" } });
        task = make_task();
        task->events.push_back(make_event(action::RESET_FRESH, "[]"));
    }

    void spawn_sender()
    {
        yplatform::spawn(notification_sender_t{ task, rc, http, settings, cb, fake_factory });
    }

    auto emulate_rc_error(rc_error e)
    {
        rc->results.push_back(e);
        spawn_sender();
        return sender_ec;
    }

    auto emulate_http_error(yhttp::errc e, int status = 500)
    {
        http = e ? fake_http_client{ std::vector<yhttp::errc>{ e } } :
                   fake_http_client{ std::vector<yhttp::response>{ { status, {}, {}, {} } } };
        spawn_sender();
        if (http.requests().size() != 1)
        {
            throw std::runtime_error(
                "unexpected number of requests: " + std::to_string(http.requests().size()));
        }
        return sender_ec;
    }

    auto emulate_http_status(int status)
    {
        return emulate_http_error({}, status);
    }

    auto& event()
    {
        return task->events.back();
    }
};

TEST_CASE_METHOD(
    notification_sender_fixture,
    "notification_sender/returns specific errors after rate controller failure",
    "")
{
    REQUIRE(
        emulate_rc_error(rc_error::add_to_rc_queue_exception) == make_error(error::internal_error));
    REQUIRE(emulate_rc_error(rc_error::task_aborted) == make_error(error::task_cancelled));
    REQUIRE(emulate_rc_error(rc_error::capacity_exceeded) == make_error(error::rate_limit));
}

TEST_CASE_METHOD(
    notification_sender_fixture,
    "notification_sender/returns specific errors after http client failiure",
    "")
{
    using namespace yhttp;
    REQUIRE(emulate_http_error(errc::connect_error) == make_error(error::network_error));
    REQUIRE(emulate_http_error(errc::ssl_error) == make_error(error::network_error));
    REQUIRE(emulate_http_error(errc::read_error) == make_error(error::network_error));
    REQUIRE(emulate_http_error(errc::write_error) == make_error(error::network_error));
    REQUIRE(emulate_http_error(errc::server_response_error) == make_error(error::network_error));
    REQUIRE(emulate_http_error(errc::server_status_error) == make_error(error::network_error));
    REQUIRE(emulate_http_error(errc::server_header_error) == make_error(error::network_error));
    REQUIRE(emulate_http_error(errc::response_handler_error) == make_error(error::network_error));
    REQUIRE(emulate_http_error(errc::session_closed_error) == make_error(error::network_error));
    REQUIRE(emulate_http_error(errc::parse_response_error) == make_error(error::network_error));
    REQUIRE(emulate_http_error(errc::invalid_url) == make_error(error::internal_error));
    REQUIRE(emulate_http_error(errc::unknown_error) == make_error(error::network_error));
    REQUIRE(emulate_http_error(errc::task_canceled) == make_error(error::task_cancelled));
    REQUIRE(emulate_http_error(errc::connection_timeout) == make_error(error::network_error));
    REQUIRE(emulate_http_error(errc::request_timeout) == make_error(error::task_cancelled));
    REQUIRE(emulate_http_error(errc::eof_error) == make_error(error::network_error));
    REQUIRE(emulate_http_error(errc::protocol_error) == make_error(error::network_error));
    REQUIRE(emulate_http_error(errc::unsupported_scheme) == make_error(error::internal_error));
    REQUIRE(emulate_http_error(errc::no_service_ticket) == make_error(error::internal_error));
}

TEST_CASE_METHOD(
    notification_sender_fixture,
    "notification_sender/returns specific errors after specific http responses",
    "")
{
    REQUIRE(emulate_http_status(500) == make_error(error::bad_gateway));
    REQUIRE(emulate_http_status(400) == make_error(error::remote_bad_request));
    REQUIRE(emulate_http_status(404) == make_error(error::remote_bad_request));
    REQUIRE(emulate_http_status(429) == make_error(error::rate_limit));
}

TEST_CASE_METHOD(
    notification_sender_fixture,
    "notification_sender/fails if second message fails",
    "")
{
    fake_factory.notifications =
        std::vector<notification>{ { "1", {}, {}, {}, {}, {} }, { "2", {}, {}, {}, {}, {} } };
    http = fake_http_client(
        { yhttp::response{ 200, {}, "", "" }, yhttp::response{ 500, {}, "", "" } });
    spawn_sender();
    REQUIRE(sender_ec == make_error(error::bad_gateway));
    REQUIRE(http.requests().size() == 2);
    REQUIRE(event().sent_notification_ids.size() == 1);
    REQUIRE(event().sent_notification_ids[0] == "1");
}

TEST_CASE_METHOD(
    notification_sender_fixture,
    "notification_sender/does not send notifications sent on previous try",
    "")
{
    fake_factory.notifications =
        std::vector<notification>{ { "1", {}, {}, {}, {}, {} }, { "2", {}, {}, {}, {}, {} } };
    event().sent_notification_ids = { "1" };
    http = fake_http_client({ yhttp::response{ 200, {}, "", "" } });
    spawn_sender();
    REQUIRE(sender_ec == make_error(error::success));
    REQUIRE(http.requests().size() == 1);
    REQUIRE(http.requests()[0].url.find("&request_id=ctx_id_2") != string::npos);
    REQUIRE(event().sent_notification_ids.size() == 2);
    REQUIRE(event().sent_notification_ids[0] == "1");
    REQUIRE(event().sent_notification_ids[1] == "2");
}

static const string BODY = R"(
    {
        "operation": "status change",
        "sessionKey": "xxx",
        "foo": "bar"
    }
)";
static const string KEYS = R"(
    {
        "fee": "fum"
    }
)";

TEST_CASE_METHOD(notification_sender_fixture, "notification_sender/produces valid xiva request")
{
    settings.send.request = "/send/request";
    settings.send.ttl = 777;
    task->uid = "123";
    task->events = std::vector<struct event>{ make_event(action::CHANGE_STATUS, "[]") };
    fake_factory.notifications = std::vector<notification>{
        { "very_important_step", {}, queued, {}, json_parse(BODY), json_parse(KEYS) }
    };
    spawn_sender();
    REQUIRE(sender_ec == make_error(error::success));
    REQUIRE(http.requests().size() == 1);

    SECTION("url")
    {
        using namespace boost::algorithm;
        auto& url = http.requests()[0].url;
        CHECK(
            url ==
            "/send/"
            "request?user=123&service=mail&event=status+change&request_id=ctx_id_very_important_"
            "step&source="
            "mailpusher&session=xxx&ttl=777");
        REQUIRE(starts_with(url, settings.send.request));
        REQUIRE(url.find("user=123") != string::npos);
        REQUIRE(url.find("service=mail") != string::npos);
        REQUIRE(url.find("event=status+change") != string::npos);
        REQUIRE(url.find("request_id=ctx_id_very_important_step") != string::npos);
        REQUIRE(url.find("source=mailpusher") != string::npos);
        REQUIRE(url.find("session=xxx") != string::npos);
        REQUIRE(url.find("ttl=777") != string::npos);
    }

    SECTION("method")
    {
        REQUIRE(http.requests()[0].method == yhttp::request::method_t::POST);
    }

    SECTION("body")
    {
        auto json_body = json_parse(*http.requests()[0].body);
        REQUIRE(json_body["payload"] == json_parse(BODY));
        REQUIRE(json_body["keys"] == json_parse(KEYS));
    }
}

TEST_CASE_METHOD(
    notification_sender_fixture,
    "notification_sender/adds subscription filters from destination")
{
    const string SUBSCRIPTIONS = R"(
        [
            { "transport": ["mobile", "webpush"] },
            { "platform": ["apns", "wns"] },
            { "subscription_id": ["1", "2", "3"] }
        ]
    )";
    const auto DESTINATION =
        destination{ { "mobile", "webpush" }, { "apns", "wns" }, { "1", "2", "3" } };
    fake_factory.notifications = std::vector<notification>{
        { "step", DESTINATION, {}, {}, json_parse(BODY), json_parse(KEYS) }
    };
    spawn_sender();
    REQUIRE(sender_ec == make_error(error::success));
    REQUIRE(http.requests().size() == 1);
    auto json_body = json_parse(*http.requests()[0].body);
    REQUIRE(json_body["subscriptions"] == json_parse(SUBSCRIPTIONS, json_type::tarray));
}

TEST_CASE_METHOD(
    notification_sender_fixture,
    "notification_sender/adds delivery mode header for direct")
{
    fake_factory.notifications = std::vector<notification>{
        { {}, DESTINATION_MOBILE, direct, {}, {}, {} },
    };
    spawn_sender();
    auto& headers = std::get<std::string>(http.requests()[0].headers);
    REQUIRE(sender_ec == make_error(error::success));
    REQUIRE(http.requests().size() == 1);
    REQUIRE(headers == "X-DeliveryMode: direct\r\n");
}

TEST_CASE_METHOD(
    notification_sender_fixture,
    "notification_sender/sends ttl=0 for silent notifications")
{
    settings.send.ttl = 100500;
    fake_factory.notifications.back().silent = true;
    spawn_sender();
    REQUIRE(sender_ec == make_error(error::success));
    REQUIRE(http.requests().size() == 1);
    auto& url = http.requests()[0].url;
    REQUIRE(url.find("ttl=0") != string::npos);
}

TEST_CASE_METHOD(notification_sender_fixture, "notification_sender/adds repack if it exists")
{
    const string REPACK = R"({
        "fcm": { "repack_payload": ["*", {"my_transit_id": "::xiva::transit_id"}] },
        "hms": { "repack_payload": ["*", {"my_transit_id": "::xiva::transit_id"}] }
    })";
    fake_factory.notifications = std::vector<notification>{
        { {}, DESTINATION_ANDROID, direct, {}, {}, {}, json_parse(REPACK) }
    };
    spawn_sender();
    REQUIRE(sender_ec == make_error(error::success));
    REQUIRE(http.requests().size() == 1);
    auto json_body = json_parse(*http.requests()[0].body);
    REQUIRE(json_body["repack"] == json_parse(REPACK));
}

TEST_CASE_METHOD(notification_sender_fixture, "notification_sender/ignored_folders")
{
    settings.send.ignored_folder_codes = std::set{ 3, 4, 11 };
    struct event event;
    event.action_type = action::NEW_MAIL;
    event.items.push_back(json_value{});
    auto&& folder = event.items.back()["folder"];

    SECTION("spam")
    {
        folder["symbolicName"]["code"] = 4;
        REQUIRE(!notification_sender_t::is_appropriate_insert_folder(event, settings));
    }

    SECTION("trash")
    {
        folder["symbolicName"]["code"] = 3;
        REQUIRE(!notification_sender_t::is_appropriate_insert_folder(event, settings));
    }

    SECTION("pending")
    {
        folder["symbolicName"]["code"] = 11;
        REQUIRE(!notification_sender_t::is_appropriate_insert_folder(event, settings));
    }

    SECTION("inbox")
    {
        folder["symbolicName"]["code"] = 1;
        REQUIRE(notification_sender_t::is_appropriate_insert_folder(event, settings));
    }
}
