#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>
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
        <shop-sku>yml-with-vendor-code</shop-sku>
        <price>7</price>
        <vendorCode>123</vendorCode>
      </offer>
      <offer>
        <shop-sku>yml-without-vendor-code</shop-sku>
        <price>77</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-vendor-code",
        "Price": 7,
        "VendorCode": "123",
    },
    {
        "OfferId": "yml-without-vendor-code",
        "Price": 77,
    }
]
)wrap");


TEST(BlueYmlParser, VendorCode) {
const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            if (item->DataCampOffer.Getcontent().Getpartner().Getoriginal().Getvendor_code().Hasvalue()) {
                result["VendorCode"] = item->DataCampOffer.Getcontent().Getpartner().Getoriginal().Getvendor_code().Getvalue();
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
ASSERT_EQ(actual, expected);
}
