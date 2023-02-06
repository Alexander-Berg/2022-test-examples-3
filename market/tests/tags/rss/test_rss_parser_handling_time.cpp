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
        <g:min_handling_time></g:min_handling_time>
        <g:max_handling_time></g:max_handling_time>
    </item>
    <item>
        <g:id>2</g:id>
        <g:min_handling_time>-1</g:min_handling_time>
        <g:max_handling_time>-3</g:max_handling_time>
    </item>
    <item>
        <g:id>3</g:id>
        <g:min_handling_time>1</g:min_handling_time>
        <g:max_handling_time>3</g:max_handling_time>
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
        "MaxHandlingTime": 3,
        "MinHandlingTime": 1,
        "OfferId": "3"
    }
]
)wrap");

}

TEST(RssFeedParser, HandlingTime) {
    const auto actual = RunRssFeedParser<NCommon::TGoogleMerchantRssParser>(
        INPUT_XML,
        [] (const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            const auto& handlingTime = item->DataCampOffer.content().type_specific_content().google_merchant().shipping_detail().handling_time();
            if (handlingTime.has_min()) {
                result["MinHandlingTime"] = handlingTime.min();
            }
            if (handlingTime.has_max()) {
                result["MaxHandlingTime"] = handlingTime.max();
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
