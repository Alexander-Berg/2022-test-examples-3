#include <crypta/lib/native/crypta_id/crypta_id_generator.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(NCryptaIdGenerator) {
    using namespace NCrypta;

    Y_UNIT_TEST(Generate) {
        UNIT_ASSERT_EQUAL(5524518316514484593ULL, NCryptaIdGenerator::Generate("yandexuid", "1743214861544862539"));
        UNIT_ASSERT_EQUAL(13198405724937708644ULL, NCryptaIdGenerator::Generate("yandexuid", "148814881581488666"));
        UNIT_ASSERT_EQUAL(14847286688105759375ULL, NCryptaIdGenerator::Generate("email", "qwerty@yandex.ru"));
        UNIT_ASSERT_EQUAL(10180751488204869158ULL, NCryptaIdGenerator::Generate("gaid", "17536264-D38B-4F27-9AC2-BE63312EDFA6")); // No normalization
    }

    Y_UNIT_TEST(GenerateSingle) {
        TVector<std::pair<TString, TString>> ids = {
                {"yandexuid", "1743214861544862539"},
                {"yandexuid", "148814881581488666"},
                {"email", "qwerty@yandex.ru"},
                {"gaid", "17536264-D38B-4F27-9AC2-BE63312EDFA6"}
        };

        UNIT_ASSERT_EQUAL(5524518316514484593ULL, NCryptaIdGenerator::GenerateSingle(ids, [](const auto& kvp) { return kvp; }));
    }
}
