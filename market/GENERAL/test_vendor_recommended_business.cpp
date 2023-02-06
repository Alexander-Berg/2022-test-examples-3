#include <market/library/vendor_recommended_business/vendor_recommended_business.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <util/folder/tempdir.h>
#include <util/stream/file.h>


Y_UNIT_TEST_SUITE(TestVRB) {
    Y_UNIT_TEST(TestChoosyVendors) {
        TTempDir tmp;
        const TString vcsFileName = "empty_vendor_recommended_business.csv";
        TFileOutput(vcsFileName).Finish();
        const TString choosyVendorsFileName = "choosy_vendors.txt";
        TFileOutput out(choosyVendorsFileName);
        out <<
"100\n"
"200\tbad delimiter symbol\n"
"abc bad vendor id\n"
"\n"
"300 vendor name\n";
        out.Finish();

        NMarket::TVendorRecommendedBusinessCalculator calculator;
        calculator.Load(vcsFileName, choosyVendorsFileName, /* validate */ true);
        const auto& errors = calculator.GetErrors();
        UNIT_ASSERT_EQUAL(errors.size(), 3);
        UNIT_ASSERT_EQUAL(errors[0], "Error in choosy_vendors.txt, line: 2");
        UNIT_ASSERT_EQUAL(errors[1], "Error in choosy_vendors.txt, line: 3");
        UNIT_ASSERT_EQUAL(errors[2], "Error in choosy_vendors.txt, line: 4");
        UNIT_ASSERT(calculator.IsChoosyVendor(100));
        UNIT_ASSERT(!calculator.IsChoosyVendor(200));
        UNIT_ASSERT(calculator.IsChoosyVendor(300));
        UNIT_ASSERT(!calculator.IsChoosyVendor(400));
    }

    Y_UNIT_TEST(TestRealChoosyVendorsFile) {
        TTempDir tmp;
        const TString vcsFileName = "empty_vendor_recommended_business2.csv";
        TFileOutput(vcsFileName).Finish();
        const auto svnDataPath = JoinFsPaths(ArcadiaSourceRoot(), "market/svn-data/package-data/choosy_vendors.txt");
        NMarket::TVendorRecommendedBusinessCalculator calculator;
        calculator.Load(vcsFileName, svnDataPath, /* validate */ true);
        UNIT_ASSERT(calculator.GetErrors().empty());
    }
}
