#include "common.h"

#include <library/cpp/testing/unittest/gtest.h>

using NMarket::NModifiersApplier::TModifiersMap;
using NMarket::NModifiersApplier::TRegionMeta;
using NMarket::NModifiersApplier::TRegionToMetaMap;

// Нет модификаторов
TEST(GetAvailableRegionsByModifiers, NoModifiers) {
    const auto& applier = GetModifiersApplier();

    TRegionToMetaMap metaMap;
    TModifiersMap modifiersMap;

    auto availableRegions = applier.GetAvailableRegionsByModifiers(metaMap, modifiersMap);

    EXPECT_EQ(metaMap.size(), 0);
    EXPECT_EQ(availableRegions.size(), 0);
}

// Включающий модификатор верхнего уровня
TEST(GetAvailableRegionsByModifiers, TopLevelOnModifier) {
    const auto& applier = GetModifiersApplier();

    TRegionToMetaMap metaMap{
        {40, TRegionMeta{.RegionalModifierId = 18, .RegionalModifierPriority = 1, .Flag = 2}},
    };

    TRegionToMetaMap expected{
        {40, TRegionMeta{.RegionalModifierId = 18, .RegionalModifierPriority = 1, .Flag = 2}},
    };

    TModifiersMap modifiersMap{
        {18, GetModifier(18)},
    };

    auto availableRegions = applier.GetAvailableRegionsByModifiers(metaMap, modifiersMap);

    EXPECT_EQ(metaMap[40], expected[40]);
    EXPECT_EQ(availableRegions.size(), 0);
}

// Есть только отключающий модификатор (заодно проверяем пометку на частичность опций по пути к корню)
TEST(GetAvailableRegionsByModifiers, OffModifier) {
    const auto& applier = GetModifiersApplier();

    TRegionToMetaMap metaMap{
        {213, TRegionMeta{.RegionalModifierId = 16, .RegionalModifierPriority = 1, .Flag = 2}},
    };

    TRegionToMetaMap expected{
        {213, TRegionMeta{.RegionalModifierId = 16, .RegionalModifierPriority = 1, .Flag = 2}},
        {225, TRegionMeta{.Flag = 1}},
        {10000, TRegionMeta{.Flag = 1}},
    };

    TModifiersMap modifiersMap{
        {16, GetModifier(16)},
    };

    auto availableRegions = applier.GetAvailableRegionsByModifiers(metaMap, modifiersMap);

    EXPECT_EQ(metaMap[213], expected[213]);
    EXPECT_EQ(metaMap[225], expected[225]);
    EXPECT_EQ(metaMap[10000], expected[10000]);
    EXPECT_EQ(availableRegions.size(), 0);
}

// Отключающий модификатор без регионов
TEST(GetAvailableRegionsByModifiers, OffModifierWithoutRegions) {
    const auto& applier = GetModifiersApplier();

    TRegionToMetaMap metaMap{
        {10000, TRegionMeta{.RegionalModifierId = 19, .RegionalModifierPriority = 1, .Flag = 2}},
    };

    TRegionToMetaMap expected{
        {10000, TRegionMeta{.RegionalModifierId = 19, .RegionalModifierPriority = 1, .Flag = 2}},
    };

    TModifiersMap modifiersMap{
        {19, GetModifier(19)},
    };

    auto availableRegions = applier.GetAvailableRegionsByModifiers(metaMap, modifiersMap);

    EXPECT_EQ(metaMap[10000], expected[10000]);
    EXPECT_EQ(availableRegions.size(), 0);
}

// Включающий модификатор под отключающим
TEST(GetAvailableRegionsByModifiers, OnModifierUnderOff) {
    const auto& applier = GetModifiersApplier();

    TRegionToMetaMap metaMap{
        {40, TRegionMeta{.RegionalModifierId = 18, .RegionalModifierPriority = 2}},
        {10000, TRegionMeta{.RegionalModifierId = 19, .RegionalModifierPriority = 1, .Flag = 2}},
    };

    TRegionToMetaMap expected{
        {40, TRegionMeta{.RegionalModifierId = 18, .RegionalModifierPriority = 2}},
        {10000, TRegionMeta{.RegionalModifierId = 19, .RegionalModifierPriority = 1, .Flag = 2 | 1}},
    };

    TModifiersMap modifiersMap{
        {18, GetModifier(18)},
        {19, GetModifier(19)},
    };

    auto availableRegions = applier.GetAvailableRegionsByModifiers(metaMap, modifiersMap);

    EXPECT_EQ(metaMap[40], expected[40]);
    EXPECT_EQ(metaMap[10000], expected[10000]);
    EXPECT_EQ(availableRegions, THashSet<NMarket::NTypes::TRegionId>{40});
}

