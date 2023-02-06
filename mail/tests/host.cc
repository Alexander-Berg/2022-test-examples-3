#include <ymod_webserver/request.h>
#include <parser/uri.h>
#include "zerocopy_wrapper.h"

#ifndef CATCH_CONFIG_MAIN
#define CATCH_CONFIG_MAIN
#endif
#include <catch.hpp>

using std::string;

struct host_test
{
    string host;
    string expected_domain;
    int expected_port;
};

TEST_CASE("parser/uri/parse_host", "")
{
    std::vector<host_test> tests = {
        { "xiva-daria-v6.mail.yandex.net", "xiva-daria-v6.mail.yandex.net", 80 },
        { "xiva.mail.yandex.net:443", "xiva.mail.yandex.net", 443 },
        { "localhost:1080", "localhost", 1080 },
        { "82.142.178.150", "82.142.178.150", 80 },
        { "127.0.0.1:1080", "127.0.0.1", 1080 }
    };

    zerocopy_wrapper zc_wrap;

    for (auto test = tests.begin(); test != tests.end(); test++)
    {
        ymod_webserver::request req;
        zc_wrap.reset(100);
        zc_wrap.fill_buffers(test->host + "494994949", test->host.length());
        auto result_iter = ymod_webserver::parser::parse_host(
            zc_wrap.buffer->begin(), zc_wrap.buffer->end(), req.vhost);
        CHECK((result_iter - zc_wrap.buffer->end()) == 0);
        CHECK(test->expected_domain == req.vhost.domain);
        CHECK(test->expected_port == req.vhost.port);
    }
}

TEST_CASE("parser/uri/parse_host/wrong/1", "")
{
    string uri = "http:";
    ymod_webserver::request req;
    CHECK_THROWS(ymod_webserver::parser::parse_host(uri.begin(), uri.end(), req.vhost));
}

TEST_CASE("parser/uri/parse_host/wrong/2", "")
{
    string uri = "http:/";
    ymod_webserver::request req;
    CHECK_THROWS(ymod_webserver::parser::parse_host(uri.begin(), uri.end(), req.vhost));
}

TEST_CASE("parser/uri/parse_host/wrong/3", "")
{
    string uri = "http://localhost:100500";
    ymod_webserver::request req;
    CHECK_NOTHROW(ymod_webserver::parser::parse_host(uri.begin(), uri.end() - 6, req.vhost));
    CHECK(req.vhost.domain == "localhost");
    CHECK(req.vhost.port == 80);
}

TEST_CASE("parser/uri/parse_host/wrong/4", "")
{
    string uri = "";
    ymod_webserver::request req;
    CHECK_NOTHROW(ymod_webserver::parser::parse_host(uri.begin(), uri.end(), req.vhost));
    CHECK(req.vhost.domain == "");
    CHECK(req.vhost.port == 80);
}

TEST_CASE("parser/uri/parse_host/wrong/5", "not slash after colon")
{
    string uri = "http:|";
    ymod_webserver::request req;
    CHECK_THROWS(ymod_webserver::parser::parse_host(uri.begin(), uri.end(), req.vhost));
}
