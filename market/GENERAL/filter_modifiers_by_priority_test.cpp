#include "common.h"

#include <library/cpp/testing/unittest/gtest.h>

using NMarket::NModifiersApplier::TModifiersMap;
using NMarket::NModifiersApplier::TRegionMeta;
using NMarket::NModifiersApplier::TRegionToMetaMap;

TEST(FilterModifiersByPriority, NoModifiers) {
    const auto& applier = GetModifiersApplier();

    TRegionToMetaMap metaMap;
    TModifiersMap modifiersMap;

    applier.FilterRegionalModifiersByPriority(metaMap, modifiersMap);
    applier.FilterCostModifiersByPriority(metaMap, modifiersMap);
    applier.FilterTimeModifiersByPriority(metaMap, modifiersMap);

    EXPECT_EQ(metaMap.size(), 0);
}

TEST(FilterModifiersByPriority, CostModifier) {
    const auto& applier = GetModifiersApplier();

    TRegionToMetaMap metaMap{
        {219, TRegionMeta{.CostModifierId = 10, .CostModifierPriority = 1}},
    };

    TRegionToMetaMap expected{
        {219, TRegionMeta{.CostModifierId = 10, .CostModifierPriority = 1, .Flag = 8}},
    };

    TModifiersMap modifiersMap{
        {10, GetModifier(10)},
    };

    applier.FilterCostModifiersByPriority(metaMap, modifiersMap);

    EXPECT_EQ(metaMap[219], expected[219]);
}

TEST(FilterModifiersByPriority, TimeModifierWithoutRegions) {
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

    applier.FilterTimeModifiersByPriority(metaMap, modifiersMap);

    EXPECT_EQ(metaMap[10000], expected[10000]);
}

TEST(FilterModifiersByPriority, OneRegionalModifierFilterAnother) {
    const auto& applier = GetModifiersApplier();
    TRegionToMetaMap metaMap{
        {1, TRegionMeta{.RegionalModifierId = 12, .RegionalModifierPriority = 2}},
        {213, TRegionMeta{.RegionalModifierId = 13, .RegionalModifierPriority = 1}},
        {225, TRegionMeta{.RegionalModifierId = 13, .RegionalModifierPriority = 1}},
    };

    TRegionToMetaMap expected{
        {1, TRegionMeta{.RegionalModifierId = 12, .RegionalModifierPriority = 2}},
        {213, TRegionMeta{.RegionalModifierId = 0, .RegionalModifierPriority = 0}},
        {225, TRegionMeta{.RegionalModifierId = 13, .RegionalModifierPriority = 1, .Flag = 2}},
    };

    TModifiersMap modifiersMap{
        {12, GetModifier(12)},
        {13, GetModifier(13)},
    };

    applier.FilterRegionalModifiersByPriority(metaMap, modifiersMap);

    EXPECT_EQ(metaMap[1], expected[1]);
    EXPECT_EQ(metaMap[213], expected[213]);
    EXPECT_EQ(metaMap[225], expected[225]);
}

TEST(FilterModifiersByPriority, RegionalModifierFilterItself) {
    const auto& applier = GetModifiersApplier();
    TRegionToMetaMap metaMap{
        {1, TRegionMeta{.RegionalModifierId = 12, .RegionalModifierPriority = 1}},
        {213, TRegionMeta{.RegionalModifierId = 13, .RegionalModifierPriority = 2}},
        {225, TRegionMeta{.RegionalModifierId = 13, .RegionalModifierPriority = 2}},
    };

    TRegionToMetaMap expected{
        {1, TRegionMeta{.RegionalModifierId = 0, .RegionalModifierPriority = 0}},
        {213, TRegionMeta{.RegionalModifierId = 0, .RegionalModifierPriority = 0}},
        {225, TRegionMeta{.RegionalModifierId = 13, .RegionalModifierPriority = 2, .Flag = 2}},
    };

    TModifiersMap modifiersMap{
        {12, GetModifier(12)},
        {13, GetModifier(13)},
    };

    applier.FilterRegionalModifiersByPriority(metaMap, modifiersMap);

    EXPECT_EQ(metaMap[1], expected[1]);
    EXPECT_EQ(metaMap[213], expected[213]);
    EXPECT_EQ(metaMap[225], expected[225]);
}

