#include "class_parser.h"

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TParserTestCombo){
    Y_UNIT_TEST(NewSubTest) {
        const TStringBuf entry("{\n\"address\": \"orders@combo.mail.ru\",\n\"title\": \"Дмитрий, добро пожаловать в Combo!!\",\n\"text\": \"вы оформили подписку Combo на\"\n}");
        TParser parser;
        UNIT_ASSERT_VALUES_EQUAL(parser.apply(entry), "{\"name\" : \"combo.mail.ru\",\"type\" : \"new_sub\"}");
    }
    Y_UNIT_TEST(ResubTest) {
        const TStringBuf entry("{\n\"address\": \"orders@combo.mail.ru\",\n\"title\": \"Александр, благодарим за продление подписки!\",\n\"text\": \"Подписка Combo продлена на 30 дней\"\n}");
        TParser parser;
        UNIT_ASSERT_VALUES_EQUAL(parser.apply(entry), "{\"name\" : \"combo.mail.ru\",\"type\" : \"resub\"}");
    }
    Y_UNIT_TEST(FailureTest) {
        const TStringBuf entry("{\n\"address\": \"orders@combo.mail.ru\",\n\"title\": \"Нам не удалось продлить вашу подписку\",\n\"text\": \"Спасибо за продление подписки\"\n}");
        TParser parser;
        UNIT_ASSERT_VALUES_EQUAL(parser.apply(entry), "{\"name\" : \"combo.mail.ru\",\"type\" : \"failure\"}");
    }
    Y_UNIT_TEST(CheckTest) {
        const TStringBuf entry("{\n\"address\": \"fiscal@corp.mail.ru\",\n\"title\": \"КАССОВЫЙ ЧЕК/ПРИХОД\",\n\"text\": \"combo.mail.ru\"\n}");
        TParser parser;
        UNIT_ASSERT_VALUES_EQUAL(parser.apply(entry), "{\"name\" : \"combo.mail.ru\",\"type\" : \"check\"}");
    }
    Y_UNIT_TEST(ReturnTest) {
        const TStringBuf entry("{\n\"address\": \"fiscal@corp.mail.ru\",\n\"title\": \"КАССОВЫЙ ЧЕК/ВОЗВРАТ ПРИХОД\",\n\"text\": \"combo.mail.ru\"\n}");
        TParser parser;
        UNIT_ASSERT_VALUES_EQUAL(parser.apply(entry), "{\"name\" : \"combo.mail.ru\",\"type\" : \"return\"}");
    }
};

Y_UNIT_TEST_SUITE(TParserTestMegogo){
    Y_UNIT_TEST(TrialTest1) {
        const TStringBuf entry("{\n\"address\": \"team@email.megogo.ru\",\n\"title\": \"Покупка подписки «Максимальная»\",\n\"text\": \"цена 1 руб.\"\n}");
        TParser parser;
        UNIT_ASSERT_VALUES_EQUAL(parser.apply(entry), "{\"name\" : \"megogo\",\"type\" : \"trial, 1 month\"}");
    }
    Y_UNIT_TEST(TrialTest2) {
        const TStringBuf entry("{\n\"address\": \"team@email.megogo.ru\",\n\"title\": \"Покупка подписки «Премиальная»\",\n\"text\": \"цена 97 руб.\"\n}");
        TParser parser;
        UNIT_ASSERT_VALUES_EQUAL(parser.apply(entry), "{\"name\" : \"megogo\",\"type\" : \"trial, 7 days\"}");
    }
    Y_UNIT_TEST(CheckTest) {
        const TStringBuf entry("{\n\"address\": \"noreply@1-ofd.ru\",\n\"title\": \"Чек\",\n\"text\": \"абвгдейка\"\n}");
        TParser parser;
        UNIT_ASSERT_VALUES_EQUAL(parser.apply(entry), "{\"name\" : \"megogo\",\"type\" : \"check\"}");
    }
};

