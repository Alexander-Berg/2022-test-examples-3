#pragma once

#include <market/library/libpromo/matcher/promo_matcher.h>


struct TParams {
    TString YtServerName;
    TString YtTokenPath;

    TString TablePromo;
    TString TableTovarTree;
    TString TableGenlog;

    TString CategoryTreeFile;
    TString ShopsDatFile;

    TMaybe<TString> PromoKey;

    TVector<TString> WareMd5s;

    TMaybe<TPromoMatcher::TCategoryId> Hid;
    TMaybe<TPromoMatcher::TMsku> Msku;
    TMaybe<NMarket::NPromo::TFeedOfferId> FeedOfferId;
    TMaybe<TPromoMatcher::TShopId> ShopId;
    TMaybe<TPromoMatcher::TVendorId> VendorId;
    TMaybe<TPromoMatcher::TVendorId> FeedId;
    TMaybe<TPromoMatcher::TWarehouseId> WarehouseId;
};
