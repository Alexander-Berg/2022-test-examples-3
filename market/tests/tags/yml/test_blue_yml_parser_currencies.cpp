#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <functional>

using namespace NMarket;

static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-03-26 13:10">
  <shop>
    <currencies>
      <currency id="RUR" rate="1"/>
      <currency id="USD" rate="64.81"/>
    </currencies>
    <offers>
      <offer>
        <shop-sku>offer-wo-currency</shop-sku>
        <price>7</price>
      </offer>
      <offer>
        <shop-sku>offer-ru</shop-sku>
         <currencyId>RUB</currencyId>
         <price>77</price>
      </offer>
      <offer>
         <shop-sku>offer-usd</shop-sku>
         <currencyId>USD</currencyId>
         <price>777</price>
      </offer>
      <offer>
         <shop-sku>offer-byn</shop-sku>
         <currencyId>BYN</currencyId>
         <price>7777</price>
      </offer>
      <offer>
         <shop-sku>offer-unknown-currency</shop-sku>
         <currencyId>UNK</currencyId>
         <price>7776</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "offer-wo-currency",
        "Currency": "RUR",
        "Disabled": 0,
    },
    {
        "OfferId": "offer-ru",
        "Currency": "RUR",
        "Disabled": 0,

    },
    {
        "OfferId": "offer-usd",
        "Currency": "RUR",
        "Disabled": 0,

    },
    {
        "OfferId": "offer-byn",
        "Currency": "RUR",
        "Disabled": 0,

    },
    {
        "OfferId": "offer-unknown-currency",
        "Currency": "RUR",
        "Disabled": 0,

    }
]
)wrap");


TEST(BlueYmlParser, Currencies) {
    /**
     * - считаем, что референсной валютой (rate=1) всегда является валюта домашнего региона (если в фиде передается в
     *   качестве референсной валюта, не соответствующая домашнему региону, игнорируем это и используем в качестве
     *   референсной валюту домашнего региона
     * - всегда используем RUR, даже если магазин указал другой currencyId (убрать в MARKETINDEXER-36969)
     */
    const auto [actual, _] = RunBlueYmlFeedParserWithCheckFeed<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            bool disabled = false;
            for (const auto& flag : item->DataCampOffer.status().disabled()) {
                disabled |= flag.flag();
            }
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Currency"] = item->Currency;
            result["Disabled"] = disabled;
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
