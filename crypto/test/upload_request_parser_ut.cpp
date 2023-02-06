#include <crypta/cm/services/api/lib/logic/upload/request/upload_request_parser.h>
#include <crypta/cm/services/common/data/id.h>
#include <crypta/cm/services/common/data/matched_id.h>
#include <crypta/cm/services/common/serializers/match/json/match_json_serializer.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/cgiparam/cgiparam.h>

#include <fmt/core.h>

Y_UNIT_TEST_SUITE(NUploadRequestParser) {
    using namespace NCrypta::NCm;
    using namespace NCrypta::NCm::NApi;

    const TString MATCH_JSON = R"JSON(
    {
        "ext_id": {
            "type": "tag",
            "value": "XXXXXXXXXXXXXXXXXXXX"
        },
        "ids": [
            {
                "type": "yandexuid",
                "value": "2340000001500000000",
                "attributes": {
                    "synt": "0"
                },
                "cas": 0
            },
            {
                "type": "icookie",
                "value": "2340000001500000001",
                "attributes": {
                    "synt": "1"
                },
                "cas": 1
            }
        ],
        "track_back_reference": false
    }
    )JSON";

    TMatch MakeRefMatch() {
        TMatch ref;
        ref.SetExtId(TId("tag", "XXXXXXXXXXXXXXXXXXXX"));
        ref.AddId(TMatchedId(TId("yandexuid", "2340000001500000000"), TInstant::Zero(), 0, {{"synt", "0"}}));
        ref.AddId(TMatchedId(TId("icookie", "2340000001500000001"), TInstant::Zero(), 0, {{"synt", "1"}}));
        return ref;
    }

    Y_UNIT_TEST(Parse) {
        const auto& request = NUploadRequestParser::Parse("/upload?subclient=crypta", MATCH_JSON);

        UNIT_ASSERT_STRINGS_EQUAL("crypta", request.Subclient);

        UNIT_ASSERT_EQUAL(MakeRefMatch(), request.Match);
    }

    Y_UNIT_TEST(ParseCgi) {
        const auto& request = NUploadRequestParser::Parse(TCgiParameters("subclient=crypta"), MATCH_JSON);

        UNIT_ASSERT_STRINGS_EQUAL("crypta", request.Subclient);

        UNIT_ASSERT_EQUAL(MakeRefMatch(), request.Match);
    }

    Y_UNIT_TEST(BadPath) {
        UNIT_ASSERT_EXCEPTION(NUploadRequestParser::Parse("/identify?subclient=crypta", MATCH_JSON), yexception);
    }

    const char* MATCH_TRACK_BACK_REFERENCE_TEMPLATE_JSON = R"JSON(
    {{
        "ext_id": {{"type": "tag", "value": "XXXXXXXXXXXXXXXXXXXX"}},
        "ids": [{{"type": "yandexuid", "value": "2340000001500000000"}}],
        "track_back_reference": {}
    }}
    )JSON";

    Y_UNIT_TEST(TrackBackReference) {
        for (bool trackBackReference : TVector<bool>{false, true}) {
            const auto& requestBody = fmt::format(MATCH_TRACK_BACK_REFERENCE_TEMPLATE_JSON, trackBackReference);
            const auto& request = NUploadRequestParser::Parse(TCgiParameters(), requestBody);
            UNIT_ASSERT_EQUAL(trackBackReference, request.Match.GetTrackBackReference());
        }
    }

    const char* DEFAULT_TRACK_BACK_REFERENCE = R"JSON(
    {
        "ext_id": {"type": "tag", "value": "XXXXXXXXXXXXXXXXXXXX"},
        "ids": [{"type": "yandexuid", "value": "2340000001500000000"}]
    }
    )JSON";

    Y_UNIT_TEST(DefaultTrackBackReference) {
        const auto& request = NUploadRequestParser::Parse(TCgiParameters(), DEFAULT_TRACK_BACK_REFERENCE);
        UNIT_ASSERT(!request.Match.GetTrackBackReference());
    }
}
