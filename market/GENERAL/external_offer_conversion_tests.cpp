#include <market/idx/datacamp/lib/conversion/OfferConversions.h>

#include <contrib/libs/protobuf/src/google/protobuf/util/message_differencer.h>

#include <library/cpp/protobuf/json/proto2json.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <google/protobuf/util/time_util.h>

TEST(ExternalConversionTest, FromToTest)
{
    google::protobuf::Timestamp timestamp = google::protobuf::Timestamp();

    Market::DataCamp::External::Offer original;
    Market::DataCamp::UpdateMeta meta;

    *meta.mutable_timestamp() = timestamp;
    meta.set_source(Market::DataCamp::DataSource::PUSH_PARTNER_FEED);

    *original.mutable_timestamp() = timestamp;
    *original.mutable_creation_timestamp() = timestamp;

    original.set_business_id(1);
    original.set_offer_id("offer_id");
    original.set_shop_id(2);
    original.set_feed_id(3);
    original.set_offer_yabs_id(4);
    original.set_direct_feed_id(27);
    original.set_client_id(28);
    original.set_ware_md5("WAREMD5");
    original.set_original_sku("original_sku");

    auto& price = *original.mutable_price();
    price.set_currency(Market::DataCamp::RUR);
    price.set_price(100500);
    price.set_old_price(100505);
    // ToDo: price.set_purchase_price(100000);

    for(const auto url : {"http://test_url.test/test_pic1/", "https://test_url.test/test_pic2/"}) {
        auto* picture = original.add_original_pictures();
        picture->set_url(url);
        picture->set_source(Market::DataCamp::PictureSource::DIRECT_LINK);
    }

    auto& original_content = *original.mutable_original_content();
    original_content.set_shop_model("model");
    original_content.set_shop_vendor("vendor");
    original_content.set_type_prefix("type_prefix");
    original_content.set_shop_vendor_code("shop_vendor_code");
    original_content.set_name("name");
    original_content.set_url("url");

    original_content.set_shop_category_id(12345);
    original_content.set_shop_category_name("shop_category_name");
    original_content.set_shop_category_path_ids("shop_category_path_ids");
    original_content.set_shop_category_path("shop_category_path");
    // ToDo:
    //original_content.set_supplier("supplier");
    //original_content.set_sales_notes("sales_notes");
    //quantity
    original_content.set_description("description");

    auto& service = *original.mutable_service();
    service.set_platform(Market::DataCamp::Consumer::Platform::DIRECT);
    service.set_preview(true);
    service.set_data_source(Market::DataCamp::DataSource::PUSH_PARTNER_SITE);
    //service.set_cpa(true);
    //service.set_cpc(false);


    original_content.set_manufacturer_warranty(true);
    original_content.set_adult(true);
    original_content.set_available(true);
    for(const auto barcode : {"111111", "2222222"}) {
        *original_content.add_barcode() = barcode;
    }
    // ToDo:
    auto param1 = original_content.add_params();
    param1->set_name("color");
    param1->set_value("black");
    auto param2 = original_content.add_params();
    param2->set_name("gender");
    param2->set_value("woman");
    //ToDo:
    //expiry
    original_content.set_brutto_weight_in_grams(150);
    original_content.mutable_brutto_dimensions()->set_height_mkm(100);
    original_content.mutable_brutto_dimensions()->set_length_mkm(200);
    original_content.mutable_brutto_dimensions()->set_width_mkm(150);
    original_content.set_downloadable(true);

    Market::DataCamp::Flag disabled_flag;
    disabled_flag.set_flag(true);
    disabled_flag.mutable_meta()->set_source(Market::DataCamp::DataSource::PUSH_PARTNER_SITE);
    (*original.mutable_disable_status())[static_cast<NProtoBuf::uint32>(disabled_flag.meta().source())] = disabled_flag;

    //ToDo:
    //seller_warranty
    //age
    //condition
    //original_regions_info

    Market::DataCamp::External::Offer converted;
    converted = NMarket::NDataCamp::DataCampOffer2External(
        NMarket::NDataCamp::External2DataCampOffer(
            original,
            Market::DataCamp::MarketColor::DIRECT,
            Market::DataCamp::DataSource::PUSH_PARTNER_SITE
        ),
        timestamp
    );

    google::protobuf::util::MessageDifferencer differencer;
    TString strDiff;
    differencer.ReportDifferencesToString(&strDiff);
    differencer.Compare(original, converted);

    ASSERT_EQ(strDiff, TString{});
}

