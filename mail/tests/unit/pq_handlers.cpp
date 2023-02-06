#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include "errors.h"
#include "../../src/pq/multiuser_rules_handler.hpp"
#include "../../src/pq/get_rules_handler.hpp"
#include "../../src/pq/get_conditions_handler.hpp"
#include "../../src/pq/get_actions_handler.hpp"
#include "../../src/pq/bw_list_handler.hpp"
#include "../../src/pq/edit_rule_handler.hpp"
#include "../../src/pq/get_not_verified_params_handler.hpp"
#include "http_client_mock.h"
#include <boost/optional.hpp>

namespace {
template <typename Future>
std::string exception_description(Future& f) {
    if (!f.has_exception())
        return "no exceptions";
    try {
        f.get();
    } catch (const yplatform::exception& e) {
        return e.public_message();
    } catch (const std::exception& e) {
        return e.what();
    } catch (...) {
    }
    return "unknown";
}

furita::pq::reflection::UnionRuleConditionAction CreateUnionWithRule(uint64_t uid, uint64_t rule_id,
                                                                     std::string name, bool enabled, int64_t prio, bool stop,
                                                                     size_t created, std::string type) {
    furita::pq::reflection::UnionRuleConditionAction result;
    result.uid = uid;
    result.rule_id = rule_id;
    result.name = name;
    result.enabled = enabled;
    result.prio = prio;
    result.stop = stop;
    result.created = created;
    result.type = type;
    return result;
}

furita::pq::reflection::UnionRuleConditionAction CreateUnionWithCondition(uint64_t uid, uint64_t rule_id,
                                                                          std::string field_type, std::string field, std::string pattern,
                                                                          std::string condition_oper, std::string link, bool negative) {
    furita::pq::reflection::UnionRuleConditionAction result;
    result.uid = uid;
    result.rule_id = rule_id;
    result.field_type = field_type;
    result.field = field;
    result.pattern = pattern;
    result.condition_oper = condition_oper;
    result.link = link;
    result.negative = negative;
    return result;
}

furita::pq::reflection::UnionRuleConditionAction CreateUnionWithAction(uint64_t uid, uint64_t rule_id,
                                                                       uint64_t action_id, std::string oper, std::string param,
                                                                       bool verified) {
    furita::pq::reflection::UnionRuleConditionAction result;
    result.uid = uid;
    result.rule_id = rule_id;
    result.action_id = action_id;
    result.action_oper = oper;
    result.param = param;
    result.verified = verified;
    return result;
}

void CheckRule(furita::rules::rule_ptr r, boost::optional<uint64_t> uid, uint64_t rule_id,
               std::string name, bool enabled, int64_t prio, bool stop,
               std::time_t created, std::string type) {
    ASSERT_EQ(r->name, name);
    ASSERT_EQ(r->prio, prio);
    ASSERT_EQ(r->enabled, enabled);
    ASSERT_EQ(r->stop, stop);
    ASSERT_EQ(r->type, type);
    ASSERT_EQ(r->created, created);
    ASSERT_EQ(r->id, rule_id);
    if (uid) {
        ASSERT_EQ(r->uid, *uid);
    }
}

void CheckCondition(furita::rules::condition_ptr c, uint64_t rule_id, std::string field_type, std::string field, std::string pattern,
                    std::string condition_oper, std::string link, bool negative) {
    ASSERT_EQ(c->rule_id, rule_id);
    ASSERT_EQ(c->field_type, field_type);
    ASSERT_EQ(c->field, field);
    ASSERT_EQ(c->pattern, pattern);
    ASSERT_EQ(c->link, furita::rules::condition::link_type::fromString(link));
    ASSERT_EQ(c->oper, furita::rules::condition::oper_type::fromString(condition_oper));
    ASSERT_EQ(c->neg, negative);
}

void CheckAction(furita::rules::action_ptr a, uint64_t rule_id,
                 uint64_t action_id, std::string oper, std::string param,
                 bool verified) {
    ASSERT_EQ(a->rule_id, rule_id);
    ASSERT_EQ(a->id, action_id);
    ASSERT_EQ(a->oper, oper);
    ASSERT_EQ(a->param, param);
    ASSERT_EQ(a->verified, verified);
}

}

