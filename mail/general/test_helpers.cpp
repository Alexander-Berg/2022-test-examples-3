#include <types/labels.h>
#include <web/util/helpers.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

namespace {

using namespace ::testing;

using NMdb::TLabel;
using NMdb::NWeb::BuildAddresses;
using NMdb::NWeb::BuildFilterActions;
using NMdb::NWeb::BuildFolder;
using NMdb::NWeb::BuildHeaders;
using NMdb::NWeb::BuildLabels;
using NMdb::NWeb::BuildMimeParts;
using NMdb::NWeb::BuildMessageInfo;
using NMdb::NWeb::BuildNoSuchFolderAction;
using NMdb::NWeb::BuildTab;
using NMdb::NWeb::BuildToAndCc;
using NMdb::NWeb::BuildThreadInfo;
using NMdb::NWeb::BuildThreadsMergeRule;
using NMdb::NWeb::BuildThreadsHashNamespace;
using NMdb::NWeb::BuildUserInfo;
using NMdb::NWeb::TActions;
using NMdb::NWeb::TFolderActions;
using NMdb::NWeb::THeaderAddress;
using NMdb::NWeb::THeaders;
using NMdb::NWeb::TMimePart;
using NMdb::NWeb::TMessage;
using NMdb::NWeb::TPath;
using NMdb::NWeb::TRequestAddedLids;
using NMdb::NWeb::TRequestAddedSymbols;
using NMdb::NWeb::TRequestLabels;
using NMdb::NWeb::TRequestLabelSymbols;
using NMdb::NWeb::TRequestLids;
using NMdb::NWeb::TRequestFolder;
using NMdb::NWeb::TRequestFolders;
using NMdb::NWeb::BuildStoreParams;
using NMdb::NWeb::TThreadInfo;
using NMdb::NWeb::TUser;

TEST(TTestBuildUserInfo, for_full_TUser_return_full_TUserInfo) {
    TUser user {"uid", "suid"};

    auto result = BuildUserInfo(user);
    EXPECT_EQ(result.Uid, "uid");
    EXPECT_EQ(result.Suid, "suid");
}

TEST(TTestBuildUserInfo, for_nullopt_suid_in_TUser_assign_empty_suid_in_TUserInfo) {
    auto result = BuildUserInfo(TUser{});
    EXPECT_EQ(result.Suid, "");
}

TEST(TTestBuildMessageInfo, for_full_TMessage_return_full_TMessageBase) {
    TMessage message;
    message.Storage.Stid = "stid";
    message.Storage.Offset = 1;
    message.Firstline = "fl";
    message.Headers.RecievedDate = std::time_t(123);
    message.Size= 2;

    auto result = BuildMessageInfo(message);
    EXPECT_EQ(result.Stid, "stid");
    EXPECT_EQ(result.OffsetDiff, 1u);
    EXPECT_EQ(result.FirstLine, "fl");
    EXPECT_EQ(result.ReceivedDate, std::time_t(123));
    EXPECT_EQ(result.Size, *message.Storage.Offset + *message.Size);
}

TEST(TTestBuildMessageInfo, for_nullopt_offset_and_size_in_TMessage_assign_zero_offset_and_size_in_TMessageBase) {
    auto result = BuildMessageInfo(TMessage{});

    EXPECT_EQ(result.OffsetDiff, 0u);
    EXPECT_EQ(result.Size, 0u);
}

TEST(TTestBuildFolder, for_not_nullopt_fid_in_TRequestFolder_assign_TFolderCoords_fid) {
    TRequestFolder folder;
    folder.Fid = "fid";

    auto result = BuildFolder(folder);
    EXPECT_TRUE(result);
    EXPECT_EQ(result->Fid, "fid");
    EXPECT_TRUE(result->Path.empty());
}

TEST(TTestBuildFolder, for_nullopt_fid_and_empty_delim_in_TRequestFolder_assign_TFolderCoords_path_as_path) {
    TRequestFolder folder;
    folder.Path = TPath();
    folder.Path->Path = "path";

    auto result = BuildFolder(folder);
    EXPECT_TRUE(result);
    EXPECT_EQ(result->Fid, "");
    EXPECT_THAT(result->Path, ElementsAre("path"));
}

TEST(TTestBuildFolder, for_nullopt_fid_and_not_empty_delim_in_TRequestFolder_assign_TFolderCoords_path_as_splited_paths_by_delim) {
    TRequestFolder folder;
    folder.Path = TPath();
    folder.Path->Path = "path0/path1";
    folder.Path->Delimeter = "/";

    auto result = BuildFolder(folder);
    EXPECT_TRUE(result);
    EXPECT_EQ(result->Fid, "");
    EXPECT_THAT(result->Path, ElementsAre("path0", "path1"));
}

TEST(TTestBuildFolder, for_nullopt_all_fields_in_TRequestFolder_return_nullopt) {
    auto result = BuildFolder(TRequestFolder{});
    EXPECT_FALSE(result);
}

TEST(TTestBuildFolder, for_not_nullopt_dest_folder_in_TRequestFolders_return_dest_folder_as_TFolderCoords) {
    TRequestFolders folders;
    folders.Destination = TRequestFolder();
    folders.Destination->Fid = "dest_fid";
    folders.Original = TRequestFolder();
    folders.Original->Fid = "orig_fid";

    auto result = BuildFolder(folders);
    EXPECT_EQ(result.Fid, "dest_fid");
}

TEST(TTestBuildFolder, for_nullopt_dest_folder_and_not_nullopt_origin_folder_in_TRequestFolders_return_origin_folder_as_TFolderCoords) {
    TRequestFolders folders;
    folders.Original = TRequestFolder();
    folders.Original->Fid = "orig_fid";

    auto result = BuildFolder(folders);
    EXPECT_EQ(result.Fid, "orig_fid");
}

TEST(TTestBuildFolder, for_nullopt_all_folders_in_TRequestFolders_return_inbox_folder_as_TFolderCoords) {
    auto result = BuildFolder(TRequestFolders{});
    EXPECT_THAT(result.Path, ElementsAre("\\Inbox"));
}

TEST(TTestBuildLabels, for_not_nullopt_lids_and_symbols_assign_lids_and_symbols_in_TLabelsArgs) {
    TRequestLids lids = {"lid0", "lid1"};
    TRequestLabelSymbols symbols = {"sym0", "sym1"};

    auto result = BuildLabels(lids, symbols, {}, {}, {});
    EXPECT_THAT(result.Lids, ElementsAre("lid0", "lid1"));
    EXPECT_THAT(result.Symbols, ElementsAre("sym0", "sym1"));
    EXPECT_TRUE(result.Labels.empty());
}

TEST(TTestBuildLabels, for_nullopt_lids_and_symbols_and_not_nullopt_added_lids_and_symbol_assign_lids_and_symbols_in_TLabelsArgs) {
    TRequestAddedLids added_lids = {"lid0", "lid1"};
    TRequestAddedSymbols added_symbols = {"sym0", "sym1"};

    auto result = BuildLabels({}, {}, {}, added_lids, added_symbols);
    EXPECT_THAT(result.Lids, ElementsAre("lid0", "lid1"));
    EXPECT_THAT(result.Symbols, ElementsAre("sym0", "sym1"));
    EXPECT_TRUE(result.Labels.empty());
}

TEST(TTestBuildLabels, for_not_nullopt_lids_and_symbols_and_not_nullopt_added_lids_and_symbol_assign_merged_lids_and_symbols_in_TLabelsArgs) {
    TRequestLids lids = {"lid0", "lid1"};
    TRequestAddedLids added_lids = {"lid2", "lid3"};
    TRequestLabelSymbols symbols = {"sym0", "sym1"};
    TRequestAddedSymbols added_symbols = {"sym2", "sym3"};

    auto result = BuildLabels(lids, symbols, {}, added_lids, added_symbols);
    EXPECT_THAT(result.Lids, ElementsAre("lid0", "lid1", "lid2", "lid3"));
    EXPECT_THAT(result.Symbols, ElementsAre("sym0", "sym1", "sym2", "sym3"));
    EXPECT_TRUE(result.Labels.empty());
}

TEST(TTestBuildLabels, for_not_nullopt_labels_assign_labels_in_TLabelsArgs) {
    TRequestLabels labels = {{"name0", "type0"}, {"name1", "type1"}};

    auto result = BuildLabels({}, {}, labels, {}, {});
    EXPECT_TRUE(result.Lids.empty());
    EXPECT_TRUE(result.Symbols.empty());
    EXPECT_THAT(result.Labels, ElementsAre(TLabel{"name0", "type0", ""}, TLabel{"name1", "type1", ""}));
}

TEST(TTestBuildLabels, for_not_nullopt_labels_and_has_user_labels_assign_user_labels_with_color_in_TLabelsArgs) {
    TRequestLabels labels = {{"name0", "type0"}, {"name1", "user"}};

    auto result = BuildLabels({}, {}, labels, {}, {});
    EXPECT_TRUE(result.Lids.empty());
    EXPECT_TRUE(result.Symbols.empty());
    EXPECT_THAT(result.Labels, ElementsAre(TLabel{"name0", "type0", ""}, TLabel{"name1", "user", "16711680"}));
}

TEST(TTestBuildTabs, for_nullopt_tab_return_nullopt_tab) {
    auto result = BuildTab({});
    EXPECT_FALSE(result);
}

TEST(TTestBuildTabs, for_not_nullopt_tab_return_not_nullopt_tab) {
    auto result = BuildTab("tab");
    EXPECT_TRUE(result);
    EXPECT_EQ(*result, "tab");
}

TEST(TTestBuildAddresses, for_empty_addresses_return_empty_string) {
    auto result = BuildAddresses({});
    EXPECT_TRUE(result.empty());
}

TEST(TTestBuildAddresses, for_full_addresses_return_merged_addresses) {
    std::vector<THeaderAddress> addresses {
        {"local0", "domain0", "display_name0"},
        {"local1", "domain1", "display_name1"}
    };

    auto result = BuildAddresses(addresses);
    EXPECT_EQ(result, "\"display_name0\" <local0@domain0>,\"display_name1\" <local1@domain1>");
}

TEST(TTestBuildAddresses, for_addresses_with_empty_domain_return_merged_addresses_without_domain) {
    std::vector<THeaderAddress> addresses {
        {"local0", "", "display_name0"},
        {"local1", "", "display_name1"}
    };

    auto result = BuildAddresses(addresses);
    EXPECT_EQ(result, "\"display_name0\" <local0>,\"display_name1\" <local1>");
}

TEST(TTestBuildAddresses, for_addresses_with_empty_display_name_return_merged_addresses_without_display_name) {
    std::vector<THeaderAddress> addresses {
        {"local0", "domain0", ""},
        {"local1", "domain1", ""}
    };

    auto result = BuildAddresses(addresses);
    EXPECT_EQ(result, "<local0@domain0>,<local1@domain1>");
}

TEST(TTestBuildToAndCc, for_empty_to_and_cc_return_empty_string) {
    auto result = BuildToAndCc({}, {});
    EXPECT_TRUE(result.empty());
}

TEST(TTestBuildToAndCc, for_to_and_cc_return_merged_addresses_without_display_name) {
    std::vector<THeaderAddress> to {{"local0", "", ""}, {"local1", "", ""}};
    std::vector<THeaderAddress> cc {{"local2", "domain2", "display_name2"}};

    auto result = BuildToAndCc(to, cc);
    EXPECT_EQ(result, "local0,local1,local2@domain2");
}

TEST(TTestBuildToAndCc, for_empty_cc_return_merged_addresses_from_to) {
    std::vector<THeaderAddress> to {{"local0", "", ""}, {"local1", "", ""}};

    auto result = BuildToAndCc(to, {});
    EXPECT_EQ(result, "local0,local1");
}

TEST(TTestBuildHeaders, for_full_THeaders_from_request_return_full_THeaders) {
    THeaders headers;
    headers.Subject = "subject";
    headers.MsgId = "msg_id";
    headers.Date = std::time_t(123);
    headers.ReplyTo = "reply_to";
    headers.InReplyTo = "in_reply_to";
    headers.From = {
        {"from_local0", "from_domain0", "from_display_name0"},
        {"from_local1", "from_domain1", "from_display_name1"}
    };
    headers.To = {
        {"to_local0", "to_domain0", "to_display_name0"},
        {"to_local1", "to_domain1", "to_display_name1"}
    };
    headers.Cc = {
        {"cc_local0", "cc_domain0", "cc_display_name0"},
        {"cc_local1", "cc_domain1", "cc_display_name1"}
    };
    headers.Bcc = {
        {"bcc_local0", "bcc_domain0", "bcc_display_name0"},
        {"bcc_local1", "bcc_domain1", "bcc_display_name1"}
    };

    auto result = BuildHeaders(headers);
    EXPECT_EQ(result.Subject, "subject");
    EXPECT_EQ(result.MessageId, "msg_id");
    EXPECT_EQ(result.Date, std::time_t(123));
    EXPECT_EQ(result.ReplyTo, "reply_to");
    EXPECT_EQ(result.InReplyTo, "in_reply_to");
    EXPECT_EQ(result.From, BuildAddresses(headers.From));
    EXPECT_EQ(result.To, BuildAddresses(headers.To));
    EXPECT_EQ(result.Cc, BuildAddresses(*headers.Cc));
    EXPECT_EQ(result.Bcc, BuildAddresses(*headers.Bcc));
    EXPECT_EQ(result.ToAndCc, BuildToAndCc(headers.To, headers.Cc));
}

TEST(TTestBuildHeaders, for_nullopt_fields_THeaders_from_request_return_empty_fields_THeaders) {
    auto result = BuildHeaders(THeaders{});
    EXPECT_TRUE(result.MessageId.empty());
    EXPECT_EQ(result.Date, std::time_t(0));
    EXPECT_TRUE(result.ReplyTo.empty());
    EXPECT_TRUE(result.InReplyTo.empty());
    EXPECT_TRUE(result.Cc.empty());
    EXPECT_TRUE(result.Bcc.empty());
    EXPECT_TRUE(result.ToAndCc.empty());
}

TEST(TTestBuildMimeParts, for_nullopt_mime_parts_return_empty_mime_parts) {
    auto result = BuildMimeParts({}, 0);
    EXPECT_TRUE(result.empty());
}

TEST(TTestBuildMimeParts, for_empty_mime_parts_return_empty_mime_parts) {
    auto result = BuildMimeParts(std::vector<TMimePart>(), 0);
    EXPECT_TRUE(result.empty());
}

TEST(TTestBuildMimeParts, for_filled_mime_parts_return_filled_mime_parts) {
    TMimePart part;
    part.Hid = "hid";
    part.ContentType = "type";
    part.ContentSubtype = "subtype";
    part.Boundary = "boundary";
    part.Name = "name";
    part.Charset = "charset";
    part.Encoding = "encoding";
    part.ContentDisposition = "disposition";
    part.FileName = "file";
    part.ContentId = "content_id";
    part.Offset = 1u;
    part.Length = 2u;
    std::size_t offset = 3u;

    auto result = BuildMimeParts({{part, TMimePart()}}, offset);

    EXPECT_EQ(result.size(), 2u);

    EXPECT_EQ(result.front().Hid, "hid");
    EXPECT_EQ(result.front().ContentType, "type");
    EXPECT_EQ(result.front().ContentSubtype, "subtype");
    EXPECT_EQ(result.front().Boundary, "boundary");
    EXPECT_EQ(result.front().Name, "name");
    EXPECT_EQ(result.front().Charset, "charset");
    EXPECT_EQ(result.front().Encoding, "encoding");
    EXPECT_EQ(result.front().ContentDisposition, "disposition");
    EXPECT_EQ(result.front().FileName, "file");
    EXPECT_EQ(result.front().ContentId, "content_id");
    EXPECT_EQ(result.front().Offset, *part.Offset + offset);
    EXPECT_EQ(result.front().Length, 2u);

    EXPECT_TRUE(result.back().Hid.empty());
    EXPECT_TRUE(result.back().ContentType.empty());
    EXPECT_TRUE(result.back().ContentSubtype.empty());
    EXPECT_TRUE(result.back().Boundary.empty());
    EXPECT_TRUE(result.back().Name.empty());
    EXPECT_TRUE(result.back().Charset.empty());
    EXPECT_TRUE(result.back().Encoding.empty());
    EXPECT_TRUE(result.back().ContentDisposition.empty());
    EXPECT_TRUE(result.back().FileName.empty());
    EXPECT_TRUE(result.back().ContentId.empty());
    EXPECT_EQ(result.back().Offset, offset);
    EXPECT_EQ(result.back().Length, 0u);
}

TEST(TTestBuildThreadsHashNamespace, test_different_ns) {
    EXPECT_EQ(BuildThreadsHashNamespace("subject"), NMdb::EThreadsHashNamespaces::Subject);
    EXPECT_EQ(BuildThreadsHashNamespace("from"), NMdb::EThreadsHashNamespaces::From);
    ASSERT_THROW(BuildThreadsHashNamespace("ns"), std::runtime_error);
}

TEST(TTestBuildThreadsMergeRule, test_different_rules) {
    EXPECT_EQ(BuildThreadsMergeRule("force_new"), NMdb::EThreadsMergeRules::ForceNewThread);
    EXPECT_EQ(BuildThreadsMergeRule("hash"), NMdb::EThreadsMergeRules::Hash);
    EXPECT_EQ(BuildThreadsMergeRule("references"), NMdb::EThreadsMergeRules::References);
    ASSERT_THROW(BuildThreadsMergeRule("rule"), std::runtime_error);
}

TEST(TTestBuildThreadInfo, for_full_thread_info_request_return_full_thread_info) {
    TThreadInfo threadInfo;
    threadInfo.Limits.Days = 1u;
    threadInfo.Limits.Count = 2u;
    threadInfo.Hash.Value = "value";
    threadInfo.Hash.Namespace = "subject";
    threadInfo.Rule = "hash";
    threadInfo.ReferenceHashes = {"ref0", "ref1"};
    threadInfo.MessageIds = {"msg0", "msg1"};
    threadInfo.InReplyToHash = "reply";
    threadInfo.MessageIdHash = "msg_hash";

    auto result = BuildThreadInfo(threadInfo);
    EXPECT_EQ(result.Limits.DaysLimit, 1u);
    EXPECT_EQ(result.Limits.CountLimit, 2u);
    EXPECT_EQ(result.Hash.Value, "value");
    EXPECT_EQ(result.Hash.Ns, BuildThreadsHashNamespace(threadInfo.Hash.Namespace));
    EXPECT_EQ(result.MergeRule, BuildThreadsMergeRule(threadInfo.Rule));
    EXPECT_THAT(result.ReferenceHashes, ElementsAre("ref0", "ref1"));
    EXPECT_THAT(result.MessageIds, ElementsAre("msg0", "msg1"));
    EXPECT_EQ(result.InReplyToHash, "reply");
    EXPECT_EQ(result.MessageIdHash, "msg_hash");
}

TEST(TTestBuildStoreParams, for_full_TActions_request_return_full_TStoreParams) {
    TActions actions;
    actions.Duplicates.Ignore = true;
    actions.Duplicates.Remove = false;
    actions.DisablePush = true;
    bool imap = true;

    auto result = BuildStoreParams(actions, imap);
    EXPECT_TRUE(result.IgnoreDuplicates);
    EXPECT_FALSE(result.NeedRemoveDuplicates);
    EXPECT_TRUE(result.DisablePush);
    EXPECT_TRUE(result.Imap);
}

TEST(TTestBuildStoreParams, for_true_orig_StoreAsDeleted_return_true_StoreAsDeleted) {
    TActions actions;
    actions.Original.StoreAsDeleted = true;
    actions.RulesApplied = TFolderActions();
    actions.RulesApplied->StoreAsDeleted = false;


    auto result = BuildStoreParams(actions, false);
    EXPECT_TRUE(result.StoreAsDeleted);
}

TEST(TTestBuildStoreParams, for_true_rules_applied_StoreAsDeleted_return_true_StoreAsDeleted) {
    TActions actions;
    actions.Original.StoreAsDeleted = false;
    actions.RulesApplied = TFolderActions();
    actions.RulesApplied->StoreAsDeleted = true;

    auto result = BuildStoreParams(actions, false);
    EXPECT_TRUE(result.StoreAsDeleted);
}

TEST(TTestBuildStoreParams, for_false_orig_and_rules_applied_StoreAsDeleted_return_false_StoreAsDeleted) {
    TActions actions;
    actions.Original.StoreAsDeleted = false;
    actions.RulesApplied = TFolderActions();
    actions.RulesApplied->StoreAsDeleted = false;

    auto result = BuildStoreParams(actions, false);
    EXPECT_FALSE(result.StoreAsDeleted);
}

TEST(TTestBuildNoSuchFolderAction, test_return_correct_ENoSuchFolderAction) {
    const std::map<std::string, NMdb::ENoSuchFolderAction> actions {
        {"fail", NMdb::ENoSuchFolderAction::Fail},
        {"fallback_to_inbox", NMdb::ENoSuchFolderAction::FallbackToInbox},
        {"create", NMdb::ENoSuchFolderAction::Create}};

    for (const auto& [key, value] : actions) {
        TActions action;
        action.RulesApplied = TFolderActions();
        action.RulesApplied->NoSuchFolder = key;
        EXPECT_EQ(BuildNoSuchFolderAction(action), value);
    }

    TActions action;
    action.RulesApplied = TFolderActions();
    action.RulesApplied->NoSuchFolder = "nothing";
    ASSERT_THROW(BuildNoSuchFolderAction(action), std::runtime_error);
}

TEST(TTestBuildNoSuchFolderAction, for_not_nullopt_RulesApplied_return_from_RulesApplied) {
    TActions action;
    action.RulesApplied = TFolderActions();
    action.RulesApplied->NoSuchFolder = "fail";
    action.Original.NoSuchFolder = "create";
    EXPECT_EQ(BuildNoSuchFolderAction(action), NMdb::ENoSuchFolderAction::Fail);
}

TEST(TTestBuildNoSuchFolderAction, for_nullopt_RulesApplied_and_not_nullopt_Original_return_from_Original) {
    TActions action;
    action.Original.NoSuchFolder = "create";
    EXPECT_EQ(BuildNoSuchFolderAction(action), NMdb::ENoSuchFolderAction::Create);
}

TEST(TTestBuildNoSuchFolderAction, for_nullopt_RulesApplied_and_Original_return_from_fallback_to_inbox) {
    EXPECT_EQ(BuildNoSuchFolderAction(TActions{}), NMdb::ENoSuchFolderAction::FallbackToInbox);
}

TEST(TTestBuildFilterActions, for_not_UseFilters_return_nullopt) {
    TActions action;
    action.UseFilters = false;
    EXPECT_FALSE(BuildFilterActions(action, {}, {}, {}));
}

TEST(TTestBuildFilterActions, for_full_request_return_full_TFilterActions) {
    TActions action;
    action.UseFilters = true;
    action.Original.StoreAsDeleted = true;
    TRequestFolders folders;
    folders.Original = TRequestFolder();
    folders.Original->Fid = "fid";
    TRequestAddedLids added_lids = {"lid0", "lid1"};
    TRequestAddedSymbols added_symbols = {"sym0", "sym1"};

    auto result = BuildFilterActions(action, folders, added_lids, added_symbols);
    EXPECT_TRUE(result);
    EXPECT_EQ(result->OriginalFolder.Fid, "fid");
    EXPECT_TRUE(result->OriginalStoreAsDeleted);
    EXPECT_THAT(result->AddedLids, ElementsAre("lid0", "lid1"));
    EXPECT_THAT(result->AddedSymbols, ElementsAre("sym0", "sym1"));
}

TEST(TTestBuildFilterActions, for_nullopt_Original_folder_return_inbox) {
    TActions action;
    action.UseFilters = true;

    auto result = BuildFilterActions(action, {}, {}, {});
    EXPECT_TRUE(result);
    EXPECT_THAT(result->OriginalFolder.Path, ElementsAre("\\Inbox"));
}

TEST(TTestBuildFilterActions, for_nullopt_lids_and_symbols_folder_return_empty_added_lids_and_symbols) {
    TActions action;
    action.UseFilters = true;

    auto result = BuildFilterActions(action, {}, {}, {});
    EXPECT_TRUE(result);
    EXPECT_TRUE(result->AddedLids.empty());
    EXPECT_TRUE(result->AddedSymbols.empty());
}

}
