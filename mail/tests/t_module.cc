#include "tvmknife.h"

#include <ymod_tvm/tvm.h>
#include <ymod_webserver/server.h>
#include <yplatform/application/config/yaml_to_ptree.h>
#include <catch.hpp>
#include <atomic>
#include <chrono>
#include <future>
#include <string>
#include <thread>

using namespace ymod_tvm;
using namespace tvm2;
using namespace std::chrono_literals;
using std::tuple;

static const std::chrono::duration TIMEOUT = 1s;

class tvm_server
{
    auto serve_string(const std::string& s)
    {
        return [s, this](ymod_webserver::http::stream_ptr stream) {
            ++request_count_;
            stream->result(ymod_webserver::codes::ok, s);
        };
    }

public:
    tvm_server()
    {
        front_.init(1, 1);
        ymod_webserver::settings settings;
        settings.endpoints.emplace("", ymod_webserver::endpoint("", "::", 10080));
        server_ = std::make_shared<ymod_webserver::server>(*front_.io(), settings);
        server_->bind("", { "/no_keys" }, [this](ymod_webserver::http::stream_ptr stream) {
            ++request_count_;
            stream->result(ymod_webserver::codes::internal_server_error, "");
        });
        server_->bind("", { "/no_tickets" }, [this](ymod_webserver::http::stream_ptr stream) {
            ++request_count_;
            stream->result(ymod_webserver::codes::internal_server_error, "");
        });
        server_->bind("", { "/keys" }, serve_string(get_keys()));
        server_->bind(
            "",
            { "/valid_tickets_456" },
            serve_string(make_tickets({ { "456", get_service_ticket(123, 456) } })));
        server_->bind(
            "",
            { "/valid_tickets_456_222" },
            serve_string(make_tickets({ { "456", get_service_ticket(123, 456) },
                                        { "222", get_service_ticket(123, 222) } })));
        server_->bind("", { "/bad_tickets_456" }, serve_string(make_tickets({ { "456", "" } })));
        server_->bind(
            "",
            { "/mixed_tickets_789_222" },
            serve_string(make_tickets({ { "789", "" }, { "222", get_service_ticket(123, 222) } })));
        server_->bind("", { "/empty_tickets" }, serve_string(make_tickets({})));
        server_->start();
        front_.run();
    }

    ~tvm_server()
    {
        server_->stop();
        front_.stop();
        server_.reset();
        front_.fini();
    }

    yplatform::ptree stats() const
    {
        return server_->get_stats();
    }

    uint32_t total_requests() const
    {
        return request_count_;
    }

private:
    yplatform::reactor front_;
    std::shared_ptr<ymod_webserver::server> server_;
    std::atomic_uint32_t request_count_;
};

static const tvm_server server;

settings make_settings(const std::string& keys_req, const std::string& tickets_req)
{
    auto conf_str = R"(
    keys_query: )" +
        keys_req + R"(
    tickets_query: )" +
        tickets_req + R"(
    service_id: 123
    keys_update_interval: 1:00:00
    tickets_update_interval: 1:00:00
    retry_interval: 1:00:00
    https: false
    tvm_host: localhost:10080
    target_services:
        - name: blackbox
        - name: test
          id: 456
          host: yandex.ru
    blackbox_environments:
        - blackbox
        - blackbox-test
    tvm_secret: test_secret
    http:
        reuse_connection: true
        retry_policy:
            max_attempts: 1
    )";

    yplatform::ptree conf;
    utils::config::yaml_to_ptree::convert_str(conf_str, conf);
    return settings{ conf };
}

