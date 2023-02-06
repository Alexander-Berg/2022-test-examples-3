#include <crypta/lib/native/lemmer/get_lemmas.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/string.h>

using namespace NCrypta;

Y_UNIT_TEST_SUITE(GetLemmas) {
    Y_UNIT_TEST(Basic) {
        TLangMask langs = TLangMask(LANG_RUS, LANG_ENG);

        THashSet<TUtf16String> result = GetLemmas(u"мама мыла раму мылом mazda cx 5", langs);

        THashSet<TUtf16String> reference({
            u"мама",
            u"мыло",
            u"мыть",
            u"рама",
            u"mazda",
            u"cx",
            u"5"
        });

        UNIT_ASSERT_EQUAL(reference, result);
    }
}

Y_UNIT_TEST_SUITE(GetNotUniqueLemmas) {
Y_UNIT_TEST(Basic) {
        TLangMask langs = TLangMask(LANG_RUS, LANG_ENG);

        TVector<TUtf16String> result = GetNotUniqueLemmas(u"мама мыла раму мама mazda cx 5", langs);

        TVector<TUtf16String> reference({
            u"мама",
            u"мыло",
            u"мыть",
            u"рама",
            u"мама",
            u"mazda",
            u"cx",
            u"5"
        });

        UNIT_ASSERT_EQUAL(reference, result);
    }
}
