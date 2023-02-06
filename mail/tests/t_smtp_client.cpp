#include <gtest/gtest.h>

#include "dummy_smtp_server.h"

#include <request_data.h>
#include <server_response.h>

#include <ymod_smtpclient/smtp_point.h>
#include <ymod_smtpclient/call.h>
#include <yplatform/application.h>
#include <yplatform/log.h>

#include <boost/algorithm/string.hpp>
#include <future>

using namespace yplatform;
using namespace ymod_smtpclient;
using testing::server::SmtpServer;
using testing::server::SMTP_HOST;
using testing::server::SMTP_PORT;
using Headers = std::multimap<std::string, std::string>;
using Recipients = std::vector<RcptTo>;

namespace {

constexpr unsigned short SMTP_SSL_PORT = 1465;

SmtpPoint getSmtpPoint(uint16_t smtpPort = SMTP_PORT) {
    SmtpPoint testSmtpPoint;
    testSmtpPoint.host = SMTP_HOST;
    testSmtpPoint.port = smtpPort;
    return testSmtpPoint;
}

std::string buildMessage(std::string body, const Headers& headers) {
    std::string message;
    for (const auto& header : headers) {
        message += header.first;
        message += ": ";
        message += header.second;
        message += "\r\n";
    }
    return message + "\r\n" + body + "\r\n";
}

Request buildRequest(
    SmtpPoint dest,
    boost::optional<AuthData> authData,
    MailFrom mailFrom,
    const Recipients& recipients,
    std::string message)
{
    RequestBuilder builder;
    builder.address(dest);
    if (authData) {
        builder.auth(authData.get());
    }
    builder.mailfrom(mailFrom);
    for (const auto& recipient : recipients) {
        builder.addRcpt(recipient);
    }
    builder.message(message);
    return builder.release();
}

Request buildDefaultRequest(
    const Recipients& recipients = {{"to@bar.com"}},
    uint16_t smtpPort = SMTP_PORT)
{
    Headers headers{{"From", "from@foo.ru"}};
    for (const auto& recipient : recipients) {
        headers.emplace("To", recipient.email);
    }
    auto msg = buildMessage("Hello", headers);
    return buildRequest(getSmtpPoint(smtpPort), {}, {"from@foo.com"}, recipients, msg);
}

auto getCtx() {
    return boost::make_shared<task_context>("context");
}

auto findSmtpClient() {
    return find<Call>("smtp_client");
}

struct Result {
    error::Code errc;
    Response response;
};

Result runSmtpClient(boost::shared_ptr<Call> client, Request request, Options options = Options()) {
    std::promise<Result> promise;
    auto f = promise.get_future();
    client->asyncRun(getCtx(), request, options,
        [&promise](error::Code errc, Response resp) {
            auto res = std::move(promise);
            res.set_value({errc, resp});
        });
    return f.get();
}

Result runSmtpClient(Request request, Options options = Options()) {
    return runSmtpClient(findSmtpClient(), request, options);
}

inline void checkCommand(std::string expected, std::string actual) {
    ASSERT_TRUE(boost::starts_with(actual, expected));
}

inline void checkMessage(std::string expected, std::string actual) {
    ASSERT_EQ(actual, expected);
}

} // namespace

TEST(SmtpClient, ConnectFailedAfterRefuse) {
    auto result = runSmtpClient(buildDefaultRequest());
    ASSERT_EQ(result.errc, error::ConnectError);
}

TEST(SmtpClient, CommandTimedOut) {
    using namespace std::chrono_literals;
    SmtpServer server([](auto& /*session*/) {
        std::this_thread::sleep_for(70ms);
    });

    Options opts;
    opts.timeouts.command = 50ms;
    auto result = runSmtpClient(buildDefaultRequest(), opts);
    ASSERT_EQ(result.errc, error::RequestTimedOut);
}

TEST(SmtpClient, DataTimedOut) {
    using namespace std::chrono_literals;
    SmtpServer server([](auto& session) {
        session.writeLine("220 Greeting");
        checkCommand("EHLO", session.handleCommand("250 Helo"));
        checkCommand("MAIL FROM", session.handleCommand("250 MailFrom"));
        checkCommand("RCPT TO", session.handleCommand("250 RcptTo"));
        checkCommand("DATA", session.handleCommand("354 Data"));
        session.readLine(); // message
        std::this_thread::sleep_for(70ms);
        session.handleMessage("250 Queued ok");
    });
    Options opts;
    opts.timeouts.data = 50ms;
    auto result = runSmtpClient(buildDefaultRequest(), opts);
    ASSERT_EQ(result.errc, error::RequestTimedOut);
}

