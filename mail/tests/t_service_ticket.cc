#include <catch.hpp>

#include <ymod_httpclient/client.h>
#include <ymod_httpclient/errors.h>
#include <ymod_webserver/server.h>
#include <ticket_provider/for_host.h>
#include <yplatform/application.h>
#include <yplatform/module.h>
#include <yplatform/module_registration.h>
#include <yplatform/ptree.h>
#include <yplatform/reactor.h>
#include <boost/optional.hpp>
#include <boost/system/error_code.hpp>

#include <chrono>
#include <future>
#include <iostream>
#include <stdexcept>
#include <string>

using namespace std::chrono_literals;
using namespace ymod_httpclient;
using yplatform::task_context;

static const std::chrono::duration TIMEOUT = 250ms;

class fake_tvm
    : public ticket_provider::for_host
    , public yplatform::module
{
public:
    void set_ready(bool ready = true)
    {
        is_ready = ready;
    }

    void set_localhost(bool localhost = true)
    {
        localhost_enabled = localhost;
    }

    void set_unknown(bool unknown = true)
    {
        unknown_service = unknown;
    }

    std::tuple<boost::system::error_code, std::string> get_service_ticket_for_host(
        const std::string& host) override
    {
        auto enabled = localhost_enabled && host == "localhost";
        auto fail = !is_ready || unknown_service;
        return { make_error_code(
                     enabled && fail ? http_error::no_service_ticket : http_error::success),
                 enabled && !fail ? "fake_ticket" : "" };
    }

private:
    bool is_ready = true;
    bool localhost_enabled = true;
    bool unknown_service = false;
};

class local_server : public ymod_webserver::server
{
public:
    local_server(yplatform::reactor& reactor, unsigned short port)
        : ymod_webserver::server(*reactor.io(), make_settings(port))
    {
    }

private:
    ymod_webserver::settings make_settings(unsigned short port)
    {
        ymod_webserver::endpoint ep;
        ep.addr = "::";
        ep.port = port;
        ymod_webserver::settings settings;
        settings.endpoints.emplace("", ep);
        return settings;
    }
};

TEST_CASE("tvm/ymod_tvm_reqired", "[tvm]")
{
    settings settings;
    auto reactor = std::make_shared<yplatform::reactor>();
    reactor->init(1, 1);

    SECTION("not required when disabled")
    {
        settings.service_ticket_provider.module = "";
        CHECK_NOTHROW(client(*reactor, settings));
    }

    settings.service_ticket_provider.module = "tvm";
    SECTION("required when enabled")
    {
        CHECK_THROWS_WITH(client(*reactor, settings), Catch::Contains("module \"tvm\" not found"));
    }
    SECTION("found when enabled and present")
    {
        yplatform::register_module(
            *reactor->io(), settings.service_ticket_provider.module, make_shared<fake_tvm>());
        CHECK_NOTHROW(client(*reactor, settings));
    }
    reactor->fini();
}

