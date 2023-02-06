#include <mail/notsolitesrv/src/meta_save_op/util/tupita.h>

#include <mail/message_types/lib/message_types.h>

#include <mail/notsolitesrv/tests/unit/util/tupita.h>

#include <gtest/gtest.h>

#include <string>
#include <utility>
#include <vector>

namespace {

using namespace NNotSoLiteSrv::NMetaSaveOp;

using NNotSoLiteSrv::NFurita::TFuritaRule;
using NNotSoLiteSrv::NTupita::TLabelsInfoMap;
using NNotSoLiteSrv::NTupita::TTupitaMessage;
using NNotSoLiteSrv::NTupita::TTupitaMessageLabel;
using NNotSoLiteSrv::NTupita::TTupitaQuery;
using NNotSoLiteSrv::NTupita::TTupitaUser;
using NNotSoLiteSrv::TEmailAddress;
using NNotSoLiteSrv::TUid;

TAttachmentMap MakeTestAttachmentMap() {
    TAttachmentMap map;
    for (auto index = 0; index < 3; ++index) {
        const auto suffix{std::to_string(index)};
        const auto hid{"0.0." + suffix};
        map[hid].name = "Name" + suffix;
        map[hid].type = "Type" + suffix;
        map[hid].size = index;
    }

    return map;
}

TLabelsInfoMap MakeTestLabelsInfoMap() {
    TTupitaMessageLabel tupitaMessageLabel{.Name = "DomainLabel", .IsSystem = false, .IsUser = false,
        .Type = {.Title ="social"}};
    return {{"FAKE_DOMAIN_LABEL", std::move(tupitaMessageLabel)}};
}

std::vector<TFuritaRule> MakeTestFuritaRules() {
    return {
        {
            .Id = "Id1",
            .Priority = 1,
            .Query = "Query1",
            .Enabled = true,
            .Stop = true,
            .Actions = {{.Verified = true}}
        }, {
            .Id = "Id0",
            .Priority = 0,
            .Query = "Query0",
            .Enabled = true,
            .Stop = false,
            .Actions = {{.Verified = true}}
        }
    };
}

TTupitaUser MakeTestTupitaUser(TUid uid) {
    return {
        .Uid = uid,
        .Queries = {
            {.Id = "Id0", .Query = "Query0", .Stop = false},
            {.Id = "Id1", .Query = "Query1", .Stop = true}
        },
        .Spam = true
    };
}

TEST(TestMakeLabelsInfoMap, must_make_labels_info_map) {
    const auto expectedLabelsInfo{MakeTestLabelsInfoMap()};
    EXPECT_EQ(expectedLabelsInfo, MakeLabelsInfoMap(expectedLabelsInfo.at("FAKE_DOMAIN_LABEL").Name));
}

TEST(TestMakeTupitaMessage, for_optional_fields_unavailable_must_return_empty_optionals) {
    const auto tupitaMessage{MakeTupitaMessage({}, {})};
    EXPECT_FALSE(tupitaMessage.Cc);
    EXPECT_FALSE(tupitaMessage.AttachmentsCount);
    EXPECT_FALSE(tupitaMessage.LabelsInfo);
    EXPECT_FALSE(tupitaMessage.Firstline);
}

TEST(TestMakeTupitaMessage, must_make_tupita_message) {
    NNotSoLiteSrv::NTupita::TTupitaMessage expectedTupitaMessage{
        .Subject = "Subject",
        .From = {{"Local0", "Domain0", "DisplayName0"}},
        .To = {{"Local1", "Domain1", "DisplayName1"}},
        .Cc = {{{"Local2", "Domain2", "DisplayName2"}}},
        .Stid = "Stid",
        .Spam = true,
        .Types = {NMail::MT_DELIVERY, NMail::MT_REGISTRATION},
        .AttachmentsCount = 3,
        .LabelsInfo = MakeTestLabelsInfoMap(),
        .Firstline = "Firstline"
    };

    const auto& from{expectedTupitaMessage.From.front()};
    const auto& to{expectedTupitaMessage.To.front()};
    const auto& cc{expectedTupitaMessage.Cc->front()};

    TRequest request;
    request.message.subject = expectedTupitaMessage.Subject;
    request.message.from.emplace_back(TEmailAddress{from.Local, from.Domain, from.DisplayName});
    request.message.to.emplace_back(TEmailAddress{to.Local, to.Domain, to.DisplayName});
    request.message.cc = TEmailAddressList{{cc.Local, cc.Domain, cc.DisplayName}};
    request.message.attachments = MakeTestAttachmentMap();
    request.message.spam = expectedTupitaMessage.Spam;
    request.stid = expectedTupitaMessage.Stid;
    request.domain_label = expectedTupitaMessage.LabelsInfo->at("FAKE_DOMAIN_LABEL").Name;
    request.types = expectedTupitaMessage.Types;

    EXPECT_EQ(expectedTupitaMessage, MakeTupitaMessage(request, {{*expectedTupitaMessage.Firstline}}));
}

TEST(TestMakeTupitaQuery, must_make_tupita_query) {
    const TTupitaQuery expectedTupitaQuery{.Id = "Id", .Query = "Query", .Stop = true};
    EXPECT_EQ(expectedTupitaQuery, MakeTupitaQuery({.Id = expectedTupitaQuery.Id,
        .Query = expectedTupitaQuery.Query, .Stop = expectedTupitaQuery.Stop}));
}

TEST(TestMakeTupitaUsers, for_recipient_available_must_make_tupita_user_with_spam_flag_taken_from_recipient) {
    const TUid uid{1};
    const std::vector<TTupitaUser> expectedTupitaUsers{MakeTestTupitaUser(uid)};

    TRequest request;
    auto& recipient{request.recipients["DeliveryId"]};
    recipient.user.uid = uid;
    recipient.params.spam = true;

    EXPECT_EQ(expectedTupitaUsers, MakeTupitaUsers(request, uid, MakeTestFuritaRules()));
}

TEST(TestMakeTupitaUsers, for_recipient_unavailable_must_make_tupita_user_with_spam_flag_taken_from_message) {
    const TUid uid{1};
    const std::vector<TTupitaUser> expectedTupitaUsers{MakeTestTupitaUser(uid)};

    TRequest request;
    request.message.spam = true;
    request.recipients["DeliveryId"].user.uid = uid + 1;

    EXPECT_EQ(expectedTupitaUsers, MakeTupitaUsers(request, uid, MakeTestFuritaRules()));
}

}
