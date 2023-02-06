#include "common.h"

#include <library/cpp/testing/unittest/gtest.h>

using NMarket::NModifiersApplier::EBucketType;
using NMarket::NModifiersApplier::TRegionMeta;
using NMarket::NModifiersApplier::TOptionGroups;

TEST(CollectGroups, BucketNotExists) {
    const auto& applier = GetModifiersApplier();
    const i64 bucketId = 0;

    EXPECT_THROW(applier.CollectGroups(bucketId, EBucketType::COURIER), yexception);
    EXPECT_THROW(applier.CollectGroups(bucketId, EBucketType::OLD_PICKUP), yexception);
    EXPECT_THROW(applier.CollectGroups(bucketId, EBucketType::OLD_POST), yexception);
    EXPECT_THROW(applier.CollectGroups(bucketId, EBucketType::PICKUP), yexception);
}

TEST(CollectGroups, BucketWithoutOptions) {
    const auto& applier = GetModifiersApplier();
    const i64 bucketId = 1;
    ASSERT_NO_THROW(applier.CollectGroups(bucketId, EBucketType::COURIER));

    auto meta = applier.CollectGroups(bucketId, EBucketType::COURIER);
    EXPECT_EQ(meta.RegionsMeta.size(), 0);
    EXPECT_EQ(meta.CarrierIds.size(), 0);
    EXPECT_EQ(meta.RegionWithOptionsGroup.size(), 0);
}

TEST(CollectGroups, CourierBucketWithNormalOption) {
    const auto& applier = GetModifiersApplier();
    const i64 bucketId = 2;
    ASSERT_NO_THROW(applier.CollectGroups(bucketId, EBucketType::COURIER));

    auto meta = applier.CollectGroups(bucketId, EBucketType::COURIER);
    TRegionMeta expectedMeta{.OptionGroups = {{1, delivery_calc::mbi::NORMAL_OPTION}}};
    EXPECT_EQ(meta.RegionsMeta.size(), 1);
    EXPECT_EQ(meta.RegionsMeta[178], expectedMeta);
    EXPECT_EQ(TVector<i32>(meta.CarrierIds.begin(), meta.CarrierIds.end()), TVector<i32>{99});
    EXPECT_EQ(meta.RegionWithOptionsGroup, THashSet<NMarket::NTypes::TRegionId>{178});
}

TEST(CollectGroups, CourierBucketWithForbiddenAndUnexpectedOptions) {
    const auto& applier = GetModifiersApplier();
    const i64 bucketId = 3;
    ASSERT_NO_THROW(applier.CollectGroups(bucketId, EBucketType::COURIER));

    auto meta = applier.CollectGroups(bucketId, EBucketType::COURIER);
    TRegionMeta expectedBelarusMeta{.OptionGroups = {{0, delivery_calc::mbi::FORBIDDEN_OPTION}}};
    TRegionMeta expectedUkraineMeta{.OptionGroups = {{0, delivery_calc::mbi::UNSPECIFIC_OPTION}}};
    EXPECT_EQ(meta.RegionsMeta.size(), 2);
    EXPECT_EQ(meta.RegionsMeta[149], expectedBelarusMeta);
    EXPECT_EQ(meta.RegionsMeta[187], expectedUkraineMeta);
    EXPECT_EQ(TVector<i32>(meta.CarrierIds.begin(), meta.CarrierIds.end()), (TVector<i32>{101, 102}));
    EXPECT_EQ(meta.RegionWithOptionsGroup, (THashSet<NMarket::NTypes::TRegionId>{149, 187}));
}

TEST(CollectGroups, OldPickupBucket) {
    const auto& applier = GetModifiersApplier();
    const i64 bucketId = 4;
    ASSERT_NO_THROW(applier.CollectGroups(bucketId, EBucketType::OLD_PICKUP));

    auto meta = applier.CollectGroups(bucketId, EBucketType::OLD_PICKUP);
    TRegionMeta expectedAzerbaijanMeta{.OptionGroups = {{2, delivery_calc::mbi::NORMAL_OPTION}}};
    TRegionMeta expectedArmeniaMeta{.OptionGroups = {{3, delivery_calc::mbi::NORMAL_OPTION}}};
    EXPECT_EQ(meta.RegionsMeta.size(), 2);
    EXPECT_EQ(meta.RegionsMeta[167], expectedAzerbaijanMeta);
    EXPECT_EQ(meta.RegionsMeta[168], expectedArmeniaMeta);
    EXPECT_EQ(TVector<i32>(meta.CarrierIds.begin(), meta.CarrierIds.end()), (TVector<i32>{103, 104}));
    EXPECT_EQ(meta.RegionWithOptionsGroup, (THashSet<NMarket::NTypes::TRegionId>{167, 168}));
}

