#include "../src/filter/parser/decode_filter_v1.h"
#include "../src/filter/parser/decode_filter_v2.h"
#include <yxiva/core/filter/filter_set.h>
#include <yxiva/core/filter/parse.h>
#include <yxiva/core/message.h>
#include <catch.hpp>

using namespace yxiva;

TEST_CASE("filters/tags/2or2", "")
{
    string jsfilter = R"({
      "rules": [
      { "condition": "has_tags", "action": "skip", "key" : "", "value": ["tagA", "tagB"] },
      { "condition": "has_tags", "action": "skip", "key" : "", "value": ["tagC", "tagD"] }
      ]
      })";
    filter_set fset;
    filter::parse(fset, jsfilter);

    message m;

    m.tags = { "tagA", "tagB", "tagC", "tagD" };
    REQUIRE(fset.apply(m) == filter::action::skip);

    m.tags = { "tagA", "tagB" };
    REQUIRE(fset.apply(m) == filter::action::skip);

    m.tags = { "tagC", "tagD" };
    REQUIRE(fset.apply(m) == filter::action::skip);

    m.tags = { "tagA", "tagD" };
    REQUIRE(fset.apply(m) != filter::action::skip);

    m.tags = { "tagB", "tagC" };
    REQUIRE(fset.apply(m) != filter::action::skip);

    m.tags = {};
    REQUIRE(fset.apply(m) != filter::action::skip);
}

TEST_CASE("filters/tags/with_default_action", "")
{
    string jsfilter = R"({
      "rules": [
      { "condition": "has_tags", "action": "send_silent", "key" : "", "value": ["tagA"] }
      ],
      "default_action": "skip"
      })";
    filter_set fset;
    REQUIRE(filter::parse(fset, jsfilter));

    message m;

    m.tags = { "tagA" };
    REQUIRE(fset.apply(m) == filter::action::send_silent);

    m.tags = { "tagA", "tagB" };
    REQUIRE(fset.apply(m) == filter::action::send_silent);

    m.tags = { "tagD" };
    REQUIRE(fset.apply(m) == filter::action::skip);
}

TEST_CASE("filters/expressions/1SimpleOrDefault", "")
{
    string jsfilter = R"({
      "vars": { "FID": {"fid": {"$eq": ["val1", "val2"]}} },
      "rules": [
      { "do": "skip", "if": "FID" },
      { "do": "send_silent" }
      ]})";
    filter_set fset;
    REQUIRE(filter::parse(fset, jsfilter));

    message m;
    m.data["fid"] = "val1";
    CHECK(fset.apply(m) == filter::action::skip);

    m.data["fid"] = "val3";
    CHECK(fset.apply(m) == filter::action::send_silent);

    m.data = {};
    CHECK(fset.apply(m) == filter::action::send_silent);
}

TEST_CASE("filters/expressions/1Combined", "")
{
    string jsfilter = R"({
      "vars": { "FID": {"fid": {"$eq": ["val1", "val2"]}},
                "OP": {"operation": {"$eq": ["diff"]}}
      },
      "rules": [
      { "do": "skip", "if": "FID & !OP" }
      ]})";
    filter_set fset;
    REQUIRE(filter::parse(fset, jsfilter));

    message m;
    m.data["fid"] = "val1";
    m.data["operation"] = "diff";
    CHECK(fset.apply(m) == filter::action::send_bright);

    m.data["operation"] = "fdiff";
    CHECK(fset.apply(m) == filter::action::skip);

    m.data["fid"] = "val3";
    CHECK(fset.apply(m) == filter::action::send_bright);

    m.data = {};
    CHECK(fset.apply(m) == filter::action::send_bright);
}

TEST_CASE("filters/expressions/always_false", "")
{
    string jsfilter = R"({
      "vars": { "FID": {"fid": {"$eq": ["val1", "val2"]}} },
      "rules": [
      { "do": "skip", "if": "FID & !FID" }
      ]})";
    filter_set fset;
    REQUIRE(filter::parse(fset, jsfilter));

    message m;
    m.data["fid"] = "val1";
    CHECK(fset.apply(m) == filter::action::send_bright);
    m.data["fid"] = "val3";
    CHECK(fset.apply(m) == filter::action::send_bright);
}

TEST_CASE("filters/expressions/LongExpressionNoNot", "")
{
    string jsfilter = R"({
      "vars": {
        "F1": {"f1": {"$eq": ["val1", "val2"]}},
        "F2": {"f2": {"$eq": ["val1"]}},
        "F3": {"f3": {"$eq": ["val1", "val2"]}},
        "F4": {"f4": {"$eq": ["val1"]}},
        "F5": {"f5": {"$eq": ["val1", "val2"]}}
      },
      "rules": [
      { "do": "skip", "if": "F1 & F2 & F3 & F4 & F5" }
      ]})";
    filter_set fset;
    REQUIRE(filter::parse(fset, jsfilter));

    message m;
    m.data["f1"] = "val1";
    CHECK(fset.apply(m) == filter::action::send_bright);
    m.data["f2"] = "val1";
    CHECK(fset.apply(m) == filter::action::send_bright);
    m.data["f3"] = "val1";
    CHECK(fset.apply(m) == filter::action::send_bright);
    m.data["f4"] = "val1";
    CHECK(fset.apply(m) == filter::action::send_bright);
    m.data["f5"] = "val3";
    CHECK(fset.apply(m) == filter::action::send_bright);
    m.data["f5"] = "val1";
    CHECK(fset.apply(m) == filter::action::skip);
}

