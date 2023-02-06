#include <market/library/libyt/YtHelpers.h>
#include <market/library/delivery_calc/bucket_uploader.h>
#include <market/library/delivery_calc/delivery_calc.h>
#include <market/proto/delivery/delivery_yt/indexer_part.pb.h>

#include <library/cpp/testing/unittest/registar.h>

#include <mapreduce/yt/interface/client.h>
#include <mapreduce/yt/interface/serialize.h>

#include <util/system/env.h>
#include <util/string/join.h>


Y_UNIT_TEST_SUITE(TestBucketUploader)
{
    bool HasSchemaKeyInList(const NYT::TNode::TListType& list, const TString& key) {
        for (const auto& column : list) {
            if (column.AsMap().at("name") == key) {
                return true;
            }
        }

        return false;
    }

    Y_UNIT_TEST(TestCreateTable)
    {
        // тест на создания реплицированная таблицы для хранения бакетов
        TString ytProxy = GetEnv("YT_PROXY");

        auto client = NYT::CreateClient(ytProxy);
        NMarket::NYTHelper::WaitGoodTabletCell(client);

        TString replicatedTablePath = "//home/replicated_table";
        TString syncReplicaPath = "//home/replica_sync";
        TString asyncReplicaPath = "//home/replica_async";

        auto options =
            NDeliveryCalc::TDeliveryBucketUploaderOptions()
                .YtMetaProxy(ytProxy)
                .YtPrimaryProxy(ytProxy)
                .YtReserveProxy(ytProxy)
                .YtMetaTablepath(replicatedTablePath)
                .YtPrimaryTablepath(syncReplicaPath)
                .YtReserveTablepath(asyncReplicaPath);

        NDeliveryCalc::TDeliveryBucketUploader uploader(options);

        UNIT_ASSERT(client->Exists(replicatedTablePath));
        UNIT_ASSERT(client->Exists(syncReplicaPath));
        UNIT_ASSERT(client->Exists(asyncReplicaPath));

        auto schema = client->Get(replicatedTablePath + "/@schema");
        UNIT_ASSERT(schema.IsList());

        auto schemaList = schema.AsList();
        UNIT_ASSERT(HasSchemaKeyInList(schemaList, "bucket_id"));
        UNIT_ASSERT(HasSchemaKeyInList(schemaList, "bucket"));
        UNIT_ASSERT(HasSchemaKeyInList(schemaList, "options_groups"));
        UNIT_ASSERT(HasSchemaKeyInList(schemaList, "delivery_options_group"));
    }

    Y_UNIT_TEST(TestInsertBuckets)
    {
        // Тест записи бакетов в ыть
        // так как локальный ыть не поддерживает реплицированные таблицы, то создаем обычную динтаблицу, вне класса
        // и внутри будем смотреть, что если по такому пути таблица есть, то создавать не будем

        TString ytProxy = GetEnv("YT_PROXY");

        auto client = NYT::CreateClient(ytProxy);
        NMarket::NYTHelper::WaitGoodTabletCell(client);

        TString replicatedTablePath = "//home/dummy_replicated_table";
        TString syncReplicaPath = "//dummypath";
        TString asyncReplicaPath = "//dummypath";

        auto options =
                NDeliveryCalc::TDeliveryBucketUploaderOptions()
                        .YtMetaProxy(ytProxy)
                        .YtPrimaryProxy(ytProxy)
                        .YtReserveProxy(ytProxy)
                        .YtMetaTablepath(replicatedTablePath);

        // создаем вначале динтаблица, чтобы была возможность писать в нее в тестах
        NMarket::NYTHelper::CreateYtTable(*client, replicatedTablePath, NDeliveryCalc::GetDeliveryBucketsAttributes());
        NMarket::NYTHelper::MountTableAndWait(client, replicatedTablePath);
        NDeliveryCalc::TDeliveryBucketUploader uploader(options);

        auto generationID = 5555;
        auto bucketID = 111;

        NDeliveryCalc::TFeedDeliveryOptionsResponse deliveryOptionsResponse;

        deliveryOptionsResponse.set_generation_id(generationID);

        auto& deliveryOptByFeed = *deliveryOptionsResponse.mutable_delivery_options_by_feed();
        auto& deliveryOptionBuckets = *deliveryOptByFeed.add_delivery_option_buckets();

        deliveryOptionBuckets.set_delivery_opt_bucket_id(bucketID);
        deliveryOptionBuckets.set_currency("RUR");
        deliveryOptionBuckets.set_program(delivery_calc::mbi::REGULAR_PROGRAM);
        deliveryOptionBuckets.add_carrier_ids(100);

        auto& deliveryOptionGroupRegs = *deliveryOptionBuckets.add_delivery_option_group_regs();
        deliveryOptionGroupRegs.set_region(213);
        deliveryOptionGroupRegs.set_delivery_opt_group_id(50);
        deliveryOptionGroupRegs.set_option_type(delivery_calc::mbi::NORMAL_OPTION);

        auto& deliveryOptionGroups = *deliveryOptByFeed.add_delivery_option_groups();
        deliveryOptionGroups.set_delivery_option_group_id(50);

        auto& deliveryOptinon = *deliveryOptionGroups.add_delivery_options();
        deliveryOptinon.set_delivery_cost(1000);
        deliveryOptinon.set_min_days_count(1);
        deliveryOptinon.set_max_days_count(2);
        deliveryOptinon.set_order_before(13);

        deliveryOptionGroups.add_payment_types(delivery_calc::mbi::CASH_ON_DELIVERY);

        uploader.UploadBucketsToYT(deliveryOptionsResponse);

        TString bucketIdKey = ToString(NMarket::NDeliveryCalc::DELIVERY_CALC_COMMON_GENERATION) + "_" + ToString(bucketID);

        auto lookupResult = NMarket::NYTHelper::LookupRows(client, replicatedTablePath, NYT::TNode().Add(NYT::TNode()("bucket_id", bucketIdKey)).AsList(), NYT::TLookupRowsOptions(), TDuration::Seconds(15));
        UNIT_ASSERT_EQUAL(1, lookupResult.size());

        delivery_calc::mbi::CommonDeliveryOptionsBucket commonBucket;
        Y_PROTOBUF_SUPPRESS_NODISCARD commonBucket.ParseFromString(lookupResult[0].AsMap()["bucket"].AsString());

        UNIT_ASSERT_EQUAL(commonBucket.delivery_opt_bucket_id(), bucketID);
        UNIT_ASSERT_EQUAL(commonBucket.program(), delivery_calc::mbi::REGULAR_PROGRAM);
        UNIT_ASSERT_EQUAL(commonBucket.generation_id(), generationID);
        UNIT_ASSERT_EQUAL(commonBucket.currency(), "RUR");

        UNIT_ASSERT_EQUAL(commonBucket.carrier_ids().size(), 1);
        UNIT_ASSERT_EQUAL(commonBucket.carrier_ids(0), 100);

        UNIT_ASSERT_EQUAL(commonBucket.delivery_option_group_regs().size(), 1);
        UNIT_ASSERT_EQUAL(commonBucket.delivery_option_group_regs(0).region(), 213);
        UNIT_ASSERT_EQUAL(commonBucket.delivery_option_group_regs(0).option_type(), delivery_calc::mbi::NORMAL_OPTION);


        delivery_calc::mbi::DeliveryOptionsGroupsForBucket deliveryOptionsGroupsForBucket;
        Y_PROTOBUF_SUPPRESS_NODISCARD deliveryOptionsGroupsForBucket.ParseFromString(lookupResult[0].AsMap()["options_groups"].AsString());

        UNIT_ASSERT_EQUAL(deliveryOptionsGroupsForBucket.delivery_option_groups().size(), 1);
        const auto& group = deliveryOptionsGroupsForBucket.delivery_option_groups(0);
        UNIT_ASSERT_EQUAL(group.delivery_option_group_id(), 50);

        UNIT_ASSERT_EQUAL(group.delivery_options().size(), 1);
        UNIT_ASSERT_EQUAL(group.delivery_options(0).delivery_cost(), 1000);
        UNIT_ASSERT_EQUAL(group.delivery_options(0).min_days_count(), 1);
        UNIT_ASSERT_EQUAL(group.delivery_options(0).max_days_count(), 2);
        UNIT_ASSERT_EQUAL(group.delivery_options(0).order_before(), 13);

        UNIT_ASSERT_EQUAL(group.payment_types().size(), 1);
        UNIT_ASSERT_EQUAL(group.payment_types(0), delivery_calc::mbi::CASH_ON_DELIVERY);

        delivery_calc::mbi::DeliveryOptionsGroup deliveryOptionsGroup;
        Y_PROTOBUF_SUPPRESS_NODISCARD deliveryOptionsGroup.ParseFromString(lookupResult[0].AsMap()["delivery_options_group"].AsString());

        UNIT_ASSERT_EQUAL(deliveryOptionsGroup.delivery_option_group_id(), 50);

        UNIT_ASSERT_EQUAL(deliveryOptionsGroup.delivery_options().size(), 1);
        UNIT_ASSERT_EQUAL(deliveryOptionsGroup.delivery_options(0).delivery_cost(), 1000);
        UNIT_ASSERT_EQUAL(deliveryOptionsGroup.delivery_options(0).min_days_count(), 1);
        UNIT_ASSERT_EQUAL(deliveryOptionsGroup.delivery_options(0).max_days_count(), 2);
        UNIT_ASSERT_EQUAL(deliveryOptionsGroup.delivery_options(0).order_before(), 13);

        UNIT_ASSERT_EQUAL(deliveryOptionsGroup.payment_types().size(), 1);
        UNIT_ASSERT_EQUAL(deliveryOptionsGroup.payment_types(0), delivery_calc::mbi::CASH_ON_DELIVERY);
    }
};
