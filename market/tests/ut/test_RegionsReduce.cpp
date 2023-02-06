#include <market/idx/stats/src/Helper.h>

#include <market/library/offers_common/Geo.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

namespace {
void LoadGeo()
{
    // Load geobase
    GEO.loadTree(SRC_("geobase/geo.c2p").c_str());
    GEO.loadInfo(SRC_("geobase/geobase.xml").c_str());
    GEO.cache_heatup();
}
}

TEST(TestRegionsReduce, test_geoext_reduce_regions)
{
    LoadGeo();

    constexpr size_t SANE_REGIONS_SIZE = 3;
    TVector<ui32> minorRegions = {
        /*10000, 10001, 225, 3, 1, 213,*/
        216, 20358, 20362, 114619, 20356, 20357,
        20363, 114620, 20279, 20359, 20361, 20360,
        /*10002, 84, */29360, 29355, 29361
    };
    TVector<ui32> allRegions = {
        10000, 10001, 225, 3, 1, 213,
        216, 20358, 20362, 114619, 20356, 20357,
        20363, 114620, 20279, 20359, 20361, 20360,
        10002, 84, 29360, 29355, 29361
    };

    THashSet<ui32> result;
    GeoExt::ReduceRegions(minorRegions, result, SANE_REGIONS_SIZE, true);
    ASSERT_TRUE(result.size() == 2);
    ASSERT_TRUE(std::find(result.begin(), result.end(), 84) != result.end());
    ASSERT_TRUE(std::find(result.begin(), result.end(), 225) != result.end());

    result.clear();
    GeoExt::ReduceRegions(allRegions, result, SANE_REGIONS_SIZE, false);
    ASSERT_TRUE(result.size() == 3);
    ASSERT_TRUE(std::find(result.begin(), result.end(), 10000) != result.end());
    ASSERT_TRUE(std::find(result.begin(), result.end(), 10001) != result.end());
    ASSERT_TRUE(std::find(result.begin(), result.end(), 10002) != result.end());
}


TEST(TestRegionsReduce, test_geoext_reduce_regions_to_cities)
{
    LoadGeo();

    Market::regions_list_type minorRegions = {
            /*10000, 10001, 225, 3, 1, 213,*/
            216, 20358, 20362, 114619, 20356, 20357,
            20363, 114620, 20279, 20359, 20361, 20360,
            /*10002, 84, */29360, 29355, 29361
    };
    TString minorString = GeoExt::MakeRegionsString(minorRegions);
    GeoExt::ReduceToCities(minorString);
    ASSERT_STREQ(minorString, "213 216 29355 29360 29361");

    Market::regions_list_type allRegions = {
            10000, 10001, 225, 3, 1, 213,
            216, 20358, 20362, 114619, 20356, 20357,
            20363, 114620, 20279, 20359, 20361, 20360,
            10002, 84, 29360, 29355, 29361
    };
    TString allString = GeoExt::MakeRegionsString(allRegions);
    GeoExt::ReduceToCities(allString);
    ASSERT_STREQ(allString, "1 3 84 213 216 225 10000 10001 10002 29355 29360 29361");

    Market::regions_list_type emptyRegions {};
    TString emptyString = GeoExt::MakeRegionsString(emptyRegions);
    GeoExt::ReduceToCities(emptyString);
    ASSERT_STREQ(emptyString, "");
}


template <typename C1, typename C2>
void AssertContainers(const C1& c1, const C2& c2) {
    ASSERT_EQ(c1.size(), c2.size());
    auto it1 = c1.begin();
    auto it2 = c2.begin();
    for (; it1 != c1.end() && it2 != c2.end();) {
        ASSERT_EQ(*it1, *it2);
        ++it1; ++it2;
    }
}


TEST(TestRegionsReduce, reduce_int_regions_equal_to_reduce_string_regions)
{
    LoadGeo();

    TVector<Market::TRegionId> allRegions = {
            10000, 10001, 225, 3, 1, 213,
            216, 20358, 20362, 114619, 20356, 20357,
            20363, 114620, 20279, 20359, 20361, 20360,
            10002, 84, 29360, 29355, 29361
    };
    TString allString = GeoExt::MakeRegionsString(allRegions);
    GeoExt::ReduceToCities(allString);
    ASSERT_STREQ(allString, "1 3 84 213 216 225 10000 10001 10002 29355 29360 29361");

    TVector<Market::TRegionId> newRegions {};
    GeoExt::SplitRegionsString(allString, &newRegions);
    auto intRegions = GeoExt::ReduceToCities(allRegions);
    AssertContainers(intRegions, newRegions);

    TVector<Market::TRegionId> newRegions1 {};
    GeoExt::SplitRegionsString(allString, &newRegions1);
    auto intRegions1 = GeoExt::ReduceToCities(allRegions);
    AssertContainers(intRegions1, newRegions1);


    TVector<Market::TRegionId> emptyRegions {};
    TString emptyString = GeoExt::MakeRegionsString(emptyRegions);
    GeoExt::ReduceToCities(emptyString);
    ASSERT_STREQ(emptyString, "");

    TVector<Market::TRegionId> newEmptyRegions {};
    GeoExt::SplitRegionsString(emptyString, &newEmptyRegions);
    auto intEmptyRegions = GeoExt::ReduceToCities(emptyRegions);
    AssertContainers(intEmptyRegions, newEmptyRegions);
}
