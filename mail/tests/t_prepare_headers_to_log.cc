#include "prepare_headers_to_log.h"
#include <catch.hpp>

const std::unordered_set<std::string> HEADERS_TO_PROTECT = { "protected" };

std::string prepare(const std::string& headers)
{
    return ymod_httpclient::prepare_headers_to_log(headers, HEADERS_TO_PROTECT);
}

TEST_CASE("prepare_headers_to_log/empty_string")
{
    REQUIRE(prepare("") == "");
}

TEST_CASE("prepare_headers_to_log/replace_tskv_specials")
{
    REQUIRE(prepare("header: value\r\n") == "header: value<CR><LF>");
}

TEST_CASE("prepare_headers_to_log/convert_header_names_to_lower_case")
{
    REQUIRE(prepare("HEADER: value\r\n") == "header: value<CR><LF>");
}

TEST_CASE("prepare_headers_to_log/dont_change_header_values_case")
{
    REQUIRE(prepare("header: VALUE\r\n") == "header: VALUE<CR><LF>");
}

TEST_CASE("prepare_headers_to_log/collapse_multiline_values")
{
    REQUIRE(prepare("header: multiline\r\n value\r\n") == "header: multiline value<CR><LF>");
}

TEST_CASE("prepare_headers_to_log/protect_headers")
{
    REQUIRE(prepare("protected: value\r\n") == "protected: xxxxx<CR><LF>");
}

TEST_CASE("prepare_headers_to_log/protect_multiline_values")
{
    REQUIRE(prepare("protected: multiline\r\n value\r\n") == "protected: xxxxxxxxxxxxxxx<CR><LF>");
}
