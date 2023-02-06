#include <market/idx/datacamp/lib/conversion/OfferConversions.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <market/library/datetime/datetime.h>
#include <market/library/interface/indexer_report_interface.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <library/cpp/json/json_reader.h>

#include <cmath>

TEST(ConversionsTest, TestStatus)
{
    Market::DataCamp::Offer datacampOffer;

    auto identifiers = datacampOffer.mutable_identifiers();
    auto status = datacampOffer.mutable_status();

    identifiers->set_feed_id(1);
    identifiers->set_offer_id("2");

    auto& flag = *status->add_disabled();
    flag.set_flag(true);
    flag.mutable_meta()->set_source(Market::DataCamp::MARKET_IDX);

    auto& stock_flag = *status->add_disabled();
    stock_flag.set_flag(false);
    stock_flag.mutable_meta()->set_source(Market::DataCamp::MARKET_STOCK);

    auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

    ASSERT_TRUE(actualOffer.has_has_gone());
    ASSERT_TRUE(actualOffer.has_gone());

    ASSERT_EQ(actualOffer.disabled_flags(), (1 << Market::DataCamp::MARKET_IDX));
    ASSERT_EQ(actualOffer.disabled_flag_sources(), (1 << Market::DataCamp::MARKET_IDX) | (1 << Market::DataCamp::MARKET_STOCK));
}

namespace {
    Market::DataCamp::Offer MakeDefaultDatacampOffer() {
        Market::DataCamp::Offer datacampOffer;

        auto identifiers = datacampOffer.mutable_identifiers();
        identifiers->set_feed_id(1);
        identifiers->set_offer_id("2");

        return datacampOffer;
    }

    Market::DataCamp::Offer MakeDefaultWhiteDatacampOffer() {
        auto datacampOffer = MakeDefaultDatacampOffer();
        datacampOffer.mutable_meta()->set_rgb(Market::DataCamp::MarketColor::WHITE); // чтобы считались флаги
        (*datacampOffer.mutable_meta()->mutable_platforms())[Market::DataCamp::MarketColor::WHITE] = true;
        return datacampOffer;
    }
}

TEST(ConversionsTest, TestEnableAutoDiscount_FromDatacampOffer)
{
    Market::DataCamp::Offer datacampOffer = MakeDefaultDatacampOffer();
    datacampOffer.mutable_price()->mutable_enable_auto_discounts()->set_flag(true);

    auto offer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

    ASSERT_TRUE(offer.has_enable_auto_discounts());
    ASSERT_TRUE(offer.enable_auto_discounts());
}

TEST(ConversionsTest, TestStockStoreCount_FromDatacampOffer)
{
    Market::DataCamp::Offer datacampOffer = MakeDefaultDatacampOffer();
    datacampOffer.mutable_stock_info()->mutable_market_stocks()->set_count(123);

    auto offer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

    ASSERT_EQ(offer.stock_store_count(), 123);
}

TEST(ConversionsTest, TestDirectProductMapping_FromDatacampOffer)
{
    Market::DataCamp::Offer datacampOffer = MakeDefaultDatacampOffer();
    datacampOffer.mutable_partner_info()->set_direct_product_mapping(true);

    {
        auto offer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_FALSE(offer.flags() & NMarket::NDocumentFlags::IS_SMB);
    }

    {
        // SMB-оффера поддерживаются только для белых
        datacampOffer.mutable_meta()->set_rgb(Market::DataCamp::MarketColor::WHITE);
        (*datacampOffer.mutable_meta()->mutable_platforms())[Market::DataCamp::MarketColor::BLUE] = true;
        auto offer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_TRUE(offer.flags() & NMarket::NDocumentFlags::IS_SMB);
        ASSERT_TRUE(offer.flags() & NMarket::NDocumentFlags::CPC);
    }
}


TEST(ConversionsTest, TestHasGone_MARKET_STOCK)
{
    /* Тест на проверку того, что игнорируются скрытия по стокам */
    Market::DataCamp::Offer datacampOffer = MakeDefaultDatacampOffer();

    auto status = datacampOffer.mutable_status();
    auto& flag = *status->add_disabled();
    flag.set_flag(true);
    flag.mutable_meta()->set_source(Market::DataCamp::MARKET_STOCK);

    auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

    ASSERT_TRUE(actualOffer.has_has_gone());
    ASSERT_FALSE(actualOffer.has_gone());

    ASSERT_FALSE(actualOffer.flags() & NMarket::NDocumentFlags::OFFER_HAS_GONE);
}


TEST(ConversionsTest, Test_PRE_ORDERED)
{
    /* Для офферов с предзаказом должен проставляться IS_PREORDER в flags */
    Market::DataCamp::Offer datacampOffer = MakeDefaultDatacampOffer();

    auto meta = datacampOffer.mutable_meta();
    meta->set_rgb(Market::DataCamp::MarketColor::BLUE);
    (*meta->mutable_platforms())[Market::DataCamp::MarketColor::BLUE] = true;

    auto assert_preorder = [&datacampOffer](bool isPreorder) {
        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_EQ(static_cast<bool>(actualOffer.flags() & NMarket::NDocumentFlags::IS_PREORDER), isPreorder);
    };
    assert_preorder(false);
    datacampOffer.mutable_order_properties()->set_order_method(Market::DataCamp::OfferOrderProperties::PRE_ORDERED);
    assert_preorder(true);
}


