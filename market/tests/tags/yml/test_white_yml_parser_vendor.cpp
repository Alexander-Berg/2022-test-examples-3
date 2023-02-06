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
      <offer id="w-yml-with-vendor">
        <price>1</price>
        <vendor>Vasya Pupkin</vendor>
      </offer>
      <offer id="w-yml-without-vendor">
        <price>2</price>
      </offer>
      <offer id="w-yml-with-empty-vendor">
        <price>2</price>
        <vendor></vendor>
      </offer>
      <offer id="yml-vendor-with-spaces">
        <price>2</price>
        <vendor>  My Vendor  </vendor>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "w-yml-with-vendor",
        "Price": 1,
        "Vendor": "Vasya Pupkin",
    },
    {
        "OfferId": "w-yml-without-vendor",
        "Price": 2,
    },
    {
        "OfferId": "w-yml-with-empty-vendor",
        "Price": 2,
    },
    {
        "OfferId": "yml-vendor-with-spaces",
        "Vendor": "My Vendor",
        "Price": 2,
    },
]
)wrap");


TEST(WhiteYmlParser, Vendor) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            if (item->DataCampOffer.content().partner().original().vendor().has_value()) {
                result["Vendor"] = item->DataCampOffer.content().partner().original().vendor().value();
            }
            ASSERT_FALSE(item->HasWarning);
            ASSERT_FALSE(item->HasError);
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
