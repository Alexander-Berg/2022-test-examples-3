#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <market/report/library/global/parallel_import_warranty/parallel_import_warranty.h>


TEST(TestParallelImportWarrantyGlobal, LoadJsonFile) {

    const auto path = SRC_("./../../svn-data/package-data/parallel_import_warranty.json");

    EXPECT_NO_THROW(NMarketReport::NGlobal::LoadParallelImportWarranty(path));
}

TEST(TestParallelImportWarranty, LoadJsonFile) {

    NMarket::NParallelImportWarranty::TParallelImportWarranty warranty;

    const auto path = SRC_("./../../svn-data/package-data/parallel_import_warranty.json");

    ASSERT_NO_THROW(warranty.LoadParallelImportWarranty(path));
}
