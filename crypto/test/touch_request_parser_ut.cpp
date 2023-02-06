#include <crypta/cm/services/api/lib/logic/touch/request/touch_request_parser.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/generic/yexception.h>

using namespace NCrypta::NCm;
using namespace NCrypta::NCm::NApi;

const TString IDS_JSON = R"JSON(
    {
        "items": [
            {
                "id": {
                    "type": "tag1",
                    "value": "XXXXXXXXXXXXXXXXXXXX"
                },
                "touch_timestamp": 1600000000
            },
            {
                "id": {
                    "type": "tag2",
                    "value": "XXXYYY"
                },
                "touch_timestamp": 1500000000
            }
        ]
    }
)JSON";

const TVector<TId> IDS = {
    {"tag1", "XXXXXXXXXXXXXXXXXXXX"},
    {"tag2", "XXXYYY"},
};

const TVector<TInstant> TOUCH_TIMESTAMPS = {
    TInstant::Seconds(1600000000),
    TInstant::Seconds(1500000000),
};

TEST(TTouchRequestParser, Parse) {
    const auto& request = NTouchRequestParser::Parse("/touch?subclient=crypta", IDS_JSON);

    ASSERT_EQ("crypta", request.Subclient);
    ASSERT_EQ(IDS, request.Ids);
    ASSERT_EQ(TOUCH_TIMESTAMPS, request.TouchTimestamps);
}

TEST(TTouchRequestParser, ParseCgi) {
    const auto& request = NTouchRequestParser::Parse(TCgiParameters("subclient=crypta"), IDS_JSON);

    ASSERT_EQ("crypta", request.Subclient);
    ASSERT_EQ(IDS, request.Ids);
    ASSERT_EQ(TOUCH_TIMESTAMPS, request.TouchTimestamps);
}

TEST(TTouchRequestParser, ParseExtraArgs) {
    const auto& request = NTouchRequestParser::Parse("/touch?subclient=phantom2d&request_tag=Aa_-0", IDS_JSON);

    ASSERT_EQ("phantom2d", request.Subclient);
    ASSERT_EQ(IDS, request.Ids);
    ASSERT_EQ(TOUCH_TIMESTAMPS, request.TouchTimestamps);
}

TEST(TTouchRequestParser, BadPath) {
    EXPECT_THROW(NTouchRequestParser::Parse("/upload?subclient=crypta&type=tag&value=XXXYYY", IDS_JSON), yexception);
}
