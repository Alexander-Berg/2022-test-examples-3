#include "helpers.h"
#include <processor/subscription_fetcher.h>
#include <yplatform/coroutine.h>
#include <catch.hpp>

using subscription_fetcher_t = subscription_fetcher<fake_http_client>;

static auto make_default_settings()
{
    settings s;
    s.environment = "a";
    s.list["a"].request = "/list";
    return s;
}

TEST_CASE("subscription_fetcher/rate_controller/errors")
{
    auto task = make_task();
    fake_http_client http;
    std::vector<rc_error> rc_errors{ rc_error::add_to_rc_queue_exception,
                                     rc_error::task_aborted,
                                     rc_error::capacity_exceeded };
    std::vector<error_code> expected =
        to_error_codes({ error::internal_error, error::task_cancelled, error::rate_limit });
    auto rate_controller = std::make_shared<fake_rate_controller>(rc_errors);
    auto s = make_default_settings();

    std::vector<error_code> errors;
    auto cb = [&errors](auto& ec) { errors.push_back(ec); };
    for ([[maybe_unused]] auto& err : expected)
    {
        yplatform::spawn(subscription_fetcher_t{
            task, make_rate_controllers(rate_controller), make_http_clients(http), s, cb });
    }
    REQUIRE(errors == expected);
}

TEST_CASE("subscription_fetcher/http/errors")
{
    auto task = make_task();
    fake_http_client http({ yhttp::errc::connect_error,
                            yhttp::errc::ssl_error,
                            yhttp::errc::read_error,
                            yhttp::errc::write_error,
                            yhttp::errc::server_response_error,
                            yhttp::errc::server_status_error,
                            yhttp::errc::server_header_error,
                            yhttp::errc::response_handler_error,
                            yhttp::errc::session_closed_error,
                            yhttp::errc::parse_response_error,
                            yhttp::errc::invalid_url,
                            yhttp::errc::unknown_error,
                            yhttp::errc::task_canceled,
                            yhttp::errc::connection_timeout,
                            yhttp::errc::request_timeout,
                            yhttp::errc::eof_error,
                            yhttp::errc::protocol_error,
                            yhttp::errc::unsupported_scheme,
                            yhttp::errc::no_service_ticket });
    std::vector<error_code> expected = to_error_codes({ error::network_error,
                                                        error::network_error,
                                                        error::network_error,
                                                        error::network_error,
                                                        error::network_error,
                                                        error::network_error,
                                                        error::network_error,
                                                        error::network_error,
                                                        error::network_error,
                                                        error::network_error,
                                                        error::internal_error,
                                                        error::network_error,
                                                        error::task_cancelled,
                                                        error::network_error,
                                                        error::task_cancelled,
                                                        error::network_error,
                                                        error::network_error,
                                                        error::internal_error,
                                                        error::internal_error });
    auto rate_controller = std::make_shared<fake_rate_controller>();
    auto s = make_default_settings();

    std::vector<error_code> errors;
    auto cb = [&errors](auto& ec) { errors.push_back(ec); };
    for ([[maybe_unused]] auto& err : expected)
    {
        yplatform::spawn(subscription_fetcher_t{
            task, make_rate_controllers(rate_controller), make_http_clients(http), s, cb });
    }
    REQUIRE(errors == expected);
}

