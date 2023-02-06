#include "common.h"

#include <market/idx/delivery/bin/shop_delivery_options_builder/lib/pipeline.h>

#include <market/proto/indexer/GenerationLog.pb.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

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
                            .DaysMin = 4,
                            .DaysMax = 4,
                            .OrderBeforeHour = 12,
                        },
                        {
                            .PriceValue = 100,
                            .DaysMin = 2,
                            .DaysMax = 10,
                            .OrderBeforeHour = 12,
                        },
                        {
                            .PriceValue = 100,
                            .DaysMin = 2,
                            .DaysMax = 10,
                            .OrderBeforeHour = 13,
                        },
                        {
                            .PriceValue = 100,
                            .DaysMin = 2,
                            .DaysMax = 11,
                            .OrderBeforeHour = 12,
                        }},
                },
                {
                    .GroupId = 2,
                    .CurrencyId = 1,
                    .Options = {{
                                    .PriceValue = 500,
                                    .DaysMin = 4,
                                    .DaysMax = 4,
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
                    .DeliveryProgram = Market::NDeliveryProgram::MARKET_DELIVERY_PROGRAM,
                    .TariffId = 456,
                },
            },
            .Regions = {
                {.BucketId = 1, .RegionId = 1, .GroupId = 1},
                {.BucketId = 2, .RegionId = 1, .GroupId = 2},
            }};
    }

    MarketIndexer::GenerationLog::Record InputData() {
        MarketIndexer::GenerationLog::Record record;
        record.set_shop_id(31184);
        record.add_delivery_bucket_ids(1);
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
                    ("day_from", 4u)
                    ("day_to", 4u)
                    ("order_before", 12u)),
        };
    }
}

TEST(ShopDeliveryOptionsBuilder, Buckets) {
    auto client = NYT::NTesting::CreateTestClient();
    auto options = PrepareOptions(client, RegionalDeliveryData());

    constexpr auto GENLOG_TABLE_PATH = "//buckets/genlog_table";
    CreateSchematizedTable(client, GENLOG_TABLE_PATH, {InputData()});
    options.AddGenlogTable(GENLOG_TABLE_PATH);

    Market::Geo geo = LoadGeo();
    NMarket::NShopDelivery::RunMRPipeline(client, options, std::move(geo));

    auto data = NYT::NTesting::ReadTable(client, options.GetShopDeliveryOptionsTable());
    EXPECT_EQ(data, ExpectedData());
}
