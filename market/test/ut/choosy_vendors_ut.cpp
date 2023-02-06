#include <market/library/vendor_recommended_business/vendor_recommended_business.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <util/folder/tempdir.h>
#include <util/stream/file.h>


Y_UNIT_TEST_SUITE(TestChoosyVendors) {
    Y_UNIT_TEST(TestFile) {
        TTempDir tmp;
        const TString vcsFileName = "empty_vendor_recommended_business.csv";
        TFileOutput(vcsFileName).Finish();
        const auto path = JoinFsPaths(ArcadiaSourceRoot(), "market/svn-data/package-data/choosy_vendors.txt");
        NMarket::TVendorRecommendedBusinessCalculator calculator;
        calculator.Load(vcsFileName, path, /* validate */ true);
        UNIT_ASSERT(calculator.GetErrors().empty());
    }
}
