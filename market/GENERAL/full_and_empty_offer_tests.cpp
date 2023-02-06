#include <market/amore/ms_mapper/mapper.h>

#include <market/idx/datacamp/lib/conversion/OfferConversions.h>

#include <market/idx/datacamp/proto/offer/DataCampOffer.pb.h>

#include <market/idx/library/datacamp/offer_utils.h>

#include <market/library/datetime/datetime.h>
#include <market/library/interface/indexer_report_interface.h>
#include <market/proto/indexer/GenerationLog.pb.h>

#include <modadvert/bigmod/protos/interface/markup_flags.pb.h>

#include <library/cpp/protobuf/json/proto2json.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <utility>

#define ASSERT_PB_EQ(x, y) ASSERT_STREQ(NProtobufJson::Proto2Json(x), NProtobufJson::Proto2Json(y))
const static auto CurrencyRatesPath = JoinFsPaths(ArcadiaSourceRoot(), "market/idx/feeds/qparser/tests/data/currency_rates.xml");

TEST (ConverstionsTest, testEmpty)
{
    Market::DataCamp::Offer datacampOffer;
    MarketIndexer::GenerationLog::Record offer;

    auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

    ASSERT_PB_EQ(offer, actualOffer);
}

TEST (ConversionsTest, testRawVendor)
{
    Market::DataCamp::Offer datacampOffer;
    auto content = datacampOffer.mutable_content();
    datacampOffer.mutable_identifiers()->set_feed_id(1); // с пустым identifiers конвертации не будет

    auto enriched = new Market::UltraControllerServiceData::EnrichedOffer();
    enriched->set_vendor_id(0); // Для проверки, что raw_vendor заполняется только если УК не нашел глобального вендора
    content->mutable_market()->set_allocated_enriched_offer(enriched);
    content->mutable_partner()->mutable_actual()->mutable_vendor()->set_value("not global vendor");

    auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
    ASSERT_EQ(actualOffer.vendor_string(), "not global vendor");
}