TEST(ConversionsTest, Test_Fulfillment)
{
    /* Тест на проверку того, что игнорируются скрытия по стокам */
    Market::DataCamp::Offer datacampOffer = MakeDefaultDatacampOffer();

    auto meta = datacampOffer.mutable_meta();
    meta->set_rgb(Market::DataCamp::MarketColor::BLUE);
    (*meta->mutable_platforms())[Market::DataCamp::MarketColor::BLUE] = true;

    auto assert_fulfillment = [&datacampOffer](bool fulfillment) {
        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_EQ(
                static_cast<bool>(actualOffer.flags() & NMarket::NDocumentFlags::IS_FULFILLMENT),
                fulfillment);
        ASSERT_EQ(
                static_cast<bool>(actualOffer.flags() & NMarket::NDocumentFlags::IS_FULFILLMENT),
                fulfillment);
    };

    // Если is_fulfillment не установлен, то оффер в программе fulfillment
    assert_fulfillment(true);
    // Если is_fulfillment == false, то оффер не в программе fulfillment (dropship, click&collect, ...)
    datacampOffer.mutable_partner_info()->set_is_fulfillment(false);
    assert_fulfillment(false);
    // Если is_fulfillment == true, то оффер в программе fulfillment
    datacampOffer.mutable_partner_info()->set_is_fulfillment(true);
    assert_fulfillment(true);
}

TEST(ConversionsTest, Test_Bids)
{
    /* Проверка заполняемости ставок из datacamp offer */

    Market::DataCamp::Offer datacampOffer = MakeDefaultDatacampOffer();
    datacampOffer.mutable_bids()->mutable_bid_actual()->set_value(123);
    datacampOffer.mutable_bids()->mutable_amore_beru_supplier_data()->set_value("supplier");
    datacampOffer.mutable_bids()->mutable_amore_beru_vendor_data()->set_value("vendor");

    auto offer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

    ASSERT_TRUE(offer.has_bid());
    ASSERT_EQ(offer.bid(), 123);

    ASSERT_TRUE(offer.has_amore_beru_supplier_data());
    ASSERT_EQ(offer.amore_beru_supplier_data(), "supplier");

    ASSERT_TRUE(offer.has_amore_beru_vendor_data());
    ASSERT_EQ(offer.amore_beru_vendor_data(), "vendor");
}

TEST(ConversionsTest, Test_ContriesOfOrigin)
{
    /* Проверка корректного заполнения поля country_of_origin из datacamp offer */
    Market::DataCamp::Offer datacampOffer = MakeDefaultDatacampOffer();
    datacampOffer.mutable_content()->mutable_partner()->mutable_actual()->mutable_country_of_origin_id()->add_value(123);
    datacampOffer.mutable_content()->mutable_partner()->mutable_actual()->mutable_country_of_origin_id()->add_value(124);
    datacampOffer.mutable_content()->mutable_partner()->mutable_actual()->mutable_country_of_origin_id()->add_value(125);

    auto offer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

    ASSERT_EQ(offer.country_of_origin(), "123,124,125");
}

TEST(ConversionsTest, Test_WeightAndDimensions)
{
    /* Заполнение полей ВГ при обратной конвертации: для белых берем из partner.actual,
    для синих из content.master_data (legacy: из content.market) */
    Market::DataCamp::Offer datacampOffer = MakeDefaultDatacampOffer();
    datacampOffer.mutable_meta()->set_rgb(Market::DataCamp::MarketColor::WHITE);
    (*datacampOffer.mutable_meta()->mutable_platforms())[Market::DataCamp::MarketColor::WHITE] = true;

    datacampOffer.mutable_content()->mutable_partner()->mutable_actual()->mutable_weight()->set_grams(1500);
    datacampOffer.mutable_content()->mutable_partner()->mutable_actual()->mutable_dimensions()->set_length_mkm(25000);
    datacampOffer.mutable_content()->mutable_partner()->mutable_actual()->mutable_dimensions()->set_width_mkm(35000);
    datacampOffer.mutable_content()->mutable_partner()->mutable_actual()->mutable_dimensions()->set_height_mkm(45000);
    datacampOffer.mutable_content()->mutable_market()->mutable_dimensions()->set_weight(9);
    datacampOffer.mutable_content()->mutable_market()->mutable_dimensions()->set_length(99);
    datacampOffer.mutable_content()->mutable_market()->mutable_dimensions()->set_width(999);
    datacampOffer.mutable_content()->mutable_market()->mutable_dimensions()->set_height(9999);

    auto whiteOffer= NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

    ASSERT_EQ(whiteOffer.weight(), 1.5);
    ASSERT_EQ(whiteOffer.length(), 2.5);
    ASSERT_EQ(whiteOffer.width(), 3.5);
    ASSERT_EQ(whiteOffer.height(), 4.5);

    //сделаем оффер синим - вг поля заполнятся из маркетного контента
    datacampOffer.mutable_meta()->set_rgb(Market::DataCamp::MarketColor::BLUE);
    (*datacampOffer.mutable_meta()->mutable_platforms())[Market::DataCamp::MarketColor::BLUE] = true;
    auto blueOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

    ASSERT_EQ(blueOffer.weight(), 9);
    ASSERT_EQ(blueOffer.length(), 99);
    ASSERT_EQ(blueOffer.width(), 999);
    ASSERT_EQ(blueOffer.height(), 9999);
}

