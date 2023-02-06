#include <backend/backend_types.h>

#include <yplatform.hpp>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

TEST(TEST_APPEND, READ_APPEND_OPTIONS)
{

    const size_t SMTP_GATE_PERCENT = 0;

    const std::string SMTP_HOST = "mxbacktst1o.cmail.yandex.net";
    const uint32_t SMTP_PORT = 1234;
    const uint32_t SMTP_TIMEOUT = 10;
    const size_t SMTP_RETRY_COUNT = 2;
    const bool SMTP_IPV6 = false;

    const std::string HTTP_HOST = "https://mxbacktst1o.cmail.yandex.net";
    const uint32_t HTTP_PORT = 2443;
    const size_t HTTP_RETRY_COUNT = 3;
    const bool HTTP_RETRY_SMTP = true;

    yplatform::ptree tree;

    tree.put("append.smtpgate_percent", std::to_string(SMTP_GATE_PERCENT));

    tree.put("append.smtp.host", SMTP_HOST);
    tree.put("append.smtp.port", std::to_string(SMTP_PORT));
    tree.put("append.smtp.timeout", std::to_string(SMTP_TIMEOUT));
    tree.put("append.smtp.retry_count", std::to_string(SMTP_RETRY_COUNT));
    tree.put("append.smtp.ipv6", (SMTP_IPV6 == false ? "0" : "1"));

    tree.put("append.http.host", HTTP_HOST);
    tree.put("append.http.port", std::to_string(HTTP_PORT));
    tree.put("append.http.retry_count", std::to_string(HTTP_RETRY_COUNT));
    tree.put("append.http.retry_smtp", (HTTP_RETRY_SMTP == false ? "0" : "1"));

    auto appendOpt = yimap::AppendSettings::create(tree.get_child("append"));

    EXPECT_EQ(appendOpt.smtpgatePercent, SMTP_GATE_PERCENT);
    EXPECT_EQ(appendOpt.smtpHost, SMTP_HOST);
    EXPECT_EQ(appendOpt.smtpPort, SMTP_PORT);
    EXPECT_EQ(appendOpt.smtpTimeout, SMTP_TIMEOUT);
    EXPECT_EQ(appendOpt.smtpRetryCount, SMTP_RETRY_COUNT);
    EXPECT_EQ(appendOpt.smtpIpv6, SMTP_IPV6);
    EXPECT_EQ(appendOpt.httpHost, HTTP_HOST);
    EXPECT_EQ(appendOpt.httpPort, HTTP_PORT);
    EXPECT_EQ(appendOpt.httpRetryCount, HTTP_RETRY_COUNT);
    EXPECT_EQ(appendOpt.httpRetrySmtp, HTTP_RETRY_SMTP);
}

TEST(TEST_APPEND, SHOULD_USE_SMTP_GATE)
{
    yimap::AppendSettings settings;

    settings.httpHost = "https://mxbacktst1o.cmail.yandex.net";
    settings.smtpgatePercent = 49u;
    EXPECT_FALSE(settings.shouldUseSmtpgate("hce2000OOa6a"));
    EXPECT_FALSE(settings.shouldUseSmtpgate("hce2000OOa6s"));
    EXPECT_TRUE(settings.shouldUseSmtpgate("hce2000OOa6t"));

    settings.smtpgatePercent = 0u;
    EXPECT_FALSE(settings.shouldUseSmtpgate("hce2000OOa6g"));

    settings.smtpgatePercent = 100u;
    EXPECT_TRUE(settings.shouldUseSmtpgate("hce2000OOa6s"));

    settings.httpHost = "";
    EXPECT_FALSE(settings.shouldUseSmtpgate("hce2000OOa6a"));
}
