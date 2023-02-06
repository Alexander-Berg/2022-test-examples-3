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
        <g:size></g:size>
        <g:size_type></g:size_type>
        <g:size_system></g:size_system>
    </item>
    <item>
        <g:id>2</g:id>
        <g:size>XL</g:size>
        <g:size_type>unsupported</g:size_type>
        <g:size_system>unsupported</g:size_system>
    </item>
    <item>
        <g:id>3</g:id>
        <g:size>XL</g:size>
        <g:size_type>regular</g:size_type>
        <g:size_system>AU</g:size_system>
    </item>
    <item>
        <g:id>4</g:id>
        <g:size>XL</g:size>
        <g:size_type>PETITE</g:size_type>
        <g:size_system>br</g:size_system>
    </item>
    <item>
        <g:id>5</g:id>
        <g:size>XL</g:size>
        <g:size_type>plus</g:size_type>
        <g:size_system> CN </g:size_system>
    </item>
    <item>
        <g:id>6</g:id>
        <g:size>XL</g:size>
        <g:size_type> TALL </g:size_type>
        <g:size_system>DE</g:size_system>
    </item>
    <item>
        <g:id>7</g:id>
        <g:size>XL</g:size>
        <g:size_type>BiG</g:size_type>
        <g:size_system>EU</g:size_system>
    </item>
    <item>
        <g:id>8</g:id>
        <g:size>XL</g:size>
        <g:size_type>maternity</g:size_type>
        <g:size_system>FR</g:size_system>
    </item>
    <item>
        <g:id>9</g:id>
        <g:size_system>IT</g:size_system>
    </item>
    <item>
        <g:id>10</g:id>
        <g:size_system>JP</g:size_system>
    </item>
    <item>
        <g:id>11</g:id>
        <g:size_system>MEX</g:size_system>
    </item>
    <item>
        <g:id>12</g:id>
        <g:size_system>UK</g:size_system>
    </item>
    <item>
        <g:id>13</g:id>
        <g:size_system>Us</g:size_system>
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
        "OfferId": "2",
        "Size": {
            "size": "XL"
        }
    },
    {
        "OfferId": "3",
        "Size": {
            "size": "XL",
            "size_system": "GM_SIZE_SYSTEM_AU",
            "size_type": "GM_SIZE_TYPE_REGULAR"
        }
    },
    {
        "OfferId": "4",
        "Size": {
            "size": "XL",
            "size_system": "GM_SIZE_SYSTEM_BR",
            "size_type": "GM_SIZE_TYPE_PETITE"
        }
    },
    {
        "OfferId": "5",
        "Size": {
            "size": "XL",
            "size_system": "GM_SIZE_SYSTEM_CN",
            "size_type": "GM_SIZE_TYPE_PLUS"
        }
    },
    {
        "OfferId": "6",
        "Size": {
            "size": "XL",
            "size_system": "GM_SIZE_SYSTEM_DE",
            "size_type": "GM_SIZE_TYPE_TALL"
        }
    },
    {
        "OfferId": "7",
        "Size": {
            "size": "XL",
            "size_system": "GM_SIZE_SYSTEM_EU",
            "size_type": "GM_SIZE_TYPE_BIG"
        }
    },
    {
        "OfferId": "8",
        "Size": {
            "size": "XL",
            "size_system": "GM_SIZE_SYSTEM_FR",
            "size_type": "GM_SIZE_TYPE_MATERNITY"
        }
    },
    {
        "OfferId": "9",
        "Size": {
            "size_system": "GM_SIZE_SYSTEM_IT"
        }
    },
    {
        "OfferId": "10",
        "Size": {
            "size_system": "GM_SIZE_SYSTEM_JP"
        }
    },
    {
        "OfferId": "11",
        "Size": {
            "size_system": "GM_SIZE_SYSTEM_MEX"
        }
    },
    {
        "OfferId": "12",
        "Size": {
            "size_system": "GM_SIZE_SYSTEM_UK"
        }
    },
    {
        "OfferId": "13",
        "Size": {
            "size_system": "GM_SIZE_SYSTEM_US"
        }
    }
]
)wrap");

}

TEST(RssFeedParser, Size) {
    const auto actual = RunRssFeedParser<NCommon::TGoogleMerchantRssParser>(
        INPUT_XML,
        [] (const TQueueItem& item) {
            NSc::TValue result;
            result["OfferId"] = item->DataCampOffer.identifiers().offer_id();

            const auto& merchant = item->DataCampOffer.content().type_specific_content().google_merchant();
            if (merchant.has_size()) {
                result["Size"] = NSc::TValue::From(merchant.size());
            }
            return result;
        }
    );
    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
