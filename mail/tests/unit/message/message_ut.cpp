#include <mail/notsolitesrv/tests/unit/fakes/context.h>

#include <mail/notsolitesrv/src/message/message.h>
#include <mail/notsolitesrv/src/message/parser.h>
#include <mail/notsolitesrv/src/message/processor.h>
#include <mail/notsolitesrv/src/util/headers.h>

#include <butil/butil.h>
#include <yplatform/log/logger.h>

#include <boost/algorithm/string/predicate.hpp>
#include <boost/asio/ip/host_name.hpp>

#include <library/cpp/resource/resource.h>
#include <util/memory/blob.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

using namespace testing;
using namespace NNotSoLiteSrv;

class TMessageTest: public Test {
public:
    void ReadEml(const std::string& name, const std::string& myAddress = "localmx") {
        auto data = NResource::Find(std::string("messages/") + name);
        MessageData.assign(data.data(), data.size());
        Message = ParseMessage(MessageData, Ctx);
        ASSERT_TRUE(Message);

        Envelope.Lhlo = "localhost";
        Envelope.RemoteHost = "localhost.localdomain";
        Envelope.RemoteIp = "127.0.0.1";
        Envelope.Hostname = myAddress;
        Envelope.MailFrom = "sender@ya.ru";
        Preprocess(Ctx, Message, Envelope, UserStorage);
    }

protected:
    std::string MessageData;
    TMessagePtr Message;
    TEnvelope Envelope;
    NUser::TStoragePtr UserStorage = std::make_shared<NUser::TStorage>();
    TContextPtr Ctx = GetContext();
};

TEST_F(TMessageTest, AddReceivedForOneRcpt) {
    UserStorage->AddUser("test@ya.ru", true, true);
    ReadEml("simple.eml");
    auto composedZC = Message->Compose();
    std::string composed(composedZC.begin(), composedZC.end());
    std::string expectedReceived =
        "Received: from localhost (localhost.localdomain [127.0.0.1])\r\n"
        "\tby localmx with LMTP id ";
    expectedReceived.append(Ctx->GetFullSessionId());
    expectedReceived.append("\r\n\tfor <test@ya.ru>;");

    EXPECT_THAT(composed, StartsWith(expectedReceived));
}

TEST_F(TMessageTest, AddReceivedForManyRcpt) {
    UserStorage->AddUser("test@ya.ru", true, true);
    UserStorage->AddUser("test2@ya.ru", true, true);
    ReadEml("simple.eml", "");
    auto composedZC = Message->Compose();
    std::string composed(composedZC.begin(), composedZC.end());
    std::string hostname = boost::asio::ip::host_name();
    std::string expectedReceived =
        "Received: from localhost (localhost.localdomain [127.0.0.1])\r\n"
        "\tby " + hostname + " with LMTP id ";
    expectedReceived.append(Ctx->GetFullSessionId());
    expectedReceived.append(";\r\n\t");

    EXPECT_THAT(composed, StartsWith(expectedReceived));
}

TEST_F(TMessageTest, AddHeaderCorrectlyCalcOffsetDiff) {
    UserStorage->AddUser("test@ya.ru", true, true);
    UserStorage->AddUser("test2@ya.ru", true, true);
    ReadEml("simple.eml");
    auto composedZC = Message->Compose();
    std::string composed(composedZC.begin(), composedZC.end());

    std::string expectedReceived =
        "Received: from localhost (localhost.localdomain [127.0.0.1])\r\n"
        "\tby localmx with LMTP id ";
    expectedReceived.append(Ctx->GetFullSessionId());
    expectedReceived.append(";\r\n\t");
    expectedReceived.append(NUtil::MakeRfc2822Date(time(nullptr)));
    expectedReceived.append("\r\n");

    std::string expectedReturnPath = "Return-Path: " + Envelope.MailFrom + "\r\n";

    auto addedHeadersLen = static_cast<ssize_t>(expectedReceived.size())
        + static_cast<ssize_t>(expectedReturnPath.size());
    EXPECT_EQ(-Message->GetOffsetDiff(), addedHeadersLen);

    std::string expectedMsg = MessageData.substr(0, Message->GetOffset() - 2)
        + expectedReturnPath + MessageData.substr(Message->GetOffset() - 2);
    EXPECT_EQ(composed.substr(expectedReceived.size()), expectedMsg);
    EXPECT_EQ(composed.size(), MessageData.size() + addedHeadersLen);
    EXPECT_EQ(Message->GetFinalLength(), MessageData.size() + addedHeadersLen);

    Message->AddHeader("123", "678");
    int hdrLength = sizeof("123: 678\r\n") - 1;
    EXPECT_EQ(-Message->GetOffsetDiff(), addedHeadersLen + hdrLength);
}

