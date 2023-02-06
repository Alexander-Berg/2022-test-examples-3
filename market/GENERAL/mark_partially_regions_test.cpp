#include "common.h"

#include <library/cpp/testing/unittest/gtest.h>

using NMarket::NModifiersApplier::TModifiersMap;
using NMarket::NModifiersApplier::TRegionMeta;
using NMarket::NModifiersApplier::TRegionToMetaMap;

TEST(PartiallyRegion, NoModifiers) {
    const auto& applier = GetModifiersApplier();

    TRegionToMetaMap metaMap;
    TModifiersMap modifiersMap;

    applier.MarkCostPartiallyRegions(metaMap, modifiersMap);
    applier.MarkTimePartiallyRegions(metaMap, modifiersMap);

    EXPECT_EQ(metaMap.size(), 0);
}

TEST(PartiallyRegion, CostModifierWithOneRegion) {
    const auto& applier = GetModifiersApplier();

    TRegionToMetaMap metaMap{
        {219, TRegionMeta{.CostModifierId = 10, .CostModifierPriority = 1, .Flag = 8}},
    };

    TRegionToMetaMap expected{
        {219, TRegionMeta{.CostModifierId = 10, .CostModifierPriority = 1, .Flag = 8}},
        {225, TRegionMeta{.Flag = 4}},
    };

    TModifiersMap modifiersMap{
        {10, GetModifier(10)},
    };

    applier.MarkCostPartiallyRegions(metaMap, modifiersMap);

    EXPECT_EQ(metaMap[219], expected[219]);
    EXPECT_EQ(metaMap[225], expected[225]);
}

TEST(PartiallyRegion, TimeModifierWithoutRegions) {
    const auto& applier = GetModifiersApplier();

    TRegionToMetaMap metaMap{
        {10000, TRegionMeta{.TimeModifierId = 11, .TimeModifierPriority = 1}},
    };

    TRegionToMetaMap expected{
        {10000, TRegionMeta{.TimeModifierId = 11, .TimeModifierPriority = 1}},
    };

    TModifiersMap modifiersMap{
        {11, GetModifier(11)},
    };

    applier.MarkTimePartiallyRegions(metaMap, modifiersMap);

    EXPECT_EQ(metaMap[10000], expected[10000]);
}

TEST(PartiallyRegion, CostModifiersInIndependentRegions) {
    const auto& applier = GetModifiersApplier();

    TRegionToMetaMap metaMap{
        {219, TRegionMeta{.CostModifierId = 10, .CostModifierPriority = 2, .Flag = 8}},
        {213, TRegionMeta{.CostModifierId = 22, .CostModifierPriority = 1, .Flag = 8}},
    };

    TRegionToMetaMap expected{
        {219, TRegionMeta{.CostModifierId = 10, .CostModifierPriority = 2, .Flag = 8}},
        {213, TRegionMeta{.CostModifierId = 22, .CostModifierPriority = 1, .Flag = 8}},
        {225, TRegionMeta{.Flag = 4}},
    };

    TModifiersMap modifiersMap{
        {10, GetModifier(10)},
        {22, GetModifier(22)},
    };

    applier.MarkCostPartiallyRegions(metaMap, modifiersMap);

    EXPECT_EQ(metaMap[213], expected[213]);
    EXPECT_EQ(metaMap[219], expected[219]);
    EXPECT_EQ(metaMap[225], expected[225]);
}

TEST(PartiallyRegion, TwoCostModifiersInSameBranchRegions) {
    const auto& applier = GetModifiersApplier();

    TRegionToMetaMap metaMap{
        {148660, TRegionMeta{.CostModifierId = 14, .CostModifierPriority = 2}},
        {11010, TRegionMeta{.CostModifierId = 15, .CostModifierPriority = 1, .Flag = 8}},
    };

    TRegionToMetaMap expected{
        {148660, TRegionMeta{.CostModifierId = 14, .CostModifierPriority = 2}},
        {11010, TRegionMeta{.CostModifierId = 15, .CostModifierPriority = 1, .Flag = 8 | 4}},
        {99314, TRegionMeta{.Flag = 4}}, // Бабаюртовский район
        {225, TRegionMeta{.Flag = 4}},
    };

    TModifiersMap modifiersMap{
        {14, GetModifier(14)},
        {15, GetModifier(15)},
    };

    applier.MarkCostPartiallyRegions(metaMap, modifiersMap);

    EXPECT_EQ(metaMap[148660], expected[148660]);
    EXPECT_EQ(metaMap[11010], expected[11010]);
    EXPECT_EQ(metaMap[99314], expected[99314]);
    EXPECT_EQ(metaMap[225], expected[225]);
}

TEST(PartiallyRegion, DifferentTypeModifiersInTwoIndependentRegions) {
    const auto& applier = GetModifiersApplier();

    TRegionToMetaMap metaMap{
        {
            213,
            TRegionMeta{
                .TimeModifierId = 23,
                .TimeModifierPriority = 1,
                .CostModifierId = 22,
                .CostModifierPriority = 1,
                .Flag = 8 | 32,
            }
        },
    };

    TRegionToMetaMap expected{
        {
            213,
            TRegionMeta{
                .TimeModifierId = 23,
                .TimeModifierPriority = 1,
                .CostModifierId = 22,
                .CostModifierPriority = 1,
                .Flag = 8 | 32
            }
        },
        {
            225,
            TRegionMeta{.Flag = 4 | 16}
        },
    };

    applier.MarkTimePartiallyRegions(metaMap, {{23, GetModifier(23)}});
    applier.MarkCostPartiallyRegions(metaMap, {{22, GetModifier(22)}});

    EXPECT_EQ(metaMap[213], expected[213]);
    EXPECT_EQ(metaMap[225], expected[225]);
}
