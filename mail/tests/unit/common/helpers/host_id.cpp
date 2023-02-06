#include <common/helpers/helpers.h>

#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <boost/regex.hpp>
#include <regex>

using yimap::HostId;

TEST(HELPERS, ParseHostId)
{
    EXPECT_EQ("12j", HostId::parse("imap12j.mail.yandex.net").suffix);
    EXPECT_EQ("1h", HostId::parse("imapcorp1h.mail.yandex.net").suffix);
    EXPECT_EQ("qa", HostId::parse("imap-qa.mail.yandex.net").suffix);
    EXPECT_EQ("qa", HostId::parse("imapcorp-qa.mail.yandex.net").suffix);
    EXPECT_EQ("rhel", HostId::parse("imap-rhel.mail.yandex.net").suffix);

    EXPECT_EQ("61-qa", HostId::parse("rpop61-qa.mail.yandex.net").suffix);

    EXPECT_EQ("", HostId::parse("imap.yandex.ru").suffix);
    EXPECT_EQ("imap.yandex.ru", HostId::parse("imap.yandex.ru").hostName);

    EXPECT_EQ("", HostId::parse("text.example.com").suffix);
    EXPECT_EQ("text.example.com", HostId::parse("text.example.com").hostName);
}

TEST(HELPERS, ErrorSuffix)
{
    boost::regex checkErrorSuffix("\\ssc=NrJ200022Sws_");
    auto errorSuffix = yimap::createErrorSuffix("NrJ200022Sws");
    EXPECT_TRUE(boost::regex_search(errorSuffix, checkErrorSuffix));
}
