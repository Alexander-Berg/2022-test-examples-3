#include "../src/filter/parser/decode_filter_v1.h"
#include "../src/filter/parser/decode_filter_v2.h"
#include <yxiva/core/filter/filter_set.h>
#include <yxiva/core/filter/parse.h>
#include <yxiva/core/message.h>
#include <catch.hpp>

using namespace yxiva;

TEST_CASE("filters/to_string", "")
{
    std::vector<filter::rule> rules = { { filter::action::skip,
                                          filter::expression{ {}, { "TagA" } } } };
    filter::vars_t vars = { { "TagA",
                              { filter::message_condition_type::has_tags, "", { "tagA" } } } };

    filter_set fset(std::move(rules), std::move(vars), filter::action::send_silent);

    auto json_str = fset.to_string();

    REQUIRE(
        json_str ==
        R"({"rules":[{"do":"skip","if":"TagA"},{"do":"send_silent"}],"vars":{"TagA":{"$has_tags":["tagA"]}}})");

    filter_set fset2;
    REQUIRE(filter::parse(fset2, json_str));
    REQUIRE(fset2 == fset);
}

TEST_CASE("filters/to_empty_string", "")
{
    filter_set fset1({}, {}, filter::action::send_bright);
    REQUIRE(fset1.to_string() == "");
    filter_set fset2({}, {}, filter::action::send_silent);
    REQUIRE(fset2.to_string() != "");
}

TEST_CASE("filters/to_string/v2/embedded_condition/default_default", "")
{
    const char* str = R"({"rules":[{"do":"skip","if":{"$event":["insert"]}}],"vars":{}})";
    filter_set fset;
    filter::parse_v2(fset, str);
    REQUIRE(fset.to_string() == str);
}

TEST_CASE("filters/to_string/v2/embedded_condition/not_default_default", "")
{
    const char* str =
        R"({"rules":[{"do":"skip","if":{"$event":["insert"]}},{"do":"send_silent"}],"vars":{}})";
    filter_set fset;
    filter::parse_v2(fset, str);
    REQUIRE(fset.to_string() == str);
}
