#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog>
  <shop>
    <offers>
      <offer>
        <shop-sku>yml-with-single-certificate</shop-sku>
        <price>7</price>
        <certificate>RU Д-TR.НВ32.В.00164/19</certificate>
      </offer>
      <offer>
        <shop-sku>yml-with-multiple-certificate</shop-sku>
        <price>7</price>
        <certificate>RU Д-TR.НВ32.В.00164/19</certificate>
        <certificate>RU Д-FR.АД22.В.04626</certificate>
      </offer>
      <offer>
        <shop-sku>yml-without-certificate</shop-sku>
        <price>7</price>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-with-single-certificate",
        "Certificate": "RU Д-TR.НВ32.В.00164/19",
    },
    {
        "OfferId": "yml-with-multiple-certificate",
        "Certificate": "RU Д-TR.НВ32.В.00164/19, RU Д-FR.АД22.В.04626",
    },
    {
        "OfferId": "yml-without-certificate",
        "Certificate": "",
    }
]
)wrap");


TEST(BlueYmlParser, Certificate) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Certificate"] = ToString(item->GetOriginalSpecification().certificates().value());
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
