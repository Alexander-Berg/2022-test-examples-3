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
      <offer id="0">
        <step-quantity>5</step-quantity>
        <price>77</price>
      </offer>
      <offer id="1">
        <step_quantity>10</step_quantity>
        <price>77</price>
      </offer>
      <offer id="2">
        <price>77</price>
      </offer>
      <offer id="3">
        <step-quantity>0</step-quantity>
        <price>77</price>
      </offer>
      <offer id="4">
        <step-quantity>-1</step-quantity>
        <price>77</price>
      </offer>
      <offer id="5">
        <step-quantity>TwentyTwo</step-quantity>
        <price>77</price>
      </offer>
      <offer id="6">
        <step-quantity>22TwentyTwo</step-quantity>
        <price>77</price>
      </offer>
      <offer id="7">
        <step-quantity>4294967296</step-quantity>
        <price>77</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "0",
        "StepQuantity": 5,
    },
    {
        "OfferId": "1",
        "StepQuantity": 10,
    },
    {
        "OfferId": "2",
    },
    {
        "OfferId": "3",
    },
    {
        "OfferId": "4",
    },
    {
        "OfferId": "5",
    },
    {
        "OfferId": "6",
    },
    {
        "OfferId": "7",
    },
]
)wrap");


TEST(WhiteYmlParser, StepQuantity) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
            INPUT_XML,
            [](const TQueueItem& item) {
                NSc::TValue result;
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                if (item->GetOriginalTerms().has_quantity() && item->GetOriginalTerms().quantity().has_step()) {
                    result["StepQuantity"] = item->GetOriginalTerms().quantity().step();
                }
                return result;
            },
            GetDefaultWhiteFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
