#include <mail/notsolitesrv/src/meta_save_op/util/mdbsave.h>

#include <mail/notsolitesrv/tests/unit/util/email_address.h>
#include <mail/notsolitesrv/tests/unit/util/mdbsave.h>

#include <gtest/gtest.h>

#include <functional>

namespace {

using namespace NNotSoLiteSrv::NMetaSaveOp;

using NNotSoLiteSrv::TDeliveryId;
using NNotSoLiteSrv::THid;

TEST(TestRecipientSucceeded, for_rules_applier_response_unavailable_must_return_true) {
    EXPECT_TRUE(RecipientSucceeded({}, {}));
}

TEST(TestRecipientSucceeded, must_be_able_to_detect_recipient_succeeded) {
    EXPECT_FALSE(RecipientSucceeded("DeliveryId", {{{}, {"DeliveryId"}}}));
    EXPECT_TRUE(RecipientSucceeded("DeliveryId0", {{{}, {"DeliveryId1"}}}));
}

TEST(TestRecipientToSaveForAvailable, must_be_able_to_detect_recipient_to_save_for_available) {
    const TRecipientMap recipients{{"DeliveryId0", {}}, {"DeliveryId1", {}}, {"DeliveryId2", {}}};
    std::vector<TDeliveryId> failedRecipients{"DeliveryId0", "DeliveryId1"};
    EXPECT_TRUE(RecipientToSaveForAvailable(recipients, {{{}, failedRecipients}}));

    failedRecipients.emplace_back("DeliveryId2");
    EXPECT_FALSE(RecipientToSaveForAvailable(recipients, {{{}, failedRecipients}}));
}

TEST(TestMakeMdbSaveUser, must_make_mdb_save_user) {
    TUser user;
    user.uid = 3;
    user.suid = "Suid";
    const auto mdbSaveUser = MakeMdbSaveUser(user);
    EXPECT_EQ("3", mdbSaveUser.Uid);
    ASSERT_TRUE(mdbSaveUser.Suid);
    EXPECT_EQ("Suid", *mdbSaveUser.Suid);
}

TEST(TestMakeMdbSaveRequestLabel, must_make_mdb_save_request_label) {
    const NNotSoLiteSrv::NMdbSave::TRequestLabel expectedLabel{"Name", "Type"};
    EXPECT_EQ(expectedLabel, MakeMdbSaveRequestLabel({"Name", "Type"}));
}

TEST(TestMakeMdbSaveRequestLabels, for_labels_unavailable_must_return_empty_optional) {
    EXPECT_FALSE(MakeMdbSaveRequestLabels({}));
}

TEST(TestMakeMdbSaveRequestLabels, must_make_mdb_save_request_labels) {
    const NNotSoLiteSrv::NMdbSave::TRequestLabels expectedLabels{{"Name0", "Type0"}, {"Name1", "Type1"}};
    const auto requestLabels = MakeMdbSaveRequestLabels({{"Name0", "Type0"}, {"Name1", "Type1"}});
    ASSERT_TRUE(requestLabels);
    EXPECT_EQ(expectedLabels, *requestLabels);
}

TEST(TestMakeMdbSaveStorage, for_recipient_stid_unavailable_must_return_request_stid) {
    TRequest request;
    const TDeliveryId deliveryId{"DeliveryId"};
    request.recipients.emplace(deliveryId, TRecipient{});
    request.stid = "Stid";
    const auto storage = MakeMdbSaveStorage(deliveryId, request);
    EXPECT_EQ(request.stid, storage.Stid);
    EXPECT_FALSE(storage.Offset);
}

TEST(TestMakeMdbSaveStorage, for_recipient_stid_available_must_return_recipient_stid_and_offset) {
    TRequest request;
    const TDeliveryId deliveryId{"DeliveryId"};
    request.recipients[deliveryId].params.stid = "Stid";
    request.recipients[deliveryId].params.offset_diff = 8;
    const auto storage = MakeMdbSaveStorage(deliveryId, request);
    EXPECT_EQ(*request.recipients[deliveryId].params.stid, storage.Stid);
    ASSERT_TRUE(storage.Offset);
    EXPECT_EQ(*request.recipients[deliveryId].params.offset_diff, *storage.Offset);
}

TEST(TestMakeMdbSaveHeaders, must_make_mdb_save_headers) {
    TRequest request;
    request.envelope.received_date = 3;
    request.message.date = 4;
    request.message.subject = "Subject";
    request.message.message_id = "MessageId";
    request.message.reply_to = "ReplyTo";
    request.message.in_reply_to = "InReplyTo";
    request.message.from = {{"Local0", "Domain0", "DisplayName0"}, {"Local1", "Domain1", "DisplayName1"}};
    request.message.to = {{"Local2", "Domain2", "DisplayName2"}, {"Local3", "Domain3", "DisplayName3"}};
    request.message.cc = {{{"Local4", "Domain4", "DisplayName4"}, {"Local5", "Domain5", "DisplayName5"}}};
    const TDeliveryId deliveryId{"DeliveryId"};
    request.recipients[deliveryId].params.bcc = {{{"Local6", "Domain6", "DisplayName6"},
        {"Local7", "Domain7", "DisplayName7"}}};

    const auto headers = MakeMdbSaveHeaders(deliveryId, request);
    EXPECT_EQ(request.envelope.received_date, headers.RecievedDate);
    ASSERT_TRUE(headers.Date);
    EXPECT_EQ(request.message.date, *headers.Date);
    EXPECT_EQ(request.message.subject, headers.Subject);
    ASSERT_TRUE(headers.MsgId);
    EXPECT_EQ(request.message.message_id, *headers.MsgId);
    ASSERT_TRUE(headers.ReplyTo);
    EXPECT_EQ(*request.message.reply_to, *headers.ReplyTo);
    ASSERT_TRUE(headers.InReplyTo);
    EXPECT_EQ(*request.message.in_reply_to, *headers.InReplyTo);
    const std::vector<NNotSoLiteSrv::TEmailAddress> expectedFrom{{"Local0", "Domain0", "DisplayName0"},
        {"Local1", "Domain1", "DisplayName1"}};
    EXPECT_EQ(expectedFrom, headers.From);
    const std::vector<NNotSoLiteSrv::TEmailAddress> expectedTo{{"Local2", "Domain2", "DisplayName2"},
        {"Local3", "Domain3", "DisplayName3"}};
    EXPECT_EQ(expectedTo, headers.To);
    const std::vector<NNotSoLiteSrv::TEmailAddress> expectedCc{{"Local4", "Domain4", "DisplayName4"},
        {"Local5", "Domain5", "DisplayName5"}};
    ASSERT_TRUE(headers.Cc);
    EXPECT_EQ(expectedCc, *headers.Cc);
    const std::vector<NNotSoLiteSrv::TEmailAddress> expectedBcc{{"Local6", "Domain6", "DisplayName6"},
        {"Local7", "Domain7", "DisplayName7"}};
    ASSERT_TRUE(headers.Bcc);
    EXPECT_EQ(expectedBcc, *headers.Bcc);
}

TEST(TestMakeMdbSaveHeaders, for_cc_and_bcc_unavailable_must_return_empty_optionals) {
    TRequest request;
    const TDeliveryId deliveryId{"DeliveryId"};
    request.recipients.emplace(deliveryId, TRecipient{});
    const auto headers = MakeMdbSaveHeaders(deliveryId, request);
    EXPECT_FALSE(headers.Cc);
    EXPECT_FALSE(headers.Bcc);
}

TEST(TestMakeMdbSaveHeaders, for_cc_and_bcc_empty_must_return_empty_optionals) {
    TRequest request;
    request.message.cc = TEmailAddressList{};
    const TDeliveryId deliveryId{"DeliveryId"};
    request.recipients[deliveryId].params.bcc = TEmailAddressList{};
    const auto headers = MakeMdbSaveHeaders(deliveryId, request);
    EXPECT_FALSE(headers.Cc);
    EXPECT_FALSE(headers.Bcc);
}

TEST(TestMakeMdbSaveAttachment, must_make_mdb_save_attachment) {
    const THid hid{"0.1.2"};
    TAttachment attachment;
    attachment.name = "Name";
    attachment.type = "Type";
    attachment.size = 3;
    const auto mdbSaveAttachment = MakeMdbSaveAttachment(hid, attachment);
    EXPECT_EQ(hid, mdbSaveAttachment.Hid);
    EXPECT_EQ(attachment.name, mdbSaveAttachment.Name);
    EXPECT_EQ(attachment.type, mdbSaveAttachment.Type);
    EXPECT_EQ(attachment.size, mdbSaveAttachment.Size);
}

TEST(TestMakeMdbSaveAttachments, for_attachments_unavailable_must_return_empty_optional) {
    TMessage message;
    EXPECT_FALSE(MakeMdbSaveAttachments(message));
    message.attachments = TAttachmentMap{};
    EXPECT_FALSE(MakeMdbSaveAttachments(message));
}

TEST(TestMakeMdbSaveAttachments, must_make_mdb_save_attachments) {
    const auto size = 3u;
    std::vector<NNotSoLiteSrv::NMdbSave::TAttachment> expectedAttachments{
        {"0.1.2", "Name0", "Type0", size}, {"1.2.3", "Name1", "Type1", size + 1}};

    TMessage message;
    message.attachments = TAttachmentMap{};
    for (const auto& expectedAttachmentWrapper: {std::cref(expectedAttachments[1]),
        std::cref(expectedAttachments[0])})
    {
        auto& expectedAttachment = expectedAttachmentWrapper.get();
        auto& attachment = (*message.attachments)[expectedAttachment.Hid];
        attachment.name = expectedAttachment.Name;
        attachment.type = expectedAttachment.Type;
        attachment.size = expectedAttachment.Size;
    }

    const auto attachments = MakeMdbSaveAttachments(message);
    ASSERT_TRUE(attachments);
    EXPECT_EQ(expectedAttachments, *attachments);
}

TEST(TestMakeMdbSaveMimePart, must_make_mdb_save_mime_part) {
    NNotSoLiteSrv::NMdbSave::TMimePart expectedPart;
    expectedPart.Hid = "0.1.2";
    expectedPart.ContentType = "ContentType";
    expectedPart.ContentSubtype = "ContentSubtype";
    expectedPart.Boundary = "Boundary";
    expectedPart.Name = "Name";
    expectedPart.Charset = "Charset";
    expectedPart.Encoding = "Encoding";
    expectedPart.ContentDisposition = "ContentDisposition";
    expectedPart.FileName = "FileName";
    expectedPart.ContentId = "ContentId";
    expectedPart.Offset = 3;
    expectedPart.Length = 4;

    TPart part;
    part.content_type = expectedPart.ContentType;
    part.content_subtype = expectedPart.ContentSubtype;
    part.charset = *expectedPart.Charset;
    part.encoding = *expectedPart.Encoding;
    part.offset = *expectedPart.Offset;
    part.length = *expectedPart.Length;
    part.boundary = *expectedPart.Boundary;
    part.name = *expectedPart.Name;
    part.content_disposition = *expectedPart.ContentDisposition;
    part.file_name = *expectedPart.FileName;
    part.content_id = *expectedPart.ContentId;
    EXPECT_EQ(expectedPart, MakeMdbSaveMimePart(expectedPart.Hid, part));
}

TEST(TestMakeMdbSaveMimeParts, for_parts_unavailable_must_return_empty_optional) {
    EXPECT_FALSE(MakeMdbSaveMimeParts({}));
}

TEST(TestMakeMdbSaveMimeParts, must_make_mdb_save_mime_parts) {
    std::vector<NNotSoLiteSrv::NMdbSave::TMimePart> expectedParts {
        {
            .Hid = "0.1.2",
            .ContentType = "ContentType0",
            .ContentSubtype = "ContentSubtype0",
            .Boundary = "Boundary0",
            .Name = "Name0",
            .Charset = "Charset0",
            .Encoding = "Encoding0",
            .ContentDisposition = "ContentDisposition0",
            .FileName = "FileName0",
            .ContentId = "ContentId0",
            .Offset = 3,
            .Length = 4
        }, {
            .Hid = "1.2.3",
            .ContentType = "ContentType1",
            .ContentSubtype = "ContentSubtype1",
            .Boundary = "Boundary1",
            .Name = "Name1",
            .Charset = "Charset1",
            .Encoding = "Encoding1",
            .ContentDisposition = "ContentDisposition1",
            .FileName = "FileName1",
            .ContentId = "ContentId1",
            .Offset = 5,
            .Length = 6
        }
    };

    TPartMap map;
    for (const auto& expectedPartWrapper : {std::cref(expectedParts[1]), std::cref(expectedParts[0])}) {
        auto& expectedPart = expectedPartWrapper.get();
        auto& part = map[expectedPart.Hid];
        part.content_type = expectedPart.ContentType;
        part.content_subtype = expectedPart.ContentSubtype;
        part.charset = *expectedPart.Charset;
        part.encoding = *expectedPart.Encoding;
        part.offset = *expectedPart.Offset;
        part.length = *expectedPart.Length;
        part.boundary = *expectedPart.Boundary;
        part.name = *expectedPart.Name;
        part.content_disposition = *expectedPart.ContentDisposition;
        part.file_name = *expectedPart.FileName;
        part.content_id = *expectedPart.ContentId;
    }

    const auto parts = MakeMdbSaveMimeParts(map);
    ASSERT_TRUE(parts);
    EXPECT_EQ(expectedParts, *parts);
}

TEST(TestMakeMdbSaveThreadInfo, must_make_mdb_save_thread_info) {
    NNotSoLiteSrv::NMdbSave::TThreadInfo expectedThreadInfo;
    expectedThreadInfo.Hash.Namespace = "Namespace";
    expectedThreadInfo.Hash.Value = "Value";
    expectedThreadInfo.Limits.Days = 3;
    expectedThreadInfo.Limits.Count = 4;
    expectedThreadInfo.Rule = "Rule";
    expectedThreadInfo.ReferenceHashes = {"ReferenceHashe0", "ReferenceHashe1"};
    expectedThreadInfo.MessageIds = {"MessageId0", "MessageId1"};
    expectedThreadInfo.InReplyToHash = "InReplyToHash";
    expectedThreadInfo.MessageIdHash = "MessageIdHash";
    NNotSoLiteSrv::NMthr::TThreadInfo threadInfo;
    threadInfo.Hash.Namespace = expectedThreadInfo.Hash.Namespace;
    threadInfo.Hash.Value = expectedThreadInfo.Hash.Value;
    threadInfo.Limits.Days = expectedThreadInfo.Limits.Days;
    threadInfo.Limits.Count = expectedThreadInfo.Limits.Count;
    threadInfo.Rule = expectedThreadInfo.Rule;
    threadInfo.ReferenceHashes = expectedThreadInfo.ReferenceHashes;
    threadInfo.MessageIds = expectedThreadInfo.MessageIds;
    threadInfo.InReplyToHash = expectedThreadInfo.InReplyToHash;
    threadInfo.MessageIdHash = expectedThreadInfo.MessageIdHash;
    EXPECT_EQ(expectedThreadInfo, MakeMdbSaveThreadInfo(threadInfo));
}

TEST(TestMakeMdbSaveMessage, must_make_mdb_save_thread_info) {
    TRequest request;
    const TDeliveryId deliveryId{"DeliveryId"};
    request.recipients[deliveryId].params.lids = {"Lid0", "Lid1"};
    request.recipients[deliveryId].params.label_symbols = {"LabelSymbol0", "LabelSymbol1"};
    request.recipients[deliveryId].params.tab = "Tab";
    request.recipients[deliveryId].params.old_mid = "OldMid";
    request.recipients[deliveryId].params.external_imap_id = 3;
    request.message.size = 4;
    const std::string firstline{"Firstline"};
    const auto mdbSaveMessage = MakeMdbSaveMessage(deliveryId, request, {{firstline}}, {});
    ASSERT_TRUE(mdbSaveMessage.OldMid);
    EXPECT_EQ(*request.recipients[deliveryId].params.old_mid, *mdbSaveMessage.OldMid);
    ASSERT_TRUE(mdbSaveMessage.ExtImapId);
    EXPECT_EQ(*request.recipients[deliveryId].params.external_imap_id, static_cast<uint64_t>(
        *mdbSaveMessage.ExtImapId));
    EXPECT_EQ(firstline, mdbSaveMessage.Firstline);
    ASSERT_TRUE(mdbSaveMessage.Size);
    EXPECT_EQ(request.message.size, *mdbSaveMessage.Size);
    ASSERT_TRUE(mdbSaveMessage.Lids);
    EXPECT_EQ(request.recipients[deliveryId].params.lids, *mdbSaveMessage.Lids);
    ASSERT_TRUE(mdbSaveMessage.LabelSymbols);
    EXPECT_EQ(request.recipients[deliveryId].params.label_symbols, *mdbSaveMessage.LabelSymbols);
    ASSERT_TRUE(mdbSaveMessage.Tab);
    EXPECT_EQ(request.recipients[deliveryId].params.tab, *mdbSaveMessage.Tab);
}

TEST(TestMakeMdbSaveRequestDestinationFolder,
    for_rules_applier_response_unavailable_must_return_empty_optional)
{
    const std::optional<NNotSoLiteSrv::NRulesApplier::TRulesApplierResponse> emptyRulesApplierResponse;
    EXPECT_FALSE(MakeMdbSaveRequestDestinationFolder({}, emptyRulesApplierResponse));
}

TEST(TestMakeMdbSaveRequestDestinationFolder, for_applied_rules_unavailable_must_return_empty_optional) {
    EXPECT_FALSE(MakeMdbSaveRequestDestinationFolder("DeliveryId",
        NNotSoLiteSrv::NRulesApplier::TRulesApplierResponse{}));
}

TEST(TestMakeMdbSaveRequestDestinationFolder, for_empty_fid_and_path_must_return_empty_optional) {
    NNotSoLiteSrv::NRulesApplier::TRulesApplierResponse response;
    const TDeliveryId deliveryId{"DeliveryId"};
    response.AppliedRules.emplace(deliveryId, NNotSoLiteSrv::NRulesApplier::TAppliedRules{});
    EXPECT_FALSE(MakeMdbSaveRequestDestinationFolder(deliveryId, response));
}

TEST(TestMakeMdbSaveRequestDestinationFolder, must_make_mdb_save_request_destination_folder) {
    auto expectedRequestFolderWithFid = std::make_optional(NNotSoLiteSrv::NMdbSave::TRequestFolder{});
    const std::string fid{"Fid"};
    expectedRequestFolderWithFid->Fid = fid;
    NNotSoLiteSrv::NRulesApplier::TRulesApplierResponse responseWithFid;
    const TDeliveryId deliveryId{"DeliveryId"};
    responseWithFid.AppliedRules[deliveryId].DestFolder.Fid = fid;
    EXPECT_EQ(expectedRequestFolderWithFid, MakeMdbSaveRequestDestinationFolder(deliveryId, responseWithFid));

    auto expectedRequestFolderWithPath = std::make_optional(NNotSoLiteSrv::NMdbSave::TRequestFolder{});
    expectedRequestFolderWithPath->Path = std::make_optional(NNotSoLiteSrv::NMdbSave::TPath{});
    const std::string path{"Path"};
    expectedRequestFolderWithPath->Path->Path = path;
    NNotSoLiteSrv::NRulesApplier::TRulesApplierResponse responseWithPath;
    responseWithPath.AppliedRules[deliveryId].DestFolder.Path = path;
    EXPECT_EQ(expectedRequestFolderWithPath,
        MakeMdbSaveRequestDestinationFolder(deliveryId, responseWithPath));

    auto expectedRequestFolderWithFidAndPath = std::make_optional(NNotSoLiteSrv::NMdbSave::TRequestFolder{});
    expectedRequestFolderWithFidAndPath->Fid = fid;
    expectedRequestFolderWithFidAndPath->Path = std::make_optional(NNotSoLiteSrv::NMdbSave::TPath{});
    expectedRequestFolderWithFidAndPath->Path->Path = path;
    NNotSoLiteSrv::NRulesApplier::TRulesApplierResponse responseWithFidAndPath;
    responseWithFidAndPath.AppliedRules[deliveryId].DestFolder.Fid = fid;
    responseWithFidAndPath.AppliedRules[deliveryId].DestFolder.Path = path;
    EXPECT_EQ(expectedRequestFolderWithFidAndPath,
        MakeMdbSaveRequestDestinationFolder(deliveryId, responseWithFidAndPath));
}

TEST(TestMakeMdbSaveRequestOriginalFolder, for_params_folder_unavailable_must_return_empty_optional) {
    EXPECT_FALSE(MakeMdbSaveRequestOriginalFolder({}));
}

TEST(TestMakeMdbSaveRequestOriginalFolder,
    for_params_folder_fid_and_path_unavailable_must_return_empty_optional)
{
    TParams params;
    params.folder = TFolder{};
    EXPECT_FALSE(MakeMdbSaveRequestOriginalFolder(params));
}

TEST(TestMakeMdbSaveRequestOriginalFolder, must_make_mdb_save_request_original_folder)
{
    auto expectedRequestFolderWithFid = std::make_optional(NNotSoLiteSrv::NMdbSave::TRequestFolder{});
    const std::string fid{"Fid"};
    expectedRequestFolderWithFid->Fid = fid;
    TParams paramsWithFid;
    paramsWithFid.folder = TFolder{};
    paramsWithFid.folder->fid = fid;
    EXPECT_EQ(expectedRequestFolderWithFid, MakeMdbSaveRequestOriginalFolder(paramsWithFid));

    TParams paramsWithPath;
    paramsWithPath.folder = TFolder{};
    paramsWithPath.folder->path = TFolderPath{};
    const std::string path{"Path"};
    paramsWithPath.folder->path->path = path;
    auto expectedRequestFolderWithPath = std::make_optional(NNotSoLiteSrv::NMdbSave::TRequestFolder{});
    expectedRequestFolderWithPath->Path = NNotSoLiteSrv::NMdbSave::TPath{};
    expectedRequestFolderWithPath->Path->Path = path;
    expectedRequestFolderWithPath->Path->Delimeter = paramsWithPath.folder->path->delim;
    EXPECT_EQ(expectedRequestFolderWithPath, MakeMdbSaveRequestOriginalFolder(paramsWithPath));

    TParams paramsWithFidAndPath;
    paramsWithFidAndPath.folder = TFolder{};
    paramsWithFidAndPath.folder->fid = fid;
    paramsWithFidAndPath.folder->path = TFolderPath{};
    paramsWithFidAndPath.folder->path->path = path;
    auto expectedRequestFolderWithFidAndPath = std::make_optional(NNotSoLiteSrv::NMdbSave::TRequestFolder{});
    expectedRequestFolderWithFidAndPath->Fid = fid;
    expectedRequestFolderWithFidAndPath->Path = NNotSoLiteSrv::NMdbSave::TPath{};
    expectedRequestFolderWithFidAndPath->Path->Path = path;
    expectedRequestFolderWithFidAndPath->Path->Delimeter = paramsWithFidAndPath.folder->path->delim;
    EXPECT_EQ(expectedRequestFolderWithFidAndPath, MakeMdbSaveRequestOriginalFolder(paramsWithFidAndPath));
}

TEST(TestMakeMdbSaveActionDuplicates, must_make_mdb_save_action_duplicates) {
    NNotSoLiteSrv::NMdbSave::TDuplicates expectedDuplicates;
    expectedDuplicates.Ignore = true;
    expectedDuplicates.Remove = true;
    TParams params;
    params.ignore_duplicates = expectedDuplicates.Ignore;
    params.remove_duplicates = expectedDuplicates.Remove;
    EXPECT_EQ(expectedDuplicates, MakeMdbSaveActionDuplicates(params));
}

TEST(TestMakeMdbSaveOriginalFolderActions, must_make_mdb_save_original_folder_actions) {
    TParams params;
    params.store_as_deleted = true;
    params.no_such_folder_action = ENoSuchFolderAction::Create;
    const std::string noSuchFolder{"create"};
    EXPECT_EQ((NNotSoLiteSrv::NMdbSave::TFolderActions{params.store_as_deleted, noSuchFolder}),
        MakeMdbSaveOriginalFolderActions(params));
}

TEST(TestMakeMdbSaveRulesApplierFolderActions,
    for_rules_applier_response_unavailable_must_return_empty_optional)
{
    const std::optional<NNotSoLiteSrv::NRulesApplier::TRulesApplierResponse> emptyRulesApplierResponse;
    EXPECT_FALSE(MakeMdbSaveRulesApplierFolderActions({}, emptyRulesApplierResponse));
}

TEST(TestMakeMdbSaveRulesApplierFolderActions, for_applied_rules_unavailable_must_return_empty_optional) {
    EXPECT_FALSE(MakeMdbSaveRulesApplierFolderActions("DeliveryId",
        NNotSoLiteSrv::NRulesApplier::TRulesApplierResponse{}));
}

TEST(TestMakeMdbSaveRulesApplierFolderActions, must_make_mdb_save_rules_applier_folder_actions) {
    NNotSoLiteSrv::NMdbSave::TFolderActions expectedFolderActions;
    expectedFolderActions.StoreAsDeleted = true;
    expectedFolderActions.NoSuchFolder = "create";
    NNotSoLiteSrv::NRulesApplier::TRulesApplierResponse response;
    const TDeliveryId deliveryId{"DeliveryId"};
    response.AppliedRules[deliveryId].StoreAsDeleted = expectedFolderActions.StoreAsDeleted;
    response.AppliedRules[deliveryId].NoSuchFolderAction = *expectedFolderActions.NoSuchFolder;
    const auto folderActions = MakeMdbSaveRulesApplierFolderActions(deliveryId, response);
    ASSERT_TRUE(folderActions);
    EXPECT_EQ(expectedFolderActions, *folderActions);
}

TEST(TestMakeMdbSaveAddedLids, for_rules_applier_response_unavailable_must_return_empty_optional)
{
    const std::optional<NNotSoLiteSrv::NRulesApplier::TRulesApplierResponse> emptyRulesApplierResponse;
    EXPECT_FALSE(MakeMdbSaveAddedLids({}, emptyRulesApplierResponse));
}

TEST(TestMakeMdbSaveAddedLids, must_make_mdb_save_added_lids)
{
    NNotSoLiteSrv::NRulesApplier::TRulesApplierResponse response;
    const TDeliveryId deliveryId{"DeliveryId"};
    const NNotSoLiteSrv::NMdbSave::TRequestAddedLids expectedLids{"Lid0", "Lid1", "Lid2"};
    response.AppliedRules[deliveryId].Lids = expectedLids;
    const auto lids = MakeMdbSaveAddedLids(deliveryId, response);
    ASSERT_TRUE(lids);
    EXPECT_EQ(expectedLids, *lids);
}

TEST(TestMakeMdbSaveAddedSymbols, for_rules_applier_response_unavailable_must_return_empty_optional)
{
    const std::optional<NNotSoLiteSrv::NRulesApplier::TRulesApplierResponse> emptyRulesApplierResponse;
    EXPECT_FALSE(MakeMdbSaveAddedSymbols({}, emptyRulesApplierResponse));
}

TEST(TestMakeMdbSaveAddedSymbols, must_make_mdb_save_added_symbols)
{
    NNotSoLiteSrv::NRulesApplier::TRulesApplierResponse response;
    const TDeliveryId deliveryId{"DeliveryId"};
    const NNotSoLiteSrv::NMdbSave::TRequestAddedSymbols expectedSymbols{"Symbol0", "Symbol1", "Symbol2"};
    response.AppliedRules[deliveryId].LabelSymbols = expectedSymbols;
    const auto symbols = MakeMdbSaveAddedSymbols(deliveryId, response);
    ASSERT_TRUE(symbols);
    EXPECT_EQ(expectedSymbols, *symbols);
}

TEST(TestFindMdbSaveResponseRcptByDeliveryId, must_be_able_to_find_mdb_save_response_rcpt_by_delivery_id) {
    const TDeliveryId availableDeliveryId{"DeliveryId1"};
    const std::string expectedUid{"Uid1"};
    const std::vector<NNotSoLiteSrv::NMdbSave::TMdbSaveResponseRcptNode> rcpts{
        {"DeliveryId0", {.Uid = "Uid0"}}, {availableDeliveryId, {.Uid = expectedUid}}};
    const TDeliveryId unavailableDeliveryId{"DeliveryId2"};
    EXPECT_FALSE(FindMdbSaveResponseRcptByDeliveryId(unavailableDeliveryId, rcpts));

    const auto rcpt = FindMdbSaveResponseRcptByDeliveryId(availableDeliveryId, rcpts);
    ASSERT_TRUE(rcpt);
    EXPECT_EQ(expectedUid, rcpt->get().Uid);
}

}
