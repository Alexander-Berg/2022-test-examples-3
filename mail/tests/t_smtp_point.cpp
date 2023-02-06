#include <gtest/gtest.h>
#include <ymod_smtpclient/smtp_point.h>

using namespace ymod_smtpclient;
using namespace testing;

TEST(SmtpPoint, ParseOnlyHosts) {
    auto point = SmtpPoint::fromString("mail.yandex.ru");
    EXPECT_EQ(point.proto, SmtpPoint::smtp);
    EXPECT_EQ(point.host, "mail.yandex.ru");
    EXPECT_EQ(point.port, SMTP_DEFAULT_PORT);

    point = SmtpPoint::fromString("[::0]");
    EXPECT_EQ(point.proto, SmtpPoint::smtp);
    EXPECT_EQ(point.host, "::0");
    EXPECT_EQ(point.port, SMTP_DEFAULT_PORT);
}

TEST(SmtpPoint, ParseSmtpProtoHost) {
    auto point = SmtpPoint::fromString("smtp://mxfront1j.mail.yandex.net");
    EXPECT_EQ(point.proto,  SmtpPoint::smtp);
    EXPECT_EQ(point.host, "mxfront1j.mail.yandex.net");
    EXPECT_EQ(point.port, SMTP_DEFAULT_PORT);

    point = SmtpPoint::fromString("SmTp://[127.0.0.1]");
    EXPECT_EQ(point.proto, SmtpPoint::smtp);
    EXPECT_EQ(point.host, "127.0.0.1");
    EXPECT_EQ(point.port, SMTP_DEFAULT_PORT);
}

TEST(SmtpPoint, ParseLmtpProtoHost) {
    auto point = SmtpPoint::fromString("lmtp://mxfront1j.mail.yandex.net");
    EXPECT_EQ(point.proto,  SmtpPoint::lmtp);
    EXPECT_EQ(point.host, "mxfront1j.mail.yandex.net");
    EXPECT_EQ(point.port, SMTP_DEFAULT_PORT);

    point = SmtpPoint::fromString("LMtP://[127.0.0.1]");
    EXPECT_EQ(point.proto, SmtpPoint::lmtp);
    EXPECT_EQ(point.host, "127.0.0.1");
    EXPECT_EQ(point.port, SMTP_DEFAULT_PORT);
}


TEST(SmtpPoint, ParseHostPort) {
    auto point = SmtpPoint::fromString("localhost:465");
    EXPECT_EQ(point.proto, SmtpPoint::smtp);
    EXPECT_EQ(point.host, "localhost");
    EXPECT_EQ(point.port, 465);

    point = SmtpPoint::fromString("[::0]:1234");
    EXPECT_EQ(point.proto, SmtpPoint::smtp);
    EXPECT_EQ(point.host, "::0");
    EXPECT_EQ(point.port, 1234);
}

TEST(SmtpPoint, ParseProtoHostPort) {
    auto point = SmtpPoint::fromString("Smtp://localhost:465");
    EXPECT_EQ(point.proto, SmtpPoint::smtp);
    EXPECT_EQ(point.host, "localhost");
    EXPECT_EQ(point.port, 465);

    point = SmtpPoint::fromString("smtp://mx.yandex.ru:1234");
    EXPECT_EQ(point.proto, SmtpPoint::smtp);
    EXPECT_EQ(point.host, "mx.yandex.ru");
    EXPECT_EQ(point.port, 1234);

    point = SmtpPoint::fromString("lmtp://[::0]:5678");
    EXPECT_EQ(point.proto, SmtpPoint::lmtp);
    EXPECT_EQ(point.host, "::0");
    EXPECT_EQ(point.port, 5678);

    point = SmtpPoint::fromString("lMtp://[1.2.3.4]:1025");
    EXPECT_EQ(point.proto, SmtpPoint::lmtp);
    EXPECT_EQ(point.host, "1.2.3.4");
    EXPECT_EQ(point.port, 1025);
}

TEST(SmtpPoint, ParseInvalidProto) {
    EXPECT_THROW(SmtpPoint::fromString("NoSuchProto://localhost:465"), std::logic_error);
}

TEST(SmtpPoint, ParseInvalidHost) {
    EXPECT_THROW(SmtpPoint::fromString("smtp:/\\localhost"), std::logic_error);
    EXPECT_THROW(SmtpPoint::fromString("lmtp://[localhost:465"), std::logic_error);
    EXPECT_THROW(SmtpPoint::fromString("Lmtp://localhost]"), std::logic_error);
    EXPECT_THROW(SmtpPoint::fromString("127.]]0.[0.1]:15"), std::logic_error);
}

TEST(SmtpPoint, ParseInvalidPort) {
    EXPECT_THROW(SmtpPoint::fromString("lmtp://localhost:str123"), std::logic_error);
}
