#include "helpers.h"
#include <processor/metadata_fetcher.h>
#include <yplatform/coroutine.h>
#include <catch.hpp>

using metadata_fetcher_t = metadata_fetcher<fake_http_client>;

TEST_CASE("metadata_fetcher/rate_controller/errors")
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
        yplatform::spawn(metadata_fetcher_t{ task, rate_controller, http, s, cb });
    }
    REQUIRE(errors == expected);
}

TEST_CASE("metadata_fetcher/http/errors")
{
    auto task = make_task();
    task->events.resize(1);
    task->events.back().action_type = action::NEW_MAIL;
    task->events.back().items.push_back(json_parse(R"({"mid": "123"})"));

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
    s.meta.max_mids_in_filter_search_request = 1;

    std::vector<error_code> errors;
    auto cb = [&errors](auto& ec) { errors.push_back(ec); };
    for ([[maybe_unused]] auto& err : expected)
    {
        yplatform::spawn(metadata_fetcher_t{ task, rate_controller, http, s, cb });
    }
    // Check that every run terminated on the first error.
    REQUIRE(http.requests().size() == expected.size());
    REQUIRE(errors == expected);
}

TEST_CASE("metadata_fetcher/bad_response")
{
    auto task = make_task();
    task->events.resize(1);
    task->events.back().action_type = action::NEW_MAIL;
    task->events.back().items.push_back(json_parse(R"({"mid": "123"})"));

    fake_http_client http({ yhttp::response{ 500, {}, "", "" },
                            yhttp::response{ 400, {}, "", "" },
                            yhttp::response{ 404, {}, "", "" },
                            yhttp::response{ 429, {}, "", "" },
                            yhttp::response{ 200, {}, "", "" },
                            yhttp::response{ 200, {}, "not json", "" },
                            yhttp::response{ 200, {}, "[}", "" },
                            yhttp::response{ 200, {}, "[\"not an object\"]", "" },
                            yhttp::response{ 200, {}, R"({"error": "some error"})", "" },
                            yhttp::response{ 200, {}, R"({"missing": "envelopes"})", "" },
                            yhttp::response{ 200, {}, R"({"envelopes": {"wrong": "type"}})", "" },
                            yhttp::response{ 200, {}, R"({"error": {"code": 1006}})", "" } });
    std::vector<error_code> expected = to_error_codes({ error::bad_gateway,
                                                        error::remote_bad_request,
                                                        error::remote_bad_request,
                                                        error::rate_limit,
                                                        error::bad_gateway,
                                                        error::bad_gateway,
                                                        error::bad_gateway,
                                                        error::bad_gateway,
                                                        error::bad_gateway,
                                                        error::bad_gateway,
                                                        error::bad_gateway,
                                                        error::uninitialized_user });
    auto rate_controller = std::make_shared<fake_rate_controller>();
    settings s;
    s.meta.max_mids_in_filter_search_request = 1;

    std::vector<error_code> errors;
    auto cb = [&errors](auto& ec) { errors.push_back(ec); };
    for ([[maybe_unused]] auto& err : expected)
    {
        yplatform::spawn(metadata_fetcher_t{ task, rate_controller, http, s, cb });
    }
    // Check that every run terminated on the first error.
    REQUIRE(http.requests().size() == expected.size());
    REQUIRE(errors == expected);
}

TEST_CASE("metadata_fetcher/master_fail")
{
    auto task = make_task();
    task->uid = "123";
    task->events.push_back(make_event(action::MOVE_MAILS, R"([{"mid": "123"},{"mid": "456"}])"));
    REQUIRE(metadata_fetcher_t::required(task->events.back()));
    REQUIRE_FALSE(metadata_fetcher_t::done(task->events.back()));

    fake_http_client http(
        { yhttp::response{ 200, {}, R"({"envelopes": [{"mid": "123", "key": "value"}]})", "" },
          yhttp::response{ 200, {}, R"({"error": "fail"})", "" } });
    auto rate_controller = std::make_shared<fake_rate_controller>();
    settings s;
    s.meta.request = "/filter_search";
    s.meta.fallback_to_db_master_max_lag = 0;
    s.meta.max_mids_in_filter_search_request = 10;

    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(metadata_fetcher_t{ task, rate_controller, http, s, cb });
    // Replica and master requests.
    REQUIRE(error == make_error(error::bad_gateway));
    REQUIRE(http.requests().size() == 2);
    REQUIRE(
        http.requests()[0].url ==
        "/filter_search?uid=123&dbtype=replica&full_folders_and_labels=1&mids=123&mids=456");
    // Retries whole batch to master db.
    REQUIRE(
        http.requests()[1].url ==
        "/filter_search?uid=123&dbtype=master&full_folders_and_labels=1&mids=123&mids=456");
    REQUIRE(metadata_fetcher_t::required(task->events.back()));
    REQUIRE_FALSE(metadata_fetcher_t::done(task->events.back()));
    REQUIRE_FALSE(task->events.back().skip);
}

