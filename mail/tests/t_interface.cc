#include <catch.hpp>
#include <ymod_httpclient/client.h>
#include <yplatform/coroutine.h>
#include <sstream>
#include <string>

using boost::system::error_code;
using namespace ymod_httpclient;

const std::string HEADER_NAME = "Host";
const std::string HEADER_VALUE = "localhost";
const std::string HEADER_STR = HEADER_NAME + ": " + HEADER_VALUE + "\r\n";
const header_dict HEADER_DICT = { { HEADER_NAME, HEADER_VALUE } };

TEST_CASE("ymod_httpclient/interface/compilation/ec_and_response_callback", "")
{
    client client;
    client.async_run(
        yplatform::task_context::fake(), request::GET("url"), [](error_code /**/, response /**/) {
        });
    REQUIRE(true);
}

TEST_CASE("ymod_httpclient/interface/request_construction/headers_as_string", "")
{
    auto request = request::GET("url", HEADER_STR);
    auto& headers = std::get<std::string>(request.headers);
    REQUIRE(headers == HEADER_STR);
}

TEST_CASE("ymod_httpclient/interface/request_construction/headers_as_C_string", "")
{
    auto request = request::GET("url", HEADER_STR.c_str());
    auto& headers = std::get<std::string>(request.headers);
    REQUIRE(headers == HEADER_STR);
}

TEST_CASE("ymod_httpclient/interface/request_construction/headers_as_dictionary", "")
{
    auto request = request::GET("url", HEADER_DICT);
    auto& headers = std::get<ymod_httpclient::header_dict>(request.headers);
    REQUIRE(headers == HEADER_DICT);
}

TEST_CASE("ymod_httpclient/interface/request_construction/headers_as_initializer_list", "")
{
    auto request = request::GET("url", { { HEADER_NAME, HEADER_VALUE } });
    auto& headers = std::get<header_dict>(request.headers);
    REQUIRE(headers == HEADER_DICT);
}

TEST_CASE("ymod_httpclient/interface/header_operations/ostream_operator_with_string_type", "")
{
    std::stringstream stream;
    request::headers_type headers = HEADER_STR;
    stream << headers;
    REQUIRE(stream.str() == HEADER_STR);
}

TEST_CASE("ymod_httpclient/interface/header_operations/ostream_operator_with_dictionary_type", "")
{
    std::stringstream stream;
    request::headers_type headers = HEADER_DICT;
    stream << headers;
    REQUIRE(stream.str() == HEADER_STR);
}

TEST_CASE("ymod_httpclient/interface/header_operations/add_to_string_initialized_headers", "")
{
    request::headers_type headers = string{};
    add(headers, HEADER_NAME, HEADER_VALUE);
    REQUIRE(std::get<std::string>(headers) == HEADER_STR);
}

TEST_CASE("ymod_httpclient/interface/header_operations/add_to_dictionary_initialized_headers", "")
{
    request::headers_type headers = header_dict{};
    add(headers, HEADER_NAME, HEADER_VALUE);
    REQUIRE(std::get<ymod_httpclient::header_dict>(headers) == HEADER_DICT);
}

#ifndef _LIBCPP_HAS_NO_COROUTINES
TEST_CASE("ymod_httpclient/interface/compilation/coroutine", "")
{
    [[maybe_unused]] auto coro = []() -> yplatform::async_void {
        client client;
        auto ctx = yplatform::task_context::fake();
        auto request = request::GET("url");
        auto [ec, response] = co_await client.async_run(ctx, request, use_awaitable);
    };
    REQUIRE(true);
}
#endif
