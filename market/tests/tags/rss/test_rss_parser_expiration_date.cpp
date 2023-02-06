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
        <g:expiration_date></g:expiration_date>
    </item>
    <item>
        <g:id>2</g:id>
        <g:expiration_date>unsupported</g:expiration_date>
    </item>
    <item>
        <g:id>3</g:id>
        <g:expiration_date>2016-11-25T13:00+0300</g:expiration_date>
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
        "Expiry": {
            "datetime": {
                "seconds": 1480068000
            },
            "meta": {
                "applier": "QPARSER",
                "source": "PUSH_PARTNER_FEED",
                "timestamp": {
                    "seconds": 1000
                }
            }
        },
        "OfferId": "3"
    }
]
)wrap");

}

TEST(RssFeedParser, ExpirationDate) {
    const auto actual = RunRssFeedParser<NCommon::TGoogleMerchantRssParser>(
        INPUT_XML,
        [] (const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            const auto& original = item->GetOriginalSpecification();
            if (original.has_expiry()) {
                result["Expiry"] = NSc::TValue::From(original.expiry());
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
