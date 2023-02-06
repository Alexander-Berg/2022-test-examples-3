#include <market/idx/delivery/lib/buckets/buckets.h>
#include <market/library/offers_common/Geo.h>
#include <market/idx/offers/lib/iworkers/OfferCtx.h>
#include <market/idx/offers/lib/iworkers/DeliveryWorker.h>
#include <market/proto/delivery/region_cache.pb.h>
#include <market/proto/feedparser/OffersData.pb.h>

#include <vector>
#include <string>
#include <fstream>
#include <limits>
#include <tuple>
#include <functional>

#include <library/cpp/streams/zstd/zstd.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <util/folder/path.h>
#include <util/generic/size_literals.h>
#include <util/generic/string.h>
#include <util/generic/variant.h>
#include <util/string/vector.h>
#include <util/system/fstat.h>
#include <util/stream/zlib.h>

using namespace NDelivery;


void AssertNormalBucketInfo(const delivery_calc::mbi::BucketInfo& bucketInfo, bool isNew) {
    ASSERT_EQ(bucketInfo.bucket_id(), 1005);
    ASSERT_EQ(bucketInfo.is_new(), isNew);

    ASSERT_EQ(bucketInfo.cost_modifiers_ids().size(), 2);
    ASSERT_EQ(bucketInfo.cost_modifiers_ids(0), 1003);
    ASSERT_EQ(bucketInfo.cost_modifiers_ids(1), 1005);

    ASSERT_EQ(bucketInfo.time_modifiers_ids().size(), 1);
    ASSERT_EQ(bucketInfo.time_modifiers_ids(0), 1001);
}

std::function<void(const TGlRecord&, const TOfferCtx&)> GetAssertFunc(bool hasIncompleteDelivery, bool isNew) {
    return [hasIncompleteDelivery, isNew](const TGlRecord& glRecord,  const TOfferCtx& offerContext) {
        const auto &deliveryInfo = glRecord.offers_delivery_info_renumerated();
        ASSERT_EQ(deliveryInfo.courier_buckets_info().size(), 1);
        ASSERT_EQ(deliveryInfo.pickup_buckets_info().size(), 0);
        ASSERT_EQ(deliveryInfo.post_buckets_info().size(), 1);

        AssertNormalBucketInfo(deliveryInfo.courier_buckets_info(0), isNew);
        AssertNormalBucketInfo(deliveryInfo.post_buckets_info(0), isNew);
        ASSERT_EQ(offerContext.HasIncompleteDeliveryInfo, hasIncompleteDelivery);
    };
}

// Подготовить данные о доставки с бакетами которые не будут выброшены в процессе перенумерации
std::tuple<delivery_calc::mbi::OffersDeliveryInfo, TIdConversionMap, TIdConversionMap> PrepareDeliveryInfoGood() {
    using namespace delivery_calc::mbi;

    // I. Заполняем информацию о доставке
    OffersDeliveryInfo di;

    // Бакет, для идентификаторов которого есть замена (в рамках перенумерации).
    BucketInfo normal_bi;
    normal_bi.set_bucket_id(5);
    normal_bi.add_cost_modifiers_ids(3);
    normal_bi.add_cost_modifiers_ids(5);
    normal_bi.add_time_modifiers_ids(1);
    normal_bi.set_is_new(false);

    // II. Карта конвертации id бакетов и модификаторов.
    TIdConversionMap bucketIds;
    bucketIds[5] = 1005;
    TIdConversionMap modifierIds;
    modifierIds[3] = 1003;
    modifierIds[5] = 1005;
    modifierIds[1] = 1001;

    *di.add_courier_buckets_info() = normal_bi;
    *di.add_post_buckets_info() = normal_bi;

    return {di, bucketIds, modifierIds};
}

