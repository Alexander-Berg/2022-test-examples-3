#include <mail/notsolitesrv/src/rules/domain/util/tupita.h>

#include <mail/notsolitesrv/tests/unit/util/tupita.h>

#include <gtest/gtest.h>

#include <vector>

namespace {

using NNotSoLiteSrv::NFurita::TFuritaDomainRule;
using NNotSoLiteSrv::NMetaSaveOp::TRequest;
using NNotSoLiteSrv::NRules::MakeMatchedDomainRuleIndices;
using NNotSoLiteSrv::NRules::MakeTupitaQuery;
using NNotSoLiteSrv::NRules::MakeTupitaUsers;
using NNotSoLiteSrv::NRules::MatchedDomainRuleIndicesCorrect;
using NNotSoLiteSrv::NRules::MatchedQueriesNumeric;
using NNotSoLiteSrv::NRules::TMatchedDomainRuleIndices;
using NNotSoLiteSrv::NTupita::TTupitaQuery;
using NNotSoLiteSrv::NTupita::TTupitaUser;
using NNotSoLiteSrv::TUid;

std::vector<TTupitaUser> MakeTestTupitaUsers(TUid uid) {
    return {{
        .Uid = uid,
        .Queries = {
            {.Id = "0", .Query = "Query0", .Stop = false},
            {.Id = "1", .Query = "Query1", .Stop = true}
        },
        .Spam = true
    }};
}

TRequest MakeTestRequest() {
    TRequest request;
    request.message.spam = true;
    return request;
}

std::vector<TFuritaDomainRule> MakeFuritaDomainRules() {
    return {{.Terminal = false, .ConditionQuery{"Query0"}}, {.Terminal = true, .ConditionQuery{"Query1"}}};
}

TEST(TestMakeTupitaQuery, must_make_tupita_query) {
    TTupitaQuery expectedTupitaQuery{.Id{"Id"}, .Query{"Query"}, .Stop = false};
    EXPECT_EQ(expectedTupitaQuery, MakeTupitaQuery(expectedTupitaQuery.Id,
        {.ConditionQuery = expectedTupitaQuery.Query}));
    EXPECT_EQ(expectedTupitaQuery, MakeTupitaQuery(expectedTupitaQuery.Id,
        {.Terminal = expectedTupitaQuery.Stop, .ConditionQuery = expectedTupitaQuery.Query}));
    expectedTupitaQuery.Stop = true;
    EXPECT_EQ(expectedTupitaQuery, MakeTupitaQuery(expectedTupitaQuery.Id,
        {.Terminal = expectedTupitaQuery.Stop, .ConditionQuery = expectedTupitaQuery.Query}));
}

TEST(TestMakeTupitaUsers, must_make_tupita_users) {
    const TUid uid{1};
    EXPECT_EQ(MakeTestTupitaUsers(uid), MakeTupitaUsers(MakeTestRequest(), uid, MakeFuritaDomainRules()));
}

TEST(TestMatchedQueriesNumeric, must_be_able_to_detect_incorrect_queries) {
    EXPECT_FALSE(MatchedQueriesNumeric({"0", "1a", "2"}));
    EXPECT_FALSE(MatchedQueriesNumeric({"0", "1", ""}));
    EXPECT_TRUE(MatchedQueriesNumeric({"0", "1", "2"}));
}

TEST(TestMakeMatchedDomainRuleIndices, must_make_matched_domain_rule_indices) {
    EXPECT_EQ((TMatchedDomainRuleIndices{0, 1, 2}), MakeMatchedDomainRuleIndices({"2", "2", "0", "1", "1"}));
}

TEST(TestMatchedDomainRuleIndicesCorrect, must_be_able_to_detect_incorrect_domain_rule_indices) {
    const auto ruleCount{3ull};
    EXPECT_FALSE(MatchedDomainRuleIndicesCorrect(ruleCount, TMatchedDomainRuleIndices{0, 1, 3}));
    EXPECT_TRUE(MatchedDomainRuleIndicesCorrect(ruleCount, TMatchedDomainRuleIndices{0, 1, 2}));
}

}