TEST_F(TMessageTest, XYandexHintAddGet) {
    ReadEml("simple.eml");
    std::string hint = "fid=4";
    Message->AddXYHint(encode_base64(hint));
    auto xyhint = Message->GetXYHintByEmail("");
    EXPECT_EQ(xyhint->fid, "4");
    EXPECT_TRUE(xyhint->lid.empty());
}

TEST_F(TMessageTest, XYandexHintAddGetTargetted) {
    ReadEml("simple.eml");
    std::string hint = "fid=4\nemail=test@ya.ru";
    Message->AddXYHint(encode_base64(hint));
    auto xyhint = Message->GetXYHintByEmail("test@ya.ru");
    EXPECT_EQ(xyhint->fid, "4");
    EXPECT_TRUE(xyhint->lid.empty());
}

TEST_F(TMessageTest, XYandexHintCombine) {
    ReadEml("simple.eml");
    std::string hint = "fid=4\nemail=test@ya.ru";
    Message->AddXYHint(encode_base64(hint));
    hint = "copy_to_inbox=1\nemail=test@ya.ru";
    Message->AddXYHint(encode_base64(hint));
    Message->CombineXYHints();

    auto xyhint = Message->GetXYHintByEmail("test@ya.ru");
    EXPECT_EQ(xyhint->fid, "4");
    EXPECT_EQ(xyhint->copy_to_inbox, true);
    EXPECT_TRUE(xyhint->lid.empty());
}

TEST_F(TMessageTest, GetByUidMergeSoLabels) {
    UserStorage->AddUser("test@yandex.ru", true, true);
    ReadEml("simple.eml");
    const std::string soLabelXxx = "SystMetkaSO:xxxxxx";
    const std::string soLabelYyy = "SystMetkaSO:yyyyyy";
    const std::string soLabelZzz = "SystMetkaSO:zzzzzz";
    const std::string otherLabelAaa = "some:metka_aaa";
    const std::string otherLabelBbb = "some:metka_bbb";
    const std::string genericHint = "fid=4\n"
        "label=" + soLabelXxx + "\nlabel=" + soLabelYyy + "\nlabel=" + otherLabelAaa + "\n";
    Message->AddXYHint(encode_base64(genericHint));
    const std::string userHint = "email=test@yandex.ru\n"
        "label=" + soLabelYyy + "\nlabel=" + soLabelZzz + "\nlabel=" + otherLabelBbb + "\n";
    Message->AddXYHint(encode_base64(userHint));
    for (auto& user: *UserStorage) {
        user.second.Status = NUser::ELoadStatus::Found;
        user.second.Uid = "17";
        user.second.Suid = "18";
    }
    Message->UpdateResolvedUsers(*UserStorage);

    auto hint = Message->GetXYHintByUid("17");

    EXPECT_THAT(hint.label, UnorderedElementsAre(
        soLabelXxx,
        soLabelYyy,
        soLabelZzz,
        otherLabelAaa,
        otherLabelBbb
    ));
}