// Подготовить данные о доставки с бакетами, которые будут выброшены в процессе перенумерации.
std::tuple<delivery_calc::mbi::OffersDeliveryInfo, TIdConversionMap, TIdConversionMap> PrepareDeliveryInfoIncomplete() {
    using namespace delivery_calc::mbi;

    // I. Заполняем информацию о доставке
    OffersDeliveryInfo di;

    // 1. Бакет, для идентификатора которого нет замены.
    BucketInfo no_bucket_id_bi;
    no_bucket_id_bi.set_bucket_id(6);
    no_bucket_id_bi.add_cost_modifiers_ids(7);

    // 2. Бакет, для идентификаторов которого есть замена (в рамках перенумерации).
    BucketInfo normal_bi;
    normal_bi.set_bucket_id(5);
    normal_bi.add_cost_modifiers_ids(3);
    normal_bi.add_cost_modifiers_ids(5);
    normal_bi.add_time_modifiers_ids(1);
    normal_bi.set_is_new(true);

    // 3. Бакет у которого для одного модификатора нет замены.
    BucketInfo no_modifier_id_bi;
    no_modifier_id_bi.set_bucket_id(7);
    no_modifier_id_bi.add_cost_modifiers_ids(8);

    *di.add_courier_buckets_info() = no_bucket_id_bi;
    *di.add_courier_buckets_info() = normal_bi;
    *di.add_courier_buckets_info() = no_modifier_id_bi;

    *di.add_pickup_buckets_info() = no_bucket_id_bi;

    *di.add_post_buckets_info() = normal_bi;

    // II. Карта конвертации id бакетов и модификаторов.
    TIdConversionMap bucketIds;
    bucketIds[5] = 1005;
    bucketIds[7] = 1007;
    TIdConversionMap modifierIds;
    modifierIds[7] = 1007;
    modifierIds[3] = 1003;
    modifierIds[5] = 1005;
    modifierIds[1] = 1001;

    return {di, bucketIds, modifierIds};
}

void FillDeliveryInfoRenumberedTestCore(const delivery_calc::mbi::OffersDeliveryInfo& offersDeliveryInfo,
                                        const TIdConversionMap& bucketIds,
                                        const TIdConversionMap& modifierIds,
                                        std::function<void(const TGlRecord &, const TOfferCtx& offerContext)> assertFunc) {
    const std::string GeoPath(SRC_("geobase/geo.c2p"));
    GEO.loadTree(GeoPath.c_str());

    TGlRecord glRecord;
    TOfferCtx offerContext;

    glRecord.SetDeliveryCalcGeneration(1000);
    // I. Заполняем информацию о доставке

    *glRecord.mutable_offers_delivery_info() = offersDeliveryInfo;

    TIdConversionMap flatCourierIds;
    auto worker = MakeDeliveryWorker(
        bucketIds,
        modifierIds,
        flatCourierIds,
        MakeHolder<TRegionsCache>());

    worker->ProcessOffer(&glRecord, &offerContext);

    assertFunc(glRecord, offerContext);
}

TEST(TDeliveryWorker, FillDeliveryInfoRenumberedIncompleteDelivery)
{
    auto [offersDeliveryInfo, bucketIds, modifierIds] = PrepareDeliveryInfoIncomplete();
    FillDeliveryInfoRenumberedTestCore(offersDeliveryInfo, bucketIds, modifierIds, GetAssertFunc(true, true));
}

TEST(TDeliveryWorker, FillDeliveryInfoRenumberedFullDelivery)
{
    auto [offersDeliveryInfo, bucketIds, modifierIds] = PrepareDeliveryInfoGood();
    FillDeliveryInfoRenumberedTestCore(offersDeliveryInfo, bucketIds, modifierIds, GetAssertFunc(false, false));
}

