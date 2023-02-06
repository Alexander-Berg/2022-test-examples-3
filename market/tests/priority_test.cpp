#include "common.h"

#include <market/idx/delivery/bin/shop_delivery_options_builder/lib/pipeline.h>
#include <market/idx/delivery/bin/shop_delivery_options_builder/proto/options.pb.h>

#include <market/proto/indexer/GenerationLog.pb.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

/// Тест мержа с приоритетным регионом. На вход получаем 1 бакет с 2мя опциями (дешевая и быстрая)
/// для региона 213, а также 2 локальные опции для приоритетного региона (213) - тоже дешевую и быструю.
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
                            .PriceValue = 10000000000,
                            .DaysMin = 0,
                            .DaysMax = 0,
                            .OrderBeforeHour = 12,
                        },
                        {
                            .PriceValue = 1000000000,
                            .DaysMin = 2,
                            .DaysMax = 10,
                            .OrderBeforeHour = 12,
                        },
                    },
                },
            },
            .Buckets = {
                {
                    .BucketId = 1,
                    .CarrierIds = {1, 2},
                    .DeliveryProgram = Market::NDeliveryProgram::REGULAR_PROGRAM,
                    .TariffId = 123,
                },
            },
            .Regions = {
                {.BucketId = 1, .RegionId = 213, .GroupId = 1},
            }};
    }

    MarketIndexer::GenerationLog::Record InputData() {
        MarketIndexer::GenerationLog::Record record;
        record.set_shop_id(31184);
        record.add_delivery_bucket_ids(1);
        record.set_delivery_currency("RUR");
        record.set_use_yml_delivery(true);
        auto cheapOption = record.add_offer_delivery_options();
        cheapOption->SetCost(50);
        cheapOption->SetDaysMin(1);
        cheapOption->SetDaysMax(10);
        cheapOption->SetOrderBeforeHour(12);
        auto fastOption = record.add_offer_delivery_options();
        fastOption->SetCost(500);
        fastOption->SetDaysMin(1);
        fastOption->SetDaysMax(5);
        fastOption->SetOrderBeforeHour(12);
        return record;
    }

    TVector<NYT::TNode> ExpectedData() {
        return {
            NYT::TNode()
                ("shop_id", 31184u)
                ("region_id", 213u)
                ("currency_id", 3u)
                ("carrier_ids", NYT::TNode().Add(1u).Add(2u).Add(99u))
                ("option_type", 0u)
                ("cheapest_option", NYT::TNode()
                    ("price", 500000000u)
                    ("day_from", 1u)
                    ("day_to", 10u)
                    ("order_before", 12u))
                ("fastest_option", NYT::TNode()
                    ("price", 10000000000u)
                    ("day_from", 0u)
                    ("day_to", 0u)
                    ("order_before", 12u)),
        };
    }
}

TEST(ShopDeliveryOptionsBuilder, Priority) {
    auto client = NYT::NTesting::CreateTestClient();
    auto options = PrepareOptions(client, RegionalDeliveryData());

    constexpr auto GENLOG_TABLE_PATH = "//priority/genlog_table";
    CreateSchematizedTable(client, GENLOG_TABLE_PATH, {InputData()});
    options.AddGenlogTable(GENLOG_TABLE_PATH);

    Market::Geo geo = LoadGeo();
    NMarket::NShopDelivery::RunMRPipeline(client, options, std::move(geo));

    auto data = NYT::NTesting::ReadTable(client, options.GetShopDeliveryOptionsTable());
    EXPECT_EQ(data, ExpectedData());
}
