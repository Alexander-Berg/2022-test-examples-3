#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <functional>

using namespace NMarket;


TEST(BlueYmlParser, FeedStockCountCheckCorrectFieldParsingForDropship) {
    static const TString INPUT_XML_DROPSHIP(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog date="2019-12-20 12:18">
      <shop>
        <offers>
          <offer>
             <shop-sku>yml-without-feed-stock-count</shop-sku>
             <price>1990.00</price>
          </offer>
          <offer>
             <shop-sku>yml-feed-stock-count-100</shop-sku>
             <price>1990.00</price>
             <count>100</count>
          </offer>
          <offer>
             <shop-sku>yml-feed-stock-count-0</shop-sku>
             <price>1990.00</price>
             <count>0</count>
          </offer>
          <offer>
             <shop-sku>yml-feed-stock-count-100.1</shop-sku>
             <price>1990.00</price>
             <count>100.1</count>
          </offer>
          <offer>
             <shop-sku>yml-feed-stock-count-lalala</shop-sku>
             <price>1990.00</price>
             <count>lalala</count>
          </offer>
          <offer>
             <shop-sku>yml-feed-stock-count--100</shop-sku>
             <price>1990.00</price>
             <count>-100</count>
          </offer>
        </offers>
      </shop>
    </yml_catalog>)wrap");

    static const TString EXPECTED_JSON_DROPSHIP = TString(R"wrap(
    [
        {
            "OfferId": "yml-without-feed-stock-count",
            "IsValid": 1,
        },
        {
            "OfferId": "yml-feed-stock-count-100",
            "IsValid": 1,
            "FeedStockCount": "100",
        },
        {
            "OfferId": "yml-feed-stock-count-0",
            "IsValid": 1,
            "FeedStockCount": "0",
        },
        {
            "OfferId": "yml-feed-stock-count-100.1",
            "IsValid": 1,
        },
        {
            "OfferId": "yml-feed-stock-count-lalala",
            "IsValid": 1,
        },
        {
            "OfferId": "yml-feed-stock-count--100",
            "IsValid": 1,
        },
    ]
    )wrap");

    auto feedInfo = GetDefaultBlueFeedInfo();
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
            INPUT_XML_DROPSHIP,
            [](const TQueueItem &item) {
                NSc::TValue result;
                result["IsValid"] = item->IsValid;
                if (item->IsValid) {
                    result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                    if (item->DataCampOffer.stock_info().partner_stocks().has_count()) {
                        result["FeedStockCount"] = ToString(item->DataCampOffer.stock_info().partner_stocks().count());
                    }
                }
                return result;
            },
            feedInfo
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON_DROPSHIP);
    ASSERT_EQ(actual, expected);
}


TEST(BlueYmlParser, FeedStockCountDropshipAndSC) {
    static const TString INPUT_XML_DROPSHIP_AND_SC(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog date="2019-12-20 12:18">
      <shop>
        <offers>
          <offer>
             <shop-sku>yml-dropship-and-sc-feed-stock-count-100</shop-sku>
             <price>1990.00</price>
             <count>100</count>
          </offer>
        </offers>
      </shop>
    </yml_catalog>
    )wrap");


    static const TString EXPECTED_JSON_DROPSHIP_AND_SC = TString(R"wrap(
    [
        {
            "OfferId": "yml-dropship-and-sc-feed-stock-count-100",
            "IsValid": 1,
            "FeedStockCount": "100"
        },
    ]
    )wrap");

    auto feedInfo = GetDefaultBlueFeedInfo();
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
            INPUT_XML_DROPSHIP_AND_SC,
            [](const TQueueItem &item) {
                NSc::TValue result;
                result["IsValid"] = item->IsValid;
                if (item->IsValid) {
                    result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                    result["FeedStockCount"] = ToString(item->DataCampOffer.stock_info().partner_stocks().count());
                }
                return result;
            },
            feedInfo
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON_DROPSHIP_AND_SC);
    ASSERT_EQ(actual, expected);
}


TEST(BlueYmlParser, FeedStockCountCrossdock) {
    static const TString INPUT_XML_CROSSDOCK(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog date="2019-12-20 12:18">
      <shop>
        <offers>
          <offer>
             <shop-sku>yml-crossdock-feed-stock-count-100</shop-sku>
             <price>1990.00</price>
             <count>100</count>
          </offer>
        </offers>
      </shop>
    </yml_catalog>
    )wrap");


    static const TString EXPECTED_JSON_CROSSDOCK = TString(R"wrap(
    [
        {
            "OfferId": "yml-crossdock-feed-stock-count-100",
            "IsValid": 1,
            "FeedStockCount": "100"
        },
    ]
    )wrap");

    auto feedInfo = GetDefaultBlueFeedInfo();
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
            INPUT_XML_CROSSDOCK,
            [](const TQueueItem &item) {
                NSc::TValue result;
                result["IsValid"] = item->IsValid;
                if (item->IsValid) {
                    result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                    result["FeedStockCount"] = ToString(item->DataCampOffer.stock_info().partner_stocks().count());
                }
                return result;
            },
            feedInfo
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON_CROSSDOCK);
    ASSERT_EQ(actual, expected);
}


