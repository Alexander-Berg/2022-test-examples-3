#include <market/library/caps_lock/tag_token.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>


using namespace NMarket::NCapsLock;


class TTagTokenTest : public TTestBase {
    UNIT_TEST_SUITE(TTagTokenTest);
        UNIT_TEST(TestDefaultConstructor);
        UNIT_TEST(TestSetTokenString);
        UNIT_TEST(TestConcatenation);
        UNIT_TEST(TestCyrillicAlphaInCaps);
    UNIT_TEST_SUITE_END();

public:
    void TestDefaultConstructor() {
        // Проверяем дефолный конструктор.
        // Ожидаем: флаги в false, строковое представление пустое, саб-токенов нет.
        TTagToken token;

        ASSERT_FALSE(token.GetToLowercase());
        ASSERT_FALSE(token.GetToCapitalize());
        ASSERT_EQ(TUtf16String(), token.GetToken());
        ASSERT_EQ(0, token.GetSubTokens().size());
    }

    void TestSetTokenString() {
        // Проверяем конструктор из строки <=> SetTokenString().
        // Ожидаем: флаги в false, строковое представление = использованная строка,
        //          саб-токен единственный, равный строке.
        TUtf16String testStr = u"Hello there";
        TTagToken token(testStr);

        ASSERT_FALSE(token.GetToLowercase());
        ASSERT_FALSE(token.GetToCapitalize());

        ASSERT_EQ(testStr, token.GetToken());

        ASSERT_EQ(1, token.GetSubTokens().size());
        ASSERT_EQ(0, token.GetSubTokens()[0].Pos);
        ASSERT_EQ(11, token.GetSubTokens()[0].Len);
    }

    void TestConcatenation() {
        // Проверяем конкатенацию двух токенов.
        // Ожидаем: флаги не меняются, строковое представление = конкатенация операндов,
        //          к саб-токенам добавляются саб-токены операнда.
        TUtf16String str1 = u"First";
        TUtf16String str2 = u"Second";

        TTagToken token1(str1), token2(str2);
        token2.SetToLowercase(true);

        token1 += token2;

        // Check that += does not affect flags.
        ASSERT_FALSE(token1.GetToLowercase());
        ASSERT_FALSE(token1.GetToCapitalize());
        // Check result string.
        ASSERT_EQ(str1 + str2, token1.GetToken());
        // Check subtoken structure.
        ASSERT_EQ(2, token1.GetSubTokens().size());
        ASSERT_EQ(str1, token1.GetSubTokenString(0));
        ASSERT_EQ(str2, token1.GetSubTokenString(1));
    }

    void TestCyrillicAlphaInCaps() {
        // Проверяем IsCyrillicAlphaInCaps().
        // Ожидаемая логика: true, если:
        //   1) Нет небуквенных символов, кроме '-'
        //   2) Есть как минимум 4 кириллистических символа подряд в капсе.
        auto TestCase = [](const TString& str, bool expected) {
            TTagToken token(UTF8ToWide(str));
            ASSERT_EQ(expected, token.IsCyrillicAlphaInCaps());
        };

        TestCase("ПРИВЕТ", true);
        TestCase("АБВГде", true);
        TestCase("деЁЖЗИ", true);
        TestCase("ДЕёЖЗИ", false);   // Не хватает капсов
        TestCase("WXYZабвг", false); // Не хватает КИРИЛЛИСТИЧЕСКИХ капсов
        TestCase("wxyzАБВГ", true);
        TestCase("АБВГ-ДЕ", true);
        TestCase("АБВГ.ДЕ", false);  // Есть неудобоваримый символ
    }
};


UNIT_TEST_SUITE_REGISTRATION(TTagTokenTest);
