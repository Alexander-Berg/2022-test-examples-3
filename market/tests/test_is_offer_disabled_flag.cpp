#include <market/idx/feeds/qparser/inc/feed_info.h>
#include <market/idx/feeds/qparser/lib/flags/disabled.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;
using NMarket::NFlags::IsOfferDisabled;

TOfferCarrier DefaultProcessedOffer() {
    // задаем все поля в оффере, во избежание неоднозначностей
    TOfferCarrier offer(TFeedInfo{});
    offer.DataCampOffer.mutable_identifiers()->set_offer_id("test-offer-id");
    offer.DataCampOffer.mutable_content()->mutable_binding()->mutable_partner()->set_market_sku_id(1234);
    offer.RurPrice = 300.0;
    auto* price = offer.DataCampOffer.mutable_price()->mutable_basic()->mutable_binary_price();
    *price = NMarketIndexer::Common::PriceExpression();
    price->set_price(300);
    offer.IsDisabled = false;
    return offer;
}

TEST(IsOfferDisabledFlag, OfferEnabled) {
    auto offer = DefaultProcessedOffer();
    ASSERT_FALSE(IsOfferDisabled(offer));
}

TEST(IsOfferDisabledFlag, OfferDisabledByDisabledFlag) {
    auto offer = DefaultProcessedOffer();
    offer.IsDisabled = true;
    ASSERT_TRUE(IsOfferDisabled(offer));
}
