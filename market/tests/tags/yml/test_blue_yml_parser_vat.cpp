#include <market/idx/feeds/qparser/tests/blue_yml_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/blue/yml/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;


static const TString INPUT_XML_1(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>
      <offer>
        <shop-sku>yml-with-vat-18</shop-sku>
        <price>1</price>
        <vat>VAT_18</vat>
      </offer>
      <offer>
        <shop-sku>yml-with-invalid-vat</shop-sku>
        <price>2</price>
        <vat>invalid</vat>
      </offer>
      <offer>
        <shop-sku>yml-with-invalid-int-vat</shop-sku>
        <price>3</price>
        <vat>-1</vat>
      </offer>
      <offer>
        <shop-sku>yml-without-vat</shop-sku>
        <price>4</price>
      </offer>
      <offer>
        <shop-sku>yml-with-no-vat</shop-sku>
        <price>5</price>
        <vat>NO_VAT</vat>
      </offer>
      <offer>
        <shop-sku>yml-with-vat-repr</shop-sku>
        <price>6</price>
        <vat>VAT_10</vat>
      </offer>
    </offers>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON_1 = TString(R"wrap(
[
    {
        "OfferId": "yml-with-vat-18",
        "Price": 1,
        "Vat": "1",
        "RawVatSource": "VAT_18",
    },
    {
        "OfferId": "yml-with-invalid-vat",
        "Price": 2,
        "Vat": "7",
        "RawVatSource": "invalid",
    },
    {
        "OfferId": "yml-with-invalid-int-vat",
        "Price": 3,
        "Vat": "7",
        "RawVatSource": "-1"
    },
    {
        "OfferId": "yml-without-vat",
        "Price": 4,
        "Vat": "7",
        "RawVatSource": "(empty maybe)",
    },
    {
        "OfferId": "yml-with-no-vat",
        "Price": 5,
        "Vat": "6",
        "RawVatSource": "NO_VAT",
    },
    {
        "OfferId": "yml-with-vat-repr",
        "Price": 6,
        "Vat": "2",
        "RawVatSource": "VAT_10",
    },
]
)wrap");


TEST(BlueYmlParser, VatFromBasicPrice) {
    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::YML);
    feedInfo.Vat = 7;
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML_1,
        [&feedInfo](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            // берем ндс из базовой цены
            const auto& dataCampOfferPrice = item->DataCampOffer.price();
            const auto vat = dataCampOfferPrice.basic().has_vat() ? dataCampOfferPrice.basic().vat() : feedInfo.Vat;
            result["Vat"] = ToString(vat);
            result["RawVatSource"] = ToString(item->RawVatSource);
            return result;
        },
        feedInfo
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON_1);
    ASSERT_EQ(actual, expected);
}

static const TString INPUT_XML_2(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE yml_catalog SYSTEM 'shops.dtd'>
<yml_catalog date="2019-12-03 00:00">
  <shop>
    <offers>
      <offer>
        <shop-sku>yml-with-vat-18</shop-sku>
        <price>1</price>
        <vat>VAT_18</vat>
      </offer>
      <offer>
        <shop-sku>yml-without-vat</shop-sku>
        <price>4</price>
      </offer>
  </shop>
</yml_catalog>)wrap");


static const TString EXPECTED_JSON_2 = TString(R"wrap(
[
    {
        "OfferId": "yml-with-vat-18",
        "Price": 1,
        "Vat": 1,
        "RawVatSource": "VAT_18",
    },
    {
        "OfferId": "yml-without-vat",
        "Price": 4,
        "RawVatSource": "(empty maybe)",
    },
]
)wrap");

TEST(BlueYmlParser, VatFromOriginalPriceFields) {
    // проверяем, что ндс также кладется в поле original_price_fields
    auto feedInfo = GetDefaultBlueFeedInfo(EFeedType::YML);
    const auto actual = RunBlueYmlFeedParser<NBlue::TYmlFeedParser>(
        INPUT_XML_2,
        [](const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->RawPrice) {
                result["Price"] = *item->RawPrice;
            }
            // берем ндс из оригинальных ценовых полей
            if (item->DataCampOffer.price().original_price_fields().has_vat()) {
                const auto vat = item->DataCampOffer.price().original_price_fields().vat().value();
                result["Vat"] = vat;
            }
            result["RawVatSource"] = ToString(item->RawVatSource);
            return result;
        },
        feedInfo
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON_2);
    ASSERT_EQ(actual, expected);
}
