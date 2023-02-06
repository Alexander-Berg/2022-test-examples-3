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
        <shop-sku>yml-with-valid-url</shop-sku>
        <price>1</price>
        <url>http://www.test.com/test1</url>
      </offer>
      <offer>
        <shop-sku>yml-with-invalid-url</shop-sku>
        <price>2</price>
        <url>invalid</url>
      </offer>
      <offer>
        <shop-sku>yml-with-empty-url</shop-sku>
        <price>3</price>
        <url></url>
      </offer>
      <offer>
        <shop-sku>yml-no-url</shop-sku>
        <price>4</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-valid-url",
        "Price": 1,
        "Url": "http://www.test.com/test1",
    },
    {
        "OfferId": "yml-with-invalid-url",
        "Price": 2,
        "Url": "invalid",
    },
    {
        "OfferId": "yml-with-empty-url",
        "Price": 3,
        "Url": "",
    },
    {
        "OfferId": "yml-no-url",
        "Price": 4,
    }
]
)wrap");


TEST(BlueYmlParser, Url) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            if (item->GetOriginalSpecification().has_url()) {
              result["Url"] = item->GetOriginalSpecification().url().value();
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
