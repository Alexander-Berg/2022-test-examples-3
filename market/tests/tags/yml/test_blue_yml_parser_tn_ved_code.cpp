#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/string/join.h>

using namespace NMarket;

static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2020-11-26 00:00">
  <shop>
    <offers>
      <offer>
        <shop-sku>yml-no-tn-ved-codes</shop-sku>
        <price>1</price>
      </offer>
      <offer>
        <shop-sku>yml-empty-tn-ved-codes</shop-sku>
        <price>2</price>
        <tn-ved-codes></tn-ved-codes>
      </offer>
      <offer>
        <shop-sku>yml-one-tn-ved-code</shop-sku>
        <price>3</price>
        <tn-ved-codes>
          <tn-ved-code>123</tn-ved-code>
        </tn-ved-codes>
      </offer>
      <offer>
        <shop-sku>yml-many-tn-ved-codes</shop-sku>
        <price>4</price>
        <tn-ved-codes>
          <tn-ved-code>1234567890</tn-ved-code>
          <tn-ved-code>4567890123</tn-ved-code>
        </tn-ved-codes>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-no-tn-ved-codes",
        "Price": 1,
        "TnVedCodes": "(empty maybe)"
    },
    {
        "OfferId": "yml-empty-tn-ved-codes",
        "Price": 2,
        "TnVedCodes": "(empty maybe)"
    },
    {
        "OfferId": "yml-one-tn-ved-code",
        "Price": 3,
        "TnVedCodes": "123"
    },
    {
        "OfferId": "yml-many-tn-ved-codes",
        "Price": 4,
        "TnVedCodes": "1234567890,4567890123"
    },
]
)wrap");

TEST(BlueYmlParser, TnVedCodes) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            const auto& original = item->DataCampOffer.content().partner().original();
            if (original.has_tn_ved_code()) {
                result["TnVedCodes"] = JoinSeq(",", original.tn_ved_code().value());
            } else {
                result["TnVedCodes"] = ToString(Nothing());
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML));

    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
