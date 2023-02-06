#include <catch.hpp>

#include <src/http_filter.h>

using namespace ymod_httpclient;

#define CHECK_MATCHES(s, offset, match_pos)                                                        \
    {                                                                                              \
        auto parse_result = filter((s).begin() + (offset), (s).end());                             \
        CHECK(parse_result.second);                                                                \
        CHECK(parse_result.first == (s).begin() + (match_pos));                                    \
    }

#define CHECK_DOES_NOT_MATCH(chars)                                                                \
    {                                                                                              \
        std::string ss = chars;                                                                    \
        filter.set_state(http_filter::state::headers);                                             \
        auto parse_result = filter(ss.begin(), ss.end());                                          \
        CHECK(!parse_result.second);                                                               \
        CHECK(parse_result.first == ss.end());                                                     \
    }

TEST_CASE("when in state status_line", "")
{
    http_filter filter;
    filter.set_state(http_filter::state::status_line);

    SECTION("http_filter matches newlines")
    {
        std::string s = "123\n45\n";
        CHECK_MATCHES(s, 0, 3); // "123/n"
        CHECK_MATCHES(s, 4, 6); // "45/n"
    }

    SECTION("http_filter does not match strings without newlines")
    {
        std::string s = "123456";
        CHECK_DOES_NOT_MATCH(s);
    }

    SECTION(
        "http_filter skips empty lines before first non-empty line after set_state has been called")
    {
        std::string s = "\n\n\n123\n\n\n456\n";
        CHECK_MATCHES(s, 0, 6); // "/n/n/n123/n"
        CHECK_MATCHES(s, 7, 7); // "/n"
        filter.set_state(http_filter::state::status_line);
        CHECK_MATCHES(s, 7, s.size() - 1); // "/n/n/n456\n"
    }
}

TEST_CASE("when in state headers", "")
{
    http_filter filter;
    filter.set_state(http_filter::state::headers);

    SECTION("http_filter matches \r\n\r\n")
    {
        std::string s = "one: 1\r\ntwo: 2\r\n\r\nbody";
        CHECK_MATCHES(s, 0, s.size() - 5);  // "\nbody"
        CHECK_MATCHES(s, 13, s.size() - 5); // "\r\n\r\nbody" -> "/nbody"
    }

    SECTION("http_filter does not match other \r\n combinations")
    {
        CHECK_DOES_NOT_MATCH("abcdef");
        CHECK_DOES_NOT_MATCH("\r\n \r\n");
        CHECK_DOES_NOT_MATCH("\r\n\r \n");
        // FIXME: these should not match.
        // CHECK_DOES_NOT_MATCH("\r\n\n");
        // CHECK_DOES_NOT_MATCH("\r \n\r\n");
        // CHECK_DOES_NOT_MATCH("\r\n\r\r\n");
        // CHECK_DOES_NOT_MATCH("\r\n\n\r\n");
    }
}

TEST_CASE("when in state content http_filter matches everything", "")
{
    http_filter filter;
    filter.set_state(http_filter::state::content);
    std::string s = "abcdef\r\n\r\n123456";
    CHECK_MATCHES(s, 0, s.size() - 1);
}