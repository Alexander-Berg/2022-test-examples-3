#include <iostream>
#include <sstream>
#include <parser/request.h>
#include <validator.h>
#include "init_log.h"

using std::string;

#ifndef CATCH_CONFIG_MAIN
#define CATCH_CONFIG_MAIN
#endif
#include <catch.hpp>

class t_request
{
public:
    t_request() : default_ctx(new ymod_webserver::context())
    {
        default_ctx->local_port = 80;
        parser.reset(default_ctx);
    }

    void parse(const string& REQ)
    {
        string::const_iterator i_start = REQ.begin();
        try
        {
            parser(REQ.begin(), i_start, REQ.end());
        }
        catch (ymod_webserver::parse_error& error)
        {
            INFO(error.public_message() + ":\n\t" + error.private_message());
            throw error;
        }
    }

    void reset()
    {
        parser.reset(default_ctx);
    }

    ymod_webserver::context_ptr default_ctx;
    ymod_webserver::parser::request_parser<string::const_iterator> parser;
};

TEST_CASE_METHOD(t_request, "parser/request/options", "")
{
    parse("OPTIONS /api/ping?par1=arg1&par2=arg2;par3&par4;par5=1 HTTP/15.143\r\nHost: "
          "http://mail.yan\n\tde\r\n x.ru\r\n\r\n");
    ymod_webserver::validator v;
    v(ymod_webserver::endpoint(), parser.req());

    REQUIRE(parser.is_finished());
    REQUIRE(parser.req()->vhost.proto == "http");
    REQUIRE(parser.req()->vhost.domain == "mail.yandex.ru");
    REQUIRE(
        parser.req()->raw_request_line ==
        "OPTIONS /api/ping?par1=arg1&par2=arg2;par3&par4;par5=1 HTTP/15.143");
}

TEST_CASE_METHOD(t_request, "parser/request/post/1", "")
{
    parse(
        "POST /neo2/handlers/handlers.xml HTTP/1.1\r\nHost: mail.yandex.ua\r\nUser-Agent: "
        "Mozilla/5.0 (Windows; U; Windows NT 5.1; ru; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 "
        "YB/4.3.0\r\nAccept: */*\r\nAccept-Language: "
        "ru-ru,ru;q=0.8,en-us;q=0.5,en;q=0.3\r\nAccept-Encoding: gzip,deflate\r\nAccept-Charset: "
        "windows-1251,utf-8;q=0.7,*;q=0.7\r\nKeep-Alive: 115\r\nConnection: TE, "
        "keep-alive\r\nContent-Type: application/x-www-form-urlencoded; "
        "charset=UTF-8\r\nX-Requested-With: XMLHttpRequest\r\nReferer: "
        "http://mail.yandex.ua/neo2/\r\nContent-Length: 58\r\nCookie: "
        "fuid01=4b974c5901be107b.VKZWm2wm5DO_6zaMN-"
        "6xCHTt3U7v27c6foUtHMCMO847e986g9O6a5Ib8vmb56ngKN1xwqi-jznatLNdGFfxgjxngXS5-"
        "HCcasieCRsfPrOcwCxN0OKrlneqNU3kIRQx; yabs-frequency=/3/phtO0Fm5BW00/; "
        "Session_id=1295443025.-3198711.2.30172036.2:74122593:340.8:1295441517231:1602552190:122."
        "yandex_ua:39131.4169.99ca1a7543613670ca39468815106a52; my=YwECAAAnAgABAA==; "
        "yandexuid=6851881921271666725; "
        "L=ZWIEOGBbZ0QDckkGUlpGfABtbGN0AFhWLR9MAhN8MkYUOl0GHTgWHQsHPwUPXBxDQg0EKAN2AXZSBRlNBz8oVw=="
        ".1295441517.8767.239384.235774fd9b7ed25d5c61950b2bcd3894; t=f; yandex_login=kvnkivlove; "
        "yandex_mail=kvnkivlove\r\nPragma: no-cache\r\nCache-Control: no-cache\r\n\r\n");
    ymod_webserver::validator v;
    v(ymod_webserver::endpoint(), parser.req());

    REQUIRE(parser.is_finished());
    REQUIRE(parser.req()->raw_request_line == "POST /neo2/handlers/handlers.xml HTTP/1.1");
    REQUIRE(parser.req()->connection == ymod_webserver::connection_keep_alive);
    REQUIRE(parser.req()->content.type == "application");
    REQUIRE(parser.req()->content.subtype == "x-www-form-urlencoded");
    REQUIRE(parser.req()->content.length == 58);
}

