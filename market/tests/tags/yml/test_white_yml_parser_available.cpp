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
      <offer available="false" id="yml-with-available">
        <price>7</price>
      </offer>
      <offer id="yml-without-available">
        <price>77</price>
      </offer>
      <offer available="" id="yml-with-empty-available">
        <price>77</price>
      </offer>
      <offer id="yml-with-invalid-available" available="something">
        <price>78</price>
      </offer>
      <offer available="unknown" id="yml-with-unknown-available">
        <price>42</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-available",
        "Available": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } flag: false }",
        "Deadline": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "IsValid": 1
    },
    {
        "OfferId": "yml-without-available",
        "Available": "{  }",
        "Deadline": "{  }",
        "IsValid": 1
    },
    {
        "OfferId": "yml-with-empty-available",
        "Available": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "Deadline": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "IsValid": 1
    },
    {
        "OfferId": "yml-with-invalid-available",
        "Available": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "Deadline": "{ meta { source: PUSH_PARTNER_FEED timestamp { seconds: 1000 } applier: QPARSER } }",
        "IsValid": 1
    },
]
)wrap");


TEST(WhiteYmlParser, Available) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Available"] = ToString(item->GetOriginalPartnerDelivery().available());
            result["Deadline"] = ToString(item->GetOriginalPartnerDelivery().deadline());
            result["IsValid"] = item->IsValid;
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
);
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
