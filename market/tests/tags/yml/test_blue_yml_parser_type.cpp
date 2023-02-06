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
         <shop-sku>yml-blue-typeless</shop-sku>
         <price>100</price>
      </offer>
      <offer type="qazwsxed">
         <shop-sku>yml-blue-unknown</shop-sku>
         <price>100</price>
      </offer>
      <offer type="vendor.model">
         <shop-sku>yml-blue-vendormodel</shop-sku>
         <price>100</price>
      </offer>
      <offer type="book">
         <shop-sku>yml-blue-book</shop-sku>
         <price>100</price>
      </offer>
      <offer type="audiobook">
         <shop-sku>yml-blue-audiobook</shop-sku>
         <price>100</price>
      </offer>
      <offer type="artist.title">
         <shop-sku>yml-blue-artisttitle</shop-sku>
         <price>100</price>
      </offer>
      <offer type="general">
         <shop-sku>yml-blue-general</shop-sku>
         <price>100</price>
      </offer>
      <offer type="medicine">
         <shop-sku>yml-blue-medicine</shop-sku>
         <price>100</price>
      </offer>
      <offer type="alco">
         <shop-sku>yml-blue-alcohol</shop-sku>
         <price>100</price>
      </offer>
      <offer type="tour">
         <shop-sku>yml-blue-tour</shop-sku>
         <price>100</price>
      </offer>
      <offer type="event-ticket">
         <shop-sku>yml-blue-event-ticket</shop-sku>
         <price>100</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-blue-typeless",
        "Type": 1,
    },
    {
        "OfferId": "yml-blue-unknown",
        "Type": 1,
    },
    {
        "OfferId": "yml-blue-vendormodel",
        "Type": 2,
    },
    {
        "OfferId": "yml-blue-book",
        "Type": 4,
    },
    {
        "OfferId": "yml-blue-audiobook",
        "Type": 5,
    },
    {
        "OfferId": "yml-blue-artisttitle",
        "Type": 6,
    },
    {
        "OfferId": "yml-blue-general",
        "Type": 1,
    },
    {
        "OfferId": "yml-blue-medicine",
        "Type": 3,
    },
    {
        "OfferId": "yml-blue-alcohol",
        "Type": 9,
    },
    {
        "OfferId": "yml-blue-tour",
        "Type": 1,
    },
    {
        "OfferId": "yml-blue-event-ticket",
        "Type": 1,
    }
]
)wrap");


TEST(BlueYmlParser, OfferType) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOriginalSpecification().has_type()) {
                result["Type"] = static_cast<int>(item->GetOriginalSpecification().type().value());
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}

