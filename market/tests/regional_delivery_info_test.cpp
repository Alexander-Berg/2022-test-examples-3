#include "common.h"

#include <market/idx/delivery/bin/shop_delivery_options_builder/lib/pipeline.h>
#include <market/idx/delivery/bin/shop_delivery_options_builder/proto/options.pb.h>

#include <market/proto/indexer/GenerationLog.pb.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

/// Тест с двумя офферами. Различие в том, что у одного заполнено offers_delivery_info.
/// Ожидается, что для него будут взяты региональные опции из offers_delivery_info, даже если они будут хуже.

using ::delivery_calc::mbi::BucketInfo;
using ::delivery_calc::mbi::DeliveryOption;
using ::delivery_calc::mbi::DeliveryOptionsBucket;
using ::delivery_calc::mbi::DeliveryOptionsGroup;
using ::delivery_calc::mbi::DeliveryOptionsGroupRegion;
using ::delivery_calc::mbi::OffersDeliveryInfo;
using ::delivery_calc::mbi::OptionType;
using ::MarketIndexer::GenerationLog::Record;

namespace {
    TVector<DeliveryOptionsGroup> OptionGroups() {
        DeliveryOptionsGroup optionsGroup;
        {
            optionsGroup.set_delivery_option_group_id(27);

            DeliveryOption* firstDeliveryOption = optionsGroup.add_delivery_options();
            firstDeliveryOption->set_delivery_cost(10000);
            firstDeliveryOption->set_min_days_count(5);
            firstDeliveryOption->set_max_days_count(5);
            firstDeliveryOption->set_order_before(10);

            DeliveryOption* secondDeliveryOption = optionsGroup.add_delivery_options();
            secondDeliveryOption->set_delivery_cost(9000);
            secondDeliveryOption->set_min_days_count(6);
            secondDeliveryOption->set_max_days_count(6);
            secondDeliveryOption->set_order_before(10);
        }

        return {
            optionsGroup,
        };
    }

    TVector<DeliveryOptionsBucket> CourierBuckets() {
        DeliveryOptionsBucket courierBucket;
        {
            courierBucket.set_delivery_opt_bucket_id(1011);
            courierBucket.set_currency("RUR");
            courierBucket.add_carrier_ids(5);
            courierBucket.add_carrier_ids(6);
            DeliveryOptionsGroupRegion* groupRegion = courierBucket.add_delivery_option_group_regs();
            groupRegion->set_region(217);
            groupRegion->set_delivery_opt_group_id(27);
            groupRegion->set_option_type(OptionType::NORMAL_OPTION);
        }

        return {
            courierBucket,
        };
    }

    TData RegionalDeliveryData() {
        return TData{
            .Precision = 7,
            .Groups = {
                {
                    .GroupId = 1,
                    .CurrencyId = 3,
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
                    .CurrencyId = 3,
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
                {.BucketId = 1, .RegionId = 213, .GroupId = 1},
                {.BucketId = 2, .RegionId = 213, .GroupId = 2},
            }};
    }

    TVector<Record> InputData() {
        Record record;
        record.set_shop_id(31184);
        record.add_delivery_bucket_ids(1);
        record.add_delivery_bucket_ids(2);
        record.set_delivery_currency("RUR");
        record.set_delivery_flag(true);

        Record recordWithOffersRegionalDelivery;
        recordWithOffersRegionalDelivery.CopyFrom(record);

        OffersDeliveryInfo* deliveryInfo = recordWithOffersRegionalDelivery.mutable_offers_delivery_info();
        BucketInfo* bucketInfo = deliveryInfo->add_courier_buckets_info();
        bucketInfo->set_bucket_id(1011);

        return {
            record,
            recordWithOffersRegionalDelivery,
        };
    }

    TVector<NYT::TNode> ExpectedData() {
        return {
            NYT::TNode()
                ("shop_id", 31184u)
                ("region_id", 213u)
                ("currency_id", 3u)
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
            NYT::TNode()
                ("shop_id", 31184u)
                ("region_id", 217u)
                ("currency_id", 3u)
                ("carrier_ids", NYT::TNode().Add(5u).Add(6u))
                ("option_type", 0u)
                ("cheapest_option", NYT::TNode()
                    ("price", 900000000u)
                    ("day_from", 6u)
                    ("day_to", 6u)
                    ("order_before", 10u))
                ("fastest_option", NYT::TNode()
                    ("price", 1000000000u)
                    ("day_from", 5u)
                    ("day_to", 5u)
                    ("order_before", 10u)),
        };
    }
}

TEST(ShopDeliveryOptionsBuilder, RegionalDeliveryInfo) {
    auto client = NYT::NTesting::CreateTestClient();
    auto options = PrepareOptions(client, RegionalDeliveryData(), OptionGroups(), CourierBuckets());

    constexpr auto GENLOG_TABLE_PATH = "//offers_delivery_info/genlog_table";
    CreateSchematizedTable(client, GENLOG_TABLE_PATH, InputData());
    options.AddGenlogTable(GENLOG_TABLE_PATH);

    Market::Geo geo = LoadGeo();
    NMarket::NShopDelivery::RunMRPipeline(client, options, std::move(geo));

    auto data = NYT::NTesting::ReadTable(client, options.GetShopDeliveryOptionsTable());
    Sort(data, [](const NYT::TNode& lhs, const NYT::TNode& rhs) {
        return std::forward_as_tuple(lhs["shop_id"].AsUint64(), lhs["region_id"].AsUint64())
             < std::forward_as_tuple(rhs["shop_id"].AsUint64(), rhs["region_id"].AsUint64());
    });
    EXPECT_EQ(data, ExpectedData());
}