TEST(TDeliveryWorker, ProcessFlatCourierBuckets)
{
    using namespace delivery_calc::mbi;
    const std::string GeoPath(SRC_("geobase/geo.c2p"));
    GEO.loadTree(GeoPath.c_str());

    TGlRecord glRecord;
    TOfferCtx offerContext;

    glRecord.SetDeliveryCalcGeneration(1000);

    OffersDeliveryInfo offersDeliveryInfo;
    // 1. бакет только с mmap курьеркой
    BucketInfo* withoutFlatIds = offersDeliveryInfo.add_courier_buckets_info();
    withoutFlatIds->set_bucket_id(1002);

    // бакет с mmap и flatbuffer курьеркой
    BucketInfo* withBothIds = offersDeliveryInfo.add_courier_buckets_info();
    withBothIds->set_bucket_id(1004);
    withBothIds->set_is_new(false);

    // бакет только с flatbuffer курьеркой
    BucketInfo* withOnlyFlatIds = offersDeliveryInfo.add_courier_buckets_info();
    withOnlyFlatIds->set_bucket_id(1006);
    withOnlyFlatIds->set_is_new(true);

    *glRecord.mutable_offers_delivery_info() = offersDeliveryInfo;

    TIdConversionMap bucketIds {
        {1002, 2},
        {1004, 4}
    };
    TIdConversionMap modifierIds;
    TIdConversionMap flatCourierIds {
        {1004, 40},
        {1006, 60},
    };
    auto worker = MakeDeliveryWorker(
        bucketIds,
        modifierIds,
        flatCourierIds,
        MakeHolder<TRegionsCache>());

    worker->ProcessOffer(&glRecord, &offerContext);

    const auto &renumberedDeliveryInfo = glRecord.offers_delivery_info_renumerated();

    ASSERT_EQ(renumberedDeliveryInfo.courier_buckets_info().size(), 3);
    ASSERT_EQ(renumberedDeliveryInfo.pickup_buckets_info().size(), 0);
    ASSERT_EQ(renumberedDeliveryInfo.post_buckets_info().size(), 0);
    ASSERT_EQ(offerContext.HasIncompleteDeliveryInfo, false);

    ASSERT_EQ(renumberedDeliveryInfo.courier_buckets_info(0).bucket_id(), 2);
    ASSERT_EQ(renumberedDeliveryInfo.courier_buckets_info(0).is_new(), false);
    ASSERT_EQ(renumberedDeliveryInfo.courier_buckets_info(1).bucket_id(), 40);
    ASSERT_EQ(renumberedDeliveryInfo.courier_buckets_info(1).is_new(), false);
    ASSERT_EQ(renumberedDeliveryInfo.courier_buckets_info(2).bucket_id(), 60);
    ASSERT_EQ(renumberedDeliveryInfo.courier_buckets_info(2).is_new(), true);
}

THolder<TRegionsCache> GetRegionCache() {
    using NMarket::NDelivery::ERegionCacheRecordType;

    TRegionsCache cache;
    const TString resource = "data/region_cache_with_modifiers.pb.zstd";
    TFileInput in(SRC_(resource));

    TZstdDecompress compressed(&in, 8_MB);
    ui32 size;
    ::Load(&compressed, size);

    THashMap<ERegionCacheRecordType, std::variant<TRegionsCache::TCache*, TRegionsCache::TCacheOld*>> recTypeToCache{
        {ERegionCacheRecordType::COURIER_OLD, &cache.CourierRegionsOld},
        {ERegionCacheRecordType::PICKUP_OLD, &cache.PickupRegionsOld},
        {ERegionCacheRecordType::POST_OLD, &cache.PostRegionsOld},
        {ERegionCacheRecordType::COURIER, &cache.CourierRegionsWithModifiers},
        {ERegionCacheRecordType::PICKUP, &cache.PickupRegionsWithModifiers},
        {ERegionCacheRecordType::POST, &cache.PostRegionsWithModifiers}
    };
    for (ui32 i = 0; i < size; i++) {
        TString str;
        ::Load(&compressed, str);
        NMarket::NDelivery::TRegionCacheRecord record;
        record.ParseFromStringOrThrow(str);

        bool oldBucketType = record.GetType() == ERegionCacheRecordType::COURIER_OLD ||
            record.GetType() == ERegionCacheRecordType::PICKUP_OLD ||
            record.GetType() == ERegionCacheRecordType::POST_OLD;

        const auto& regions = record.GetRegions();
        TRegionsList regionList(regions.cbegin(), regions.cend());
        const TRegionsList* uniqueRegionList = cache.MakeUniqueRegionList(regionList);
        if (oldBucketType) {
            const auto& buckets = record.GetBuckets();
            (*std::get<TRegionsCache::TCacheOld*>(recTypeToCache[record.GetType()]))[record.GetOutletType()].emplace(
                buckets,
                uniqueRegionList);
        } else {
            const auto& bucketInfos = record.GetBucketInfos().GetBucketInfos();
            (*std::get<TRegionsCache::TCache*>(recTypeToCache[record.GetType()]))[record.GetOutletType()].emplace(
                bucketInfos,
                uniqueRegionList);
        }
    }
    return MakeHolder<TRegionsCache>(std::move(cache));
}