TEST (ExternalConversionTest, OriginalVideos)
{
    Market::DataCamp::Offer offer;
    google::protobuf::Timestamp timestamp = google::protobuf::Timestamp();
    offer.mutable_pictures()->mutable_videos()->mutable_source()->add_videos()->set_url("video1.url");
    offer.mutable_pictures()->mutable_videos()->mutable_source()->add_videos()->set_url("video2.url");
    auto external = NMarket::NDataCamp::DataCampOffer2External(offer, timestamp);

    ASSERT_EQ(external.original_videos().size(), 2);
    ASSERT_EQ(external.original_videos()[0].url(), "video1.url");
    ASSERT_EQ(external.original_videos()[1].url(), "video2.url");
}

TEST (ExternalConversionTest, ActualVideos)
{
    Market::DataCamp::Offer offer;
    google::protobuf::Timestamp timestamp = google::protobuf::Timestamp();
    auto video = (*(*offer.mutable_pictures()->mutable_videos()->mutable_actual())["video.url"].mutable_by_namespace())["direct"].mutable_video();
    video->set_id("12345");
    video->set_status(Market::DataCamp::ProcessedVideo::AVAILABLE);
    video->set_original_url("video.url");
    auto external = NMarket::NDataCamp::DataCampOffer2External(offer, timestamp);

    ASSERT_EQ(external.actual_videos().at("video.url").by_namespace().at("direct").id(), "12345");
    ASSERT_TRUE(external.actual_videos().at("video.url").by_namespace().at("direct").status() == Market::DataCamp::ProcessedVideo::AVAILABLE);
    ASSERT_EQ(external.actual_videos().at("video.url").by_namespace().at("direct").original_url(), "video.url");
}

TEST(ExternalConversionTest, Weight)
{
    Market::DataCamp::Offer offer;
    google::protobuf::Timestamp timestamp = google::protobuf::Timestamp();

    auto& weight = *offer.mutable_content()->mutable_partner()->mutable_original()->mutable_weight();
    *weight.mutable_meta()->mutable_timestamp() = timestamp;

    auto external = NMarket::NDataCamp::DataCampOffer2External(offer, timestamp);
    ASSERT_FALSE(external.original_content().has_brutto_weight_in_grams());

    weight.set_value_mg(12345);
    external = NMarket::NDataCamp::DataCampOffer2External(offer, timestamp);
    ASSERT_EQ(external.original_content().brutto_weight_in_grams(), 12);

    weight.clear_value_mg();
    weight.set_grams(567);
    external = NMarket::NDataCamp::DataCampOffer2External(offer, timestamp);
    ASSERT_EQ(external.original_content().brutto_weight_in_grams(), 567);

    weight.set_value_mg(12345);
    weight.set_grams(567);
    external = NMarket::NDataCamp::DataCampOffer2External(offer, timestamp);
    ASSERT_EQ(external.original_content().brutto_weight_in_grams(), 12);

}

TEST(ExternalConversionTest, TechInfo)
{
    Market::DataCamp::Offer offer;
    google::protobuf::Timestamp timestamp = google::protobuf::Timestamp();

    const auto feedTs = google::protobuf::util::TimeUtil::SecondsToTimestamp(1);
    const auto startTs = google::protobuf::util::TimeUtil::SecondsToTimestamp(2);
    const auto endTs = google::protobuf::util::TimeUtil::SecondsToTimestamp(3);

    offer.mutable_tech_info()->mutable_last_parsing()->mutable_feed_timestamp()->CopyFrom(feedTs);
    offer.mutable_tech_info()->mutable_last_parsing()->mutable_start_parsing()->CopyFrom(startTs);
    offer.mutable_tech_info()->mutable_last_parsing()->mutable_end_parsing()->CopyFrom(endTs);

    auto external = NMarket::NDataCamp::DataCampOffer2External(offer, timestamp);

    ASSERT_EQ(external.tech_info().last_parsing().feed_timestamp().seconds(), feedTs.seconds());
    ASSERT_EQ(external.tech_info().last_parsing().start_parsing().seconds(), startTs.seconds());
    ASSERT_EQ(external.tech_info().last_parsing().end_parsing().seconds(), endTs.seconds());
}

