#include "../src/filter/parser/decode_filter_v1.h"
#include "../src/filter/parser/decode_filter_v2.h"
#include <yxiva/core/filter/filter_set.h>
#include <yxiva/core/filter/parse.h>
#include <yxiva/core/message.h>
#include <catch.hpp>

using namespace yxiva;

TEST_CASE("filters/parser/decode_values/bad_json", "")
{
    json_value vals_raw;
    std::vector<std::string> vals;
    auto parse_result = filter::parser::decode_values(vals, vals_raw);
    CHECK(!parse_result);
    REQUIRE(parse_result.error_reason == "no value array in rule");
    REQUIRE(vals.size() == 0);
}

TEST_CASE("filters/parser/decode_values/empty_array", "")
{
    json_value vals_raw(json_type::tarray);
    std::vector<std::string> vals;
    auto parse_result = filter::parser::decode_values(vals, vals_raw);
    CHECK(!parse_result);
    REQUIRE(parse_result.error_reason == "empty value array node");
    REQUIRE(vals.size() == 0);
}

TEST_CASE("filters/parser/decode_values/bad_value", "")
{
    json_value vals_raw(json_type::tarray);
    vals_raw.push_back(4);
    std::vector<std::string> vals;
    {
        auto parse_result = filter::parser::decode_values(vals, vals_raw);
        CHECK(!parse_result);
        REQUIRE(parse_result.error_reason == "not string value");
        REQUIRE(vals.size() == 0);
    }

    vals_raw[0UL] = "1234";
    vals_raw.push_back(4);
    {
        auto parse_result = filter::parser::decode_values(vals, vals_raw);
        CHECK(!parse_result);
        REQUIRE(parse_result.error_reason == "not string value");
        REQUIRE(vals.size() == 0);
    }
}

TEST_CASE("filters/parser/decode_values/ok", "")
{
    json_value vals_raw(json_type::tarray);
    vals_raw.push_back("0");
    std::vector<std::string> vals;
    {
        auto parse_result = filter::parser::decode_values(vals, vals_raw);
        CHECK(parse_result);
        REQUIRE(parse_result.error_reason == "");
        REQUIRE(vals.size() == 1);
        REQUIRE(vals[0] == "0");
    }

    const int count = 10;
    for (int i = 1; i < count; i++)
        vals_raw.push_back(std::to_string(i));
    {
        auto parse_result = filter::parser::decode_values(vals, vals_raw);
        CHECK(parse_result);
        REQUIRE(parse_result.error_reason == "");
        REQUIRE(vals.size() == count);
        for (int i = 0; i < count; i++)
        {
            REQUIRE(vals[i] == std::to_string(i));
        }
    }
}

TEST_CASE("filters/parser/decode_condition", "ok")
{
    json_value raw(json_type::tobject);
    raw["condition"] = "has_tags";
    raw["value"].push_back("id1");
    raw["value"].push_back("id2");
    filter::condition condition;
    {
        auto parse_result = filter::parser::decode_condition(condition, raw);
        CHECK(parse_result);
        REQUIRE(parse_result.error_reason == "");
    }

    raw["condition"] = "data_field_equals";
    raw["key"] = "id";
    {
        auto parse_result = filter::parser::decode_condition(condition, raw);
        CHECK(parse_result);
        REQUIRE(parse_result.error_reason == "");
    }
}

TEST_CASE("filters/parser/decode_condition/missing_type", "")
{
    json_value raw(json_type::tobject);
    filter::condition condition;
    auto parse_result = filter::parser::decode_condition(condition, raw);
    CHECK(!parse_result);
    REQUIRE(parse_result.error_reason == "no condition string in rule");
}

TEST_CASE("filters/parser/decode_condition/condition_type", "")
{
    json_value raw(json_type::tobject);
    raw["condition"] = "has_cheese";
    filter::condition condition;
    auto parse_result = filter::parser::decode_condition(condition, raw);
    CHECK(!parse_result);
    REQUIRE(parse_result.error_reason == "unknown condition");
}

TEST_CASE("filters/parser/decode_condition/key_not_needed", "")
{
    json_value raw(json_type::tobject);
    raw["condition"] = "has_tags";
    raw["value"].push_back("id1");
    raw["value"].push_back("id2");
    {
        filter::condition condition;
        auto parse_result = filter::parser::decode_condition(condition, raw);
        CHECK(parse_result);
        REQUIRE(parse_result.error_reason == "");
    }
    raw["key"] = "";
    {
        filter::condition condition;
        auto parse_result = filter::parser::decode_condition(condition, raw);
        CHECK(parse_result);
        REQUIRE(parse_result.error_reason == "");
    }
}

TEST_CASE("filters/parser/decode_condition/missing_key", "")
{
    json_value raw(json_type::tobject);
    raw["condition"] = "data_field_equals";
    {
        filter::condition condition;
        auto parse_result = filter::parser::decode_condition(condition, raw);
        CHECK(!parse_result);
        REQUIRE(parse_result.error_reason == "missing key in condition");
    }
    raw["key"] = "";
    {
        filter::condition condition;
        auto parse_result = filter::parser::decode_condition(condition, raw);
        CHECK(!parse_result);
        REQUIRE(parse_result.error_reason == "missing key in condition");
    }
}