TEST_CASE("metadata_fetcher/lag_ignore")
{
    auto task = make_task();
    task->uid = "123";
    task->events.push_back(make_event(action::NEW_MAIL, R"([{"mid": "123"}])"));
    task->events.back().ts -= 20;
    task->events.push_back(make_event(action::NEW_MAIL, R"([{"mid": "456"}])"));

    fake_http_client http(
        { yhttp::response{ 200, {}, R"({"envelopes": [{"mid": "456", "key": "value"}]})", "" } });
    auto rate_controller = std::make_shared<fake_rate_controller>();
    settings s;
    s.meta.request = "/filter_search";
    s.meta.fallback_to_db_master_max_lag = 10;
    s.meta.max_mids_in_filter_search_request = 10;

    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(metadata_fetcher_t{ task, rate_controller, http, s, cb });
    // Replica and master requests.
    REQUIRE(error == make_error(error::success));
    REQUIRE(http.requests().size() == 1);
    REQUIRE(
        http.requests()[0].url ==
        "/filter_search?uid=123&dbtype=replica&full_folders_and_labels=1&mids=123&mids=456");
    REQUIRE(task->events.size() == 2);
    REQUIRE_FALSE(metadata_fetcher_t::done(task->events[0]));
    REQUIRE(task->events[0].skip);
    REQUIRE(metadata_fetcher_t::done(task->events[1]));
    REQUIRE_FALSE(task->events[1].skip);
}