TEST(ConversionsTest, Test_BlueWeightAndDimensionsFromMasterData)
{
    /* Проверяем, что приоритетно ВГ берутся из content.master_data (а не из content.market) */
    Market::DataCamp::Offer datacampOffer = MakeDefaultDatacampOffer();
    datacampOffer.mutable_meta()->set_rgb(Market::DataCamp::MarketColor::BLUE);
    (*datacampOffer.mutable_meta()->mutable_platforms())[Market::DataCamp::MarketColor::BLUE] = true;

    datacampOffer.mutable_content()->mutable_master_data()->mutable_weight_gross()->set_value_mg(5000000);
    datacampOffer.mutable_content()->mutable_master_data()->mutable_dimensions()->set_length_mkm(40000);
    datacampOffer.mutable_content()->mutable_master_data()->mutable_dimensions()->set_width_mkm(30000);
    datacampOffer.mutable_content()->mutable_master_data()->mutable_dimensions()->set_height_mkm(20000);

    datacampOffer.mutable_content()->mutable_market()->mutable_dimensions()->set_weight(9);
    datacampOffer.mutable_content()->mutable_market()->mutable_dimensions()->set_length(99);
    datacampOffer.mutable_content()->mutable_market()->mutable_dimensions()->set_width(999);
    datacampOffer.mutable_content()->mutable_market()->mutable_dimensions()->set_height(9999);

    auto blueOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

    ASSERT_EQ(blueOffer.weight(), 5);
    ASSERT_EQ(blueOffer.length(), 4);
    ASSERT_EQ(blueOffer.width(), 3);
    ASSERT_EQ(blueOffer.height(), 2);
}

TEST(ConversionsTest, Test_MasterData)
{
    /* Проверяем, что мастер-данные берутся из content.master_data */
    Market::DataCamp::Offer datacampOffer = MakeDefaultDatacampOffer();

    auto& masterData = *datacampOffer.mutable_content()->mutable_master_data();

    for (auto geoId: {1, 2, 3}) {
        auto& country = *masterData.mutable_manufacturer_countries()->add_countries();
        country.set_geo_id(geoId);
    }

    auto offer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
    ASSERT_EQ(offer.manufacturer_country_ids(), "1,2,3");
}

TEST(ConversionsTest, Test_PriceFrom)
{
    /* Заполнение поля price_from и связного с ним флажка THIS_IS_PRICE_FROM */
    Market::DataCamp::Offer datacampOffer = MakeDefaultDatacampOffer();

    auto assertPriceFrom = [&datacampOffer](bool priceFrom) {
        auto whiteOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_EQ(
                static_cast<bool>(whiteOffer.flags() & NMarket::NDocumentFlags::THIS_IS_PRICE_FROM),
                priceFrom);
        ASSERT_EQ(
                static_cast<bool>(whiteOffer.flags() & NMarket::NDocumentFlags::THIS_IS_PRICE_FROM),
                priceFrom);
    };

    // не установлен price_from
    assertPriceFrom(false);

    // price_from установлен в true
    datacampOffer = MakeDefaultDatacampOffer();
    datacampOffer.mutable_content()->mutable_partner()->mutable_actual()->mutable_price_from()->set_flag(true);
    assertPriceFrom(true);

    // price_from установлен в false
    datacampOffer = MakeDefaultDatacampOffer();
    datacampOffer.mutable_content()->mutable_partner()->mutable_actual()->mutable_price_from()->set_flag(false);
    assertPriceFrom(false);
}