TEST(SmtpClient, SendMailSuccess) {
    SmtpServer server([](auto& session) {
        session.writeLine("220 Greeting");
        checkCommand("EHLO", session.handleCommand("250 Helo"));
        checkCommand("MAIL FROM", session.handleCommand("250 MailFrom"));
        checkCommand("RCPT TO", session.handleCommand("250 RcptTo"));
        checkCommand("DATA", session.handleCommand("354 Data"));
        checkMessage("From: from@foo.ru\r\nTo: to@bar.com\r\n\r\nHello\r\n",
                session.handleMessage("250 Queued ok"));
    });
    auto result = runSmtpClient(buildDefaultRequest());
    ASSERT_EQ(result.errc, error::Success);

    auto response = result.response;
    ASSERT_TRUE(response.session);
    EXPECT_EQ(response.session.get().replyCode, 250);
    EXPECT_EQ(response.session.get().data, "Queued ok");

    EXPECT_TRUE(response.rcpts.empty());
}

TEST(SmtpClient, ManyRecipientsSuccess) {
    SmtpServer server([](auto& session) {
        session.writeLine("220 Greeting");
        checkCommand("EHLO", session.handleCommand("250 Helo"));
        checkCommand("MAIL FROM", session.handleCommand("250 MailFrom"));
        checkCommand("RCPT TO", session.handleCommand("250 RcptTo"));
        checkCommand("RCPT TO", session.handleCommand("250 RcptTo"));
        checkCommand("DATA", session.handleCommand("354 Data"));
        checkMessage("From: from@foo.ru\r\nTo: to1@bar.com\r\nTo: to2@bar.com\r\n\r\nHello\r\n",
                session.handleMessage("250 Queued ok"));
    });
    auto result = runSmtpClient(buildDefaultRequest({{"to1@bar.com"}, {"to2@bar.com"}}));
    ASSERT_EQ(result.errc, error::Success);

    auto response = result.response;
    ASSERT_TRUE(response.session);
    EXPECT_EQ(response.session.get().replyCode, 250);
    EXPECT_EQ(response.session.get().data, "Queued ok");

    EXPECT_TRUE(response.rcpts.empty());
}

TEST(SmtpClient, TwoRecipientsButOneUnknown) {
    SmtpServer server([](auto& session) {
        session.writeLine("220 Greeting");
        checkCommand("EHLO", session.handleCommand("250 Helo"));
        checkCommand("MAIL FROM", session.handleCommand("250 MailFrom"));
        checkCommand("RCPT TO", session.handleCommand("250 RcptTo"));
        checkCommand("RCPT TO", session.handleCommand("550 RcptTo unknown user"));
        checkCommand("DATA", session.handleCommand("354 Data"));
        checkMessage("From: from@foo.ru\r\nTo: to1@bar.com\r\nTo: to2@bar.com\r\n\r\nHello\r\n",
                session.handleMessage("250 Queued ok"));
    });
    auto result = runSmtpClient(buildDefaultRequest({{"to1@bar.com"}, {"to2@bar.com"}}));
    ASSERT_EQ(result.errc, error::PartialSend);

    auto response = result.response;
    ASSERT_TRUE(response.session);
    EXPECT_EQ(response.session.get().replyCode, 250);
    EXPECT_EQ(response.session.get().data, "Queued ok");

    ASSERT_EQ(response.rcpts.count("to2@bar.com"), 1UL);
    EXPECT_EQ(response.rcpts["to2@bar.com"].replyCode, 550);
}

TEST(SmtpClient, SingleUnknownRcpt) {
    SmtpServer server([](auto& session) {
        session.writeLine("220 Greeting");
        checkCommand("EHLO", session.handleCommand("250 Helo"));
        checkCommand("MAIL FROM", session.handleCommand("250 MailFrom"));
        checkCommand("RCPT TO", session.handleCommand("550 RcptTo unknown user"));
        checkCommand("DATA", session.handleCommand("354 Data"));
        checkMessage("From: from@foo.ru\r\nTo: to@bar.com\r\n\r\nHello\r\n",
                session.handleMessage("250 Queued ok"));
    });
    auto result = runSmtpClient(buildDefaultRequest());
    ASSERT_EQ(result.errc, error::PartialSend);

    auto response = result.response;
    ASSERT_TRUE(response.session);
    EXPECT_EQ(response.session.get().replyCode, 250);

    ASSERT_EQ(response.rcpts.count("to@bar.com"), 1UL);
    EXPECT_EQ(response.rcpts["to@bar.com"].replyCode, 550);
}

