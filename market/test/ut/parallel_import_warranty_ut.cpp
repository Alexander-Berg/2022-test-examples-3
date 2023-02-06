#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <market/library/parallel_import_warranty/parallel_import_warranty.h>

Y_UNIT_TEST_SUITE(TestParallelImportWarranty) {
    Y_UNIT_TEST(TestFile) {
        const auto path = JoinFsPaths(ArcadiaSourceRoot(), "market/svn-data/package-data/parallel_import_warranty.json");
        NMarket::NParallelImportWarranty::TParallelImportWarranty warranty;
        try {
            warranty.LoadParallelImportWarranty(path);
        } catch (yexception& e) {
            Cerr << "File " << path << ": " << e.what() << Endl;
            UNIT_ASSERT(false);
        }
    }
}