TEST(ConversionsTest, Test_ISO8601StringFromDuration) {
    // данные для теста взяты отсюда
    // https://a.yandex-team.ru/arc/trunk/arcadia/market/library/datetime/datetime_ut.cpp?rev=4312451#L185
    tm duration;
    Market::DataCamp::Duration actualDuration;
    actualDuration.set_years(1);
    actualDuration.set_months(2);
    actualDuration.set_days(10);
    actualDuration.set_hours(2);
    actualDuration.set_minutes(30);
    actualDuration.set_seconds(51);
    ASSERT_TRUE(ParseDuration("P1Y2M10DT2H30M51S", &duration));
    ASSERT_EQ("P1Y2M10DT2H30M51S", NMarket::NDataCamp::ConvertDatacampDurationToISO8601String(actualDuration));

    actualDuration.Clear();
    actualDuration.set_years(0);
    actualDuration.set_months(5);
    actualDuration.set_days(3);
    actualDuration.set_hours(20);
    actualDuration.set_minutes(7);
    actualDuration.set_seconds(0);
    ASSERT_TRUE(ParseDuration("P0Y5M3DT20H7M0S", &duration));
    ASSERT_EQ("P0Y5M3DT20H7M0S", NMarket::NDataCamp::ConvertDatacampDurationToISO8601String(actualDuration));

    actualDuration.Clear();
    actualDuration.set_years(0);
    actualDuration.set_months(5);
    actualDuration.set_days(3);
    actualDuration.set_hours(0);
    actualDuration.set_minutes(7);
    actualDuration.set_seconds(0);
    ASSERT_TRUE(ParseDuration("P0Y5M3DT0H7M0S", &duration));
    ASSERT_EQ("P0Y5M3DT0H7M0S", NMarket::NDataCamp::ConvertDatacampDurationToISO8601String(actualDuration));

    actualDuration.Clear();
    actualDuration.set_years(99);
    actualDuration.set_months(0);
    actualDuration.set_days(30);
    actualDuration.set_hours(8);
    actualDuration.set_minutes(0);
    actualDuration.set_seconds(0);
    ASSERT_TRUE(ParseDuration("P99Y0M30DT8H0M0S", &duration));
    ASSERT_EQ("P99Y0M30DT8H0M0S", NMarket::NDataCamp::ConvertDatacampDurationToISO8601String(actualDuration));

    actualDuration.Clear();
    actualDuration.set_years(2);
    actualDuration.set_months(10);
    actualDuration.set_days(25);
    actualDuration.set_hours(0);
    actualDuration.set_minutes(0);
    actualDuration.set_seconds(0);
    ASSERT_TRUE(ParseDuration("P2Y10M25D", &duration));
    ASSERT_EQ("P2Y10M25D", NMarket::NDataCamp::ConvertDatacampDurationToISO8601String(actualDuration));

    actualDuration.Clear();
    actualDuration.set_years(2);
    actualDuration.set_months(0);
    actualDuration.set_days(25);
    actualDuration.set_hours(0);
    actualDuration.set_minutes(0);
    actualDuration.set_seconds(0);
    ASSERT_TRUE(ParseDuration("P2Y0M25D", &duration));
    ASSERT_EQ("P2Y0M25D", NMarket::NDataCamp::ConvertDatacampDurationToISO8601String(actualDuration));

    actualDuration.Clear();
    actualDuration.set_years(7);
    actualDuration.set_months(11);
    actualDuration.set_days(0);
    actualDuration.set_hours(0);
    actualDuration.set_minutes(0);
    actualDuration.set_seconds(0);
    ASSERT_TRUE(ParseDuration("P7Y11M", &duration));
    ASSERT_EQ("P7Y11M", NMarket::NDataCamp::ConvertDatacampDurationToISO8601String(actualDuration));

    actualDuration.Clear();
    actualDuration.set_years(0);
    actualDuration.set_months(2);
    actualDuration.set_days(30);
    actualDuration.set_hours(0);
    actualDuration.set_minutes(0);
    actualDuration.set_seconds(0);
    ASSERT_TRUE(ParseDuration("P2M30D", &duration));
    ASSERT_EQ("P2M30D", NMarket::NDataCamp::ConvertDatacampDurationToISO8601String(actualDuration));

    actualDuration.Clear();
    actualDuration.set_years(3);
    actualDuration.set_months(0);
    actualDuration.set_days(0);
    actualDuration.set_hours(0);
    actualDuration.set_minutes(0);
    actualDuration.set_seconds(0);
    ASSERT_TRUE(ParseDuration("P3Y", &duration));
    ASSERT_EQ("P3Y", NMarket::NDataCamp::ConvertDatacampDurationToISO8601String(actualDuration));

    actualDuration.Clear();
    actualDuration.set_years(0);
    actualDuration.set_months(6);
    actualDuration.set_days(0);
    actualDuration.set_hours(0);
    actualDuration.set_minutes(0);
    actualDuration.set_seconds(0);
    ASSERT_TRUE(ParseDuration("P6M", &duration));
    ASSERT_EQ("P6M", NMarket::NDataCamp::ConvertDatacampDurationToISO8601String(actualDuration));

    actualDuration.Clear();
    actualDuration.set_years(0);
    actualDuration.set_months(0);
    actualDuration.set_days(30);
    actualDuration.set_hours(0);
    actualDuration.set_minutes(0);
    actualDuration.set_seconds(0);
    ASSERT_TRUE(ParseDuration("P30D", &duration));
    ASSERT_EQ("P30D", NMarket::NDataCamp::ConvertDatacampDurationToISO8601String(actualDuration));

    actualDuration.Clear();
    actualDuration.set_years(0);
    actualDuration.set_months(0);
    actualDuration.set_days(0);
    actualDuration.set_hours(1);
    actualDuration.set_minutes(1);
    actualDuration.set_seconds(1);
    ASSERT_FALSE(ParseDuration("PT1H1M1S", &duration));
    ASSERT_EQ("", NMarket::NDataCamp::ConvertDatacampDurationToISO8601String(actualDuration));
}

TEST(ConversionsTest, Test_SellerWarranty)
{
    // offer with duration warranty (Datacamp->Genlog)
    Market::DataCamp::Duration actualDuration;
    actualDuration.set_months(11);
    actualDuration.set_days(10);
    actualDuration.set_hours(2);
    actualDuration.set_seconds(51);
    TString durationStr = "P0Y11M10DT2H0M51S";

    auto datacampOffer = MakeDefaultDatacampOffer();
    datacampOffer.mutable_content()->mutable_partner()->mutable_actual()->mutable_seller_warranty()->mutable_warranty_period()->CopyFrom(actualDuration);
    auto offerWithWarrantyDuration= NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
    ASSERT_TRUE(offerWithWarrantyDuration.has_seller_warranty());
    ASSERT_EQ(offerWithWarrantyDuration.seller_warranty(), durationStr);
}

TEST(ConversionsTest, Test_Expiry)
{
    // offer without expiry (Datacamp->Genlog)
    auto datacampOffer = MakeDefaultDatacampOffer();
    auto offerWithoutExpiry = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
    ASSERT_FALSE(offerWithoutExpiry.has_expiry());

    // offer with datetime expiry (Datacamp->Genlog)
    TString expiryStr = "2020-05-21T12:01:27";
    time_t expiryTs = parsedatet(expiryStr.c_str());

    datacampOffer = MakeDefaultDatacampOffer();
    datacampOffer.mutable_content()->mutable_partner()->mutable_actual()->mutable_expiry()->mutable_datetime()->CopyFrom(
        google::protobuf::util::TimeUtil::SecondsToTimestamp(expiryTs));
    auto whiteOfferWithDatetime = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
    ASSERT_TRUE(whiteOfferWithDatetime.has_expiry());
    ASSERT_EQ(whiteOfferWithDatetime.expiry(), expiryStr);

    // offer with duration expiry (Datacamp->Genlog)
    Market::DataCamp::Duration actualDuration;
    actualDuration.set_years(1);
    actualDuration.set_months(2);
    actualDuration.set_days(10);
    actualDuration.set_hours(2);
    actualDuration.set_minutes(30);
    actualDuration.set_seconds(51);
    TString durationStr = "P1Y2M10DT2H30M51S";
    datacampOffer = MakeDefaultDatacampOffer();
    datacampOffer.mutable_content()->mutable_partner()->mutable_actual()->mutable_expiry()->mutable_validity_period()->CopyFrom(actualDuration);
    auto offerWithExpiryDuration = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
    ASSERT_TRUE(offerWithExpiryDuration.has_expiry());
    ASSERT_EQ(offerWithExpiryDuration.expiry(), durationStr);
}

