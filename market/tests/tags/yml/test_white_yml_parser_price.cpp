#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>
#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <functional>

using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-03-26 13:10">
  <shop>
    <offers>
      <offer id="white-without-price"/>
      <offer id="white-with-zero-price"><price>0</price></offer>
      <offer id="white-price-8990"><price>8990</price></offer>
      <offer id="white-price-8990-01"><price>8990.01</price></offer>
      <offer id="white-price-8999-99"><price  from="true">   8999,99   </price></offer>
      <offer id="white-price-lalala"><price> lalala </price></offer>
      <offer id="white-price--9001"><price>-9001</price></offer>
    </offers>
  </shop>
</yml_catalog>
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "white-without-price",
        "RawPrice": "(empty maybe)",
        "IsValid": 1,
    },
    {
        "OfferId": "white-with-zero-price",
        "RawPrice": "0",
        "IsValid": 1,
    },
    {
        "OfferId": "white-price-8990",
        "Price": 8990,
        "RawPrice": "8990",
        "IsValid": 1,
    },
    {
        "OfferId": "white-price-8990-01",
        "Price": 8990.01,
        "RawPrice": "8990.01",
        "IsValid": 1,
    },
    {
        "OfferId": "white-price-8999-99",
        "Price": 8999.99,
        "RawPrice": "8999,99",
        "From": 1,
        "IsValid": 1,
    },
    {
        "OfferId": "white-price-lalala",
        "RawPrice": "lalala",
        "IsValid": 1,
    },
    {
        "OfferId": "white-price--9001",
        "RawPrice": "-9001",
        "IsValid": 1,
    },
]
)wrap");


TEST(WhiteYmlParser, price) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }

            if (item->DataCampOffer.Getcontent().Getpartner().Getoriginal().Getprice_from().Getflag()) {
                result["From"] = true;
            }
            result["RawPrice"] = ToString(item->RawOriginalPrice);
            result["IsValid"] = item->IsValid;
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
