#pragma once

#include <market/idx/delivery/bin/shop_delivery_options_builder/proto/options.pb.h>

#include <market/proto/indexer/GenerationLog.pb.h>

#include <market/library/geo/Geo.h>
#include <market/library/regional_delivery_mms/common.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

struct TOptionGroupInfo {
    Market::NDelivery::TOptionGroupId GroupId;
    ui8 CurrencyId;
    TVector<Market::NDelivery::TRawDeliveryOption> Options;
};

struct TBucketInfo {
    Market::NDelivery::TBucketId BucketId;
    TVector<i32> CarrierIds;
    Market::NDeliveryProgram::TDeliveryProgram DeliveryProgram;
    Market::NDelivery::TTariffId TariffId;
};

struct TRegionInfo {
    Market::NDelivery::TBucketId BucketId;
    Market::TRegionId RegionId;
    Market::NDelivery::TOptionGroupId GroupId;
};

struct TData {
    ui8 Precision{0};
    TVector<TOptionGroupInfo> Groups;
    TVector<TBucketInfo> Buckets;
    TVector<TRegionInfo> Regions;
};

::NMarket::NShopDelivery::TShopDeliveryBuilderOptions PrepareOptions(const NYT::IClientPtr& client,
                                                                     const TData& data,
                                                                     const TVector<delivery_calc::mbi::DeliveryOptionsGroup>& a = {},
                                                                     const TVector<delivery_calc::mbi::DeliveryOptionsBucket>& b = {});

Market::Geo LoadGeo();

void CreateTable(NYT::IClientPtr client, const NYT::TYPath& tablePath, const TVector<NYT::TNode>& data);
void CreateSchematizedTable(NYT::IClientPtr client,
                            const NYT::TYPath& tablePath,
                            const TVector<MarketIndexer::GenerationLog::Record>& data);
