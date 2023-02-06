#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>
#include <market/idx/library/feed/offer_type.h>
#include <market/idx/library/ultracontroller/fill_enriching_offer.h>
#include <market/library/interface/indexer_report_interface.h>
#include <market/proto/ir/UltraController.pb.h>
#include <market/proto/ir/MarketParameters.pb.h>

#define ASSERT_PB_EQ(x, y) ASSERT_STRING_EQ((x).DebugString(), (y).DebugString())

void FillMarketParatemeters(Market::DataCamp::MarketParameterValue* p_value_1, Market::DataCamp::MarketParameterValue* p_value_2) {
    p_value_1->set_param_id(1);
    p_value_1->set_param_name("утюг");
    p_value_1->mutable_value()->set_str_value("для легкового автомобиля");
    p_value_1->mutable_value()->set_value_type(Market::DataCamp::MarketValueType::ENUM);
    p_value_1->set_value_source(Market::DataCamp::MarketValueSource::PARTNER);
    p_value_1->set_value_state(Market::DataCamp::MarketValueState::DEFAULT);

    p_value_2->set_param_id(4866984);
    p_value_2->set_param_name("ТВ-тюнер");
    p_value_2->mutable_value()->set_bool_value(true);
    p_value_2->mutable_value()->set_value_type(Market::DataCamp::MarketValueType::BOOLEAN);
    p_value_2->set_value_source(Market::DataCamp::MarketValueSource::RULE);
    p_value_2->set_value_state(Market::DataCamp::MarketValueState::REMOVE);
}

void FillIdentifiers(Market::DataCamp::Offer& dataCampOffer) {
    auto ids = dataCampOffer.mutable_identifiers();

    ids->set_offer_id("123");
    ids->set_feed_id(12345);
    ids->set_shop_id(1234500);
}

void FillBasicIdentifiers(Market::DataCamp::Offer& dataCampOffer) {
    auto ids = dataCampOffer.mutable_identifiers();
    ids->mutable_extra()->set_classifier_good_id("b6520eaa81c3aecc66ed825f57a561f5");
    ids->mutable_extra()->set_classifier_magic_id2("3ad83d81f50ad08db1bffee78a6b8ad7");
}

void FillContent(Market::DataCamp::Offer& dataCampOffer) {
    auto partner_content = dataCampOffer.mutable_content()->mutable_partner();
    auto actual_partner_content = partner_content->mutable_actual();

    actual_partner_content->mutable_adult()->set_flag(true);
    actual_partner_content->mutable_description()->set_value("Кофеварка с таймером");
    actual_partner_content->mutable_isbn()->add_value("978-5-09-071627-7");
    actual_partner_content->mutable_title()->set_value("Красный модель вендор");
    actual_partner_content->mutable_type()->set_value(NMarket::EProductType::ALCO);
    actual_partner_content->mutable_barcode()->add_value("1111111111");
    actual_partner_content->mutable_barcode()->add_value("0000000000");
    actual_partner_content->mutable_vendor_code()->set_value("Borsch_In_Content_Field");
    actual_partner_content->mutable_category()->set_path_category_names("Все товары\\Часть товаров");

    partner_content->mutable_partner_content_desc()->set_is_cut_price(true);

    auto parameter_values = partner_content->mutable_market_specific_content()->mutable_parameter_values();
    auto p_value_1 = parameter_values->add_parameter_values();
    auto p_value_2 = parameter_values->add_parameter_values();
    FillMarketParatemeters(p_value_1, p_value_2);

    dataCampOffer.mutable_content()->mutable_market()->set_market_category("Категория маркетная");
}

void FillPartnerInfo(Market::DataCamp::Offer& dataCampOffer) {
    dataCampOffer.mutable_partner_info()->set_datasource_name("Магазин кофеварок");
    dataCampOffer.mutable_partner_info()->set_shop_country(300);
}

void FillPrice(Market::DataCamp::Offer& dataCampOffer) {
    dataCampOffer.mutable_price()->set_ru_price(100500);
    dataCampOffer.mutable_price()->mutable_basic()->mutable_binary_price()->set_price(15000000000);
}

void FillPictures(Market::DataCamp::Offer& dataCampOffer) {
    dataCampOffer.mutable_pictures()->mutable_partner()->mutable_original()->add_source()->set_url("https://pic.com/url");
    dataCampOffer.mutable_pictures()->mutable_partner()->mutable_original()->add_source()->set_url("https://pic.com/url2");
}

void FillFullOffer(Market::DataCamp::Offer& dataCampOffer) {
    FillIdentifiers(dataCampOffer);
    FillBasicIdentifiers(dataCampOffer);
    FillContent(dataCampOffer);
    FillPrice(dataCampOffer);
    FillPictures(dataCampOffer);
    FillPartnerInfo(dataCampOffer);
}

