#include <market/idx/feeds/qparser/tests/test_utils.h>
#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-03-26 13:10">
  <shop>
    <offers>
      <offer id="yml-white-typeless">
         <price>100</price>
      </offer>
      <offer type="qazwsxed" id="yml-white-unknown">
         <price>100</price>
      </offer>
      <offer id="yml-white-vendormodel" type="  vendor.model  ">
         <price>100</price>
      </offer>
      <offer id="yml-white-book" type="book">
         <price>100</price>
      </offer>
      <offer id="yml-white-audiobook" type="audiobook">
         <price>100</price>
      </offer>
      <offer type="artist.title" id="yml-white-artisttitle">
         <price>100</price>
      </offer>
      <offer type="general" id="yml-white-general">
         <price>100</price>
      </offer>
      <offer type="medicine" id="yml-white-medicine">
         <price>100</price>
      </offer>
      <offer type="alco" id="yml-white-alcohol">
         <price>100</price>
      </offer>
      <offer type="on.demand" id="yml-white-ondemand">
         <price>100</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-white-typeless",
        "Type": 1,
    },
    {
        "OfferId": "yml-white-unknown",
        "Type": 1,
    },
    {
        "OfferId": "yml-white-vendormodel",
        "Type": 2,
    },
    {
        "OfferId": "yml-white-book",
        "Type": 4,
    },
    {
        "OfferId": "yml-white-audiobook",
        "Type": 5,
    },
    {
        "OfferId": "yml-white-artisttitle",
        "Type": 6,
    },
    {
        "OfferId": "yml-white-general",
        "Type": 1,
    },
    {
        "OfferId": "yml-white-medicine",
        "Type": 3,
    },
    {
        "OfferId": "yml-white-alcohol",
        "Type": 9,
    },
    {
        "OfferId": "yml-white-ondemand",
        "Type": 10,
    }
]
)wrap");


TEST(WhiteYmlParser, Type) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOriginalSpecification().has_type()) {
                result["Type"] = item->GetOriginalSpecification().type().value();
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
);
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