TEST_CASE("tvm/ticket_callbacks", "[tvm]")
{
    tvm tvm(make_settings("/no_keys", "/valid_tickets_456"));
    std::promise<boost::system::error_code> prom_test, prom_bb, prom_fake;
    tvm.subscribe_service_ticket(
        "blackbox", [&prom_bb](auto ec, auto, auto) { prom_bb.set_value(ec); });
    tvm.subscribe_service_ticket(
        "test", [&prom_test](auto ec, auto, auto) { prom_test.set_value(ec); });
    tvm.subscribe_service_ticket(
        "fake", [&prom_fake](auto ec, auto, auto) { prom_fake.set_value(ec); });
    tvm.start();
    auto fut_test = prom_test.get_future();
    auto fut_bb = prom_bb.get_future();
    auto fut_fake = prom_fake.get_future();
    REQUIRE(fut_test.wait_for(TIMEOUT) == std::future_status::ready);
    REQUIRE(fut_bb.wait_for(TIMEOUT) == std::future_status::ready);
    REQUIRE(fut_fake.wait_for(TIMEOUT) == std::future_status::ready);
    CHECK(fut_test.get() == make_error_code(error::success));
    CHECK(fut_bb.get() == make_error_code(error::no_ticket_for_service));
    CHECK(fut_fake.get() == make_error_code(error::unknown_service));
}

TEST_CASE("tvm/keys_callbacks", "[tvm]")
{
    tvm tvm(make_settings("/keys", "/no_tickets"));
    std::promise<void> keys_loaded_prom;
    auto keys_loaded = keys_loaded_prom.get_future();
    tvm.subscribe_keys_loaded([&keys_loaded_prom]() { keys_loaded_prom.set_value(); });
    tvm.start();
    REQUIRE(keys_loaded.wait_for(TIMEOUT) == std::future_status::ready);
}

TEST_CASE("tvm/no_callbacks_when_nothing_available", "[tvm]")
{
    tvm tvm(make_settings("/no_keys", "/no_tickets"));
    std::promise<void> keys_loaded_prom, tickets_loaded_prom;
    auto keys_loaded = keys_loaded_prom.get_future(),
         tickets_loaded = tickets_loaded_prom.get_future();
    tvm.subscribe_keys_loaded([&keys_loaded_prom]() { keys_loaded_prom.set_value(); });
    tvm.subscribe_all_tickets_are_ready(
        [&tickets_loaded_prom]() { tickets_loaded_prom.set_value(); });
    tvm.start();
    REQUIRE(keys_loaded.wait_for(TIMEOUT) == std::future_status::timeout);
    REQUIRE(tickets_loaded.wait_for(TIMEOUT) == std::future_status::timeout);
}

TEST_CASE("tvm/no_requests_before_start", "[tvm]")
{
    auto r0 = server.total_requests();
    tvm tvm(make_settings("/keys", "/valid_tickets_456"));
    auto r1 = server.total_requests();
    std::this_thread::sleep_for(250ms);
    auto r2 = server.total_requests();
    CHECK(r0 == r1);
    CHECK(r0 == r2);
}

TEST_CASE("tvm/requests_keys_and_tockets_on_start", "[tvm]")
{
    auto r0 = server.total_requests();
    tvm tvm(make_settings("/keys", "/valid_tickets_456"));
    auto r1 = server.total_requests();
    tvm.start();
    std::this_thread::sleep_for(250ms);
    auto r2 = server.total_requests();
    CHECK(r0 == r1);
    CHECK(r2 == r1 + 2);
}

TEST_CASE("tvm/cant_get_and_check_tickets_without_keys_and_tickets", "[tvm]")
{
    tvm tvm(make_settings("/no_keys", "/no_tickets"));
    tvm.start();
    std::this_thread::sleep_for(250ms);

    auto ctx = boost::make_shared<yplatform::task_context>();
    std::string ticket;
    CHECK(tvm.check_service_ticket(ctx, "...") == make_error_code(error::keys_not_loaded));
    CHECK(tvm.get_service_ticket("...", ticket) == make_error_code(error::tickets_not_loaded));
}

