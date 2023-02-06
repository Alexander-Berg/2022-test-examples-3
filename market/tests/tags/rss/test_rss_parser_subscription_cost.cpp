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
        <g:subscription_cost></g:subscription_cost>
    </item>
    <item>
        <g:id>2</g:id>
        <g:subscription_cost>
            <g:period></g:period>
        </g:subscription_cost>
    </item>
    <item>
        <g:id>3</g:id>
        <g:subscription_cost>
            <g:period>unsupported</g:period>
        </g:subscription_cost>
    </item>
    <item>
        <g:id>4</g:id>
        <g:subscription_cost>
            <g:period>month</g:period>
            <g:amount></g:amount>
        </g:subscription_cost>
    </item>
    <item>
        <g:id>5</g:id>
        <g:subscription_cost>
            <g:period>month</g:period>
            <g:amount>unsupported</g:amount>
        </g:subscription_cost>
    </item>
    <item>
        <g:id>6</g:id>
        <g:subscription_cost>
            <g:period>month</g:period>
            <g:amount>-1</g:amount>
        </g:subscription_cost>
    </item>
    <item>
        <g:id>7</g:id>
        <g:subscription_cost>
            <g:period>month</g:period>
            <g:period_length></g:period_length>
        </g:subscription_cost>
    </item>
    <item>
        <g:id>8</g:id>
        <g:subscription_cost>
            <g:period>month</g:period>
            <g:period_length>-1</g:period_length>
        </g:subscription_cost>
    </item>
    <item>
        <g:id>9</g:id>
        <g:subscription_cost>
            <g:period>month</g:period>
            <g:period_length>6</g:period_length>
        </g:subscription_cost>
    </item>
    <item>
        <g:id>10</g:id>
        <g:subscription_cost>
            <g:period>month</g:period>
            <g:period_length>6</g:period_length>
            <g:amount>9.99 RUB</g:amount>
        </g:subscription_cost>
    </item>
    <item>
        <g:id>11</g:id>
        <g:subscription_cost>
            <g:period>year</g:period>
            <g:period_length>1</g:period_length>
            <g:amount>15.99 RUB</g:amount>
        </g:subscription_cost>
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
        "OfferId": "3"
    },
    {
        "OfferId": "4",
        "SubscriptionCost": {
            "period": "GM_PERIOD_MONTH"
        }
    },
    {
        "OfferId": "5",
        "SubscriptionCost": {
            "period": "GM_PERIOD_MONTH"
        }
    },
    {
        "OfferId": "6",
        "SubscriptionCost": {
            "period": "GM_PERIOD_MONTH"
        }
    },
    {
        "OfferId": "7",
        "SubscriptionCost": {
            "period": "GM_PERIOD_MONTH"
        }
    },
    {
        "OfferId": "8",
        "SubscriptionCost": {
            "period": "GM_PERIOD_MONTH"
        }
    },
    {
        "OfferId": "9",
        "SubscriptionCost": {
            "period": "GM_PERIOD_MONTH",
            "period_length": 6
        }
    },
    {
        "OfferId": "10",
        "SubscriptionCost": {
            "amount": {
                "currency": "RUR",
                "price": 99900000
            },
            "period": "GM_PERIOD_MONTH",
            "period_length": 6
        }
    },
    {
        "OfferId": "11",
        "SubscriptionCost": {
            "amount": {
                "currency": "RUR",
                "price": 159900000
            },
            "period": "GM_PERIOD_YEAR",
            "period_length": 1
        }
    }
]
)wrap");

}

TEST(RssFeedParser, SubscriptionCost) {
    const auto actual = RunRssFeedParser<NCommon::TGoogleMerchantRssParser>(
        INPUT_XML,
        [] (const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            const auto& merchant = item->DataCampOffer.content().type_specific_content().google_merchant();
            if (merchant.has_subscription_cost()) {
                result["SubscriptionCost"] = NSc::TValue::From(merchant.subscription_cost());
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
