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
        <g:age_group></g:age_group>
    </item>
    <item>
        <g:id>2</g:id>
        <g:age_group>1</g:age_group>
    </item>
    <item>
        <g:id>3</g:id>
        <g:age_group>unsupported</g:age_group>
    </item>
    <item>
        <g:id>4</g:id>
        <g:age_group>newborn</g:age_group>
    </item>
    <item>
        <g:id>5</g:id>
        <g:age_group> INFANT </g:age_group>
    </item>
    <item>
        <g:id>6</g:id>
        <g:age_group> kids </g:age_group>
    </item>
    <item>
        <g:id>7</g:id>
        <g:age_group>AdUlt</g:age_group>
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
        "AgeGroup": 1,
        "OfferId": "4"
    },
    {
        "AgeGroup": 2,
        "OfferId": "5"
    },
    {
        "AgeGroup": 4,
        "OfferId": "6"
    },
    {
        "AgeGroup": 5,
        "OfferId": "7"
    }
]
)wrap");

}

TEST(RssFeedParser, AgeGroup) {
    const auto actual = RunRssFeedParser<NCommon::TGoogleMerchantRssParser>(
        INPUT_XML,
        [] (const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            const auto& merchant = item->DataCampOffer.content().type_specific_content().google_merchant();
            if (merchant.has_age_group()) {
                result["AgeGroup"] = merchant.age_group();
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