Y_UNIT_TEST_SUITE(TParserTestSpotify){
    Y_UNIT_TEST(TrialTest1) {
        const TStringBuf entry("{\n\"address\": \"no-reply@spotify.com\",\n\"title\": \"Квитанция Spotify\",\n\"text\": \"до окончания срока действия пробного периода, мы начнем автоматически списывать ежемесячную плату в размере 169₽\"\n}");
        TParser parser;
        UNIT_ASSERT_VALUES_EQUAL(parser.apply(entry), "{\"name\" : \"spotify\",\"type\" : \"trial, 169 rub\"}");
    }
    Y_UNIT_TEST(TrialTest2) {
        const TStringBuf entry("{\n\"address\": \"no-reply@spotify.com\",\n\"title\": \"Квитанция Spotify\",\n\"text\": \"до окончания срока действия пробного периода, мы начнем автоматически списывать ежемесячную плату в размере 219₽\"\n}");
        TParser parser;
        UNIT_ASSERT_VALUES_EQUAL(parser.apply(entry), "{\"name\" : \"spotify\",\"type\" : \"trial, 219 rub\"}");
    }
    Y_UNIT_TEST(TrialTest3) {
        const TStringBuf entry("{\n\"address\": \"no-reply@spotify.com\",\n\"title\": \"Квитанция Spotify\",\n\"text\": \"до окончания срока действия пробного периода, мы начнем автоматически списывать ежемесячную плату в размере 269₽\"\n}");
        TParser parser;
        UNIT_ASSERT_VALUES_EQUAL(parser.apply(entry), "{\"name\" : \"spotify\",\"type\" : \"trial, 269 rub\"}");
    }
    Y_UNIT_TEST(TrialTest4) {
        const TStringBuf entry("{\n\"address\": \"no-reply@spotify.com\",\n\"title\": \"Квитанция Spotify\",\n\"text\": \"до окончания срока действия пробного периода, мы начнем автоматически списывать ежемесячную плату в размере 85₽\"\n}");
        TParser parser;
        UNIT_ASSERT_VALUES_EQUAL(parser.apply(entry), "{\"name\" : \"spotify\",\"type\" : \"trial, 85 rub\"}");
    }
};

Y_UNIT_TEST_SUITE(TParserTestOkko) {
    Y_UNIT_TEST(RegisterTest){
        const TStringBuf entry("{\n\"address\": \"hi@okko.tv\",\n\"title\": \"Добро пожаловать в Okko!\",\n\"text\": \"что-то\"\n}");
        TParser parser;
        UNIT_ASSERT_VALUES_EQUAL(parser.apply(entry), "{\"name\" : \"okko\",\"type\" : \"register\"}");
    }
};

Y_UNIT_TEST_SUITE(TParserTestWink){
    Y_UNIT_TEST(RegisterTest){
        const TStringBuf entry("{\n\"address\": \"noreply@itv.rt.ru\",\n\"title\": \"Регистрация в Wink\",\n\"text\": \"что-то\"\n}");
        TParser parser;
        UNIT_ASSERT_VALUES_EQUAL(parser.apply(entry), "{\"name\" : \"wink\",\"type\" : \"register\"}");
    }
};

Y_UNIT_TEST_SUITE(TParserTestIVI){
    Y_UNIT_TEST(TrialTest){
        const TStringBuf entry("{\n\"address\": \"info@notify.ivi.ru\",\n\"title\": \"Поздравляем! Вы подключили пробный период подписки ivi\",\n\"text\": \"что-то\"\n}");
        TParser parser;
        UNIT_ASSERT_VALUES_EQUAL(parser.apply(entry), "{\"name\" : \"ivi\",\"type\" : \"trial, 14 days\"}");
    }
    Y_UNIT_TEST(UnsubTest) {
        const TStringBuf entry("{\n\"address\": \"info@notify.ivi.ru\",\n\"title\": \"Автопродление подписки ivi было отклонено\",\n\"text\": \"что-то\"\n}");
        TParser parser;
        UNIT_ASSERT_VALUES_EQUAL(parser.apply(entry), "{\"name\" : \"ivi\",\"type\" : \"unsub\"}");
    }
};

Y_UNIT_TEST_SUITE(TParserTestNetflix){
    Y_UNIT_TEST(TrialTest){
        const TStringBuf entry("{\n\"address\": \"info@mailer.netflix.com\",\n\"title\": \"Reminder: your free month ends on Tuesday, February 18th, 2020.\",\n\"text\": \"что-то\"\n}");
        TParser parser;
        UNIT_ASSERT_VALUES_EQUAL(parser.apply(entry), "{\"name\" : \"netflix\",\"type\" : \"trial, 30 days\"}");
    }
};