TEST_CASE("tvm/can_check_tickets_but_cant_get_with_keys", "[tvm]")
{
    tvm tvm(make_settings("/keys", "/no_tickets"));
    std::promise<void> keys_loaded_prom;
    auto keys_loaded = keys_loaded_prom.get_future();
    tvm.subscribe_keys_loaded([&keys_loaded_prom]() { keys_loaded_prom.set_value(); });
    tvm.start();

    auto ctx = boost::make_shared<yplatform::task_context>();
    std::string ticket;
    REQUIRE(keys_loaded.wait_for(TIMEOUT) == std::future_status::ready);
    CHECK(
        tvm.check_service_ticket(ctx, "I'm not really a ticket") ==
        make_error_code(TA_EErrorCode::TA_EC_MALFORMED_TICKET));
    CHECK(
        tvm.check_service_ticket(ctx, get_service_ticket(456, 789)) ==
        make_error_code(TA_EErrorCode::TA_EC_INVALID_DST));
    CHECK(
        tvm.check_service_ticket(ctx, get_service_ticket(456, 123)) ==
        make_error_code(TA_EErrorCode::TA_EC_OK));
    CHECK(tvm.get_service_ticket("...", ticket) == make_error_code(error::tickets_not_loaded));
    // Tickets for host.
    CHECK(
        tvm.get_service_ticket_for_host("yandex.ru") ==
        tuple(make_error_code(error::tickets_not_loaded), string{}));
    // Host that doesn't need tickets.
    CHECK(
        tvm.get_service_ticket_for_host("yandex.com") ==
        tuple(make_error_code(error::success), string{}));
}

TEST_CASE("tvm/cant_check_tickets_with_tickets", "[tvm]")
{
    tvm tvm(make_settings("/no_keys", "/valid_tickets_456"));
    std::promise<void> tickets_loaded_prom;
    auto tickets_loaded = tickets_loaded_prom.get_future();
    tvm.subscribe_all_tickets_are_ready(
        [&tickets_loaded_prom]() { tickets_loaded_prom.set_value(); });
    tvm.start();

    auto ctx = boost::make_shared<yplatform::task_context>();
    REQUIRE(tickets_loaded.wait_for(TIMEOUT) == std::future_status::ready);
    CHECK(tvm.check_service_ticket(ctx, "...") == make_error_code(error::keys_not_loaded));
}

TEST_CASE("tvm/get_tickets", "[tvm]")
{
    tvm tvm(make_settings("/no_keys", "/valid_tickets_456_222"));
    std::promise<void> tickets_loaded_prom;
    auto tickets_loaded = tickets_loaded_prom.get_future();
    tvm.subscribe_all_tickets_are_ready(
        [&tickets_loaded_prom]() { tickets_loaded_prom.set_value(); });
    tvm.start();

    REQUIRE(tickets_loaded.wait_for(TIMEOUT) == std::future_status::ready);
    std::string ticket_test, ticket_bb, ticket, host_ticket;
    CHECK(tvm.get_service_ticket("test", ticket_test) == make_error_code(error::success));
    CHECK(tvm.get_service_ticket("blackbox", ticket_bb) == make_error_code(error::success));
    CHECK(ticket_test.size() > 0);
    CHECK(ticket_bb.size() > 0);
    CHECK(tvm.get_service_ticket("fake", ticket) == make_error_code(error::unknown_service));
}

TEST_CASE("tvm/missing_tickets", "[tvm]")
{
    tvm tvm(make_settings("/no_keys", "/bad_tickets_456"));
    std::promise<void> tickets_loaded_prom;
    auto tickets_loaded = tickets_loaded_prom.get_future();
    tvm.subscribe_all_tickets_are_ready(
        [&tickets_loaded_prom]() { tickets_loaded_prom.set_value(); });
    tvm.start();

    REQUIRE(tickets_loaded.wait_for(TIMEOUT) == std::future_status::ready);
    std::string ticket;
    CHECK(tvm.get_service_ticket("test", ticket) == make_error_code(error::no_ticket_for_service));
    CHECK(
        tvm.get_service_ticket("blackbox", ticket) ==
        make_error_code(error::no_ticket_for_service));
    // Tickets for host.
    CHECK(
        tvm.get_service_ticket_for_host("yandex.ru") ==
        tuple(make_error_code(error::no_ticket_for_service), string{}));
}

