#include "common.h"

#include <library/cpp/testing/unittest/gtest.h>

using NMarket::NModifiersApplier::TRegionMeta;
using NMarket::NModifiersApplier::TRegionToMetaMap;

TEST(FillModifiers, NoModifiers) {
    const auto& applier = GetModifiersApplier();
    delivery_calc::mbi::BucketInfo bucketInfo;
    TRegionToMetaMap metaMap;
    applier.FillRegionalModifiers(metaMap, bucketInfo);
    applier.FillCostModifiers(metaMap, bucketInfo);
    applier.FillTimeModifiers(metaMap, bucketInfo);

    EXPECT_EQ(metaMap.size(), 0);
}

TEST(FillModifiers, NoModifierConversion) {
    const auto& applier = GetModifiersApplier();
    delivery_calc::mbi::BucketInfo bucketInfo;
    bucketInfo.add_region_availability_modifiers_ids(0);
    TRegionToMetaMap metaMap;
    EXPECT_THROW(applier.FillRegionalModifiers(metaMap, bucketInfo), yexception);
}

TEST(FillModifiers, CostModifier) {
    const auto& applier = GetModifiersApplier();
    delivery_calc::mbi::BucketInfo bucketInfo;
    bucketInfo.add_cost_modifiers_ids(10);
    TRegionToMetaMap metaMap;
    applier.FillCostModifiers(metaMap, bucketInfo);

    TRegionToMetaMap expected{
        {219, TRegionMeta{.CostModifierId = 10, .CostModifierPriority = 1}},
    };

    EXPECT_EQ(metaMap.size(), expected.size());
    EXPECT_EQ(metaMap[219], expected[219]);
}

TEST(FillModifiers, TimeModifierWithoutRegions) {
    const auto& applier = GetModifiersApplier();
    delivery_calc::mbi::BucketInfo bucketInfo;
    bucketInfo.add_time_modifiers_ids(11);
    TRegionToMetaMap metaMap;
    applier.FillTimeModifiers(metaMap, bucketInfo);

    TRegionToMetaMap expected{
        {10000, TRegionMeta{.TimeModifierId = 11, .TimeModifierPriority = 1}},
    };

    EXPECT_EQ(metaMap.size(), expected.size());
    EXPECT_EQ(metaMap[10000], expected[10000]);
}

TEST(FillModifiers, ManyRegionalModifiers) {
    const auto& applier = GetModifiersApplier();
    delivery_calc::mbi::BucketInfo bucketInfo;
    bucketInfo.add_region_availability_modifiers_ids(12);
    bucketInfo.add_region_availability_modifiers_ids(13);
    TRegionToMetaMap metaMap;
    applier.FillRegionalModifiers(metaMap, bucketInfo);

    TRegionToMetaMap expected{
        {1, TRegionMeta{.RegionalModifierId = 12, .RegionalModifierPriority = 2}},
        {213, TRegionMeta{.RegionalModifierId = 13, .RegionalModifierPriority = 1}},
        {225, TRegionMeta{.RegionalModifierId = 13, .RegionalModifierPriority = 1}},
    };

    EXPECT_EQ(metaMap.size(), expected.size());
    EXPECT_EQ(metaMap[1], expected[1]);
    EXPECT_EQ(metaMap[213], expected[213]);
    EXPECT_EQ(metaMap[225], expected[225]);
}

TEST(FillModifiers, CostModifiersWithSameRegion) {
    const auto& applier = GetModifiersApplier();
    delivery_calc::mbi::BucketInfo bucketInfo;
    bucketInfo.add_cost_modifiers_ids(14);
    bucketInfo.add_cost_modifiers_ids(15);
    TRegionToMetaMap metaMap;
    applier.FillCostModifiers(metaMap, bucketInfo);

    TRegionToMetaMap expected{
        {148660, TRegionMeta{.CostModifierId = 14, .CostModifierPriority = 2}},
        {11010, TRegionMeta{.CostModifierId = 14, .CostModifierPriority = 2}},
        {121902, TRegionMeta{.CostModifierId = 15, .CostModifierPriority = 1}},
    };

    EXPECT_EQ(metaMap.size(), expected.size());
    EXPECT_EQ(metaMap[148660], expected[148660]);
    EXPECT_EQ(metaMap[11010], expected[11010]);
    EXPECT_EQ(metaMap[121902], expected[121902]);
}

TEST(FillModifiers, ModifiersWithDifferentTypeInSameRegion) {
    const auto& applier = GetModifiersApplier();
    delivery_calc::mbi::BucketInfo bucketInfo;
    bucketInfo.add_cost_modifiers_ids(10);
    bucketInfo.add_time_modifiers_ids(17);
    TRegionToMetaMap metaMap;
    applier.FillCostModifiers(metaMap, bucketInfo);
    applier.FillTimeModifiers(metaMap, bucketInfo);

    TRegionToMetaMap expected{
        {
            219,
            TRegionMeta{
                .TimeModifierId = 17,
                .TimeModifierPriority = 1,
                .CostModifierId = 10,
                .CostModifierPriority = 1,
            },
        },
    };

    EXPECT_EQ(metaMap.size(), expected.size());
    EXPECT_EQ(metaMap[219], expected[219]);
}
