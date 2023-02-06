#include <market/idx/datacamp/routines/tasks/lib/utils.h>

#include <library/cpp/testing/unittest/gtest.h>

TEST(IsReadyForIndexerDumper, OfferNotDisabledByPartner) {
    // проверяем, что по умолчанию офферы должны включаться в индекс

    Market::DataCamp::Offer offer;

    bool ready = NMarket::IsReadyForIndexerDumper(
        offer,
        TDuration::Days(1),
        {},
        false,
        {}
    );

    EXPECT_TRUE(ready);
}

TEST(IsReadyForIndexerDumper, OfferDisabledByPartnerLongTimeAgo) {
    // проверяем, что офферы, скрытые раньше порогового значения, не должны включаться в индекс

    auto now = TInstant::Now();

    Market::DataCamp::Offer offer;
    offer.mutable_status()->mutable_disabled_by_partner_since_ts()->set_seconds((now - TDuration::Days(2)).Seconds());

    bool ready = NMarket::IsReadyForIndexerDumper(
        offer,
        TDuration::Days(1),
        {},
        false,
        {}
    );

    EXPECT_FALSE(ready);
}

TEST(IsReadyForIndexerDumper, OfferDisabledByPartnerRecently) {
    // проверяем, что офферы, скрытые позже порогового значения, должны включаться в индекс

    auto now = TInstant::Now();

    Market::DataCamp::Offer offer;
    offer.mutable_status()->mutable_disabled_by_partner_since_ts()->set_seconds((now - TDuration::Hours(12)).Seconds());

    bool ready = NMarket::IsReadyForIndexerDumper(
        offer,
        TDuration::Days(1),
        {},
        false,
        {}
    );

    EXPECT_TRUE(ready);
}

TEST(IsReadyForIndexerDumper, BlueOfferDisabledByPartnerLongTimeAgo) {
    // проверяем, что для синих офферов TTL игнорируется

    auto now = TInstant::Now();

    Market::DataCamp::Offer offer;
    offer.mutable_status()->mutable_disabled_by_partner_since_ts()->set_seconds((now - TDuration::Days(2)).Seconds());
    offer.mutable_meta()->set_rgb(Market::DataCamp::BLUE);

    bool ready = NMarket::IsReadyForIndexerDumper(
        offer,
        TDuration::Days(1),
        {},
        false,
        {}
    );

    EXPECT_TRUE(ready);
}

TEST(IsReadyForIndexerDumper, OfferDisabledByPartnerLongTimeAgoWithCustomTTL) {
    // проверяем, что для цветных офферов, порог берется из по-цветной настройки

    auto now = TInstant::Now();

    Market::DataCamp::Offer offer;
    offer.mutable_status()->mutable_disabled_by_partner_since_ts()->set_seconds((now - TDuration::Days(2)).Seconds());
    offer.mutable_meta()->set_rgb(Market::DataCamp::LAVKA);

    bool ready = NMarket::IsReadyForIndexerDumper(
        offer,
        TDuration::Days(1), // default TTL
        {{Market::DataCamp::LAVKA, TDuration::Days(3)}},    // per-color TTL
        false,
        {}
    );

    EXPECT_TRUE(ready);
}

TEST(IsReadyForIndexerDumper, OfferWithUnknownPriceCurrency) {
    // проверяем, что для оффера с неизвестной валютой не попадают в выгрузку

    const auto now = TInstant::Now();

    Market::DataCamp::Offer offerWithPrice;
    offerWithPrice.mutable_price()->mutable_basic()->mutable_binary_price()->set_price(1000);
    offerWithPrice.mutable_price()->mutable_basic()->mutable_binary_oldprice()->set_price(2000);
    offerWithPrice.mutable_price()->mutable_basic()->mutable_meta()->mutable_timestamp()->set_seconds(now.Seconds());

    Market::DataCamp::Offer offerWithBadCurrency = offerWithPrice;
    offerWithBadCurrency.mutable_price()->mutable_basic()->mutable_binary_price()->set_id("BAD");

    Market::DataCamp::Offer offerWithBadRefCurrency = offerWithPrice;
    offerWithBadRefCurrency.mutable_price()->mutable_basic()->mutable_binary_price()->set_ref_id("BAD");

    Market::DataCamp::Offer offerWithBadOldCurrency = offerWithPrice;
    offerWithBadOldCurrency.mutable_price()->mutable_basic()->mutable_binary_oldprice()->set_id("BAD");

    Market::DataCamp::Offer offerWithBadOldRefCurrency = offerWithPrice;
    offerWithBadOldRefCurrency.mutable_price()->mutable_basic()->mutable_binary_oldprice()->set_ref_id("BAD");

    for (const auto& offer: {offerWithBadCurrency, offerWithBadRefCurrency, offerWithBadOldCurrency, offerWithBadOldRefCurrency}) {
        bool ready = NMarket::IsReadyForIndexerDumper(
            offer,
            TDuration::Days(1),
            {},
            false,
            {}
        );
        EXPECT_FALSE(ready);
    }
}

