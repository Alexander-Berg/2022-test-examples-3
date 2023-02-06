#include "promo_matcher_tester.h"

#include <market/library/libpromo/matcher/promo_matcher.h>
#include <market/library/libshopsdat/shopsdat.h>
#include <market/proto/feedparser/Promo.pb.h>

#include <market/library/libyt/YtHelpers.h>
#include <util/string/join.h>
#include <market/library/libpromo/common.h>
#include <library/cpp/logger/global/global.h>


using TRecord = NMarket::NShopsDat::TShopsDatRecord<NMarket::NShopsDat::TWarehouseId, NMarket::NShopsDat::TIsDsbs>;

class TShopsDatLoader : public NMarket::NShopsDat::IShopsDatParser<TRecord>
{
public:
    TShopsDatLoader(TVector<TPromoMatcher::TShopInfo>& shops, const THashSet<TPromoMatcher::TWarehouseId>& expressWarehouses)
        : Shops_(shops)
        , ExpressWarehouses_(expressWarehouses)
    {
    }

private:
    void Process(TRecord &shop) final
    {
        Shops_.push_back({
            .FeedId = shop.FeedId,
            .WarehouseId = shop.WarehouseId,
            .IsExpress = shop.WarehouseId && ExpressWarehouses_.contains(*shop.WarehouseId),
            .IsDsbs = shop.IsDsbs,
        });
    }

private:
    TVector<TPromoMatcher::TShopInfo>& Shops_;
    const THashSet<TPromoMatcher::TWarehouseId>& ExpressWarehouses_;
};

TVector<TPromoMatcher::TShopInfo> ProcessShopsDat(const TString& shopsDatPath, const THashSet<TPromoMatcher::TWarehouseId>& expressWarehouses) {
    TVector<TPromoMatcher::TShopInfo> shops;
    TShopsDatLoader loader(shops, expressWarehouses);
    loader.Parse(shopsDatPath);
    return shops;
}

TPromoMatcherTester::TPromoMatcherTester(const TParams& params)
    : Params(params)
    , YtClient(NYT::CreateClient(Params.YtServerName, Params.YtTokenPath ? NYT::TCreateClientOptions().TokenPath(Params.YtTokenPath) : NYT::TCreateClientOptions()))
    , CategoryTree(Market::CreateCategoryTreeFromPackedProtoFile(params.CategoryTreeFile))
    , PromoMatcher(MakeHolder<TPromoMatcher>())
    , WareMd5s(params.WareMd5s.begin(), params.WareMd5s.end())
{
    PromoMatcher->SetCategoryTree(&*CategoryTree);
    PromoMatcher->LoadShopsData(ProcessShopsDat(params.ShopsDatFile, {}));

    uint32_t counter = 0;
    for (auto reader = YtClient->CreateTableReader<NYT::TNode>(params.TablePromo); reader->IsValid(); reader->Next()) {
        auto&& row = reader->MoveRow();
        auto&& promoKey = row["promo_key"].AsString();
        if (params.PromoKey ? promoKey == *(params.PromoKey) : true) {
            Market::Promo::PromoDetails promoDetailsProto;
            Y_PROTOBUF_SUPPRESS_NODISCARD promoDetailsProto.ParseFromString(row["promo"].AsString());
            PromoMatcher->PreloadPromoDetails(promoDetailsProto, promoKey);
        }
        counter += 1;
    }
    INFO_LOG << "Loaded " << counter << " promos" << Endl;
}

TPromoMatcherTester::~TPromoMatcherTester()
{
}

void TPromoMatcherTester::Run() {
    if (Params.TableGenlog) {
        NYT::TRichYPath source;
        source.Path(Params.TableGenlog).Columns({"feed_id", "shop_id", "offer_id", "ware_md5", "category_id", "vendor_id", "market_sku", "warehouse_id"});

        uint32_t counter = 0;
        THashSet<TString> matchedPromos;
        for (auto reader = YtClient->CreateTableReader<NYT::TNode>(source); reader->IsValid(); reader->Next()) {
            auto&& row = reader->MoveRow();
            if (!WareMd5s.empty() && !row["ware_md5"].IsNull() && !WareMd5s.contains(row["ware_md5"].AsString())) {
                continue;
            }

            TPromoMatcher::TOfferInfo offerInfo {
                .Category = row["category_id"].IsNull() ? Nothing() : TMaybe<TPromoMatcher::TCategoryId>(row["category_id"].AsUint64()),
                .Msku = row["market_sku"].IsNull() ? Nothing() : TMaybe<TPromoMatcher::TMsku>(row["market_sku"].AsUint64()),
                .FeedOfferId = NMarket::NPromo::TFeedOfferId{uint32_t(row["feed_id"].AsUint64()), row["offer_id"].AsString()},
                .ShopId = row["shop_id"].AsUint64(),
                .VendorId = row["vendor_id"].IsNull() ? Nothing() : TMaybe<TPromoMatcher::TVendorId>(row["vendor_id"].AsUint64()),
                .FeedId = row["feed_id"].AsUint64(),
                .WarehouseId = row["warehouse_id"].IsNull() ? Nothing() : TMaybe<TPromoMatcher::TWarehouseId>(row["warehouse_id"].AsUint64()),
            };

            auto funcAcceptMatchedPromo = [&](const TPromoMatcher::TPromoInfo& promoInfo) {
                INFO_LOG << "Offer " << row["ware_md5"].AsString() << " matched with promo " << promoInfo.Key << " " << Endl;
            };

            PromoMatcher->MatchPromos<false>(offerInfo, funcAcceptMatchedPromo);

            counter += 1;
            if (counter % 100000 == 0) INFO_LOG << counter << Endl;
        }
        INFO_LOG << counter << " offers" << Endl;
    } else {
        auto funcAcceptMatchedPromo = [](const TPromoMatcher::TPromoInfo& promoInfo) {
            INFO_LOG << "Promo " << promoInfo.Key << " matched" << Endl;
        };

            TPromoMatcher::TOfferInfo offerInfo {
                .Category = Params.Hid,
                .Msku = Params.Msku,
                .FeedOfferId = Params.FeedOfferId,
                .ShopId = Params.ShopId,
                .VendorId = Params.VendorId,
                .FeedId = Params.FeedId,
                .WarehouseId = Params.WarehouseId,
            };

        INFO_LOG << "matcher-1 {" << Endl;
        PromoMatcher->MatchPromos<false>(offerInfo, funcAcceptMatchedPromo);
        INFO_LOG << "matcher-1 }" << Endl;

    }
}
