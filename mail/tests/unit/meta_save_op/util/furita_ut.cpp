#include <mail/notsolitesrv/src/meta_save_op/util/furita.h>

#include <mail/notsolitesrv/tests/unit/util/furita.h>

#include <gtest/gtest.h>

namespace {

using namespace NNotSoLiteSrv::NMetaSaveOp;

using NNotSoLiteSrv::NFurita::TFuritaAction;
using NNotSoLiteSrv::NFurita::TFuritaRule;
using NNotSoLiteSrv::TUid;

TEST(TestMakeFuritaUids, must_make_furita_uids) {
    TUser user;
    TParams params;
    TRecipientMap recipients;
    user.uid = 0;
    recipients["DeliveryId0"] = {user, params};
    user.uid = 1;
    params.use_filters = false;
    recipients["DeliveryId1"] = {user, params};
    user.uid = 2;
    params.use_filters = true;
    recipients["DeliveryId2"] = {user, params};
    user.uid = 3;
    recipients["DeliveryId3"] = {std::move(user), std::move(params)};
    EXPECT_EQ((std::set<TUid>{0, 2, 3}), MakeFuritaUids(recipients));
}

TEST(TestHasVerifiedAction, must_be_able_to_detect_verified_action) {
    TFuritaRule rule;
    EXPECT_FALSE(HasVerifiedAction(rule));
    rule.Actions.emplace_back(TFuritaAction{});
    rule.Actions.emplace_back(TFuritaAction{});
    EXPECT_FALSE(HasVerifiedAction(rule));
    rule.Actions[1].Verified = true;
    EXPECT_TRUE(HasVerifiedAction(rule));
}

TEST(TestEnabledRuleWithVerifiedAction, must_check_if_rule_is_enabled_and_has_verified_action) {
    TFuritaRule rule;
    rule.Enabled = false;
    rule.Actions = {TFuritaAction{}, TFuritaAction{}};
    EXPECT_FALSE(EnabledRuleWithVerifiedAction(rule));
    rule.Actions[1].Verified = true;
    EXPECT_FALSE(EnabledRuleWithVerifiedAction(rule));
    rule.Enabled = true;
    rule.Actions[1].Verified = false;
    EXPECT_FALSE(EnabledRuleWithVerifiedAction(rule));
    rule.Actions[1].Verified = true;
    EXPECT_TRUE(EnabledRuleWithVerifiedAction(rule));
}

TEST(TestEnabledRuleWithVerifiedActionAvailable, must_be_able_to_detect_enabled_rule_with_verified_action) {
    EXPECT_FALSE(EnabledRuleWithVerifiedActionAvailable({}));
    std::vector<TFuritaRule> rules{2};
    rules[0].Enabled = false;
    rules[0].Actions = {TFuritaAction{}};
    rules[1].Enabled = false;
    rules[1].Actions = {TFuritaAction{}, TFuritaAction{}};
    EXPECT_FALSE(EnabledRuleWithVerifiedActionAvailable(rules));
    rules[1].Enabled = true;
    rules[1].Actions[1].Verified = true;
    EXPECT_TRUE(EnabledRuleWithVerifiedActionAvailable(rules));
}

TEST(TestMakeFuritaRuleByPriorityAndIdMap, must_make_furita_rule_by_priority_and_id_map) {
    std::vector<TFuritaRule> rules{3};
    rules[0].Enabled = false;
    rules[0].Stop = false;
    rules[0].Actions = {TFuritaAction{}};
    rules[1].Enabled = true;
    rules[1].Stop = false;
    rules[1].Actions = {TFuritaAction{}};
    rules[2].Enabled = false;
    rules[2].Stop = false;
    rules[2].Actions = {TFuritaAction{}, TFuritaAction{}};
    rules[2].Actions[1].Verified = true;
    EXPECT_TRUE(MakeFuritaRuleByPriorityAndIdMap(rules).empty());

    TFuritaRule ruleA;
    ruleA.Id = "a";
    ruleA.Priority = 1;
    ruleA.Enabled = true;
    ruleA.Stop = false;
    ruleA.Actions = {TFuritaAction{}, TFuritaAction{}};
    ruleA.Actions[1].Verified = true;

    TFuritaRule ruleB;
    ruleB.Id = "b";
    ruleB.Priority = 0;
    ruleB.Enabled = true;
    ruleB.Stop = false;
    ruleB.Actions = {TFuritaAction{}, TFuritaAction{}};
    ruleB.Actions[1].Verified = true;

    rules.emplace_back(ruleA);
    rules.emplace_back(ruleB);

    TFuritaRuleByPriorityAndIdMap expectedMap {
        {std::make_pair(ruleA.Priority, ruleA.Id), std::move(ruleA)},
        {std::make_pair(ruleB.Priority, ruleB.Id), std::move(ruleB)},
    };

    EXPECT_EQ(expectedMap, MakeFuritaRuleByPriorityAndIdMap(rules));
}

}
