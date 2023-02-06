#include <market/idx/feeds/qparser/tests/tags/common_tests.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


TEST(BlueCsvParser, CommentLifeDays) {
    TestBlueSimpleTextCsvTag("comment_life_days", [](const TOfferCarrier& item){
      const auto& lifespan = item.GetOriginalSpecification().lifespan();
      return lifespan.has_service_life_comment() ? MakeMaybe<TString>(lifespan.service_life_comment()) : Nothing();
    });
}

TEST(BlueCsvParser, CommentValidityDays) {
    TestBlueSimpleTextCsvTag("comment_validity_days", [](const TOfferCarrier& item) {
      const auto& expiry = item.GetOriginalSpecification().expiry();
      return expiry.has_validity_comment() ? MakeMaybe<TString>(expiry.validity_comment()) : Nothing();
    });
}

TEST(BlueCsvParser, CommentWarranty) {
    TestBlueSimpleTextCsvTag("comment_warranty", [](const TOfferCarrier& item) {
      const auto& warranty = item.GetOriginalTerms().seller_warranty();
      return warranty.has_comment_warranty() ? MakeMaybe<TString>(warranty.comment_warranty()) : Nothing();
    });
}

TEST(BlueCsvParser, SingleCertificate) {
    TestSimpleListTextCsvTag(
        "certificate",
        [](const TOfferCarrier& item) { return item.GetOriginalSpecification().certificates();},
        "RU Д-TR.НВ32.В.00164/19",
        "RU Д-TR.НВ32.В.00164/19"
    );
}

TEST(BlueCsvParser, MultipleCertificate) {
    TestSimpleListTextCsvTag(
        "certificate",
        [](const TOfferCarrier& item) { return item.GetOriginalSpecification().certificates();},
        "RU Д-TR.НВ32.В.00164/19,RU Д-FR.АИ33.В.07037",
        "RU Д-TR.НВ32.В.00164/19, RU Д-FR.АИ33.В.07037"
    );
}

TEST(BlueCsvParser, Quantum) {
    TestBlueSimpleIntCsvTag<ui32>("quantum", [](const TOfferCarrier& item) {
      auto terms = item.GetOriginalTerms();
      return terms.has_supply_quantity() && terms.supply_quantity().has_step() ? MakeMaybe<ui32>(terms.supply_quantity().step()) : Nothing();
    });
}

TEST(BlueCsvParser, MinDeliveryPiecies) {
    TestBlueSimpleIntCsvTag<ui32>("min_delivery_pieces", [](const TOfferCarrier& item) {
      auto terms = item.GetOriginalTerms();
      return terms.has_supply_quantity() && terms.supply_quantity().has_min() ? MakeMaybe<ui32>(terms.supply_quantity().min()) : Nothing();
    });
}

TEST(BlueCsvParser, TransportUnit) {
    TestBlueSimpleIntCsvTag<ui32>("transport_unit", [](const TOfferCarrier& item) {
      auto block = item.GetOriginalTerms().transport_unit_size();
      return block.has_value() ? MakeMaybe<ui32>(block.value()) : Nothing();
    });
}

TEST(BlueCsvParser, BoxCount) {
    TestBlueSimpleIntCsvTag<ui32>("box_count", [](const TOfferCarrier& item) {
      auto block = item.GetOriginalTerms().box_count();
      return block.has_value() ? MakeMaybe<ui32>(block.value()) : Nothing();
    });
}

TEST(BlueCsvParser, Leadtime) {
    TestBlueSimpleIntCsvTag<ui32>("leadtime", [](const TOfferCarrier& item) {
      auto block = item.GetOriginalTerms().partner_delivery_time();
      return block.has_value() ? MakeMaybe<ui32>(block.value()) : Nothing();
    });
}

TEST(BlueCsvParser, SingleMercuryGUID) {
    auto mercuryGetter = [](const TOfferCarrier& item) {
        return item.GetOriginalSpecification().mercury_guid();
    };
    TestSimpleListTextCsvTag(
        "mercury-guid",
        mercuryGetter,
        "f5e08ec7-7a56-e311-8719-0025906126df",
        "f5e08ec7-7a56-e311-8719-0025906126df");
}

TEST(BlueCsvParser, ManyMercuryGUIDs) {
    auto mercuryGetter = [](const TOfferCarrier& item) {
        return item.GetOriginalSpecification().mercury_guid();
    };
    TestSimpleListTextCsvTag(
        "mercury-guid",
        mercuryGetter,
        "f5e08ec7-7a56-e311-8719-0025906126df,f5e08ec7-7a56-e311-8719-0025906126df",
        "f5e08ec7-7a56-e311-8719-0025906126df, f5e08ec7-7a56-e311-8719-0025906126df");
}

TEST(BlueCsvParser, SingleTnVedCode) {
    auto tnVedCodeGetter = [](const TOfferCarrier& item) {
        return item.GetOriginalSpecification().tn_ved_code();
    };
    TestSimpleListTextCsvTag("tn-ved-code", tnVedCodeGetter, "123", "123");
}

TEST(BlueCsvParser, AnimalProducts) {
    TestBlueFlagCsvTag("animal-products", [](const TOfferCarrier& item) {
      return item.GetOriginalSpecification().animal_products();
    });
}

TEST(BlueCsvParser, ManyTnVedCodes) {
    auto tnVedCodeGetter = [](const TOfferCarrier& item) {
        return item.GetOriginalSpecification().tn_ved_code();
    };
    TestSimpleListTextCsvTag(
        "tn_ved_code",
        tnVedCodeGetter,
        "1234567890,4567890123",
        "1234567890, 4567890123");
}

TEST(BlueCsvParser, MarketSku) {
    TestBlueSimpleIntCsvTag<i64>("market-sku", [](const TOfferCarrier& item) {
        auto matching = item.DataCampOffer.content().binding().partner();
        return matching.has_market_sku_id() ? MakeMaybe<i64>(matching.market_sku_id()) : Nothing();
    });

    TestBlueSimpleIntCsvTag<i64>("market_sku", [](const TOfferCarrier& item) {
        auto matching = item.DataCampOffer.content().binding().partner();
        return matching.has_market_sku_id() ? MakeMaybe<i64>(matching.market_sku_id()) : Nothing();
    });
}
