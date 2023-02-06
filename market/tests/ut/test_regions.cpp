#include <market/idx/stats/src/Helper.h>

#include <market/library/offers_common/Geo.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

namespace {
void LoadGeo(Market::Geo &geo)
{
    // Load geobase
    geo.loadTree(SRC_("geobase/geo.c2p").c_str());
    geo.loadInfo(SRC_("geobase/geobase.xml").c_str());
    geo.cache_heatup();
}
}

TEST(TestRegions, test_region_children_cache)
{
    Market::Geo geo;
    LoadGeo(geo);

    RegionChildrenCache c(geo);

    const std::vector<int>& r123 = c.Get("1 2 3", true, true);
    const std::vector<int>& r123_copy = c.Get("1 2 3", true, true);
    const std::vector<int>& r321 = c.Get("3 2 1", true, true);
    const std::vector<int>& r123_nc = c.Get("1 2 3", false, true);
    const std::vector<int>& r321_nc = c.Get("3 2 1", false, true);
    const std::vector<int>& r321_nz = c.Get("3 2 1", true, false);


    ASSERT_TRUE(r123 == r123_copy);
    ASSERT_TRUE(&r123 == &r123_copy);
    ASSERT_TRUE(r123 == r321);
    ASSERT_TRUE(std::find(r123.begin(), r123.end(), 0) != r123.end());
    ASSERT_TRUE(std::find(r123.begin(), r123.end(), 213) != r123.end());

    ASSERT_TRUE(r123_nc == r321_nc);
    ASSERT_TRUE(std::find(r123_nc.begin(), r123_nc.end(), 213) == r123_nc.end());
    ASSERT_TRUE(std::find(r123_nc.begin(), r123_nc.end(), 0) != r123_nc.end());

    ASSERT_TRUE(r321 != r321_nc);
    ASSERT_TRUE(r321 != r321_nz);
    ASSERT_TRUE(std::find(r321_nz.begin(), r321_nz.end(), 213) != r321_nz.end());
    ASSERT_TRUE(std::find(r321_nz.begin(), r321_nz.end(), 0) == r321_nz.end());
}

TEST(TestRegions, test_split_regions)
{
    Market::regions_list_type regions;

    GeoExt::SplitRegionsString("", &regions);
    ASSERT_TRUE(regions.size() == 0);

    GeoExt::SplitRegionsString("    ", &regions);
    ASSERT_TRUE(regions.size() == 0);

    GeoExt::SplitRegionsString("  225  ", &regions);
    ASSERT_TRUE(regions.size() == 1);
    ASSERT_TRUE(regions.find(225) != regions.end());

    regions.clear();
    GeoExt::SplitRegionsString("215 225 1 166", &regions);
    ASSERT_TRUE(regions.size() == 4);
    ASSERT_TRUE(regions.find(215) != regions.end());
    ASSERT_TRUE(regions.find(225) != regions.end());
    ASSERT_TRUE(regions.find(1) != regions.end());
    ASSERT_TRUE(regions.find(166) != regions.end());
}