void CreateRegionCacheTestOffer(bool withModifiedBucketIds, bool withRegionModifiers, TGlRecord& glRecord, TOfferCtx&) {
    glRecord.SetDeliveryCalcGeneration(1000);
    if (withModifiedBucketIds) {
        glRecord.add_pickup_bucket_ids(40);
        glRecord.add_pickup_bucket_ids(60);
    }
    delivery_calc::mbi::BucketInfo* bucketInfo = glRecord.mutable_offers_delivery_info()->add_pickup_buckets_info();
    bucketInfo->set_bucket_id(10004);
    bucketInfo = glRecord.mutable_offers_delivery_info()->add_pickup_buckets_info();
    bucketInfo->set_bucket_id(10006);
    if (withRegionModifiers) {
        bucketInfo->add_region_availability_modifiers_ids(2006);
    }
}

/*
 * region_cache_without_modifiers contents:
 * [
 *  {
 *      bucket_ids:     [40, 60],
 *      regions:        [213, 120999],
 *      type:           PICKUP_OLD,
 *      bucket_infos:   []
 *  },
 *  {
 *      bucket_ids:     [40, 60],
 *      regions:        [213, 120992],
 *      type:           PICKUP,
 *      bucket_infos:   [...]
 *  }
 * ]
 *
 * YQL-scripts to prepare input YT-tables for the cache:
 * - bucket_vectors     https://yql.yandex-team.ru/Operations/YA_6GNK3DImprUcTpE-i1lI2kL7cmF-RtKMyIzU7lhw=
 */
TEST(TDeliveryWorker, UseRegionCache) {
    const std::string GeoPath(SRC_("geobase/geo.c2p"));
    GEO.loadTree(GeoPath.c_str());

    TGlRecord glRecord;
    TOfferCtx offerContext;

    CreateRegionCacheTestOffer(true, false, glRecord, offerContext);
    TIdConversionMap bucketIds;
    TIdConversionMap modifierIds;
    TIdConversionMap flatCourierIds;
    NDelivery::AppendBucketIndices(SRC_("data/modifier_indices.csv"), modifierIds, true);
    NDelivery::AppendBucketIndices(SRC_("data/pickup_options_bucket_indices.csv"), flatCourierIds, true);

    auto worker = MakeDeliveryWorker(
        bucketIds,
        modifierIds,
        flatCourierIds,
        GetRegionCache());

    worker->ProcessOffer(&glRecord, &offerContext);

    ASSERT_EQ(offerContext.PickupAndPostRegions.size(), 2);
    const auto& regions_0 = offerContext.PickupAndPostRegions[0].get();
    const auto& regions_1 = offerContext.PickupAndPostRegions[1].get();
    ASSERT_TRUE(Find(regions_0, 213) != regions_0.end());
    ASSERT_TRUE(Find(regions_0, 120992) != regions_0.end());
    ASSERT_TRUE(Find(regions_1, 120999) != regions_1.end());

    bool allConverted = true;
    allConverted &= true;
    ASSERT_TRUE(allConverted);
}