TEST (ConversionsTest, testFull)
{
    CEXCHANGE.Load(CurrencyRatesPath);

    const ui64 offerFlags = NMarket::NDocumentFlags::DEPOT
                          | NMarket::NDocumentFlags::ADULT
                          | NMarket::NDocumentFlags::AVAILABLE
                          | NMarket::NDocumentFlags::AUTOBROKER_ENABLED
                          | NMarket::NDocumentFlags::STORE
                          | NMarket::NDocumentFlags::MODEL_COLOR_WHITE
                          | NMarket::NDocumentFlags::MODEL_COLOR_BLUE
                          | NMarket::NDocumentFlags::IS_FULFILLMENT
                          | NMarket::NDocumentFlags::CPC
                          | NMarket::NDocumentFlags::BLUE_OFFER
                          | NMarket::NDocumentFlags::CPA
                          | NMarket::NDocumentFlags::POST_TERM
                          | NMarket::NDocumentFlags::HAS_PICTURES
                          | NMarket::NDocumentFlags::IS_PUSH_PARTNER
                          | NMarket::NDocumentFlags::DOWNLOADABLE
                          | NMarket::NDocumentFlags::MANUFACTURER_WARRANTY
                          | NMarket::NDocumentFlags::ALCOHOL;

    auto enriched = new Market::UltraControllerServiceData::EnrichedOffer();
    enriched->set_classifier_category_id(10); // To make it nonempty
    enriched->set_vendor_id(123); // Для проверки, что raw_vendor не заполняется, если УК нашел глобального вендора

    Market::DataCamp::Offer datacampOffer;

    auto identifiers = datacampOffer.mutable_identifiers();
    auto extra = identifiers->mutable_extra();
    auto content = datacampOffer.mutable_content();
    auto partner = content->mutable_partner();
    auto master_data = content->mutable_master_data();
    auto partner_content_desc = partner->mutable_partner_content_desc();
    auto partner_actual_content = partner->mutable_actual();
    auto partner_actual_category = partner_actual_content->mutable_category();
    auto partner_actual_params = partner_actual_content->mutable_offer_params();
    auto approved = content->mutable_binding()->mutable_approved();
    auto pictures = datacampOffer.mutable_pictures();
    auto partner_pictures = pictures->mutable_partner();
    auto price = datacampOffer.mutable_price();
    auto delivery = datacampOffer.mutable_delivery();
    auto market = content->mutable_market();
    auto dimensions = market->mutable_dimensions();
    auto delivery_info = delivery->mutable_delivery_info();
    auto specific_delivery = delivery->mutable_specific();
    auto delivery_calculator = delivery->mutable_calculator();
    auto market_delivery = delivery->mutable_market();
    auto actual_delivery = delivery->mutable_partner()->mutable_actual();
    auto meta = datacampOffer.mutable_meta();
    auto status = datacampOffer.mutable_status();
    auto partner_info = datacampOffer.mutable_partner_info();

    TString offerParamsString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<offer_params>";
    MarketIndexer::GenerationLog::Record offer;

    offer.set_ware_md5("ware_md5");
    extra->set_ware_md5("ware_md5");
    offer.set_description("description");
    partner_content_desc->set_description("description");
    partner_actual_content->mutable_description()->set_value("description");
    offer.set_feed_id(1);
    offer.set_supplier_feed_id(1);
    identifiers->set_feed_id(1);
    offer.set_warehouse_id(2);
    identifiers->set_warehouse_id(2);
    offer.Setclassifier_magic_id("classifier_magic_id2");
    extra->set_classifier_magic_id2("classifier_magic_id2");
    offer.Setclassifier_good_id("classifier_good_id");
    extra->set_classifier_good_id("classifier_good_id");
    offer.Setdirect_feed_id(std::numeric_limits<std::int64_t>::max());
    extra->set_direct_feed_id(std::numeric_limits<std::int64_t>::max());
    offer.set_client_id(std::numeric_limits<std::int32_t>::max());
    extra->set_client_id(std::numeric_limits<std::int32_t>::max());
    offer.set_original_sku("original_sku");
    extra->set_original_sku("original_sku");

    offer.set_url("url");
    partner_content_desc->set_url("url");
    partner_actual_content->mutable_url()->set_value("url");
    offer.set_picture_url("http://test_url.test/test_pic1/");
    for (const auto url : {"http://test_url.test/test_pic1/", "https://test_url.test/test_pic2/"}) {
        auto* source = partner_pictures->mutable_original()->add_source();
        source->set_url(url);
        source->set_source(Market::DataCamp::PictureSource::DIRECT_LINK);
        offer.Addpic_url_ids("");


        offer.add_picture_urls(url);
    }

    offer.set_title("title");
    partner_content_desc->set_title("title");
    partner_actual_content->mutable_title()->set_value("title");
    offer.set_offer_id("yx_shop_offer_id");
    identifiers->set_offer_id("yx_shop_offer_id");
    offer.set_shop_id(2);
    identifiers->set_shop_id(2);
    offer.set_shop_category_path("shop_category_path");
    partner_actual_category->set_path_category_names("shop_category_path");
    offer.set_shop_category_path_ids("shop_category_path_ids: 1//2//3");
    partner_actual_category->set_path_category_ids("shop_category_path_ids: 1//2//3");
    offer.set_shop_category_id("3");
    partner_actual_category->set_id(3);
    offer.set_barcode("barcode1|barcode2");
    partner_actual_content->mutable_barcode()->add_value("barcode1");
    partner_actual_content->mutable_barcode()->add_value("barcode2");
    offer.set_sales_notes("sales_notes");
    partner_content_desc->set_sales_notes("sales_notes");
    partner_actual_content->mutable_sales_notes()->set_value("sales_notes");
    offer.set_shop_sku("shop_sku");
    extra->set_shop_sku("shop_sku");
    offer.set_market_sku(3);
    approved->set_market_sku_id(3);
    offer.set_market_sku_type(Market::DataCamp::Mapping::MARKET_SKU_TYPE_MSKU);
    approved->set_market_sku_type(Market::DataCamp::Mapping::MARKET_SKU_TYPE_MSKU);
    market->set_allocated_enriched_offer(enriched);
    offer.set_is_blue_offer(true);
    offer.Setflags(offerFlags);  // default mask for blue offers + adult (2500150171)
    partner_content_desc->set_is_cut_price(false);
    meta->set_rgb(Market::DataCamp::MarketColor::BLUE);
    (*meta->mutable_platforms())[Market::DataCamp::MarketColor::BLUE] = true;
    offer.set_manufacturer_country_ids("1,2,3");
    master_data->mutable_manufacturer_countries()->add_countries()->set_geo_id(1);
    master_data->mutable_manufacturer_countries()->add_countries()->set_geo_id(2);
    master_data->mutable_manufacturer_countries()->add_countries()->set_geo_id(3);
    offer.set_supplier_description("description");
    partner_content_desc->set_adult(true);
    partner_actual_content->mutable_adult()->set_flag(true);
    offer.set_age("18");
    partner_content_desc->set_age("18");
    partner_actual_content->mutable_age()->set_value(18);
    offer.set_age_unit("year");
    partner_content_desc->set_age_unit("year");
    partner_actual_content->mutable_age()->set_unit(Market::DataCamp::Age::YEAR);
    offer.set_downloadable(true);
    partner_content_desc->set_downloadable(true);
    partner_actual_content->mutable_downloadable()->set_flag(true);
    offer.set_flags(offer.flags() | NMarket::NDocumentFlags::MANUFACTURER_WARRANTY);
    partner_content_desc->set_manufacturer_warranty(true);
    partner_actual_content->mutable_manufacturer_warranty()->set_flag(true);
    TString expiryDateStr = "2015-09-19T23:59:00";
    offer.set_expiry(expiryDateStr);
    partner_content_desc->set_expiry(expiryDateStr);
    partner_actual_content->mutable_expiry()->mutable_datetime()->set_seconds(parsedatet(expiryDateStr.c_str()));
    offer.set_type_prefix("Product category");
    partner_content_desc->set_type_prefix("Product category");
    partner_actual_content->mutable_type_prefix()->set_value("Product category");
    offer.set_type(static_cast<int>(NMarket::EOfferType::ALCOHOL));
    partner_content_desc->set_type(static_cast<int>(NMarket::EOfferType::ALCOHOL));
    partner_actual_content->mutable_type()->set_value(NMarket::EProductType::ALCO);
    offer.set_seller_warranty("true");
    partner_content_desc->set_seller_warranty("true");
    partner_actual_content->mutable_seller_warranty()->set_has_warranty(true);
    offer.Setmin_quantity(10);
    partner_content_desc->set_min_quantity(10);
    partner_actual_content->mutable_quantity()->set_min(10);
    offer.Setstep_quantity(20);
    partner_content_desc->set_step_quantity(20);
    partner_actual_content->mutable_quantity()->set_step(20);

    offer.set_flags(offer.flags() | NMarket::NDocumentFlags::IS_RESALE);
    partner_actual_content->mutable_is_resale()->set_flag(true);
    // TODO (polarman): add other resale fields

    offer.set_flags(offer.flags() | NMarket::NDocumentFlags::ADULT);
    offer.set_weight(1.1);
    offer.set_length(2.2);
    offer.set_width(3.3);
    offer.set_height(4.4);
    dimensions->set_weight(1.1);
    dimensions->set_length(2.2);
    dimensions->set_width(3.3);
    dimensions->set_height(4.4);
    partner_actual_content->mutable_weight()->set_grams(1100);
    partner_actual_content->mutable_dimensions()->set_length_mkm(22000);
    partner_actual_content->mutable_dimensions()->set_width_mkm(33000);
    partner_actual_content->mutable_dimensions()->set_height_mkm(44000);
    auto weight_param = partner_actual_params->add_param();
    weight_param->set_name("delivery_weight");
    weight_param->set_unit("кг");
    weight_param->set_value("1.1");
    offerParamsString += "<param name=\"delivery_weight\" unit=\"кг\">1.1</param>";
    auto length_param = partner_actual_params->add_param();
    length_param->set_name("delivery_length");
    length_param->set_unit("см");
    length_param->set_value("2.2");
    offerParamsString += "<param name=\"delivery_length\" unit=\"см\">2.2</param>";
    auto width_param = partner_actual_params->add_param();
    width_param->set_name("delivery_width");
    width_param->set_unit("см");
    width_param->set_value("3.3");
    offerParamsString += "<param name=\"delivery_width\" unit=\"см\">3.3</param>";
    auto height_param = partner_actual_params->add_param();
    height_param->set_name("delivery_height");
    height_param->set_unit("см");
    height_param->set_value("4.4");
    offerParamsString += "<param name=\"delivery_height\" unit=\"см\">4.4</param>";
    partner_actual_content->mutable_price_from()->set_flag(false);
    partner_actual_content->mutable_vendor()->set_value("Vendor");
    offer.set_vendor("Vendor"); // raw_vendor не должно быть, т.к нашелся глобальный вендор
    auto vendor_param = partner_actual_params->add_param();
    vendor_param->set_name("vendor");
    vendor_param->set_value("Vendor");
    offerParamsString += "<param name=\"vendor\">Vendor</param>";
    offerParamsString += "</offer_params>\n";

    offer.set_raw_offer_params(offerParamsString);

    for(size_t i = 0; i < 3; ++i) {
        auto* groupFrom = partner_actual_content->mutable_installment_options()->mutable_options_groups()->Add();
        auto* groupTo = offer.mutable_installment_options()->Add();
        groupFrom->set_group_name("installment options group " + ToString(i));
        groupTo->set_group_name(groupFrom->group_name());
        for (uint32_t days: {30, 90}) {
            groupFrom->mutable_installment_time_in_days()->Add(days);
            groupTo->mutable_installment_time_in_days()->Add(days);
        }
        groupFrom->set_bnpl_available(i % 2);
        groupTo->set_bnpl_available(groupFrom->bnpl_available());
    }

    auto datacamp_basic_price = price->mutable_basic();

    auto binary_price = offer.mutable_binary_price();
    binary_price->set_price(4 * NMarket::NDataCamp::PRICE_PRECISION);
    binary_price->set_id("RUR");
    binary_price->set_ref_id("RUR");


    offer.set_ru_price(4);

    auto datacamp_binary_price = datacamp_basic_price->mutable_binary_price();
    datacamp_binary_price->set_price(4 * NMarket::NDataCamp::PRICE_PRECISION);
    datacamp_binary_price->set_id("RUR");

    auto binary_oldprice = offer.mutable_binary_unverified_oldprice();
    binary_oldprice->set_price(5 * NMarket::NDataCamp::PRICE_PRECISION);
    binary_oldprice->set_id("RUR");
    binary_oldprice->set_ref_id("RUR");
    auto datacamp_binary_oldprice = datacamp_basic_price->mutable_binary_oldprice();
    datacamp_binary_oldprice->set_price(5 * NMarket::NDataCamp::PRICE_PRECISION);
    datacamp_binary_oldprice->set_id("RUR");

    offer.set_vat(::Market::DataCamp::Vat::VAT_18);
    datacamp_basic_price->set_vat(::Market::DataCamp::Vat::VAT_20); // Игнорируем price.basic.vat, если заполнен actual_price_fields.vat.value
    price->mutable_actual_price_fields()->mutable_vat()->set_value(::Market::DataCamp::Vat::VAT_18);


    price->mutable_dynamic_pricing()->set_type(Market::DataCamp::DynamicPricing::RECOMMENDED_PRICE);
    offer.set_dynamic_pricing_type(Market::DataCamp::DynamicPricing::RECOMMENDED_PRICE);

    price->mutable_dynamic_pricing()->set_threshold_fixed_value(14568000);
    offer.set_dynamic_pricing_threshold_is_percent(false);
    offer.set_dynamic_pricing_threshold_value(14568000);

    offer.set_flags(offer.flags() | NMarket::NDocumentFlags::AVAILABLE);
    delivery_info->set_available(0);
    actual_delivery->mutable_available()->set_flag(false);
    offer.set_flags(offer.flags() | NMarket::NDocumentFlags::PICKUP);
    delivery_info->set_pickup(true);
    actual_delivery->mutable_pickup()->set_flag(true);
    offer.set_flags(offer.flags() | NMarket::NDocumentFlags::STORE);
    delivery_info->set_store(true);
    actual_delivery->mutable_store()->set_flag(true);

    offer.Setuse_yml_delivery(true);
    delivery_info->set_use_yml_delivery(true);
    market_delivery->mutable_use_yml_delivery()->set_flag(true);

    offer.Addcargo_types(30);
    market_delivery->Mutablecargo_type()->add_value(30);
    delivery_info->add_cargo_type(30);
    content->mutable_master_data()->mutable_cargo_type()->add_value(30);

    // specific delivery

    offer.Setdelivery_currency("RUR");
    specific_delivery->set_delivery_currency("RUR");
    actual_delivery->mutable_delivery_currency()->set_value("RUR");

    NMarketIndexer::Common::TDeliveryOption deliveryOptions;
    deliveryOptions.set_cost(123.4);
    deliveryOptions.set_daysmin(1);
    deliveryOptions.set_daysmax(2);
    deliveryOptions.set_orderbeforehour(13);

    offer.Addoffer_delivery_options()->CopyFrom(deliveryOptions);
    specific_delivery->add_delivery_options()->CopyFrom(deliveryOptions);
    actual_delivery->mutable_delivery_options()->add_options()->CopyFrom(deliveryOptions);

    NMarketIndexer::Common::TPickupOption pickupOptions;
    pickupOptions.set_cost(567.8);
    pickupOptions.set_daysmin(2);
    pickupOptions.set_daysmax(3);
    pickupOptions.set_orderbeforehour(14);

    offer.Addpickup_options()->CopyFrom(pickupOptions);
    specific_delivery->add_pickup_options()->CopyFrom(pickupOptions);
    actual_delivery->mutable_pickup_options()->add_options()->CopyFrom(pickupOptions);

    Market::OffersData::TOfferOutlet offerOutlet;
    offerOutlet.SetShopPointId("ya id");

    offer.AddOutlets()->CopyFrom(offerOutlet);
    specific_delivery->add_outlets()->CopyFrom(offerOutlet);
    actual_delivery->mutable_outlets()->add_outlets()->CopyFrom(offerOutlet);

    // delivery calculator

    offer.SetDeliveryCalcGeneration(123);
    delivery_calculator->set_delivery_calc_generation(123);

    offer.add_mbi_delivery_bucket_ids(222);
    delivery_calculator->add_delivery_bucket_ids(222);

    Market::OffersData::DeliveryCalc deliveryCalc;
    deliveryCalc.set_id(111);

    delivery_calculator->add_fulfillment_delivery_calc()->CopyFrom(deliveryCalc);

    offer.add_mbi_pickup_bucket_ids(723);
    delivery_calculator->add_pickup_bucket_ids(723);

    offer.add_mbi_post_bucket_ids(109);
    delivery_calculator->add_post_bucket_ids(109);

    delivery_calc::mbi::BucketInfo bucketInfo;
    bucketInfo.set_bucket_id(100500);
    bucketInfo.add_cost_modifiers_ids(1);
    bucketInfo.add_cost_modifiers_ids(2);

    delivery_calc::mbi::OffersDeliveryInfo offerDeliveryInfo;
    auto* offer_delivery_info = offer.mutable_offers_delivery_info();
    offer_delivery_info->add_courier_buckets_info()->CopyFrom(bucketInfo);
    offer_delivery_info->add_pickup_buckets_info()->CopyFrom(bucketInfo);

    delivery_calculator->add_courier_buckets_info()->CopyFrom(bucketInfo);
    delivery_calculator->add_pickup_buckets_info()->CopyFrom(bucketInfo);

    market_delivery->mutable_calculator()->CopyFrom(*delivery_calculator);

    // partner_info
    offer.set_cpa(4);
    offer.set_flags(offer.flags() | NMarket::NDocumentFlags::AUTOBROKER_ENABLED);

    partner_info->set_cpa(4);
    status->mutable_original_cpa()->set_flag(true);
    status->mutable_actual_cpa()->set_flag(true);
    status->mutable_available_for_businesses()->set_flag(false);
    status->mutable_prohibited_for_persons()->set_flag(false);
    partner_info->set_autobroker_enabled(true);
    partner_info->set_is_blue_offer(true);
    partner_info->set_is_fulfillment(true);

    offer.set_has_gone(false);
    offer.set_disabled_flags(0);
    offer.set_disabled_flag_sources(0);
    offer.set_is_fast_sku(false);
    partner_info->set_is_lavka(false);
    partner_info->set_is_express(false);
    partner_info->set_is_eda(false);

    // goods_sm_mapping - не полжен повлиять при отсутствии флага
    auto goods_sm_mapping = datacampOffer.mutable_content()->mutable_binding()->mutable_goods_sm_mapping();
    goods_sm_mapping->mutable_vendor_id()->set_value(111);
    goods_sm_mapping->mutable_vendor_name()->set_value("Vendor111");
    goods_sm_mapping->mutable_mapping()->set_market_category_id(222);
    goods_sm_mapping->mutable_mapping()->set_market_model_id(333);
    goods_sm_mapping->mutable_mapping()->set_market_sku_id(444);
    goods_sm_mapping->mutable_mapping()->set_market_sku_type(Market::DataCamp::Mapping::MARKET_SKU_TYPE_PSKU);

    auto goods_cvdups_mapping = datacampOffer.mutable_content()->mutable_binding()->mutable_goods_cvdups_mapping();
    goods_cvdups_mapping->set_model_id(121);
    goods_cvdups_mapping->set_sku_id(131);
    offer.mutable_goods_cvdups_mapping()->set_model_id(121);
    offer.mutable_goods_cvdups_mapping()->set_sku_id(131);

    auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

    ASSERT_PB_EQ(offer, actualOffer);
}

