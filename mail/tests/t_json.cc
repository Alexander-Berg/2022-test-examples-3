#include <yplatform/json.h>
#include <yplatform/config.h>
#include <catch.hpp>

using namespace yplatform;

TEST_CASE("json/parse/ok")
{
    json_value j;
    auto error = j.parse(R"(
    {
      "key": "value",
      "arr": [1,2,3,4]
    }
  )");
    REQUIRE(!error);
}

TEST_CASE("json/parse/fail")
{
    json_value j;
    auto error = j.parse(R"(
    {
      "key": "value",
      "arr" }
  )");
    CAPTURE(j.stringify());
    REQUIRE(error);
}

TEST_CASE("json/assign")
{
    json_value val;
    val = "text1";
    REQUIRE(val.to_string() == "text1");
    val = string("text2");
    REQUIRE(val.to_string() == "text2");
    val = 5;
    REQUIRE(val.to_string() == "5");
    json_value val2 = val;
    REQUIRE(val2.to_string() == "5");
}

TEST_CASE("json/ref-assign")
{
    json_value val;
    auto subval = val["key"];
    subval = "text1";
    REQUIRE(subval.to_string() == "text1");
    subval = 5;
    REQUIRE(subval.to_string() == "5");

    val["key2"] = subval;
    REQUIRE(val["key2"].to_string() == "5");

    subval = 6;
    REQUIRE(val["key2"].to_string() == "5");
}

TEST_CASE("json/ref-move")
{
    json_value val;
    auto subval1 = val["key1"];
    auto subval2 = val["key2"];
    subval1 = "text1";
    subval2 = std::move(subval1);
    REQUIRE(subval2.to_string() == "text1");

    string key3 = "__key3";
    json_value value3;
    value3 = "text3";
    val[key3.substr(2)] = std::move(value3);
    key3.clear();
    REQUIRE(val.stringify() == "{\"key1\":null,\"key2\":\"text1\",\"key3\":\"text3\"}");
}

TEST_CASE("json/cascade_access")
{
    json_value root;

    SECTION("const")
    {
        auto subkey = root.cref["key"]["subkey"];
        REQUIRE(root.empty());
        REQUIRE(subkey.is_null());
    }

    SECTION("const + to_string")
    {
        auto subkey = root.cref["key"]["subkey"];
        REQUIRE(root.empty());
        REQUIRE(subkey.is_null());
        REQUIRE(root.to_string() == "");
    }

    SECTION("non const")
    {
        auto subkey = root["key"]["subkey"];
        REQUIRE(root.size() == 1);
        REQUIRE(subkey.is_null());
    }

    SECTION("non const with assignment")
    {
        root["key"]["subkey"] = "text";
        auto subkey = root["key"]["subkey"];
        REQUIRE(root.size() == 1);
        REQUIRE(!subkey.is_null());
        REQUIRE(subkey.to_string() == "text");
    }

    SECTION("access to not object's subkey throws")
    {
        root["key"] = "text";
        REQUIRE_THROWS(root["key"]["subkey"]);
    }

    SECTION("const access to not object's subkey returns null")
    {
        root["key"] = "text";
        REQUIRE_NOTHROW(root.cref["key"]["subkey"]);
        auto subkey = root.cref["key"]["subkey"];
        REQUIRE(subkey.is_null());
    }
}
