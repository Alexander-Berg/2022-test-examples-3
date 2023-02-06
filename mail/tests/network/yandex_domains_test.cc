
#include <stdexcept>
#include <gtest/gtest.h>

#include <butil/network/yandex_domains.h>

using namespace std;

TEST(YandexDomainsTest, findReturnsValidIteratorForDomainInList) {
    YandexDomains domains;
    domains.add("yandex.ru", "ru");
    ASSERT_TRUE(domains.find("yandex.ru") != domains.end());
}

TEST(YandexDomainsTest, findReturnsValidIteratorForDomainInListInUpperCase) {
    YandexDomains domains;
    domains.add("yandex.ru", "ru");
    ASSERT_TRUE(domains.find("YANDEX.RU") != domains.end());
}

TEST(YandexDomainsTest, findEndsReturnsValidIteratorForThirdLevelDomain) {
      YandexDomains domains;
      domains.add("yandex.ru", "ru");
      ASSERT_TRUE(domains.findEnds("mail.yandex.ru") != domains.end());
  }

TEST(YandexDomainsTest, findReturnsValidIteratorForCyrillicDomainInIdn) {
    YandexDomains domains;
    domains.add("яндекс.рф", "рф");
    ASSERT_TRUE(domains.find("xn--d1acpjx3f.xn--p1ai") != domains.end());
}

TEST(YandexDomainsTest, findReturnsIteratorToEndForFourthLevelDomainNotInList) {
    YandexDomains domains;
    domains.add("yandex.ru", "ru");
    ASSERT_TRUE(domains.find("mail.yandex.ru.com") == domains.end());
}

TEST(YandexDomainsTest, findReturnsIteratorToEndForDomainNotInList) {
    YandexDomains domains;
    domains.add("yandex.ru", "ru");
    domains.add("ya.ru", "ru");
    ASSERT_TRUE(domains.find("mail.ru") == domains.end());
    ASSERT_TRUE(domains.find("passport.yandex.ru") == domains.end());
    ASSERT_TRUE(domains.find("demidovskaya.ru") == domains.end());
}
