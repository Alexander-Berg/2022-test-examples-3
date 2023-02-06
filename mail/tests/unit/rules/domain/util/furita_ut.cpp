#include <mail/notsolitesrv/src/rules/domain/util/furita.h>

#include <mail/notsolitesrv/tests/unit/util/furita.h>

#include <gtest/gtest.h>

namespace {

using NNotSoLiteSrv::EError;
using NNotSoLiteSrv::make_error_code;
using NNotSoLiteSrv::NFurita::TFuritaDomainAction;
using NNotSoLiteSrv::NFurita::TFuritaDomainRule;
using NNotSoLiteSrv::NMetaSaveOp::TRecipientMap;
using NNotSoLiteSrv::NRules::ApplyDomainAction;
using NNotSoLiteSrv::NRules::DomainActionActionCorrect;
using NNotSoLiteSrv::NRules::DomainActionCorrect;
using NNotSoLiteSrv::NRules::DomainActionDataCorrect;
using NNotSoLiteSrv::NRules::DomainActionsCorrect;
using NNotSoLiteSrv::NRules::FuritaDomainRulesAvailable;
using NNotSoLiteSrv::NRules::MakeDomainRulesAccumulatedResult;
using NNotSoLiteSrv::NRules::MakeEmptyFuritaClientResult;
using NNotSoLiteSrv::NRules::MakeFuritaOrgIds;
using NNotSoLiteSrv::NRules::MatchedDomainRulesCorrect;
using NNotSoLiteSrv::NRules::TDomainRulesAccumulatedResult;
using NNotSoLiteSrv::TOrgId;

const std::string DOMAIN_ACTION_INCORRECT{"incorrect"};
const std::string DOMAIN_ACTION_DROP{"drop"};
const std::string DOMAIN_ACTION_FORWARD{"forward"};

std::vector<TFuritaDomainAction> MakeTestDomainActions(bool correct = true, std::string email = "email") {
    return {
        {.Action{correct ? DOMAIN_ACTION_DROP : DOMAIN_ACTION_INCORRECT}},
        {.Action{DOMAIN_ACTION_FORWARD}, .Data{{.Email{std::move(email)}}}}
    };
}

std::vector<TFuritaDomainRule> MakeTestDomainRules() {
    const auto incorrect{false};
    const auto correct{true};
    return {
        {.Terminal = false, .Actions{MakeTestDomainActions(incorrect)}},
        {.Actions{MakeTestDomainActions(correct, "email0")}},
        {.Terminal = false, .Actions{MakeTestDomainActions(correct, "email1")}}
    };
}

TEST(TestMakeFuritaOrgIds, must_make_furita_org_ids) {
    TRecipientMap recipients {
        {"DeliveryId0", {}},
        {"DeliveryId1", {.user{.org_id{"0"}}, .params{.use_domain_rules = true}}},
        {"DeliveryId2", {.user{.org_id{"1"}}, .params{.use_domain_rules = true}}},
        {"DeliveryId3", {.user{.org_id{"1"}}, .params{.use_domain_rules = true}}}
    };

    EXPECT_EQ((std::set<TOrgId>{"0", "1"}), MakeFuritaOrgIds(recipients));
}

TEST(TestFuritaDomainRulesAvailable, must_be_able_to_detect_furita_domain_rules) {
    EXPECT_FALSE(FuritaDomainRulesAvailable({}));
    EXPECT_FALSE(FuritaDomainRulesAvailable({.Result{{}}}));
    EXPECT_TRUE(FuritaDomainRulesAvailable({.Result{{.Rules{{}}}}}));
}

TEST(TestMakeEmptyFuritaClientResult, must_make_empty_furita_client_result) {
    const auto result{MakeEmptyFuritaClientResult()};
    EXPECT_EQ(make_error_code(EError::Ok), result.ErrorCode);
    EXPECT_TRUE(result.Result);
    EXPECT_TRUE(result.Result->Rules.empty());
    EXPECT_EQ(0ull, result.Result->Revision);
}

TEST(TestMatchedDomainRulesCorrect, must_be_able_to_detect_incorrect_matched_domain_rules) {
    const auto rules{MakeTestDomainRules()};
    EXPECT_FALSE(MatchedDomainRulesCorrect(rules, {0, 1}));
    EXPECT_TRUE(MatchedDomainRulesCorrect(rules, {1, 2}));
}

TEST(TestDomainActionsCorrect, must_be_able_to_detect_incorrect_domain_actions) {
    const auto incorrect{false};
    EXPECT_FALSE(DomainActionsCorrect(MakeTestDomainActions(incorrect)));
    EXPECT_TRUE(DomainActionsCorrect(MakeTestDomainActions()));
}

TEST(TestDomainActionCorrect, must_be_able_to_detect_incorrect_domain_action) {
    EXPECT_FALSE(DomainActionCorrect({.Action{DOMAIN_ACTION_INCORRECT}, .Data{{.Email{"email"}}}}));
    EXPECT_FALSE(DomainActionCorrect({.Action{DOMAIN_ACTION_FORWARD}}));
    EXPECT_FALSE(DomainActionCorrect({.Action{DOMAIN_ACTION_FORWARD}, .Data{{}}}));
    EXPECT_TRUE(DomainActionCorrect({.Action{DOMAIN_ACTION_FORWARD}, .Data{{.Email{"email"}}}}));
}

TEST(TestDomainActionActionCorrect, must_be_able_to_detect_incorrect_domain_action_action) {
    EXPECT_FALSE(DomainActionActionCorrect(DOMAIN_ACTION_INCORRECT));
    EXPECT_TRUE(DomainActionActionCorrect(DOMAIN_ACTION_DROP));
    EXPECT_TRUE(DomainActionActionCorrect(DOMAIN_ACTION_FORWARD));
}

TEST(TestDomainActionDataCorrect, must_be_able_to_detect_incorrect_domain_action_data) {
    EXPECT_FALSE(DomainActionDataCorrect(DOMAIN_ACTION_FORWARD, {}));
    EXPECT_FALSE(DomainActionDataCorrect(DOMAIN_ACTION_FORWARD, {{}}));
    EXPECT_FALSE(DomainActionDataCorrect(DOMAIN_ACTION_FORWARD, {{.Email{""}}}));
    EXPECT_TRUE(DomainActionDataCorrect(DOMAIN_ACTION_FORWARD, {{.Email{"email"}}}));
    EXPECT_TRUE(DomainActionDataCorrect(DOMAIN_ACTION_DROP, {}));
}

TEST(TestMakeDomainRulesAccumulatedResult, must_make_domain_rules_accumulated_result) {
    const TDomainRulesAccumulatedResult expectedResult{.Forwards{"email0", "email1"}, .Drop = true,
        .AppliedDomainRuleIds{"1", "2"}};
    auto rules{MakeTestDomainRules()};
    rules[2].Terminal = true;
    const auto correct{true};
    rules.emplace_back(TFuritaDomainRule{.Terminal = false, .Actions{
        MakeTestDomainActions(correct, "email2")}});
    const auto actualResult{MakeDomainRulesAccumulatedResult(rules, {1, 2, 3})};
    EXPECT_EQ(expectedResult, actualResult);
}

TEST(TestApplyDomainAction, must_be_able_to_apply_domain_action) {
    TDomainRulesAccumulatedResult result;
    EXPECT_FALSE(result.Drop);
    ApplyDomainAction({.Action{DOMAIN_ACTION_DROP}}, result);
    const std::string email0{"email0"};
    ApplyDomainAction({.Action{DOMAIN_ACTION_FORWARD}, .Data{{.Email{email0}}}}, result);
    const std::string email1{"email1"};
    ApplyDomainAction({.Action{DOMAIN_ACTION_FORWARD}, .Data{{.Email{email1}}}}, result);
    EXPECT_EQ((TDomainRulesAccumulatedResult{.Forwards{email0, email1}, .Drop = true}), result);
}

}
