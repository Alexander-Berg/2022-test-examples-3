#include <mail/notsolitesrv/src/mthr/util.h>

#include <gmock/gmock.h>

namespace {

using namespace NNotSoLiteSrv::NMthr;
using namespace testing;

using NMthr::EHashNameSpace;
using NMthr::EMergePolicy;
using NMthr::TMailThreadInfo;
using NMthr::TMergeRule;
using NMthr::TMessageInfo;

TEST(TTestSetReferences, for_references_set_sanitized_References_in_TMessageInfo) {
    TMessageInfo messageInfo;
    std::vector<std::string> references = {"<foo@bar.com>\x00\x00", "<foo@bar.com>"};
    SetReferences(messageInfo, references);
    ASSERT_THAT(messageInfo.References, ElementsAre("<foo@bar.com>", "<foo@bar.com>"));
}

TEST(TTestSetReferences, for_invalid_msd_id_references_not_set_References_in_TMessageInfo) {
    TMessageInfo messageInfo;
    std::vector<std::string> references = {""};
    SetReferences(messageInfo, references);
    ASSERT_FALSE(messageInfo.References);
}

TEST(TTestBuildMessageInfo, for_full_request_return_full_sanitaized_TMessageInfo) {
    TMthrRequest request;
    request.HdrFromDomain = "ya.ru\x00\x00";
    request.Subject = "subject\x00\x00";
    request.MsgTypes = {100, 101};
    request.MessageId = "<foo@bar.com>\x00\x00";
    request.References = {"<foo@bar.com>\x00\x00"};
    request.InReplyTo = "foo@bar.com\x00\x00";
    request.DomainLabel = "bar.com\x00\x00";

    auto result = BuildMessageInfo(request);

    EXPECT_EQ(result.HeaderFromDomain, "ya.ru");
    EXPECT_EQ(result.Subject, "subject");
    EXPECT_EQ(result.MessageId, "<foo@bar.com>");
    EXPECT_EQ(result.InReplyTo, "foo@bar.com");
    EXPECT_EQ(result.DomainLabel, "bar.com");
    ASSERT_THAT(result.References, ElementsAre("<foo@bar.com>"));
    ASSERT_THAT(result.MessageTypes, ElementsAre(NMail::MT_T_NEWS, NMail::MT_T_SOCIAL));
}

TEST(TTestBuildMessageInfo,
    for_empty_and_null_opt_fields_in_HdrFromDomain_in_request_return_empty_fields_in_TMessageInfo)
{
    auto result = BuildMessageInfo(TMthrRequest{});
    EXPECT_EQ(result.HeaderFromDomain, "");
    EXPECT_EQ(result.Subject, "");
    EXPECT_EQ(result.MessageId, "");
    EXPECT_EQ(result.InReplyTo, "");
    EXPECT_EQ(result.DomainLabel, "");
    EXPECT_TRUE(result.References.empty());
    EXPECT_TRUE(result.MessageTypes.empty());
}

TEST(TTestBuildMessageInfo, for_empty_References_in_request_return_empty_References_in_TMessageInfo) {
    TMthrRequest request;
    request.References = {};
    auto result = BuildMessageInfo(request);
    EXPECT_TRUE(result.References.empty());
}

TEST(TTestMakeThreadInfo, for_TMailThreadInfo_make_correct_TThreadInfo) {
    TMailThreadInfo threadInfo;
    threadInfo.Hash.NameSpace = EHashNameSpace::Subject;
    threadInfo.Hash.Value = 0;
    threadInfo.Limits.DaysLimit = 1;
    threadInfo.Limits.CountLimit = 2;
    threadInfo.MergePolicy = EMergePolicy::ForceNew;
    threadInfo.MergeRuleId = TMergeRule::DEFAULT_ID;
    threadInfo.ReferenceHashes = {0, 1};
    threadInfo.MessageIds= {"id0", "id1"};
    threadInfo.InReplyToHash = 3;
    threadInfo.MessageIdHash = 4;

    auto result = MakeThreadInfo(threadInfo);

    EXPECT_EQ(result.Hash.Namespace, "subject");
    EXPECT_EQ(result.Hash.Value, "0");
    EXPECT_EQ(result.Limits.Days, 1u);
    EXPECT_EQ(result.Limits.Count, 2u);
    EXPECT_EQ(result.Rule, "force_new");
    ASSERT_THAT(result.ReferenceHashes, ElementsAre("0", "1"));
    ASSERT_THAT(result.MessageIds, ElementsAre("id0", "id1"));
    EXPECT_EQ(result.InReplyToHash, "3");
    EXPECT_EQ(result.MessageIdHash, "4");
}

}