TEST_F(TMessageTest, GetByUidReplaceSoLabels) {
    UserStorage->AddUser("test@yandex.ru", true, true);
    ReadEml("simple.eml");
    const std::string soLabelXxx = "SystMetkaSO:xxxxxx";
    const std::string soLabelYyy = "SystMetkaSO:yyyyyy";
    const std::string soLabelZzz = "SystMetkaSO:zzzzzz";
    const std::string otherLabelAaa = "some:metka_aaa";
    const std::string otherLabelBbb = "some:metka_bbb";
    const std::string genericHint = "fid=4\n"
        "label=" + soLabelXxx + "\nlabel=" + soLabelYyy + "\nlabel=" + otherLabelAaa + "\n";
    Message->AddXYHint(encode_base64(genericHint));
    const std::string userHint = "email=test@yandex.ru\nreplace_so_labels=1\n"
        "label=" + soLabelYyy + "\nlabel=" + soLabelZzz + "\nlabel=" + otherLabelBbb + "\n";
    Message->AddXYHint(encode_base64(userHint));
    for (auto& user: *UserStorage) {
        user.second.Status = NUser::ELoadStatus::Found;
        user.second.Uid = "17";
        user.second.Suid = "18";
    }
    Message->UpdateResolvedUsers(*UserStorage);

    auto hint = Message->GetXYHintByUid("17");

    EXPECT_THAT(hint.label, UnorderedElementsAre(
        soLabelYyy,
        soLabelZzz,
        otherLabelAaa,
        otherLabelBbb
    ));
}

TEST_F(TMessageTest, XYandexHintUpdateResolved) {
    UserStorage->AddUser("test@ya.ru", true, true);
    UserStorage->AddUser("test@yandex.ru", true, true);
    ReadEml("simple.eml");
    std::string hint = "fid=4\nemail=test@ya.ru";
    Message->AddXYHint(encode_base64(hint));
    hint = "copy_to_inbox=1\nemail=test@yandex.ru\n";
    Message->AddXYHint(encode_base64(hint));
    Message->CombineXYHints();

    for (auto& user: *UserStorage) {
        user.second.Status = NUser::ELoadStatus::Found;
        user.second.Uid = "17";
        user.second.Suid = "18";
    }
    Message->UpdateResolvedUsers(*UserStorage);

    auto xyhint = Message->GetXYHintByUid("17");
    EXPECT_EQ(xyhint.fid, "4");
    EXPECT_EQ(xyhint.copy_to_inbox, true);
    EXPECT_TRUE(xyhint.lid.empty());
}

TEST_F(TMessageTest, XYandexHintIgnoreUntargettedHintsThatShouldBeTargetted) {
    ReadEml("simple.eml");
    std::string hint = "copy_to_inbox=1\nsave_to_sent=0\nfid=4";
    Message->AddXYHint(encode_base64(hint));
    Message->CombineXYHints();
    Message->UpdateResolvedUsers(*UserStorage);

    auto xyhint = Message->GetXYHint();
    EXPECT_EQ(xyhint->fid, "4");
    EXPECT_EQ(xyhint->copy_to_inbox, false);
    EXPECT_EQ(xyhint->save_to_sent, boost::none);
}

TEST_F(TMessageTest, SpamType) {
    ReadEml("simple.eml");
    EXPECT_FALSE(Message->IsSpam());
    EXPECT_EQ(Message->GetSpamType(), ESpamType::Unknown);

    Message->SetSpamType(ESpamType::Spam);
    EXPECT_TRUE(Message->IsSpam());

    Message->SetSpamTypeFromXYSpam(0);
    EXPECT_FALSE(Message->IsSpam());
    EXPECT_EQ(Message->GetSpamType(), ESpamType::Unknown);

    Message->SetSpamTypeFromXYSpam(4);
    EXPECT_TRUE(Message->IsSpam());
    EXPECT_EQ(Message->GetSpamType(), ESpamType::Spam);

    Message->SetSpamTypeFromXYSpam(2);
    EXPECT_FALSE(Message->IsSpam());
    EXPECT_EQ(Message->GetSpamType(), ESpamType::Delivery);
}