TEST(ExternalConversionTest, Navigation_External_To_DataCamp) {
    // arrange
    Market::DataCamp::External::Offer externalOffer;

    {
        auto path1 = externalOffer.add_navigation_paths();

        auto node11 = path1->add_nodes();
        node11->set_id(11);
        node11->set_name("Picnic");

        auto path2 = externalOffer.add_navigation_paths();

        auto node21 = path2->add_nodes();
        node21->set_id(21);
        node21->set_name("Meat");

        auto node22 = path2->add_nodes();
        node22->set_id(22);
        node22->set_name("Kebab");
    }

    // act
    Market::DataCamp::Offer dataCampOffer;
    NMarket::NDataCamp::External2DataCampOffer(externalOffer, dataCampOffer, Market::DataCamp::MarketColor::WHITE);

    // assert
    auto& navigation = dataCampOffer.content().binding().navigation();
    ASSERT_EQ(navigation.paths().size(), 2);

    auto& path1 = navigation.paths()[0];
    ASSERT_EQ(path1.nodes().size(), 1);

    EXPECT_EQ(path1.nodes()[0].id(), 11);
    EXPECT_EQ(path1.nodes()[0].name(), "Picnic");

    auto& path2 = navigation.paths()[1];
    ASSERT_EQ(path2.nodes().size(), 2);

    EXPECT_EQ(path2.nodes()[0].id(), 21);
    EXPECT_EQ(path2.nodes()[0].name(), "Meat");

    EXPECT_EQ(path2.nodes()[1].id(), 22);
    EXPECT_EQ(path2.nodes()[1].name(), "Kebab");
}

TEST(ExternalConversionTest, Breadcrumbs_External_To_DataCamp) {
    // arrange
    Market::DataCamp::External::Offer externalOffer;

    externalOffer.add_breadcrumbs("crumb1");
    externalOffer.add_breadcrumbs("crumb2");

    // act
    Market::DataCamp::Offer dataCampOffer;
    NMarket::NDataCamp::External2DataCampOffer(externalOffer, dataCampOffer, Market::DataCamp::MarketColor::WHITE);

    // assert
    auto& navigation = dataCampOffer.content().binding().navigation();
    ASSERT_EQ(navigation.paths().size(), 1);

    auto& path = navigation.paths()[0];
    ASSERT_EQ(path.nodes().size(), 2);

    EXPECT_EQ(path.nodes()[0].id(), 0);
    EXPECT_EQ(path.nodes()[0].name(), "crumb1");

    EXPECT_EQ(path.nodes()[1].id(), 0);
    EXPECT_EQ(path.nodes()[1].name(), "crumb2");
}

TEST(ExternalConversionTest, Navigation_DataCamp_To_External) {
    // arrange
    Market::DataCamp::Offer dataCampOffer;

    {
        auto navigation = dataCampOffer.mutable_content()->mutable_binding()->mutable_navigation();

        auto path1 = navigation->add_paths();

        auto node11 = path1->add_nodes();
        node11->set_id(11);
        node11->set_name("Meat");

        auto node12 = path1->add_nodes();
        node12->set_id(12);
        node12->set_name("Kebab");

        auto path2 = navigation->add_paths();

        auto node2 = path2->add_nodes();
        node2->set_id(21);
        node2->set_name("Picnic");
    }

    // act
    Market::DataCamp::External::Offer externalOffer;
    NMarket::NDataCamp::DataCampOffer2External(dataCampOffer, externalOffer, {});

    // assert
    const auto& navigation_paths = externalOffer.navigation_paths();
    ASSERT_EQ(navigation_paths.size(), 2);

    auto& path1 = navigation_paths[0];
    ASSERT_EQ(path1.nodes().size(), 2);

    EXPECT_EQ(path1.nodes()[0].id(), 11);
    EXPECT_EQ(path1.nodes()[0].name(), "Meat");

    EXPECT_EQ(path1.nodes()[1].id(), 12);
    EXPECT_EQ(path1.nodes()[1].name(), "Kebab");

    auto& path2 = navigation_paths[1];
    ASSERT_EQ(path2.nodes().size(), 1);

    EXPECT_EQ(path2.nodes()[0].id(), 21);
    EXPECT_EQ(path2.nodes()[0].name(), "Picnic");

    // check bread_crumbs were initialized from first nav path
    ASSERT_EQ(externalOffer.breadcrumbs().size(), 2);
    ASSERT_EQ(externalOffer.breadcrumbs()[0], "Meat");
    ASSERT_EQ(externalOffer.breadcrumbs()[1], "Kebab");
}

