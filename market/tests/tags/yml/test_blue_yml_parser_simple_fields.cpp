#include <market/idx/feeds/qparser/tests/tags/common_tests.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


TEST(BlueYmlParser, CommentLifeDays) {
    TestBlueSimpleTextYmlTag("comment-life-days", [](const TOfferCarrier& item){
        const auto& lifespan = item.GetOriginalSpecification().lifespan();
        return lifespan.has_service_life_comment() ? MakeMaybe<TString>(lifespan.service_life_comment()) : Nothing();
    });
}

TEST(BlueYmlParser, CommentValidityDays) {
    TestBlueSimpleTextYmlTag("comment-validity-days", [](const TOfferCarrier& item) {
      const auto& expiry = item.GetOriginalSpecification().expiry();
      return expiry.has_validity_comment() ? MakeMaybe<TString>(expiry.validity_comment()) : Nothing();
    });
}

TEST(BlueYmlParser, CommentWarranty) {
    TestBlueSimpleTextYmlTag("comment-warranty", [](const TOfferCarrier& item) {
      const auto& warranty = item.GetOriginalTerms().seller_warranty();
      return warranty.has_comment_warranty() ? MakeMaybe<TString>(warranty.comment_warranty())  : Nothing();
    });
}

TEST(BlueYmlParser, Quantum) {
    TestBlueSimpleIntYmlTag<ui32>("quantum", [](const TOfferCarrier& item) {
        auto terms = item.GetOriginalTerms();
        return terms.has_supply_quantity() && terms.supply_quantity().has_step() ? MakeMaybe<ui32>(terms.supply_quantity().step()) : Nothing();
    });
}

TEST(BlueYmlParser, MinDeliveryPiecies) {
    TestBlueSimpleIntYmlTag<ui32>("min-delivery-pieces", [](const TOfferCarrier& item) {
        auto terms = item.GetOriginalTerms();
        return terms.has_supply_quantity() && terms.supply_quantity().has_min() ? MakeMaybe<ui32>(terms.supply_quantity().min()) : Nothing();
    });
}

TEST(BlueYmlParser, TransportUnit) {
    TestBlueSimpleIntYmlTag<ui32>("transport-unit", [](const TOfferCarrier& item) {
        auto block = item.GetOriginalTerms().transport_unit_size();
        return block.has_value() ? MakeMaybe<ui32>(block.value()) : Nothing();
    });
}

TEST(BlueYmlParser, BoxCount) {
    TestBlueSimpleIntYmlTag<ui32>("box-count", [](const TOfferCarrier& item) {
      auto block = item.GetOriginalTerms().box_count();
      return block.has_value() ? MakeMaybe<ui32>(block.value()) : Nothing();
    });
}

TEST(BlueYmlParser, Leadtime) {
    TestBlueSimpleIntYmlTag<ui32>("leadtime", [](const TOfferCarrier& item) {
      auto block = item.GetOriginalTerms().partner_delivery_time();
      return block.has_value() ? MakeMaybe<ui32>(block.value()) : Nothing();
    });
}

TEST(BlueYmlParser, MarketSku) {
    TestBlueSimpleIntYmlTag<i64>("market-sku", [](const TOfferCarrier& item) {
        auto matching = item.DataCampOffer.content().binding().partner();
        return matching.has_market_sku_id() ? MakeMaybe<i64>(matching.market_sku_id()) : Nothing();
    });

    TestBlueSimpleIntYmlTag<i64>("market_sku", [](const TOfferCarrier& item) {
        auto matching = item.DataCampOffer.content().binding().partner();
        return matching.has_market_sku_id() ? MakeMaybe<i64>(matching.market_sku_id()) : Nothing();
    });
}

TEST(BlueYmlParser, AnimalProducts) {
    TestBlueFlagYmlTag("animal-products", [](const TOfferCarrier& item) {
        return item.GetOriginalSpecification().animal_products();
    });
}
