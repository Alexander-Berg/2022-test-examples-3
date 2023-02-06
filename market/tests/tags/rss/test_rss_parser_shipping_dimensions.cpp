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
        <g:shipping_length></g:shipping_length>
        <g:shipping_width></g:shipping_width>
        <g:shipping_height></g:shipping_height>
        <g:shipping_weight></g:shipping_weight>
    </item>
    <item>
        <g:id>2</g:id>
        <g:shipping_length>unsupported</g:shipping_length>
        <g:shipping_width>unsupported</g:shipping_width>
        <g:shipping_height>unsupported</g:shipping_height>
        <g:shipping_weight>unsupported</g:shipping_weight>
    </item>
    <item>
        <g:id>3</g:id>
        <g:shipping_length>1.0</g:shipping_length>
        <g:shipping_width>3.28</g:shipping_width>
        <g:shipping_height>39.37</g:shipping_height>
        <g:shipping_weight>2.2</g:shipping_weight>
    </item>
    <item>
        <g:id>4</g:id>
        <g:shipping_length>-1.0 m</g:shipping_length>
        <g:shipping_width>-3.28 ft</g:shipping_width>
        <g:shipping_height>-39.37  in</g:shipping_height>
        <g:shipping_weight>-2.2 lb </g:shipping_weight>
    </item>
    <item>
        <g:id>5</g:id>
        <g:shipping_length>1.0 oz</g:shipping_length>
        <g:shipping_width>3.28 l</g:shipping_width>
        <g:shipping_height>39.37 sqft</g:shipping_height>
        <g:shipping_weight>2.2 cm</g:shipping_weight>
    </item>
    <item>
        <g:id>6</g:id>
        <g:shipping_length>1.0 m</g:shipping_length>
        <g:shipping_width>3.28 ft</g:shipping_width>
        <g:shipping_height>39.37  in</g:shipping_height>
        <g:shipping_weight>2.2 lb </g:shipping_weight>
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
        "OfferId": "4"
    },
    {
        "OfferId": "5"
    },
    {
        "Dimensions": {
            "height_mkm": 999744,
            "length_mkm": 1000000,
            "meta": {
                "applier": "QPARSER",
                "source": "PUSH_PARTNER_FEED",
                "timestamp": {
                    "seconds": 1000
                }
            },
            "width_mkm": 999744
        },
        "MerchantShippingDimensions": {
            "height_mkm": 999744,
            "length_mkm": 1000000,
            "width_mkm": 999744
        },
        "MerchantShippingWeight": 997903,
        "OfferId": "6",
        "Weight": 997903
    }
]
)wrap");

}

TEST(RssFeedParser, ShippingDimensions) {
    const auto actual = RunRssFeedParser<NCommon::TGoogleMerchantRssParser>(
        INPUT_XML,
        [] (const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            const auto& merchant = item->DataCampOffer.content().type_specific_content().google_merchant();
            if (merchant.shipping_detail().has_shipping_dimensions()) {
                result["MerchantShippingDimensions"] = NSc::TValue::From(merchant.shipping_detail().shipping_dimensions());
            }
            if (merchant.shipping_detail().has_shipping_weight_mg()) {
                result["MerchantShippingWeight"] = merchant.shipping_detail().shipping_weight_mg();
            }

            const auto& original = item->GetOriginalSpecification();
            if (original.has_dimensions()) {
                result["Dimensions"] = NSc::TValue::From(original.dimensions());
            }
            if (original.has_weight()) {
                result["Weight"] = original.weight().value_mg();
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