TEST(ExternalConversionTest, Empty_Product_External_To_DataCamp) {
    // arrange
    Market::DataCamp::External::Offer externalOffer;

    // act
    Market::DataCamp::Offer dataCampOffer;
    NMarket::NDataCamp::External2DataCampOffer(externalOffer, dataCampOffer, Market::DataCamp::MarketColor::WHITE);

    // assert
    ASSERT_TRUE(dataCampOffer.meta().platforms().empty());
}

TEST(ExternalConversionTest, One_Product_External_To_DataCamp) {
    // arrange
    Market::DataCamp::External::Offer externalOffer;
    externalOffer.mutable_service()->add_product(::Market::DataCamp::External::AdvProduct::DIRECT_SITE_PREVIEW);

    // act
    Market::DataCamp::Offer dataCampOffer;
    NMarket::NDataCamp::External2DataCampOffer(externalOffer, dataCampOffer, Market::DataCamp::MarketColor::WHITE);

    // assert
    ASSERT_EQ(dataCampOffer.meta().platforms().size(), 1);
    ASSERT_TRUE(dataCampOffer.meta().platforms().contains(Market::DataCamp::MarketColor::DIRECT_SITE_PREVIEW));
    ASSERT_TRUE(dataCampOffer.meta().platforms().at(Market::DataCamp::MarketColor::DIRECT_SITE_PREVIEW));
}

TEST(ExternalConversionTest, Two_Product_External_To_DataCamp) {
    // arrange
    Market::DataCamp::External::Offer externalOffer;
    externalOffer.mutable_service()->add_product(::Market::DataCamp::External::AdvProduct::DIRECT_STANDBY);
    externalOffer.mutable_service()->add_product(::Market::DataCamp::External::AdvProduct::DIRECT_GOODS_ADS);

    // act
    Market::DataCamp::Offer dataCampOffer;
    NMarket::NDataCamp::External2DataCampOffer(externalOffer, dataCampOffer, Market::DataCamp::MarketColor::WHITE);

    // assert
    ASSERT_EQ(dataCampOffer.meta().platforms().size(), 2);
    ASSERT_TRUE(dataCampOffer.meta().platforms().contains(Market::DataCamp::MarketColor::DIRECT_STANDBY));
    ASSERT_TRUE(dataCampOffer.meta().platforms().at(Market::DataCamp::MarketColor::DIRECT_STANDBY));

    ASSERT_TRUE(dataCampOffer.meta().platforms().contains(Market::DataCamp::MarketColor::DIRECT_GOODS_ADS));
    ASSERT_TRUE(dataCampOffer.meta().platforms().at(Market::DataCamp::MarketColor::DIRECT_GOODS_ADS));
}

TEST(ExternalConversionTest, GoodPlatform) {
    Market::DataCamp::Offer offer;
    google::protobuf::Timestamp timestamp = google::protobuf::Timestamp();
    offer.mutable_meta()->set_rgb(Market::DataCamp::VERTICAL_GOODS_ADS);

    auto external = NMarket::NDataCamp::DataCampOffer2External(offer, timestamp);
    UNIT_ASSERT_EQUAL(external.Getservice().Getplatform(), Market::DataCamp::Consumer::Platform::VERTICAL_GOODS);
}

TEST(ExternalConversionTest, PublishByPartner) {
    Market::DataCamp::External::Offer externalOffer;
    Market::DataCamp::Offer dataCampOffer;

    for (auto publicationStatus: {
            Market::DataCamp::SummaryPublicationStatus::AVAILABLE,
            Market::DataCamp::SummaryPublicationStatus::HIDDEN,
            Market::DataCamp::SummaryPublicationStatus::UNKNOWN_SUMMARY,
            }) {
        dataCampOffer.mutable_status()->clear_publish_by_partner();
        dataCampOffer.mutable_status()->set_publish_by_partner(publicationStatus);
        externalOffer = NMarket::NDataCamp::DataCampOffer2External(dataCampOffer);
        ASSERT_EQ(publicationStatus, externalOffer.publish_by_partner());
    }
}

