#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>

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
      <currency id="USD"/>
      <currency id="BYN" rate="1.1"/>
      <currency id="EUR" rate="1"/>
    </currencies>
    <offers>
      <offer id="offer-no-currency">
        <price>7</price>
      </offer>
      <offer id="offer-ru">
         <currencyId>RUR</currencyId>
         <price>77</price>
      </offer>
      <offer id="offer-usd">
         <currencyId>USD</currencyId>
         <price>777</price>
      </offer>
      <offer id="offer-byn">
         <currencyId>BYN</currencyId>
         <price>7777</price>
      </offer>
      <offer id="offer-kzt">
         <currencyId>KZT</currencyId>
         <price>77777</price>
      </offer>
      <offer id="offer-currency-unknown">
         <currencyId>UNKNOWN</currencyId>
         <price>7776</price>
      </offer>
      <offer id="offer-eur">
         <currencyId>EUR</currencyId>
         <price>777777</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "offer-no-currency",
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
        "Currency": "USD",
        "Disabled": 0,
    },
    {
        "OfferId": "offer-byn",
        "Currency": "BYN",
        "Disabled": 1,
    },
    {
        "OfferId": "offer-kzt",
        "Currency": "KZT",
        "Disabled": 0,
    },
    {
        "OfferId": "offer-currency-unknown",
        "Currency": "UNKNOWN",
        "Disabled": 1,
    },
    {
        "OfferId": "offer-eur",
        "Currency": "EUR",
        "Disabled": 0,
    }
]
)wrap");


TEST(WhiteYmlParser, Currencies) {
    /**
     * - считаем, что референсной валютой (rate=1) всегда является валюта домашнего региона (если в фиде передается в
     *   качестве референсной валюта, не соответствующая домашнему региону, игнорируем это и используем в качестве
     *   референсной валюту домашнего региона
     * - если передается rate != 1 и != ЦБ, то скрываем офферы с этим currencyId
     * - если currencyId не указан, используем RUR
     * - если currencyId невалидный, скрываем такой оффер
     */
    const auto actual = RunWhiteYmlFeedParserWithCheckFeed<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            bool disabled = false;
            for (const auto& flag : item->DataCampOffer.status().disabled()) {
                disabled |= flag.flag();
            }
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Currency"] = item->Currency;
            result["Disabled"] = disabled;
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