void FillWhiteServiceOffer(Market::DataCamp::Offer& dataCampOffer) {
    FillIdentifiers(dataCampOffer);
    FillPartnerInfo(dataCampOffer);
    FillPrice(dataCampOffer);
}

void FillWhiteBasicOffer(Market::DataCamp::Offer& dataCampOffer) {
    FillBasicIdentifiers(dataCampOffer);
    FillContent(dataCampOffer);
    FillPictures(dataCampOffer);
}

void FillBlueServiceOffer(Market::DataCamp::Offer& dataCampOffer) {
    FillIdentifiers(dataCampOffer);
    FillContent(dataCampOffer);
    FillPartnerInfo(dataCampOffer);
    FillPrice(dataCampOffer);
}

void FillBlueBasicOffer(Market::DataCamp::Offer& dataCampOffer) {
    FillBasicIdentifiers(dataCampOffer);
    FillPictures(dataCampOffer);
}

void FillExpectedRequestOffer(Market::UltraControllerServiceData::Offer& expectedEnrichingOffer) {
    expectedEnrichingOffer.set_adult(true);
    expectedEnrichingOffer.set_alcohol(true);
    expectedEnrichingOffer.set_barcode("1111111111|0000000000");
    expectedEnrichingOffer.set_classifier_good_id("b6520eaa81c3aecc66ed825f57a561f5");
    expectedEnrichingOffer.set_classifier_magic_id("3ad83d81f50ad08db1bffee78a6b8ad7");
    expectedEnrichingOffer.set_description("Кофеварка с таймером");
    expectedEnrichingOffer.set_feed_id(12345);
    expectedEnrichingOffer.set_isbn("978-5-09-071627-7");
    expectedEnrichingOffer.set_market_category("Категория маркетная");
    expectedEnrichingOffer.set_offer("Красный модель вендор");
    expectedEnrichingOffer.set_pic_urls("https://pic.com/url\thttps://pic.com/url2");
    expectedEnrichingOffer.set_price(1500);
    expectedEnrichingOffer.set_shop_category_name("Все товары\\Часть товаров");
    expectedEnrichingOffer.set_shop_country(300);
    expectedEnrichingOffer.set_shop_id(1234500);
    expectedEnrichingOffer.set_shop_name("Магазин кофеварок");
    expectedEnrichingOffer.set_shop_offer_id("123");
    expectedEnrichingOffer.set_use_price_range(false);
    expectedEnrichingOffer.set_vendorcode("Borsch_In_Content_Field");
    expectedEnrichingOffer.set_offer_type(Market::UltraControllerServiceData::OT_ALCOHOL);
    expectedEnrichingOffer.set_product_type(NMarket::EProductType::ALCO);

    auto p_value_1 = expectedEnrichingOffer.add_parameter_values();
    auto p_value_2 = expectedEnrichingOffer.add_parameter_values();
    FillMarketParatemeters(p_value_1, p_value_2);

    //из параметров, которые передали в функцию
    expectedEnrichingOffer.set_is_sample(false);
    expectedEnrichingOffer.set_use_market_dimensions(false);
    expectedEnrichingOffer.set_use_cache(false);
}

Y_UNIT_TEST_SUITE(FillEnrichingOffer) {
    Y_UNIT_TEST(FullOffer) {
        Market::UltraControllerServiceData::Offer enrichingOffer;
        Market::UltraControllerServiceData::Offer expectedEnrichingOffer;
        Market::DataCamp::Offer dataCampOffer;

        FillFullOffer(dataCampOffer);
        FillExpectedRequestOffer(expectedEnrichingOffer);

        NUltraController::FillEnrichingOffer(
            enrichingOffer,
            dataCampOffer,
            &dataCampOffer,
            &dataCampOffer.price().basic(),
            false,
            false,
            false);

        UNIT_ASSERT_STRINGS_EQUAL(expectedEnrichingOffer.DebugString(), enrichingOffer.DebugString());
    }

    Y_UNIT_TEST(WhiteOffers) {
        Market::UltraControllerServiceData::Offer enrichingOffer;
        Market::UltraControllerServiceData::Offer expectedEnrichingOffer;
        Market::DataCamp::Offer dataCampOfferForPrice;
        Market::DataCamp::Offer dataCampOfferBasic;
        Market::DataCamp::Offer dataCampOfferService;

        FillPrice(dataCampOfferForPrice);
        FillWhiteBasicOffer(dataCampOfferBasic);
        FillWhiteServiceOffer(dataCampOfferService);
        FillExpectedRequestOffer(expectedEnrichingOffer);

        NUltraController::FillEnrichingOffer(
            enrichingOffer,
            dataCampOfferBasic,
            &dataCampOfferService,
            &dataCampOfferForPrice.price().basic(),
            false,
            false,
            false);

        UNIT_ASSERT_STRINGS_EQUAL(expectedEnrichingOffer.DebugString(), enrichingOffer.DebugString());
    }
}