TEST(IsReadyForIndexerDumper, OfferWithEmptyPriceCurrency) {
    // проверяем, что для оффера с пустой валютой не попадают в выгрузку

    const auto now = TInstant::Now();

    Market::DataCamp::Offer offerWithPrice;
    offerWithPrice.mutable_price()->mutable_basic()->mutable_binary_price()->set_price(1000);
    offerWithPrice.mutable_price()->mutable_basic()->mutable_binary_oldprice()->set_price(2000);
    offerWithPrice.mutable_price()->mutable_basic()->mutable_meta()->mutable_timestamp()->set_seconds(now.Seconds());

    Market::DataCamp::Offer offerWithBadCurrency = offerWithPrice;
    offerWithBadCurrency.mutable_price()->mutable_basic()->mutable_binary_price()->set_id("");

    Market::DataCamp::Offer offerWithBadRefCurrency = offerWithPrice;
    offerWithBadRefCurrency.mutable_price()->mutable_basic()->mutable_binary_price()->set_ref_id("");

    Market::DataCamp::Offer offerWithBadOldCurrency = offerWithPrice;
    offerWithBadOldCurrency.mutable_price()->mutable_basic()->mutable_binary_oldprice()->set_id("");

    Market::DataCamp::Offer offerWithBadOldRefCurrency = offerWithPrice;
    offerWithBadOldRefCurrency.mutable_price()->mutable_basic()->mutable_binary_oldprice()->set_ref_id("");

    for (const auto& offer: {offerWithBadCurrency, offerWithBadRefCurrency, offerWithBadOldCurrency, offerWithBadOldRefCurrency}) {
        bool ready = NMarket::IsReadyForIndexerDumper(
            offer,
            TDuration::Days(1),
            {},
            false,
            {}
        );
        EXPECT_FALSE(ready);
    }
}

TEST(IsReadyForIndexerDumper, OfferWithoutPriceCurrency) {
    // проверяем, что для оффера с ценой, но без указания валюты попадают в выгрузку

    const auto now = TInstant::Now();

    Market::DataCamp::Offer offerWithPrice;
    offerWithPrice.mutable_price()->mutable_basic()->mutable_binary_price()->set_price(1000);
    offerWithPrice.mutable_price()->mutable_basic()->mutable_binary_oldprice()->set_price(2000);
    offerWithPrice.mutable_price()->mutable_basic()->mutable_meta()->mutable_timestamp()->set_seconds(now.Seconds());

    bool ready = NMarket::IsReadyForIndexerDumper(
        offerWithPrice,
        TDuration::Days(1),
        {},
        false,
        {}
    );
    EXPECT_TRUE(ready);
}

TEST(IsReadyForIndexerDumper, NotDirectOfferWithSpecificCurrency) {
    // проверяем, что не директовые оффера с TRY или GBP не попадают в выгрузку

    const auto now = TInstant::Now();

    Market::DataCamp::Offer blueOfferWithPrice;
    blueOfferWithPrice.mutable_meta()->set_rgb(Market::DataCamp::BLUE);

    blueOfferWithPrice.mutable_price()->mutable_basic()->mutable_binary_price()->set_price(1000);
    blueOfferWithPrice.mutable_price()->mutable_basic()->mutable_binary_oldprice()->set_price(2000);
    blueOfferWithPrice.mutable_price()->mutable_basic()->mutable_meta()->mutable_timestamp()->set_seconds(now.Seconds());

    Market::DataCamp::Offer notDirectOfferWithBasicGBPCurrency = blueOfferWithPrice;
    notDirectOfferWithBasicGBPCurrency.mutable_price()->mutable_basic()->mutable_binary_price()->set_id("GBP");

    Market::DataCamp::Offer notDirectOfferWithBasicRefGBPCurrency = blueOfferWithPrice;
    notDirectOfferWithBasicRefGBPCurrency.mutable_price()->mutable_basic()->mutable_binary_price()->set_ref_id("GBP");

    Market::DataCamp::Offer notDirectOfferWithBasicOldGBPCurrency = blueOfferWithPrice;
    notDirectOfferWithBasicOldGBPCurrency.mutable_price()->mutable_basic()->mutable_binary_oldprice()->set_id("GBP");

    Market::DataCamp::Offer notDirectOfferWithBasicOldRegGBPCurrency = blueOfferWithPrice;
    notDirectOfferWithBasicOldRegGBPCurrency.mutable_price()->mutable_basic()->mutable_binary_oldprice()->set_ref_id("GBP");

    for (const auto& offer: {notDirectOfferWithBasicGBPCurrency,
                            notDirectOfferWithBasicRefGBPCurrency,
                            notDirectOfferWithBasicOldGBPCurrency,
                            notDirectOfferWithBasicOldRegGBPCurrency}) {
        bool ready = NMarket::IsReadyForIndexerDumper(
            offer,
            TDuration::Days(1),
            {},
            false,
            {}
        );
        EXPECT_FALSE(ready);
    }
}

