#include <market/idx/feeds/qparser/tests_with_yt/yt_env.h>

#include <market/library/libyt/YtHelpers.h>
#include <market/idx/datacamp/proto/tables/offers_storage_schema.pb.h>
#include <market/idx/datacamp/controllers/lib/utils/yt_storage/schema.h>
#include <market/idx/feeds/qparser/lib/logger.h>
#include <yt/yt/core/misc/shutdown.h>
#include <util/system/env.h>

namespace NMarket {

Market::DataCamp::Offer TYtEnv::CreateServiceOffer(
    const NMarket::TFeedInfo& feedInfo,
    const TString& offerId,
    ui64 price,
    Market::DataCamp::MarketColor color
) {
    Market::DataCamp::Offer offer;
    offer.mutable_identifiers()->set_business_id(feedInfo.BusinessId);
    offer.mutable_identifiers()->set_offer_id(offerId);
    offer.mutable_identifiers()->set_shop_id(feedInfo.ShopId);
    offer.mutable_identifiers()->set_feed_id(feedInfo.FeedId);
    offer.mutable_identifiers()->set_warehouse_id(feedInfo.WarehouseId);
    offer.mutable_price()->mutable_basic()->mutable_binary_price()->set_price(price);
    offer.mutable_price()->mutable_basic()->mutable_meta()->mutable_timestamp()->set_seconds(1);
    offer.mutable_tech_info()->mutable_last_parsing()->mutable_meta()->mutable_timestamp()->set_seconds(1);
    offer.mutable_tech_info()->mutable_last_parsing()->mutable_feed_timestamp()->set_seconds(1);
    offer.mutable_meta()->set_rgb(color);
    offer.mutable_meta()->set_synthetic(feedInfo.IsSynthetic);
    (*offer.mutable_meta()->mutable_platforms())[color] = true;
    return offer;
}

void TYtEnv::Shutdown() {
    NYT::Shutdown();
}

TYtEnv::TYtEnv(bool isRealDataTest) {
    IsRealDataTest_ = isRealDataTest;
}

TYtEnv::~TYtEnv() {
    Client_.Reset();
}

void TYtEnv::Initialize() {
    TString ytProxy = GetEnv("YT_PROXY");
    INFO_LOG << "GetEnv(\"YT_PROXY\"): " << ytProxy << Endl;
    Client_ = NMarket::NYTHelper::CreateYtClient(ytProxy, "");
    auto attrs = NMarket::NDataCamp::CreateYtTableAttributes<Market::DataCamp::ServiceOffersTableRow>();
    attrs["tablet_cell_bundle"] = "default";
    attrs["primary_medium"] = "default";
    TString serviceTablePath = GetServiceTablePath();
    NMarket::NYTHelper::CreateYtTable(*Client_, serviceTablePath, attrs);
    NMarket::NYTHelper::MountTableAndWait(Client_, ServiceTablePath_);
}

TString TYtEnv::GetServiceTablePath() {
    return IsRealDataTest_ ? RealServiceTablePath_ : ServiceTablePath_;
}

void TYtEnv::InsertServiceOffers(const TVector<Market::DataCamp::Offer>& offers) {
    NYT::TNode::TListType nodes;
    for(auto& offer: offers) {
        nodes.emplace_back(NYT::TNode()
            ("business_id", offer.identifiers().business_id())
            ("shop_sku", offer.identifiers().offer_id())
            ("shop_id", offer.identifiers().shop_id())
            ("warehouse_id", offer.identifiers().warehouse_id())
            ("outlet_id", offer.identifiers().outlet_id())
            ("identifiers", offer.identifiers().SerializeAsStringOrThrow())
            ("price", offer.price().SerializeAsStringOrThrow())
            ("meta", offer.meta().SerializeAsStringOrThrow()));
    }
    Client_->InsertRows(ServiceTablePath_, nodes);
}

} // namespace
