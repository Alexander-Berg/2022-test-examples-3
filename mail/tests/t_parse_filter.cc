#include "../src/filter/parser/decode_filter_v1.h"
#include "../src/filter/parser/decode_filter_v2.h"
#include <yxiva/core/filter/filter_set.h>
#include <yxiva/core/filter/parse.h>
#include <yxiva/core/message.h>
#include <catch.hpp>

using namespace yxiva;

TEST_CASE("filters/parser/old_json/ok/SeveralRulesAndDefaultAction", "")
{
    string jsfilter = R"({
      "rules": [
      { "condition": "has_tags", "action": "skip", "key" : "", "value": ["tagA", "tagB"] },
      { "condition": "has_tags", "action": "skip", "key" : "", "value": ["tagC", "tagD"] }
      ],
      "default_action": "skip"
      })";
    filter_set fset;
    auto parse_result = filter::parse(fset, jsfilter);
    CHECK(parse_result);
    REQUIRE(parse_result.error_reason == "");

    filter::rules_t expected_rules = { { filter::action::skip, { {}, { "1" } } },
                                       { filter::action::skip, { {}, { "2" } } } };
    REQUIRE(fset.get_rules() == expected_rules);

    auto has_tags = filter::message_condition_type::has_tags;
    filter::vars_t expected_vars = { { "1", { has_tags, "", { "tagA", "tagB" } } },
                                     { "2", { has_tags, "", { "tagC", "tagD" } } } };
    REQUIRE(fset.get_vars() == expected_vars);

    REQUIRE(fset.get_default() == filter::action::skip);
}

TEST_CASE("filters/parser/old_json/emptyRules", "success")
{
    string jsfilter = R"({
      "rules": []
      })";
    filter_set fset;
    auto parse_result = filter::parse(fset, jsfilter);
    CHECK(!parse_result);
    REQUIRE(parse_result.error_reason == "empty rules array node");
    REQUIRE(fset.get_default() == filter::action::send_bright);
}

TEST_CASE("filters/parser/old_json/oneRule", "success")
{
    string jsfilter = R"({
      "rules": [
      { "condition": "has_tags", "action": "skip", "key" : "", "value": ["tagA", "tagB"] }
      ]
      })";
    filter_set fset;
    auto parse_result = filter::parse(fset, jsfilter);
    CHECK(parse_result);
    REQUIRE(parse_result.error_reason == "");

    filter::rules_t expected_rules = { { filter::action::skip, { {}, { "1" } } } };
    REQUIRE(fset.get_rules() == expected_rules);

    auto has_tags = filter::message_condition_type::has_tags;
    filter::vars_t expected_vars = { { "1", { has_tags, "", { "tagA", "tagB" } } } };
    REQUIRE(fset.get_vars() == expected_vars);

    REQUIRE(fset.get_default() == filter::action::send_bright);
}

TEST_CASE(
    "filters/parser/old_json/invalidJson",
    "failure on empty or unexpected json type or invalid json")
{
    filter_set fset;
    REQUIRE(!filter::parse(fset, R"({})"));
    REQUIRE(!filter::parse(fset, R"([])"));
    REQUIRE(!filter::parse(fset, R"("")"));
    REQUIRE(!filter::parse(fset, R"("123")"));
    REQUIRE(!filter::parse(fset, R"(123)"));
}