// Старый пикап бакет с группой опций без региона
// Сейчас мы скипаем группы опций без региона.
// Чтобы изменить это поведение нужно или договариваться с mstat/аналитиками о протоколе
// или с КД о том, чтобы они начали проставлять таким группам опций домашний регион магазина
TEST(CollectGroups, OldPickupBucketWithUnregionOptionGroup) {
    const auto& applier = GetModifiersApplier();
    const i64 bucketId = 5;
    ASSERT_NO_THROW(applier.CollectGroups(bucketId, EBucketType::OLD_PICKUP));

    auto meta = applier.CollectGroups(bucketId, EBucketType::OLD_PICKUP);
    EXPECT_EQ(meta.RegionsMeta.size(), 0);
    EXPECT_EQ(TVector<i32>(meta.CarrierIds.begin(), meta.CarrierIds.end()), TVector<i32>{105});
    EXPECT_EQ(meta.RegionWithOptionsGroup.size(), 0);
}

TEST(CollectGroups, OldPostBucket) {
    const auto& applier = GetModifiersApplier();
    const i64 bucketId = 6;
    ASSERT_NO_THROW(applier.CollectGroups(bucketId, EBucketType::OLD_POST));

    auto meta = applier.CollectGroups(bucketId, EBucketType::OLD_POST);
    TRegionMeta expectedKazakhstanMeta{.OptionGroups = {{4, delivery_calc::mbi::NORMAL_OPTION}}};
    TRegionMeta expectedKyrgyzstanMeta{.OptionGroups = {{5, delivery_calc::mbi::NORMAL_OPTION}}};
    EXPECT_EQ(meta.RegionsMeta.size(), 2);
    EXPECT_EQ(meta.RegionsMeta[159], expectedKazakhstanMeta);
    EXPECT_EQ(meta.RegionsMeta[207], expectedKyrgyzstanMeta);
    EXPECT_EQ(TVector<i32>(meta.CarrierIds.begin(), meta.CarrierIds.end()), (TVector<i32>{106, 107}));
    EXPECT_EQ(meta.RegionWithOptionsGroup, (THashSet<NMarket::NTypes::TRegionId>{159, 207}));
}

TEST(CollectGroups, PickupBucket) {
    const auto& applier = GetModifiersApplier();
    const i64 bucketId = 7;
    ASSERT_NO_THROW(applier.CollectGroups(bucketId, EBucketType::PICKUP));

    auto meta = applier.CollectGroups(bucketId, EBucketType::PICKUP);
    TRegionMeta expectedMoscowMeta{.OptionGroups = {{6, delivery_calc::mbi::NORMAL_OPTION}}};
    TRegionMeta expectedChernogolovkaMeta{.OptionGroups = {{7, delivery_calc::mbi::NORMAL_OPTION}}};
    EXPECT_EQ(meta.RegionsMeta.size(), 2);
    EXPECT_EQ(meta.RegionsMeta[213], expectedMoscowMeta);
    EXPECT_EQ(meta.RegionsMeta[219], expectedChernogolovkaMeta);
    EXPECT_EQ(TVector<i32>(meta.CarrierIds.begin(), meta.CarrierIds.end()), TVector<i32>{108});
    EXPECT_EQ(meta.RegionWithOptionsGroup, (THashSet<NMarket::NTypes::TRegionId>{213, 219}));
}

TEST(CollectGroups, OldPickupBucketTwoSameRegions) {
    const auto& applier = GetModifiersApplier();
    const i64 bucketId = 8;
    ASSERT_NO_THROW(applier.CollectGroups(bucketId, EBucketType::OLD_PICKUP));

    auto meta = applier.CollectGroups(bucketId, EBucketType::OLD_PICKUP);
    TRegionMeta expectedMeta{.OptionGroups = {{10, delivery_calc::mbi::NORMAL_OPTION}}};
    EXPECT_EQ(meta.RegionsMeta.size(), 1);
    EXPECT_EQ(meta.RegionsMeta[225], expectedMeta);
    EXPECT_EQ(TVector<i32>(meta.CarrierIds.begin(), meta.CarrierIds.end()), TVector<i32>{109});
    EXPECT_EQ(meta.RegionWithOptionsGroup, (THashSet<NMarket::NTypes::TRegionId>{225}));
}

TEST(CollectGroups, OldPickupBucketTwoSameRegionsDiffGroup) {
    const auto& applier = GetModifiersApplier();
    const i64 bucketId = 9;
    ASSERT_NO_THROW(applier.CollectGroups(bucketId, EBucketType::OLD_PICKUP));

    auto meta = applier.CollectGroups(bucketId, EBucketType::OLD_PICKUP);
    TRegionMeta expectedMeta{.OptionGroups = {
            {10, delivery_calc::mbi::NORMAL_OPTION},
            {11, delivery_calc::mbi::NORMAL_OPTION},
    }};
    EXPECT_EQ(meta.RegionsMeta.size(), 1);
    EXPECT_EQ(meta.RegionsMeta[225], expectedMeta);
    EXPECT_EQ(TVector<i32>(meta.CarrierIds.begin(), meta.CarrierIds.end()), TVector<i32>{109});
    EXPECT_EQ(meta.RegionWithOptionsGroup, (THashSet<NMarket::NTypes::TRegionId>{225}));
}
