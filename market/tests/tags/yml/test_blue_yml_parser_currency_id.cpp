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
    <offers>
      <offer>
         <shop-sku>without-currencyId</shop-sku>
         <price>1990.00</price>
      </offer>
      <offer>
         <shop-sku>currencyId-RUR</shop-sku>
         <currencyId> RUR </currencyId>
         <price>1990.00</price>
      </offer>
      <offer>
         <shop-sku>currencyId-USD</shop-sku>
         <currencyId>USD </currencyId>
         <price>1990.00</price>
      </offer>
      <offer>
         <shop-sku>currencyId-UNKNOWN</shop-sku>
         <currencyId>UNKNOWN</currencyId>
         <price>1980.00</price>
      </offer>
      <offer>
         <shop-sku>currencyId-BYN</shop-sku>
         <currencyId>BYN</currencyId>
         <price>1990.00</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");

static const TString EXPECTED_JSON_FORCE_RUR = TString(R"wrap(
[
    {
        "OfferId": "without-currencyId",
        "Currency": "RUR",
    },
    {
        "OfferId": "currencyId-RUR",
        "Currency": "RUR",
    },
    {
        "OfferId": "currencyId-USD",
        "Currency": "RUR",
    },
    {
        "OfferId": "currencyId-UNKNOWN",
        "Currency": "RUR",
    },
    {
        "OfferId": "currencyId-BYN",
        "Currency": "RUR",

    },
]
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "without-currencyId",
        "Currency": "RUR",
        "Disabled": 0,
    },
    {
        "OfferId": "currencyId-RUR",
        "Currency": "RUR",
        "Disabled": 0,
    },
    {
        "OfferId": "currencyId-USD",
        "Currency": "USD",
        "Disabled": 1,
    },
    {
        "OfferId": "currencyId-UNKNOWN",
        "Currency": "UNKNOWN",
        "Disabled": 1,
    },
    {
        "OfferId": "currencyId-BYN",
        "Currency": "BYN",
        "Disabled": 1,
    },
]
)wrap");


TEST(BlueYmlParser, CurrencyIdForceRur) {
    /**
     * Всегда используем RUR, даже если магазин указал другой currencyId (убрать в MARKETINDEXER-37256)
     */

    auto feedInfo = GetDefaultBlueFeedInfo();
    feedInfo.ForceRurCurrencyForBlueMode = true;

    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Currency"] = item->Currency;
            ASSERT_TRUE(item->IsValid);
            ASSERT_FALSE(item->IsDisabled);
            return result;
        },
        feedInfo
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON_FORCE_RUR);
    ASSERT_EQ(actual, expected);
}

TEST(BlueYmlParser, CurrencyId) {
    /**
     * MARKETINDEXER-36734 - Фид без секции currencies считается валидным и это дефолтная спецификация
     */

    auto feedInfo = GetDefaultBlueFeedInfo();
    feedInfo.ForceRurCurrencyForBlueMode = false;

    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Currency"] = item->Currency;
            ASSERT_TRUE(item->IsValid);
            bool disabled = false;
            for (const auto& flag : item->DataCampOffer.status().disabled()) {
                disabled |= flag.flag();
            }
            result["Disabled"] = disabled;
            return result;
        },
        feedInfo
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