TEST(BlueYmlParser, FeedStockCountFulfillment) {
    static const TString INPUT_XML_FULFILLMENT(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog date="2019-12-20 12:18">
      <shop>
        <offers>
          <offer>
             <shop-sku>yml-fulfillment-feed-stock-count-100</shop-sku>
             <price>1990.00</price>
             <count>100</count>
          </offer>
        </offers>
      </shop>
    </yml_catalog>
    )wrap");


    static const TString EXPECTED_JSON_FULFILLMENT = TString(R"wrap(
    [
        {
            "OfferId": "yml-fulfillment-feed-stock-count-100",
            "IsValid": 1,
            "FeedStockCount": "100"
        },
    ]
    )wrap");

    auto feedInfo = GetDefaultBlueFeedInfo();
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
            INPUT_XML_FULFILLMENT,
            [](const TQueueItem &item) {
                NSc::TValue result;
                result["IsValid"] = item->IsValid;
                if (item->IsValid) {
                    result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                    result["FeedStockCount"] = ToString(item->DataCampOffer.stock_info().partner_stocks().count());
                }
                return result;
            },
            feedInfo
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON_FULFILLMENT);
    ASSERT_EQ(actual, expected);
}


TEST(BlueYmlParser, FeedStockCountClickAndCollect) {
    static const TString INPUT_XML_CLICK_AND_COLLECT(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog date="2019-12-20 12:18">
      <shop>
        <offers>
          <offer>
             <shop-sku>yml-click-and-collect-feed-stock-count-100</shop-sku>
             <price>1990.00</price>
             <count>100</count>
          </offer>
        </offers>
      </shop>
    </yml_catalog>
    )wrap");


    static const TString EXPECTED_JSON_CLICK_AND_COLLECT = TString(R"wrap(
    [
        {
            "OfferId": "yml-click-and-collect-feed-stock-count-100",
            "IsValid": 1,
            "FeedStockCount": "100"
        },
    ]
    )wrap");

    auto feedInfo = GetDefaultBlueFeedInfo();
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
            INPUT_XML_CLICK_AND_COLLECT,
            [](const TQueueItem &item) {
                NSc::TValue result;
                result["IsValid"] = item->IsValid;
                if (item->IsValid) {
                    result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                    result["FeedStockCount"] = ToString(item->DataCampOffer.stock_info().partner_stocks().count());
                }
                return result;
            },
            feedInfo
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON_CLICK_AND_COLLECT);
    ASSERT_EQ(actual, expected);
}

TEST(BlueYmlParser, TestParsingStockFeed) {
    /// Провряем, что для стоковых фидов будут обрабатываться тольк поля shop-sku и count
    static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog date="2020-05-19 20:00">
      <shop>
        <offers>
          <offer>
             <shop-sku>yml-stock-feed-without-feed-stock-count</shop-sku>
             <price>10.00</price>
          </offer>
          <offer>
             <shop-sku>yml-stock-feed-feed-stock-count-100</shop-sku>
             <price>10.00</price>
             <count>100</count>
          </offer>
          <offer>
             <shop-sku>yml-stock-feed-feed-stock-count-0</shop-sku>
             <price>10.00</price>
             <count>0</count>
          </offer>
        </offers>
      </shop>
    </yml_catalog>)wrap");

    static const TString EXPECTED_JSON = TString(R"wrap(
    [
        {
            "OfferId": "yml-stock-feed-without-feed-stock-count",
            "IsValid": 1,
            "FeedStockCount": "0",
            "Price": "(empty maybe)"
        },
        {
            "OfferId": "yml-stock-feed-feed-stock-count-100",
            "IsValid": 1,
            "FeedStockCount": "100",
            "Price": "(empty maybe)"
        },
        {
            "OfferId": "yml-stock-feed-feed-stock-count-0",
            "IsValid": 1,
            "FeedStockCount": "0",
            "Price": "(empty maybe)"
        },
    ]
    )wrap");

    auto feedInfo = GetDefaultBlueFeedInfo();
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlStockFeedParser>(
            INPUT_XML,
            [](const TQueueItem &item) {
                NSc::TValue result;
                result["IsValid"] = item->IsValid;
                if (item->IsValid) {
                    result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                    result["FeedStockCount"] = ToString(item->DataCampOffer.stock_info().partner_stocks().count());
                    result["Price"] = ToString(item->RawPrice);
                }
                return result;
            },
            feedInfo
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
