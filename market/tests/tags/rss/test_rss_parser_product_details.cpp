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
        <g:product_detail></g:product_detail>
    </item>
    <item>
        <g:id>2</g:id>
        <g:product_detail>
            <g:section_name></g:section_name>
            <g:attribute_name></g:attribute_name>
            <g:attribute_value></g:attribute_value>
        </g:product_detail>
    </item>
    <item>
        <g:id>3</g:id>
        <g:product_detail>
            <g:section_name>section 1</g:section_name>
            <g:attribute_name>attr 1</g:attribute_name>
            <g:attribute_value>val 1</g:attribute_value>
        </g:product_detail>
        <g:product_detail>
            <g:section_name>section 2</g:section_name>
            <g:attribute_name>attr 2</g:attribute_name>
            <g:attribute_value>val 2</g:attribute_value>
        </g:product_detail>
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
        "ProductDetails": [
            {
                "attribute_name": "attr 1",
                "attribute_value": "val 1",
                "section_name": "section 1"
            },
            {
                "attribute_name": "attr 2",
                "attribute_value": "val 2",
                "section_name": "section 2"
            }
        ]
    }
]
)wrap");

}

TEST(RssFeedParser, ProductDetails) {
    const auto actual = RunRssFeedParser<NCommon::TGoogleMerchantRssParser>(
        INPUT_XML,
        [] (const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            const auto& merchant = item->DataCampOffer.content().type_specific_content().google_merchant();
            for (const auto& item : merchant.product_detail()) {
                auto schemeItem = NSc::TValue::From(item);
                if (schemeItem.IsNull()) {
                    continue;
                }
                result["ProductDetails"].Push().Swap(schemeItem);
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
