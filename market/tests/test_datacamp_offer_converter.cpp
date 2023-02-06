#include <market/idx/feeds/qparser/lib/datacamp_offer_converter.h>

#include <google/protobuf/util/time_util.h>

#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

const static auto ShopsDatPath = JoinFsPaths(ArcadiaSourceRoot(), "market/idx/feeds/qparser/tests/data/shops-utf8.dat");

TEST(DataCampOfferConverter, MetaInServicePart) {
    // Проверка заполнения меты в сервисных партнерских параметрах функцией FillMetaForPartnerServiceFields

    Market::DataCamp::Offer serviceOffer;
    constexpr ui64 seconds = 123;
    const auto timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds);
    NMarket::TFeedInfo feedInfo;
    feedInfo.Auction = NMarket::NBind::EAuction::YML;
    NMarket::FillMetaForPartnerServiceFields(serviceOffer, timestamp, feedInfo);

#define CHECK_META(proto)                                                 \
    EXPECT_EQ(seconds, proto.meta().timestamp().seconds());              \
    EXPECT_TRUE(Market::DataCamp::DataSource::PUSH_PARTNER_FEED == proto.meta().source());

#define CHECK_FLAG(proto)           \
    CHECK_META(proto)               \
    EXPECT_FALSE(proto.has_flag())

    // Статус оффера
    CHECK_FLAG(serviceOffer.status().incomplete_wizard());
    CHECK_FLAG(serviceOffer.status().original_cpa());
    EXPECT_FALSE(serviceOffer.status().has_ready_for_publication());
    EXPECT_FALSE(serviceOffer.status().has_publication());

    // Цена оффера
    CHECK_META(serviceOffer.price().basic());
    CHECK_FLAG(serviceOffer.price().enable_auto_discounts());
    CHECK_META(serviceOffer.price().purchase_price());
    CHECK_META(serviceOffer.price().dynamic_pricing());
    EXPECT_FALSE(serviceOffer.price().basic().has_binary_price());
    EXPECT_EQ(serviceOffer.price().price_by_warehouse_size(), 0);
    EXPECT_FALSE(serviceOffer.price().dynamic_pricing().has_type());

    // Доставка
    CHECK_FLAG(serviceOffer.delivery().partner().original().available());
    CHECK_FLAG(serviceOffer.delivery().partner().original().pickup());
    CHECK_FLAG(serviceOffer.delivery().partner().original().store());
    CHECK_FLAG(serviceOffer.delivery().partner().original().delivery());
    CHECK_META(serviceOffer.delivery().partner().original().delivery_options());
    EXPECT_EQ(serviceOffer.delivery().partner().original().delivery_options().options_size(), 0);
    CHECK_META(serviceOffer.delivery().partner().original().pickup_options());
    EXPECT_EQ(serviceOffer.delivery().partner().original().pickup_options().options_size(), 0);
    CHECK_META(serviceOffer.delivery().partner().original().outlets());
    EXPECT_EQ(serviceOffer.delivery().partner().original().outlets().outlets_size(), 0);
    EXPECT_FALSE(serviceOffer.delivery().has_market());
    EXPECT_FALSE(serviceOffer.delivery().partner().has_actual());
    EXPECT_FALSE(serviceOffer.delivery().has_delivery_info());

    // Ставки
    CHECK_META(serviceOffer.bids().bid());
    EXPECT_FALSE(serviceOffer.bids().bid().has_value());
    EXPECT_FALSE(serviceOffer.bids().has_flag_dont_pull_up_bids());
    EXPECT_FALSE(serviceOffer.bids().has_fee());

    // Стоки
    CHECK_META(serviceOffer.stock_info().partner_stocks());
    EXPECT_FALSE(serviceOffer.stock_info().partner_stocks().has_count());
    EXPECT_FALSE(serviceOffer.stock_info().has_market_stocks());

    // Партнерский контент
    CHECK_META(serviceOffer.content().partner().original_terms().quantity());
    CHECK_META(serviceOffer.content().partner().original_terms().sales_notes());
    CHECK_META(serviceOffer.content().partner().original_terms().supply_plan());
    CHECK_META(serviceOffer.content().partner().original_terms().transport_unit_size());
    CHECK_META(serviceOffer.content().partner().original_terms().supply_quantity());
    CHECK_META(serviceOffer.content().partner().original_terms().supply_weekdays());
    CHECK_META(serviceOffer.content().partner().original_terms().partner_delivery_time());
    EXPECT_FALSE(serviceOffer.content().partner().original_terms().has_seller_warranty());
    CHECK_META(serviceOffer.content().partner().original().url());
    CHECK_META(serviceOffer.content().partner().original().supplier_info());
    EXPECT_FALSE(serviceOffer.content().partner().original().has_description());

#undef CHECK_FLAG
#undef CHECK_META
}

TEST(DataCampOfferConverter, NoBidsMetaForNotYMLSource) {
    // Проверка отсутствия меты, если источник ставок != YML
    Market::DataCamp::Offer serviceOffer;
    constexpr ui64 seconds = 123;
    const auto timestamp = google::protobuf::util::TimeUtil::SecondsToTimestamp(seconds);
    NMarket::TFeedInfo feedInfo;
    feedInfo.Auction = NMarket::NBind::EAuction::WEB;
    NMarket::FillMetaForPartnerServiceFields(serviceOffer, timestamp, feedInfo);
    EXPECT_FALSE(serviceOffer.bids().bid().has_meta());
}

TEST(DataCampOfferConverter, SetupOfferColorDirectStandByFromShopsDatParameters) {

    NMarket::TFeedInfo feedInfo{
        .OfferColor = NMarket::EMarketColor::MC_DIRECT
    };
    NMarket::TFeedParsingTask feedParsingTask;
    feedParsingTask.mutable_shops_dat_parameters()->set_direct_standby(true);
    feedParsingTask.mutable_shops_dat_parameters()->set_color(Market::DataCamp::DIRECT);

    Market::DataCamp::Offer datacampOffer;
    NMarket::SetupOfferColor(feedInfo, feedParsingTask, datacampOffer);

    EXPECT_EQ(datacampOffer.meta().rgb(), Market::DataCamp::DIRECT_STANDBY);
    EXPECT_TRUE(datacampOffer.meta().platforms().at(Market::DataCamp::DIRECT_STANDBY));
}

TEST(DataCampOfferConverter, SetupOfferColorDirectStandByFromSupplierData) {
    NMarket::TFeedInfo feedInfo{
        .ShopId = 1,
        .FeedId = 121,
        .OfferColor = NMarket::EMarketColor::MC_DIRECT
    };
    NMarket::TFeedParsingTask feedParsingTask;
    SUPPLIER_DATA.Init(ShopsDatPath, 121 /* feed_id */);

    Market::DataCamp::Offer datacampOffer;
    NMarket::SetupOfferColor(feedInfo, feedParsingTask, datacampOffer);

    EXPECT_EQ(datacampOffer.meta().rgb(), Market::DataCamp::DIRECT_STANDBY);
    EXPECT_TRUE(datacampOffer.meta().platforms().at(Market::DataCamp::DIRECT_STANDBY));
}
