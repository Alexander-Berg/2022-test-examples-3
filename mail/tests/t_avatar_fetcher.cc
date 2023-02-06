#include "helpers.h"
#include <processor/avatar_fetcher.h>
#include <yplatform/coroutine.h>
#include <catch.hpp>

using avatar_fetcher_t = avatar_fetcher<fake_http_client>;

TEST_CASE("avatar_fetcher/rate_controller/errors")
{
    auto task = make_task();
    fake_http_client http;
    std::vector<rc_error> rc_errors{ rc_error::add_to_rc_queue_exception,
                                     rc_error::task_aborted,
                                     rc_error::capacity_exceeded };
    std::vector<error_code> expected =
        to_error_codes({ error::internal_error, error::task_cancelled, error::rate_limit });
    auto rate_controller = std::make_shared<fake_rate_controller>(rc_errors);
    settings s;

    std::vector<error_code> errors;
    auto cb = [&errors](auto& ec) { errors.push_back(ec); };
    for ([[maybe_unused]] auto& err : expected)
    {
        yplatform::spawn(avatar_fetcher_t{ task, rate_controller, http, s, cb });
    }
    REQUIRE(errors == expected);
}

TEST_CASE("avatar_fetcher/http/errors")
{
    auto task = make_task();
    task->events.resize(1);
    task->events.back().action_type = action::NEW_MAIL;
    task->events.back().items.push_back(
        json_parse(R"({"from":[{"local":"user","domain":"domain"}]})"));
    task->events.back().metadata_count = 1;

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
    settings s;
    s.ava.max_emails_in_profiles_request = 1;

    std::vector<error_code> errors;
    auto cb = [&errors](auto& ec) { errors.push_back(ec); };
    for ([[maybe_unused]] auto& err : expected)
    {
        task->events.back().avatar_fetched = fetch_status::NONE;
        yplatform::spawn(avatar_fetcher_t{ task, rate_controller, http, s, cb });
    }
    REQUIRE(errors == expected);
}

TEST_CASE("avatar_fetcher/bad_response")
{
    auto task = make_task();
    task->events.resize(1);
    task->events.back().action_type = action::NEW_MAIL;
    task->events.back().items.push_back(
        json_parse(R"({"from":[{"local":"user","domain":"domain"}]})"));
    task->events.back().metadata_count = 1;

    fake_http_client http({ yhttp::response{ 500, {}, "", "" },
                            yhttp::response{ 400, {}, "", "" },
                            yhttp::response{ 404, {}, "", "" },
                            yhttp::response{ 429, {}, "", "" },
                            yhttp::response{ 200, {}, "", "" },
                            yhttp::response{ 200, {}, "not json", "" },
                            yhttp::response{ 200, {}, "[}", "" },
                            yhttp::response{ 200, {}, "[\"not an object\"]", "" } });
    std::vector<error_code> expected = to_error_codes({ error::bad_gateway,
                                                        error::remote_bad_request,
                                                        error::remote_bad_request,
                                                        error::rate_limit,
                                                        error::bad_gateway,
                                                        error::bad_gateway,
                                                        error::bad_gateway,
                                                        error::bad_gateway });
    auto rate_controller = std::make_shared<fake_rate_controller>();
    settings s;
    s.ava.max_emails_in_profiles_request = 1;

    std::vector<error_code> errors;
    auto cb = [&errors](auto& ec) { errors.push_back(ec); };
    for ([[maybe_unused]] auto& err : expected)
    {
        task->events.back().avatar_fetched = fetch_status::NONE;
        yplatform::spawn(avatar_fetcher_t{ task, rate_controller, http, s, cb });
    }
    REQUIRE(errors == expected);
}

static event make_ava_event(action a, const std::string& item)
{
    auto e = make_event(a, "[" + item + "]");
    e.metadata_count = 1;
    return e;
}