TEST_CASE("filters/parser/json_v2/ok", "")
{
    filter_set fset;
    const char* test_str = R"({
   "vars": {
     "A": { "operation": { "$eq": [ "O1" ] } },
     "B": { "id": { "$has": ["id1","id2","id3"] } },
     "C": { "operation": { "$eq": ["O2"] } },
     "D": { "$has_tags": ["t1", "t2", "t3"] }
   },
  "rules": [
     { "if": "A & B", "do": "send_bright" },
     { "if": "D & !C", "do": "send_silent" },
     { "do": "skip" }
  ]})";
    REQUIRE(filter::parse(fset, test_str).error_reason == "");
    REQUIRE(filter::parse(fset, test_str));

    using filter::OP_NOT;
    using filter::OP_AND;
    filter::rules_t expected_rules = {
        { filter::action::send_bright, { { OP_AND }, { "A", "B" } } },
        { filter::action::send_silent, { { OP_AND, OP_NOT }, { "D", "C" } } }
    };
    REQUIRE(fset.get_rules() == expected_rules);

    auto has_tags = filter::message_condition_type::has_tags;
    auto eq = filter::message_condition_type::data_field_equals;
    auto has_least_one = filter::message_condition_type::has_least_one;
    filter::vars_t expected_vars = { { "A", { eq, "operation", { "O1" } } },
                                     { "B", { has_least_one, "id", { "id1", "id2", "id3" } } },
                                     { "C", { eq, "operation", { "O2" } } },
                                     { "D", { has_tags, "", { "t1", "t2", "t3" } } } };
    REQUIRE(fset.get_vars() == expected_vars);

    REQUIRE(fset.get_default() == filter::action::skip);
}

TEST_CASE("filters/parser/json_v2/ok/NoDefaultAction", "")
{
    filter_set fset;
    const char* test_str = R"({
   "vars": {
     "A": { "operation": { "$eq": [ "O1" ] } },
     "B": { "id": { "$has": ["id1","id2","id3"] } },
     "C": { "operation": { "$eq": ["O2"] } },
     "D": { "$has_tags": ["t1", "t2", "t3"] }
   },
  "rules": [
     { "if": "A & B", "do": "send_bright" },
     { "if": "D & !C", "do": "send_silent" }
   ]
  })";
    REQUIRE(filter::parse(fset, test_str).error_reason == "");
    REQUIRE(filter::parse(fset, test_str));
    REQUIRE(fset.get_default() == filter::action::send_bright);
}

TEST_CASE("filters/parser/json_v2/ok/OnlyDefaultRule", "")
{
    filter_set fset;
    const char* test_str1 = R"({
   "vars": {
     "A": { "operation": { "$eq": [ "O1" ] } },
     "B": { "id": { "$has": ["id1","id2","id3"] } },
     "C": { "operation": { "$eq": ["O2"] } },
     "D": { "$has_tags": ["t1", "t2", "t3"] }
   },
  "rules": [ { "do": "skip" } ] })";
    REQUIRE(filter::parse(fset, test_str1).error_reason == "");
    REQUIRE(filter::parse(fset, test_str1));
    REQUIRE(fset.get_default() == filter::action::skip);
    REQUIRE(fset.get_vars().empty());
    REQUIRE(fset.get_rules().empty());

    const char* test_str2 = R"({
   "vars": { "A": { "operation": { "$eq": [ "O1" ] } } },
   "rules": [ { "do": "skip" } ] })";
    REQUIRE(filter::parse(fset, test_str2).error_reason == "");
    REQUIRE(filter::parse(fset, test_str2));
    REQUIRE(fset.get_default() == filter::action::skip);
    REQUIRE(fset.get_vars().empty());
    REQUIRE(fset.get_rules().empty());

    REQUIRE(filter::parse_v2(fset, R"({ "rules": [ { "do": "skip", "if": "" } ] })"));
    REQUIRE(fset.get_default() == filter::action::skip);
    REQUIRE(fset.get_rules().empty());

    REQUIRE(filter::parse_v2(fset, R"({ "rules": [ { "do": "skip", "if": null } ] })"));
    REQUIRE(fset.get_default() == filter::action::skip);
    REQUIRE(fset.get_rules().empty());
}

TEST_CASE("filters/parser/json_v2/ok/EmptyVariables", "")
{
    filter_set fset;
    const char* test_str = R"( { "rules": [ { "do": "skip" } ] })";
    REQUIRE(filter::parse_v2(fset, test_str));
    const char* test_str2 = R"( { "vars": {}, "rules": [ { "do": "skip" } ] })";
    REQUIRE(filter::parse_v2(fset, test_str2));
}