TEST (ConversionsTest, testDeliveryFallback)
{
    Market::DataCamp::Offer datacampOfferOld;
    Market::DataCamp::Offer datacampOffer;
    datacampOfferOld.mutable_identifiers()->set_offer_id("offer_id");
    datacampOffer.mutable_identifiers()->set_offer_id("offer_id");

    auto delivery_old = datacampOfferOld.mutable_delivery();
    auto delivery_info_old = delivery_old->mutable_delivery_info();
    auto specific_delivery_old = delivery_old->mutable_specific();
    auto delivery_calculator_old = delivery_old->mutable_calculator();

    auto delivery = datacampOffer.mutable_delivery();
    auto delivery_calculator = delivery->mutable_calculator();
    auto market_delivery = delivery->mutable_market();
    auto actual_delivery = delivery->mutable_partner()->mutable_actual();

    delivery_info_old->set_available(0);
    actual_delivery->mutable_available()->set_flag(false);
    delivery_info_old->set_pickup(true);
    actual_delivery->mutable_pickup()->set_flag(true);
    delivery_info_old->set_store(true);
    actual_delivery->mutable_store()->set_flag(true);
    delivery_info_old->set_has_delivery(true);
    actual_delivery->mutable_delivery()->set_flag(true);

    delivery_info_old->set_use_yml_delivery(true);
    market_delivery->mutable_use_yml_delivery()->set_flag(true);

    delivery_info_old->add_cargo_type(30);
    market_delivery->mutable_cargo_type()->add_value(30);

    // specific delivery

    specific_delivery_old->set_delivery_currency("RUR");
    actual_delivery->mutable_delivery_currency()->set_value("RUR");

    NMarketIndexer::Common::TDeliveryOption deliveryOptions;
    deliveryOptions.set_cost(123.4);
    deliveryOptions.set_daysmin(1);
    deliveryOptions.set_daysmax(2);
    deliveryOptions.set_orderbeforehour(13);

    specific_delivery_old->add_delivery_options()->CopyFrom(deliveryOptions);
    actual_delivery->mutable_delivery_options()->add_options()->CopyFrom(deliveryOptions);

    NMarketIndexer::Common::TPickupOption pickupOptions;
    pickupOptions.set_cost(567.8);
    pickupOptions.set_daysmin(2);
    pickupOptions.set_daysmax(3);
    pickupOptions.set_orderbeforehour(14);

    specific_delivery_old->add_pickup_options()->CopyFrom(pickupOptions);
    actual_delivery->mutable_pickup_options()->add_options()->CopyFrom(pickupOptions);

    Market::OffersData::TOfferOutlet offerOutlet;
    offerOutlet.SetShopPointId("ya id");

    specific_delivery_old->add_outlets()->CopyFrom(offerOutlet);
    actual_delivery->mutable_outlets()->add_outlets()->CopyFrom(offerOutlet);

    // delivery calculator

    delivery_calculator_old->set_delivery_calc_generation(123);
    delivery_calculator_old->add_delivery_bucket_ids(222);

    Market::OffersData::DeliveryCalc deliveryCalc;
    deliveryCalc.set_id(111);
    delivery_calculator->add_fulfillment_delivery_calc()->CopyFrom(deliveryCalc);
    delivery_calculator->add_pickup_bucket_ids(723);
    delivery_calculator->add_post_bucket_ids(109);

    delivery_calc::mbi::BucketInfo bucketInfo;
    bucketInfo.set_bucket_id(100500);
    bucketInfo.add_cost_modifiers_ids(1);
    bucketInfo.add_cost_modifiers_ids(2);
    delivery_calculator->add_courier_buckets_info()->CopyFrom(bucketInfo);
    delivery_calculator->add_pickup_buckets_info()->CopyFrom(bucketInfo);

    delivery_calculator_old->CopyFrom(*delivery_calculator);

    auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
    auto actualOfferOld = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOfferOld);

    ASSERT_PB_EQ(actualOffer, actualOfferOld);
}

