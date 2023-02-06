#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <market/library/used_goods/src/svn_data.h>

Y_UNIT_TEST_SUITE(TestUsedGoods) {
    Y_UNIT_TEST(TestAllFiles) {
        const TVector<TString> files = {"used_goods_sku.txt", "used_goods_sku_testing.txt"};
        for (const auto& file: files) {
            const auto path = JoinFsPaths(ArcadiaSourceRoot(), "market/svn-data/package-data", file);
            NMarket::NUsedGoods::TSvnDataReader reader(path, /* validate */ true);
            if (!reader.GetBadLines().empty()) {
                Cerr << "File " << file << ", bad line: " << reader.GetBadLines()[0] << Endl;
                UNIT_ASSERT(false);
            }
        }
    }
}