TEST_CASE("filters/parser/json_v2/fail/RulesMustBeArray", "")
{
    filter_set fset;
    const char* test_str1 = R"( {
    "vars": {
      "A": { "operation": { "$eq": [ "O1" ] } },
      "B": { "id": { "$has": ["id1","id2","id3"] } },
      "C": { "operation": { "$eq": ["O2"] } },
      "D": { "$has_tags": ["t1", "t2", "t3"] }
    } })";
    REQUIRE(!filter::parse(fset, test_str1));

    const char* test_str2 = R"( {
    "vars": {
      "A": { "operation": { "$eq": [ "O1" ] } },
      "B": { "id": { "$has": ["id1","id2","id3"] } },
      "C": { "operation": { "$eq": ["O2"] } },
      "D": { "$has_tags": ["t1", "t2", "t3"] }
    },
    "rules": {} })";
    REQUIRE(!filter::parse(fset, test_str2));
}

TEST_CASE("filters/parser/json_v2/fail/NoRulesAnyVars", "")
{
    filter_set fset;
    const char* test_str2 = R"({
    "vars": {
      "A": { "operation": { "$eq": [ "O1" ] } },
      "B": { "id": { "$has": ["id1","id2","id3"] } },
      "C": { "operation": { "$eq": ["O2"] } },
      "D": { "$has_tags": ["t1", "t2", "t3"] }
    },
    "rules": []
  })";
    REQUIRE(!filter::parse(fset, test_str2));
}

TEST_CASE("filters/parser/json_v2/fail/UnknownVariable", "")
{
    filter_set fset;
    const char* test_str = R"({
   "vars": {
     "A": { "operation": { "$eq": [ "O1" ] } }
   },
  "rules": [
     { "if": "D & !C", "do": "send_silent" },
     { "do": "skip" }
   ]})";
    REQUIRE(!filter::parse(fset, test_str));

    const char* test_str2 = R"({
   "vars": null,
   "rules": [
      { "if": "D & !C", "do": "send_silent" },
      { "do": "skip" }
    ]})";
    REQUIRE(!filter::parse(fset, test_str2));

    const char* test_str3 = R"({
   "rules": [
      { "if": "D & !C", "do": "send_silent" },
      { "do": "skip" }
    ]})";
    REQUIRE(!filter::parse(fset, test_str3));
}

TEST_CASE("filters/parser/json_v2/fail/InvalidRule", "")
{
    filter_set fset;
    const char* test_str1 = R"({
   "vars": {
     "A": { "operation": { "$eq": [ "O1" ] } }
   },
  "rules": [
     { "smth": 123 },
     { "do": "skip" }
   ]})";
    REQUIRE(!filter::parse(fset, test_str1));

    const char* test_str2 = R"({
    "vars": {
      "A": { "operation": { "$eq": [ "O1" ] } }
    },
    "rules": [
      { "do": { "do": "send_bright" } },
      { "do": "skip" }
    ] })";
    REQUIRE(!filter::parse(fset, test_str2));

    const char* test_str3 = R"({
     "vars": {
       "A": { "operation": { "$eq": [ "O1" ] } }
     },
    "rules": [
       { "do": "send_bright", "if": { "$eq": ["1", "2"] }},
       { "do": "skip" }
     ] })";
    REQUIRE(!filter::parse(fset, test_str3));
}

TEST_CASE("filters/parser/fail/DefaultFormatV1Expected", "")
{
    filter_set fset;
    const char* test_str = R"( { "rules": [ { "do": "skip" } ] })";
    REQUIRE(!filter::parse(fset, test_str));
    REQUIRE(!filter::parse(fset, R"({ "rules": [] })"));
    REQUIRE(!filter::parse(fset, R"({ "rules": null })"));
}

