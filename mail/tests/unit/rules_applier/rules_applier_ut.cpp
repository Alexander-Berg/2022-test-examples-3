#include <mail/notsolitesrv/src/rules_applier/rules_applier.h>

#include <mail/notsolitesrv/tests/unit/util/furita.h>
#include <mail/notsolitesrv/tests/unit/util/rules_applier.h>

#include <gtest/gtest.h>

#include <algorithm>
#include <cstdint>
#include <string>
#include <utility>

namespace {

using NNotSoLiteSrv::EError;
using NNotSoLiteSrv::NFurita::TFuritaAction;
using NNotSoLiteSrv::NFurita::TFuritaRule;
using NNotSoLiteSrv::NMetaSaveOp::TFuritaClientResults;
using NNotSoLiteSrv::NMetaSaveOp::TTupitaClientResults;
using NNotSoLiteSrv::NRulesApplier::ApplyRule;
using NNotSoLiteSrv::NRulesApplier::ApplyRules;
using NNotSoLiteSrv::NRulesApplier::EApplyResult;
using NNotSoLiteSrv::NRulesApplier::FailedRecipient;
using NNotSoLiteSrv::NRulesApplier::MakeAppliedRules;
using NNotSoLiteSrv::NRulesApplier::MakeAutoreplyAddress;
using NNotSoLiteSrv::NRulesApplier::MakeMatchedRuleIdsByUid;
using NNotSoLiteSrv::NRulesApplier::MakeMatchedRulesByUid;
using NNotSoLiteSrv::NRulesApplier::ProcessApplyActionResult;
using NNotSoLiteSrv::NRulesApplier::TAppliedRules;
using NNotSoLiteSrv::NRulesApplier::TAutoreply;
using NNotSoLiteSrv::NRulesApplier::TMatchedRuleIdsByUid;
using NNotSoLiteSrv::NRulesApplier::TMatchedRulesByUid;
using NNotSoLiteSrv::NRulesApplier::TRecipient;
using NNotSoLiteSrv::NRulesApplier::TRulesApplierRequest;
using NNotSoLiteSrv::NRulesApplier::TRulesApplierResponse;
using NNotSoLiteSrv::TEmailAddress;
using NNotSoLiteSrv::TUid;

TRulesApplierRequest MakeRulesApplierRequest() {
    TEmailAddress emailAddress;
    emailAddress.Local = "local";
    emailAddress.Domain = "domain.net";

    TRecipient recipient;
    recipient.Uid = 1;

    TRulesApplierRequest request;
    request.Message.From = std::move(emailAddress);

    request.Recipients["DeliveryId1"] = recipient;

    recipient.Uid = 2;
    recipient.UseFilters = false;
    request.Recipients["DeliveryId2"] = recipient;

    recipient.Uid = 3;
    recipient.UseFilters = true;
    request.Recipients["DeliveryId3"] = recipient;

    recipient.Uid = 4;
    request.Recipients["DeliveryId4"] = recipient;

    recipient.Uid = 5;
    request.Recipients["DeliveryId5"] = recipient;

    recipient.Uid = 6;
    request.Recipients["DeliveryId6"] = std::move(recipient);

    return request;
}

TFuritaClientResults MakeFuritaClientResults(uint32_t stopRuleIndex = 1) {
    TFuritaClientResults results {
        {TUid{1}, {.Result{{.Rules {
            {
                .Id{"id1"},
                .Priority = 3,
                .Query{"query"},
                .Enabled = true,
                .Stop = false,
                .Actions {
                    {.Verified = true, .Parameter{"Lid1b"}, .Type{"movel"}},
                    {.Verified = true, .Parameter{"PL"}, .Type{"status"}},
                    {.Verified = true, .Parameter{"Forward1b"}, .Type{"forward"}},
                    {.Verified = true, .Parameter{"Forwardwithstore1b"}, .Type{"forwardwithstore"}},
                    {.Verified = true, .Parameter{"Body1b"}, .Type{"reply"}},
                    {.Verified = true, .Parameter{"Notify1b"}, .Type{"notify"}}
                }
            }, {
                .Id{"id2"},
                .Priority = 2,
                .Query{"query"},
                .Enabled = true,
                .Stop = false,
                .Actions {
                    {.Verified = true, .Parameter{""}, .Type{"delete"}},
                    {.Verified = true, .Parameter{"OQ"}, .Type{"status"}},
                    {.Verified = true, .Parameter{"Forwardwithstore1a"}, .Type{"forwardwithstore"}},
                    {.Verified = true, .Parameter{"Body1a"}, .Type{"reply"}},
                    {.Verified = true, .Parameter{"Notify1a"}, .Type{"notify"}}
                }
            }, {
                .Id{"id3"},
                .Priority = 1,
                .Query{"query"},
                .Enabled = true,
                .Stop = false,
                .Actions {
                    {.Verified = false, .Type{""}},
                    {.Verified = true, .Parameter{"Fid1a"}, .Type{"move"}},
                    {.Verified = true, .Parameter{"Lid1a"}, .Type{"movel"}},
                    {.Verified = true, .Parameter{"RO"}, .Type{"status"}},
                    {.Verified = true, .Parameter{"Unknown"}, .Type{"status"}},
                    {.Verified = true, .Parameter{"Forward1a"}, .Type{"forward"}},
                    {.Verified = true, .Parameter{""}, .Type{"unknown"}}
                }
            }
        }}}}},
        {TUid{2}, {.Result{{.Rules {
            {
                .Id{"id1"},
                .Priority = 1,
                .Query{"query"},
                .Enabled = true,
                .Stop = false,
                .Actions {
                    {.Verified = false, .Parameter{""}, .Type{""}},
                    {.Verified = true, .Parameter{"Fid2"}, .Type{"move"}},
                    {.Verified = true, .Parameter{"Lid2"}, .Type{"movel"}},
                    {.Verified = true, .Parameter{"RO"}, .Type{"status"}},
                    {.Verified = true, .Parameter{"OQ"}, .Type{"status"}},
                    {.Verified = true, .Parameter{"PL"}, .Type{"status"}},
                    {.Verified = true, .Parameter{"Unknown"}, .Type{"status"}},
                    {.Verified = true, .Parameter{"Forward2"}, .Type{"forward"}},
                    {.Verified = true, .Parameter{"Forwardwithstore2"}, .Type{"forwardwithstore"}},
                    {.Verified = true, .Parameter{"Body2"}, .Type{"reply"}},
                    {.Verified = true, .Parameter{"Notify2"}, .Type{"notify"}},
                    {.Verified = true, .Parameter{""}, .Type{"unknown"}}
                }
            }
        }}}}},
        {TUid{3}, {.ErrorCode{EError::HttpRetriesExceeded}}},
        {TUid{4}, {.Result{{.Rules {
            {
                .Id{"id1"},
                .Priority = 1,
                .Query{"query"},
                .Enabled = true,
                .Stop = false,
                .Actions {
                    {.Verified = false, .Parameter{""}, .Type{""}},
                    {.Verified = true, .Parameter{""}, .Type{"delete"}},
                    {.Verified = true, .Parameter{"Lid4"}, .Type{"movel"}},
                    {.Verified = true, .Parameter{"RO"}, .Type{"status"}},
                    {.Verified = true, .Parameter{"OQ"}, .Type{"status"}},
                    {.Verified = true, .Parameter{"PL"}, .Type{"status"}},
                    {.Verified = true, .Parameter{"Unknown"}, .Type{"status"}},
                    {.Verified = true, .Parameter{"Forward4"}, .Type{"forward"}},
                    {.Verified = true, .Parameter{"Forwardwithstore4"}, .Type{"forwardwithstore"}},
                    {.Verified = true, .Parameter{"Body4"}, .Type{"reply"}},
                    {.Verified = true, .Parameter{"Notify4"}, .Type{"notify"}},
                    {.Verified = true, .Parameter{""}, .Type{"unknown"}}
                }
            }
        }}}}},
        {TUid{5}, {.Result{{.Rules {
            {
                .Id{"id1"},
                .Priority = 1,
                .Query{"query"},
                .Enabled = true,
                .Stop = false,
                .Actions{{.Verified = true, .Parameter{""}, .Type{"delete"}}}
            }
        }}}}},
        {TUid{6}, {.Result{{.Rules {
            {
                .Id{"id1"},
                .Priority = 1,
                .Query{"query"},
                .Enabled = false,
                .Stop = false
            }
        }}}}}
    };

    results[TUid{1}].Result->Rules[stopRuleIndex].Stop = true;
    return results;
}

TTupitaClientResults MakeTupitaClientResults() {
    return {
        {TUid{1}, {.Result{{.Result{{.MatchedQueries{"id3", "id2"}}}}}}},
        {TUid{2}, {.Result{{.Result{{.MatchedQueries{"id1"}}}}}}},
        {TUid{4}, {.Result{{.Result{{.MatchedQueries{"id1"}}}}}}},
        {TUid{5}, {.ErrorCode{EError::HttpRetriesExceeded}}},
    };
}

TRulesApplierResponse MakeExpectedRulesApplierResponse() {
    TAutoreply autoreply;
    autoreply.Address = "local@domain.net";
    autoreply.Body = "Body1a";

    TAppliedRules appliedRules;
    appliedRules.Notifies = {{"Notify1a"}};
    appliedRules.Replies = {{autoreply}};
    appliedRules.Forwards = {{"Forward1a"}, {"Forwardwithstore1a"}};
    appliedRules.DestFolder.Fid = "Fid1a";
    appliedRules.Lids = {{"Lid1a"}};
    appliedRules.LabelSymbols = {{"seen_label"}, {"answered_label"}};
    appliedRules.RuleIds = {{"id3"}, {"id2"}};
    appliedRules.NoSuchFolderAction = "fallback_to_inbox";

    TRulesApplierResponse response;
    response.AppliedRules["DeliveryId1"] = appliedRules;

    autoreply.Body = "Body4";

    appliedRules.Notifies = {{"Notify4"}};
    appliedRules.Replies = {{std::move(autoreply)}};
    appliedRules.Forwards = {{"Forward4"}, {"Forwardwithstore4"}};
    appliedRules.DestFolder.Fid = {};
    appliedRules.DestFolder.Path = "\\trash";
    appliedRules.Lids = {{"Lid4"}};
    appliedRules.LabelSymbols = {{"seen_label"}, {"answered_label"}, {"forwarded_label"}};
    appliedRules.RuleIds = {{"id1"}};
    appliedRules.NoSuchFolderAction = "fail";

    response.AppliedRules["DeliveryId4"] = std::move(appliedRules);
    response.AppliedRules.emplace("DeliveryId6", TAppliedRules{});
    response.FailedRecipients = {"DeliveryId3", "DeliveryId5"};
    return response;
}

TMatchedRuleIdsByUid MakeExpectedMatchedRuleIdsByUid() {
    TMatchedRuleIdsByUid matchedRuleIdsByUid;
    matchedRuleIdsByUid[1] = {"id2", "id3"};
    matchedRuleIdsByUid[2] = {"id1"};
    matchedRuleIdsByUid[4] = {"id1"};
    return matchedRuleIdsByUid;
}

TFuritaRule MakeFuritaRuleWithoutActions() {
    TFuritaRule furitaRule;
    furitaRule.Id = "id3";
    furitaRule.Priority = 1;
    furitaRule.Query = "query";
    furitaRule.Enabled = true;
    furitaRule.Stop = false;
    return furitaRule;
}

TFuritaRule MakeFuritaRuleWithoutStore() {
    auto furitaRule = MakeFuritaRuleWithoutActions();

    TFuritaAction furitaAction;
    furitaAction.Verified = true;
    furitaAction.Parameter = "Forward1a";
    furitaAction.Type = "forward";
    furitaRule.Actions.emplace_back(std::move(furitaAction));

    return furitaRule;
}

TFuritaRule MakeFuritaRuleWithStore() {
    auto furitaRule = MakeFuritaRuleWithoutActions();

    TFuritaAction furitaAction{};
    furitaRule.Actions.emplace_back(furitaAction);

    furitaAction.Verified = true;
    furitaAction.Parameter = "Fid1a";
    furitaAction.Type = "move";
    furitaRule.Actions.emplace_back(furitaAction);

    furitaAction.Parameter = "Lid1a";
    furitaAction.Type = "movel";
    furitaRule.Actions.emplace_back(furitaAction);

    furitaAction.Parameter = "RO";
    furitaAction.Type = "status";
    furitaRule.Actions.emplace_back(furitaAction);

    furitaAction.Parameter = "Unknown";
    furitaAction.Type = "status";
    furitaRule.Actions.emplace_back(furitaAction);

    furitaAction.Parameter = "Forward1a";
    furitaAction.Type = "forward";
    furitaRule.Actions.emplace_back(furitaAction);

    furitaAction.Parameter->clear();
    furitaAction.Type = "unknown";
    furitaRule.Actions.emplace_back(std::move(furitaAction));

    return furitaRule;
}

TMatchedRulesByUid MakeExpectedMatchedRulesByUid() {
    auto rule{MakeFuritaRuleWithStore()};
    rule.Stop = true;

    return {{TUid{1}, {{std::move(rule)}}}};
}

TAppliedRules MakeExpectedAppliedRulesWithStore() {
    TAppliedRules appliedRules;
    appliedRules.Forwards = {{"Forward1a"}};
    appliedRules.DestFolder.Fid = "Fid1a";
    appliedRules.Lids = {{"Lid1a"}};
    appliedRules.LabelSymbols = {{"seen_label"}};
    appliedRules.RuleIds = {{"id3"}};
    appliedRules.NoSuchFolderAction = "fallback_to_inbox";

    return appliedRules;
}

TAppliedRules MakeExpectedAppliedRulesWithoutStore() {
    TAppliedRules appliedRules;
    appliedRules.Forwards = {{"Forward1a"}};
    appliedRules.DestFolder.Path = "\\inbox";
    appliedRules.StoreAsDeleted = true;
    appliedRules.RuleIds = {{"id3"}};

    return appliedRules;
}

TEST(TTestRulesApplier, apply_rules) {
    const auto rulesApplierResponse{ApplyRules(MakeRulesApplierRequest(), MakeFuritaClientResults(),
        MakeTupitaClientResults())};
    EXPECT_EQ(MakeExpectedRulesApplierResponse(), rulesApplierResponse);
}

TEST(TTestRulesApplier, for_furita_client_error_failed_recipient_must_return_true) {
    const TUid uid{1};
    EXPECT_TRUE(FailedRecipient(uid, {{uid, {.ErrorCode{EError::HttpRetriesExceeded}}}}, {}));
}

TEST(TTestRulesApplier, for_tupita_client_error_failed_recipient_must_return_true) {
    const TUid uid{1};
    EXPECT_TRUE(FailedRecipient(uid, {{uid, {}}}, {{uid, {.ErrorCode{EError::HttpRetriesExceeded}}}}));
}

TEST(TTestRulesApplier,
    for_furita_client_success_and_missing_tupita_client_result_failed_recipient_must_return_false)
{
    const TUid uid{1};
    EXPECT_FALSE(FailedRecipient(uid, {{uid, {}}}, {}));
}

TEST(TTestRulesApplier, for_furita_and_tupita_client_success_failed_recipient_must_return_false) {
    const TUid uid{1};
    EXPECT_FALSE(FailedRecipient(uid, {{uid, {}}}, {{uid, {}}}));
}

TEST(TTestRulesApplier, for_empty_tupita_client_results_must_make_empty_matched_rule_ids_by_uid) {
    const auto matchedRuleIdsByUid{MakeMatchedRuleIdsByUid({})};
    EXPECT_TRUE(matchedRuleIdsByUid.empty());
}

TEST(TTestRulesApplier, must_skip_missing_or_invalid_tupita_client_results) {
    const auto matchedRuleIdsByUid{MakeMatchedRuleIdsByUid({{TUid{1}, {}}, {TUid{2}, {.Result{{}}}}})};
    EXPECT_TRUE(matchedRuleIdsByUid.empty());
}

TEST(TTestRulesApplier, make_matched_rule_ids_by_uid) {
    EXPECT_EQ(MakeExpectedMatchedRuleIdsByUid(), MakeMatchedRuleIdsByUid(MakeTupitaClientResults()));
}

TEST(TTestRulesApplier, make_matched_rules_by_uid) {
    const auto stopRuleIndex{2u};
    const auto matchedRulesByUid{MakeMatchedRulesByUid(MakeFuritaClientResults(stopRuleIndex), {{TUid{1},
        {"id2", "id3"}}})};
    EXPECT_EQ(MakeExpectedMatchedRulesByUid(), matchedRulesByUid);
}

TEST(TTestRulesApplier, make_applied_rules_with_store) {
    const auto appliedRules = MakeAppliedRules({}, {MakeFuritaRuleWithStore()});
    EXPECT_EQ(MakeExpectedAppliedRulesWithStore(), appliedRules);
}

TEST(TTestRulesApplier, make_applied_rules_without_store) {
    const auto appliedRules = MakeAppliedRules({}, {MakeFuritaRuleWithoutStore()});
    EXPECT_EQ(MakeExpectedAppliedRulesWithoutStore(), appliedRules);
}

TEST(TTestRulesApplier, apply_rule) {
    TAppliedRules appliedRules;
    const auto applyResult = ApplyRule({}, MakeFuritaRuleWithStore(), appliedRules);
    EXPECT_TRUE(applyResult.appliedWithStore);
    EXPECT_TRUE(applyResult.appliedWithoutStore);
    EXPECT_EQ(MakeExpectedAppliedRulesWithStore(), appliedRules);
}

TEST(TTestRulesApplier, for_unverified_action_apply_action_must_return_empty_optional) {
    TAppliedRules appliedRules;
    const auto applyResult = ApplyAction({}, {}, appliedRules);
    EXPECT_FALSE(applyResult);
}

TEST(TTestRulesApplier, for_move_apply_action_must_set_applied_rules_fields_and_return_with_store_result) {
    TAppliedRules appliedRules;
    const auto verified = true;
    const std::string fid = "Fid";
    const auto applyResult = ApplyAction({}, {verified, fid, "move"}, appliedRules);
    EXPECT_TRUE(applyResult);
    EXPECT_EQ(EApplyResult::WithStore, *applyResult);
    EXPECT_EQ(fid, appliedRules.DestFolder.Fid);
    EXPECT_EQ("fallback_to_inbox", appliedRules.NoSuchFolderAction);
}

TEST(TTestRulesApplier, for_delete_apply_action_must_set_applied_rules_fields_and_return_with_store_result) {
    TAppliedRules appliedRules;
    const auto verified = true;
    const auto applyResult = ApplyAction({}, {verified, {}, "delete"}, appliedRules);
    EXPECT_TRUE(applyResult);
    EXPECT_EQ(EApplyResult::WithStore, *applyResult);
    EXPECT_EQ("\\trash", appliedRules.DestFolder.Path);
    EXPECT_EQ("fail", appliedRules.NoSuchFolderAction);
}

TEST(TTestRulesApplier, for_movel_apply_action_must_set_applied_rules_fields_and_return_with_store_result) {
    TAppliedRules appliedRules;
    const auto verified = true;
    const std::string lid = "Lid";
    const auto applyResult = ApplyAction({}, {verified, lid, "movel"}, appliedRules);
    EXPECT_TRUE(applyResult);
    EXPECT_EQ(EApplyResult::WithStore, *applyResult);
    EXPECT_EQ(1u, appliedRules.Lids.size());
    EXPECT_EQ(lid, appliedRules.Lids[0]);
}

TEST(TTestRulesApplier,
    for_correct_status_parameters_apply_action_must_set_applied_rules_fields_and_return_with_store_result)
{
    TAppliedRules appliedRules;
    const auto verified = true;

    auto applyResult = ApplyAction({}, {verified, "RO", "status"}, appliedRules);
    EXPECT_TRUE(applyResult);
    EXPECT_EQ(EApplyResult::WithStore, *applyResult);
    EXPECT_EQ(1u, appliedRules.LabelSymbols.size());
    EXPECT_EQ("seen_label", appliedRules.LabelSymbols[0]);

    applyResult = ApplyAction({}, {verified, "OQ", "status"}, appliedRules);
    EXPECT_TRUE(applyResult);
    EXPECT_EQ(EApplyResult::WithStore, *applyResult);
    EXPECT_EQ(2u, appliedRules.LabelSymbols.size());
    EXPECT_EQ("answered_label", appliedRules.LabelSymbols[1]);

    applyResult = ApplyAction({}, {verified, "PL", "status"}, appliedRules);
    EXPECT_TRUE(applyResult);
    EXPECT_EQ(EApplyResult::WithStore, *applyResult);
    EXPECT_EQ(3u, appliedRules.LabelSymbols.size());
    EXPECT_EQ("forwarded_label", appliedRules.LabelSymbols[2]);
}

TEST(TTestRulesApplier, for_incorrect_status_parameter_apply_action_must_return_with_store_result) {
    TAppliedRules appliedRules;
    const auto verified = true;
    auto applyResult = ApplyAction({}, {verified, "Unknown", "status"}, appliedRules);
    EXPECT_TRUE(applyResult);
    EXPECT_EQ(EApplyResult::WithStore, *applyResult);
    EXPECT_TRUE(appliedRules.LabelSymbols.empty());
}

TEST(TTestRulesApplier,
    for_forward_apply_action_must_set_applied_rules_fields_and_return_without_store_result)
{
    TAppliedRules appliedRules;
    const auto verified = true;
    const std::string forward = "Forward";
    const auto applyResult = ApplyAction({}, {verified, forward, "forward"}, appliedRules);
    EXPECT_TRUE(applyResult);
    EXPECT_EQ(EApplyResult::WithoutStore, *applyResult);
    EXPECT_EQ(1u, appliedRules.Forwards.size());
    EXPECT_EQ(forward, appliedRules.Forwards[0]);
}

TEST(TTestRulesApplier,
    for_forwardwithstore_apply_action_must_set_applied_rules_fields_and_return_with_store_result)
{
    TAppliedRules appliedRules;
    const auto verified = true;
    const std::string forward = "Forwardwithstore";
    const auto applyResult = ApplyAction({}, {verified, forward, "forwardwithstore"}, appliedRules);
    EXPECT_TRUE(applyResult);
    EXPECT_EQ(EApplyResult::WithStore, *applyResult);
    EXPECT_EQ(1u, appliedRules.Forwards.size());
    EXPECT_EQ(forward, appliedRules.Forwards[0]);
}

TEST(TTestRulesApplier, for_reply_apply_action_must_set_applied_rules_fields_and_return_with_store_result) {
    TAppliedRules appliedRules;
    const auto verified = true;
    std::string address = "Address";
    std::string body = "Body";
    const auto applyResult = ApplyAction({{}, {}, address}, {verified, body, "reply"}, appliedRules);
    EXPECT_TRUE(applyResult);
    EXPECT_EQ(EApplyResult::WithStore, *applyResult);
    EXPECT_EQ(1u, appliedRules.Replies.size());
    EXPECT_EQ((TAutoreply{std::move(address), std::move(body)}), appliedRules.Replies[0]);
}

TEST(TTestRulesApplier, for_notify_apply_action_must_set_applied_rules_fields_and_return_with_store_result) {
    TAppliedRules appliedRules;
    const auto verified = true;
    const std::string notify = "Notify";
    const auto applyResult = ApplyAction({}, {verified, notify, "notify"}, appliedRules);
    EXPECT_TRUE(applyResult);
    EXPECT_EQ(EApplyResult::WithStore, *applyResult);
    EXPECT_EQ(1u, appliedRules.Notifies.size());
    EXPECT_EQ(notify, appliedRules.Notifies[0]);
}

TEST(TTestRulesApplier, for_unknown_action_type_apply_action_must_return_empty_optional) {
    TAppliedRules appliedRules;
    const auto verified = true;
    const auto applyResult = ApplyAction({}, {verified, {}, "unknown"}, appliedRules);
    EXPECT_FALSE(applyResult);
}

TEST(TTestRulesApplier, for_empty_apply_result_process_apply_action_result_must_return_false) {
    const auto applyResult = ProcessApplyActionResult({});
    EXPECT_FALSE(applyResult.appliedWithStore);
    EXPECT_FALSE(applyResult.appliedWithoutStore);
}

TEST(TTestRulesApplier, for_with_store_apply_result_process_apply_action_result_must_return_with_store_set) {
    const auto applyResult = ProcessApplyActionResult({EApplyResult::WithStore});
    EXPECT_TRUE(applyResult.appliedWithStore);
    EXPECT_FALSE(applyResult.appliedWithoutStore);
}

TEST(TTestRulesApplier,
    for_without_store_apply_result_process_apply_action_result_must_return_without_store_set)
{
    const auto applyResult = ProcessApplyActionResult({EApplyResult::WithoutStore});
    EXPECT_FALSE(applyResult.appliedWithStore);
    EXPECT_TRUE(applyResult.appliedWithoutStore);
}

TEST(TTestRulesApplier, for_sender_set_make_autoreply_address_must_return_sender) {
    const std::string sender = "Sender";
    EXPECT_EQ(sender, MakeAutoreplyAddress({{}, sender, {}}));
}

TEST(TTestRulesApplier, for_from_set_make_autoreply_address_must_return_from) {
    const std::string Local = "local";
    const std::string Domain = "domain.net";
    EXPECT_EQ(Local + "@" + Domain, MakeAutoreplyAddress({{{Local, Domain, {}}}, "Sender", {}}));
}

TEST(TTestRulesApplier, for_reply_to_set_make_autoreply_address_must_return_reply_to) {
    const std::string replyTo = "ReplyTo";
    EXPECT_EQ(replyTo, MakeAutoreplyAddress({{{"local", "domain.net", {}}}, "Sender", replyTo}));
}

TEST(TTestRulesApplier, for_no_fields_set_make_autoreply_address_must_return_empty_string) {
    EXPECT_EQ((std::string{}), MakeAutoreplyAddress({}));
}

}
