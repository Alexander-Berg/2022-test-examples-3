#include "helpers.h"
#include "rule_ast.h"
#include "rule_ast_serialization.h"

#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta::NSiberia;

Y_UNIT_TEST_SUITE(RuleParser) {
    void CheckPositiveParsing(const TString& rule, const TString& expectedExpression) {
        const auto& result = NRuleParser::Parse(rule);

        UNIT_ASSERT_C(result.Expression.Defined(), result.ErrorMessage);
        UNIT_ASSERT_C(result.ErrorMessage.empty(), result.ErrorMessage);
        UNIT_ASSERT_EQUAL_C(expectedExpression, Serialize(*result.Expression), Serialize(*result.Expression));
    }

    void CheckNegativeParsing(const TString& rule, const TString& expectedErrorMessage) {
        const auto& result = NRuleParser::Parse(rule);

        UNIT_ASSERT_C(!result.Expression.Defined(), Serialize(*result.Expression));
        UNIT_ASSERT_EQUAL_C(expectedErrorMessage, result.ErrorMessage, result.ErrorMessage);
    }

    Y_UNIT_TEST(Positive) {
        TVector<std::pair<TString, TString>> cases = {
            {"xxx.@login == 42", "(xxx.@login Equal 42u)"},
            {"field == 42", "(field Equal 42u)"},
            {"field != TRUE", "(field NotEqual \%true)"},
            {"field == FALSE", "(field Equal \%false)"},
            {"field < 11", "(field Less 11u)"},
            {"field <= 11", "(field LessOrEqual 11u)"},
            {"field > -1", "(field More -1)"},
            {"field >= -1.14", "(field MoreOrEqual -1.14)"},
            {"field CONTAINS \"xyz\"", "(field Contains \"xyz\")"},
            {"field STARTS_WITH \"xyz :!?-_\"", "(field StartsWith \"xyz :!?-_\")"},
            {"field_42 ENDS_WITH \"\"", "(field_42 EndsWith \"\")"},
            {"((((field == TRUE))))", "(field Equal %true)"},
            {"!(field == 42)", "(Not (field Equal 42u))"},
            {"(a == 42) && (b == 1)", "((a Equal 42u) And (b Equal 1u))"},
            {"(a == 42) || (b == 1)", "((a Equal 42u) Or (b Equal 1u))"},
            {"(a == 42) || (b == 1) && (c < -1)", "((a Equal 42u) Or ((b Equal 1u) And (c Less -1)))"},
            {"a == 42 || ! b == 1 && c < -1", "((a Equal 42u) Or ((Not (b Equal 1u)) And (c Less -1)))"},
            {"a > -1 && a < -1 && a == -1", "(((a More -1) And (a Less -1)) And (a Equal -1))"},
            {"a > -1 || a < -1 || a == -1", "(((a More -1) Or (a Less -1)) Or (a Equal -1))"}
        };

        for (const auto& [rule, expectedExpression] : cases) {
            CheckPositiveParsing(rule, expectedExpression);
        }
    }

    Y_UNIT_TEST(Negative) {
        TVector<std::pair<TString, TString>> cases = {
            {"field-xxx == 42", "position 5 - 6: syntax error, unexpected End"},
            {"", "position 0 - 0: syntax error, unexpected End, expecting Not or LeftParenthesis or Field"},
            {"(field == 1", "position 11 - 11: syntax error, unexpected End, expecting And or Or or RightParenthesis"}
        };

        for (const auto& [rule, expectedErrorMessage] : cases) {
            CheckNegativeParsing(rule, expectedErrorMessage);
        }
    }
}
