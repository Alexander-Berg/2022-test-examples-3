#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>
      <offer id="yml-with-min-quantity">
        <min-quantity>5</min-quantity>
        <price>77</price>
      </offer>
      <offer id="yml-with-min-quantity-1">
        <min_quantity>10</min_quantity>
        <price>77</price>
      </offer>
      <offer id="yml-without-min-quantity">
        <price>77</price>
      </offer>
      <offer id="yml-invalid-min-quantity">
        <min-quantity>0</min-quantity>
        <price>77</price>
      </offer>
      <offer id="yml-invalid-min-quantity-1">
        <min-quantity>-1</min-quantity>
        <price>77</price>
      </offer>
      <offer id="yml-invalid-min-quantity-2">
        <min-quantity>TwentyTwo</min-quantity>
        <price>77</price>
      </offer>
      <offer id="yml-invalid-min-quantity-3">
        <min-quantity>22TwentyTwo</min-quantity>
        <price>77</price>
      </offer>
      <offer id="yml-min-quantity-overflow">
        <min-quantity>4294967296</min-quantity>
        <price>77</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-min-quantity",
        "MinQuantity": 5,
    },
    {
        "OfferId": "yml-with-min-quantity-1",
        "MinQuantity": 10,
    },
    {
        "OfferId": "yml-without-min-quantity",
    },
    {
        "OfferId": "yml-invalid-min-quantity",
    },
    {
        "OfferId": "yml-invalid-min-quantity-1",
    },
    {
        "OfferId": "yml-invalid-min-quantity-2",
    },
    {
        "OfferId": "yml-invalid-min-quantity-3",
    },
    {
        "OfferId": "yml-min-quantity-overflow",
    },
]
)wrap");


TEST(WhiteYmlParser, MinQuantity) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOriginalTerms().has_quantity() && item->GetOriginalTerms().quantity().has_min()) {
                result["MinQuantity"] = item->GetOriginalTerms().quantity().min();
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
);
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