TEST(FilterModifiersByPriority, RegionalModifierWithSameType) {
    const auto& applier = GetModifiersApplier();
    TRegionToMetaMap metaMap{
        {1, TRegionMeta{.RegionalModifierId = 12, .RegionalModifierPriority = 2}},
        {213, TRegionMeta{.RegionalModifierId = 16, .RegionalModifierPriority = 1}},
    };

    TRegionToMetaMap expected{
        {1, TRegionMeta{.RegionalModifierId = 12, .RegionalModifierPriority = 2, .Flag = 2}},
        {213, TRegionMeta{.RegionalModifierId = 0, .RegionalModifierPriority = 0}},
    };

    TModifiersMap modifiersMap{
        {12, GetModifier(12)},
        {16, GetModifier(16)},
    };

    applier.FilterRegionalModifiersByPriority(metaMap, modifiersMap);

    EXPECT_EQ(metaMap[1], expected[1]);
    EXPECT_EQ(metaMap[213], expected[213]);
}

TEST(FilterModifiersByPriority, CostModifiersWithSameRegion) {
    const auto& applier = GetModifiersApplier();
    TRegionToMetaMap metaMap{
        {148660, TRegionMeta{.CostModifierId = 14, .CostModifierPriority = 2}},
        {11010, TRegionMeta{.CostModifierId = 14, .CostModifierPriority = 2}},
        {121902, TRegionMeta{.CostModifierId = 15, .CostModifierPriority = 1}},
    };

    TRegionToMetaMap expected{
        {148660, TRegionMeta{.CostModifierId = 0, .CostModifierPriority = 0}},
        {11010, TRegionMeta{.CostModifierId = 14, .CostModifierPriority = 2, .Flag = 8}},
        {121902, TRegionMeta{.CostModifierId = 0, .CostModifierPriority = 0}},
    };

    TModifiersMap modifiersMap{
        {14, GetModifier(14)},
        {15, GetModifier(15)},
    };

    applier.FilterCostModifiersByPriority(metaMap, modifiersMap);

    EXPECT_EQ(metaMap[148660], expected[148660]);
    EXPECT_EQ(metaMap[11010], expected[11010]);
    EXPECT_EQ(metaMap[121902], expected[121902]);
}

TEST(FilterModifiersByPriority, ModifiersWithDifferentTypeInDifferentRegions) {
    const auto& applier = GetModifiersApplier();
    TRegionToMetaMap metaMap{
        {1, TRegionMeta{.RegionalModifierId = 12, .RegionalModifierPriority = 1}},
        {219, TRegionMeta{.CostModifierId = 10, .CostModifierPriority = 1}},
    };

    applier.FilterRegionalModifiersByPriority(metaMap, {{12, GetModifier(12)}});
    applier.FilterCostModifiersByPriority(metaMap, {{10, GetModifier(10)}});

    TRegionToMetaMap expected{
        {1, TRegionMeta{.RegionalModifierId = 12, .RegionalModifierPriority = 1, .Flag = 2}},
        {219, TRegionMeta{.CostModifierId = 10, .CostModifierPriority = 1, .Flag = 8}},
    };

    EXPECT_EQ(metaMap[1], expected[1]);
    EXPECT_EQ(metaMap[219], expected[219]);
}

TEST(FilterModifiersByPriority, ModifiersWithDifferentTypeInSame) {
    const auto& applier = GetModifiersApplier();
    TRegionToMetaMap metaMap{
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

    applier.FilterCostModifiersByPriority(metaMap, {{10, GetModifier(10)}});
    applier.FilterTimeModifiersByPriority(metaMap, {{17, GetModifier(17)}});

    TRegionToMetaMap expected{
        {
            219,
            TRegionMeta{
                .TimeModifierId = 17,
                .TimeModifierPriority = 1,
                .CostModifierId = 10,
                .CostModifierPriority = 1,
                .Flag = 32 | 8,
            },
        },
    };

    EXPECT_EQ(metaMap[219], expected[219]);
}