TEST (ConversionsTest, testVerticalApproved)
{
    Market::DataCamp::Offer offerFlagTrue;
    offerFlagTrue.mutable_identifiers()->set_offer_id("offer_id");
    offerFlagTrue.mutable_meta()->mutable_vertical_approved_flag()->set_flag(true);
    auto convertedOfferFlagTrue = NMarket::NDataCamp::DataCamp2GenerationLog(offerFlagTrue);
    ASSERT_TRUE(convertedOfferFlagTrue.vertical_approved());

    Market::DataCamp::Offer offerFlagFalse;
    offerFlagFalse.mutable_identifiers()->set_offer_id("offer_id");
    offerFlagFalse.mutable_meta()->mutable_vertical_approved_flag()->set_flag(false);
    auto convertedOfferFlagFalse = NMarket::NDataCamp::DataCamp2GenerationLog(offerFlagFalse);
    ASSERT_TRUE(!convertedOfferFlagFalse.vertical_approved());

    Market::DataCamp::Offer offerEmptyFlag;
    offerEmptyFlag.mutable_identifiers()->set_offer_id("offer_id");
    auto convertedEmptyFlag = NMarket::NDataCamp::DataCamp2GenerationLog(offerEmptyFlag);
    ASSERT_TRUE(!convertedEmptyFlag.has_vertical_approved());
}