TEST_CASE_METHOD(t_request, "parser/request/post/2", "")
{
    parse("POST /neo2/handlers/handlers.xml HTTP/1.1\nHost: mail.yandex.ua\r\nUser-Agent: "
          "Mozilla/5.0 (Windows; U; Windows NT 5.1; ru; rv:1.9.2.13) Gecko/20101203 Firefox/3.6.13 "
          "YB/4.3.0\r\nAccept: */*\r\nAccept-Language: "
          "ru-ru,ru;q=0.8,en-us;q=0.5,en;q=0.3\r\nAccept-Encoding: gzip,deflate\r\nAccept-Charset: "
          "windows-1251,utf-8;q=0.7,*;q=0.7\r\nKeep-Alive: 115\r\nConnection: TE, "
          "keep-alive\r\nX-Requested-With: XMLHttpRequest\r\nReferer: "
          "http://mail.yandex.ua/neo2/\r\nTransfer-Encoding: chunked\r\nContent-Type: "
          "multipart/form-data; boundary=\"----------E2DFPtMIMX4lbjHoiEJ2M9\"\r\nPragma: "
          "no-cache\r\nCache-Control: no-cache\r\n\r\n");
    ymod_webserver::validator v;
    v(ymod_webserver::endpoint(), parser.req());

    REQUIRE(parser.is_finished());
    REQUIRE(parser.req()->raw_request_line == "POST /neo2/handlers/handlers.xml HTTP/1.1");
    REQUIRE(parser.req()->connection == ymod_webserver::connection_keep_alive);
    REQUIRE(parser.req()->content.type == "multipart");
    REQUIRE(parser.req()->content.subtype == "form-data");
    REQUIRE(parser.req()->content.boundary == "----------E2DFPtMIMX4lbjHoiEJ2M9");
    REQUIRE(parser.req()->transfer_encoding == ymod_webserver::transfer_encoding_chunked);
}

TEST_CASE_METHOD(t_request, "parser/request/get/1", "")
{
    parse("GET https://mail.yandex.net:80/\r\n\r\n");

    REQUIRE(parser.is_finished());
    REQUIRE(parser.req()->url.host.proto == "https");
    REQUIRE(parser.req()->url.host.domain == "mail.yandex.net");
    REQUIRE(parser.req()->url.host.port == 80);
}

TEST_CASE_METHOD(t_request, "parser/request/get/2", "")
{
    parse("GET https:///\r\n\r\n");
    REQUIRE(parser.is_finished());
}

TEST_CASE_METHOD(t_request, "parser/request/spaces/wrong_request", "")
{
    CHECK_THROWS(parse("  GET\r\n\r\n"));
    reset();
    CHECK_THROWS(parse("\tGET\r\n\r\n"));
    reset();
    CHECK_THROWS(parse("\nGET\r\n\r\n"));
    reset();
    CHECK_THROWS(parse("\rGET\r\n\r\n"));
    reset();
    CHECK_THROWS(parse("GET\r\r\n\r\n"));
    reset();
    CHECK_THROWS(parse("GET\n\r\n\r\n"));
    reset();
    CHECK_THROWS(parse("GET\t\r\n\r\n"));
    reset();
    CHECK_THROWS(parse("GET \n\r\n\r\n"));
    reset();
    CHECK_THROWS(parse("GET \t\r\n\r\n"));
    reset();
    CHECK_THROWS(parse("GET  \n\r\n\r\n"));
    reset();
    CHECK_THROWS(parse("GET  \t\r\n\r\n"));
    reset();
    CHECK_THROWS(parse("GET   \n\r\n\r\n"));
    reset();
    CHECK_THROWS(parse("GET   \t\r\n\r\n"));
}

