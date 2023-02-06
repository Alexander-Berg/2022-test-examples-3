#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "parse.h"


using namespace testing;

namespace sendbernar {
namespace tests {

TEST(SaveTemplateParams, shouldSuccessfullyParse) {
    CREATE_REQ;
    RETURN_ARGS("uploaded_attach_stids", ({"stid1", "stid2"}));
    RETURN_ARGS("forward_mids", ({"mid1", "mid2"}));
    RETURN_ARGS("parts_json", ({partJson(), partJson()}));
    RETURN_ARGS("lids", ({"1", "2"}));
    RETURN_ARGS("disk_attaches_json", ({diskAttachJson(), diskAttachJson()}));
    RETURN_ARG_OPT("disk_attaches", "disk_attach");
    RETURN_ARG_OPT("from_mailbox", "from_mailbox");
    RETURN_ARG_OPT("from_name", "from_name");
    RETURN_ARG_OPT("subj", "subject");
    RETURN_ARG_OPT("text", "some text");
    RETURN_ARG_OPT("html", "yes");
    RETURN_ARG_OPT("force7bit", "yes");
    RETURN_ARG_OPT("source_mid", "mid");
    RETURN_ARG_OPT("message_id", "<123@yandex.ru>");
    RETURN_ARG_OPT("current_time", "1377");
    RETURN_ARG_OPT("to", "to@ya.ru,to2@ya.ru");
    RETURN_ARG_OPT("cc", "cc@ya.ru");
    RETURN_ARG_OPT("bcc", "bcc@ya.ru");

    const auto params = getSaveTemplate(REQ);

    EXPECT_EQ(*params.attaches.disk_attaches, "disk_attach");
    EXPECT_THAT(*params.attaches.disk_attaches_json, UnorderedElementsAre(diskAttach(), diskAttach()));
    EXPECT_THAT(*params.attaches.uploaded_attach_stids, UnorderedElementsAre("stid2", "stid1"));
    EXPECT_THAT(*params.attaches.forward_mids, UnorderedElementsAre("mid1", "mid2"));
    EXPECT_THAT(*params.attaches.parts_json, UnorderedElementsAre(messagePart(), messagePart()));
    EXPECT_EQ(*params.sender.from_mailbox, "from_mailbox");
    EXPECT_EQ(*params.sender.from_name, "from_name");
    EXPECT_EQ(*params.message.subj, "subject");
    EXPECT_EQ(*params.message.text, "some text");
    EXPECT_EQ(*params.message.html, true);
    EXPECT_EQ(*params.message.force7bit, true);
    EXPECT_EQ(*params.message.source_mid, "mid");
    EXPECT_EQ(*params.message.message_id, "<123@yandex.ru>");
    EXPECT_EQ(*params.message.current_time, 1377);
    EXPECT_EQ(*params.recipients.to, "to@ya.ru,to2@ya.ru");
    EXPECT_EQ(*params.recipients.cc, "cc@ya.ru");
    EXPECT_EQ(*params.recipients.bcc, "bcc@ya.ru");
    EXPECT_THAT(*params.lids, UnorderedElementsAre("1", "2"));
}

TEST(SaveDraftParams, shouldSuccessfullyParse) {
    CREATE_REQ;
    RETURN_ARGS("uploaded_attach_stids", ({"stid1", "stid2"}));
    RETURN_ARGS("forward_mids", ({"mid1", "mid2"}));
    RETURN_ARGS("parts_json", ({partJson(), partJson()}));
    RETURN_ARGS("lids", ({"1", "2"}));

    RETURN_ARGS("disk_attaches_json", ({diskAttachJson(), diskAttachJson()}));
    RETURN_ARG_OPT("disk_attaches", "disk_attach");
    RETURN_ARG_OPT("from_mailbox", "from_mailbox");
    RETURN_ARG_OPT("from_name", "from_name");
    RETURN_ARG_OPT("subj", "subject");
    RETURN_ARG_OPT("text", "some text");
    RETURN_ARG_OPT("html", "yes");
    RETURN_ARG_OPT("force7bit", "yes");
    RETURN_ARG_OPT("source_mid", "mid");
    RETURN_ARG_OPT("message_id", "<123@yandex.ru>");
    RETURN_ARG_OPT("current_time", "1377");
    RETURN_ARG_OPT("to", "to@ya.ru,to2@ya.ru");
    RETURN_ARG_OPT("cc", "cc@ya.ru");
    RETURN_ARG_OPT("bcc", "bcc@ya.ru");
    RETURN_ARG_OPT("references", "asdf qwer");
    RETURN_ARG_OPT("inreplyto", "inreplyto");

    const auto params = getSaveDraft(REQ);

    EXPECT_EQ(*params.attaches.disk_attaches, "disk_attach");
    EXPECT_THAT(*params.attaches.disk_attaches_json, UnorderedElementsAre(diskAttach(), diskAttach()));
    EXPECT_THAT(*params.attaches.uploaded_attach_stids, UnorderedElementsAre("stid2", "stid1"));
    EXPECT_THAT(*params.attaches.forward_mids, UnorderedElementsAre("mid1", "mid2"));
    EXPECT_THAT(*params.attaches.parts_json, UnorderedElementsAre(messagePart(), messagePart()));
    EXPECT_EQ(*params.sender.from_mailbox, "from_mailbox");
    EXPECT_EQ(*params.sender.from_name, "from_name");
    EXPECT_EQ(*params.message.subj, "subject");
    EXPECT_EQ(*params.message.text, "some text");
    EXPECT_EQ(*params.message.html, true);
    EXPECT_EQ(*params.message.force7bit, true);
    EXPECT_EQ(*params.message.source_mid, "mid");
    EXPECT_EQ(*params.message.message_id, "<123@yandex.ru>");
    EXPECT_EQ(*params.message.current_time, 1377);
    EXPECT_EQ(*params.recipients.to, "to@ya.ru,to2@ya.ru");
    EXPECT_EQ(*params.recipients.cc, "cc@ya.ru");
    EXPECT_EQ(*params.recipients.bcc, "bcc@ya.ru");
    EXPECT_THAT(*params.lids, UnorderedElementsAre("1", "2"));
    EXPECT_EQ(*params.references, "asdf qwer");
    EXPECT_EQ(*params.inreplyto, "inreplyto");
}

TEST(SendMessageParams, shouldSuccessfullyParse) {
    CREATE_REQ;
    RETURN_ARGS("uploaded_attach_stids", ({"stid1", "stid2"}));
    RETURN_ARGS("forward_mids", ({"mid1", "mid2"}));
    RETURN_ARGS("parts_json", ({partJson(), partJson()}));
    RETURN_ARGS("lids", ({"1", "2"}));
    RETURN_ARGS("mentions", ({"a@ya.ru", "b@ya.ru"}));

    RETURN_ARGS("disk_attaches_json", ({diskAttachJson(), diskAttachJson()}));
    RETURN_ARG_OPT("disk_attaches", "disk_attach");
    RETURN_ARG_OPT("from_mailbox", "from_mailbox");
    RETURN_ARG_OPT("from_name", "from_name");
    RETURN_ARG_OPT("subj", "subject");
    RETURN_ARG_OPT("text", "some text");
    RETURN_ARG_OPT("html", "yes");
    RETURN_ARG_OPT("force7bit", "yes");
    RETURN_ARG_OPT("source_mid", "mid");
    RETURN_ARG_OPT("message_id", "<123@yandex.ru>");
    RETURN_ARG_OPT("current_time", "1377");
    RETURN_ARG_OPT("to", "to@ya.ru,to2@ya.ru");
    RETURN_ARG_OPT("cc", "cc@ya.ru");
    RETURN_ARG_OPT("bcc", "bcc@ya.ru");
    RETURN_ARG_OPT("references", "asdf qwer");
    RETURN_ARG_OPT("captcha_type", "type");
    RETURN_ARG("inreplyto", "inreplyto");
    RETURN_ARG("mark_as", "forwarded");
    RETURN_ARG_OPT("noanswer_remind_period", "1234567");
    RETURN_ARG_OPT("confirm_delivery", "no");
    RETURN_ARG("captcha_entered", "entered");
    RETURN_ARG("captcha_key", "key");
    RETURN_ARG_OPT("operation_id", "123");
    RETURN_ARG_OPT("captcha_passed", "no");

    const auto params = getSendMessage(REQ);

    EXPECT_THAT(*params.mentions, UnorderedElementsAre("a@ya.ru", "b@ya.ru"));
    EXPECT_EQ(*params.attaches.disk_attaches, "disk_attach");
    EXPECT_THAT(*params.attaches.disk_attaches_json, UnorderedElementsAre(diskAttach(), diskAttach()));
    EXPECT_THAT(*params.attaches.uploaded_attach_stids, UnorderedElementsAre("stid2", "stid1"));
    EXPECT_THAT(*params.attaches.forward_mids, UnorderedElementsAre("mid1", "mid2"));
    EXPECT_THAT(*params.attaches.parts_json, UnorderedElementsAre(messagePart(), messagePart()));
    EXPECT_EQ(*params.sender.from_mailbox, "from_mailbox");
    EXPECT_EQ(*params.sender.from_name, "from_name");
    EXPECT_EQ(*params.message.subj, "subject");
    EXPECT_EQ(*params.message.text, "some text");
    EXPECT_EQ(*params.message.html, true);
    EXPECT_EQ(*params.message.force7bit, true);
    EXPECT_EQ(*params.message.source_mid, "mid");
    EXPECT_EQ(*params.message.message_id, "<123@yandex.ru>");
    EXPECT_EQ(*params.message.current_time, 1377);
    EXPECT_EQ(*params.recipients.to, "to@ya.ru,to2@ya.ru");
    EXPECT_EQ(*params.recipients.cc, "cc@ya.ru");
    EXPECT_EQ(*params.recipients.bcc, "bcc@ya.ru");
    EXPECT_THAT(*params.lids, UnorderedElementsAre("1", "2"));
    EXPECT_EQ(*params.captcha_type, "type");
    EXPECT_EQ(*params.references, "asdf qwer");
    EXPECT_EQ(params.inreplyto->inreplyto, "inreplyto");
    EXPECT_EQ(params.inreplyto->mark_as, params::MarkAs::forwarded);
    EXPECT_EQ(*params.delivery.noanswer_remind_period, 1234567);
    EXPECT_EQ(*params.delivery.confirm_delivery, false);
    EXPECT_EQ(params.captcha->captcha_entered, "entered");
    EXPECT_EQ(params.captcha->captcha_key, "key");
    EXPECT_EQ(params.captcha_passed, false);
}

TEST(SendUndoParams, shouldThrowAnExceptionOnNegativeSendTime) {
    CREATE_REQ;
    RETURN_ARG("send_time", "-1");
    EXPECT_THROW(getSendDelayed(REQ, true), BadCast);
}

TEST(SendDelayedParams, shouldThrowAnExceptionOnNegativeSendTime) {
    CREATE_REQ;
    RETURN_ARG("send_time", "-1");
    EXPECT_THROW(getSendDelayed(REQ, false), BadCast);
}

template<class Func>
void shouldSuccessfullyParseUndoOrDelayed(Func func) {
    CREATE_REQ;
    RETURN_ARGS("uploaded_attach_stids", ({"stid1", "stid2"}));
    RETURN_ARGS("forward_mids", ({"mid1", "mid2"}));
    RETURN_ARGS("parts_json", ({partJson(), partJson()}));
    RETURN_ARGS("lids", ({"1", "2"}));
    RETURN_ARGS("mentions", ({"a@ya.ru", "b@ya.ru"}));

    RETURN_ARGS("disk_attaches_json", ({diskAttachJson(), diskAttachJson()}));
    RETURN_ARG_OPT("disk_attaches", "disk_attach");
    RETURN_ARG_OPT("from_mailbox", "from_mailbox");
    RETURN_ARG_OPT("from_name", "from_name");
    RETURN_ARG_OPT("subj", "subject");
    RETURN_ARG_OPT("text", "some text");
    RETURN_ARG_OPT("html", "yes");
    RETURN_ARG_OPT("force7bit", "yes");
    RETURN_ARG_OPT("source_mid", "mid");
    RETURN_ARG_OPT("message_id", "<123@yandex.ru>");
    RETURN_ARG_OPT("current_time", "1377");
    RETURN_ARG_OPT("to", "to@ya.ru,to2@ya.ru");
    RETURN_ARG_OPT("cc", "cc@ya.ru");
    RETURN_ARG_OPT("bcc", "bcc@ya.ru");
    RETURN_ARG_OPT("references", "asdf qwer");
    RETURN_ARG_OPT("captcha_type", "type");
    RETURN_ARG("inreplyto", "inreplyto");
    RETURN_ARG("mark_as", "forwarded");
    RETURN_ARG_OPT("noanswer_remind_period", "1234567");
    RETURN_ARG_OPT("confirm_delivery", "no");
    RETURN_ARG("captcha_entered", "entered");
    RETURN_ARG("captcha_key", "key");
    RETURN_ARG("send_time", "12345678");
    RETURN_ARG_OPT("operation_id", "123");
    RETURN_ARG_OPT("captcha_passed", "no");

    const auto params = func(REQ);

    EXPECT_EQ(*params.send.attaches.disk_attaches, "disk_attach");
    EXPECT_THAT(*params.send.attaches.disk_attaches_json, UnorderedElementsAre(diskAttach(), diskAttach()));
    EXPECT_THAT(*params.send.attaches.uploaded_attach_stids, UnorderedElementsAre("stid2", "stid1"));
    EXPECT_THAT(*params.send.attaches.forward_mids, UnorderedElementsAre("mid1", "mid2"));
    EXPECT_THAT(*params.send.attaches.parts_json, UnorderedElementsAre(messagePart(), messagePart()));
    EXPECT_EQ(*params.send.sender.from_mailbox, "from_mailbox");
    EXPECT_EQ(*params.send.sender.from_name, "from_name");
    EXPECT_EQ(*params.send.message.subj, "subject");
    EXPECT_EQ(*params.send.message.text, "some text");
    EXPECT_EQ(*params.send.message.html, true);
    EXPECT_EQ(*params.send.message.force7bit, true);
    EXPECT_EQ(*params.send.message.source_mid, "mid");
    EXPECT_EQ(*params.send.message.message_id, "<123@yandex.ru>");
    EXPECT_EQ(*params.send.message.current_time, 1377);
    EXPECT_EQ(*params.send.recipients.to, "to@ya.ru,to2@ya.ru");
    EXPECT_EQ(*params.send.recipients.cc, "cc@ya.ru");
    EXPECT_EQ(*params.send.recipients.bcc, "bcc@ya.ru");
    EXPECT_THAT(*params.send.lids, UnorderedElementsAre("1", "2"));
    EXPECT_EQ(*params.send.captcha_type, "type");
    EXPECT_EQ(*params.send.references, "asdf qwer");
    EXPECT_EQ(params.send.inreplyto->inreplyto, "inreplyto");
    EXPECT_EQ(params.send.inreplyto->mark_as, params::MarkAs::forwarded);
    EXPECT_EQ(*params.send.delivery.noanswer_remind_period, 1234567);
    EXPECT_EQ(*params.send.delivery.confirm_delivery, false);
    EXPECT_EQ(params.send.captcha->captcha_entered, "entered");
    EXPECT_EQ(params.send.captcha->captcha_key, "key");
    EXPECT_EQ(params.send_time, 12345678u);
    EXPECT_EQ(params.send.captcha_passed, false);
    EXPECT_THAT(*params.send.mentions, UnorderedElementsAre("a@ya.ru", "b@ya.ru"));
}

TEST(SendDelayedParams, shouldSuccessfullyParse) {
    shouldSuccessfullyParseUndoOrDelayed([](const auto& req) { return getSendDelayed(req, false); });
}

TEST(SendUndoParams, shouldSuccessfullyParse) {
    shouldSuccessfullyParseUndoOrDelayed([](const auto& req) { return getSendDelayed(req, true); });
}

TEST(CancelSendDelayedParams, shouldSuccessfullyParse) {
    CREATE_REQ;
    RETURN_ARG("mid", "32");

    const auto params = getCancelSendDelayed(REQ);

    EXPECT_EQ(params.mid, "32");
}

TEST(ComposeMessageParams, shouldSuccessfullyParse) {
    CREATE_REQ;
    RETURN_ARGS("uploaded_attach_stids", ({"stid1", "stid2"}));
    RETURN_ARGS("forward_mids", ({"mid1", "mid2"}));
    RETURN_ARGS("parts_json", ({partJson(), partJson()}));
    RETURN_ARGS("lids", ({"1", "2"}));

    RETURN_ARGS("disk_attaches_json", ({diskAttachJson(), diskAttachJson()}));
    RETURN_ARG_OPT("disk_attaches", "disk_attach");
    RETURN_ARG_OPT("from_mailbox", "from_mailbox");
    RETURN_ARG_OPT("from_name", "from_name");
    RETURN_ARG_OPT("subj", "subject");
    RETURN_ARG_OPT("text", "some text");
    RETURN_ARG_OPT("html", "yes");
    RETURN_ARG_OPT("force7bit", "yes");
    RETURN_ARG_OPT("source_mid", "mid");
    RETURN_ARG_OPT("message_id", "<123@yandex.ru>");
    RETURN_ARG_OPT("current_time", "1377");
    RETURN_ARG_OPT("to", "to@ya.ru,to2@ya.ru");
    RETURN_ARG_OPT("cc", "cc@ya.ru");
    RETURN_ARG_OPT("bcc", "bcc@ya.ru");
    RETURN_ARG_OPT("references", "asdf qwer");
    RETURN_ARG_OPT("inreplyto", "inreplyto");

    const auto params = getComposeMessage(REQ);

    EXPECT_EQ(*params.attaches.disk_attaches, "disk_attach");
    EXPECT_THAT(*params.attaches.disk_attaches_json, UnorderedElementsAre(diskAttach(), diskAttach()));
    EXPECT_THAT(*params.attaches.uploaded_attach_stids, UnorderedElementsAre("stid2", "stid1"));
    EXPECT_THAT(*params.attaches.forward_mids, UnorderedElementsAre("mid1", "mid2"));
    EXPECT_THAT(*params.attaches.parts_json, UnorderedElementsAre(messagePart(), messagePart()));
    EXPECT_EQ(*params.sender.from_mailbox, "from_mailbox");
    EXPECT_EQ(*params.sender.from_name, "from_name");
    EXPECT_EQ(*params.message.subj, "subject");
    EXPECT_EQ(*params.message.text, "some text");
    EXPECT_EQ(*params.message.html, true);
    EXPECT_EQ(*params.message.force7bit, true);
    EXPECT_EQ(*params.message.source_mid, "mid");
    EXPECT_EQ(*params.message.message_id, "<123@yandex.ru>");
    EXPECT_EQ(*params.message.current_time, 1377);
    EXPECT_EQ(*params.recipients.to, "to@ya.ru,to2@ya.ru");
    EXPECT_EQ(*params.recipients.cc, "cc@ya.ru");
    EXPECT_EQ(*params.recipients.bcc, "bcc@ya.ru");
    EXPECT_THAT(*params.lids, UnorderedElementsAre("1", "2"));
    EXPECT_EQ(*params.references, "asdf qwer");
    EXPECT_EQ(*params.inreplyto, "inreplyto");
}

TEST(WriteAttachParams, shouldSuccessfullyParse) {
    CREATE_REQ;
    RETURN_ARG("filename", "name");
    RETURN_BODY("body");

    const auto params = getWriteAttachParams(REQ);

    EXPECT_EQ(params.filename, "name");
    EXPECT_EQ(params.raw_body, "body");
}

TEST(ListUnsubscribeParams, shouldSuccessfullyParse) {
    {
        CREATE_REQ;
        RETURN_ARG("to", "a@b.ru");
        RETURN_ARG_OPT("subject", "subj");
        RETURN_ARG_OPT("body", "body");
        RETURN_ARG_OPT("from_mailbox", "devnull@yandex.ru");

        const auto params = getListUnsubscribe(REQ);

        EXPECT_EQ(params.to, "a@b.ru");
        EXPECT_EQ(*params.subject, "subj");
        EXPECT_EQ(*params.body, "body");
        EXPECT_EQ(*params.from_mailbox, "devnull@yandex.ru");
    }

    {
        CREATE_REQ;
        RETURN_ARG("to", "a@b.ru");
        RETURN_ARG_OPT_EMPTY("subject");
        RETURN_ARG_OPT_EMPTY("body");
        RETURN_ARG_OPT_EMPTY("from_mailbox");

        const auto params = getListUnsubscribe(REQ);

        EXPECT_EQ(params.to, "a@b.ru");
        EXPECT_EQ(params.subject, boost::none);
        EXPECT_EQ(params.body, boost::none);
        EXPECT_EQ(params.from_mailbox, boost::none);
    }
}

}
}
