#include <catch.hpp>

#include <ymod_httpclient/util/url_parser.h>
#include <sstream>

using namespace ymod_httpclient;

struct parse_result
{
    string proto;
    string host;
    unsigned short port;
    string uri;

    bool operator==(const parse_result& other) const
    {
        return proto == other.proto && host == other.host && port == other.port && uri == other.uri;
    }
};

std::ostream& operator<<(std::ostream& os, parse_result const& v)
{
    os << v.proto + " " + v.host + " " + std::to_string(v.port) + " " + v.uri;
    return os;
}

parse_result parse(const string& url)
{
    parse_result res;
    auto err = parse_url(url, res.proto, res.host, res.port, res.uri);
    if (err) throw bad_url_error();
    return res;
}

TEST_CASE("parse_url/success_cases", "")
{
    CHECK(parse("ya.ru") == (parse_result{ "http", "ya.ru", 0, "/" }));
    CHECK(parse("ya.ru:80") == (parse_result{ "http", "ya.ru", 80, "/" }));
    CHECK(parse("ya.ru:443") == (parse_result{ "http", "ya.ru", 443, "/" }));
    CHECK(parse("https://ya.ru:443") == (parse_result{ "https", "ya.ru", 443, "/" }));
    CHECK(parse("https://ya.ru:443/") == (parse_result{ "https", "ya.ru", 443, "/" }));
    CHECK(parse("https://ya.ru/test") == (parse_result{ "https", "ya.ru", 0, "/test" }));
    CHECK(parse("http://ya.ru:80/test") == (parse_result{ "http", "ya.ru", 80, "/test" }));
    CHECK(parse("ya.ru/test") == (parse_result{ "http", "ya.ru", 0, "/test" }));
    CHECK(parse("ya.ru:80/test") == (parse_result{ "http", "ya.ru", 80, "/test" }));
    CHECK(
        parse("http://ya.ru/test?param=A&param=B") ==
        (parse_result{ "http", "ya.ru", 0, "/test?param=A&param=B" }));
    CHECK(
        parse("http://ya.ru?param=A&param=B") ==
        (parse_result{ "http", "ya.ru", 0, "/?param=A&param=B" }));
    CHECK(
        parse("ya.ru:80/webpush:endpoint") ==
        (parse_result{ "http", "ya.ru", 80, "/webpush:endpoint" }));
    CHECK(parse("127.0.0.1/test") == (parse_result{ "http", "127.0.0.1", 0, "/test" }));
    CHECK(parse("[::1]/test") == (parse_result{ "http", "[::1]", 0, "/test" }));
    CHECK(
        parse("[::ffff:192.0.2.1]/test") ==
        (parse_result{ "http", "[::ffff:192.0.2.1]", 0, "/test" }));
}

TEST_CASE("parse_url/fail_cases", "")
{
    CHECK_THROWS(parse("localhost:8080path"));
}

TEST_CASE("parse_url/border_cases", "")
{
    CHECK(parse("https://ya.ru?test") == (parse_result{ "https", "ya.ru", 0, "/?test" }));
}
