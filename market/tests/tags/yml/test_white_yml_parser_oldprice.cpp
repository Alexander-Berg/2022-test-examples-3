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
      <offer id="white-without-oldprice"><price>1990.00</price></offer>
      <offer id="white-oldprice-8990"><price>1990.00</price><oldprice>8990</oldprice></offer>
      <offer id="white-oldprice-8990-01"><price>1990.00</price><oldprice>8990.01</oldprice></offer>
      <offer id="white-oldprice-8999-99"><price>1990.00</price><oldprice>   8999,99   </oldprice></offer>
      <offer id="white-oldprice-lalala"><price>1990.00</price><oldprice>lalala</oldprice></offer>
      <offer id="white-oldprice--9001"><price>100</price><oldprice>-9001</oldprice></offer>
    </offers>
  </shop>
</yml_catalog>)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "white-without-oldprice",
        "OldPrice": "(empty maybe)",
    },
    {
        "OfferId": "white-oldprice-8990",
        "OldPrice": "8990",
    },
    {
        "OfferId": "white-oldprice-8990-01",
        "OldPrice": "8990.01",
    },
    {
        "OfferId": "white-oldprice-8999-99",
        "OldPrice": "8999.99",
    },
    {
        "OfferId": "white-oldprice-lalala",
        "OldPrice": "(empty maybe)",
    },
    {
        "OfferId": "white-oldprice--9001",
        "OldPrice": "(empty maybe)",
    },
]
)wrap");


TEST(WhiteYmlParser, OldPrice) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["OldPrice"] = ToString(item->RawOldPrice);
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