TEST(SmtpClient, HasUnknownRcptWithAllowRcptToErrorsFalse) {
    SmtpServer server([](auto& session) {
        session.writeLine("220 Greeting");
        checkCommand("EHLO", session.handleCommand("250 Helo"));
        checkCommand("MAIL FROM", session.handleCommand("250 MailFrom"));
        checkCommand("RCPT TO", session.handleCommand("250 RcptTo"));
        checkCommand("RCPT TO", session.handleCommand("550 RcptTo unknown user"));
    });
    Options opts;
    opts.allowRcptToErrors = false;
    auto result = runSmtpClient(buildDefaultRequest({{"to1@bar.com"}, {"to2@bar.com"}}), opts);
    ASSERT_EQ(result.errc, error::BadRecipient);
}

TEST(SmtpClient, AttemptToAuthorizeWithoutSSL) {
    SmtpServer server([](auto& session) {
        session.writeLine("220 Greeting");
        checkCommand("EHLO", session.handleCommand("250 Helo"));
    });
    AuthData plainAuth = AuthData::PLAIN("login", "pwd");
    auto msg = buildMessage("Hello", {});
    auto request = buildRequest(getSmtpPoint(), plainAuth, {"from@foo.com"}, {{"to@bar.com"}}, msg);

    auto result = runSmtpClient(request);
    ASSERT_EQ(result.errc, error::AuthWithoutSSL);
}

TEST(SmtpClient, ServerExtensionsSuccess) {
    SmtpServer server([](auto& session) {
        session.writeLine("220 Greeting");
        checkCommand("EHLO", session.readLine());
        session.writeLine("250-localhost");
        session.writeLine("250-SIZE 1024");
        session.writeLine("250 ENHANCEDSTATUSCODES");
        checkCommand("MAIL FROM", session.handleCommand("250 MailFrom"));
        checkCommand("RCPT TO", session.handleCommand("250 RcptTo"));
        checkCommand("DATA", session.handleCommand("354 Data"));
        checkMessage("From: from@foo.ru\r\nTo: to@bar.com\r\n\r\nHello\r\n",
                session.handleMessage("250 Queued ok"));
    });
    auto result = runSmtpClient(buildDefaultRequest());
    ASSERT_EQ(result.errc, error::Success);
}

TEST(SmtpClient, ServerExtensionsParseError) {
    SmtpServer server([](auto& session) {
        session.writeLine("220 Greeting");
        checkCommand("EHLO", session.readLine());
        session.writeLine("250-localhost");
        session.writeLine("250-SIZE BAD_SIZE_VALUE");
        session.writeLine("250 ENHANCEDSTATUSCODES");
    });
    auto result = runSmtpClient(buildDefaultRequest());
    ASSERT_EQ(result.errc, error::ProtocolError);
}

TEST(SmtpClient, ServerExtensionsWithPipelineSuccess) {
    SmtpServer server([](auto& session) {
        session.writeLine("220 Greeting");
        checkCommand("EHLO", session.readLine());
        session.writeLine("250-localhost");
        session.writeLine("250 PIPELINING");
        checkCommand("MAIL FROM", session.readLine());
        checkCommand("RCPT TO", session.readLine());
        checkCommand("DATA", session.readLine());
        session.writeLine("250 MailFrom");
        session.writeLine("250 RcptTo");
        session.writeLine("354 Data");
        session.handleMessage("250 Queued ok");
    });
    auto result = runSmtpClient(buildDefaultRequest());
    ASSERT_EQ(result.errc, error::Success);
}

TEST(SmtpClient, ReuseSession) {
    SmtpServer server([](auto& session) {
        session.writeLine("220 Greeting");
        checkCommand("EHLO", session.handleCommand("250 Helo"));
        checkCommand("MAIL FROM", session.handleCommand("250 MailFrom"));
        checkCommand("RCPT TO", session.handleCommand("250 RcptTo"));
        checkCommand("DATA", session.handleCommand("354 Data"));
        checkMessage("From: from@foo.ru\r\nTo: to@bar.com\r\n\r\nHello\r\n",
                session.handleMessage("250 Queued ok"));

        checkCommand("MAIL FROM", session.handleCommand("250 MailFrom"));
        checkCommand("RCPT TO", session.handleCommand("250 RcptTo"));
        checkCommand("DATA", session.handleCommand("354 Data"));
        checkMessage("From: from@foo.ru\r\nTo: to@bar.com\r\n\r\nHello\r\n",
                session.handleMessage("250 Queued ok"));
    });
    Options opts;
    opts.reuseSession = true;

    auto client = findSmtpClient();
    auto result = runSmtpClient(client, buildDefaultRequest());
    ASSERT_EQ(result.errc, error::Success);

    result = runSmtpClient(client, buildDefaultRequest());
    ASSERT_EQ(result.errc, error::Success);
}