TEST_CASE("avatar_fetcher/ok")
{
    settings s;
    auto task = make_task();
    task->processed = 1;
    // Skipped – fully processed.
    task->events.push_back(
        make_ava_event(action::NEW_MAIL, R"({"from":[{"local":"user1","domain":"domain1"}]})"));
    REQUIRE(avatar_fetcher_t::required(task->events.back(), s));
    // Loaded.
    task->events.push_back(
        make_ava_event(action::NEW_MAIL, R"({"from":[{"local":"user2","domain":"domain2"}]})"));
    REQUIRE(avatar_fetcher_t::required(task->events.back(), s));
    // Skipped action.
    task->events.push_back(
        make_ava_event(action::MOVE_MAILS, R"({"from":[{"local":"user3","domain":"domain3"}]})"));
    REQUIRE_FALSE(avatar_fetcher_t::required(task->events.back(), s));
    // Skipped no sender.
    task->events.push_back(make_ava_event(action::NEW_MAIL, R"({"from":[]})"));
    REQUIRE(avatar_fetcher_t::required(task->events.back(), s));
    // Skipped multiple senders.
    task->events.push_back(make_ava_event(
        action::NEW_MAIL,
        R"({"from":[{"local":"user2","domain":"domain2"}, {"local":"user1","domain":"domain1"}]})"));
    REQUIRE(avatar_fetcher_t::required(task->events.back(), s));
    // Loaded.
    task->events.push_back(
        make_ava_event(action::NEW_MAIL, R"({"from":[{"local":"user4","domain":"domain4"}]})"));
    REQUIRE(avatar_fetcher_t::required(task->events.back(), s));
    // Skipped – no meta.
    task->events.push_back(
        make_ava_event(action::NEW_MAIL, R"({"from":[{"local":"user5","domain":"domain5"}]})"));
    task->events.back().metadata_count = 0;
    REQUIRE_FALSE(avatar_fetcher_t::required(task->events.back(), s));
    // Skipped – meta error.
    task->events.push_back(
        make_ava_event(action::NEW_MAIL, R"({"from":[{"local":"user6","domain":"domain6"}]})"));
    task->events.back().skip = true;
    REQUIRE_FALSE(avatar_fetcher_t::required(task->events.back(), s));
    // Loaded.
    task->events.push_back(
        make_ava_event(action::NEW_MAIL, R"({"from":[{"local":"user2","domain":"domain2"}]})"));
    REQUIRE(avatar_fetcher_t::required(task->events.back(), s));
    // Skipped – has avatar.
    task->events.push_back(
        make_ava_event(action::NEW_MAIL, R"({"from":[{"local":"user7","domain":"domain7"}]})"));
    task->events.back().avatar_fetched = fetch_status::FETCHED;
    REQUIRE_FALSE(avatar_fetcher_t::required(task->events.back(), s));
    // Loaded (no result).
    task->events.push_back(
        make_ava_event(action::NEW_MAIL, R"({"from":[{"local":"user8","domain":"domain8"}]})"));
    REQUIRE(avatar_fetcher_t::required(task->events.back(), s));
    // Skipped – group limit.
    task->events.push_back(
        make_ava_event(action::NEW_MAIL, R"({"from":[{"local":"user9","domain":"domain9"}]})"));
    REQUIRE(avatar_fetcher_t::required(task->events.back(), s));

    fake_http_client http({
        yhttp::response{ 200,
                         {},
                         R"({
            "user2@domain2": "avatar2",
            "user4@domain4": "avatar4"
        })",
                         "" },

    });
    auto rate_controller = std::make_shared<fake_rate_controller>();
    s.ava.max_emails_in_profiles_request = 4;
    s.ava.request = "/simple-profiles";
    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(avatar_fetcher_t{ task, rate_controller, http, s, cb });
    REQUIRE(error == make_error(error::success));

    REQUIRE(task->events.size() == 12);
    std::vector<fetch_status> expect_status{
        fetch_status::NONE,    fetch_status::FETCHED, fetch_status::NONE,    fetch_status::SKIPPED,
        fetch_status::SKIPPED, fetch_status::FETCHED, fetch_status::NONE,    fetch_status::NONE,
        fetch_status::FETCHED, fetch_status::FETCHED, fetch_status::MISSING, fetch_status::NONE,
    };
    std::vector<fetch_status> status;
    std::transform(
        task->events.begin(), task->events.end(), std::back_inserter(status), [](auto& e) {
            return e.avatar_fetched;
        });
    REQUIRE(status == expect_status);
    std::vector<size_t> expect_no_avatar{ 2, 3, 4, 6, 7, 9, 10, 11 };
    for (auto i : expect_no_avatar)
    {
        INFO(i);
        REQUIRE(task->events[i].items[0]["avatarUrl"].is_null());
    }
    REQUIRE(task->events[1].items[0]["avatarUrl"] == "avatar2");
    REQUIRE(task->events[5].items[0]["avatarUrl"] == "avatar4");
    REQUIRE(task->events[8].items[0]["avatarUrl"] == "avatar2");

    REQUIRE(http.requests().size() == 1);
    REQUIRE(http.requests().back().method == yhttp::request::method_t::POST);
    REQUIRE(http.requests().back().url == "/simple-profiles");
    REQUIRE(http.requests().back().body);
    REQUIRE(
        *http.requests().back().body ==
        "json=1&email=user2@domain2&email=user4@domain4&email=user2@domain2&email=user8@domain8");
}

TEST_CASE("avatar_fetcher/disabled_percent")
{
    settings s;
    s.ava.disabled_percent = 50;
    auto ava_event =
        make_ava_event(action::NEW_MAIL, R"({"from":[{"local":"user1","domain":"domain1"}]})");

    SECTION("uids less than disabled_percent do not require avatar")
    {
        ava_event.uid = "49";
        REQUIRE(not avatar_fetcher_t::required(ava_event, s));
    }

    SECTION("uids greater or equal than disabled_percent require avatar")
    {
        ava_event.uid = "50";
        REQUIRE(avatar_fetcher_t::required(ava_event, s));
    }

    SECTION("by default all uids require avatar")
    {
        s = {};
        ava_event.uid = "0";
        REQUIRE(avatar_fetcher_t::required(ava_event, s));
    }

    SECTION("when disabled is set to 100, no avatar is required")
    {
        s.ava.disabled_percent = 100;
        ava_event.uid = "0";
        REQUIRE(not avatar_fetcher_t::required(ava_event, s));
        ava_event.uid = "50";
        REQUIRE(not avatar_fetcher_t::required(ava_event, s));
        ava_event.uid = "99";
        REQUIRE(not avatar_fetcher_t::required(ava_event, s));
        ava_event.uid = "100";
        REQUIRE(not avatar_fetcher_t::required(ava_event, s));
    }
}