TEST (ConversionsTest, testWeightFallback)
{
    Market::DataCamp::Offer offerOld;
    Market::DataCamp::Offer offerNew;

    offerOld.mutable_meta()->set_rgb(Market::DataCamp::MarketColor::WHITE);
    (*offerOld.mutable_meta()->mutable_platforms())[Market::DataCamp::MarketColor::WHITE] = true;
    offerNew.mutable_meta()->set_rgb(Market::DataCamp::MarketColor::WHITE);
    (*offerNew.mutable_meta()->mutable_platforms())[Market::DataCamp::MarketColor::WHITE] = true;
    offerOld.mutable_identifiers()->set_offer_id("offer_id");
    offerNew.mutable_identifiers()->set_offer_id("offer_id");

    // при конвертации в кг граммов из старого оффера и мг из нового получим одинаковый вес
    offerOld.mutable_content()->mutable_partner()->mutable_actual()->mutable_weight()->set_grams(1100);
    offerNew.mutable_content()->mutable_partner()->mutable_actual()->mutable_weight()->set_grams(5400);
    offerNew.mutable_content()->mutable_partner()->mutable_actual()->mutable_weight()->set_value_mg(1100000);

    auto convertedOffer = NMarket::NDataCamp::DataCamp2GenerationLog(offerNew);
    auto convertedOfferOld = NMarket::NDataCamp::DataCamp2GenerationLog(offerOld);

    ASSERT_PB_EQ(convertedOffer, convertedOfferOld);
}

