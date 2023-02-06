#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/csv/feed_parser.h>
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
        <shop-sku>yml-with-manufacturer-warranty</shop-sku>
        <price>1</price>
        <manufacturer_warranty>false</manufacturer_warranty>
      </offer>
      <offer>
        <shop-sku>yml-without-manufacturer-warranty</shop-sku>
        <price>2</price>
      </offer>
      <offer>
        <shop-sku>yml-with-empty-manufacturer-warranty</shop-sku>
        <price>3</price>
        <manufacturer_warranty></manufacturer_warranty>
      </offer>
      <offer>
        <shop-sku>yml-with-invalid-manufacturer-warranty</shop-sku>
        <price>4</price>
        <manufacturer_warranty>lalala</manufacturer_warranty>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-manufacturer-warranty",
        "Price": 1,
        "ManufacturerWarranty": "0",
    },
    {
        "OfferId": "yml-without-manufacturer-warranty",
        "Price": 2,
    },
    {
        "OfferId": "yml-with-empty-manufacturer-warranty",
        "Price": 3,
    },
    {
        "OfferId": "yml-with-invalid-manufacturer-warranty",
        "Price": 4,
    },
]
)wrap");


TEST(BlueYmlParser, ManufacturerWarranty) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            if (item->DataCampOffer.content().partner().original().manufacturer_warranty().has_flag()) {
              result["ManufacturerWarranty"] = ToString(
                  item->DataCampOffer.content().partner().original().manufacturer_warranty().flag());
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
