#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/furita/src/api/domain_rules_set/types.h>
#include <mail/furita/src/api/domain_rules_set/utils.h>

#include <yplatform/json.h>

namespace utils = furita::domain_rules_set::utils;
using furita::domain_rules_set::EDirectionScope;

namespace furita::domain_rules_set {

bool operator==(const TScope& left, const TScope& right) {
    return left.Direction == right.Direction;
}

bool operator==(const TAction& left, const TAction& right) {
    return left.Action == right.Action
        && left.Email == right.Email;
}

bool operator==(const TRule& left, const TRule& right) {
    return left.Terminal == right.Terminal
        && left.Condition == right.Condition
        && left.ConditionQuery == right.ConditionQuery
        && left.Actions == right.Actions
        && left.Scope == right.Scope;
}

}

TEST(DomainRuleSetUtils, for_valid_json_parse_rule_should_return_ok) {
    auto json = R"({
        "terminal": true,
        "scope": {"direction": "inbound"},
        "condition": {"foo": "bar"},
        "actions": [
            {
                "action": "forward",
                "data": {
                    "email": "foo"
                }
            },
            {
                "action": "drop"
            }
        ]
    })";
    yplatform::json_value val = utils::FromJson(json);

    auto result = utils::ParseRule(val);

    furita::domain_rules_set::TRule correctRule {
        .Terminal = true,
        .Scope = {.Direction = EDirectionScope::INBOUND},
        .Condition = utils::FromJson(R"({"foo": "bar"})"),
        .Actions = {
            {
                .Action = "forward",
                .Email = "foo"
            },
            {
                .Action = "drop"
            }
        }
    };

    EXPECT_EQ(result, correctRule);
}

struct TTestForInvalidArgument : public ::testing::TestWithParam<std::tuple<std::string, std::string>> {
};

TEST_P(TTestForInvalidArgument, for_invalid_json_parse_rule_should_throw_invalid_argument) {
    const auto& [json, excShouldContain] = GetParam();
    yplatform::json_value val = utils::FromJson(json);
    EXPECT_THROW({
        try {
            auto result = utils::ParseRule(val);
        } catch (const std::invalid_argument& exc) {
            EXPECT_TRUE(std::string{exc.what()}.find(excShouldContain) != std::string::npos);
            throw;
        }
    }, std::invalid_argument);
}

INSTANTIATE_TEST_SUITE_P(TestForInvalidArgument, TTestForInvalidArgument,
    ::testing::Values(
        // Terminal issues
        std::tuple<std::string, std::string>{
            R"({"terminal": "foobar", "scope": {"direction": "inbound"}, "condition": {"foo": "bar"}, "actions": [{"action": "move", "data": {"email": "foo"}}]})",
            "terminal"},

        // Scope issues
        std::tuple<std::string, std::string>{
            R"({"terminal": true, "condition": {"foo": "bar"}, "actions": [{"action": "move", "data": {"email": "foo"}}]})",
            "scope"},

        // Actions issues
        std::tuple<std::string, std::string>{
            R"({"terminal": true, "scope": {"direction": "inbound"}, "condition": {"foo": "bar"}, "actions": "foo"})",
            "action"},
        std::tuple<std::string, std::string>{
            R"({"terminal": true, "scope": {"direction": "inbound"}, "condition": {"foo": "bar"}, "actions": {"foo": "bar"}})",
            "action"},
        std::tuple<std::string, std::string>{
            R"({"terminal": true, "scope": {"direction": "inbound"}, "condition": {"foo": "bar"}, "actions": {"action": "foo"}})",
            "action"},
        std::tuple<std::string, std::string>{
            R"({"terminal": true, "scope": {"direction": "inbound"}, "condition": {"foo": "bar"}, "actions": {"action": "forward"}})",
            "action"}
    )
);