TEST_CASE_METHOD(t_request, "parser/request/spaces/simple_request", "")
{
    parse("GET /ping\r\n\r\n");
    CHECK(parser.is_finished());
    REQUIRE(parser.req()->url.path.size() == 1);
    REQUIRE(parser.req()->url.path[0] == "ping");

    reset();
    parse("GET    /ping       \r\n\r\n");
    CHECK(parser.is_finished());
    REQUIRE(parser.req()->url.path.size() == 1);
    REQUIRE(parser.req()->url.path[0] == "ping");
}

TEST_CASE_METHOD(t_request, "parser/request/spaces/request_line", "")
{
    parse("GET /ping HTTP/1.1\r\n\r\n");
    CHECK(parser.is_finished());
    REQUIRE(parser.req()->url.path.size() == 1);
    REQUIRE(parser.req()->url.path[0] == "ping");

    reset();
    try
    {
        parse("GET    /ping      HTTP/1.1\r\n\r\n");
    }
    catch (ymod_webserver::parse_error const& error)
    {
        std::cout << error.public_message() << ":\n\t" << error.private_message();
    }
    CHECK(parser.is_finished());
    REQUIRE(parser.req()->url.path.size() == 1);
    REQUIRE(parser.req()->url.path[0] == "ping");

    reset();
    // actually spaces before \r\n are not allowed, but why not
    // (nginx also allows this)
    parse("GET    /ping      HTTP/1.1       \r\n\r\n");
    CHECK(parser.is_finished());
    REQUIRE(parser.req()->url.path.size() == 1);
    REQUIRE(parser.req()->url.path[0] == "ping");
}

TEST_CASE_METHOD(t_request, "parser/request/iterators/on_start", "")
{
    const string REQ = "123";

    string::const_iterator i_start = REQ.end();
    string::const_iterator begin = REQ.end();
    string::const_iterator end = REQ.end();
    auto result_iter = parser(begin, i_start, end);
    CHECK((result_iter - end) == 0);
    REQUIRE((i_start - end) == 0);

    i_start = REQ.end();
    begin = REQ.begin();
    end = REQ.end();
    result_iter = parser(begin, i_start, end);
    CHECK((result_iter - end) == 0);
    REQUIRE((i_start - end) == 0);
}

TEST_CASE_METHOD(t_request, "parser/request/iterators/on_method", "")
{
    const string REQ = "\r\n";

    string::const_iterator i_start = REQ.begin();
    string::const_iterator begin = REQ.begin();
    string::const_iterator end = REQ.end();
    auto result_iter = parser(begin, i_start, end);
    CHECK((result_iter - end) == 0);
    REQUIRE((i_start - end) == 0);

    begin = result_iter;
    result_iter = parser(begin, i_start, end);
    CHECK((result_iter - end) == 0);
    REQUIRE((i_start - end) == 0);
}

TEST_CASE_METHOD(t_request, "parser/request/iterators/on_policy_request", "")
{
    const string REQ = "<";

    string::const_iterator i_start = REQ.begin();
    string::const_iterator begin = REQ.begin();
    string::const_iterator end = REQ.end();
    auto result_iter = parser(begin, i_start, end);
    CHECK((result_iter - i_start) == 0);
    REQUIRE((i_start - end) == 0);

    begin = result_iter;
    result_iter = parser(begin, i_start, end);
    CHECK((result_iter - begin) == 0);
    REQUIRE((i_start - end) == 0);
}

TEST_CASE_METHOD(t_request, "parser/request/invalid_uri", "")
{
    CHECK_THROWS(parse("GET http:\r\n"));
    reset();
    CHECK_THROWS(parse("GET http:/\r\n"));
    reset();
    CHECK_THROWS(parse("GET http:|\r\n"));
}

TEST_CASE_METHOD(t_request, "parser/request/invalid_method", "")
{
    CHECK_THROWS(parse("PET / HTTP/1.1\r\n"));
}

TEST_CASE_METHOD(t_request, "parser/request/invalid_request_end", "")
{
    CHECK_THROWS(parse("GET / HTTP/1.1\rHost: mail.yandex.ua\r\n"));
}
