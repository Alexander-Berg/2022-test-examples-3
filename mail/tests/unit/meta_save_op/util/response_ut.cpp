#include <mail/notsolitesrv/src/meta_save_op/util/response.h>

#include <mail/notsolitesrv/tests/unit/util/meta_save_op.h>

#include <gtest/gtest.h>

#include <string>

namespace {

using namespace NNotSoLiteSrv::NMetaSaveOp;
using namespace std::string_literals;

using NNotSoLiteSrv::TDeliveryId;

TEST(TestMakeFailureStatusByDeliveryIdMap, must_make_failure_status_by_delivery_id_map) {
    const std::map<TDeliveryId, std::string> expectedMap{
        {"DeliveryId0", "temp error"}, {"DeliveryId1", "perm error"}, {"DeliveryId2", "temp error"}};
    const NNotSoLiteSrv::NRulesApplier::TRulesApplierResponse rulesApplierResponse{
        {}, {"DeliveryId0", "DeliveryId2"}};
    const NNotSoLiteSrv::NMdbSave::TMdbSaveResponse mdbSaveResponse{{
        {"DeliveryId1", {.Status = "perm error"}}, {"DeliveryId3", {.Status = "ok"}}}};
    EXPECT_EQ(expectedMap, MakeFailureStatusByDeliveryIdMap(rulesApplierResponse, mdbSaveResponse));
}

TEST(TestMakeRecipientResponseStatus, must_make_recipient_response_status) {
    EXPECT_EQ(EStatus::Ok, MakeRecipientResponseStatus("ok"));
    EXPECT_EQ(EStatus::TemporaryError, MakeRecipientResponseStatus("temp error"));
    EXPECT_EQ(EStatus::PermanentError, MakeRecipientResponseStatus("perm error"));
    EXPECT_EQ(EStatus::Unknown, MakeRecipientResponseStatus("unknown"));
}

TEST(TestMakeRecipientResponseReply, for_replies_unavailable_must_return_empty_optional) {
    EXPECT_FALSE(MakeRecipientResponseReply({}));
}

TEST(TestMakeRecipientResponseReply, must_make_recipient_response_reply) {
    const std::vector<TAutoReply> expectedReplies{{"Address0", "Body0"}, {"Address1", "Body1"}};
    const std::vector<NNotSoLiteSrv::NRulesApplier::TAutoreply> rulesApplierReplies {
        {expectedReplies[0].address, expectedReplies[0].body},
        {expectedReplies[1].address, expectedReplies[1].body}
    };

    const auto actualReplies{MakeRecipientResponseReply(rulesApplierReplies)};
    ASSERT_TRUE(actualReplies);
    EXPECT_EQ(expectedReplies, *actualReplies);
}

TEST(TestMakeRecipientResponseForward, for_forwards_unavailable_must_return_empty_optional) {
    EXPECT_FALSE(MakeRecipientResponseForward({}));
}

TEST(TestMakeRecipientResponseForward, must_make_recipient_response_forward) {
    const std::vector<TForward> expectedForwards{{"Forward0"}, {"Forward1"}};
    const std::vector<std::string> rulesApplierForwards{{expectedForwards[0].address},
        {expectedForwards[1].address}};
    const auto actualForwards{MakeRecipientResponseForward(rulesApplierForwards)};
    ASSERT_TRUE(actualForwards);
    EXPECT_EQ(expectedForwards, *actualForwards);
}

TEST(TestMakeRecipientResponseNotify, for_notifies_unavailable_must_return_empty_optional) {
    EXPECT_FALSE(MakeRecipientResponseNotify({}));
}

TEST(TestMakeRecipientResponseNotify, must_make_recipient_response_notify) {
    const std::vector<TNotify> expectedNotifies{{"Notify0"}, {"Notify1"}};
    const std::vector<std::string> rulesApplierNotifies{{expectedNotifies[0].address},
        {expectedNotifies[1].address}};
    const auto actualNotifies{MakeRecipientResponseNotify(rulesApplierNotifies)};
    ASSERT_TRUE(actualNotifies);
    EXPECT_EQ(expectedNotifies, *actualNotifies);
}

TEST(TestMakeMdbCommitResponseImapId, for_imap_id_unavailable_must_return_empty_optional) {
    EXPECT_FALSE(MakeMdbCommitResponseImapId({}));
}

TEST(TestMakeMdbCommitResponseImapId, must_make_mdb_commit_response_imap_id) {
    const auto expectedImapId{32u};
    EXPECT_EQ(expectedImapId, MakeMdbCommitResponseImapId(std::to_string(expectedImapId)));
}

TEST(TestMakeResolvedFolder, for_folder_unavailable_must_return_empty_optional) {
    EXPECT_FALSE(MakeResolvedFolder({}));
}

TEST(TestMakeResolvedFolder, must_make_resolved_folder) {
    const TResolvedFolder expectedFolder{.fid = "Fid", .name = "Name"s, .type = "Type"s, .type_code = 1};
    const NNotSoLiteSrv::NMdbSave::TResolvedFolder mdbSaveFolder{.Fid = expectedFolder.fid,
        .Name = *expectedFolder.name, .Type = *expectedFolder.type, .TypeCode = *expectedFolder.type_code};
    const auto actualFolder{MakeResolvedFolder(mdbSaveFolder)};
    ASSERT_TRUE(actualFolder);
    EXPECT_EQ(expectedFolder, *actualFolder);
}

TEST(TestMakeResolvedLabel, must_make_resolved_label) {
    const TResolvedLabel expectedLabel{"Lid", "Symbol"s};
    const NNotSoLiteSrv::NMdbSave::TResponseLabel mdbSaveLabel{.Lid = expectedLabel.lid,
        .Symbol = *expectedLabel.symbol};
    EXPECT_EQ(expectedLabel, MakeResolvedLabel(mdbSaveLabel));
}

TEST(TestMakeResolvedLabels, for_labels_unavailable_must_return_empty_optional) {
    EXPECT_FALSE(MakeResolvedLabels({}));
}

TEST(TestMakeResolvedLabels, must_make_resolved_labels) {
    const std::vector<TResolvedLabel> expectedLabels{{"Lid0", "Symbol0"s}, {"Lid1", "Symbol1"s}};
    const std::vector<NNotSoLiteSrv::NMdbSave::TResponseLabel> mdbSaveLabels {
        {.Lid = expectedLabels[0].lid, .Symbol = *expectedLabels[0].symbol},
        {.Lid = expectedLabels[1].lid, .Symbol = *expectedLabels[1].symbol}
    };

    const auto actualLabels{MakeResolvedLabels(mdbSaveLabels)};
    ASSERT_TRUE(actualLabels);
    EXPECT_EQ(expectedLabels, *actualLabels);
}

TEST(TestMakeMdbCommitResponse, for_mdb_save_result_unavailable_must_return_empty_optional) {
    const NNotSoLiteSrv::NMdbSave::TMdbSaveResult emptyMdbSaveResult;
    EXPECT_FALSE(MakeMdbCommitResponse({}, emptyMdbSaveResult));
}

TEST(TestMakeMdbCommitResponse, for_mdb_save_response_rcpt_unavailable_must_return_empty_optional) {
    const NNotSoLiteSrv::NMdbSave::TMdbSaveResponse mdbSaveResponse {
        .Rcpts = {
            {"DeliveryId0", {.Uid = "Uid"}}
        }
    };

    EXPECT_FALSE(MakeMdbCommitResponse("DeliveryId1", mdbSaveResponse));
}

TEST(TestMakeMdbCommitResponse, must_make_mdb_commit_response) {
    const TResolvedFolder expectedFolder{.fid = "Fid", .name = "Name"s, .type = "Type"s, .type_code = 1};
    const std::vector<TResolvedLabel> expectedLabels{{"Lid", "Symbol"s}};
    const TMdbCommitResponse expectedMdbCommitResponse {
        .uid = "Uid",
        .status = EStatus::Ok,
        .description = "Description"s,
        .mid = "Mid"s,
        .imap_id = 1,
        .duplicate = true,
        .tid = "Tid"s,
        .folder = expectedFolder,
        .labels = expectedLabels
    };

    const TDeliveryId deliveryId{"DeliveryId"};
    NNotSoLiteSrv::NMdbSave::TResolvedFolder mdbSaveFolder{.Fid = expectedFolder.fid,
        .Name = *expectedFolder.name, .Type = *expectedFolder.type, .TypeCode = *expectedFolder.type_code};
    std::vector<NNotSoLiteSrv::NMdbSave::TResponseLabel> mdbSaveLabels{{.Lid = expectedLabels[0].lid,
        .Symbol = *expectedLabels[0].symbol}};
    const NNotSoLiteSrv::NMdbSave::TMdbSaveResponse mdbSaveResponse{{{deliveryId, {
        .Uid = expectedMdbCommitResponse.uid,
        .Status = "ok",
        .Description = *expectedMdbCommitResponse.description,
        .Mid = *expectedMdbCommitResponse.mid,
        .ImapId = std::to_string(*expectedMdbCommitResponse.imap_id),
        .Tid = *expectedMdbCommitResponse.tid,
        .Duplicate = *expectedMdbCommitResponse.duplicate,
        .Folder = std::move(mdbSaveFolder),
        .Labels = std::move(mdbSaveLabels)
    }}}};

    const auto actualMdbCommitResponse{MakeMdbCommitResponse(deliveryId, mdbSaveResponse)};
    ASSERT_TRUE(actualMdbCommitResponse);
    EXPECT_EQ(expectedMdbCommitResponse, *actualMdbCommitResponse);
}

TEST(TestMakeMdbResponses, must_make_mdb_responses) {
    const TRecipientMap recipients{{"DeliveryId0", {}}, {"DeliveryId1", {{}, {.use_filters = false}}},
        {"DeliveryId2", {}}};
    const std::vector<std::string> expectedRuleIds{"RuleId2", "RuleId3"};
    const NNotSoLiteSrv::NRulesApplier::TRulesApplierResponse rulesApplierResponse {
        {{"DeliveryId1", {.RuleIds{"RuleId0", "RuleId1"}}}, {"DeliveryId2", {.RuleIds{expectedRuleIds}}}},
        {"DeliveryId0"}
    };

    const auto responses{MakeMdbResponses(recipients, rulesApplierResponse, {})};
    ASSERT_EQ(3u, responses.size());
    EXPECT_EQ(EStatus::TemporaryError, responses.at("DeliveryId0").status);
    EXPECT_EQ(EStatus::Ok, responses.at("DeliveryId1").status);
    EXPECT_FALSE(responses.at("DeliveryId1").rule_ids);
    EXPECT_EQ(EStatus::Ok, responses.at("DeliveryId2").status);
    ASSERT_TRUE(responses.at("DeliveryId2").rule_ids);
    EXPECT_EQ(expectedRuleIds, *responses.at("DeliveryId2").rule_ids);
}

}
