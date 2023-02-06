#include "../src/filter/parser/decode_filter_v1.h"
#include "../src/filter/parser/decode_filter_v2.h"
#include <yxiva/core/filter/filter_set.h>
#include <yxiva/core/filter/parse.h>
#include <yxiva/core/message.h>
#include <catch.hpp>

using namespace yxiva;

template <typename Condition = filter::condition>
void check_definition_parser_ok(
    const char* raw_defs,
    const std::string& var_name,
    const Condition& expected_condition)
{
    const json_value json_defs = json_parse_no_type_check(raw_defs);
    filter::parser::basic_definition_translator<Condition> translator;
    REQUIRE(translator(json_defs, var_name) == true);
    filter::basic_var_t<Condition> definition;
    translator.move_result_to(definition);
    REQUIRE(definition.first == var_name);
    REQUIRE(definition.second == expected_condition);
}

template <typename Condition = filter::condition>
void check_definition_parser_fail(const char* raw_defs, const std::string& var_name)
{
    const json_value json_defs = json_parse_no_type_check(raw_defs);
    filter::parser::basic_definition_translator<Condition> translator;
    REQUIRE(translator(json_defs, var_name) == false);
}

TEST_CASE("filters/parser/definition_parser/1", "ok")
{
    check_definition_parser_ok(
        R"({ "operation": { "$eq": [ "O1" ] } })",
        "A",
        { filter::message_condition_type::data_field_equals, "operation", { "O1" } });
    check_definition_parser_ok(
        R"({ "id": { "$has": ["id1","id2","id3"] } })",
        "A",
        { filter::message_condition_type::has_least_one, "id", { "id1", "id2", "id3" } });
    check_definition_parser_ok(
        R"({ "operation": { "$eq": ["O2"] } })",
        "A",
        { filter::message_condition_type::data_field_equals, "operation", { "O2" } });
    check_definition_parser_ok(
        R"({ "$has_tags": ["t1", "t2", "t3"] })",
        "A",
        { filter::message_condition_type::has_tags, "", { "t1", "t2", "t3" } });
    check_definition_parser_ok(
        R"({ "$event": ["op1", "op2", "op3"] })",
        "A",
        { filter::message_condition_type::event, "", { "op1", "op2", "op3" } });
}

TEST_CASE("filters/parser/definition_parser/1.1", "ok")
{
    using sub_cond = filter::subscription_condition;
    check_definition_parser_ok<sub_cond>(
        R"({ "platform": ["gcm", "wns"] })",
        "A", // gcm_compatibility
        { filter::subscription_condition_type::platform,
          "",
          { "gcm", "wns" } }); // gcm_compatibility
    check_definition_parser_ok<sub_cond>(
        R"({ "platform": ["fcm", "wns"] })",
        "A",
        { filter::subscription_condition_type::platform, "", { "fcm", "wns" } });
    check_definition_parser_ok<sub_cond>(
        R"({ "transport": ["mobile", "webpush"] })",
        "A",
        { filter::subscription_condition_type::transport, "", { "mobile", "webpush" } });
    check_definition_parser_ok<sub_cond>(
        R"({ "subscription_id": ["qwe", "123"] })",
        "A",
        { filter::subscription_condition_type::subscription_id, "", { "qwe", "123" } });
    check_definition_parser_ok<sub_cond>(
        R"({ "session": ["qwe", "123"] })",
        "A",
        { filter::subscription_condition_type::session, "", { "qwe", "123" } });
    check_definition_parser_ok<sub_cond>(
        R"({ "uuid": ["qwe", "123"] })",
        "A",
        { filter::subscription_condition_type::uuid, "", { "qwe", "123" } });
    check_definition_parser_ok<sub_cond>(
        R"({ "device": ["qwe", "123"] })",
        "A",
        { filter::subscription_condition_type::device, "", { "qwe", "123" } });
    check_definition_parser_ok<sub_cond>(
        R"({ "app": ["ya.maps.by", "1a2b3"] })",
        "B",
        { filter::subscription_condition_type::app, "", { "ya.maps.by", "1a2b3" } });
}

TEST_CASE("filters/parser/definition_parser/2", "empty")
{
    check_definition_parser_fail(R"({})", "A");
    check_definition_parser_fail(R"(null)", "A");
}

TEST_CASE("filters/parser/definition_parser/3", "unexpected predicate")
{
    check_definition_parser_fail(R"({ "$has": ["id1","id2","id3"] })", "A");
    check_definition_parser_fail(R"({ "$eq": ["id1","id2","id3"] })", "A");
    check_definition_parser_fail(R"({ "item": { "$has_tags": ["id1","id2","id3"] } } )", "A");
    check_definition_parser_fail(R"({ "item": { "$event": ["id1","id2","id3"] } } )", "A");
    check_definition_parser_fail(R"({ "item": { "$has": { "$has": ["id1","id2","id3"] } } })", "A");
}

TEST_CASE("filters/parser/definition_parser/4", "field name when predicate expected")
{
    check_definition_parser_fail(R"({ "operation": { "id": [ "O1" ] } })", "A");
}

TEST_CASE("filters/parser/definition_parser/4.1", "too deep")
{
    check_definition_parser_fail(
        R"({ "operation": { "$eq": { "id": { "$eq": [ "O1" ] } } } })", "A");
    check_definition_parser_fail(
        R"({ "operation": { "$eq": [ {"id": { "$eq": [ "O1" ] } } ] } })", "A");
}

TEST_CASE("filters/parser/definition_parser/5", "unknown predicate")
{
    check_definition_parser_fail(R"({ "operation": { "$make_everything_ok": [ "O1" ] } })", "A");
    check_definition_parser_fail(R"({ "$make_everything_ok": ["id1","id2","id3"] })", "A");
}

TEST_CASE("filters/parser/definition_parser/6", "no array of values")
{
    check_definition_parser_fail(R"({ "operation": { "$eq": "O1" } })", "A");
    check_definition_parser_fail(R"({ "operation": { "$eq": {} } })", "A");
    check_definition_parser_fail(R"({ "operation": { "$eq": null } })", "A");
    check_definition_parser_fail(R"({ "operation": { "$eq": [] } })", "A");
    check_definition_parser_fail(R"({ "operation": { "$eq": 10 } })", "A");
    check_definition_parser_fail(R"({ "operation": { "$eq": {"O1":"O1"} } })", "A");
}