TEST_F(TMessageTest, PersonalResolutionSpamType) {
    ReadEml("simple.eml");
    Message->SetSpamTypeFromXYSpam(256);
    EXPECT_TRUE(Message->IsSpam());
    EXPECT_EQ(ESpamType::Malicious, Message->GetSpamType());

    const std::string uidWithPersonalResolution{"1"};
    Message->SetSpamTypeByUid(uidWithPersonalResolution, ESpamType::Ham);
    EXPECT_FALSE(Message->IsSpam(uidWithPersonalResolution));
    const std::string uidWithoutPersonalResolution{"2"};
    EXPECT_TRUE(Message->IsSpam(uidWithoutPersonalResolution));
    EXPECT_EQ(ESpamType::Ham, Message->GetSpamType(uidWithPersonalResolution));
    EXPECT_EQ(ESpamType::Malicious, Message->GetSpamType(uidWithoutPersonalResolution));
}

TEST_F(TMessageTest, PersonalResolutionSpamTypeFromUidStatusHeader) {
    ReadEml("uid_status.eml");
    EXPECT_FALSE(Message->IsSpam());
    EXPECT_EQ(ESpamType::Unknown, Message->GetSpamType());

    const std::string uidWithHamPersonalResolution{"1"};
    EXPECT_FALSE(Message->IsSpam(uidWithHamPersonalResolution));
    EXPECT_EQ(ESpamType::Ham, Message->GetSpamType(uidWithHamPersonalResolution));
    const std::string uidWithDeliveryPersonalResolution{"2"};
    EXPECT_FALSE(Message->IsSpam(uidWithDeliveryPersonalResolution));
    EXPECT_EQ(ESpamType::Delivery, Message->GetSpamType(uidWithDeliveryPersonalResolution));
    const std::string uidWithSpamPersonalResolution{"3"};
    EXPECT_TRUE(Message->IsSpam(uidWithSpamPersonalResolution));
    EXPECT_EQ(ESpamType::Spam, Message->GetSpamType(uidWithSpamPersonalResolution));
    const std::string uidWithMaliciousPersonalResolution{"4"};
    EXPECT_TRUE(Message->IsSpam(uidWithMaliciousPersonalResolution));
    EXPECT_EQ(ESpamType::Malicious, Message->GetSpamType(uidWithMaliciousPersonalResolution));
}

TEST_F(TMessageTest, SpamResolutionType) {
    ReadEml("simple.eml");
    const std::string uid{"1"};
    EXPECT_EQ(ESpamResolutionType::Global, Message->GetSpamResolutionType(uid));
    Message->SetSpamTypeByUid(uid, ESpamType::Ham);
    EXPECT_EQ(ESpamResolutionType::PersonalByUid, Message->GetSpamResolutionType(uid));
}

TEST_F(TMessageTest, Subject) {
    ReadEml("simple-utf.eml");
    EXPECT_EQ(Message->GetSubject(), "просто тест");
}

TEST_F(TMessageTest, GetXYForwardsFromDSN) {
    ReadEml("normal-dsn.eml");
    EXPECT_TRUE(Message->IsDSN());
    EXPECT_TRUE(Message->IsInXYForwards("1"));
    EXPECT_FALSE(Message->IsInXYForwards("2"));
}

TEST_F(TMessageTest, GetXYForwardsFromMailRuDSN) {
    ReadEml("mailru-dsn.eml");
    EXPECT_FALSE(Message->IsDSN());
    EXPECT_TRUE(Message->IsInXYForwards("1"));
    EXPECT_FALSE(Message->IsInXYForwards("2"));
}

TEST_F(TMessageTest, References) {
    ReadEml("normal-dsn.eml");
    EXPECT_EQ(Message->GetMessageId(), "<20181010141437.D1CA940F84@forward104j.mail.yandex.net>");
    EXPECT_EQ(Message->GetInReplyTo(), "<3301881539180865@myt4-174696c9aa9d.qloud-c.yandex.net>");

    decltype(Message->GetReferences()) expectedRefs{
        "<3301881539180865@myt4-174696c9aa9d.qloud-c.yandex.net>",
        "<3301881539180866@myt4-174696c9aa9d.qloud-c.yandex.net>"
    };
    EXPECT_EQ(Message->GetReferences(), expectedRefs);
}
