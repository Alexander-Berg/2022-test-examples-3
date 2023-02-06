#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "parse.h"


using namespace testing;

namespace sendbernar {
using namespace params;

namespace tests {

TEST(AttachesParams, shouldSuccessfullyParse) {
    CREATE_REQ;
    RETURN_ARGS("uploaded_attach_stids", ({"stid1", "stid2"}));
    RETURN_ARGS("forward_mids", ({"mid1", "mid2"}));
    RETURN_ARGS("parts_json", ({partJson(), partJson()}));
    RETURN_ARGS("disk_attaches_json", ({diskAttachJson(), diskAttachJson()}));
    RETURN_ARG_OPT("disk_attaches", "disk_attach");

    const auto params = get<Attaches>(REQ);

    EXPECT_EQ(*params.disk_attaches, "disk_attach");
    EXPECT_THAT(*params.disk_attaches_json, UnorderedElementsAre(diskAttach(), diskAttach()));
    EXPECT_THAT(*params.uploaded_attach_stids, UnorderedElementsAre("stid2", "stid1"));
    EXPECT_THAT(*params.forward_mids, UnorderedElementsAre("mid1", "mid2"));
    EXPECT_THAT(*params.parts_json, UnorderedElementsAre(messagePart(), messagePart()));
}

TEST(AttachesParams, shouldThrowReaderExceptionInCaseOfWrongDiskAttachesJson) {
    CREATE_REQ;
    RETURN_ARG_OPT_EMPTY("uploaded_attach_stids");
    RETURN_ARG_OPT_EMPTY("forward_mids");
    RETURN_ARG_OPT_EMPTY("parts_json");
    RETURN_ARG_OPT_EMPTY("disk_attaches");
    RETURN_ARGS("disk_attaches_json", ({R"r({"name":"filename","size":65535})r"}));

    EXPECT_THROW(get<Attaches>(REQ), ReaderException);
}

TEST(AttachesParams, shouldThrowReaderExceptionInCaseOfWrongPartsJson) {
    CREATE_REQ;
    RETURN_ARG_OPT_EMPTY("uploaded_attach_stids");
    RETURN_ARG_OPT_EMPTY("forward_mids");
    RETURN_ARGS("parts_json", ({R"r({"hid":"hid"})r"}));
    RETURN_ARG_OPT_EMPTY("disk_attaches");
    RETURN_ARG_OPT_EMPTY("disk_attaches_json");

    EXPECT_THROW(get<Attaches>(REQ), ReaderException);
}

TEST(SenderParams, shouldSuccessfullyParse) {
    CREATE_REQ;
    RETURN_ARG_OPT("from_mailbox", "from_mailbox");
    RETURN_ARG_OPT("from_name", "from_name");

    const auto params = get<Sender>(REQ);

    EXPECT_EQ(*params.from_mailbox, "from_mailbox");
    EXPECT_EQ(*params.from_name, "from_name");
}

TEST(MessageParams, shouldSuccessfullyParse) {
    CREATE_REQ;
    RETURN_ARG_OPT("subj", "subject");
    RETURN_ARG_OPT("text", "some text");
    RETURN_ARG_OPT("html", "yes");
    RETURN_ARG_OPT("force7bit", "yes");
    RETURN_ARG_OPT("source_mid", "mid");
    RETURN_ARG_OPT("message_id", "<123@yandex.ru>");
    RETURN_ARG_OPT("current_time", "1377");

    const auto params = get<Message>(REQ);

    EXPECT_EQ(*params.subj, "subject");
    EXPECT_EQ(*params.text, "some text");
    EXPECT_EQ(*params.html, true);
    EXPECT_EQ(*params.force7bit, true);
    EXPECT_EQ(*params.source_mid, "mid");
    EXPECT_EQ(*params.message_id, "<123@yandex.ru>");
    EXPECT_EQ(*params.current_time, 1377);
}

TEST(RecipientsParams, shouldSuccessfullyParse) {
    CREATE_REQ;
    RETURN_ARG_OPT("to", "to@ya.ru,to2@ya.ru");
    RETURN_ARG_OPT("cc", "cc@ya.ru");
    RETURN_ARG_OPT("bcc", "bcc@ya.ru");

    const auto params = get<Recipients>(REQ);

    EXPECT_EQ(*params.to, "to@ya.ru,to2@ya.ru");
    EXPECT_EQ(*params.cc, "cc@ya.ru");
    EXPECT_EQ(*params.bcc, "bcc@ya.ru");
}

TEST(DeliveryParams, shouldSuccessfullyParse) {
    CREATE_REQ;
    RETURN_ARG_OPT("noanswer_remind_period", "1234567");
    RETURN_ARG_OPT("confirm_delivery", "no");

    const auto params = get<Delivery>(REQ);

    EXPECT_EQ(*params.noanswer_remind_period, 1234567);
    EXPECT_EQ(*params.confirm_delivery, false);
}

TEST(CaptchaParams, shouldSuccessfullyParse) {
    CREATE_REQ;
    RETURN_ARG("captcha_entered", "entered");
    RETURN_ARG("captcha_key", "key");

    const auto params = get<Captcha>(REQ);

    EXPECT_EQ(params.captcha_entered, "entered");
    EXPECT_EQ(params.captcha_key, "key");
}

TEST(NoAnswerReminderParams, shouldSuccessfullyParse) {
    CREATE_REQ;
    RETURN_ARG("message_id", "hdr");
    RETURN_ARG("no_answer_period", "123");

    const auto params = get<NoAnswerReminder>(REQ);

    EXPECT_EQ(params.message_id, "hdr");
    EXPECT_EQ(params.no_answer_period, 123ul);
}

}
}