/*
 * region_cache_with_modifiers contents:
 * [
 *  {
 *      bucket_ids:     [40, 60],
 *      type:           PICKUP_OLD,
 *      bucket_infos:   [],
 *      regions:        [213, 120999],
 *  },
 *  {
 *      bucket_ids:     [40, 60],
 *      type:           PICKUP,
 *      bucket_infos:   [
 *          {bucket_id: 10004, 'region_availability_modifiers_ids': [], ...},
 *          {bucket_id: 10006, 'region_availability_modifiers_ids': [], ...}
 *      ]
 *      regions:        [213, 120007, 120992],
 *  },
 *  {
 *      bucket_ids:     [40, 60],
 *      type:           PICKUP,
 *      bucket_infos:   [
 *          {bucket_id: 10004, 'region_availability_modifiers_ids': [], ...},
 *          {bucket_id: 10006, 'region_availability_modifiers_ids': [2006], ...}
 *      ]
 *      regions:        [120007, 120992],
 *  }
 * ]
 *
 * YQL-script templates to prepare input YT-tables for the cache:
 * - bucket_vectors     https://yql.yandex-team.ru/Operations/YBEtVlJ2-XQdAyk1A7rh8QbBdBNKpwIuwCQRlatnqoU=
 * - options_groups     https://yql.yandex-team.ru/Operations/YBAmbSyLNR8WsyPgD5DJk7TkKC7E99Y-RnyZHCPV8ok=
 * - courier_buckets    https://yql.yandex-team.ru/Operations/YBAo0AuEI0WippDny5v46LhNEPXOcEM79tPlxqFwmm0=
 * - pickup_buckets     https://yql.yandex-team.ru/Operations/YBAtDguEI0WippMQUjZD3AcfgeGQCxi0Xh4fvPMfDGY=
 * - old_pickup_buckets https://yql.yandex-team.ru/Operations/YBAutFJ2-ecfVgSyBAGpkHzltVRl9iqQc4OuG8dxd9A=
 * - old_post_buckets   https://yql.yandex-team.ru/Operations/YBAvX_MBwynr7melJvRxu0hrenRMakRdjnEETd7UwLA=
 */
TEST(TDeliveryWorker, UseRegionCacheWithModifiers) {
    const std::string GeoPath(SRC_("geobase/geo.c2p"));
    GEO.loadTree(GeoPath.c_str());

    TGlRecord glRecord, glRecordModified;
    TOfferCtx offerContext, offerContextModified;


    CreateRegionCacheTestOffer(false, false, glRecord, offerContext);
    CreateRegionCacheTestOffer(false, true, glRecordModified, offerContextModified);

    TIdConversionMap bucketIds;
    TIdConversionMap modifierIds;
    TIdConversionMap flatCourierIds;
    NDelivery::AppendBucketIndices(SRC_("data/modifier_indices.csv"), modifierIds, true);
    NDelivery::AppendBucketIndices(SRC_("data/pickup_options_bucket_indices.csv"), flatCourierIds, true);

    auto worker = MakeDeliveryWorker(
        bucketIds,
        modifierIds,
        flatCourierIds,
        GetRegionCache());

    worker->ProcessOffer(&glRecord, &offerContext);
    worker->ProcessOffer(&glRecordModified, &offerContextModified);

    const auto& regions = offerContext.PickupAndPostRegions[0].get();
    ASSERT_EQ(regions.size(), 3);
    ASSERT_TRUE(Find(regions, 213) != regions.end());
    ASSERT_TRUE(Find(regions, 120007) != regions.end());
    ASSERT_TRUE(Find(regions, 120992) != regions.end());

    const auto& regionsMod = offerContextModified.PickupAndPostRegions[0].get();
    ASSERT_EQ(regionsMod.size(), 2);
    ASSERT_TRUE(Find(regionsMod, 120007) != regionsMod.end());
    ASSERT_TRUE(Find(regionsMod, 120992) != regionsMod.end());
}
