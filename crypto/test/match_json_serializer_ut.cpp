#include <crypta/cm/services/common/data/match_validator.h>
#include <crypta/cm/services/common/data/id_utils.h>
#include <crypta/cm/services/common/serializers/match/json/match_json_serializer.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(NMatchJsonSerializer) {
    using namespace NCrypta::NCm;

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
                    "synt":"1",
                    "rt": "1"
                },
                "cas": 1
            }
        ]
    }
    )JSON";

    Y_UNIT_TEST(Deserialize) {
        auto match = NMatchSerializer::FromJson(MATCH_JSON);

        UNIT_ASSERT(NMatchValidator::IsValid(match));
        UNIT_ASSERT_EQUAL(TId("tag", "XXXXXXXXXXXXXXXXXXXX"), match.GetExtId());

        UNIT_ASSERT_EQUAL_C(2, match.GetInternalIds().size(), match.GetInternalIds().size());
        {
            const auto& yuid = match.GetInternalIds().at("yandexuid");
            const auto& refYuid = TMatchedId(TId("yandexuid", "2340000001500000000"),
                                             TInstant::Zero(), 0, {{SYNT_ATTRIBUTE, SYNT_FALSE_STR}});
            UNIT_ASSERT_EQUAL(refYuid, yuid);
        }
        {
            const auto& icookie = match.GetInternalIds().at("icookie");
            const auto& refIcookie = TMatchedId(TId("icookie", "2340000001500000001"),
                                                TInstant::Zero(), 1, {{SYNT_ATTRIBUTE, SYNT_TRUE_STR}, {REALTIME_ATTRIBUTE, REALTIME_TRUE_STR}});
            UNIT_ASSERT_EQUAL(refIcookie, icookie);
        }
    }

    Y_UNIT_TEST(DeserializeSerializeDeserialize) {
        auto deserialized = NMatchSerializer::FromJson(MATCH_JSON);
        auto serialized2ndOrder = NMatchSerializer::ToJson(deserialized);
        auto deserialized2ndOrder = NMatchSerializer::FromJson(serialized2ndOrder);

        UNIT_ASSERT_EQUAL(deserialized, deserialized2ndOrder);
        UNIT_ASSERT(NMatchValidator::IsValid(deserialized2ndOrder));
    }

    Y_UNIT_TEST(ValidWithOneId) {
        auto match = NMatchSerializer::FromJson(
                R"JSON({"ext_id": {"type": "tag", "value": "XXX"}, "ids": [{"type": "yandexuid", "value": "123"}]})JSON");
        UNIT_ASSERT(NMatchValidator::IsValid(match));
    }

    Y_UNIT_TEST(ValidWithExtraKey) {
        auto match = NMatchSerializer::FromJson(
                R"JSON({"ext_id": {"type": "tag", "value": "XXX"}, "foo": "bar", "ids": [{"type": "yandexuid", "value": "123"}]})JSON");
        UNIT_ASSERT(NMatchValidator::IsValid(match));
    }

    Y_UNIT_TEST(ValidWithExtraDataInExtId) {
        auto match = NMatchSerializer::FromJson(
                R"JSON({"ext_id": {"foo": "bar", "type": "tag", "value": "XXX"}, "ids": [{"type": "yandexuid", "value": "123"}]})JSON");
        UNIT_ASSERT(NMatchValidator::IsValid(match));
    }

    Y_UNIT_TEST(ValidWithExtraDataInIds) {
        auto match = NMatchSerializer::FromJson(
                R"JSON({"ext_id": {"type": "tag", "value": "XXX"}, "ids": [{"foo": "bar", "type": "yandexuid", "value": "123"}]})JSON");
        UNIT_ASSERT(NMatchValidator::IsValid(match));
    }

    Y_UNIT_TEST(UnfinishedIdsList) {
        UNIT_ASSERT_EXCEPTION_CONTAINS(
            NMatchSerializer::FromJson(R"JSON({"ext_id": {"type": "tag", "value": "XXX"}, "ids": [{"type": "yandexuid", "value": "123"}})JSON"),
            yexception, "Error: Missing a comma or ']' after an array element.");
    }

    Y_UNIT_TEST(EmptyJson) {
        UNIT_ASSERT_EXCEPTION_CONTAINS(NMatchSerializer::FromJson(""), yexception, "Error: The document is empty.");
        UNIT_ASSERT_EXCEPTION_CONTAINS(NMatchSerializer::FromJson("[]"), yexception, "Json must be a map");
        UNIT_ASSERT_EXCEPTION_CONTAINS(NMatchSerializer::FromJson("{}"), yexception, "Ext id match is not valid");
    }

    Y_UNIT_TEST(InvalidJson) {
        UNIT_ASSERT_EXCEPTION_CONTAINS(NMatchSerializer::FromJson("{"), yexception, "Error: Missing a name for object member.");
    }

    Y_UNIT_TEST(MissingIds) {
        UNIT_ASSERT_EXCEPTION_CONTAINS(NMatchSerializer::FromJson(R"JSON({"ext_id": {"type": "tag", "value": "XXX"}})JSON"),
                yexception, "Ext id match is not valid");
    }

    Y_UNIT_TEST(EmptyIds) {
        UNIT_ASSERT_EXCEPTION_CONTAINS(NMatchSerializer::FromJson(R"JSON({"ext_id": {"type": "tag", "value": "XXX"}, "ids": []})JSON"),
                yexception, "Ext id match is not valid");
    }

    const TString EMPTY_ATTRIBUTES_JSON = R"JSON(
    {
        "ext_id": { "type": "tag", "value": "XXXXXXXXXXXXXXXXXXXX" },
        "ids": [ { "type": "yandexuid", "value": "2340000001500000000", "cas": 0, "attributes": { } } ]
    }
    )JSON";

    Y_UNIT_TEST(EmptyAttributesJson) {
        auto deserialized = NMatchSerializer::FromJson(EMPTY_ATTRIBUTES_JSON);
        UNIT_ASSERT_EQUAL(0, deserialized.GetInternalIds().at("yandexuid").GetAttributes().size());
    }

    const TString NO_ATTRIBUTES_JSON = R"JSON(
    {
        "ext_id": { "type": "tag", "value": "XXXXXXXXXXXXXXXXXXXX" },
        "ids": [ { "type": "yandexuid", "value": "2340000001500000000", "cas": 0 } ]
    }
    )JSON";

    Y_UNIT_TEST(NoAttributesJson) {
        auto deserialized = NMatchSerializer::FromJson(NO_ATTRIBUTES_JSON);
        UNIT_ASSERT_EQUAL(0, deserialized.GetInternalIds().at("yandexuid").GetAttributes().size());
    }

    const TString TRACK_BACK_REFERENCE_TRUE_JSON = R"JSON(
    {
        "ext_id": { "type": "tag", "value": "XXXXXXXXXXXXXXXXXXXX" },
        "ids": [ { "type": "yandexuid", "value": "2340000001500000000", "cas": 0 } ],
        "track_back_reference": true
    }
    )JSON";

    Y_UNIT_TEST(TrackBackReferenceTrueJson) {
        auto deserialized = NMatchSerializer::FromJson(TRACK_BACK_REFERENCE_TRUE_JSON);
        UNIT_ASSERT(deserialized.GetTrackBackReference());
    }

    const TString TRACK_BACK_REFERENCE_FALSE_JSON = R"JSON(
    {
        "ext_id": { "type": "tag", "value": "XXXXXXXXXXXXXXXXXXXX" },
        "ids": [ { "type": "yandexuid", "value": "2340000001500000000", "cas": 0 } ],
        "track_back_reference": false
    }
    )JSON";

    Y_UNIT_TEST(TrackBackReferenceFalseJson) {
        auto deserialized = NMatchSerializer::FromJson(TRACK_BACK_REFERENCE_FALSE_JSON);
        UNIT_ASSERT(!deserialized.GetTrackBackReference());
    }
}
