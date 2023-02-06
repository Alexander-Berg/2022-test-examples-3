#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include "parse.h"


using namespace testing;

namespace sendbernar {
namespace tests {

TEST(RemindMessageCallbackParams, shouldSuccessfullyParse) {
    CREATE_REQ;
    RETURN_ARG("mid", "mid1");
    RETURN_ARG("lang", "ru");
    RETURN_ARG("date", "21 мая 1993");
    RETURN_ARG("account", "yapoptest@yandex.ru");

    const auto params = getRemindMessageCallback(REQ);

    EXPECT_EQ(params.mid, "mid1");
    EXPECT_EQ(params.lang, "ru");
    EXPECT_EQ(params.date, "21 мая 1993");
    EXPECT_EQ(params.account, "yapoptest@yandex.ru");
}

TEST(NoAnswerRemindCallbackParams, shouldSuccessfullyParse) {
    CREATE_REQ;
    RETURN_ARG("message_id", "message_id");
    RETURN_ARG("lang", "ru");
    RETURN_ARG("date", "21 мая 1993");

    const auto params = getNoAnswerRemindCallback(REQ);

    EXPECT_EQ(params.message_id, "message_id");
    EXPECT_EQ(params.lang, "ru");
    EXPECT_EQ(params.date, "21 мая 1993");
}

TEST(ContinueSendingMessageCallbackParams, shouldSuccessfullyParse) {
    CREATE_REQ;

    RETURN_ARGS("mentions", ({"a@ya.ru", "b@ya.ru"}));
    RETURN_ARG("mid", "mid");
    RETURN_ARG("nonempty_subject", "nonempty_subject");
    RETURN_ARG("message_id", "message_id");
    RETURN_ARG("message_date", "10");
    RETURN_ARG_OPT("to", "to@yandex.ru");
    RETURN_ARG_OPT("cc", "cc@yandex.ru");
    RETURN_ARG_OPT("bcc", "bcc@yandex.ru");
    RETURN_ARGS("lids", ({"lid1", "lid2"}));
    RETURN_ARG_OPT("source_mid", "source_mid");
    RETURN_ARG_OPT("noanswer_remind_period", "10");
    RETURN_ARG("inreplyto", "inreplyto");
    RETURN_ARG("mark_as", "forwarded");
    RETURN_ARG("notify_on_send", "no");
    RETURN_ARGS("forward_mids", ({"mid1", "mid2"}));

    const auto params = getContinueSendingMessage(REQ);

    EXPECT_EQ(params.mid, "mid");
    EXPECT_EQ(params.postprocess.nonempty_subject, "nonempty_subject");
    EXPECT_EQ(params.postprocess.message_id, "message_id");
    EXPECT_EQ(params.postprocess.message_date, 10);
    EXPECT_EQ(*params.postprocess.nonempty_recipients.to, "to@yandex.ru");
    EXPECT_EQ(*params.postprocess.nonempty_recipients.cc, "cc@yandex.ru");
    EXPECT_EQ(*params.postprocess.nonempty_recipients.bcc, "bcc@yandex.ru");
    EXPECT_EQ(params.postprocess.lids, boost::make_optional<std::vector<std::string>>({"lid1", "lid2"}));
    EXPECT_EQ(params.notify_on_send, false);
    EXPECT_EQ(*params.postprocess.source_mid, "source_mid");
    EXPECT_EQ(*params.postprocess.noanswer_remind_period, 10);
    EXPECT_EQ(params.postprocess.inreplyto->inreplyto, "inreplyto");
    EXPECT_EQ(params.postprocess.inreplyto->mark_as, params::MarkAs::forwarded);
    EXPECT_EQ(params.postprocess.forward_mids, boost::make_optional<std::vector<std::string>>({"mid1", "mid2"}));
    EXPECT_THAT(*params.mentions, UnorderedElementsAre("a@ya.ru", "b@ya.ru"));
}

}
}
