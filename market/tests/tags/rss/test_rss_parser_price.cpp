#include <market/idx/feeds/qparser/tests/rss_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/common/rss/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

namespace {

const TString INPUT_XML = TString(R"wrap(<?xml version="1.0" encoding="UTF-8"?>
<rss xmlns:g="http://base.google.com/ns/1.0" version="2.0">
<channel>
    <item>
        <g:id>1</g:id>
        <g:price></g:price>
    </item>
    <item>
        <g:id>2</g:id>
        <g:price>1 unknown</g:price>
    </item>
    <item>
        <g:id>3</g:id>
        <g:price>-1</g:price>
    </item>
    <item>
        <g:id>4</g:id>
        <g:price> 123.45 </g:price>
    </item>
    <item>
        <g:id>5</g:id>
        <g:price> 234.56 rub</g:price>
    </item>
    <item>
        <g:id>6</g:id>
        <g:price> 345,67 USD </g:price>
    </item>
    <item>
        <g:id>7</g:id>
        <g:price> 345,67 USD </g:price>
        <g:sale_price></g:sale_price>
    </item>
    <item>
        <g:id>8</g:id>
        <g:price> 345,67 USD </g:price>
        <g:sale_price>1 unknown</g:sale_price>
    </item>
    <item>
        <g:id>9</g:id>
        <g:price> 345,67 USD </g:price>
        <g:sale_price>-1</g:sale_price>
    </item>
    <item>
        <g:id>10</g:id>
        <g:price> 345,67 USD </g:price>
        <g:sale_price>299.99 USD</g:sale_price>
    </item>
</channel>
</rss>
)wrap");

const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "OfferId": "1"
    },
    {
        "Disabled": 1,
        "OfferId": "2"
    },
    {
        "OfferId": "3"
    },
    {
        "Currency": "RUR",
        "OfferId": "4",
        "RawPrice": 123.45
    },
    {
        "Currency": "RUR",
        "OfferId": "5",
        "RawPrice": 234.56
    },
    {
        "Currency": "USD",
        "OfferId": "6",
        "RawPrice": 345.67
    },
    {
        "Currency": "USD",
        "OfferId": "7",
        "RawPrice": 345.67
    },
    {
        "Disabled": 1,
        "OfferId": "8"
    },
    {
        "Currency": "USD",
        "OfferId": "9",
        "RawPrice": 345.67
    },
    {
        "Currency": "USD",
        "OfferId": "10",
        "RawOldPrice": 345.67,
        "RawPrice": 299.99
    }
]
)wrap");

}

TEST(RssFeedParser, PriceAndSalePrice) {
    const auto actual = RunRssFeedParser<NCommon::TGoogleMerchantRssParser>(
        INPUT_XML,
        [] (const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            if (item->IsDisabled) {
                result["Disabled"] = item->IsDisabled;
                return result;
            }
            if (item->RawPrice) {
                result["RawPrice"] = *item->RawPrice;
                result["Currency"] = item->Currency;
            }
            if (item->RawOldPrice) {
                result["RawOldPrice"] = *item->RawOldPrice;
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
