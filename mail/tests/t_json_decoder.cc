#include <yxiva/core/json/decoder.h>
#include <catch.hpp>

using namespace yxiva;
using std::string;

TEST_CASE("json/decoder/get/values", "")
{
    json_value data;
    data["key1"] = 10;
    data["key2"] = "Hello, world!";
    data["key3"] = true;
    REQUIRE(json_decoder<int>::get(data["key1"], 0) == 10);
    REQUIRE(json_decoder<bool>::get(data["key3"], false) == true);
    REQUIRE(json_decoder<unsigned int>::get(data["key1"], 0U) == 10U);
    REQUIRE(json_decoder<long long int>::get(data["key1"], 0) == 10);
    REQUIRE(json_decoder<unsigned long long int>::get(data["key1"], 0U) == 10U);
    REQUIRE(json_decoder<string>::get(data["key2"], "") == data["key2"].to_string());
}

TEST_CASE("json/decoder/get/defaults", "")
{
    json_value data;
    REQUIRE(json_decoder<int>::get(data["key1"], 10) == 10);
    REQUIRE(json_decoder<bool>::get(data["key3"], false) == false);
    REQUIRE(json_decoder<unsigned int>::get(data["key1"], 10U) == 10U);
    REQUIRE(json_decoder<long long int>::get(data["key1"], 10) == 10);
    REQUIRE(json_decoder<unsigned long long int>::get(data["key1"], 10UL) == 10UL);
    REQUIRE(json_decoder<string>::get(data["key2"], "") == "");
}

TEST_CASE("json/decoder/get/path/values", "")
{
    json_value data;
    data["key1"] = 10;
    data["key2"] = "Hello, world!";
    data["key3"] = true;
    REQUIRE(json_decoder<int>::get(data, "key1", 0) == 10);
    REQUIRE(json_decoder<bool>::get(data, "key3", false) == true);
    REQUIRE(json_decoder<unsigned int>::get(data, "key1", 0U) == 10U);
    REQUIRE(json_decoder<long long int>::get(data, "key1", 0) == 10);
    REQUIRE(json_decoder<unsigned long long int>::get(data, "key1", 0U) == 10U);
    REQUIRE(json_decoder<string>::get(data, "key2", "") == data["key2"].to_string());
}

TEST_CASE("json/decoder/get/path/defaults", "")
{
    json_value data;
    data["another key"] = 10;
    REQUIRE(json_decoder<int>::get(data, "key1", 10) == 10);
    REQUIRE(json_decoder<bool>::get(data, "key3", false) == false);
    REQUIRE(json_decoder<unsigned int>::get(data, "key1", 10U) == 10U);
    REQUIRE(json_decoder<long long int>::get(data, "key1", 10) == 10);
    REQUIRE(json_decoder<unsigned long long int>::get(data, "key1", 10U) == 10U);
    REQUIRE(json_decoder<string>::get(data, "key2", "") == "");
}