TEST_CASE("tvm/mismatch_tickets", "[tvm]")
{
    tvm tvm(make_settings("/no_keys", "/mixed_tickets_789_222"));
    std::promise<void> tickets_loaded_prom;
    auto tickets_loaded = tickets_loaded_prom.get_future();
    tvm.subscribe_all_tickets_are_ready(
        [&tickets_loaded_prom]() { tickets_loaded_prom.set_value(); });
    tvm.start();

    REQUIRE(tickets_loaded.wait_for(TIMEOUT) == std::future_status::ready);
    std::string ticket;
    CHECK(tvm.get_service_ticket("test", ticket) == make_error_code(error::no_ticket_for_service));
    CHECK(tvm.get_service_ticket("blackbox", ticket) == make_error_code(error::success));
    CHECK(ticket.size() > 0);
}

TEST_CASE("tvm/get_tickets_for_host", "[tvm]")
{
    tvm tvm(make_settings("/no_keys", "/valid_tickets_456_222"));
    std::promise<void> tickets_loaded_prom;
    auto tickets_loaded = tickets_loaded_prom.get_future();
    tvm.subscribe_all_tickets_are_ready(
        [&tickets_loaded_prom]() { tickets_loaded_prom.set_value(); });
    tvm.start();

    REQUIRE(tickets_loaded.wait_for(TIMEOUT) == std::future_status::ready);
    CHECK(
        tvm.get_service_ticket_for_host("other.host") ==
        tuple(make_error_code(error::success), string{}));
    // Ticket for service to compare with.
    std::string ticket;
    CHECK(tvm.get_service_ticket("test", ticket) == make_error_code(error::success));
    CHECK_FALSE(ticket.empty());

    CHECK(
        tvm.get_service_ticket_for_host("yandex.ru") ==
        tuple(make_error_code(error::success), ticket));
    CHECK(
        tvm.get_service_ticket_for_host("yaNdeX.rU") ==
        tuple(make_error_code(error::success), ticket));
}

TEST_CASE("tvm/wait_first_update_on_start", "[tvm]")
{
    auto settings = make_settings("/keys", "/valid_tickets_456_222");
    settings.wait_first_update_on_start = true;
    auto r0 = server.total_requests();
    tvm tvm(settings);
    auto r1 = server.total_requests();
    REQUIRE(r1 == r0 + 2);

    std::promise<void> keys_loaded_prom, tickets_loaded_prom;
    auto keys_loaded = keys_loaded_prom.get_future(),
         tickets_loaded = tickets_loaded_prom.get_future();
    tvm.subscribe_keys_loaded([&keys_loaded_prom]() { keys_loaded_prom.set_value(); });
    tvm.subscribe_all_tickets_are_ready(
        [&tickets_loaded_prom]() { tickets_loaded_prom.set_value(); });
    tvm.start();
    REQUIRE(keys_loaded.wait_for(TIMEOUT) == std::future_status::ready);
    REQUIRE(tickets_loaded.wait_for(TIMEOUT) == std::future_status::ready);
}

TEST_CASE("tvm/runtime_host_configuration", "[tvm]")
{
    auto settings = make_settings("/keys", "/valid_tickets_456_222");
    tvm tvm(settings);
    // Service doesn't exist.
    CHECK_THROWS(tvm.bind_host("newhost", "text"));
    // OK.
    CHECK_NOTHROW(tvm.bind_host("newhost", "test"));
    // Already bound.
    CHECK_THROWS(tvm.bind_host("yandex.ru", "test"));

    std::promise<void> tickets_loaded_prom;
    auto tickets_loaded = tickets_loaded_prom.get_future();
    tvm.subscribe_all_tickets_are_ready(
        [&tickets_loaded_prom]() { tickets_loaded_prom.set_value(); });
    tvm.start();
    REQUIRE(tickets_loaded.wait_for(TIMEOUT) == std::future_status::ready);
    // Module is started.
    CHECK_THROWS(tvm.bind_host("newhost2", "test"));
    std::string ticket;
    CHECK(tvm.get_service_ticket("test", ticket) == make_error_code(error::success));
    CHECK(
        tvm.get_service_ticket_for_host("newhost") ==
        tuple(make_error_code(error::success), ticket));
}
