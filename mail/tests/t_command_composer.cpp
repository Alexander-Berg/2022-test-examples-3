#include <gtest/gtest.h>
#include <command_composer.h>
#include <boost/asio/ip/host_name.hpp>

using namespace ymod_smtpclient;
using namespace testing;

TEST(Composer, HelloCommand) {
    EXPECT_EQ(Composer::HELLO().str(), "EHLO " + boost::asio::ip::host_name() + "\r\n");
    EXPECT_EQ(Composer::HELLO(SmtpPoint::smtp).str(), "EHLO " + boost::asio::ip::host_name() + "\r\n");
    EXPECT_EQ(Composer::HELLO(SmtpPoint::lmtp).str(), "LHLO " + boost::asio::ip::host_name() + "\r\n");
    EXPECT_EQ(Composer::HELLO(SmtpPoint::lmtp, "hostname").str(), "LHLO hostname\r\n");
}

TEST(Composer, AuthCommand) {
    EXPECT_EQ(Composer::AUTH(sasl::Mechanism::Login).str(), "AUTH LOGIN\r\n");
    EXPECT_EQ(Composer::AUTH(sasl::Mechanism::Plain).str(), "AUTH PLAIN\r\n");
    EXPECT_EQ(Composer::AUTH(sasl::Mechanism::Xoauth2).str(), "AUTH XOAUTH2\r\n");
}

TEST(Composer, MailfromCommand) {
    MailFrom mailfrom("example@ya.by");
    // without envid
    EXPECT_EQ(Composer::MAIL_FROM(mailfrom).str(), "MAIL FROM:<example@ya.by>\r\n");
    // with envid
    mailfrom.envid = "Env-Id";
    EXPECT_EQ(Composer::MAIL_FROM(mailfrom).str(), "MAIL FROM:<example@ya.by> ENVID=Env-Id\r\n");
    // email with brackets
    mailfrom.email = "<email-with-brackets@ya.ru>";
    EXPECT_EQ(Composer::MAIL_FROM(mailfrom).str(), "MAIL FROM:<email-with-brackets@ya.ru> ENVID=Env-Id\r\n");
    // only brackets
    mailfrom.email = "<>";
    EXPECT_EQ(Composer::MAIL_FROM(mailfrom).str(), "MAIL FROM:<> ENVID=Env-Id\r\n");
    // empty email
    mailfrom.email = "";
    EXPECT_EQ(Composer::MAIL_FROM(mailfrom).str(), "MAIL FROM:<> ENVID=Env-Id\r\n");
}

TEST(Composer, RcptToCommand) {
    RcptTo rcpt("rcpt@ya.ru");
    EXPECT_EQ(Composer::RCPT_TO(rcpt).str(), "RCPT TO:<rcpt@ya.ru>\r\n");
    // ignore notify mode if enableDsn=false
    rcpt.notifyModes.push_back(NotifyMode::Never);
    EXPECT_EQ(Composer::RCPT_TO(rcpt).str(), "RCPT TO:<rcpt@ya.ru>\r\n");
    // add notify modes
    rcpt.notifyModes.clear();
    rcpt.notifyModes.push_back(NotifyMode::Success);
    rcpt.notifyModes.push_back(NotifyMode::Failure);
    EXPECT_EQ(Composer::RCPT_TO(rcpt, true).str(), "RCPT TO:<rcpt@ya.ru> NOTIFY=SUCCESS,FAILURE\r\n");
}

TEST(Composer, ConstantCommands) {
    EXPECT_EQ(Composer::DATA_START().str(), "DATA\r\n");
    EXPECT_EQ(Composer::STARTTLS().str(), "STARTTLS\r\n");
    EXPECT_EQ(Composer::RSET().str(), "RSET\r\n");
    EXPECT_EQ(Composer::QUIT().str(), "QUIT\r\n");
    EXPECT_EQ(Composer::WITHOUT_NAME("args").str(), "args\r\n");
    EXPECT_EQ(Composer::WITHOUT_NAME("args", std::string("debug")).str(), "args\r\n");
}

TEST(Composer, DebugCommands) {
    EXPECT_EQ(Composer::WITHOUT_NAME("args").debugStr(), "args");
    EXPECT_EQ(Composer::WITHOUT_NAME("args", std::string("debug")).debugStr(), "debug");

    auto command = Composer::AUTH(sasl::Mechanism::Plain);
    command.args = "args";
    command.debugArgs = "<secret>";

    EXPECT_EQ(command.debugStr(), "AUTH PLAIN <secret>");
}

