#include "rule_applier.h"
#include "rule_ast.h"
#include "rule_ast_serialization.h"

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/yson/node/node_io.h>

#include <util/string/builder.h>

using namespace NCrypta::NSiberia;

Y_UNIT_TEST_SUITE(Segmentator) {
    TString Comment(const TExpressionPtr& expression, const NYT::TNode& node, const TString& msg) {
        return TStringBuilder() << msg
                                << "; Rule: " << Serialize(expression)
                                << "; Node: " << NYT::NodeToCanonicalYsonString(node);
    }

    void Check(const TExpressionPtr& expression, const NYT::TNode& nodeThatSatisfy, const NYT::TNode& nodeThatNotSatisfy) {
        TRuleApplier ruleApplier(expression);

        UNIT_ASSERT_C(ruleApplier.IsSatisfy(nodeThatSatisfy.AsMap()), Comment(expression, nodeThatSatisfy, "Not satisfy, but must"));
        UNIT_ASSERT_C(!ruleApplier.IsSatisfy(nodeThatNotSatisfy.AsMap()), Comment(expression, nodeThatNotSatisfy, "Satisfy, but must not"));
    }

    Y_UNIT_TEST(Basic) {
        TVector<std::tuple<TExpressionPtr, NYT::TNode, NYT::TNode>> cases = {
            {MakeSimpleExpression(TSimpleExpression::EOp::Equal, "field", 1), NYT::TNode()("field", 1), NYT::TNode()("field", 4)},
            {MakeSimpleExpression(TSimpleExpression::EOp::NotEqual, "field", 1), NYT::TNode()("field", 4), NYT::TNode()("field", 1)},
            {MakeSimpleExpression(TSimpleExpression::EOp::Less, "field", 1), NYT::TNode()("field", -1), NYT::TNode()("field", 4)},
            {MakeSimpleExpression(TSimpleExpression::EOp::LessOrEqual, "field", 1), NYT::TNode()("field", 1), NYT::TNode()("field", 4)},
            {MakeSimpleExpression(TSimpleExpression::EOp::LessOrEqual, "field", 1), NYT::TNode()("field", -1), NYT::TNode()("field", 4)},
            {MakeSimpleExpression(TSimpleExpression::EOp::More, "field", 1), NYT::TNode()("field", 4), NYT::TNode()("field", -1)},
            {MakeSimpleExpression(TSimpleExpression::EOp::MoreOrEqual, "field", 1), NYT::TNode()("field", 1), NYT::TNode()("field", -1)},
            {MakeSimpleExpression(TSimpleExpression::EOp::MoreOrEqual, "field", 1), NYT::TNode()("field", 4), NYT::TNode()("field", -1)},
            {
                MakeSimpleExpression(TSimpleExpression::EOp::Contains, "field", "Moscow"),
                NYT::TNode()("field", NYT::TNode::CreateList().Add("Novosibirsk").Add("Moscow")),
                NYT::TNode()("field", NYT::TNode::CreateList().Add("Novosibirsk"))
            },
            {MakeSimpleExpression(TSimpleExpression::EOp::StartsWith, "field", "ab"), NYT::TNode()("field", "abc"), NYT::TNode()("field", "xyz")},
            {MakeSimpleExpression(TSimpleExpression::EOp::EndsWith, "field", "bc"), NYT::TNode()("field", "abc"), NYT::TNode()("field", "xyz")},

            {
                MakeUnaryExpression(TUnaryExpression::EOp::Not, MakeSimpleExpression(TSimpleExpression::EOp::Equal, "field", 1)),
                NYT::TNode()("field", 4),
                NYT::TNode()("field", 1)
            },

            {
                MakeBinaryExpression(TBinaryExpression::EOp::Or,
                                     MakeSimpleExpression(TSimpleExpression::EOp::Equal, "field", 1),
                                     MakeSimpleExpression(TSimpleExpression::EOp::Equal, "field", 4)),
                NYT::TNode()("field", 1),
                NYT::TNode()("field", -1)
            },
            {
                MakeBinaryExpression(TBinaryExpression::EOp::And,
                                     MakeSimpleExpression(TSimpleExpression::EOp::StartsWith, "field", "ab"),
                                     MakeSimpleExpression(TSimpleExpression::EOp::EndsWith, "field", "c")),
                NYT::TNode()("field", "abc"),
                NYT::TNode()("field", "ab")
            },

            {
                MakeBinaryExpression(TBinaryExpression::EOp::And,
                                     MakeSimpleExpression(TSimpleExpression::EOp::StartsWith, "field", "ab"),
                                     MakeUnaryExpression(TUnaryExpression::EOp::Not,
                                                         MakeSimpleExpression(TSimpleExpression::EOp::EndsWith, "field", "c"))),
                NYT::TNode()("field", "ab"),
                NYT::TNode()("field", "abc")
            }
        };

        for (const auto& [expression, nodeThatSatisfy, nodeThatNotSatisfy] : cases) {
            Check(expression, nodeThatSatisfy, nodeThatNotSatisfy);
        }
    }
}
