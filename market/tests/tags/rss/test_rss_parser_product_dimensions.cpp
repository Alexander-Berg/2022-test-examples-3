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
        <g:product_length></g:product_length>
        <g:product_width></g:product_width>
        <g:product_height></g:product_height>
        <g:product_weight></g:product_weight>
    </item>
    <item>
        <g:id>2</g:id>
        <g:product_length>unsupported</g:product_length>
        <g:product_width>unsupported</g:product_width>
        <g:product_height>unsupported</g:product_height>
        <g:product_weight>unsupported</g:product_weight>
    </item>
    <item>
        <g:id>3</g:id>
        <g:product_length>1.0</g:product_length>
        <g:product_width>3.28</g:product_width>
        <g:product_height>39.37</g:product_height>
        <g:product_weight>2.2</g:product_weight>
    </item>
    <item>
        <g:id>4</g:id>
        <g:product_length>-1.0 m</g:product_length>
        <g:product_width>-3.28 ft</g:product_width>
        <g:product_height>-39.37  in</g:product_height>
        <g:product_weight>-2.2 lb </g:product_weight>
    </item>
    <item>
        <g:id>5</g:id>
        <g:product_length>1.0 oz</g:product_length>
        <g:product_width>3.28 l</g:product_width>
        <g:product_height>39.37 sqft</g:product_height>
        <g:product_weight>2.2 cm</g:product_weight>
    </item>
    <item>
        <g:id>6</g:id>
        <g:product_length>1.0 m</g:product_length>
        <g:product_width>3.28 ft</g:product_width>
        <g:product_height>39.37  in</g:product_height>
        <g:product_weight>2.2 lb </g:product_weight>
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
        "OfferId": "6",
        "ProductDimensions": {
            "height_mkm": 999744,
            "length_mkm": 1000000,
            "width_mkm": 999744
        },
        "ProductWeight": 997903
    }
]
)wrap");

}

TEST(RssFeedParser, ProductDimensions) {
    const auto actual = RunRssFeedParser<NCommon::TGoogleMerchantRssParser>(
        INPUT_XML,
        [] (const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            const auto& merchant = item->DataCampOffer.content().type_specific_content().google_merchant();
            if (merchant.has_product_dimensions()) {
                result["ProductDimensions"] = NSc::TValue::From(merchant.product_dimensions());
            }
            if (merchant.has_product_weight_mg()) {
                result["ProductWeight"] = merchant.product_weight_mg();
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