TEST_CASE("subscription_fetcher/bad_response")
{
    auto task = make_task();
    fake_http_client http({
        yhttp::response{ 500, {}, "", "" },
        yhttp::response{ 400, {}, "", "" },
        yhttp::response{ 404, {}, "", "" },
        yhttp::response{ 429, {}, "", "" },
        yhttp::response{ 200, {}, "", "" },
        yhttp::response{ 200, {}, "not json", "" },
        yhttp::response{ 200, {}, "[}", "" },
        yhttp::response{ 200, {}, "{\"json\": \"not array\"}", "" },
    });
    std::vector<error_code> expected = to_error_codes({
        error::bad_gateway,
        error::remote_bad_request,
        error::remote_bad_request,
        error::rate_limit,
        error::bad_gateway,
        error::bad_gateway,
        error::bad_gateway,
        error::bad_gateway,
    });
    auto rate_controller = std::make_shared<fake_rate_controller>();
    auto s = make_default_settings();

    std::vector<error_code> errors;
    auto cb = [&errors](auto& ec) { errors.push_back(ec); };
    for ([[maybe_unused]] auto& err : expected)
    {
        yplatform::spawn(subscription_fetcher_t{
            task, make_rate_controllers(rate_controller), make_http_clients(http), s, cb });
    }
    REQUIRE(errors == expected);
}

TEST_CASE("subscription_fetcher/ok")
{
    auto task = make_task();
    task->uid = "123";
    fake_http_client http({
        yhttp::response{ 200, {}, "[]", "" },
        yhttp::response{ 200,
                         {},
                         R"([
            {
                "client" : "test_client",
                "filter" : "",
                "id" : "870a4d3070ff6319119946ef3e25049953fc2c87",
                "session" : "session_abcde",
                "ttl" : 8,
                "url" : "http://sample/callback",
                "extra" : "extra1",
                "init_time" : 12345,
                "last_sent" : 12346,
                "pos" : 1
            },
            {
                "client" : "mobile",
                "device" : "test_device",
                "extra" : "extra2",
                "filter" : "some_filter",
                "id" : "mob:870a4d3070ff6319119946ef3e25049953fc2c88",
                "platform" : "fcm",
                "session" : "test_session",
                "ttl" : 31536000,
                "uuid" : "test_uuid",
                "app" : "ru.yandex.mail",
                "init_time" : 123,
                "last_sent" : 124,
                "pos" : 2
            }
        ])",
                         "" },

    });
    auto rate_controller = std::make_shared<fake_rate_controller>();
    auto s = make_default_settings();

    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(subscription_fetcher_t{
        task, make_rate_controllers(rate_controller), make_http_clients(http), s, cb });
    REQUIRE(error == make_error(error::success));
    REQUIRE(task->subscriptions.empty());
    yplatform::spawn(subscription_fetcher_t{
        task, make_rate_controllers(rate_controller), make_http_clients(http), s, cb });
    REQUIRE(error == make_error(error::success));

    REQUIRE(task->subscriptions.size() == 2);
    auto http_sub = task->subscriptions[0].http();
    REQUIRE(http_sub);
    REQUIRE(http_sub->id == "870a4d3070ff6319119946ef3e25049953fc2c87");
    REQUIRE(http_sub->client == "test_client");
    REQUIRE(http_sub->session == "session_abcde");
    REQUIRE(http_sub->filter == "");
    REQUIRE(http_sub->ttl == 8);
    REQUIRE(http_sub->extra == "extra1");
    REQUIRE(http_sub->init_time == 12345);
    REQUIRE(http_sub->last_sent == 12346);
    REQUIRE(http_sub->pos == 1);
    REQUIRE(http_sub->url == "http://sample/callback");

    auto mobile_sub = task->subscriptions[1].mobile();
    REQUIRE(mobile_sub);
    REQUIRE(mobile_sub->id == "mob:870a4d3070ff6319119946ef3e25049953fc2c88");
    REQUIRE(mobile_sub->client == "mobile");
    REQUIRE(mobile_sub->session == "test_session");
    REQUIRE(mobile_sub->filter == "some_filter");
    REQUIRE(mobile_sub->ttl == 31536000);
    REQUIRE(mobile_sub->extra == "extra2");
    REQUIRE(mobile_sub->init_time == 123);
    REQUIRE(mobile_sub->last_sent == 124);
    REQUIRE(mobile_sub->pos == 2);
    REQUIRE(mobile_sub->app == "ru.yandex.mail");
    REQUIRE(mobile_sub->uuid == "test_uuid");
    REQUIRE(mobile_sub->platform == "fcm");
    REQUIRE(mobile_sub->device == "test_device");

    REQUIRE(http.requests().size() == 2);
    for (auto& req : http.requests())
    {
        REQUIRE(req.method == yhttp::request::method_t::GET);
        REQUIRE(req.url == "/list?service=mail&user=123");
    }
}

