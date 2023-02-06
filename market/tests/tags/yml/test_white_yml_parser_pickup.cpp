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
      <offer id="yml-with-pickup-t">
        <price>1</price>
        <pickup>true</pickup>
      </offer>
      <offer id="yml-with-pickup-f">
        <price>1</price>
        <pickup>false</pickup>
      </offer>
      <offer id="yml-with-pickup-t-ws">
        <price>1</price>
        <pickup>  true  </pickup>
      </offer>
      <offer id="yml-without-pickup">
        <price>1</price>
      </offer>
      <offer id="yml-with-bad-pickup">
        <price>1</price>
        <pickup>kakashka</pickup>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-pickup-t",
        "Pickup": 1,
    },
    {
        "OfferId": "yml-with-pickup-f",
        "Pickup": 0,
    },
    {
        "OfferId": "yml-with-pickup-t-ws",
        "Pickup": 1,
    },
    {
        "OfferId": "yml-without-pickup"
    },
    {
        "OfferId": "yml-with-bad-pickup",
    }
]
)wrap");


TEST(WhiteYmlParser, Pickup) {
    const auto actual = RunWhiteYmlFeedParser<TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->GetOriginalPartnerDelivery().pickup().has_flag()) {
                result["Pickup"] = item->GetOriginalPartnerDelivery().pickup().flag();
            }
            return result;
        },
        GetDefaultWhiteFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
