#include "helpers.h"
#include <processor/counters_fetcher.h>
#include <yplatform/coroutine.h>
#include <catch.hpp>

using counters_fetcher_t = counters_fetcher<fake_http_client>;

namespace yxiva::mailpusher {

inline bool operator==(const fid_counters& lhs, const fid_counters& rhs)
{
    return lhs.fid == rhs.fid && lhs.count == rhs.count && lhs.count_new == rhs.count_new;
}

inline bool operator!=(const fid_counters& lhs, const fid_counters& rhs)
{
    return !operator==(lhs, rhs);
}

inline std::ostream& operator<<(std::ostream& os, const fid_counters& c)
{
    os << "fid: " << c.fid << " count: " << c.count << " count_new: " << c.count_new;
    return os;
}

}

static auto make_default_settings()
{
    settings s;
    s.environment = "a";
    s.counters["a"].request = "/folders_counters";
    return s;
}

TEST_CASE("counters_fetcher/rate_controller/errors")
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
        yplatform::spawn(counters_fetcher_t{
            task, make_rate_controllers(rate_controller), make_http_clients(http), s, cb });
    }
    REQUIRE(errors == expected);
}

TEST_CASE("counters_fetcher/http/errors")
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
        yplatform::spawn(counters_fetcher_t{
            task, make_rate_controllers(rate_controller), make_http_clients(http), s, cb });
    }
    REQUIRE(errors == expected);
}

TEST_CASE("counters_fetcher/bad_response")
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
        yhttp::response{ 200, {}, "[\"not an object\"]", "" },
        yhttp::response{ 200, {}, R"({"missing": "folders key"})", "" },
        yhttp::response{ 200, {}, R"({"folders": "wrong type"})", "" },
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
        error::bad_gateway,
        error::bad_gateway,
    });
    auto rate_controller = std::make_shared<fake_rate_controller>();
    auto s = make_default_settings();

    std::vector<error_code> errors;
    auto cb = [&errors](auto& ec) { errors.push_back(ec); };
    for ([[maybe_unused]] auto& err : expected)
    {
        yplatform::spawn(counters_fetcher_t{
            task, make_rate_controllers(rate_controller), make_http_clients(http), s, cb });
    }
    REQUIRE(errors == expected);
}

TEST_CASE("counters_fetcher/ok")
{
    auto task = make_task();
    task->uid = "123";
    task->events.resize(3);
    task->events[0].action_type = task->events[2].action_type = action::NEW_MAIL;
    task->events[1].action_type = action::MOVE_MAILS;
    fake_http_client http({
        yhttp::response{ 200,
                         {},
                         R"({
           "folders" : {
              "4" : {
                 "cnt" : 0,
                 "new" : 0
              },
              "5" : {
                 "cnt" : 7,
                 "new" : 3
              },
              "7" : {
                 "new" : 0,
                 "cnt" : 0
              },
              "2" : {
                 "cnt" : 0,
                 "new" : 0
              },
              "6" : {
                 "cnt" : 3,
                 "new" : 0
              },
              "3" : {
                 "cnt" : 0,
                 "new" : 0
              },
              "1" : {
                 "cnt" : 1,
                 "new" : 1
              }
           }
        })",
                         "" },

    });
    std::string counters = "[5,7,6,3,1,1]";
    std::string counters_new = "[5,3,1,1]";
    auto rate_controller = std::make_shared<fake_rate_controller>();
    auto s = make_default_settings();

    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(counters_fetcher_t{
        task, make_rate_controllers(rate_controller), make_http_clients(http), s, cb });
    REQUIRE(error == make_error(error::success));

    REQUIRE(task->events.size() == 3);
    REQUIRE(json_write(task->events[0].args["counters"]) == counters);
    REQUIRE(json_write(task->events[0].args["countersNew"]) == counters_new);
    REQUIRE(task->events[1].args.empty());
    REQUIRE(json_write(task->events[2].args["counters"]) == counters);
    REQUIRE(json_write(task->events[2].args["countersNew"]) == counters_new);

    REQUIRE(http.requests().size() == 1);
    REQUIRE(http.requests().back().method == yhttp::request::method_t::GET);
    REQUIRE(http.requests().back().url == "/folders_counters?uid=123");
}

TEST_CASE("counters_fetcher/fetches counters for extra accounts, if any")
{
    static const string COUNTERS_123 = R"({
        "folders" : {
            "1" : {
                "cnt" : 2,
                "new" : 3
            }
        }
    })";
    static const string COUNTERS_A = R"({
        "folders" : {
            "2" : {
                "cnt" : 7,
                "new" : 7
            }
        }
    })";
    static const string COUNTERS_B = R"({
        "folders" : {
            "3" : {
                "cnt" : 8,
                "new" : 8
            }
        }
    })";
    auto task = make_task();
    task->uid = "123";
    task->events.resize(1);
    task->events[0].action_type = action::NEW_MAIL;
    fake_http_client http({
        yhttp::response{ 200, {}, COUNTERS_123, "" },
        yhttp::response{ 200, {}, COUNTERS_A, "" },
        yhttp::response{ 200, {}, COUNTERS_B, "" },
    });
    auto rate_controller = std::make_shared<fake_rate_controller>();
    rate_controllers<ymod_ratecontroller::rate_controller_ptr> rate_controllers;
    rate_controllers.emplace("a", rate_controller);
    rate_controllers.emplace("b", rate_controller);
    http_clients<fake_http_client> http_clients;
    http_clients.emplace("a", http);
    http_clients.emplace("b", http);
    auto s = make_default_settings();
    s.counters["b"] = s.counters["a"];
    task->devices.emplace(
        "mob:id",
        device{ "device_id",
                "device_platform",
                "mobile_app",
                {
                    { "A", "a", {}, true, true, {} },
                    { "B", "b", {}, true, true, {} },
                } });

    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(counters_fetcher_t{ task, rate_controllers, http_clients, s, cb });
    REQUIRE(error == make_error(error::success));

    auto& account_a = task->devices["mob:id"].accounts[0];
    REQUIRE(account_a.counters_fetched == fetch_status::FETCHED);
    REQUIRE(account_a.fids_counters == std::vector<fid_counters>{ { 2, 7, 7 } });
    auto& account_b = task->devices["mob:id"].accounts[1];
    REQUIRE(account_b.counters_fetched == fetch_status::FETCHED);
    REQUIRE(account_b.fids_counters == std::vector<fid_counters>{ { 3, 8, 8 } });
}