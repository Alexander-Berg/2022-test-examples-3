#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <butil/email/yandex_login.h>

namespace {
using namespace testing;

typedef Test YandexLoginTest;

TEST_F(YandexLoginTest, isValidYandexLogin_FirstPlus_ReturnsTrue)
{
    ASSERT_TRUE(isValidYandexLogin("+super"));
}

TEST_F(YandexLoginTest, isValidYandexLogin_SecondPlus_ReturnsTrue)
{
    ASSERT_TRUE(isValidYandexLogin("s+uper"));
}

TEST_F(YandexLoginTest, isValidYandexLogin_HaveDot_ReturnsTrue)
{
    ASSERT_TRUE(isValidYandexLogin("s.uper"));
}

TEST_F(YandexLoginTest, isValidYandexLogin_HaveMinus_ReturnsTrue)
{
    ASSERT_TRUE(isValidYandexLogin("s-uper"));
}

TEST_F(YandexLoginTest, isValidYandexLogin_HaveDigit_ReturnsTrue)
{
    ASSERT_TRUE(isValidYandexLogin("s8uper"));
}

TEST_F(YandexLoginTest, isValidYandexLogin_HaveDollar_ReturnsFalse)
{
    ASSERT_FALSE(isValidYandexLogin("s$uper"));
}

}
