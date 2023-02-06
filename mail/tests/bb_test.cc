#include <gtest/gtest.h>
#include <gmock/gmock.h>

#include <mail/sendbernar/core/include/account.h>
#include <mail/sendbernar/services/include/blackbox.h>
#include <mail/sendbernar/client/include/category.h>
#include <library/cpp/testing/unittest/env.h>
#include <iostream>
#include <fstream>

const std::string root = std::string(ArcadiaSourceRoot().c_str()) + "/mail/sendbernar/services/tests/bb_json/";

using namespace testing;

namespace sendbernar {

std::string json(const std::string& name) {
    std::ifstream in(root + name+".json");

    return std::string(std::istream_iterator<char>(in),
                       std::istream_iterator<char>());
}

TEST(BlackboxTest, shouldParseBbResponseForUsualAccount) {
    auto parsed = BlackBox::parse(json("usual"));

    EXPECT_EQ(std::get_if<std::string>(&parsed), nullptr);

    Account a = std::get<Account>(parsed);

    EXPECT_EQ(a.uid, "479158379");
    EXPECT_EQ(a.language, "ru");
    EXPECT_EQ(a.login, "yndxsavetemplate");
    EXPECT_EQ(a.domain, "");
    EXPECT_EQ(a.country, "ru");
    EXPECT_EQ(a.karmaStatus, "0");
    EXPECT_EQ(a.karmaValue, "0");
    EXPECT_EQ(a.timezone, "Europe/Moscow");
    EXPECT_EQ(a.defaultAddress, "yndxsavetemplate@yandex.ru");
    EXPECT_THAT(a.addresses, UnorderedElementsAre("yndxsavetemplate@ya.ru", "yndxsavetemplate@yandex.by",
                                                  "yndxsavetemplate@yandex.com", "yndxsavetemplate@yandex.kz",
                                                  "yndxsavetemplate@yandex.ru", "yndxsavetemplate@yandex.ua"));
}


TEST(BlackboxTest, shouldParseBbResponseForPddAccountWithNonemptyDomain) {
    auto parsed = BlackBox::parse(json("pdd"));

    EXPECT_EQ(std::get_if<std::string>(&parsed), nullptr);

    Account a = std::get<Account>(parsed);

    EXPECT_EQ(a.uid, "1130000002644591");
    EXPECT_EQ(a.language, "ru");
    EXPECT_EQ(a.login, "lanwen");
    EXPECT_EQ(a.domain, "kida-lo-vo.name");
    EXPECT_EQ(a.country, "ru");
    EXPECT_EQ(a.karmaStatus, "0");
    EXPECT_EQ(a.karmaValue, "0");
    EXPECT_EQ(a.timezone, "Europe/Moscow");
    EXPECT_EQ(a.defaultAddress, "lanwen@kida-lo-vo.name");
    EXPECT_THAT(a.addresses, UnorderedElementsAre("lanwen@kida-lo-vo.name", "lanwen2@kida-lo-vo.name",
                                                  "lanwen@turk.havroshik.ru", "lanwen2@turk.havroshik.ru"));
}

TEST(BlackboxTest, shouldDecodePunycodeInAddressList) {
    auto parsed = BlackBox::parse(json("with_punycode"));

    EXPECT_EQ(std::get_if<std::string>(&parsed), nullptr);

    Account a = std::get<Account>(parsed);

    EXPECT_EQ(a.login, "yndxsavetemplate");
    EXPECT_EQ(a.domain, "");
    EXPECT_EQ(a.defaultAddress, "default@админкапдд.рф");
    EXPECT_THAT(a.addresses, UnorderedElementsAre("default@админкапдд.рф"));
    EXPECT_THAT(a.validated, UnorderedElementsAre("validated@админкапдд.рф"));
}

TEST(BlackboxTest, shouldParseBbResponseForAccountWithoutMail) {
    EXPECT_NO_THROW(std::get<std::string>(BlackBox::parse(json("without_mail"))));
}

TEST(BlackboxTest, shouldParseBbResponseForNonexistingAccount) {
    EXPECT_NO_THROW(std::get<std::string>(BlackBox::parse(json("nonexisting"))));
}

TEST(BlackboxTest, shouldNotParseInvalidBbResponse) {
    EXPECT_NO_THROW(std::get<std::string>(BlackBox::parse(json("invalid_param"))));
}

TEST(AddressParsingTest, shouldExtractNativaAndValidatedAddresses) {
    auto parsed = BlackBox::parse(json("with_different_address_types"));

    EXPECT_EQ(std::get_if<std::string>(&parsed), nullptr);

    Account a = std::get<Account>(parsed);

    EXPECT_THAT(a.addresses, UnorderedElementsAre("default@ya.ru", "native@ya.ru"));
    EXPECT_THAT(a.validated, UnorderedElementsAre("validated@gmail.com"));
}

TEST(AccountAddressTest, shouldFindEmailInAddressesAndValidatedAsValidated) {
    Account acc;
    acc.addresses = {"ololo-1@ya.ru"};
    acc.validated = {"azaza-1@gmail.com"};

    EXPECT_TRUE(acc.validateAddress("ololo-1@ya.ru"));
    EXPECT_TRUE(acc.validateAddress("ololo.1@ya.ru"));
    EXPECT_TRUE(acc.validateAddress("azaza-1@gmail.com"));
    EXPECT_FALSE(acc.validateAddress("azaza.1@gmail.com"));
    EXPECT_FALSE(acc.validateAddress("uzuzu.1@gmail.com"));
}

TEST(AccountAddressTest, shouldFindEmailInAddressesAndValidated) {
    Account acc;
    acc.addresses = {"ololo-1@ya.ru"};
    acc.validated = {"azaza-1@gmail.com"};

    EXPECT_TRUE(acc.hasAddress("ololo-1@ya.ru"));
    EXPECT_TRUE(acc.hasAddress("ololo.1@ya.ru"));
    EXPECT_FALSE(acc.validateAddress("azaza-1@ygmail.com"));
    EXPECT_FALSE(acc.validateAddress("uzuzu.1@gmail.com"));
}

TEST(AccountAddressTest, shouldSetDefaultFromNameFromBlackbox) {
    auto parsed = BlackBox::parse(json("default_from_name"));

    EXPECT_EQ(std::get_if<std::string>(&parsed), nullptr);

    Account a = std::get<Account>(parsed);

    EXPECT_EQ(a.fromName, "Sid Vicious");
}

}