TEST_CASE("filters/parser/json_v2/fail/OldRulesFormatUnacceptable", "")
{
    string jsfilter = R"({
      "rules": [
      { "condition": "has_tags", "action": "skip", "key" : "", "value": ["tagA", "tagB"] },
      { "condition": "has_tags", "action": "skip", "key" : "", "value": ["tagC", "tagD"] }
      ],
      "default_action": "skip"
      })";
    filter_set fset;
    REQUIRE(!filter::parse_v2(fset, jsfilter));
}

TEST_CASE("filters/parser/json_v2/fail/EmptyRules", "")
{
    filter_set fset;
    REQUIRE(!filter::parse_v2(fset, R"({ "rules": [] })"));
    REQUIRE(!filter::parse_v2(fset, R"({ "rules": null })"));
}

TEST_CASE("filters/parser/json_v2/ok/EmbeddedOperandInRule", "")
{
    const char* test_str = R"({
    "rules": [
       { "if": { "operation": { "$eq": [ "O1" ] } }, "do": "send_bright" },
       { "if": { "$has_tags": ["t1", "t2", "t3"] }, "do": "send_silent" },
       { "do": "skip" }
     ]
   })";
    filter_set fset;
    REQUIRE(filter::parse_v2(fset, test_str));

    filter::rules_t expected_rules = { { filter::action::send_bright, { {}, { "1" } } },
                                       { filter::action::send_silent, { {}, { "2" } } } };
    REQUIRE(fset.get_rules() == expected_rules);

    auto data_field_equals = filter::message_condition_type::data_field_equals;
    auto has_tags = filter::message_condition_type::has_tags;
    filter::vars_t expected_vars = { { "1", { data_field_equals, "operation", { "O1" } } },
                                     { "2", { has_tags, "", { "t1", "t2", "t3" } } } };
    REQUIRE(fset.get_vars() == expected_vars);

    REQUIRE(fset.get_default() == filter::action::skip);
}

TEST_CASE("filters/parser/json_v2/ok/EmbeddedOperandInRuleAndRuleReferencingVariable", "")
{
    const char* test_str = R"({
    "vars": {
      "A": { "$has_tags": ["t1"] }
    },
    "rules": [
       { "if": { "operation": { "$eq": [ "O1" ] } }, "do": "send_bright" },
       { "if": "A", "do": "send_silent" },
       { "do": "skip" }
     ]
   })";
    filter_set fset;
    CHECK(filter::parse_v2(fset, test_str).error_reason == "");
    REQUIRE(filter::parse_v2(fset, test_str));

    filter::rules_t expected_rules = { { filter::action::send_bright, { {}, { "1" } } },
                                       { filter::action::send_silent, { {}, { "A" } } } };
    REQUIRE(fset.get_rules() == expected_rules);

    auto data_field_equals = filter::message_condition_type::data_field_equals;
    auto has_tags = filter::message_condition_type::has_tags;
    filter::vars_t expected_vars = { { "1", { data_field_equals, "operation", { "O1" } } },
                                     { "A", { has_tags, "", { "t1" } } } };
    REQUIRE(fset.get_vars() == expected_vars);

    REQUIRE(fset.get_default() == filter::action::skip);
}

TEST_CASE("filters/parser/decode_condition/ok/values_are_sorted", "")
{
    std::vector<const char*> test_strings{ R"({"transport": ["abc", "def", "ghi"]})",
                                           R"({"platform": ["789", "456", "123"]})",
                                           R"({"device": ["bbb", "aaa", "ccc"]})",
                                           R"({"app": ["ya.mail.ru", "aaa", "abc"]})" };

    for (auto& test_str : test_strings)
    {
        json_value test_json;
        filter::subscription_condition cond;
        REQUIRE(json_parse(test_json, test_str).error_reason == "");
        REQUIRE(
            filter::decode_condition<filter::subscription_condition>(cond, test_json)
                .error_reason == "");
        REQUIRE(std::is_sorted(cond.value.begin(), cond.value.end()));
    }
}

