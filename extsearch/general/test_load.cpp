#include <extsearch/geo/kernel/resource/load.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NGeosearch;

Y_UNIT_TEST_SUITE(TTestLoad) {
    Y_UNIT_TEST(TestLoadTsv) {
        TVector<TVector<TString>> loadedTsv = LoadTsvResource("tsv1");
        UNIT_ASSERT_EQUAL(loadedTsv.size(), 4);
        UNIT_ASSERT_EQUAL(loadedTsv[0], TVector<TString>({"1", "2", "3"}));
        UNIT_ASSERT_EQUAL(loadedTsv[1], TVector<TString>({"4", "", "", ""}));
        UNIT_ASSERT_EQUAL(loadedTsv[2], TVector<TString>({"5"}));
        UNIT_ASSERT_EQUAL(loadedTsv[3], TVector<TString>({"6", "7"}));
    }
}