// Отключающий модификатор под включающим
TEST(GetAvailableRegionsByModifiers, OffModifierUnderOn) {
    const auto& applier = GetModifiersApplier();

    TRegionToMetaMap metaMap{
        {40, TRegionMeta{.RegionalModifierId = 18, .RegionalModifierPriority = 1, .Flag = 2}},
        {11119, TRegionMeta{.RegionalModifierId = 20, .RegionalModifierPriority = 2}},
    };

    TRegionToMetaMap expected{
        {40, TRegionMeta{.RegionalModifierId = 18, .RegionalModifierPriority = 1, .Flag = 2 | 1}},
        {10000, TRegionMeta{.Flag = 1}},
        {11119, TRegionMeta{.RegionalModifierId = 20, .RegionalModifierPriority = 2}},
    };

    TModifiersMap modifiersMap{
        {18, GetModifier(18)},
        {20, GetModifier(20)},
    };

    auto availableRegions = applier.GetAvailableRegionsByModifiers(metaMap, modifiersMap);

    EXPECT_EQ(metaMap[40], expected[40]);
    EXPECT_EQ(metaMap[10000], expected[10000]);
    EXPECT_EQ(metaMap[11119], expected[11119]);
    EXPECT_EQ(availableRegions.size(), 0);
}

// Включающий модификатор под включающим не приводит к появлению пометки частичности, дочерний модификатор не возвращается
TEST(GetAvailableRegionsByModifiers, OnModifierUnderNotTopLevelOn) {
    const auto& applier = GetModifiersApplier();

    TRegionToMetaMap metaMap{
        {40, TRegionMeta{.RegionalModifierId = 18, .RegionalModifierPriority = 2}},
        {10000, TRegionMeta{.RegionalModifierId = 19, .RegionalModifierPriority = 1, .Flag = 2}},
        {11119, TRegionMeta{.RegionalModifierId = 21, .RegionalModifierPriority = 3}},
    };

    TRegionToMetaMap expected{
        {40, TRegionMeta{.RegionalModifierId = 18, .RegionalModifierPriority = 2}},
        {225, TRegionMeta{.Flag = 1}},
        {10000, TRegionMeta{.RegionalModifierId = 19, .RegionalModifierPriority = 1, .Flag = 2 | 1}},
        {11119, TRegionMeta{.RegionalModifierId = 0, .RegionalModifierPriority = 0}},
    };

    TModifiersMap modifiersMap{
        {18, GetModifier(18)},
        {19, GetModifier(19)},
        {21, GetModifier(21)},
    };

    auto availableRegions = applier.GetAvailableRegionsByModifiers(metaMap, modifiersMap);

    EXPECT_EQ(metaMap[40], expected[40]);
    EXPECT_EQ(metaMap[225], expected[225]);
    EXPECT_EQ(metaMap[10000], expected[10000]);
    EXPECT_EQ(metaMap[11119], expected[11119]);
    EXPECT_EQ(availableRegions, THashSet<NMarket::NTypes::TRegionId>{40});
}

// Отключающий под двумя включающими приводит к появлению флага частичности между ними.
TEST(GetAvailableRegionsByModifiers, OffModifierUnderTwoOn) {
    const auto& applier = GetModifiersApplier();

    TRegionToMetaMap metaMap{
        {40, TRegionMeta{.RegionalModifierId = 18, .RegionalModifierPriority = 2}},
        {10000, TRegionMeta{.RegionalModifierId = 19, .RegionalModifierPriority = 1, .Flag = 2}},
        {11119, TRegionMeta{.RegionalModifierId = 20, .RegionalModifierPriority = 3}},
    };

    TRegionToMetaMap expected{
        {40, TRegionMeta{.RegionalModifierId = 18, .RegionalModifierPriority = 2, .Flag = 1}},
        {225, TRegionMeta{.Flag = 1}},
        {10000, TRegionMeta{.RegionalModifierId = 19, .RegionalModifierPriority = 1, .Flag = 2 | 1}},
        {11119, TRegionMeta{.RegionalModifierId = 20, .RegionalModifierPriority = 3}},
    };

    TModifiersMap modifiersMap{
        {18, GetModifier(18)},
        {19, GetModifier(19)},
        {20, GetModifier(20)},
    };

    auto availableRegions = applier.GetAvailableRegionsByModifiers(metaMap, modifiersMap);

    EXPECT_EQ(metaMap[40], expected[40]);
    EXPECT_EQ(metaMap[225], expected[225]);
    EXPECT_EQ(metaMap[10000], expected[10000]);
    EXPECT_EQ(metaMap[11119], expected[11119]);
    EXPECT_EQ(availableRegions, THashSet<NMarket::NTypes::TRegionId>{40});
}
