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
        <g:loyalty_points></g:loyalty_points>
    </item>
    <item>
        <g:id>2</g:id>
        <g:loyalty_points>
            <g:name></g:name>
        </g:loyalty_points>
    </item>
    <item>
        <g:id>3</g:id>
        <g:loyalty_points>
            <g:name>Super discount program</g:name>
            <g:points_value>unsupported</g:points_value>
            <g:ratio></g:ratio>
        </g:loyalty_points>
    </item>
    <item>
        <g:id>4</g:id>
        <g:loyalty_points>
            <g:name>Super discount program</g:name>
            <g:points_value>-1</g:points_value>
            <g:ratio>unsupported</g:ratio>
        </g:loyalty_points>
    </item>
    <item>
        <g:id>5</g:id>
        <g:loyalty_points>
            <g:name>Super discount program</g:name>
            <g:points_value>50</g:points_value>
            <g:ratio>-0.1</g:ratio>
        </g:loyalty_points>
    </item>
    <item>
        <g:id>6</g:id>
        <g:loyalty_points>
            <g:name>Super discount program</g:name>
            <g:points_value>50</g:points_value>
            <g:ratio>0.1</g:ratio>
        </g:loyalty_points>
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
        "OfferId": "2"
    },
    {
        "LoyaltyPoints": {
            "program": "Super discount program"
        },
        "OfferId": "3"
    },
    {
        "LoyaltyPoints": {
            "program": "Super discount program"
        },
        "OfferId": "4"
    },
    {
        "LoyaltyPoints": {
            "points_value": 50,
            "program": "Super discount program"
        },
        "OfferId": "5"
    },
    {
        "LoyaltyPoints": {
            "points_value": 50,
            "program": "Super discount program",
            "ratio": 0.1
        },
        "OfferId": "6"
    }
]
)wrap");

}

TEST(RssFeedParser, LoyaltyPoints) {
    const auto actual = RunRssFeedParser<NCommon::TGoogleMerchantRssParser>(
        INPUT_XML,
        [] (const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            const auto& merchant = item->DataCampOffer.content().type_specific_content().google_merchant();
            if (merchant.has_loyalty_points()) {
                result["LoyaltyPoints"] = NSc::TValue::From(merchant.loyalty_points());
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