TEST_CASE("tvm/work_sync", "[tvm]")
{
    std::string ping_url = "http://localhost:10080/ping";

    settings settings;
    settings.service_ticket_provider.module = "tvm";

    auto reactor = std::make_shared<yplatform::reactor>();
    reactor->init(1, 1);

    auto tvm = make_shared<fake_tvm>();
    yplatform::register_module(*reactor->io(), "tvm", tvm);

    local_server srv(*reactor, 10080);
    boost::optional<std::string> tvm_header;
    bool request_sent = false;
    srv.bind(
        "", { "/ping" }, [&tvm_header, &request_sent](ymod_webserver::http::stream_ptr stream) {
            request_sent = true;
            auto it = stream->request()->headers.find("x-ya-service-ticket");
            if (it != stream->request()->headers.end()) tvm_header = it->second;
            stream->result(ymod_webserver::codes::ok, "pong");
        });
    srv.start();
    reactor->run();

    SECTION("tvm errors are not ignored")
    {
        settings.service_ticket_provider.ignore_errors = false;
        client http(*reactor, settings);

        SECTION("no header is sent if no ticket acquired")
        {
            tvm->set_ready(false);
            tvm->set_localhost(false);
            auto res = http.run(boost::make_shared<task_context>(), request::GET(ping_url));
            REQUIRE(res.status == 200);
            REQUIRE(request_sent);
            REQUIRE(!tvm_header);
        }
        SECTION("correct header is sent when ticket acquired")
        {
            tvm->set_ready();
            tvm->set_localhost();
            auto res = http.run(boost::make_shared<task_context>(), request::GET(ping_url));
            REQUIRE(res.status == 200);
            REQUIRE(request_sent);
            REQUIRE(tvm_header);
            REQUIRE(tvm_header.get() == "fake_ticket");
        }
        SECTION("request fails if tvm is not ready")
        {
            tvm->set_ready(false);
            tvm->set_localhost();
            CHECK_THROWS_AS(
                http.run(boost::make_shared<task_context>(), request::GET(ping_url)),
                no_service_ticket_error);
            REQUIRE(!request_sent);
            REQUIRE(!tvm_header);
        }
        SECTION("request fails if there's no correct ticket")
        {
            tvm->set_ready();
            tvm->set_localhost();
            tvm->set_unknown();
            CHECK_THROWS_AS(
                http.run(boost::make_shared<task_context>(), request::GET(ping_url)),
                no_service_ticket_error);
            REQUIRE(!request_sent);
            REQUIRE(!tvm_header);
        }
    }
    SECTION("tvm errors are ignored")
    {
        settings.service_ticket_provider.ignore_errors = true;
        client http(*reactor, settings);

        SECTION("no header is sent if no ticket acquired")
        {
            tvm->set_ready(false);
            tvm->set_localhost(false);
            auto res = http.run(boost::make_shared<task_context>(), request::GET(ping_url));
            REQUIRE(res.status == 200);
            REQUIRE(request_sent);
            REQUIRE(!tvm_header);
        }
        SECTION("correct header is sent when ticket acquired")
        {
            tvm->set_ready();
            tvm->set_localhost();
            auto res = http.run(boost::make_shared<task_context>(), request::GET(ping_url));
            REQUIRE(res.status == 200);
            REQUIRE(request_sent);
            REQUIRE(tvm_header);
            REQUIRE(tvm_header.get() == "fake_ticket");
        }
        SECTION("no header is sent if tvm is not ready")
        {
            tvm->set_ready(false);
            tvm->set_localhost();
            auto res = http.run(boost::make_shared<task_context>(), request::GET(ping_url));
            REQUIRE(res.status == 200);
            REQUIRE(request_sent);
            REQUIRE(!tvm_header);
        }
        SECTION("no header is sent if there's no correct ticket")
        {
            tvm->set_ready();
            tvm->set_localhost();
            tvm->set_unknown();
            auto res = http.run(boost::make_shared<task_context>(), request::GET(ping_url));
            REQUIRE(res.status == 200);
            REQUIRE(request_sent);
            REQUIRE(!tvm_header);
        }
    }
    reactor->stop();
    reactor->fini();
}

