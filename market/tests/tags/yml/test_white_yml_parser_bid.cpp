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
      <offer bid="123" cbid="bla-bla">
        <shop-sku>yml-with-bid</shop-sku>
        <price>7</price>
      </offer>
      <offer bid="123456789">
        <shop-sku>yml-max-bid</shop-sku>
        <price>77</price>
      </offer>
      <offer>
        <shop-sku>yml-without-bid</shop-sku>
        <price>77</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-bid",
        "Bid": 123,
    },
    {
        "OfferId": "yml-max-bid",
        "Bid": 8400,
    },
    {
        "OfferId": "yml-without-bid",
    }
]
)wrap");


TEST(BlueYmlParser, Bid) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            if (item->DataCampOffer.bids().bid().has_value()) {
              result["Bid"] = item->DataCampOffer.bids().bid().value();
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
);
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
