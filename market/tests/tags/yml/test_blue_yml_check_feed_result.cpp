#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2020-04-03 21:15">
  <shop>
    <currencies>
      <currency id="RUR" rate="1"/>
      <currency id="USD"/>
    </currencies>
    <offers>
      <offer>
         <shop-sku>YmlCheckFeed1</shop-sku>
         <price>1</price>
      </offer>
      <offer>
         <shop-sku>YmlCheckFeed2</shop-sku>
         <price>-2</price>
      </offer>
      <offer>
         <price>3</price>
      </offer>
      <offer>
         <shop-sku>YmlCheckFeed3</shop-sku>
         <price>3</price>
         <count>lalala</count>
       </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


TEST(BlueYmlParser, CheckFeed) {
    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::YML);
    feedInfo.FeedId = 567890;

    const auto [actual, checkResult] = RunBlueYmlFeedParserWithCheckFeed<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& /*item*/) {
            return NSc::TValue();
        },
        feedInfo
    );

    ASSERT_STREQ(checkResult.feed_type(), "YML");

    ASSERT_EQ(checkResult.log_message().size(), 4);

    // test feed warning
    const auto& wrongFeedDateForYmlCheckFeed2 = checkResult.log_message()[0];
    ASSERT_EQ(wrongFeedDateForYmlCheckFeed2.code(), "36F");
    ASSERT_EQ(wrongFeedDateForYmlCheckFeed2.level(), 2);
    ASSERT_EQ(wrongFeedDateForYmlCheckFeed2.position(), "3:37");
    ASSERT_EQ(wrongFeedDateForYmlCheckFeed2.feed_id(), 567890);
    ASSERT_NE(wrongFeedDateForYmlCheckFeed2.details().find("\"date\":\"2020-04-03 21:15\""), std::string::npos);

    // test warning for offer with wrong price
    const auto& invalidPriceForYmlCheckFeed2 = checkResult.log_message()[1];
    ASSERT_EQ(invalidPriceForYmlCheckFeed2.code(), "453");
    ASSERT_EQ(invalidPriceForYmlCheckFeed2.level(), 3);
    ASSERT_EQ(invalidPriceForYmlCheckFeed2.offer_supplier_sku(), "YmlCheckFeed2");
    ASSERT_EQ(invalidPriceForYmlCheckFeed2.position(), "16:27");
    ASSERT_EQ(invalidPriceForYmlCheckFeed2.feed_id(), 567890);

    // test error for offer without shop_sku
    const auto& emptySku = checkResult.log_message()[2];
    ASSERT_EQ(emptySku.code(), "45e");
    ASSERT_EQ(emptySku.level(), 3);
    ASSERT_EQ(emptySku.offer_supplier_sku(), "");
    ASSERT_EQ(emptySku.position(), "20:15");
    ASSERT_EQ(emptySku.feed_id(), 567890);

    // test warning for offer with invalid stock
    const auto& invalidStockSku = checkResult.log_message()[3];
    ASSERT_EQ(invalidStockSku.code(), "358");
    ASSERT_EQ(invalidStockSku.level(), 2);
    ASSERT_EQ(invalidStockSku.position(), "24:31");
    ASSERT_EQ(invalidStockSku.feed_id(), 567890);
}

