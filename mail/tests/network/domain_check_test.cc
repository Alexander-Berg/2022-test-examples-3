#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <butil/network/domain_check.h>

namespace {
using namespace testing;

typedef Test DomainTest;

TEST_F(DomainTest, is_yandex_host_YandexAddress_ReturnsTrue)
{
    ASSERT_TRUE(is_yandex_host("yandex.ru"));
    ASSERT_TRUE(is_yandex_host("yandex.com.tr"));
    ASSERT_TRUE(is_yandex_host("яндекс.рф"));
    ASSERT_TRUE(is_yandex_host("ya.ru"));
    ASSERT_TRUE(is_yandex_host("narod.ru"));
    ASSERT_TRUE(is_yandex_host("yandex.ua"));
    ASSERT_TRUE(is_yandex_host("yandex.com"));
    ASSERT_TRUE(is_yandex_host("yandex.by"));
    ASSERT_TRUE(is_yandex_host("yandex.kz"));
    ASSERT_TRUE(is_yandex_host("yandex.net"));
    ASSERT_TRUE(is_yandex_host("xn--d1acpjx3f.xn--p1ai"));
    ASSERT_TRUE(is_yandex_host(idna::encode("xn--d1acpjx3f.xn--p1ai")));
    ASSERT_TRUE(is_yandex_host("yandex.az"));
    ASSERT_TRUE(is_yandex_host("yandex.com.am"));
    ASSERT_TRUE(is_yandex_host("yandex.com.ge"));
    ASSERT_TRUE(is_yandex_host("yandex.co.il"));
    ASSERT_TRUE(is_yandex_host("yandex.kg"));
    ASSERT_TRUE(is_yandex_host("yandex.lt"));
    ASSERT_TRUE(is_yandex_host("yandex.lv"));
    ASSERT_TRUE(is_yandex_host("yandex.md"));
    ASSERT_TRUE(is_yandex_host("yandex.tj"));
    ASSERT_TRUE(is_yandex_host("yandex.tm"));
    ASSERT_TRUE(is_yandex_host("yandex.uz"));
    ASSERT_TRUE(is_yandex_host("yandex.fr"));
    ASSERT_TRUE(is_yandex_host("yandex.ee"));
}

TEST_F(DomainTest, is_yandex_host_NoYandexAddress_ReturnsFalse)
{
    ASSERT_FALSE(is_yandex_host("yandexx.ru"));
    ASSERT_FALSE(is_yandex_host("mail.ru"));
    ASSERT_FALSE(is_yandex_host("gmail.com"));
    ASSERT_FALSE(is_yandex_host("yandex.gov"));
}

TEST_F(DomainTest, is_yandex_team_host_YandexTeamAddress_ReturnsTrue)
{
    ASSERT_TRUE(is_yandex_team_host("yandex-team.ru"));
    ASSERT_TRUE(is_yandex_team_host("yandex-team.com.tr"));
    ASSERT_TRUE(is_yandex_team_host("k50.ru"));
    ASSERT_TRUE(is_yandex_team_host("openyard.ru"));
}

TEST_F(DomainTest, is_yandex_team_host_NonYandexTeamAddress_ReturnsFalse)
{
    ASSERT_FALSE(is_yandex_team_host("yx-team.ru"));
    ASSERT_FALSE(is_yandex_team_host("super-team.com.tr"));
}


} // namespace