TEST(DomainRuleSetUtils, for_valid_json_parse_http_request_should_return_ok) {
    auto json = R"({
        "rules": [
            {
                "scope": {"direction": "inbound"},
                "condition": {"foo": "bar"}, 
                "actions": [
                    {
                        "action": "drop"
                    }
                ]
            },
            {
                "terminal": false,
                "scope": {"direction": "inbound"},
                "condition": {"bar": "foo"}, 
                "actions": [
                    {
                        "action": "forward",
                        "data": { "email": "foo" }
                    }
                ]
            }
        ]
    })";

    auto result = utils::ParseRulesSpec(json);

    EXPECT_EQ(result.Rules.size(), 2u);

    furita::domain_rules_set::TRule firstRule {
        .Scope = {.Direction = EDirectionScope::INBOUND},
        .Condition = utils::FromJson(R"({"foo": "bar"})"),
        .Actions = {
            {
                .Action = "drop"
            }
        }
    };
    EXPECT_EQ(result.Rules[0], firstRule);

    furita::domain_rules_set::TRule secondRule {
        .Terminal = false,
        .Scope = {.Direction = EDirectionScope::INBOUND},
        .Condition = utils::FromJson(R"({"bar": "foo"})"),
        .Actions = {
            {
                .Action = "forward",
                .Email = "foo"
            }
        }
    };
    EXPECT_EQ(result.Rules[1], secondRule);
}



TEST(DomainRuleSetUtils, for_rule_rule_to_json_should_produce_correct_json) {
    furita::domain_rules_set::TRule rule {
        .Terminal = true,
        .Scope = {.Direction = EDirectionScope::INBOUND},
        .Condition = utils::FromJson(R"({"foo": "bar"})"),
        .ConditionQuery = "foobar",
        .Actions = {
            {
                .Action = "move",
                .Email = "foo"
            }
        }
    };
    auto json = utils::RuleToJson(rule);
    EXPECT_EQ(json, utils::FromJson(R"({
        "terminal":true,
        "scope": {"direction": "inbound"},
        "condition":{"foo":"bar"},
        "condition_query":"foobar",
        "actions":[
            {"action": "move", "data": {"email": "foo"}}
        ]
    })"));
}

TEST(DomainRuleSetUtils, for_rules_rules_to_json_should_produce_correct_json) {
    std::vector<furita::domain_rules_set::TRule> rules {
        furita::domain_rules_set::TRule {
            .Terminal = true,
            .Scope = {.Direction = EDirectionScope::INBOUND},
            .Condition = utils::FromJson(R"({"foo": "bar"})"),
            .ConditionQuery = "foobar",
            .Actions = {
                {
                    .Action = "move",
                    .Email = "foo"
                }
            }
        },
        furita::domain_rules_set::TRule {
            .Scope = {.Direction = EDirectionScope::INBOUND},
            .Condition = utils::FromJson(R"({"bar": "foo"})"),
            .ConditionQuery = "barfoo",
            .Actions = {
                {
                    .Action = "drop",
                }
            }
        }
    };

    auto json = utils::RulesToJson(rules);
    auto correctJson = utils::FromJson(R"({
        "rules": [
            {
                "terminal": true,
                "scope": {"direction": "inbound"},
                "condition": {"foo": "bar"},
                "condition_query": "foobar",
                "actions": [
                    {
                        "action": "move",
                        "data": {
                            "email": "foo"
                        }
                    }
                ]
            },
            {
                "scope": {"direction": "inbound"},
                "condition": {"bar": "foo"},
                "condition_query": "barfoo",
                "actions": [
                    {
                        "action": "drop"
                    }
                ]
            }
        ]
    })");

    EXPECT_EQ(json, correctJson);
}

TEST(DomainRuleSetUtils, for_rule_with_empty_actions_should_produce_correct_json_with_empty_array) {
    std::vector<furita::domain_rules_set::TRule> rules {
        furita::domain_rules_set::TRule {
            .Terminal = true,
            .Scope = {.Direction = EDirectionScope::INBOUND},
            .Condition = utils::FromJson("{}"),
            .ConditionQuery = "anything",
            .Actions = {}
        }
    };

    auto json = utils::RulesToJson(rules);
    auto correctJson = utils::FromJson(R"({
        "rules": [
            {
                "terminal": true,
                "scope": {"direction": "inbound"},
                "condition": {},
                "condition_query": "anything",
                "actions": []
            }
        ]
    })");

    EXPECT_EQ(json, correctJson);
}
