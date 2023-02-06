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
        <g:installment></g:installment>
    </item>
    <item>
        <g:id>2</g:id>
        <g:installment>
            <g:months>5</g:months>
        </g:installment>
    </item>
    <item>
        <g:id>3</g:id>
        <g:installment>
            <g:amount>19,99 </g:amount>
        </g:installment>
    </item>
    <item>
        <g:id>4</g:id>
        <g:installment>
            <g:months>-1</g:months>
            <g:amount>19,99 </g:amount>
        </g:installment>
    </item>
    <item>
        <g:id>5</g:id>
        <g:installment>
            <g:months>5</g:months>
            <g:amount>-19,99 </g:amount>
        </g:installment>
    </item>
    <item>
        <g:id>6</g:id>
        <g:installment>
            <g:months>5</g:months>
            <g:amount>19,99 USD</g:amount>
        </g:installment>
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
        "Installment": {
            "months": 5
        },
        "OfferId": "2"
    },
    {
        "Installment": {
            "amount": {
                "currency": "RUR",
                "price": 199900000
            }
        },
        "OfferId": "3"
    },
    {
        "Installment": {
            "amount": {
                "currency": "RUR",
                "price": 199900000
            }
        },
        "OfferId": "4"
    },
    {
        "Installment": {
            "months": 5
        },
        "OfferId": "5"
    },
    {
        "Installment": {
            "amount": {
                "currency": "USD",
                "price": 199900000
            },
            "months": 5
        },
        "OfferId": "6"
    }
]
)wrap");

}

TEST(RssFeedParser, Installment) {
    const auto actual = RunRssFeedParser<NCommon::TGoogleMerchantRssParser>(
        INPUT_XML,
        [] (const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            const auto& merchant = item->DataCampOffer.content().type_specific_content().google_merchant();
            if (merchant.has_installment()) {
                result["Installment"] = NSc::TValue::From(merchant.installment());
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