TEST(ExternalConversionTest, SortDCContext) {
    Market::DataCamp::Offer dataCampOffer;
    google::protobuf::Timestamp timestamp = google::protobuf::Timestamp();

    auto& sortdc_context_item = *dataCampOffer.mutable_meta()->mutable_sortdc_context()->mutable_export_items()->Add();
    sortdc_context_item.set_url_hash_first(123456789012345);
    sortdc_context_item.set_url_hash_second(987654321012345);
    sortdc_context_item.set_policy_id(1000000000);
    sortdc_context_item.set_user(2000000000);

    auto externalOffer = NMarket::NDataCamp::DataCampOffer2External(dataCampOffer, timestamp);
    ASSERT_TRUE(google::protobuf::util::MessageDifferencer::Equivalent(externalOffer.sortdc_context(), dataCampOffer.meta().sortdc_context()));
}

TEST(ExternalConversionTest, DirectCategory) {
    Market::DataCamp::Offer offer;
    google::protobuf::Timestamp timestamp = google::protobuf::Timestamp();
    offer.mutable_meta()->set_rgb(Market::DataCamp::DIRECT);

    offer.mutable_content()->mutable_partner()->mutable_original()->mutable_direct_category()->set_id(34351);
    offer.mutable_content()->mutable_partner()->mutable_original()->mutable_direct_category()->set_name("ExpectedDirectCategoryName");

    offer.mutable_content()->mutable_partner()->mutable_original()->mutable_category()->set_id(34342);
    offer.mutable_content()->mutable_partner()->mutable_original()->mutable_category()->set_name("UnexpectedDirectCategoryName");

    auto external = NMarket::NDataCamp::DataCampOffer2External(offer, timestamp);
    UNIT_ASSERT_EQUAL(external.Getservice().Getplatform(), Market::DataCamp::Consumer::Platform::DIRECT);
    // prefer direct_category only in direct offers
    UNIT_ASSERT_EQUAL(external.Getoriginal_content().Getshop_category_id(), 34351);
    UNIT_ASSERT_EQUAL(external.Getoriginal_content().Getshop_category_name(), "ExpectedDirectCategoryName");
}

TEST(ExternalConversionTest, Available) {
    Market::DataCamp::External::Offer externalOffer;
    externalOffer.mutable_original_content()->set_available(true);

    Market::DataCamp::Offer dataCampOffer;
    NMarket::NDataCamp::External2DataCampOffer(externalOffer, dataCampOffer, Market::DataCamp::MarketColor::DIRECT_GOODS_ADS);

    ASSERT_TRUE(dataCampOffer.delivery().partner().original().available().flag());
}

TEST(ExternalConversionTest, GoodsModelId) {
    {
        Market::DataCamp::Offer offer;
        offer.mutable_content()->mutable_binding()->mutable_goods_cvdups_mapping()->set_model_id(1);
        offer.mutable_content()->mutable_binding()->mutable_goods_cvdups_mapping()->set_sku_id(2);

        Market::DataCamp::External::Offer external;
        NMarket::NDataCamp::DataCampOffer2External(offer, external, {});

        UNIT_ASSERT_EQUAL(external.goods_model_id(), offer.content().binding().goods_cvdups_mapping().model_id());
        UNIT_ASSERT_EQUAL(external.goods_sku_id(), offer.content().binding().goods_cvdups_mapping().sku_id());
    }
    {
        Market::DataCamp::External::Offer external;
        external.set_goods_model_id(1);
        external.set_goods_sku_id(2);

        Market::DataCamp::Offer offer;
        NMarket::NDataCamp::External2DataCampOffer(external, offer, {});

        UNIT_ASSERT_EQUAL(external.goods_model_id(), offer.content().binding().goods_cvdups_mapping().model_id());
        UNIT_ASSERT_EQUAL(external.goods_sku_id(), offer.content().binding().goods_cvdups_mapping().sku_id());
    }
}
