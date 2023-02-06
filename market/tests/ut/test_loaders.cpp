#include <market/idx/offers/lib/loaders/load_biz_logic.h>
#include <market/library/snippet_builder/snippet_builder.h>


#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <market/proto/feedparser/deprecated/OffersData.pb.h>
#include <market/proto/ir/UltraController.pb.h>
#include <market/library/glparams/gl_mbo_pbuf_loader.h>
#include <market/library/snappy-protostream/proto_snappy_stream.h>
#include <market/library/interface/indexer_report_interface.h>
#include <market/library/queue/Queue.h>

#include <util/generic/ptr.h>
#include <util/generic/vector.h>
#include <util/generic/string.h>
#include <util/string/cast.h>

const std::string SHOPSDAT_PATH = SRC_("data/shops-utf8.dat");
const std::string FEEDS_DATA = SRC_("data/feeds");


//==================================================================================
//==================================================================================
//==================================================================================


TEST(TestLoaders, ShopsDatLoader) {
    auto& feeds = Feeds::Instance();
    feeds.Load(SHOPSDAT_PATH);

    // Must load all feeds
    ASSERT_TRUE(feeds.Find(1000000000) != nullptr);
    ASSERT_TRUE(feeds.Find(1000000001) != nullptr);
    ASSERT_TRUE(feeds.Find(1000000002) != nullptr);
    ASSERT_TRUE(feeds.Find(1000000003) != nullptr);
    ASSERT_TRUE(feeds.Find(8023) != nullptr);
    ASSERT_TRUE(feeds.Find(7933) != nullptr);
    ASSERT_TRUE(feeds.Find(7932) != nullptr);

    // Include Fulfillment Virtual Shop
    ASSERT_TRUE(feeds.Find(1000000004) != nullptr);
    ASSERT_TRUE(feeds.Find(1000000004)->is_ff_virtual);
    ASSERT_EQ(feeds.Find(1000000004)->domain, "beru.ru");
}


TEST(TestLoaders, FulfillmentFeedId) {
    /*
    Тест проверяет, что для фулфилментовского офера с заданным
    fulfillment_shop_id, для которого есть запись в shopsdat,
    правильно вычисляются приоритетные регионы и складываются в отдельное поле
    */
    auto& feeds = Feeds::Instance();
    feeds.Load(SHOPSDAT_PATH);

    // Include Fulfillment Virtual Shop
    ASSERT_TRUE(feeds.Find(1000000004) != nullptr);
    ASSERT_TRUE(feeds.Find(1000000004)->is_ff_virtual);
    // Include Shop in Fulfillment program
    ASSERT_TRUE(feeds.Find(1000000005) != nullptr);
    ASSERT_EQ(feeds.Find(1000000005)->fulfillment_feed_id, 1000000004);
}


TEST(TestLoaders, BlueUrl) {
    /*
    Тест проверяет корректность синих урлов
    */
    auto& feeds = Feeds::Instance();
    feeds.Load(SHOPSDAT_PATH);

    // Include Fulfillment Virtual Shop
    ASSERT_TRUE(feeds.Find(1000000004) != nullptr);
    ASSERT_TRUE(feeds.Find(1000000004)->is_ff_virtual);
    // Include Shop in Fulfillment program
    ASSERT_TRUE(feeds.Find(1000000005) != nullptr);
    ASSERT_EQ(feeds.Find(1000000005)->fulfillment_feed_id, 1000000004);

    // устанавливаем данные необходимые для работы
    Market::OffersData::Deprecated::Offer offer;
    offer.mutable_genlog()->set_feed_id(1000000005); // фид магазина учавствующего в программе Fulfillment
    offer.mutable_genlog()->set_fulfillment_shop_id(1000000004); // идентификатор виртуального магазина

    offer.mutable_genlog()->set_is_blue_offer(true);
    offer.mutable_genlog()->set_market_sku(12345);
    offer.mutable_genlog()->set_ware_md5("test_ware_md5");

    NMarket::NIdx::TOfferSnippetContext snippetCtx(offer, nullptr, nullptr, false);
    ASSERT_EQ(snippetCtx.GetUrl(), "https://pokupki.market.yandex.ru/product/12345?offerid=test_ware_md5");

    offer.mutable_genlog()->set_model_title("test model title");
    NMarket::NIdx::TOfferSnippetContext snippetCtxForTestWithSlug(offer, nullptr, nullptr, false);
    ASSERT_EQ(snippetCtxForTestWithSlug.GetUrl(), "https://pokupki.market.yandex.ru/product--test-model-title/12345?offerid=test_ware_md5");
}

TEST(TestLoaders, BlueUrlPokupki) {
    /*
    Тест проверяет корректность синих урлов, сгенерированных для pokupki.market.yandex.ru
    */

    // устанавливаем данные необходимые для работы
    Market::OffersData::Deprecated::Offer offer;
    offer.mutable_genlog()->set_feed_id(1000000005); // фид магазина учавствующего в программе Fulfillment
    offer.mutable_genlog()->set_fulfillment_shop_id(1000000004); // идентификатор виртуального магазина

    offer.mutable_genlog()->set_is_blue_offer(true);
    offer.mutable_genlog()->set_market_sku(12345);
    offer.mutable_genlog()->set_ware_md5("test_ware_md5");

    NMarket::NIdx::TOfferSnippetContext snippetCtx(offer, nullptr, nullptr, true);
    ASSERT_EQ(snippetCtx.GetUrl(), "https://pokupki.market.yandex.ru/product/12345?offerid=test_ware_md5");

    offer.mutable_genlog()->set_model_title("test model title");
    NMarket::NIdx::TOfferSnippetContext snippetCtxForTestWithSlug(offer, nullptr, nullptr, true);
    ASSERT_EQ(snippetCtxForTestWithSlug.GetUrl(), "https://pokupki.market.yandex.ru/product--test-model-title/12345?offerid=test_ware_md5");
}