TEST (ConversionsTest, testMergeCargoTypes)
{
    Market::DataCamp::Offer datacampOffer;

    auto content = datacampOffer.mutable_content();
    auto partner = content->mutable_partner();
    auto partner_actual_content = partner->mutable_actual();
    auto delivery = datacampOffer.mutable_delivery();
    auto market_delivery = delivery->mutable_market();

    datacampOffer.mutable_identifiers()->set_feed_id(1); // с пустым identifiers конвертации не будет

    // Market cargo types
    market_delivery->Mutablecargo_type()->add_value(30);

    // Partner cargo types
    partner_actual_content->Mutablecargo_types()->add_value(950);
    partner_actual_content->Mutablecargo_types()->add_value(980);

    auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

    // Should be merged to the united cargo types list
    ASSERT_EQ(actualOffer.cargo_types().size(), 3);
    ASSERT_EQ(actualOffer.cargo_types(0), 30);
    ASSERT_EQ(actualOffer.cargo_types(1), 950);
    ASSERT_EQ(actualOffer.cargo_types(2), 980);
}

TEST (ConversionsTest, testCargoTypesFallbackNotDsbs)
{
    Market::DataCamp::Offer datacampOfferOld;
    Market::DataCamp::Offer datacampOfferNew;

    datacampOfferOld.mutable_identifiers()->set_feed_id(1);
    datacampOfferNew.mutable_identifiers()->set_feed_id(1);
    datacampOfferOld.mutable_partner_info()->set_is_dsbs(false);
    datacampOfferNew.mutable_partner_info()->set_is_dsbs(false);

    // Delivery cargo types in old offer
    auto cargoTypesOld = datacampOfferOld.mutable_delivery()->mutable_market()->mutable_cargo_type();
    cargoTypesOld->add_value(30);

    // Master data cargo types in new offer
    auto cargoTypesNew = datacampOfferNew.mutable_content()->mutable_master_data()->mutable_cargo_type();
    cargoTypesNew->add_value(30);
    // old-fashioned cargo_types in offer with cargo_types in master_data
    // we wont use old-fashioned
    auto cargoTypesOldInNew = datacampOfferNew.mutable_delivery()->mutable_market()->mutable_cargo_type();
    cargoTypesOldInNew->add_value(130);

    auto convertedOfferNew = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOfferNew);
    auto convertedOfferOld = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOfferOld);

    ASSERT_PB_EQ(convertedOfferNew, convertedOfferOld);
}