namespace furita {
using namespace testing;
using yplatform::future::future;
using yplatform::future::promise;

TEST(BWListHandler, success) {
    auto handler = boost::make_shared< ::furita::pq::BWListHandler>();
    std::vector< ::furita::pq::reflection::ListEntry> res;
    res.emplace_back("black", "test@test.ru", 179);
    res.emplace_back("white", "test2@test.ru", 99);
    promise<rules::blacklist_ptr> prom;
    handler->HandleBWList({}, res, prom);
    future<rules::blacklist_ptr> f(prom);
    auto result = f.get();
    ASSERT_EQ(result->black.size(), 1u);
    ASSERT_EQ(result->white.size(), 1u);
    auto entry = *result->black.begin();
    ASSERT_EQ(entry.email, "test@test.ru");
    ASSERT_EQ(entry.created, 179);
    entry = *result->white.begin();
    ASSERT_EQ(entry.email, "test2@test.ru");
    ASSERT_EQ(entry.created, 99);
}

TEST(BWListHandler, error) {
    auto handler = boost::make_shared< ::furita::pq::BWListHandler>();
    std::vector< ::furita::pq::reflection::ListEntry> res;
    promise<rules::blacklist_ptr> prom;
    handler->HandleBWList(mail_errors::error_code(errors::pgg_error), res, prom);
    future<rules::blacklist_ptr> f(prom);
    ASSERT_TRUE(f.has_exception());
}

TEST(GetRuleHandler, success) {
    auto handler = boost::make_shared< ::furita::pq::GetRuleHandler>();
    std::vector< ::furita::pq::reflection::Rule> res;
    promise<rules::rule_list_ptr> prom;
    res.emplace_back(18, "name1", true, 89238921819, false, 3811991212, "user");
    handler->HandleGetRule({}, res, prom, 18);
    future<rules::rule_list_ptr> f(prom);
    auto rule_list = *f.get();
    ASSERT_EQ(rule_list.size(), 1u);
    CheckRule(rule_list[0], {}, 18, "name1", true, 89238921819, false, 3811991212, "user");
}

TEST(GetRuleHandler, severalRulesError) {
    auto handler = boost::make_shared< ::furita::pq::GetRuleHandler>();
    std::vector< ::furita::pq::reflection::Rule> res;
    promise<rules::rule_list_ptr> prom;
    res.emplace_back(18, "name1", true, 89238921819, false, 3811991212, "user");
    res.emplace_back(18, "name2", true, 181818, true, 3811991212, "user");
    handler->HandleGetRule({}, res, prom, 18);
    future<rules::rule_list_ptr> f(prom);
    ASSERT_TRUE(f.has_exception());
    ASSERT_EQ(exception_description(f), "More than one rule get by id: 18");
}

TEST(GetRuleHandler, pggError) {
    auto handler = boost::make_shared< ::furita::pq::GetRuleHandler>();
    promise<rules::rule_list_ptr> prom;
    handler->HandleGetRule(mail_errors::error_code(errors::pgg_error), {}, prom, 19);
    future<rules::rule_list_ptr> f(prom);
    ASSERT_TRUE(f.has_exception());
}

TEST(GetRulesHandler, success) {
    auto handler = boost::make_shared< ::furita::pq::GetRulesHandler>();
    std::vector< ::furita::pq::reflection::Rule> res;
    promise<rules::rule_list_ptr> prom;
    res.emplace_back(18, "name1", true, 89238921819, false, 3811991212, "user");
    res.emplace_back(19, "name2", true, 181818, true, 3811991212, "user");
    handler->HandleGetRules({}, res, prom);
    future<rules::rule_list_ptr> f(prom);
    auto rule_list = *f.get();
    ASSERT_EQ(rule_list.size(), 2u);
    CheckRule(rule_list[0], {}, 18, "name1", true, 89238921819, false, 3811991212, "user");
    CheckRule(rule_list[1], {}, 19, "name2", true, 181818, true, 3811991212, "user");
}

TEST(GetRulesHandler, error) {
    auto handler = boost::make_shared< ::furita::pq::GetRulesHandler>();
    std::vector< ::furita::pq::reflection::Rule> res;
    promise<rules::rule_list_ptr> prom;
    handler->HandleGetRules(mail_errors::error_code(errors::sharpei_error), {}, prom);
    future<rules::rule_list_ptr> f(prom);
    ASSERT_TRUE(f.has_exception());
}

TEST(GetActionsHandler, success) {
    auto handler = boost::make_shared< ::furita::pq::GetActionsHandler>();
    std::vector< ::furita::pq::reflection::Action> res;
    promise<rules::action_list_ptr> prom;
    res.emplace_back(12, 13, "oper1", "param1", true);
    res.emplace_back(17, 29, "oper2", "param2", false);
    handler->HandleActions({}, res, prom);
    future<rules::action_list_ptr> f(prom);
    const auto action_list = *f.get();
    ASSERT_EQ(action_list.size(), 2u);
    CheckAction(action_list[0], 12, 13, "oper1", "param1", true);
    CheckAction(action_list[1], 17, 29, "oper2", "param2", false);
}

TEST(GetActionsHandler, error) {
    auto handler = boost::make_shared< ::furita::pq::GetActionsHandler>();
    promise<rules::action_list_ptr> prom;
    handler->HandleActions(mail_errors::error_code(errors::sharpei_error), {}, prom);
    future<rules::action_list_ptr> f(prom);
    ASSERT_TRUE(f.has_exception());
}

TEST(GetConditionsHandler, success) {
    auto handler = boost::make_shared< ::furita::pq::GetConditionsHandler>();
    std::vector< ::furita::pq::reflection::Condition> res;
    promise<rules::condition_list_ptr> prom;
    res.emplace_back(13, "type1", "abra", "alacazam", "contains", "or", false);
    res.emplace_back(14, "type2", "cadabra", "sizam", "matches", "and", true);
    handler->HandleConditions({}, res, prom);
    future<rules::condition_list_ptr> f(prom);
    const auto condition_list = *f.get();
    ASSERT_EQ(condition_list.size(), 2u);
    CheckCondition(condition_list[0], 13, "type1", "abra", "alacazam", "contains", "or", false);
    CheckCondition(condition_list[1], 14, "type2", "cadabra", "sizam", "matches", "and", true);
}

TEST(GetConditionsHandler, error) {
    auto handler = boost::make_shared< ::furita::pq::GetConditionsHandler>();
    std::vector< ::furita::pq::reflection::Condition> res;
    promise<rules::condition_list_ptr> prom;
    handler->HandleConditions(mail_errors::error_code(errors::sharpei_error), {}, prom);
    future<rules::condition_list_ptr> f(prom);
    ASSERT_TRUE(f.has_exception());
}

TEST(EditRuleHandler, prepareActions) {
    auto handler = boost::make_shared< ::furita::pq::EditRuleHandler>();
    rules::action_list_ptr a_list = boost::make_shared<rules::action_list>();
    rules::action_ptr act = boost::make_shared<rules::action>();
    act->oper = "op1";
    act->param = "p1";
    act->verified = true;
    a_list->push_back(act);
    act = boost::make_shared<rules::action>();
    act->oper = "op2";
    act->param = "p2";
    act->verified = false;
    a_list->push_back(act);
    const auto result = handler->PrepareActions(a_list);

    ASSERT_EQ(result[0], "op1");
    ASSERT_EQ(result[1], "p1");
    ASSERT_EQ(result[2], "t");
    ASSERT_EQ(result[3], "op2");
    ASSERT_EQ(result[4], "p2");
    ASSERT_EQ(result[5], "f");
}

TEST(EditRuleHandler, prepareConditions) {
    auto handler = boost::make_shared< ::furita::pq::EditRuleHandler>();
    rules::condition_list_ptr cond_list = boost::make_shared<rules::condition_list>();
    rules::condition_ptr cond = boost::make_shared<rules::condition>();
    cond->field = "all";
    cond->pattern = "pattern1";
    cond->oper = ::furita::rules::condition::oper_type::MATCHES;
    cond->link = ::furita::rules::condition::link_type::OR;
    cond->neg = true;
    cond_list->push_back(cond);
    cond = boost::make_shared<rules::condition>();
    cond->field = "body";
    cond->pattern = "pattern2";
    cond->oper = ::furita::rules::condition::oper_type::CONTAINS;
    cond->link = ::furita::rules::condition::link_type::AND;
    cond->neg = false;
    cond_list->push_back(cond);

    const auto result = handler->PrepareConditions(cond_list);

    ASSERT_EQ(result[0], "flag");
    ASSERT_EQ(result[1], "all");
    ASSERT_EQ(result[2], "pattern1");
    ASSERT_EQ(result[3], "matches");
    ASSERT_EQ(result[4], "or");
    ASSERT_EQ(result[5], "t");
    ASSERT_EQ(result[6], "body");
    ASSERT_EQ(result[7], "body");
    ASSERT_EQ(result[8], "pattern2");
    ASSERT_EQ(result[9], "contains");
    ASSERT_EQ(result[10], "and");
    ASSERT_EQ(result[11], "f");
}

TEST(NotVerifiedParamsHandler, success) {
    auto handler = boost::make_shared< ::furita::pq::NotVerifiedParamsHandler>();
    std::vector< ::furita::pq::reflection::NotVerifiedParams> res;
    promise<std::map<uint64_t, std::pair<std::string, std::string>>> prom;
    res.emplace_back(12, "p1", "or");
    res.emplace_back(13, "p2", "and");
    handler->HandleNotVerifiedParams({}, res, prom);
    future<std::map<uint64_t, std::pair<std::string, std::string>>> f(prom);
    auto result = f.get();
    ASSERT_EQ(result.size(), 2u);
    ASSERT_EQ(result[12].first, "p1");
    ASSERT_EQ(result[12].second, "or");
    ASSERT_EQ(result[13].first, "p2");
    ASSERT_EQ(result[13].second, "and");
}

TEST(NotVerifiedParamsHandler, error) {
    auto handler = boost::make_shared< ::furita::pq::NotVerifiedParamsHandler>();
    std::vector< ::furita::pq::reflection::NotVerifiedParams> res;
    promise<std::map<uint64_t, std::pair<std::string, std::string>>> prom;
    handler->HandleNotVerifiedParams(mail_errors::error_code(errors::sharpei_error), {}, prom);
    future<std::map<uint64_t, std::pair<std::string, std::string>>> f(prom);
    ASSERT_TRUE(f.has_exception());
}

TEST(MultiuserRulesHandler, GetUsersConnInfos) {
    const std::vector<uint64_t> uids{12, 33, 44};
    auto handler = boost::make_shared< ::furita::pq::MultiuserRulesHandler>(uids);
    sharpei::client::Shard shard1, shard2;
    shard1.id = "shard1";
    shard2.id = "shard2";
    auto sharpeiMock = std::make_shared<MockSharpeiClient>();
    ON_CALL(*sharpeiMock, asyncGetConnInfo(
                              _,
                              _))
        .WillByDefault(Invoke(
            [&](const ::sharpei::client::ResolveParams& resolveParams, MockSharpeiClient::AsyncHandler handler) {
                if (resolveParams.uid == "12") {
                    handler({}, shard1);
                } else if (resolveParams.uid == "33") {
                    handler({}, shard2);
                } else if (resolveParams.uid == "44") {
                    handler(mail_errors::error_code(errors::sharpei_error), shard1);
                }
            }));
    handler->GetUsersConnInfos(uids, sharpeiMock);
    const auto connInfo1 = handler->users_conn_infos[12];
    const auto connInfo2 = handler->users_conn_infos[33];
    const auto connInfo3 = handler->users_conn_infos[44];
    ASSERT_EQ(connInfo1.which(), 0);
    ASSERT_EQ(connInfo2.which(), 0);
    ASSERT_EQ(connInfo3.which(), 1);
    ASSERT_EQ(boost::get<std::string>(connInfo1), "shard1");
    ASSERT_EQ(boost::get<std::string>(connInfo2), "shard2");
    ASSERT_EQ(boost::get<mail_errors::error_code>(connInfo3).message(), "Failed get conninfo");
}

TEST(MultiuserRulesHandler, SplitUsersByShards) {
    const std::vector<uint64_t> uids{12, 33, 44, 55};
    auto handler = boost::make_shared< ::furita::pq::MultiuserRulesHandler>(uids);
    const std::string shard1 = "shard1", shard2 = "shard2";
    handler->users_conn_infos[12] = shard1;
    handler->users_conn_infos[33] = shard2;
    handler->users_conn_infos[44] = mail_errors::error_code(errors::sharpei_error);
    handler->users_conn_infos[55] = shard1;
    auto shard_to_uids = handler->SplitUsersByShards();
    ASSERT_NE(shard_to_uids.find(shard1), shard_to_uids.end());
    ASSERT_NE(shard_to_uids.find(shard2), shard_to_uids.end());
    const auto& shard1_uids = shard_to_uids[shard1];
    const auto& shard2_uids = shard_to_uids[shard2];
    ASSERT_NE(std::find(shard1_uids.begin(), shard1_uids.end(), 12), shard1_uids.end());
    ASSERT_NE(std::find(shard1_uids.begin(), shard1_uids.end(), 55), shard1_uids.end());
    ASSERT_NE(std::find(shard1_uids.begin(), shard1_uids.end(), 33), shard2_uids.end());
}

TEST(MultiuserRulesHandler, GetRulesHandler) {
    using boost::none;

    const std::vector<uint64_t> uids{12, 33, 44, 55};
    auto handler = boost::make_shared< ::furita::pq::MultiuserRulesHandler>(uids);
    handler->requests_by_rules_count = 3;
    promise<boost::unordered_map<uint64_t, ::furita::pq::RulesResult>> prom;
    std::vector< ::furita::pq::reflection::UnionRuleConditionAction> res;
    res.emplace_back(CreateUnionWithRule(12, 22, "rname1", true, -11, false, 12812881293, "user"));
    res.emplace_back(CreateUnionWithAction(12, 22, 111, "op4", "param4", true));
    res.emplace_back(CreateUnionWithCondition(12, 22, "flag", "virus", "pat2", "contains", "and", false));
    res.emplace_back(CreateUnionWithCondition(12, 22, "header", "spam", "pat3", "matches", "or", false));

    res.emplace_back(CreateUnionWithRule(12, 23, "rname2", true, 1, true, 12812881294, "user"));
    res.emplace_back(CreateUnionWithAction(12, 23, 113, "op1", "param1", true));

    res.emplace_back(CreateUnionWithRule(33, 25, "rname3", true, 11, false, 12812881295, "system"));
    res.emplace_back(CreateUnionWithCondition(33, 25, "type", "list", "pat3", "exists", "or", false));
    handler->GetRulesHandler({}, res, prom, {12, 33});

    res.clear();
    res.emplace_back(CreateUnionWithRule(44, 29, "rname4", true, 19, true, 12812881208, "forward"));
    res.emplace_back(CreateUnionWithAction(44, 29, 110, "op3", "param3", false));
    handler->GetRulesHandler({}, res, prom, {44});

    handler->GetRulesHandler(mail_errors::error_code(errors::pgg_error), {}, prom, {55});

    future<boost::unordered_map<uint64_t, ::furita::pq::RulesResult>> f(prom);
    auto result = f.get();
    ASSERT_EQ(result[12].which(), 0);
    auto rulesResult = *boost::get<rules::rule_list_ptr>(result[12]);
    ASSERT_EQ(rulesResult.size(), 2u);
    std::sort(rulesResult.begin(), rulesResult.end(), [](rules::rule_ptr r1, rules::rule_ptr r2) {
        return r1->id < r2->id;
    });
    CheckRule(rulesResult[0], 12, 22, "rname1", true, -11, false, 12812881293, "user");

    auto acts = *rulesResult[0]->actions;
    ASSERT_EQ(acts.size(), 1u);
    CheckAction(acts[0], 22, 111, "op4", "param4", true);

    auto conds = *rulesResult[0]->conditions;
    std::sort(conds.begin(), conds.begin(), [](rules::condition_ptr c1, rules::condition_ptr c2) {
        return c1->field_type < c2->field_type;
    });
    ASSERT_EQ(conds.size(), 2u);
    CheckCondition(conds[0], 22, "flag", "virus", "pat2", "contains", "and", false);
    CheckCondition(conds[1], 22, "header", "spam", "pat3", "matches", "or", false);

    CheckRule(rulesResult[1], 12, 23, "rname2", true, 1, true, 12812881294, "user");
    ASSERT_TRUE(rulesResult[1]->conditions->empty());
    acts = *rulesResult[1]->actions;
    ASSERT_EQ(acts.size(), 1u);
    CheckAction(acts[0], 23, 113, "op1", "param1", true);

    ASSERT_EQ(result[33].which(), 0);
    rulesResult = *boost::get<rules::rule_list_ptr>(result[33]);
    ASSERT_EQ(rulesResult.size(), 1u);
    CheckRule(rulesResult[0], 33, 25, "rname3", true, 11, false, 12812881295, "system");
    conds = *rulesResult[0]->conditions;
    ASSERT_EQ(conds.size(), 1u);
    CheckCondition(conds[0], 25, "type", "list", "pat3", "exists", "or", false);

    ASSERT_EQ(result[44].which(), 0);
    rulesResult = *boost::get<rules::rule_list_ptr>(result[44]);
    ASSERT_EQ(rulesResult.size(), 1u);
    CheckRule(rulesResult[0], 44, 29, "rname4", true, 19, true, 12812881208, "forward");
    acts = *rulesResult[0]->actions;
    ASSERT_EQ(acts.size(), 1u);
    CheckAction(acts[0], 29, 110, "op3", "param3", false);

    ASSERT_EQ(result[55].which(), 1);
    ASSERT_EQ(boost::get<pgg::error_code>(result[55]).message(), "pgg failed");
}

}
