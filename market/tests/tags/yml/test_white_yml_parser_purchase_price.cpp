#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>


using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-03-26 13:10">
  <shop>
    <offers>
      <offer id="white-without-purchase-price"><price>1</price></offer>
      <offer id="white-with-zero-purchase-price"><price>1</price><purchase_price>0</purchase_price></offer>
      <offer id="white-purchase-price-8990"><price>1</price><purchase_price>8990</purchase_price></offer>
      <offer id="white-purchase-price-8990-01"><price>1</price><purchase_price>8990.01</purchase_price></offer>
      <offer id="white-purchase-price-8999-99"><price>1</price><purchase_price>   8999,99   </purchase_price></offer>
      <offer id="white-purchase-price-lalala"><price>1</price><purchase_price> lalala </purchase_price></offer>
      <offer id="white-purchase-price--9001"><price>1</price><purchase_price>-9001</purchase_price></offer>
      <offer id="white-with-empty-purchase-price"><price>1</price><purchase_price></purchase_price></offer>
    </offers>
  </shop>
</yml_catalog>
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "white-without-purchase-price",
    },
    {
        "OfferId": "white-with-zero-purchase-price",
        "PurchasePrice": 0,
        "Meta": "{ source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER }",
    },
    {
        "OfferId": "white-purchase-price-8990",
        "PurchasePrice": 8990,
        "Meta": "{ source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER }",
    },
    {
        "OfferId": "white-purchase-price-8990-01",
        "PurchasePrice": 8990.01,
        "Meta": "{ source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER }",
    },
    {
        "OfferId": "white-purchase-price-8999-99",
        "PurchasePrice": 8999.99,
        "Meta": "{ source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER }",
    },
    {
        "OfferId": "white-purchase-price-lalala",
        "Meta": "{ source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER }",
    },
    {
        "OfferId": "white-purchase-price--9001",
        "Meta": "{ source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER }",
    },
    {
        "OfferId": "white-with-empty-purchase-price",
        "Meta": "{ source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER }",
    },
]
)wrap");

TEST(WhiteYmlParser, PurchasePrice) {
const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            const auto& purchase_price = item->DataCampOffer.price().purchase_price();

            if(purchase_price.Hasbinary_price()) {
                result["PurchasePrice"] = TFixedPointNumber::CreateFromRawValue(purchase_price.binary_price().price()).AsDouble();
            }
            if (purchase_price.has_meta()) {
                result["Meta"] = ToString(item->DataCampOffer.price().purchase_price().meta());
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
);
const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);

ASSERT_EQ(actual, expected);
}