TEST_CASE("tvm/work_async", "[tvm]")
{
    std::string ping_url = "http://localhost:10080/ping";

    settings settings;
    settings.service_ticket_provider.module = "tvm";

    auto reactor = std::make_shared<yplatform::reactor>();
    reactor->init(2, 1);

    auto tvm = make_shared<fake_tvm>();
    yplatform::register_module(*reactor->io(), "tvm", tvm);

    local_server srv(*reactor, 10080);
    boost::optional<std::string> tvm_header;
    bool request_sent = false;
    srv.bind(
        "", { "/ping" }, [&tvm_header, &request_sent](ymod_webserver::http::stream_ptr stream) {
            request_sent = true;
            auto it = stream->request()->headers.find("x-ya-service-ticket");
            if (it != stream->request()->headers.end()) tvm_header = it->second;
            stream->result(ymod_webserver::codes::ok, "pong");
        });

    std::promise<boost::system::error_code> prom;
    auto cb_res = prom.get_future();
    auto cb = [&prom](const boost::system::error_code& err, response) { prom.set_value(err); };

    srv.start();
    reactor->run();

    SECTION("tvm errors are not ignored")
    {
        settings.service_ticket_provider.ignore_errors = false;
        client http(*reactor, settings);

        SECTION("no header is sent if no ticket acquired")
        {
            tvm->set_ready(false);
            tvm->set_localhost(false);
            http.async_run(boost::make_shared<task_context>(), request::GET(ping_url), cb);
            REQUIRE(cb_res.wait_for(TIMEOUT) == std::future_status::ready);
            REQUIRE(cb_res.get() == make_error_code(errc::success));
            REQUIRE(request_sent);
            REQUIRE(!tvm_header);
        }
        SECTION("correct header is sent when ticket acquired")
        {
            tvm->set_ready();
            tvm->set_localhost();
            http.async_run(boost::make_shared<task_context>(), request::GET(ping_url), cb);
            REQUIRE(cb_res.wait_for(TIMEOUT) == std::future_status::ready);
            REQUIRE(cb_res.get() == make_error_code(errc::success));
            REQUIRE(request_sent);
            REQUIRE(tvm_header);
            REQUIRE(tvm_header.get() == "fake_ticket");
        }
        SECTION("request fails if tvm is not ready")
        {
            tvm->set_ready(false);
            tvm->set_localhost();
            http.async_run(boost::make_shared<task_context>(), request::GET(ping_url), cb);
            REQUIRE(cb_res.wait_for(TIMEOUT) == std::future_status::ready);
            REQUIRE(cb_res.get() == make_error_code(errc::no_service_ticket));
            REQUIRE(!request_sent);
            REQUIRE(!tvm_header);
        }
        SECTION("request fails if there's no correct ticket")
        {
            tvm->set_ready();
            tvm->set_localhost();
            tvm->set_unknown();
            http.async_run(boost::make_shared<task_context>(), request::GET(ping_url), cb);
            REQUIRE(cb_res.wait_for(TIMEOUT) == std::future_status::ready);
            REQUIRE(cb_res.get() == make_error_code(errc::no_service_ticket));
            REQUIRE(!request_sent);
            REQUIRE(!tvm_header);
        }
    }
    SECTION("tvm errors are ignored")
    {
        settings.service_ticket_provider.ignore_errors = true;
        client http(*reactor, settings);

        SECTION("no header is sent if no ticket acquired")
        {
            tvm->set_ready(false);
            tvm->set_localhost(false);
            http.async_run(boost::make_shared<task_context>(), request::GET(ping_url), cb);
            REQUIRE(cb_res.wait_for(TIMEOUT) == std::future_status::ready);
            REQUIRE(cb_res.get() == make_error_code(errc::success));
            REQUIRE(request_sent);
            REQUIRE(!tvm_header);
        }
        SECTION("correct header is sent when ticket acquired")
        {
            tvm->set_ready();
            tvm->set_localhost();
            http.async_run(boost::make_shared<task_context>(), request::GET(ping_url), cb);
            REQUIRE(cb_res.wait_for(TIMEOUT) == std::future_status::ready);
            REQUIRE(cb_res.get() == make_error_code(errc::success));
            REQUIRE(request_sent);
            REQUIRE(tvm_header);
            REQUIRE(tvm_header.get() == "fake_ticket");
        }
        SECTION("no header is sent if tvm is not ready")
        {
            tvm->set_ready(false);
            tvm->set_localhost();
            http.async_run(boost::make_shared<task_context>(), request::GET(ping_url), cb);
            REQUIRE(cb_res.wait_for(TIMEOUT) == std::future_status::ready);
            REQUIRE(cb_res.get() == make_error_code(errc::success));
            REQUIRE(request_sent);
            REQUIRE(!tvm_header);
        }
        SECTION("no header is sent if there's no correct ticket")
        {
            tvm->set_ready();
            tvm->set_localhost();
            tvm->set_unknown();
            http.async_run(boost::make_shared<task_context>(), request::GET(ping_url), cb);
            REQUIRE(cb_res.wait_for(TIMEOUT) == std::future_status::ready);
            REQUIRE(cb_res.get() == make_error_code(errc::success));
            REQUIRE(request_sent);
            REQUIRE(!tvm_header);
        }
    }
    reactor->stop();
    reactor->fini();
}
