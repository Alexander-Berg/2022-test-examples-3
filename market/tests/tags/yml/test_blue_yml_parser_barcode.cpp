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
        <shop-sku>yml-with-barcode</shop-sku>
        <price>7</price>
        <barcode>12345678901234567890</barcode>
        <barcode>1234567890123456789x</barcode>
        <barcode>123456789012345678901</barcode>
      </offer>
      <offer>
        <shop-sku>yml-with-empty-barcode</shop-sku>
        <barcode></barcode>
        <price>77</price>
      </offer>
      <offer>
        <shop-sku>yml-without-barcode</shop-sku>
        <price>77</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-barcode",
        "Barcode": "12345678901234567890, 1234567890123456789x, 123456789012345678901",
    },
    {
        "OfferId": "yml-with-empty-barcode",
        "Barcode": "",
    },
    {
        "OfferId": "yml-without-barcode",
    }
]
)wrap");


TEST(BlueYmlParser, Barcode) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOriginalSpecification().has_barcode()) {
                result["Barcode"] = ToString(item->GetOriginalSpecification().barcode().value());
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
);
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
