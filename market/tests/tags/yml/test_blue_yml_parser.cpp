#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/parser_test_runner.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <functional>

using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-03-26 13:10">
  <shop>
    <name/>
    <company/>
    <url>https://www.pult.ru/</url>
    <currencies>
      <currency id="RUR" plus="0" rate="1"/>
    </currencies>
    <categories>
      <category id="2319">Акустические системы (Акустика)</category>
      <category id="2325" parentId="2319">Напольная акустика</category>
    </categories>
    <offers>
      <offer id="173572">
        <shop-sku>1</shop-sku>
        <url>https://www.pult.ru/product/napolnaya</url>
        <price>28490.00</price>
        <currencyId>RUR</currencyId>
        <categoryId>2325</categoryId>
        <cpa>1</cpa>
        <picture>https://www.pult.ru/upload/iblock/ba5/ba5528fffb8d62106b3cb3ca5655cb73.jpg</picture>
        <typePrefix>Напольная акустика</typePrefix>
        <vendor>Jamo</vendor>
        <model>S 805 White</model>
        <description>В серию Studio 8</description>
      </offer>
      <offer id="173547">
        <shop-sku>
            lalala
        </shop-sku>
        <url>https://www.pult.ru/product/napolnaya-akustika-jamo-s-807-walnut</url>
        <price>39490.00</price>
        <currencyId>RUR</currencyId>
        <categoryId>2325</categoryId>
        <cpa>1</cpa>
        <picture>https://www.pult.ru/upload/iblock/a74/a742e0996b0a700869f5cf10cfa5ff11.jpg</picture>
        <typePrefix>Напольная акустика</typePrefix>
        <vendor>Jamo</vendor>
        <model>S 807 walnut</model>
        <description>В серию Studio 8 входят десять моделей: трое напольников</description>
      </offer>
      <offer id="173576">
        <shop-sku>BZz13РусскиеБуквы</shop-sku>
        <url>https://www.pult.ru/product/napolnaya-akustika-jamo-s-807-black</url>
        <price>39490.00</price>
        <currencyId>RUR</currencyId>
        <categoryId>2325</categoryId>
        <cpa>1</cpa>
        <picture>https://www.pult.ru/upload/iblock/881/881a6e7295569c2b5f2d9357dcf77602.jpg</picture>
        <typePrefix>Напольная акустика</typePrefix>
        <vendor>Jamo</vendor>
        <model>S 807 Black</model>
        <description>В серию Studio 8 входят десять моделей: двое полочников</description>
      </offer>
      <offer id="173577">
        <shop_sku>shop.sku.ЫЫЫ</shop_sku>
        <url>https://www.pult.ru/product/napolnaya-akustika-jamo-s-807-black</url>
        <price>39490.00</price>
        <currencyId>RUR</currencyId>
        <categoryId>2325</categoryId>
        <cpa>1</cpa>
        <picture>https://www.pult.ru/upload/iblock/881/881a6e7295569c2b5f2d9357dcf77602.jpg</picture>
        <typePrefix>Напольная акустика</typePrefix>
        <vendor>Jamo</vendor>
        <model>S 807 Black</model>
        <description>В серию Studio 8 входят десять моделей: двое полочников</description>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "1",
    },
    {
        "OfferId": "lalala",
    },
    {
        "OfferId": "BZz13РусскиеБуквы",
    },
    {
        "OfferId": "shop.sku.ЫЫЫ",
    },
]
)wrap");


TEST(BlueYmlParser, Example) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}


TEST(BlueYmlParser, RequiredColumnsForStockFeed) {
    const TString INPUT_STOCK_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
        <yml_catalog date="2019-03-26 13:10">
          <shop>
            <name/>
            <company/>
            <url>https://www.TEST.ru/</url>
            <offers>
              <offer>
                <shop-sku>RequiredColumnsForStockFeedShopSku</shop-sku>
              </offer>
              <offer>
                <price>1000</price>
              </offer>
            </offers>
          </shop>
        </yml_catalog>)wrap");

    const TString EXPECTED_STOCK_JSON = TString(R"wrap(
    [
        {
            "OfferId": "RequiredColumnsForStockFeedShopSku",
        },
    ]
    )wrap");


    NMarket::TFeedInfo feedInfo = GetDefaultBlueFeedInfo(EFeedType::YML);
    feedInfo.PushFeedClass = Market::DataCamp::API::FEED_CLASS_STOCK;
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlStockFeedParser>(
            INPUT_STOCK_XML,
            [](const TQueueItem& item) {
                NSc::TValue result;
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                return result;
            },
            feedInfo
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_STOCK_JSON);
    ASSERT_EQ(actual, expected);
}

TEST(BlueYmlParser, IntegrationFeed) {
    const TString INPUT_STOCK_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog date="2019-03-26 13:10">
    <shop>
        <name>Магазин  Audio-Video</name>
        <local_delivery_cost>100</local_delivery_cost>
        <url>http://www.aydio-video.ru</url>
        <cpa>1</cpa>
        <company>Audio-Video</company>
        <categories>
            <category id="1" parentId="0">root category</category>
        </categories>
        <currencies>
            <currency rate="1" id="RUR"/>
        </currencies>
        <offers>
            <offer id="100xXx0">
                <dimensions>1.0/2.0/3.0</dimensions>
                <picture>http://www.1.ru/pic/PIC1.jpg</picture>
                <vendor>Default Vendor</vendor>
                <weight>1</weight>
                <price>1</price>
                <enable_auto_discounts>True</enable_auto_discounts>
                <name>offer 100xXx0</name>
                <count>1</count>
                <market_sku>123456</market_sku>
                <currencyId>RUR</currencyId>
                <shop-sku>100xXx0</shop-sku>
                <url>http://www.1.ru/?ID=100xXx0</url>
                <manufacturer_warranty>False</manufacturer_warranty>
                <categoryId>1</categoryId>
                <vat>NO_VAT</vat>
            </offer>
        </offers>
    </shop>
    </yml_catalog>)wrap");

    const TString EXPECTED_STOCK_JSON = TString(R"wrap(
    [
        {
            "OfferId": "100xXx0",
        },
    ]
    )wrap");


    NMarket::TFeedInfo feedInfo = GetDefaultBlueFeedInfo(EFeedType::YML);
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
            INPUT_STOCK_XML,
            [](const TQueueItem& item) {
                NSc::TValue result;
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                return result;
            },
            feedInfo
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_STOCK_JSON);
    ASSERT_EQ(actual, expected);
}



/*
TEST(BlueYmlParser, UnknownTag) {
    static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog date="2019-03-26 13:10">
      <shop>
        <offers>
          <offer id="173572">
            <shop-sku>1</shop-sku>
            <price>100.00</price>
            <what-a-tag>what a value</what-a-tag>
          </offer>
        </offers>
      </shop>
    </yml_catalog>)wrap");

    TSizedLFQueue<IFeedParser::TConstMsgPtr> inputQueue(10000);
    auto parser = CreateFeedParser<NBlue::TYmlFeedParser>(INPUT_XML, GetDefaultBlueFeedInfo(EFeedType::YML));
    parser->Start(inputQueue);
    ASSERT_TRUE(parser->GetLastError()->Contains("undefined tag \"what-a-tag\""));
}
*/
