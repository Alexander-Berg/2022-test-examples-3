#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/string/join.h>


using namespace NMarket;


static const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>
      <offer>
        <shop-sku>yml-valid-dimensions</shop-sku>
        <price>1</price>
        <dimensions>65.55/50.7/20.0</dimensions>
      </offer>
      <offer>
        <shop-sku>yml-invalid-value-dimensions</shop-sku>
        <price>2</price>
        <dimensions>65.55/-50.7/20.0</dimensions>
      </offer>
      <offer>
        <shop-sku>yml-invalid-dimensions</shop-sku>
        <price>3</price>
        <dimensions>invalid</dimensions>
      </offer>
      <offer>
        <shop-sku>yml-a-lot-of-dimensions</shop-sku>
        <price>4</price>
        <dimensions>65.55/50.7/20.0/10.12</dimensions>
      </offer>
      <offer>
        <shop-sku>yml-zero-dimension</shop-sku>
        <price>5</price>
        <dimensions>65.55/0.0/20.0</dimensions>
      </offer>
      <offer>
        <shop-sku>yml-invalid-dimensions-delimiter</shop-sku>
        <price>6</price>
        <dimensions>65.55,12.0,20.0</dimensions>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "yml-valid-dimensions",
        "Price": 1,
        "Dimensions": "655500,507000,200000",
    },
    {
        "OfferId": "yml-invalid-value-dimensions",
        "Price": 2,
        "Dimensions": "(empty maybe)",
    },
    {
        "OfferId": "yml-invalid-dimensions",
        "Price": 3,
        "Dimensions": "(empty maybe)",
    },
    {
        "OfferId": "yml-a-lot-of-dimensions",
        "Price": 4,
        "Dimensions": "(empty maybe)",
    },
    {
        "OfferId": "yml-zero-dimension",
        "Price": 5,
        "Dimensions": "(empty maybe)",
    },
    {
        "OfferId": "yml-invalid-dimensions-delimiter",
        "Price": 6,
        "Dimensions": "(empty maybe)",
    }
]
)wrap");


TEST(BlueYmlParser, Dimensions) {
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            if (item->DataCampOffer.content().partner().original().dimensions().Hasheight_mkm()) {
                const auto& dimensions = item->DataCampOffer.content().partner().original().dimensions();
                result["Dimensions"] = JoinSeq(
                    ",", {dimensions.length_mkm(), dimensions.width_mkm(), dimensions.height_mkm()});
            } else {
                result["Dimensions"] = ToString(Nothing());
            }
            return result;
        },
        GetDefaultBlueFeedInfo(EFeedType::YML)
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
//
// Created by m-mikhail on 04.12.2019.
//