TEST (ConversionsTest, testCargoTypesFallbackForDsbs)
{
    Market::DataCamp::Offer datacampOfferOld;
    Market::DataCamp::Offer datacampOfferNew;

    datacampOfferOld.mutable_identifiers()->set_feed_id(1);
    datacampOfferNew.mutable_identifiers()->set_feed_id(1);
    datacampOfferOld.mutable_partner_info()->set_is_dsbs(true);
    datacampOfferNew.mutable_partner_info()->set_is_dsbs(true);

    // Delivery cargo types in old offer
    auto cargoTypesOld = datacampOfferOld.mutable_delivery()->mutable_market()->mutable_cargo_type();
    cargoTypesOld->add_value(30);

    // Master data cargo types in new offer
    auto cargoTypesNew = datacampOfferNew.mutable_content()->mutable_master_data()->mutable_cargo_type();
    cargoTypesNew->add_value(1130);
    // old-fashioned cargo_types in offer with cargo_types in master_data
    // we will use old-fashioned cargo_types for dsbs
    auto cargoTypesOldInNew = datacampOfferNew.mutable_delivery()->mutable_market()->mutable_cargo_type();
    cargoTypesOldInNew->add_value(30);

    auto convertedOfferOld = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOfferOld);
    auto convertedOfferNew = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOfferNew);

    ASSERT_PB_EQ(convertedOfferNew, convertedOfferOld);
}

TEST (ConversionsTest, testDirect)
{
    Market::DataCamp::Offer datacampOffer;

    datacampOffer.mutable_identifiers()->set_feed_id(1); // с пустым identifiers конвертации не будет

    // Direct verdicts
    auto* resolution = datacampOffer.mutable_resolution();
    auto* direct = resolution->mutable_direct();
    auto* bigmod_verdict = direct->mutable_bigmod_verdict();
    bigmod_verdict->AddFlags(::NBigmodMarkup::EMarkupFlag::ALCOHOL);
    bigmod_verdict->AddFlags(::NBigmodMarkup::EMarkupFlag::TOBACCO);
    bigmod_verdict->AddMinusRegions(10000);
    bigmod_verdict->AddMinusRegions(-225);

    auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

    // Should be merged to the united cargo types list
    ASSERT_EQ(actualOffer.direct_moderation_flags2().size(), 2);
    ASSERT_EQ(actualOffer.direct_moderation_flags2(0), 5003);
    ASSERT_EQ(actualOffer.direct_moderation_flags2(1), 5004);
    ASSERT_EQ(actualOffer.direct_regions2().size(), 2);
    ASSERT_EQ(actualOffer.direct_regions2(0), 10000);
    ASSERT_EQ(actualOffer.direct_regions2(1), -225);
}

TEST (ConversionsTest, testAmoreData)
{
    auto AmoreDataFromCpa = [](uint32_t uid, uint16_t fee) {
        using namespace NMarket::NAmore;
        using namespace NMarket::NAmore::NStrategy;

        TString ret;
        ret.resize(NStrategy::StrategyBundleSize);
        NMSMapper::BinaryData binData{(int8_t*)ret.data(), ret.size()};
        const TStrategyBundle bundle{uid, NStrategy::TCpa{StrategyType::CPA, fee}, TOfferStrategy{}};
        NMSMapper::Mapper::MapAmoreBundle(bundle, binData);
        return ret;
    };

    Market::DataCamp::Offer datacampOffer;
    const auto amoreData = AmoreDataFromCpa(111, 333);
    datacampOffer.mutable_bids()->mutable_amore_data()->set_value(amoreData);
    auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
    ASSERT_EQ(actualOffer.Getfee(), 333);
}

TEST (ConversionsTest, testEmptyAmoreData)
{
    {
        Market::DataCamp::Offer datacampOffer;
        datacampOffer.mutable_bids()->mutable_amore_data()->set_value("amoreData");
        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_EQ(actualOffer.Getfee(), 0);
    }

    {
        Market::DataCamp::Offer datacampOffer;
        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);
        ASSERT_EQ(actualOffer.Getfee(), 0);
    }
}

TEST (ConversionsTest, testPicUrlIds)
{
    Market::DataCamp::Offer datacampOffer;

    datacampOffer.mutable_identifiers()->set_feed_id(1); // с пустым identifiers конвертации не будет
    auto pictures = datacampOffer.mutable_pictures();
    auto partner_pictures = pictures->mutable_partner();
    auto actual = partner_pictures->mutable_actual();
    auto original = partner_pictures->mutable_original();
    for (const auto id : {"pic4e539a3dffc5d169c714e004651f6646", "pic4e539a3dffc5d169c714e004651f6645"})
    {
        Market::DataCamp::MarketPicture pic;
        Market::DataCamp::SourcePicture source;
        pic.set_id(id);
        pic.set_status(Market::DataCamp::MarketPicture::AVAILABLE);
        Market::DataCamp::UpdateMeta meta;
        meta.set_applier(NMarketIndexer::Common::NEW_PICROBOT);
        pic.mutable_meta()->CopyFrom(meta);
        google::protobuf::MapPair<TString, Market::DataCamp::MarketPicture> mvalue(id, pic);
        actual->insert(mvalue);
        source.set_url(id);
        original->mutable_source()->Add()->CopyFrom(source);
    }

    auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

    ASSERT_EQ(actualOffer.Getpic_url_ids().size(), 2);
    TSet<TString> expectedIds = {"pic4e539a3dffc5d169c714e004651f6645", "pic4e539a3dffc5d169c714e004651f6646"};
    TSet<TString> actualIds;
    actualIds.insert(actualOffer.Getpic_url_ids(0));
    actualIds.insert(actualOffer.Getpic_url_ids(1));
    ASSERT_EQ(actualIds, expectedIds);
}

