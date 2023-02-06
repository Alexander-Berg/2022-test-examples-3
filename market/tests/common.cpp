#include "common.h"

#include <market/library/mmap_versioned/file_mapping.h>
#include <market/library/regional_delivery_mms/reader.h>
#include <market/library/regional_delivery_mms/writer.h>
#include <market/library/flat_guards/flatbuffers_guard.h>
#include <market/flat/indexer/DeliveryModifier.fbs.h>

#include <library/cpp/testing/unittest/env.h>

#include <mapreduce/yt/util/ypath_join.h>

#include <util/generic/size_literals.h>
#include <util/generic/string.h>
#include <util/stream/file.h>
#include <util/stream/zlib.h>

#include <string>

using namespace ::Market::NDelivery;
using namespace ::NMarket::NShopDelivery;
using ::MarketIndexer::GenerationLog::Record;
using ::delivery_calc::mbi::DeliveryOptionsGroup;
using ::delivery_calc::mbi::DeliveryOptionsBucket;

namespace {
    void Write(const char* path, const TData& data) {
        const auto writer = CreateRegionalDeliveryInfoWriter();

        writer->SetPrecision(data.Precision);

        for (const auto& group : data.Groups) {
            writer->AddOptionGroup(
                group.GroupId,
                group.CurrencyId,
                TRawDeliveryOptions(group.Options.begin(), group.Options.end()),
                NMarket::NPaymentMethods::METHOD_UNKNOWN);
        }

        for (const auto& bucket : data.Buckets) {
            writer->AddBucket(
                bucket.BucketId,
                TVector<TCarrierId>(bucket.CarrierIds.begin(), bucket.CarrierIds.end()),
                bucket.DeliveryProgram,
                bucket.TariffId,
                Market::NDelivery::NULL_DELIVERY_CALC_BUCKET_ID);
        }

        for (const auto& region : data.Regions) {
            writer->AddRegionToBucket(region.BucketId, region.RegionId, region.GroupId);
        }

        TStringStream intermediate;
        writer->Write(intermediate);

        TFileOutput file(path);
        TZLibCompress out(&file, ZLib::ZLib, 9, 16_MB);
        THolder<Market::NDelivery::IRegionalDeliveryInfoReader> regionalDelivery;

        regionalDelivery = Market::NDelivery::CreateRegionalDeliveryInfoReader(
            Market::NMmap::IMemoryRegion::LoadFromStream(intermediate));
        Save(regionalDelivery.Get(), out);

        out.Flush();
        out.Finish();
    }

    i64 GetId(const DeliveryOptionsGroup& group) {
        return group.delivery_option_group_id();
    }

    i64 GetId(const DeliveryOptionsBucket& bucket) {
        return bucket.delivery_opt_bucket_id();
    }

    template <typename TDataType>
    void CreateDeliveryTable(NYT::IClientPtr client, const NYT::TYPath& tablePath, const TVector<TDataType>& data) {
        TVector<NYT::TNode> nodes;
        for (const auto& item : data) {
            nodes.emplace_back(NYT::TNode()("id", GetId(item))("data", item.SerializeAsStringOrThrow()));
        }

        CreateTable(client, tablePath, nodes);
    }
}

TShopDeliveryBuilderOptions PrepareOptions(const NYT::IClientPtr& client,
                                           const TData& data,
                                           const TVector<DeliveryOptionsGroup>& a,
                                           const TVector<DeliveryOptionsBucket>& b) {
    auto path = GetOutputPath() / "regional_delivery.gz";

    Write(path.c_str(), data);

    flatbuffers::FlatBufferBuilder builder;
    builder.Finish(
        CreateDeliveryModifierVec(builder),
        DeliveryModifierVecIdentifier());
    auto emptyModifiers = builder.Release();
    TFixedBufferFileOutput modifiersFile(JoinFsPaths(GetOutputPath(), "regional_delivery_modifiers.fb"));
    modifiersFile.Write(emptyModifiers.data(), emptyModifiers.size());

    TFixedBufferFileOutput modifierIndicesFile(JoinFsPaths(GetOutputPath(), "modifier_indices.csv"));

    TShopDeliveryBuilderOptions options;
    options.SetRegionalDeliveryPath(path);
    options.SetCurrencyRatesPath(
        JoinFsPaths(
            ArcadiaSourceRoot(),
            "market/library/currency_exchange/ut/data/currency_rates_byn.xml"));
    options.SetShopsDatPath(
        JoinFsPaths(
            ArcadiaSourceRoot(),
            "market/idx/delivery/bin/shop_delivery_options_builder/tests/stubs/shops-utf8.dat.report.generated"));

    auto testDir = NYT::NTesting::CreateTestDirectory(client);
    CreateDeliveryTable(client, NYT::JoinYPaths(testDir, "options_groups"), a);
    CreateDeliveryTable(client, NYT::JoinYPaths(testDir, "courier_buckets"), b);

    options.SetIntermediateTable(NYT::JoinYPaths(testDir, "intermediate_table"));
    options.SetShopDeliveryOptionsTable(NYT::JoinYPaths(testDir, "result"));
    options.SetModifiersPath(JoinFsPaths(GetOutputPath(), "regional_delivery_modifiers.fb"));
    options.SetModifiersIndicesPath(JoinFsPaths(GetOutputPath(), "modifier_indices.csv"));
    options.SetOptionsGroupsTable(NYT::JoinYPaths(testDir, "options_groups"));
    options.SetCourierBucketsTable(NYT::JoinYPaths(testDir, "courier_buckets"));
    options.SetUseOffersDeliveryInfo(true);

    return options;
}

Market::Geo LoadGeo() {
    Market::Geo geo;
    geo.loadTree(
        JoinFsPaths(
            ArcadiaSourceRoot(),
            "market/idx/delivery/bin/shop_delivery_options_builder/tests/stubs/geo.c2p")
            .c_str());
    geo.loadInfo(
        JoinFsPaths(
            ArcadiaSourceRoot(),
            "market/idx/delivery/bin/shop_delivery_options_builder/tests/stubs/geobase.xml")
            .c_str());
    return geo;
}

void CreateTable(NYT::IClientPtr client, const NYT::TYPath& tablePath, const TVector<NYT::TNode>& data) {
    client->Create(tablePath, NYT::NT_TABLE, NYT::TCreateOptions().Recursive(true).IgnoreExisting(true));
    auto writer = client->CreateTableWriter<NYT::TNode>(tablePath);
    for (const auto& node : data) {
        writer->AddRow(node);
    }
}

void CreateSchematizedTable(NYT::IClientPtr client, const NYT::TYPath& tablePath, const TVector<Record>& data) {
    client->CreateTable<Record>(tablePath, {}, NYT::TCreateOptions().Recursive(true).IgnoreExisting(true));
    auto writer = client->CreateTableWriter<MarketIndexer::GenerationLog::Record>(tablePath);
    for (const auto& record : data) {
        writer->AddRow(record);
    }
}

