#include <library/cpp/testing/unittest/gtest.h>
#include <market/idx/promos/pricedrops/tools/tools.h>

TEST(Tools, GetClickTables) {
    TVector<TString> expected{"/tmp/testdata/14-05-2019", "/tmp/testdata/13-05-2019"};
    auto given = NMarket::NPriceDrops::GetTablesPath(TInstant::Seconds(1557906147 /*15.05.2019*/), 2, 1, "/tmp/testdata", "%d-%m-%Y");
    ASSERT_EQ(expected.size(), given.size());
    for(size_t i = 0; i < expected.size(); ++i) {
        ASSERT_STREQ(expected[i], given[i]);
    }
}

