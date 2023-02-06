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
      <offer id="yml-with-manufacturer">
        <price>7</price>
        <manufacturer>manufacturer</manufacturer>
      </offer>
      <offer id="yml-without-manufacturer">
        <price>77</price>
      </offer>
      <offer id="yml-with-empty-manufacturer">
        <price>77</price>
        <manufacturer></manufacturer>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-manufacturer",
        "Manufacturer": "manufacturer",
    },
    {
        "OfferId": "yml-without-manufacturer",
    },
    {
        "OfferId": "yml-with-empty-manufacturer",
    },
]
)wrap");


TEST(WhiteYmlParser, Manufacturer) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            if (item->GetOriginalSpecification().manufacturer().has_value()) {
                result["Manufacturer"] = item->GetOriginalSpecification().manufacturer().value();
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
);
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
