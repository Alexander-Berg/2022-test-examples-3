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
        <g:is_bundle></g:is_bundle>
    </item>
    <item>
        <g:id>2</g:id>
        <g:is_bundle>unsupported</g:is_bundle>
    </item>
    <item>
        <g:id>3</g:id>
        <g:is_bundle>yes</g:is_bundle>
    </item>
    <item>
        <g:id>4</g:id>
        <g:is_bundle> FaLsE </g:is_bundle>
    </item>
    <item>
        <g:id>5</g:id>
        <g:is_bundle> 1 </g:is_bundle>
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
        "IsBundle": 1,
        "OfferId": "3"
    },
    {
        "IsBundle": 0,
        "OfferId": "4"
    },
    {
        "IsBundle": 1,
        "OfferId": "5"
    }
]
)wrap");

}

TEST(RssFeedParser, IsBundle) {
    const auto actual = RunRssFeedParser<NCommon::TGoogleMerchantRssParser>(
        INPUT_XML,
        [] (const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            const auto& merchant = item->DataCampOffer.content().type_specific_content().google_merchant();
            if (merchant.has_is_bundle()) {
                result["IsBundle"] = merchant.is_bundle();
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
