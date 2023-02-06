#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>
      <offer>
        <shop-sku>yml-with-weight</shop-sku>
        <price>1</price>
        <weight>123.45</weight>
      </offer>
      <offer>
        <shop-sku>yml-without-weight</shop-sku>
        <price>2</price>
      </offer>
      <offer>
        <shop-sku>yml-with-invalid-weight</shop-sku>
        <price>3</price>
        <weight>-0.1</weight>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-weight",
        "Price": 1,
        "Weight": 123450000,
    },
    {
        "OfferId": "yml-without-weight",
        "Price": 2,
        "Weight": 0,
    },
    {
        "OfferId": "yml-with-invalid-weight",
        "Price": 3,
        "Weight": 0,
    }
]
)wrap");


TEST(BlueYmlParser, Weight) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            result["Weight"] = item->DataCampOffer.content().partner().original().weight().value_mg();
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}

TEST(BlueYmlParser, NetWeight) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        SubstGlobalCopy(INPUT_XML, "weight", "net-weight"),
        [](const TQueueItem& item) {
          NSc::TValue result;
          result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
          if (item->RawPrice) {
              result["Price"] = *item->RawPrice;
          }
          result["Weight"] = item->DataCampOffer.content().partner().original().weight_net().value_mg();
          return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(SubstGlobalCopy(EXPECTED_JSON, "weight", "net-weight"));
    ASSERT_EQ(actual, expected);
}