TEST_CASE("filters/parser/parse_and_apply/bright_by_default", "")
{
    sub_t subscription;
    message message;

    REQUIRE(filter::parse_and_apply(subscription, message) == filter::action::send_bright);
    filter::action action;
    REQUIRE(filter::parse_and_apply(action, subscription, message).error_reason == "");
    REQUIRE(action == filter::action::send_bright);
}

TEST_CASE("filters/parser/parse_and_apply/parse_fail_behavior", "")
{
    sub_t subscription;
    subscription.filter = "fail";
    message message;

    REQUIRE(filter::parse_and_apply(subscription, message) == filter::action::send_bright);
    filter::action action;
    REQUIRE_FALSE(filter::parse_and_apply(action, subscription, message).error_reason == "");
}

TEST_CASE("filters/parser/parse_and_apply/apply_filter_action", "")
{
    sub_t subscription;
    subscription.filter = R"({"rules": [{"do": "send_silent"}], "vars": {}})";
    message message;

    REQUIRE(filter::parse_and_apply(subscription, message) == filter::action::send_silent);
    filter::action action;
    REQUIRE(filter::parse_and_apply(action, subscription, message).error_reason == "");
    REQUIRE(action == filter::action::send_silent);
}

TEST_CASE("filters/parser/parse_and_apply/chain_actions", "")
{
    using cond_t = filter::subscription_condition_type;
    sub_t subscription;
    message message;

    message.subscription_matchers.push_back({ cond_t::subscription_id, "", { "test" } });
    message.subscription_matchers.push_back({ cond_t::session, "", { "test" } });
    message.subscription_matchers.push_back({ cond_t::transport, "", { "mobile" } });
    message.subscription_matchers.push_back({ cond_t::app, "", { "ya.test.ru" } });
    REQUIRE(filter::parse_and_apply(subscription, message) == filter::action::skip);
    subscription.id = "test";
    REQUIRE(filter::parse_and_apply(subscription, message) == filter::action::skip);
    subscription.session_key = "test";
    REQUIRE(filter::parse_and_apply(subscription, message) == filter::action::skip);
    subscription.callback_url = "xivamob:some.other.app/SOM4E3TO2KE1N";
    REQUIRE(filter::parse_and_apply(subscription, message) == filter::action::skip);
    subscription.callback_url = "xivamob:ya.test.ru/123453";
    REQUIRE(filter::parse_and_apply(subscription, message) == filter::action::send_bright);
}

TEST_CASE("filters/parser/parse_and_apply/subscription_transport", "")
{
    using cond_t = filter::subscription_condition_type;
    sub_t subscription;
    message message;

    SECTION("mobile")
    {
        message.subscription_matchers.push_back({ cond_t::transport, "", { "mobile" } });
        subscription.callback_url = "xivamob:xxx";
        REQUIRE(filter::parse_and_apply(subscription, message) == filter::action::send_bright);
        subscription.callback_url = "apnsqueue:xxx";
        REQUIRE(filter::parse_and_apply(subscription, message) == filter::action::send_bright);
        subscription.callback_url = "http:xxx";
        REQUIRE(filter::parse_and_apply(subscription, message) == filter::action::skip);
    }

    SECTION("http")
    {
        message.subscription_matchers.push_back({ cond_t::transport, "", { "http" } });
        subscription.callback_url = "xivamob:xxx";
        REQUIRE(filter::parse_and_apply(subscription, message) == filter::action::skip);
        subscription.callback_url = "apnsqueue:xxx";
        REQUIRE(filter::parse_and_apply(subscription, message) == filter::action::skip);
        subscription.callback_url = "http:xxx";
        REQUIRE(filter::parse_and_apply(subscription, message) == filter::action::send_bright);
    }
}