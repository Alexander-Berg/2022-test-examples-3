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
        <g:custom_label_0></g:custom_label_0>
        <g:custom_label_1></g:custom_label_1>
        <g:custom_label_2></g:custom_label_2>
        <g:custom_label_3></g:custom_label_3>
        <g:custom_label_4></g:custom_label_4>
    </item>
    <item>
        <g:id>2</g:id>
        <g:custom_label_0>0</g:custom_label_0>
    </item>
    <item>
        <g:id>3</g:id>
        <g:custom_label_1>1</g:custom_label_1>
    </item>
    <item>
        <g:id>4</g:id>
        <g:custom_label_2>2</g:custom_label_2>
    </item>
    <item>
        <g:id>5</g:id>
        <g:custom_label_3>3</g:custom_label_3>
    </item>
    <item>
        <g:id>6</g:id>
        <g:custom_label_4>4</g:custom_label_4>
    </item>
    <item>
        <g:id>7</g:id>
        <g:custom_label_5>5</g:custom_label_5>
    </item>
    <item>
        <g:id>8</g:id>
        <g:custom_label_0>0</g:custom_label_0>
        <g:custom_label_1>1</g:custom_label_1>
        <g:custom_label_2>2</g:custom_label_2>
        <g:custom_label_3>3</g:custom_label_3>
        <g:custom_label_4>4</g:custom_label_4>
        <g:custom_label_5>5</g:custom_label_5>
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
        "CustomLabel0": "0",
        "OfferId": "2"
    },
    {
        "CustomLabel1": "1",
        "OfferId": "3"
    },
    {
        "CustomLabel2": "2",
        "OfferId": "4"
    },
    {
        "CustomLabel3": "3",
        "OfferId": "5"
    },
    {
        "CustomLabel4": "4",
        "OfferId": "6"
    },
    {
        "OfferId": "7"
    },
    {
        "CustomLabel0": "0",
        "CustomLabel1": "1",
        "CustomLabel2": "2",
        "CustomLabel3": "3",
        "CustomLabel4": "4",
        "OfferId": "8"
    }
]
)wrap");

}

TEST(RssFeedParser, CustomLabel) {
    const auto actual = RunRssFeedParser<NCommon::TGoogleMerchantRssParser>(
        INPUT_XML,
        [] (const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            const auto& merchant = item->DataCampOffer.content().type_specific_content().google_merchant();
            for (const auto& item : merchant.custom_labels()) {
                result["CustomLabel" + ToString(item.first)] = item.second;
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