TEST(ConversionsTest, Test_Pickup_Store)
{
    /* Проверка корректного заполнения полей pickup/store из datacamp offer: если не были задан, в Genlog - true.
    Если были заданы - то перекладываем соответствующее значение */
    Market::DataCamp::Offer datacampOffer = MakeDefaultWhiteDatacampOffer();
    Market::DataCamp::Offer datacampOffer_true = MakeDefaultWhiteDatacampOffer();
    Market::DataCamp::Offer datacampOffer_false = MakeDefaultWhiteDatacampOffer();

    datacampOffer.mutable_delivery()->mutable_delivery_info()->set_use_yml_delivery(false);

    datacampOffer_true.mutable_delivery()->mutable_delivery_info()->set_pickup(true);
    datacampOffer_true.mutable_delivery()->mutable_delivery_info()->set_store(true);

    datacampOffer_false.mutable_delivery()->mutable_delivery_info()->set_pickup(false);
    datacampOffer_false.mutable_delivery()->mutable_delivery_info()->set_store(false);

    ASSERT_TRUE(!datacampOffer.delivery().delivery_info().has_pickup());
    ASSERT_TRUE(!datacampOffer.delivery().delivery_info().has_store());

    auto offer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
    auto offer_true = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer_true);
    auto offer_false = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer_false);

    ASSERT_TRUE(offer.flags() & NMarket::NDocumentFlags::PICKUP);
    ASSERT_TRUE(offer.flags() & NMarket::NDocumentFlags::STORE);
    ASSERT_EQ(offer.flags(), 4225); // PICKUP + STORE + IS_PUSH_PARTNER

    ASSERT_TRUE(offer_true.flags() & NMarket::NDocumentFlags::PICKUP);
    ASSERT_TRUE(offer_true.flags() & NMarket::NDocumentFlags::STORE);
    ASSERT_EQ(offer_true.flags(), 4225); // PICKUP + STORE + IS_PUSH_PARTNER

    ASSERT_FALSE(offer_false.flags() & NMarket::NDocumentFlags::PICKUP);
    ASSERT_FALSE(offer_false.flags() & NMarket::NDocumentFlags::STORE);
    ASSERT_EQ(offer_false.flags(), 4096); // IS_PUSH_PARTNER
}

TEST(ConversionsTest, Test_Description) {
    // offer description (Datacamp->Genlog)
    auto datacampOffer = MakeDefaultDatacampOffer();
    auto* partnerContent = datacampOffer.mutable_content()->mutable_partner();
    partnerContent->mutable_actual()->mutable_description()->set_value("description");
    partnerContent->mutable_original()->mutable_model()->set_value("raw model");

    auto offerDescr = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
    ASSERT_TRUE(offerDescr.has_description());
    ASSERT_EQ(offerDescr.description(), "description");
    ASSERT_EQ(offerDescr.raw_model(), "raw model");
}

TEST(ConversionsTest, Test_PurchasePrice) {
    // offer purchase_price (Datacamp->Genlog)
    auto priceVal = 133830.5;
    // TFixedPointNumber имеет точность 10^7
    uint64_t priceValExpression = priceVal * std::pow(10, 7);
    auto datacampOffer = MakeDefaultDatacampOffer();
    datacampOffer.mutable_price()->mutable_purchase_price()->set_enabled(true);
    datacampOffer.mutable_price()->mutable_purchase_price()->mutable_binary_price()->set_price(priceValExpression);
    auto feedOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
    ASSERT_TRUE(feedOffer.has_purchase_price());
    ASSERT_EQ(feedOffer.purchase_price(), priceVal);
}

TEST(ConversionsTest, Test_DynamicPricing) {
    // offer dynamic_pricing (Datacamp->Genlog)
    // TFixedPointNumber имеет точность 10^7
    uint64_t priceValExpression = 1334 * std::pow(10, 7);
    // Для значений в процентах - умножаем на 100
    uint64_t thresholdPercent = 33.67 * 100;
    {
        auto datacampOffer = MakeDefaultDatacampOffer();
        datacampOffer.mutable_price()->mutable_dynamic_pricing()->set_type(Market::DataCamp::DynamicPricing::RECOMMENDED_PRICE);
        datacampOffer.mutable_price()->mutable_dynamic_pricing()->set_threshold_fixed_value(priceValExpression);
        auto feedOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_TRUE(feedOffer.has_dynamic_pricing_type());
        ASSERT_EQ(feedOffer.dynamic_pricing_type(), int(Market::DataCamp::DynamicPricing::RECOMMENDED_PRICE));
        ASSERT_TRUE(feedOffer.has_dynamic_pricing_threshold_is_percent());
        ASSERT_FALSE(feedOffer.dynamic_pricing_threshold_is_percent());
        ASSERT_TRUE(feedOffer.has_dynamic_pricing_threshold_value());
        ASSERT_EQ(feedOffer.dynamic_pricing_threshold_value(), priceValExpression);
    }
    {
        auto datacampOffer = MakeDefaultDatacampOffer();
        datacampOffer.mutable_price()->mutable_dynamic_pricing()->set_type(Market::DataCamp::DynamicPricing::MINIMAL_PRICE_ON_MARKET);
        datacampOffer.mutable_price()->mutable_dynamic_pricing()->set_threshold_percent(thresholdPercent);
        auto feedOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_TRUE(feedOffer.has_dynamic_pricing_type());
        ASSERT_EQ(feedOffer.dynamic_pricing_type(), int(Market::DataCamp::DynamicPricing::MINIMAL_PRICE_ON_MARKET));
        ASSERT_TRUE(feedOffer.has_dynamic_pricing_threshold_is_percent());
        ASSERT_TRUE(feedOffer.dynamic_pricing_threshold_is_percent());
        ASSERT_TRUE(feedOffer.has_dynamic_pricing_threshold_value());
        ASSERT_EQ(feedOffer.dynamic_pricing_threshold_value(), thresholdPercent);
    }
}

