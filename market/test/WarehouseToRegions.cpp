#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>
#include <market/library/snappy-protostream/mbo_stream.h>
#include <market/proto/content/mbo/MboParameters.pb.h>
#include <util/folder/tempdir.h>
#include <util/stream/file.h>
#include <util/generic/maybe.h>

#include <market/report/library/money/warehouse_to_regions/warehouse_to_regions.h>

using namespace NMarketReport;
//using TMboCategory = Market::Mbo::Parameters::Category;
//using EOutputType = Market::Mbo::Parameters::OutputType;

class WarehouseToRegions : public ::testing::Test {
protected:
    void SetUp() override {}
};

namespace{
    void PrepareMockFile(TString text, TString path) {
        TFileOutput file(path);
        file.Write(text);
        file.Flush();
    }
}

TEST(WarehouseToRegions, Load) {
    TTempDir temporaryDirectory("tmp");
    const TString warehouseToRegions = temporaryDirectory.Path() / "region_warehouse_mapping.tsv";

    PrepareMockFile("11013\t147\n11020\t147\n11167\t303\n11162\t300\n10897\t300\n10897\t301\n", warehouseToRegions);
    NMarketReport::LoadWarehouseToRegionsMap(warehouseToRegions);

    ASSERT_EQ(true, NMarketReport::CheckWarehouse(11013, 147));
    ASSERT_EQ(true, NMarketReport::CheckWarehouse(11020, 147));
    ASSERT_EQ(true, NMarketReport::CheckWarehouse(11167, 303));
    ASSERT_EQ(true, NMarketReport::CheckWarehouse(11162, 300));
    ASSERT_EQ(true, NMarketReport::CheckWarehouse(10897, 300));
    ASSERT_EQ(true, NMarketReport::CheckWarehouse(10897, 301));

    ASSERT_EQ(false, NMarketReport::CheckWarehouse(11013, 301));
    ASSERT_EQ(false, NMarketReport::CheckWarehouse(11162, 301));
    ASSERT_EQ(false, NMarketReport::CheckWarehouse(10897, 147));
}

TEST(WarehouseToRegions, LoadFB) {
    const TString LoadWarehouseToRegionsPath = SRC_("./TestData/region_warehouse_mapping.fb").data();
    LoadWarehouseToRegionsMapFB(LoadWarehouseToRegionsPath);

    ASSERT_EQ(true, NMarketReport::CheckWarehouse(977, 147, true));
    ASSERT_EQ(true, NMarketReport::CheckWarehouse(11232, 300, true));
    ASSERT_EQ(true, NMarketReport::CheckWarehouse(10853, 301, true));
}
