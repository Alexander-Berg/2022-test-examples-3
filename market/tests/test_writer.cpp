#include "json_comparer.h"
#include "writer_test_runner.h"

#include <market/idx/feeds/qparser/src/writers/json_writer.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/string.h>

#include <algorithm>


using namespace NMarket;

TVector<NSc::TValue> ParseOutput(IInputStream& input) {
    TVector<NSc::TValue> items;
    TString s;
    while (input.ReadLine(s)) {
         items.push_back(NSc::TValue::FromJson(s));
    }
    return items;
}

TEST(JsonWriter, Example) {
    TFeedInfo feedInfo;
    feedInfo.FeedId = 1069;

    TVector<IWriter::TMsg> items;
    {
        IWriter::TMsg item(feedInfo);
        item.DataCampOffer.mutable_identifiers()->set_offer_id("lalala");
        item.DataCampOffer.mutable_content()->mutable_binding()->mutable_partner()->set_market_sku_id(123);
        item.RurPrice = 1000.0;
        items.push_back(std::move(item));
    }
    {
        IWriter::TMsg item(feedInfo);
        item.DataCampOffer.mutable_identifiers()->set_offer_id("bububu");
        item.DataCampOffer.mutable_content()->mutable_binding()->mutable_partner()->set_market_sku_id(321);
        item.RurPrice = 1900.0;
        items.push_back(std::move(item));
    }

    const TVector<TString> expected = {
        TString(R"wrap(
        {
            "feed_id":  1069,
            "offer_id": "lalala",
            "market_sku": 123,
            "rur_price": 1000.0
        }
        )wrap"),
        TString(R"wrap(
        {
            "feed_id":  1069,
            "offer_id": "bububu",
            "market_sku": 321,
            "rur_price": 1900.0
        }
        )wrap")
    };

    TStringStream stream;
    RunWriterTest<TJsonWriter>(feedInfo, items, stream);
    TVector<NSc::TValue> actualItems = ParseOutput(stream);

    TVector<NSc::TValue> expectedItems;
    std::transform(
        expected.begin(), expected.end(),
        std::back_inserter(expectedItems),
        [] (const auto& x) { return NSc::TValue::FromJson(x); }
    );

    EXPECT_EQ(actualItems.size(), expectedItems.size());
    for (size_t i = 0; i < actualItems.size(); ++i) {
        EXPECT_TRUE(ContainingJson(actualItems[i], expectedItems[i]));
    }
}
