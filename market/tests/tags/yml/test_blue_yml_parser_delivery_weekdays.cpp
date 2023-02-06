#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/string/join.h>

using namespace NMarket;

static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2020-11-26 00:00">
  <shop>
    <offers>
      <offer>
        <shop-sku>yml-no-delivery-weekdays</shop-sku>
        <price>1</price>
      </offer>
      <offer>
        <shop-sku>yml-empty-delivery-weekdays</shop-sku>
        <price>2</price>
        <delivery-weekdays>
        </delivery-weekdays>
      </offer>
      <offer>
        <shop-sku>yml-valid-single-delivery-weekdays</shop-sku>
        <price>3</price>
        <delivery-weekdays>
            <delivery-weekday>1</delivery-weekday>
        </delivery-weekdays>
      </offer>
      <offer>
        <shop-sku>yml-valid-multi-delivery-weekdays</shop-sku>
        <price>4</price>
        <delivery-weekdays>
            <delivery-weekday>1</delivery-weekday>
            <delivery-weekday>2</delivery-weekday>
        </delivery-weekdays>
      </offer>
      <offer>
        <shop-sku>yml-valid-multi-with-spaces-delivery-weekdays</shop-sku>
        <price>5</price>
        <delivery-weekdays>
            <delivery-weekday>1 </delivery-weekday>
            <delivery-weekday> 2 </delivery-weekday>
            <delivery-weekday>  3</delivery-weekday>
            <delivery-weekday>4</delivery-weekday>
        </delivery-weekdays>
      </offer>
      <offer>
        <shop-sku>yml-valid-long-text-delivery-weekdays</shop-sku>
        <price>6</price>
        <delivery-weekdays>
            <delivery-weekday>TUESDAY</delivery-weekday>
            <delivery-weekday>THURSDAY</delivery-weekday>
        </delivery-weekdays>
      </offer>
      <offer>
        <shop-sku>yml-valid-long-text-strange-capitalization-delivery-weekdays</shop-sku>
        <price>7</price>
        <delivery-weekdays>
          <delivery-weekday>frIdAy</delivery-weekday>
          <delivery-weekday>Thursday</delivery-weekday>
        </delivery-weekdays>
      </offer>
      <offer>
        <shop-sku>yml-invalid-delivery-weekdays</shop-sku>
        <price>8</price>
        <delivery-weekdays>
          <delivery-weekday>invalid</delivery-weekday>
        </delivery-weekdays>
      </offer>
      <offer>
        <shop-sku>yml-partially-valid-delivery-weekdays</shop-sku>
        <price>9</price>
        <delivery-weekdays>
          <delivery-weekday>2</delivery-weekday>
          <delivery-weekday>4</delivery-weekday>
          <delivery-weekday>8</delivery-weekday>
        </delivery-weekdays>
      </offer>
  </shop>
</yml_catalog>)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-no-delivery-weekdays",
        "Price": 1,
        "SupplyWeekdays": "(empty maybe)"
    },
    {
        "OfferId": "yml-empty-delivery-weekdays",
        "Price": 2,
        "SupplyWeekdays": "(empty maybe)"
    },
    {
        "OfferId": "yml-valid-single-delivery-weekdays",
        "Price": 3,
        "SupplyWeekdays": "1"
    },
    {
        "OfferId": "yml-valid-multi-delivery-weekdays",
        "Price": 4,
        "SupplyWeekdays": "1,2"
    },
    {
        "OfferId": "yml-valid-multi-with-spaces-delivery-weekdays",
        "Price": 5,
        "SupplyWeekdays": "1,2,3,4"
    },
    {
        "OfferId": "yml-valid-long-text-delivery-weekdays",
        "Price": 6,
        "SupplyWeekdays": "2,4"
    },
    {
        "OfferId": "yml-valid-long-text-strange-capitalization-delivery-weekdays",
        "Price": 7,
        "SupplyWeekdays": "5,4"
    },
    {
        "OfferId": "yml-invalid-delivery-weekdays",
        "Price": 8,
        "SupplyWeekdays": "(empty maybe)"
    },
    {
        "OfferId": "yml-partially-valid-delivery-weekdays",
        "Price": 9,
        "SupplyWeekdays": "(empty maybe)"
    },
]
)wrap");

TEST(BlueYmlParser, DeliveryWeekdays) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            const auto& originalTerms = item->DataCampOffer.content().partner().original_terms();
            if (originalTerms.has_supply_weekdays()) {
                result["SupplyWeekdays"] = JoinSeq(",", originalTerms.supply_weekdays().days());
            } else {
                result["SupplyWeekdays"] = ToString(Nothing());
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML));

    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
