#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/sendbernar/client/include/handler_url.h>
#include <mail/sendbernar/client/include/internal/writer.h>


#define EXPECT_POSTARGS(name, value1, value2) \
    EXPECT_EQ(req.postArgs.arguments[name].size(), 2ul); \
    EXPECT_THAT(req.postArgs.arguments[name], UnorderedElementsAre(value1, value2))

#define EXPECT_GETARG(name, value) \
    EXPECT_EQ(req.getArgs.arguments[name].size(), 1ul); \
    EXPECT_EQ(req.getArgs.arguments[name][0], value)

#define EXPECT_POSTARG(name, value) \
    EXPECT_EQ(req.postArgs.arguments[name].size(), 1ul); \
    EXPECT_EQ(req.postArgs.arguments[name][0], value)

#define EXPECT_HEADER(name, value) \
    EXPECT_EQ(req.headers.headers[name].size(), 1ul); \
    EXPECT_EQ(req.headers.headers[name][0], value)


using namespace testing;

namespace sendbernar::tests {

params::CommonParams commonParams() {
    params::CommonParams params;
    params.uid = "uid";
    params.caller = "caller";
    params.requestId = "reqid";
    params.realIp = "127.0.0.1";
    params.originalHost = "mail.ru";

    return params;
}

params::UserJournalParams ujParams() {
    params::UserJournalParams params;
    params.connectionId = "connectionId";
    params.expBoxes = "expBoxes";
    params.enabledExpBoxes = "enabledExpBoxes";
    params.clientType = "clientType";
    params.clientVersion = "clientVersion";
    params.userAgent = "userAgent";
    params.yandexUid = "yandexUid";
    params.iCookie = "iCookie";

    return params;
}

TEST(UserJournalParams, shouldWriteParamsToHeaders) {
    request::Request req;
    auto params = ujParams();

    request::write(req, params);

    EXPECT_HEADER("connection_id", "connectionId");
    EXPECT_HEADER("X-Yandex-ExpBoxes", "expBoxes");
    EXPECT_HEADER("X-Yandex-EnabledExpBoxes", "enabledExpBoxes");
    EXPECT_HEADER("X-Yandex-ClientType", "clientType");
    EXPECT_HEADER("X-Yandex-ClientVersion", "clientVersion");
    EXPECT_HEADER("yandexuid", "yandexUid");
    EXPECT_HEADER("icookie", "iCookie");
    EXPECT_HEADER("User-Agent", "userAgent");
    EXPECT_EQ(req.headers.headers.size(), 8ul);
    EXPECT_TRUE(req.postArgs.arguments.empty());
}

TEST(CommonParams, shouldWriteParamsToGetAndHeaders) {
    request::Request req;
    auto params = commonParams();

    request::write(req, params);

    EXPECT_TRUE(req.postArgs.arguments.empty());
    EXPECT_GETARG("uid", "uid");
    EXPECT_GETARG("caller", "caller");
    EXPECT_HEADER("X-Request-Id", "reqid");
    EXPECT_HEADER("X-Real-Ip", "127.0.0.1");
    EXPECT_HEADER("X-Original-Host", "mail.ru");
    EXPECT_EQ(req.headers.headers.size(), 3ul);
}


TEST(ContinueSendingMessage, shouldWriteParams) {
    params::ContinueSendingMessage params;
    params.mid = "mid";
    params.notify_on_send = false;
    params.postprocess.nonempty_subject = "nonempty_subject";
    params.postprocess.message_id = "message_id";
    params.postprocess.message_date = 10;
    params.postprocess.nonempty_recipients.to = "to@yandex.ru";
    params.postprocess.nonempty_recipients.cc = "cc@yandex.ru";
    params.postprocess.nonempty_recipients.bcc = "bcc@yandex.ru";
    params.postprocess.lids = boost::make_optional<std::vector<std::string>>({"lid1", "lid2"});
    params.postprocess.source_mid = "source_mid";
    params.postprocess.noanswer_remind_period = 10;
    params.postprocess.inreplyto = params::InReplyTo();
    params.postprocess.inreplyto->inreplyto = "inreplyto";
    params.postprocess.inreplyto->mark_as = params::MarkAs::forwarded;
    params.postprocess.forward_mids = boost::make_optional<std::vector<std::string>>({"mid1", "mid2"});;
    params.mentions = boost::make_optional<std::vector<std::string>>({"a@ya.ru", "b@ya.ru"});

    request::Request req = request::callbackDelayedOrUndoMessage(commonParams(), ujParams(), params, true);

    EXPECT_GETARG("mid", "mid");
    EXPECT_POSTARG("notify_on_send", "no");
    EXPECT_POSTARG("nonempty_subject", "nonempty_subject");
    EXPECT_POSTARG("message_id", "message_id");
    EXPECT_POSTARG("message_date", "10");
    EXPECT_POSTARG("to", "to@yandex.ru");
    EXPECT_POSTARG("cc", "cc@yandex.ru");
    EXPECT_POSTARG("bcc", "bcc@yandex.ru");
    EXPECT_POSTARGS("lids", "lid1", "lid2");
    EXPECT_POSTARG("source_mid", "source_mid");
    EXPECT_POSTARG("noanswer_remind_period", "10");
    EXPECT_POSTARG("inreplyto", "inreplyto");
    EXPECT_POSTARG("mark_as", "forwarded");
    EXPECT_POSTARGS("forward_mids", "mid1", "mid2");
    EXPECT_POSTARGS("mentions", "a@ya.ru", "b@ya.ru");
}

}
