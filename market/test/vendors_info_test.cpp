#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <market/report/library/test/mock_error_log/mock_error_log.h>
#include <market/report/library/vendors_info/vendors_info.h>
#include <util/stream/output.h>

namespace NMarketReport {

bool operator==(const TVendorLogo& a, const TVendorLogo& b) {
    return a.Type == b.Type && a.Url == b.Url;
}

bool operator==(const TVendorInfo& a, const TVendorInfo& b) {
    return std::tie(a.Name, a.RecommendedShopsUrl, a.Description, a.Logos, a.Website) ==
           std::tie(b.Name, b.RecommendedShopsUrl, b.Description, b.Logos, b.Website);
}

}  // namespace NMarketReport

using namespace NMarketReport;

template <>
inline void Out<TVendorInfo>(
        IOutputStream& out,
        const TVendorInfo& vendorInfo) {
    out << "Name: " << vendorInfo.Name << "\n";
    if (vendorInfo.RecommendedShopsUrl)
        out << "RecommendedShopsUrl: " << *vendorInfo.RecommendedShopsUrl << "\n";
    if (vendorInfo.Description)
        out << "Description: " << *vendorInfo.Description << "\n";
    if (vendorInfo.Website)
        out << "Website: " << *vendorInfo.Website << "\n";
    for (const auto& logo : vendorInfo.Logos) {
        out << "Logo: " << logo.Url << ", Type: "
            << static_cast<int>(logo.Type) << "\n";
    }
}

TEST(VendorsInfo, Parse) {
    TVendorsInfo result;
    MockErrorLog errors;
    ParseVendorsInfo(SRC_("./data/vendors-info.xml"), result);

    TVendorId vendorId = 0;
    TVendorsInfo expected;
    TVendorInfo vendorInfo;
    vendorInfo.Name = "Vendor 1";
    expected[++vendorId] = vendorInfo;
    vendorInfo = TVendorInfo();
    vendorInfo.Name = "Vendor 2";
    vendorInfo.RecommendedShopsUrl = "http://www.vendor2.com/";
    expected[++vendorId] = vendorInfo;
    vendorInfo = TVendorInfo();
    vendorInfo.Name = "Vendor 3";
    vendorInfo.Description = "Vendor 3 description.";
    vendorInfo.Website = "http://samsung.com/";
    expected[++vendorId] = vendorInfo;
    vendorInfo = TVendorInfo();
    vendorInfo.Name = "Vendor 4";
    TVendorLogo logo;
    logo.Type = TVendorLogoType::BRANDZONE;
    logo.Url = "vendor_4_logo_url";
    vendorInfo.Logos.push_back(logo);
    expected[++vendorId] = vendorInfo;
    vendorInfo = TVendorInfo();
    vendorInfo.Name = "Vendor 5";
    expected[++vendorId] = vendorInfo;

    EXPECT_EQ(expected, result);
    EXPECT_TRUE(errors.IsEmpty());
}

TEST(VendorInfo, ParseWrongFormat) {
    TVendorsInfo result;
    MockErrorLog errors;
    ParseVendorsInfo(SRC_("./data/vendors-info-wrong-format.xml"), result);

    EXPECT_TRUE(errors.ExpectErrorLike("vendors-info-wrong-format.xml(.*)Cannot find attribute with name id"));
    EXPECT_TRUE(errors.ExpectErrorLike("vendors-info-wrong-format.xml(.*)Unexpected symbol \"G\" at pos 0 in string \"G5\""));
    EXPECT_TRUE(errors.ExpectErrorLike("vendors-info-wrong-format.xml(.*)Vendor: 2,(.*)Cannot find attribute with name name"));
    EXPECT_TRUE(errors.ExpectErrorLike("vendors-info-wrong-format.xml(.*)Vendor: 3,(.*)Duplicate vendor id"));
    EXPECT_TRUE(errors.ExpectErrorLike("vendors-info-wrong-format.xml(.*)Vendor: 5,(.*)Key 'WRONG_TYPE' not found in enum"));
    EXPECT_TRUE(errors.ExpectErrorLike("vendors-info-wrong-format.xml(.*)Vendor: 6,(.*)Cannot find attribute with name type"));

    TVendorsInfo expected;

    TVendorInfo vendorInfo;
    vendorInfo.Name = "Vendor 3";
    vendorInfo.Description = "Vendor 3 description.";
    vendorInfo.Website = "http://samsung.com/";
    expected[3] = vendorInfo;

    vendorInfo = TVendorInfo();
    vendorInfo.Name = "Vendor 5";
    expected[5] = vendorInfo;

    vendorInfo = TVendorInfo();
    vendorInfo.Name = "Vendor 6";
    expected[6] = vendorInfo;

    EXPECT_EQ(expected, result);
}
