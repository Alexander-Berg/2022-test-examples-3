#include <catch.hpp>

#include <header_parser.h>
#include <sstream>
#include <string>
#include <map>

using namespace ymod_httpclient;

struct t_parse_headers
{
    void parse(const string& raw_headers)
    {
        headers.clear();
        std::stringstream ss;
        ss << raw_headers;
        parse_headers(
            ss, [this](const string& name, const string& value) { headers[name] = value; });
    }

    typedef std::map<string, string> map_t;
    map_t headers;
};

TEST_CASE_METHOD(t_parse_headers, "parse_headers/check_ending", "headers ends with crlf or lf")
{
    parse("Host:localhost"
          "\r\n"
          "\r\n");
    REQUIRE((headers == map_t{ { "host", "localhost" } }));
    parse("Host:localhost"
          "\r\n"
          "\n");
    REQUIRE((headers == map_t{ { "host", "localhost" } }));
    parse("Host:localhost"
          "\n"
          "\n");
    REQUIRE((headers == map_t{ { "host", "localhost" } }));
    parse("Host:localhost"
          "\n"
          "\r\n");
    REQUIRE((headers == map_t{ { "host", "localhost" } }));
}

TEST_CASE_METHOD(
    t_parse_headers,
    "parse_headers/trim_spaces",
    "ignore leading/ending spaces in value")
{
    parse("Host:     localhost\r\n");
    CHECK(headers.size() == 1);
    REQUIRE(headers["host"] == "localhost");
    parse("Host:localhost    \r\n");
    CHECK(headers.size() == 1);
    REQUIRE(headers["host"] == "localhost");
    parse("Host:    localhost    \r\n");
    CHECK(headers.size() == 1);
    REQUIRE(headers["host"] == "localhost");
    parse("Host:    localhost  localhost    \r\n");
    CHECK(headers.size() == 1);
    REQUIRE(headers["host"] == "localhost  localhost");
}

TEST_CASE_METHOD(t_parse_headers, "parse_headers/check_several_headers", "")
{
    parse("Host: localhost"
          "\r\n"
          "Date: Mon, 23 May 2016 12:56:35 GMT"
          "\r\n"
          "Content-Type: text/xml"
          "\r\n");
    REQUIRE(
        (headers ==
         map_t{ { "host", "localhost" },
                { "date", "Mon, 23 May 2016 12:56:35 GMT" },
                { "content-type", "text/xml" } }));
}

TEST_CASE_METHOD(t_parse_headers, "parse_headers/empty", "")
{
    parse("\r\n");
    REQUIRE(headers.size() == 0);
    parse("\n");
    REQUIRE(headers.size() == 0);
    parse("\r\n\r\n");
    REQUIRE(headers.size() == 0);
    parse(":\r\n");
    REQUIRE((headers == map_t{ { "", "" } }));
}

TEST_CASE_METHOD(t_parse_headers, "parse_headers/unfolding", "")
{
    parse("To:  \"Joe & J. Harvey\""
          "\r\n <ddd@ Org>, JJV"
          "\r\n  @BBN"
          "\r\n");
    CHECK(headers.size() == 1);
    REQUIRE(headers["to"] == "\"Joe & J. Harvey\" <ddd@ Org>, JJV  @BBN");

    parse("To:  \"Joe & J. Harvey\""
          "\r\n <ddd@ Org>, JJV"
          "\r\n  @BBN"
          "\r\n"
          "Host: localhost\r\n");
    CHECK(headers.size() == 2);
    REQUIRE(headers["to"] == "\"Joe & J. Harvey\" <ddd@ Org>, JJV  @BBN");
    REQUIRE(headers["host"] == "localhost");
}
