#include <market/idx/feeds/qparser/tests/white_yml_test_runner.h>
#include <market/idx/feeds/qparser/src/feed_parsers/white/yml/feed_parser.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

// WARNING!! IT IS GENERATED. IT IS TEMPLATE!!!


using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>
      <offer id="yml-with-group-id" group_id="777">
        <price>7</price>
      </offer>
      <offer id="yml-with-group-id-incorrect" group_id="blah-blah">
        <price>77</price>
      </offer>
      <offer id="yml-without-group-id">
        <price>77</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


// group_id support removed as of MARKETINDEXER-45038
static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-group-id",
    },
    {
        "OfferId": "yml-with-group-id-incorrect",
    },
    {
        "OfferId": "yml-without-group-id",
    }
]
)wrap");


TEST(WhiteYmlParser, GroupId) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOriginalSpecification().group_id().has_value()) {
                result["GroupId"] = item->GetOriginalSpecification().group_id().value();
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
);
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