TEST (ConversionsTest, testVatIfNoActualPriceFields)
{
    Market::DataCamp::Offer datacampOffer;

    datacampOffer.mutable_meta()->set_rgb(Market::DataCamp::MarketColor::WHITE);
    (*datacampOffer.mutable_meta()->mutable_platforms())[Market::DataCamp::MarketColor::WHITE] = true;

    datacampOffer.mutable_identifiers()->set_feed_id(1); // с пустым identifiers конвертации не будет

    datacampOffer.mutable_price()->mutable_basic()->set_vat(::Market::DataCamp::Vat::VAT_20);
    // actual_price_fields is not filled in

    auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

    ASSERT_EQ(int(actualOffer.vat()), int(::Market::DataCamp::Vat::VAT_20));
}


TEST (ConversionsTest, testUseActualPriceFieldsForLavkaEda)
{
    auto checkActualPriceFieldsVat = [](Market::DataCamp::MarketColor color) {
        Market::DataCamp::Offer datacampOffer;

        datacampOffer.mutable_meta()->set_rgb(color);
        (*datacampOffer.mutable_meta()->mutable_platforms())[color] = true;

        datacampOffer.mutable_identifiers()->set_feed_id(1); // с пустым identifiers конвертации не будет

        datacampOffer.mutable_price()->mutable_actual_price_fields()->mutable_vat()->set_value(::Market::DataCamp::Vat::VAT_18);
        // price.basic.vat is not filled in

        auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

        ASSERT_EQ(int(actualOffer.vat()), int(::Market::DataCamp::Vat::VAT_18));
    };

    checkActualPriceFieldsVat(Market::DataCamp::MarketColor::LAVKA);
    checkActualPriceFieldsVat(Market::DataCamp::MarketColor::EDA);
}

TEST (ConversionsTest, testGoodsSMMappingDataCamp2GenerationLogHasMarketSku)
{
    Market::DataCamp::Offer datacampOffer;

    // goods sm data
    auto goods_sm_mapping = datacampOffer.mutable_content()->mutable_binding()->mutable_goods_sm_mapping();
    goods_sm_mapping->mutable_vendor_id()->set_value(111);
    goods_sm_mapping->mutable_vendor_name()->set_value("Vendor111");
    goods_sm_mapping->mutable_mapping()->set_market_category_id(222);
    goods_sm_mapping->mutable_mapping()->set_market_model_id(333);
    goods_sm_mapping->mutable_mapping()->set_market_sku_id(444);
    goods_sm_mapping->mutable_mapping()->set_market_sku_type(Market::DataCamp::Mapping::MARKET_SKU_TYPE_PSKU);

    // market data
    auto content = datacampOffer.mutable_content();
    auto approved = content->mutable_binding()->mutable_approved();
    approved->set_market_sku_id(3);
    approved->set_market_sku_type(Market::DataCamp::Mapping::MARKET_SKU_TYPE_FAST);

    // expected
    MarketIndexer::GenerationLog::Record offer;
    offer.set_market_sku(3);
    offer.set_sku_source(MarketIndexer::GenerationLog::Record_ESkuSource_DEFAULT);
    offer.set_market_sku_type(Market::DataCamp::Mapping::MARKET_SKU_TYPE_FAST);
    offer.set_is_fast_sku(true);

    auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer, false, true);

    ASSERT_PB_EQ(offer, actualOffer);
}

TEST (ConversionsTest, testGoodsSMMappingDataCamp2GenerationLogNoMarketSku)
{
    Market::DataCamp::Offer datacampOffer;

    // goods sm data
    auto goods_sm_mapping = datacampOffer.mutable_content()->mutable_binding()->mutable_goods_sm_mapping();
    goods_sm_mapping->mutable_vendor_id()->set_value(111);
    goods_sm_mapping->mutable_vendor_name()->set_value("Vendor111");
    goods_sm_mapping->mutable_mapping()->set_market_category_id(222);
    goods_sm_mapping->mutable_mapping()->set_market_model_id(333);
    goods_sm_mapping->mutable_mapping()->set_market_sku_id(444);
    goods_sm_mapping->mutable_mapping()->set_market_sku_type(Market::DataCamp::Mapping::MARKET_SKU_TYPE_PSKU);

    // expected
    MarketIndexer::GenerationLog::Record offer;
    offer.set_market_sku(444);
    offer.set_vendor_id(111);
    offer.set_vendor_name("Vendor111");
    offer.set_category_id(222);
    offer.set_model_id(333);
    offer.set_sku_source(MarketIndexer::GenerationLog::Record_ESkuSource_EXTERNAL);
    offer.set_market_sku_type(Market::DataCamp::Mapping::MARKET_SKU_TYPE_PSKU);

    auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer, false, true);

    ASSERT_PB_EQ(offer, actualOffer);
}

TEST (ConversionsTest, testIsUnfamilyFlagMapping)
{
    Market::DataCamp::Offer datacampOffer;
    datacampOffer.mutable_content()->mutable_partner()->mutable_actual()->mutable_is_unfamily()->set_flag(true);

    auto actualOffer = NMarket::NDataCamp::DataCamp2GenerationLog(datacampOffer);

    ASSERT_EQ(actualOffer.is_unfamily(), true);
}