TEST(ConversionsTest, TestOffersPromo)
{
    Market::DataCamp::Offer datacampOffer;
    auto identifiers = datacampOffer.mutable_identifiers();
    identifiers->set_feed_id(1);
    identifiers->set_offer_id("2");

    auto& ap = *datacampOffer.mutable_promos()->mutable_anaplan_promos();
    {
        auto& activePromo = *ap.mutable_active_promos()->add_promos();
        activePromo.set_id("active_promo_id");
        activePromo.mutable_direct_discount()->mutable_price()->set_price(100);
        activePromo.mutable_direct_discount()->mutable_base_price()->set_price(120);
        *ap.mutable_all_promos()->add_promos() = activePromo;
    }
    {
        auto& oldPromo = *ap.mutable_all_promos()->add_promos();
        oldPromo.set_id("old_promo_id");
        oldPromo.mutable_direct_discount()->mutable_price()->set_price(30);
        oldPromo.mutable_direct_discount()->mutable_base_price()->set_price(35);
    }

    auto& pp = *datacampOffer.mutable_promos()->mutable_partner_promos();
    {
        auto& partnerPromo = *pp.add_promos();
        partnerPromo.set_id("partner_promo_id");
        partnerPromo.mutable_direct_discount()->mutable_price()->set_price(200);
        partnerPromo.mutable_direct_discount()->mutable_base_price()->set_price(320);
    }

    auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
    NJson::TJsonValue asJsonValue;
    NJson::ReadJsonTree(actualOffer.datacamp_promos(), &asJsonValue);
    const auto& array = asJsonValue.GetArraySafe();
    ASSERT_EQ(array.size(), 3u);
    ASSERT_EQ(array[0]["id"].GetString(),"active_promo_id");
    ASSERT_EQ(array[0]["oldprice"]["price"].GetInteger(), 120);
    ASSERT_EQ(array[0]["price"]["price"].GetInteger(), 100);
    ASSERT_EQ(array[0]["active"].GetBoolean(), true);

    ASSERT_EQ(array[1]["id"].GetString(),"old_promo_id");
    ASSERT_EQ(array[1]["oldprice"]["price"].GetInteger(), 35);
    ASSERT_EQ(array[1]["price"]["price"].GetInteger(), 30);
    ASSERT_EQ(array[1]["active"].GetBoolean(), false);

    ASSERT_EQ(array[2]["id"].GetString(),"partner_promo_id");
    ASSERT_EQ(array[2]["oldprice"]["price"].GetInteger(), 320);
    ASSERT_EQ(array[2]["price"]["price"].GetInteger(), 200);
    ASSERT_EQ(array[2]["active"].GetBoolean(), true);
}

TEST(ConversionsTest, TestPartnerCashbackPromoIds)
{
    Market::DataCamp::Offer datacampOffer;
    auto identifiers = datacampOffer.mutable_identifiers();
    identifiers->set_feed_id(1);
    identifiers->set_offer_id("2");

    auto* pcp = datacampOffer.mutable_promos()->mutable_partner_cashback_promos();
    {
        auto* promo = pcp->add_promos();
        promo->set_id("#partner_cashback_promo_id_1");
        promo = pcp->add_promos();
        promo->set_id("#partner_cashback_promo_id_2");
    }

    auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
    NJson::TJsonValue asJsonValue;
    NJson::ReadJsonTree(actualOffer.partner_cashback_promo_ids(), &asJsonValue);
    const auto& array = asJsonValue.GetArraySafe();
    ASSERT_EQ(array.size(), 2u);
    ASSERT_EQ(array[0]["id"].GetString(),"#partner_cashback_promo_id_1");
    ASSERT_EQ(array[1]["id"].GetString(),"#partner_cashback_promo_id_2");
}

TEST(ConversionsTest, TestDisableDatacampAggrerated)
{
    /* Скрытия с источниками DataSource более 16 должны "схлопываться" в 13-ый бит MARKET_DATACAMP
    Так как RTY поддерживает только 16 бит для скрытий
    */
    Market::DataCamp::Offer datacampOffer;

    auto identifiers = datacampOffer.mutable_identifiers();
    auto status = datacampOffer.mutable_status();

    identifiers->set_feed_id(1);
    identifiers->set_offer_id("2");

    auto& flag = *status->add_disabled();
    flag.set_flag(true);
    flag.mutable_meta()->set_source(Market::DataCamp::MARKET_MBI_OVERPRICE); // DataSource >= 16

    auto& flag2 = *status->add_disabled();
    flag2.set_flag(true);
    flag2.mutable_meta()->set_source(Market::DataCamp::MARKET_MBI);

    auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

    ASSERT_TRUE(actualOffer.has_has_gone());
    ASSERT_TRUE(actualOffer.has_gone());

    ASSERT_EQ(actualOffer.disabled_flags(), (1 << Market::DataCamp::MARKET_MBI) | (1 << Market::DataCamp::MARKET_DATACAMP));
}

TEST(ConversionsTest, TestExpress)
{
    /* Признак экспресс-гиперлокальности оферов преобразуется из/в датакэмп формат
    https://st.yandex-team.ru/MARKETPROJECT-5366 */

    // Datacamp -> Genlog
    auto datacampOffer = MakeDefaultDatacampOffer();
    datacampOffer.mutable_partner_info()->set_is_express(true);

    auto feedOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
    ASSERT_TRUE(feedOffer.flags() & NMarket::NDocumentFlags::IS_EXPRESS);
}

