#include <market/idx/feeds/qparser/tests/parser_test_runner.h>
#include <market/idx/feeds/qparser/tests/test_utils.h>

#include <market/idx/feeds/qparser/src/feed_parsers/google_hotel/csv/feed_parser.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NMarket;

static const TString INPUT_CSV(R"wrap(Property Id,Property name,Destination name,Final URL,Facilities
    1,Item1,Dest1,http://somesite.ru/1,a;b;c;d e f;g
    2,Item2,Dest2,http://somesite.ru/2,d e f
    3,Item3,Dest3,http://somesite.ru/3,    a   b;   c     ;d    ;e    ff   iii; jj;
    4,Item4,Dest4,http://somesite.ru/4,           ;      ;  aaaaa;
    5,Item5,Dest5,http://somesite.ru/5,
)wrap");

static const TString EXPECTED_JSON = TString(R"wrap(
[
    {"property_id":"1", "facilities": ["a", "b", "c", "d e f", "g"]},
    {"property_id":"2", "facilities": ["d e f"]},
    {"property_id":"3", "facilities": ["a b", "c", "d", "e ff iii", "jj"]},
    {"property_id":"4", "facilities": ["aaaaa"]},
    {"property_id":"5", "facilities": []}
]
)wrap");

TEST(GoogleHotelParser, Facilities) {
    const auto actual = RunFeedParserWithTrace<NGoogleHotel::TCsvFeedParser>(
            INPUT_CSV,
            [](const TQueueItem &item) {
                NSc::TValue result;
                if (item->DataCampOffer.content().type_specific_content().has_hotel()) {
                    auto &hotel = item->DataCampOffer.content().type_specific_content().hotel();
                    result["property_id"] = hotel.property_id();

                    result["facilities"].SetArray();
                    result["facilities"].AppendAll(hotel.facilities().begin(), hotel.facilities().end());
                }
                return result;
            },
            GetDefaultGoogleHotelFeedInfo(), "offers-trace.log");

    const auto expected = NSc::TValue::FromJson(EXPECTED_JSON);
    ASSERT_EQ(actual, expected);
}
