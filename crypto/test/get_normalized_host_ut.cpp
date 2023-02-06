#include <crypta/graph/rtmr/lib/common/get_normalized_host.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(GetNormalizedHost) {
    using namespace NCrypta::NGraph;

    void Test(const TMaybe<TString>& reference, const TMaybe<TString>& result) {
        UNIT_ASSERT_EQUAL(reference.Defined(), result.Defined());
        if (reference.Defined()) {
            UNIT_ASSERT_STRINGS_EQUAL(*reference, *result);
        }
    }

    Y_UNIT_TEST(GetNormalizedHost) {
        Test("ru.wikipedia.org", GetNormalizedHost("http://ru.wikipedia.org/wiki/Hello how are you"));
        Test("www.ru", GetNormalizedHost("http://www.ru/Теория_вероятностей"));
        Test("xn--d1abbgf6aiiy.xn--p1ai", GetNormalizedHost("http://президент.рф/junk?junk"));
        Test("ww17.ya.ru", GetNormalizedHost("http://ww17.ya.ru"));

        Test(Nothing(), GetNormalizedHost("http://,"));
        Test(Nothing(), GetNormalizedHost("abracadabra.ru"));
        Test(Nothing(), GetNormalizedHost("goal://abracadabra.ru"));
        Test(Nothing(), GetNormalizedHost("www.ya.ru"));
        Test(Nothing(), GetNormalizedHost("WwW.YA.Ru"));
        Test(Nothing(), GetNormalizedHost("//ya.ru"));
    }

    Y_UNIT_TEST(GetParam) {
        Test(Nothing(), GetParam("http://ya.ru?param=value", "yclid"));
        Test("value", GetParam("http://ya.ru?param=value", "param"));
        Test(Nothing(), GetParam("///httddd:/", "yclid"));
        Test("100", GetParam("https://site.com?param=value&yclid=100&yclid=200", "yclid"));
    }
}