TEST(ConversionsTest, TestPriceLabsParams)
{
    auto dataCampOffer = MakeDefaultDatacampOffer();
    auto feedOffer = NMarket::NDataCamp::DataCamp2GenerationLog(dataCampOffer);

    ASSERT_TRUE(feedOffer.pricelabs_params().empty());

    auto& datacampParams = *dataCampOffer.mutable_content()->mutable_partner()->mutable_original()->mutable_pricelabs_params()->mutable_params();
    datacampParams["stock"] = "10";
    datacampParams["discount"] = "30";

    feedOffer = NMarket::NDataCamp::DataCamp2GenerationLog(dataCampOffer);
    const auto& feedParams = feedOffer.pricelabs_params();
    ASSERT_EQ(feedParams.size(), 2);
    for (const auto& param: feedParams) {
        if (param.name() == "stock") {
            ASSERT_EQ(param.value(), "10");
        } else if (param.name() == "discount") {
            ASSERT_EQ(param.value(), "30");
        } else {
            ASSERT_TRUE(false);
        }
    }
}

TEST(ConversionsTest, SupplierDescriptionFromDatacamp) {
    auto dataCampOffer = MakeDefaultDatacampOffer();
    dataCampOffer.mutable_content()->mutable_partner()->mutable_actual()->mutable_description()->set_value(
            "supplier descr from datacamp");

    auto feedOffer = NMarket::NDataCamp::DataCamp2GenerationLog(dataCampOffer);
    EXPECT_STREQ(feedOffer.supplier_description(), "supplier descr from datacamp");
}

TEST(ConversionsTest, TestTraceFlagDataCamp2GenerationLog)
{
    Market::DataCamp::Offer datacampOffer;
    auto identifiers = datacampOffer.mutable_identifiers();
    identifiers->set_feed_id(101);
    identifiers->set_offer_id("101");
    datacampOffer.mutable_meta()->set_rgb(Market::DataCamp::MarketColor::WHITE);
    (*datacampOffer.mutable_meta()->mutable_platforms())[Market::DataCamp::MarketColor::WHITE] = true;
    datacampOffer.mutable_tech_info()->mutable_otrace_info()->mutable_should_trace()->set_flag(true);

    auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

    ASSERT_TRUE(actualOffer.has_should_trace());
    ASSERT_TRUE(actualOffer.should_trace());
}

TEST(ConversionsTest, TestAlcoFlagDataCamp2GenerationLog)
{
    Market::DataCamp::Offer datacampOffer;
    auto identifiers = datacampOffer.mutable_identifiers();
    identifiers->set_feed_id(101);
    identifiers->set_offer_id("101");
    datacampOffer.mutable_meta()->set_rgb(Market::DataCamp::MarketColor::WHITE);
    (*datacampOffer.mutable_meta()->mutable_platforms())[Market::DataCamp::MarketColor::WHITE] = true;

    { // no alcohol offer
        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_FALSE(actualOffer.flags() & NMarket::NDocumentFlags::ALCOHOL);
    }
    { // from actual partner content
        datacampOffer.mutable_content()->mutable_partner()->mutable_actual()->mutable_type()
            ->set_value(NMarket::EProductType::ALCO);
        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_TRUE(actualOffer.flags() & NMarket::NDocumentFlags::ALCOHOL);
        datacampOffer.mutable_content()->mutable_partner()->mutable_actual()->clear_type();
    }
    { // from partner content description
        datacampOffer.mutable_content()->mutable_partner()->mutable_partner_content_desc()
            ->set_type(static_cast<int>(NMarket::EOfferType::ALCOHOL));

        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_TRUE(actualOffer.flags() & NMarket::NDocumentFlags::ALCOHOL);
    }
}