TEST_CASE("filters/expressions/LongExpressionEverySecondNot", "")
{
    string jsfilter = R"({
      "vars": {
        "F1": {"f1": {"$eq": ["val1", "val2"]}},
        "F2": {"f2": {"$eq": ["val1"]}},
        "F3": {"f3": {"$eq": ["val1", "val2"]}},
        "F4": {"f4": {"$eq": ["val1"]}},
        "F5": {"f5": {"$eq": ["val1", "val2"]}}
      },
      "rules": [
      { "do": "skip", "if": "!F1 & F2 & !F3 & F4 & !F5" }
      ]})";
    filter_set fset;
    REQUIRE(filter::parse(fset, jsfilter));

    message m;
    m.data["f1"] = "val1";
    m.data["f2"] = "val1";
    m.data["f3"] = "val1";
    m.data["f4"] = "val1";
    m.data["f5"] = "val1";
    CHECK(fset.apply(m) == filter::action::send_bright);
    m.data["f1"] = "val3";
    m.data["f3"] = "val3";
    m.data["f5"] = "val3";
    CHECK(fset.apply(m) == filter::action::skip);
}

TEST_CASE("filters/expressions/LongExpressionAllNot", "")
{
    string jsfilter = R"({
      "vars": {
        "F1": {"f1": {"$eq": ["val1", "val2"]}},
        "F2": {"f2": {"$eq": ["val1"]}},
        "F3": {"f3": {"$eq": ["val1", "val2"]}},
        "F4": {"f4": {"$eq": ["val1"]}},
        "F5": {"f5": {"$eq": ["val1", "val2"]}}
      },
      "rules": [
      { "do": "skip", "if": "!F1 & !F2 & !F3 & !F4 & !F5" }
      ]})";
    filter_set fset;
    REQUIRE(filter::parse(fset, jsfilter));

    message m;
    m.data["f1"] = "val3";
    m.data["f2"] = "val3";
    m.data["f3"] = "val3";
    m.data["f4"] = "val3";
    m.data["f5"] = "val3";
    CHECK(fset.apply(m) == filter::action::skip);
    m.data["f5"] = "val1";
    CHECK(fset.apply(m) == filter::action::send_bright);
}

TEST_CASE("filters/expressions/SeveralSimpleExpressions", "")
{
    string jsfilter = R"({
      "vars": {
        "F1": {"f": {"$eq": ["val1"]}},
        "F2": {"f": {"$eq": ["val2"]}},
        "F3": {"f": {"$eq": ["val3"]}}
      },
      "rules": [
      { "do": "send_bright", "if": "F1" },
      { "do": "skip", "if": "F2" },
      { "do": "send_silent", "if": "F3" },
      { "do": "skip" }
      ]})";
    filter_set fset;
    REQUIRE(filter::parse(fset, jsfilter));

    message m;
    m.data["f"] = "val1";
    CHECK(fset.apply(m) == filter::action::send_bright);
    m.data["f"] = "val2";
    CHECK(fset.apply(m) == filter::action::skip);
    m.data["f"] = "val3";
    CHECK(fset.apply(m) == filter::action::send_silent);
    m.data["f"] = "val4";
    CHECK(fset.apply(m) == filter::action::skip);
}

TEST_CASE("filters/expressions/SeveralCombinedExpressions", "")
{
    string jsfilter = R"({
      "vars": {
        "F1": {"f": {"$eq": ["val1"]}},
        "F2": {"f": {"$eq": ["val2"]}},
        "F3": {"f": {"$eq": ["val3"]}},
        "D1": {"d": {"$eq": ["d1"]}},
        "D2": {"d": {"$eq": ["d2"]}}
      },
      "rules": [
      { "do": "send_bright", "if": "F1 & D1" },
      { "do": "skip", "if": "F2 & D2" },
      { "do": "send_silent", "if": "F3 & !D1" },
      { "do": "skip" }
      ]})";
    filter_set fset;
    REQUIRE(filter::parse(fset, jsfilter));

    message m;
    m.data["f"] = "val1";
    m.data["d"] = "d1";
    CHECK(fset.apply(m) == filter::action::send_bright);
    m.data["f"] = "val2";
    m.data["d"] = "d2";
    CHECK(fset.apply(m) == filter::action::skip);
    m.data["f"] = "val3";
    CHECK(fset.apply(m) == filter::action::send_silent);
    m.data["d"] = "d1";
    CHECK(fset.apply(m) == filter::action::skip);
}

TEST_CASE("filters/flags/ignore_filters_flag", "")
{
    string jsfilter = R"({
      "rules": [
      { "condition": "has_tags", "action": "send_silent", "key" : "", "value": ["tagA"] }
      ],
      "default_action": "skip"
      })";
    filter_set fset;
    REQUIRE(filter::parse(fset, jsfilter));

    message m;
    m.set_flag(message_flags::ignore_filters);

    m.tags = { "tagA" };
    REQUIRE(fset.apply(m) == filter::action::send_bright);

    m.tags = { "tagA", "tagB" };
    REQUIRE(fset.apply(m) == filter::action::send_bright);

    m.tags = { "tagD" };
    REQUIRE(fset.apply(m) == filter::action::send_bright);

    m.unset_flag(message_flags::ignore_filters);

    m.tags = { "tagA" };
    REQUIRE(fset.apply(m) == filter::action::send_silent);

    m.tags = { "tagA", "tagB" };
    REQUIRE(fset.apply(m) == filter::action::send_silent);

    m.tags = { "tagD" };
    REQUIRE(fset.apply(m) == filter::action::skip);
}
