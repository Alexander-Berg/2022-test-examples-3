#include <market/idx/offers/lib/iworkers/pickup_worker.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>


TEST(TPickupWorker, AddLocalRegion)
{
    // Тест проверяет, что если пришли pickupOptions из фида, то в geo_regions добавиться локальный регион
    // даже если до этого регионов не было


    MarketIndexer::GenerationLog::Record glRecord;
    TOfferCtx offerContext;

    glRecord.set_priority_regions("1");
    offerContext.PriorityRegions.insert(1);

    NMarketIndexer::Common::TPickupOption pickupOption;
    pickupOption.SetCost(100);
    pickupOption.SetDaysMin(1);
    pickupOption.SetDaysMax(1);
    pickupOption.SetOrderBeforeHour(13);

    glRecord.add_pickup_options()->CopyFrom(pickupOption);

    auto worker = MakePickupWorker();
    worker->ProcessOffer(&glRecord, &offerContext);

    // для geo_regions добавляем домашний регион, если пришли pickupOption из фида
    ASSERT_EQ(glRecord.int_geo_regions()[0], 1);
    ASSERT_STREQ(glRecord.priority_regions(), "1");
}


TEST(TPickupWorker, AddLocalRegionToOther) {
    // Тест проверяет, что если пришли pickupOptions из фида, то в geo_regions добавиться локальный регион

    MarketIndexer::GenerationLog::Record glRecord;
    TOfferCtx offerContext;

    glRecord.add_int_geo_regions(12345);
    glRecord.set_priority_regions("1");
    offerContext.PriorityRegions.insert(1);

    NMarketIndexer::Common::TPickupOption pickupOption;
    pickupOption.SetCost(100);
    pickupOption.SetDaysMin(1);
    pickupOption.SetDaysMax(1);
    pickupOption.SetOrderBeforeHour(13);

    glRecord.add_pickup_options()->CopyFrom(pickupOption);

    auto worker = MakePickupWorker();
    worker->ProcessOffer(&glRecord, &offerContext);

    // для geo_regions добавляем домашний регион, если пришли pickupOption из фида
    ASSERT_EQ(glRecord.int_geo_regions().size(), 2);
    EXPECT_EQ(glRecord.int_geo_regions()[0], 1);
    EXPECT_EQ(glRecord.int_geo_regions()[1], 12345);
}


TEST(TPickupWorker, AddLocalRegionToExisting) {
    // Тест проверяет, что если пришли pickupOptions из фида, но в geo_regions уже есть локальный регион,
    // то он не будет добавлен снова

    MarketIndexer::GenerationLog::Record glRecord;
    TOfferCtx offerContext;

    glRecord.add_int_geo_regions(1);
    glRecord.add_int_geo_regions(12345);
    glRecord.set_priority_regions("1");
    offerContext.PriorityRegions.insert(1);

    NMarketIndexer::Common::TPickupOption pickupOption;
    pickupOption.SetCost(100);
    pickupOption.SetDaysMin(1);
    pickupOption.SetDaysMax(1);
    pickupOption.SetOrderBeforeHour(13);

    glRecord.add_pickup_options()->CopyFrom(pickupOption);

    auto worker = MakePickupWorker();
    worker->ProcessOffer(&glRecord, &offerContext);

    ASSERT_EQ(glRecord.int_geo_regions().size(), 2);
    EXPECT_EQ(glRecord.int_geo_regions()[0], 1);
    ASSERT_EQ(glRecord.int_geo_regions()[1], 12345);
}
