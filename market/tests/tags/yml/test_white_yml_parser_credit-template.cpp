#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

// WARNING!! IT IS GENERATED. IT IS TEMPLATE!!!


using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>
      <offer>
        <shop-sku>yml-with-credit-template</shop-sku>
        <price>7</price>
        <credit-template>credit-template</credit-template>
      </offer>
      <offer>
        <shop-sku>yml-without-credit-template</shop-sku>
        <price>77</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-credit-template",
        "CreditTemplate": "credit-template",
    },
    {
        "OfferId": "yml-without-credit-template",
        "CreditTemplate": "",
    }
]
)wrap");


TEST(BlueYmlParser, CreditTemplate) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["CreditTemplate"] = item->GetOriginalSpecification().credit-template().value();
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
);
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