TEST(SmtpClient, DotStuffing) {
    SmtpServer server([](auto& session) {
        session.writeLine("220 Greeting");
        checkCommand("EHLO", session.handleCommand("250 Helo"));
        checkCommand("MAIL FROM", session.handleCommand("250 MailFrom"));
        checkCommand("RCPT TO", session.handleCommand("250 RcptTo"));
        checkCommand("DATA", session.handleCommand("354 Data"));
        checkMessage("From: from@foo.ru\r\n\r\nstart\r\n..\r\n....\r\nend\r\n",
                session.handleMessage("250 Queued ok"));
    });

    auto msg = buildMessage("start\r\n.\r\n...\r\nend", {{"From", "from@foo.ru"}});
    auto request = buildRequest(getSmtpPoint(), {}, {"from@foo.com"}, {{"to@bar.com"}}, msg);
    auto result = runSmtpClient(request);
    ASSERT_EQ(result.errc, error::Success);
}

TEST(SmtpClient, CreateSmtpSession) {
    SmtpServer server([](auto& session) {
        session.writeLine("220 Greeting");
    });
    auto client = findSmtpClient();
    auto smtpSession = client->createSmtpSession({}, {});

    smtpSession->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none, [](auto errc) {
        ASSERT_FALSE(errc);
    });
    smtpSession->asyncGreeting([](auto errc, auto resp) {
        ASSERT_FALSE(errc);
        ASSERT_FALSE(resp.dataLines.empty());
        EXPECT_EQ(resp.dataLines.front(), "Greeting");
    });
}

TEST(SmtpClient, CreateSmtpSessionWithTimeoutsFromSettings) {
    using namespace std::chrono_literals;
    SmtpServer server([](auto& session) {
        std::this_thread::sleep_for(120ms);
        session.writeLine("220 Greeting");
    });
    auto client = findSmtpClient();
    auto smtpSession = client->createSmtpSession({});

    smtpSession->asyncConnect(SMTP_HOST, SMTP_PORT, boost::none, [](auto errc) {
        ASSERT_FALSE(errc);
    });
    smtpSession->asyncGreeting([](auto errc, auto) {
        ASSERT_EQ(errc, error::RequestTimedOut);
    });
}

TEST(SmtpClient, SmtpSslNotDefaultPortWithOptionUseSslTrue) {
    SmtpServer server(SMTP_PORT, [](auto&) {
    });

    Options opts;
    opts.useSsl = true;
    auto result = runSmtpClient(buildDefaultRequest({{"to@bar.com"}}, SMTP_PORT), opts);
    ASSERT_EQ(result.errc, error::SslError);
}

TEST(SmtpClient, SmtpSslDefaultPortWithoutOptionUseSsl) {
    SmtpServer server(SMTP_SSL_PORT, [](auto&) {
    });

    auto result = runSmtpClient(buildDefaultRequest({{"to@bar.com"}}, SMTP_SSL_PORT));
    ASSERT_EQ(result.errc, error::SslError);
}

TEST(SmtpClient, SmtpSslDefaultPortWithOptionUseSslFalse) {
    SmtpServer server(SMTP_SSL_PORT, [](auto& session) {
        session.writeLine("220 Greeting");
        checkCommand("EHLO", session.handleCommand("250 Helo"));
        checkCommand("MAIL FROM", session.handleCommand("250 MailFrom"));
        checkCommand("RCPT TO", session.handleCommand("250 RcptTo"));
        checkCommand("DATA", session.handleCommand("354 Data"));
        checkMessage("From: from@foo.ru\r\nTo: to@bar.com\r\n\r\nHello\r\n",
                session.handleMessage("250 Queued ok"));
    });
    Options opts;
    opts.useSsl = false;
    auto result = runSmtpClient(buildDefaultRequest({{"to@bar.com"}}, SMTP_SSL_PORT), opts);
    ASSERT_EQ(result.errc, error::Success);
}

TEST(SmtpClient, SmtpSslNotDefaultPortWithStartTls) {
    SmtpServer server(SMTP_PORT, [](auto& session) {
        session.writeLine("220 Greeting");
        checkCommand("EHLO", session.readLine());
        session.writeLine("250-localhost");
        session.writeLine("250-SIZE 1024");
        session.writeLine("250 STARTTLS");
        checkCommand("STARTTLS", session.handleCommand("220 STARTTLS"));
    });
    auto result = runSmtpClient(buildDefaultRequest({{"to@bar.com"}}, SMTP_PORT));
    ASSERT_EQ(result.errc, error::SslError);
}
