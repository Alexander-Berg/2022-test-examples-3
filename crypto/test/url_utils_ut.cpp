#include <crypta/lib/native/url/url_utils.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(NUrlUtils) {
    using namespace NCrypta;

    Y_UNIT_TEST(TTestCaseMakeUrl) {
        UNIT_ASSERT_STRINGS_EQUAL("http://yandex.ru/search?query=text", NUrlUtils::MakeUrl("http", "yandex.ru", 80, "/search", {{"query", "text"}}));
        UNIT_ASSERT_STRINGS_EQUAL("https://yandex.ru:8080/search?query=text", NUrlUtils::MakeUrl("https", "yandex.ru", 8080, "/search", {{"query", "text"}}));
        UNIT_ASSERT_STRINGS_EQUAL("https://yandex.ru:8080/search?query=text1&query=text2", NUrlUtils::MakeUrl("https", "yandex.ru", 8080, "/search", {{"query", "text1"},{"query", "text2"}}));
        UNIT_ASSERT_STRINGS_EQUAL("http://yandex.ru:8080/search?extras=!@%23$%25^%26*%28%29&query=foo+bar", NUrlUtils::MakeUrl("http", "yandex.ru", 8080, "/search", {{"query", "foo bar"}, {"extras", "!@#$%^&*()"}}));
    }

    void Test(const TMaybe<TString>& reference, const TMaybe<TString>& result) {
        UNIT_ASSERT_EQUAL(reference.Defined(), result.Defined());
        if (reference.Defined()) {
            UNIT_ASSERT_STRINGS_EQUAL(*reference, *result);
        }
    }

    Y_UNIT_TEST(ExtractHost) {
        Test("ru.wikipedia.org", NUrlUtils::ExtractHost("http://ru.wikipedia.org/wiki/Hello how are you"));
        Test("www.ru", NUrlUtils::ExtractHost("http://www.ru/Теория_вероятностей"));
        Test("xn--d1abbgf6aiiy.xn--p1ai", NUrlUtils::ExtractHost("http://президент.рф/junk?junk"));
        Test("ya.ru", NUrlUtils::ExtractHost("www.ya.ru"));
        Test("ya.ru", NUrlUtils::ExtractHost("WwW.YA.Ru"));
        Test("ya.ru", NUrlUtils::ExtractHost("//ya.ru"));
        Test("ya.ru", NUrlUtils::ExtractHost("http://ww17.ya.ru"));
        Test("www.ru", NUrlUtils::ExtractHost("www.ru"));
        Test("www2.com", NUrlUtils::ExtractHost("www2.com"));

        Test(Nothing(), NUrlUtils::ExtractHost("http://,"));
        Test(Nothing(), NUrlUtils::ExtractHost("abracadabra."));
        Test(Nothing(), NUrlUtils::ExtractHost("abracadabra"));
        Test("abracadabra.ru", NUrlUtils::ExtractHost("abracadabra.ru"));
    }
}