TEST(ConversionsTest, TestMskuDimensionsAndWeight2Genlog)
{
    Market::DataCamp::Offer datacampOffer = MakeDefaultWhiteDatacampOffer();

    Market::DataCamp::PreciseDimensions dimensions;
    dimensions.set_length_mkm(10'000);
    dimensions.set_width_mkm(20'000);
    dimensions.set_height_mkm(30'000);
    Market::DataCamp::PreciseWeight weight;
    weight.set_value_mg(4'000'000);
    auto masterData{datacampOffer.mutable_content()->mutable_master_data()};
    masterData->mutable_msku_dimensions()->Swap(&dimensions);
    masterData->mutable_msku_weight_gross()->Swap(&weight);

    Market::DataCamp::PreciseDimensions partnerDimensions;
    partnerDimensions.set_length_mkm(15'000);
    partnerDimensions.set_width_mkm(25'000);
    partnerDimensions.set_height_mkm(35'000);
    Market::DataCamp::PreciseWeight partnerWeight;
    partnerWeight.set_value_mg(4'500'000);
    auto partnerActual{datacampOffer.mutable_content()->mutable_partner()->mutable_actual()};
    partnerActual->mutable_dimensions()->Swap(&partnerDimensions);
    partnerActual->mutable_weight()->Swap(&partnerWeight);

    Market::DataCamp::PreciseDimensions masterDataDimensions;
    masterDataDimensions.set_length_mkm(11'000);
    masterDataDimensions.set_width_mkm(21'000);
    masterDataDimensions.set_height_mkm(31'000);
    Market::DataCamp::PreciseWeight masterDataWeight;
    masterDataWeight.set_value_mg(4'100'000);

    { // both partner and msku data is set and offer is DBS
        datacampOffer.mutable_partner_info()->set_is_dsbs(true);
        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_EQ(actualOffer.length(), 1.0);
        ASSERT_EQ(actualOffer.width(), 2.0);
        ASSERT_EQ(actualOffer.height(), 3.0);
        ASSERT_EQ(actualOffer.weight(), 4.0);
    }
    { // DBS offer - prefer master_data if available
        datacampOffer.mutable_partner_info()->set_is_dsbs(true);
        masterData->mutable_dimensions()->Swap(&masterDataDimensions);
        masterData->mutable_weight_gross()->Swap(&masterDataWeight);
        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_EQ(actualOffer.length(), 1.1);
        ASSERT_EQ(actualOffer.width(), 2.1);
        ASSERT_EQ(actualOffer.height(), 3.1);
        ASSERT_EQ(actualOffer.weight(), 4.1);
        masterData->clear_dimensions();
        masterData->clear_weight_gross();
    }
    { // both partner and msku data is set but offer is not DBS
        datacampOffer.mutable_partner_info()->set_is_dsbs(false);
        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_EQ(actualOffer.length(), 1.5);
        ASSERT_EQ(actualOffer.width(), 2.5);
        ASSERT_EQ(actualOffer.height(), 3.5);
        ASSERT_EQ(actualOffer.weight(), 4.5);
    }
    { // partner data and msku weight_gross are set, but msku_dimensions is not
        datacampOffer.mutable_partner_info()->set_is_dsbs(true);
        masterData->clear_msku_dimensions();
        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_EQ(actualOffer.length(), 1.5);
        ASSERT_EQ(actualOffer.width(), 2.5);
        ASSERT_EQ(actualOffer.height(), 3.5);
        ASSERT_EQ(actualOffer.weight(), 4.5);
    }
    { // neither partner nor msku data is set
        datacampOffer.mutable_partner_info()->set_is_dsbs(true);
        masterData->clear_msku_dimensions();
        masterData->clear_msku_weight_gross();
        partnerActual->clear_dimensions();
        partnerActual->clear_weight();
        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_EQ(actualOffer.has_length(), false);
        ASSERT_EQ(actualOffer.has_width(), false);
        ASSERT_EQ(actualOffer.has_height(), false);
        ASSERT_EQ(actualOffer.has_weight(), false);
    }
}

TEST(ConversionsTest, TestAvailableForBusinessesFlagDataCamp2GenerationLog)
{
    Market::DataCamp::Offer datacampOffer = MakeDefaultWhiteDatacampOffer();
    auto status = datacampOffer.mutable_status();
    auto& available_for_businesses = *status->mutable_available_for_businesses();

    { // available_for_businesses offer
        available_for_businesses.set_flag(true);
        available_for_businesses.mutable_meta()->set_source(Market::DataCamp::MARKET_MBI);
        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_TRUE(actualOffer.flags() & NMarket::NDocumentFlags::AVAILABLE_FOR_BUSINESSES);
    }
    { // not available_for_businesses offer
        available_for_businesses.set_flag(false);
        available_for_businesses.mutable_meta()->set_source(Market::DataCamp::MARKET_MBI);
        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_FALSE(actualOffer.flags() & NMarket::NDocumentFlags::AVAILABLE_FOR_BUSINESSES);
    }
}

TEST(ConversionsTest, TestProhibitedForPersonsFlagDataCamp2GenerationLog)
{
    Market::DataCamp::Offer datacampOffer = MakeDefaultWhiteDatacampOffer();
    auto status = datacampOffer.mutable_status();
    auto& prohibited_for_persons = *status->mutable_prohibited_for_persons();

    { // prohibited_for_persons offer
        prohibited_for_persons.set_flag(true);
        prohibited_for_persons.mutable_meta()->set_source(Market::DataCamp::MARKET_MBI);
        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_TRUE(actualOffer.flags() & NMarket::NDocumentFlags::PROHIBITED_FOR_PERSONS);
    }
    { // not prohibited_for_persons offer
        prohibited_for_persons.set_flag(false);
        prohibited_for_persons.mutable_meta()->set_source(Market::DataCamp::MARKET_MBI);
        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_FALSE(actualOffer.flags() & NMarket::NDocumentFlags::PROHIBITED_FOR_PERSONS);
    }
}

TEST(ConversionsTest, TestOnDemandFlagDataCamp2GenerationLog)
{
    Market::DataCamp::Offer datacampOffer = MakeDefaultWhiteDatacampOffer();
    auto content = datacampOffer.mutable_content();
    auto actual = content->mutable_partner()->mutable_actual();
    auto& actual_type = *actual->mutable_type();

    Market::DataCamp::UpdateMeta meta;
    meta.set_applier(NMarketIndexer::Common::MINER);

    { // on demand offer
        actual_type.set_value(NMarket::EProductType::ON_DEMAND);
        actual_type.mutable_meta()->CopyFrom(meta);
        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_TRUE(actualOffer.flags() & NMarket::NDocumentFlags::ON_DEMAND);
        actual->clear_type();
    }
    { // books offer
        actual_type.set_value(NMarket::EProductType::BOOKS);
        actual_type.mutable_meta()->CopyFrom(meta);
        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_FALSE(actualOffer.flags() & NMarket::NDocumentFlags::ON_DEMAND);
    }
}

TEST(ConversionsTest, TestIsResaleFlagDataCamp2GenerationLog)
{
    Market::DataCamp::Offer datacampOffer = MakeDefaultWhiteDatacampOffer();
    auto original = datacampOffer.mutable_content()->mutable_partner()->mutable_actual();
    auto& is_resale = *original->mutable_is_resale();

    { // is_resale offer
        is_resale.set_flag(true);
        is_resale.mutable_meta()->set_source(Market::DataCamp::PUSH_PARTNER_OFFICE);
        auto record = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_TRUE(record.flags() & NMarket::NDocumentFlags::IS_RESALE);
    }
    { // not is_resale offer
        is_resale.set_flag(false);
        is_resale.mutable_meta()->set_source(Market::DataCamp::PUSH_PARTNER_OFFICE);
        auto record = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_FALSE(record.flags() & NMarket::NDocumentFlags::IS_RESALE);
    }
}
