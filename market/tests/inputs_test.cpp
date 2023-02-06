#include "common.h"

#include <market/idx/delivery/bin/shop_delivery_options_builder/lib/pipeline.h>
#include <market/idx/delivery/bin/shop_delivery_options_builder/proto/options.pb.h>

#include <market/proto/indexer/GenerationLog.pb.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

/// Тест на мерж двух бакетов из разных таблиц. На вход получаем 2 бакета с 2мя опциями в каждом.
/// На выходе получаем 2 опции - самую дешевую и самую быструю из 4х опций.

namespace {
    TData RegionalDeliveryData() {
        return TData{
            .Precision = 7,
            .Groups = {
                {
                    .GroupId = 1,
                    .CurrencyId = 1,
                    .Options = {
                        {
                            .PriceValue = 1000,
                            .DaysMin = 2,
                            .DaysMax = 2,
                            .OrderBeforeHour = 12,
                        },
                        {
                            .PriceValue = 100,
                            .DaysMin = 2,
                            .DaysMax = 10,
                            .OrderBeforeHour = 12,
                        },
                    },
                },
                {
                    .GroupId = 2,
                    .CurrencyId = 1,
                    .Options = {{
                                    .PriceValue = 500,
                                    .DaysMin = 1,
                                    .DaysMax = 1,
                                    .OrderBeforeHour = 12,
                                },
                                {
                                    .PriceValue = 250,
                                    .DaysMin = 2,
                                    .DaysMax = 5,
                                    .OrderBeforeHour = 12,
                                }},
                },
            },
            .Buckets = {
                {
                    .BucketId = 1,
                    .CarrierIds = {1, 2},
                    .DeliveryProgram = Market::NDeliveryProgram::REGULAR_PROGRAM,
                    .TariffId = 123,
                },
                {
                    .BucketId = 2,
                    .CarrierIds = {3, 4},
                    .DeliveryProgram = Market::NDeliveryProgram::REGULAR_PROGRAM,
                    .TariffId = 456,
                },
            },
            .Regions = {
                {.BucketId = 1, .RegionId = 1, .GroupId = 1},
                {.BucketId = 2, .RegionId = 1, .GroupId = 2},
            }};
    }

    MarketIndexer::GenerationLog::Record InputData1() {
        MarketIndexer::GenerationLog::Record record;
        record.set_shop_id(31184);
        record.add_delivery_bucket_ids(1);
        record.set_delivery_currency("RUR");
        return record;
    }

    MarketIndexer::GenerationLog::Record InputData2() {
        MarketIndexer::GenerationLog::Record record;
        record.set_shop_id(31184);
        record.add_delivery_bucket_ids(2);
        record.set_delivery_currency("RUR");
        return record;
    }

    TVector<NYT::TNode> ExpectedData() {
        return {
            NYT::TNode()
                ("shop_id", 31184u)
                ("region_id", 1u)
                ("currency_id", 1u)
                ("carrier_ids", NYT::TNode().Add(1u).Add(2u).Add(3u).Add(4u))
                ("option_type", 0u)
                ("cheapest_option", NYT::TNode()
                    ("price", 100u)
                    ("day_from", 2u)
                    ("day_to", 10u)
                    ("order_before", 12u))
                ("fastest_option", NYT::TNode()
                    ("price", 500u)
                    ("day_from", 1u)
                    ("day_to", 1u)
                    ("order_before", 12u)),
        };
    }
}

TEST(ShopDeliveryOptionsBuilder, Inputs) {
    auto client = NYT::NTesting::CreateTestClient();
    auto options = PrepareOptions(client, RegionalDeliveryData());

    constexpr auto GENLOG_FIRST_TABLE_PATH = "//inputs/genlog_table_1";
    constexpr auto GENLOG_SECOND_TABLE_PATH = "//inputs/genlog_table_2";
    CreateSchematizedTable(client, GENLOG_FIRST_TABLE_PATH, {InputData1()});
    options.AddGenlogTable(GENLOG_FIRST_TABLE_PATH);
    CreateSchematizedTable(client, GENLOG_SECOND_TABLE_PATH, {InputData2()});
    options.AddGenlogTable(GENLOG_SECOND_TABLE_PATH);

    Market::Geo geo = LoadGeo();
    NMarket::NShopDelivery::RunMRPipeline(client, options, std::move(geo));

    auto data = NYT::NTesting::ReadTable(client, options.GetShopDeliveryOptionsTable());
    EXPECT_EQ(data, ExpectedData());
}
