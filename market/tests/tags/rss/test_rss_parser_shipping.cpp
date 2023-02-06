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
        <g:shipping>
            <g:country></g:country>
            <g:region></g:region>
            <g:postal_code></g:postal_code>
            <g:location_id></g:location_id>
            <g:location_group_name></g:location_group_name>
            <g:service></g:service>
            <g:price></g:price>
            <g:min_handling_time></g:min_handling_time>
            <g:max_handling_time></g:max_handling_time>
            <g:min_transit_time></g:min_transit_time>
            <g:max_transit_time></g:max_transit_time>
        </g:shipping>
    </item>
    <item>
        <g:id>2</g:id>
        <g:shipping>
            <g:location_id>-1</g:location_id>
            <g:price>-15.99 USD</g:price>
            <g:min_handling_time>-1</g:min_handling_time>
            <g:max_handling_time>-3</g:max_handling_time>
            <g:min_transit_time>-2</g:min_transit_time>
            <g:max_transit_time>-5</g:max_transit_time>
        </g:shipping>
    </item>
    <item>
        <g:id>3</g:id>
        <g:shipping>
            <g:country>US</g:country>
            <g:region>MA</g:region>
            <g:postal_code>94043</g:postal_code>
            <g:location_id>21137</g:location_id>
            <g:location_group_name>west coast</g:location_group_name>
            <g:service>Ground delivery</g:service>
            <g:price>6.49 USD</g:price>
            <g:min_handling_time>1</g:min_handling_time>
            <g:max_handling_time>3</g:max_handling_time>
            <g:min_transit_time>2</g:min_transit_time>
            <g:max_transit_time>5</g:max_transit_time>
        </g:shipping>
        <g:shipping>
            <g:country>US</g:country>
            <g:region>MA</g:region>
            <g:service>Express delivery</g:service>
            <g:price>15.99 USD</g:price>
            <g:min_handling_time>1</g:min_handling_time>
            <g:max_handling_time>3</g:max_handling_time>
            <g:min_transit_time>2</g:min_transit_time>
            <g:max_transit_time>5</g:max_transit_time>
        </g:shipping>
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
        "OfferId": "3",
        "Shipping": [
            {
                "country": "US",
                "delivery_zone": {
                    "location_group_name": "west coast",
                    "location_id": 21137,
                    "postal_code": "94043",
                    "region": "MA"
                },
                "handling_time": {
                    "max": 3,
                    "min": 1
                },
                "price": {
                    "currency": "USD",
                    "price": 64900000
                },
                "service": "Ground delivery",
                "transit_time": {
                    "max": 5,
                    "min": 2
                }
            },
            {
                "country": "US",
                "delivery_zone": {
                    "region": "MA"
                },
                "handling_time": {
                    "max": 3,
                    "min": 1
                },
                "price": {
                    "currency": "USD",
                    "price": 159900000
                },
                "service": "Express delivery",
                "transit_time": {
                    "max": 5,
                    "min": 2
                }
            }
        ]
    }
]
)wrap");

}

TEST(RssFeedParser, Shipping) {
    const auto actual = RunRssFeedParser<NCommon::TGoogleMerchantRssParser>(
        INPUT_XML,
        [] (const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            const auto& merchant = item->DataCampOffer.content().type_specific_content().google_merchant();
            for (const auto& item : merchant.shipping()) {
                auto schemeItem = NSc::TValue::From(item);
                if (schemeItem.IsNull()) {
                    continue;
                }
                result["Shipping"].Push().Swap(schemeItem);
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
