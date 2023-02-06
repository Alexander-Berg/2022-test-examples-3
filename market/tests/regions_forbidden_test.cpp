#include "common.h"

#include <market/idx/delivery/bin/shop_delivery_options_builder/lib/pipeline.h>
#include <market/idx/delivery/bin/shop_delivery_options_builder/proto/options.pb.h>

#include <market/proto/indexer/GenerationLog.pb.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

/// Сложный тест на мерж дерева регионов.
/// На вход подается 2 бакета в разных файлах. В первом бакете 3 региона (2, 3, 225), но один
/// запрещен; во втором 2 региона (3, 225).
/// На выходе запрещенный перетирается доставкой из родителя в другом бакете.
/// Мерж только по цене, времена доставки везде одинаковые.

/// В виде дерева:
///     225 (100р)         255 (50р)          255 (50р)
///       /  \               /  \               /  \
///      /    \      +      /    \      =      /    \
///     /      \           /      \           /      \
///  3 (25р)  forbid   3 (80р)  empty     3 (25р)  empty

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
                            .PriceValue = 100,
                            .DaysMin = 10,
                            .DaysMax = 10,
                            .OrderBeforeHour = 12,
                        },
                    },
                },
                {
                    .GroupId = 3,
                    .CurrencyId = 1,
                    .Options = {
                        {
                            .PriceValue = 25,
                            .DaysMin = 10,
                            .DaysMax = 10,
                            .OrderBeforeHour = 12,
                        },
                    },
                },
                {
                    .GroupId = 4,
                    .CurrencyId = 1,
                    .Options = {
                        {
                            .PriceValue = 50,
                            .DaysMin = 10,
                            .DaysMax = 10,
                            .OrderBeforeHour = 12,
                        },
                    },
                },
                {
                    .GroupId = 5,
                    .CurrencyId = 1,
                    .Options = {
                        {
                            .PriceValue = 80,
                            .DaysMin = 10,
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
                {
                    .BucketId = 2,
                    .CarrierIds = {3, 4},
                    .DeliveryProgram = Market::NDeliveryProgram::REGULAR_PROGRAM,
                    .TariffId = 456,
                },
            },
            .Regions = {
                {.BucketId = 1, .RegionId = 225, .GroupId = 1},
                {.BucketId = 1, .RegionId = 2, .GroupId = 0xFFFFFFFF},
                {.BucketId = 1, .RegionId = 3, .GroupId = 3},
                {.BucketId = 2, .RegionId = 225, .GroupId = 4},
                {.BucketId = 2, .RegionId = 3, .GroupId = 5},
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
                ("region_id", 225u)
                ("currency_id", 1u)
                ("carrier_ids", NYT::TNode().Add(1u).Add(2u).Add(3u).Add(4u))
                ("option_type", 0u)
                ("cheapest_option", NYT::TNode()
                    ("price", 50u)
                    ("day_from", 10u)
                    ("day_to", 10u)
                    ("order_before", 12u))
                ("fastest_option", NYT::TNode()
                    ("price", 50u)
                    ("day_from", 10u)
                    ("day_to", 10u)
                    ("order_before", 12u)),
            NYT::TNode()
                ("shop_id", 31184u)
                ("region_id", 3u)
                ("currency_id", 1u)
                ("carrier_ids", NYT::TNode().Add(1u).Add(2u).Add(3u).Add(4u))
                ("option_type", 0u)
                ("cheapest_option", NYT::TNode()
                    ("price", 25u)
                    ("day_from", 10u)
                    ("day_to", 10u)
                    ("order_before", 12u))
                ("fastest_option", NYT::TNode()
                    ("price", 25u)
                    ("day_from", 10u)
                    ("day_to", 10u)
                    ("order_before", 12u)),
        };
    }
}

TEST(ShopDeliveryOptionsBuilder, RegionsForbidden) {
    auto client = NYT::NTesting::CreateTestClient();
    auto options = PrepareOptions(client, RegionalDeliveryData());

    constexpr auto GENLOG_FIRST_TABLE_PATH = "//regions_forbidden/genlog_table_1";
    constexpr auto GENLOG_SECOND_TABLE_PATH = "//regions_forbidden/genlog_table_2";
    CreateSchematizedTable(client, GENLOG_FIRST_TABLE_PATH, {InputData1()});
    options.AddGenlogTable(GENLOG_FIRST_TABLE_PATH);
    CreateSchematizedTable(client, GENLOG_SECOND_TABLE_PATH, {InputData2()});
    options.AddGenlogTable(GENLOG_SECOND_TABLE_PATH);

    Market::Geo geo = LoadGeo();
    NMarket::NShopDelivery::RunMRPipeline(client, options, std::move(geo));

    auto data = NYT::NTesting::ReadTable(client, options.GetShopDeliveryOptionsTable());
    EXPECT_EQ(data, ExpectedData());
}
