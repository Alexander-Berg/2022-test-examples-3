#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <functional>

using namespace NMarket;

TEST(BlueYmlParser, NonuniqShopSku) {
    const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog date="2019-03-26 13:10">
      <shop>
        <offers>
          <offer>
            <shop-sku>bububu</shop-sku>
            <price>28490.00</price>
          </offer>
          <offer>
            <shop-sku>
                bububu
            </shop-sku>
            <price>39490.00</price>
          </offer>
          <offer>
            <shop-sku>bububu</shop-sku>
            <price>100</price>
          </offer>
        </offers>
      </shop>
    </yml_catalog>)wrap");

    const TString EXPECTED_JSON = TString(R"wrap(
    [
        {
            "OfferId": "bububu",
            "Price": 28490.00,
        },
    ]
    )wrap");

    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}

TEST(BlueYmlParser, WithOfferId) {
    //MARKETINDEXER-29624

    const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog date="2019-03-26 13:10">
      <shop>
        <offers>
          <offer id="100500">
            <price>0</price>
            <shop-sku>sku</shop-sku>
          </offer>
          <offer>
            <shop-sku>100500</shop-sku>
            <price>100.00</price>
          </offer>
        </offers>
      </shop>
    </yml_catalog>)wrap");

    const TString EXPECTED_JSON(R"wrap(
    [
        {
            "OfferId": "sku",
            "IsValid": 1,
        },
        {
            "OfferId": "100500",
            "Price": 100.00,
            "IsValid": 1,
        },
    ]
    )wrap");

    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
            INPUT_XML,
            [](const TQueueItem& item) {
                NSc::TValue result;
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                if (item->RawPrice) {
                    result["Price"] = *item->RawPrice;
                }
                result["IsValid"] = item->IsValid;
                return result;
            }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}

TEST(BlueYmlParser, InvalidOfferWithCorrectSku) {
    //MARKETINDEXER-29624

    const TString INPUT_XML(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
    <yml_catalog date="2019-03-26 13:10">
      <shop>
        <offers>
          <offer>
            <shop-sku>sku</shop-sku>
            <price>0</price>
          </offer>
          <offer>
            <shop-sku>sku</shop-sku>
            <price>100.00</price>
          </offer>
        </offers>
      </shop>
    </yml_catalog>)wrap");

    const TString EXPECTED_JSON(R"wrap(
    [
        {
            "OfferId": "sku",
            "IsValid": 1,
        },
    ]
    )wrap");

    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
            INPUT_XML,
            [](const TQueueItem& item) {
                NSc::TValue result;
                result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
                if (item->RawPrice) {
                    result["Price"] = *item->RawPrice;
                }
                result["IsValid"] = item->IsValid;
                return result;
            }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
