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
        <g:availability></g:availability>
    </item>
    <item>
        <g:id>2</g:id>
        <g:availability>unsupported</g:availability>
    </item>
    <item>
        <g:id>3</g:id>
        <g:availability> in stock </g:availability>
    </item>
    <item>
        <g:id>4</g:id>
        <g:availability>OUTOFSTOCK</g:availability>
    </item>
    <item>
        <g:id>5</g:id>
        <g:availability>in-stock</g:availability>
    </item>
    <item>
        <g:id>6</g:id>
        <g:availability> OUT_OF_STOCK </g:availability>
    </item>
    <item>
        <g:id>7</g:id>
        <g:availability> pre order </g:availability>
    </item>

    <item>
        <g:id>8</g:id>
        <g:availability>backorder</g:availability>
    </item>
</channel>
</rss>
)wrap");

const TString EXPECTED_JSON = TString(R"wrap(
[
    {
        "Available": 0,
        "OfferId": "1"
    },
    {
        "Available": 0,
        "OfferId": "2"
    },
    {
        "Availability": 1,
        "Available": 1,
        "OfferId": "3"
    },
    {
        "Availability": 2,
        "Available": 0,
        "OfferId": "4"
    },
    {
        "Availability": 1,
        "Available": 1,
        "OfferId": "5"
    },
    {
        "Availability": 2,
        "Available": 0,
        "OfferId": "6"
    },
    {
        "Availability": 3,
        "Available": 0,
        "OfferId": "7"
    },
    {
        "Availability": 4,
        "Available": 0,
        "OfferId": "8"
    }
]
)wrap");

}

TEST(RssFeedParser, Availability) {
    const auto actual = RunRssFeedParser<NCommon::TGoogleMerchantRssParser>(
        INPUT_XML,
        [] (const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();
            result["Available"] = item->DataCampOffer.delivery().partner().original().available().flag();

            const auto& merchant = item->DataCampOffer.content().type_specific_content().google_merchant();
            if (merchant.has_availability()) {
                result["Availability"] = merchant.availability();
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