TEST_CASE("subscription_fetcher/gcm_becomes_fcm")
{
    auto task = make_task();
    task->uid = "123";
    fake_http_client http({
        yhttp::response{ 200,
                         {},
                         R"([
            {
                "client" : "mobile",
                "device" : "test_device",
                "extra" : "extra2",
                "filter" : "some_filter",
                "id" : "mob:870a4d3070ff6319119946ef3e25049953fc2c88",
                "platform" : "gcm",
                "session" : "test_session",
                "ttl" : 31536000,
                "uuid" : "test_uuid",
                "app" : "ru.yandex.mail",
                "init_time" : 123,
                "last_sent" : 124,
                "pos" : 2
            }
        ])",
                         "" },

    });
    auto rate_controller = std::make_shared<fake_rate_controller>();
    auto s = make_default_settings();

    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(subscription_fetcher_t{
        task, make_rate_controllers(rate_controller), make_http_clients(http), s, cb });
    REQUIRE(error == make_error(error::success));

    REQUIRE(task->subscriptions.size() == 1);
    auto mobile_sub = task->subscriptions[0].mobile();
    REQUIRE(mobile_sub);
    REQUIRE(mobile_sub->platform == "fcm");
}

TEST_CASE("subscription_fetcher/parses devices from subscription extra")
{
    static const string SUB_WITH_ACCOUNTS = R"([
        {
            "client" : "mobile",
            "device" : "test_device",
            "extra" : "{\"accounts\": [{ \"uid\": \"A\", \"environment\": \"a\", \"badge\": { \"enabled\": true } },{ \"uid\": \"B\", \"environment\": \"b\", \"badge\": { \"enabled\": true } }]}",
            "filter" : "some_filter",
            "id" : "mob:id",
            "platform" : "apns",
            "session" : "test_session",
            "ttl" : 31536000,
            "uuid" : "test_uuid",
            "app" : "ru.yandex.mail",
            "init_time" : 123,
            "last_sent" : 124,
            "pos" : 2
        }
    ])";
    auto task = make_task();
    task->uid = "123";
    fake_http_client http({
        yhttp::response{ 200, {}, SUB_WITH_ACCOUNTS, "" },
        yhttp::response{ 200, {}, SUB_WITH_ACCOUNTS, "" },
        yhttp::response{ 200, {}, SUB_WITH_ACCOUNTS, "" },
    });
    auto rate_controller = std::make_shared<fake_rate_controller>();
    rate_controllers<ymod_ratecontroller::rate_controller_ptr> rate_controllers;
    rate_controllers.emplace("a", rate_controller);
    rate_controllers.emplace("b", rate_controller);
    http_clients<fake_http_client> http_clients;
    http_clients.emplace("a", http);
    http_clients.emplace("b", http);
    auto s = make_default_settings();
    s.list["b"] = s.list["a"];

    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(subscription_fetcher_t{ task, rate_controllers, http_clients, s, cb });
    REQUIRE(error == make_error(error::success));

    REQUIRE(task->devices.count("mob:id") == 1);
    auto& device = task->devices.at("mob:id");
    REQUIRE(device.accounts.size() == 2);
    auto& account_a = device.accounts[0];
    REQUIRE(account_a.uid == "A");
    REQUIRE(account_a.environment == "a");
    REQUIRE(account_a.badge_enabled);
    auto& account_b = device.accounts[1];
    REQUIRE(account_b.uid == "B");
    REQUIRE(account_b.environment == "b");
    REQUIRE(account_b.badge_enabled);
}