TEST_CASE("metadata_fetcher/ok")
{
    auto task = make_task();
    task->uid = "123";
    task->processed = 1;
    // Skipped – already processed.
    task->events.push_back(make_event(action::MOVE_MAILS, R"([{"mid": "1"},{"mid": "2"}])"));
    REQUIRE(metadata_fetcher_t::required(task->events.back()));
    REQUIRE_FALSE(metadata_fetcher_t::done(task->events.back()));
    // Loaded from replica.
    task->events.push_back(make_event(action::NEW_MAIL, R"([{"mid": "3"}])"));
    REQUIRE(metadata_fetcher_t::required(task->events.back()));
    REQUIRE_FALSE(metadata_fetcher_t::done(task->events.back()));
    // Skipped by action.
    task->events.push_back(make_event(action::MARK_MAILS, R"([{"mid": "4"}])"));
    REQUIRE_FALSE(metadata_fetcher_t::required(task->events.back()));
    REQUIRE_FALSE(metadata_fetcher_t::done(task->events.back()));
    // Skipped – has metadata.
    task->events.push_back(make_event(action::MOVE_MAILS, R"([{"mid": "5"},{"mid": "6"}])"));
    task->events.back().metadata_count = 2;
    REQUIRE_FALSE(metadata_fetcher_t::required(task->events.back()));
    REQUIRE(metadata_fetcher_t::done(task->events.back()));
    // Processed partially – has some metadata.
    task->events.push_back(make_event(action::MOVE_MAILS, R"([{"mid": "7"},{"mid": "8"}])"));
    task->events.back().metadata_count = 1;
    REQUIRE(metadata_fetcher_t::required(task->events.back()));
    REQUIRE_FALSE(metadata_fetcher_t::done(task->events.back()));
    // Loaded from master.
    task->events.push_back(make_event(action::NEW_MAIL, R"([{"mid": "9"}])"));
    REQUIRE(metadata_fetcher_t::required(task->events.back()));
    REQUIRE_FALSE(metadata_fetcher_t::done(task->events.back()));
    // Part is loaded from replica, part is missing. Not an insert => ok.
    task->events.push_back(make_event(action::MOVE_MAILS, R"([{"mid": "10"},{},{"mid": "11"}])"));
    REQUIRE(metadata_fetcher_t::required(task->events.back()));
    REQUIRE_FALSE(metadata_fetcher_t::done(task->events.back()));
    // Loaded partially due to pack size.
    task->events.push_back(make_event(action::MOVE_MAILS, R"([{"mid": "13"},{"mid": "14"}])"));
    REQUIRE(metadata_fetcher_t::required(task->events.back()));
    REQUIRE_FALSE(metadata_fetcher_t::done(task->events.back()));
    // Skipped due to pack size.
    task->events.push_back(make_event(action::NEW_MAIL, R"([{"mid": "15"}])"));
    REQUIRE(metadata_fetcher_t::required(task->events.back()));
    REQUIRE_FALSE(metadata_fetcher_t::done(task->events.back()));

    fake_http_client http({
        yhttp::response{ 200,
                         {},
                         R"({"envelopes": [
            {"mid": "3", "key3": "value3"},
            {"mid": "8", "key8": "value8"},
            {"mid": "11", "key11": "value11"},
            {"mid": "13", "key13": "value13"}
        ]})",
                         "" },
        yhttp::response{ 200,
                         {},
                         R"({"envelopes": [
            {"mid": "3", "key3": "value3"},
            {"mid": "8", "key8": "value8"},
            {"mid": "9", "key9": "value9"},
            {"mid": "11", "key11": "value11"},
            {"mid": "13", "key13": "value13"}
        ]})",
                         "" },
    });
    auto rate_controller = std::make_shared<fake_rate_controller>();
    settings s;
    s.meta.request = "/filter_search";
    s.meta.max_mids_in_filter_search_request = 6;
    s.meta.fallback_to_db_master_max_lag = 10;
    error_code error;
    auto cb = [&error](auto& ec) { error = ec; };
    yplatform::spawn(metadata_fetcher_t{ task, rate_controller, http, s, cb });

    REQUIRE(error == make_error(error::success));
    REQUIRE(http.requests().size() == 2);
    REQUIRE(
        http.requests()[0].url ==
        "/filter_search?uid=123&dbtype=replica&full_folders_and_labels=1&mids=3&mids=8&mids=9&mids="
        "10&mids=11&mids=13");
    REQUIRE(
        http.requests()[1].url ==
        "/filter_search?uid=123&dbtype=master&full_folders_and_labels=1&mids=3&mids=8&mids=9&mids="
        "10&mids=11&mids=13");

    REQUIRE(task->events.size() == 9);
    std::vector<size_t> expect_fetched{ 1, 4, 5, 6 };
    for (auto i : expect_fetched)
    {
        INFO(i);
        REQUIRE_FALSE(task->events[i].skip);
        REQUIRE_FALSE(metadata_fetcher_t::required(task->events[i]));
        REQUIRE(metadata_fetcher_t::done(task->events[i]));
    }
    std::vector<size_t> expect_not_fetched{ 0, 2, 7, 8 };
    for (auto i : expect_not_fetched)
    {
        INFO(i);
        REQUIRE_FALSE(metadata_fetcher_t::done(task->events[i]));
    }
    std::vector<size_t> expect_processed{ 1, 2, 3, 4, 5, 6 };
    for (auto i : expect_processed)
    {
        INFO(i);
        REQUIRE_FALSE(metadata_fetcher_t::required(task->events[i]));
    }
    REQUIRE(task->events[7].metadata_count == 1);

    REQUIRE(dump(task->events[0]) == R"([{"mid":"1"},{"mid":"2"}])");
    REQUIRE(dump(task->events[1]) == R"([{"mid":"3","key3":"value3"}])");
    REQUIRE(dump(task->events[2]) == R"([{"mid":"4"}])");
    REQUIRE(dump(task->events[3]) == R"([{"mid":"5"},{"mid":"6"}])");
    REQUIRE(dump(task->events[4]) == R"([{"mid":"7"},{"mid":"8","key8":"value8"}])");
    REQUIRE(dump(task->events[5]) == R"([{"mid":"9","key9":"value9"}])");
    REQUIRE(dump(task->events[6]) == R"([{"mid":"10"},{},{"mid":"11","key11":"value11"}])");
    REQUIRE(dump(task->events[7]) == R"([{"mid":"13","key13":"value13"},{"mid":"14"}])");
    REQUIRE(dump(task->events[8]) == R"([{"mid":"15"}])");
}
