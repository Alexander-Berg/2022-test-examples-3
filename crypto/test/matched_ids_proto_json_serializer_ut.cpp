#include <crypta/cm/services/common/data/id_utils.h>
#include <crypta/cm/services/common/serializers/matched_ids/json/matched_ids_proto_json_serializer.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TMatchedIdsProtoJsonSerializer) {
    using namespace NCrypta::NCm;

    const TString IDS_JSON = R"JSON(
    [
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
                "rt": "1",
                "synt": "1"
            },
            "match_ts": 1500000001,
            "cas": 1
        }
    ]
    )JSON";

    Y_UNIT_TEST(Deserialize) {
        auto ids = NMatchedIdsProtoJsonSerializer::Deserialize(IDS_JSON);

        UNIT_ASSERT_EQUAL_C(2, ids.size(), ids.size());
        {
            const auto& yuid = ids[0];
            UNIT_ASSERT_STRINGS_EQUAL("yandexuid", yuid.GetId().GetType());
            UNIT_ASSERT_STRINGS_EQUAL("2340000001500000000", yuid.GetId().GetValue());
            UNIT_ASSERT_EQUAL(0, yuid.GetMatchTs());
            UNIT_ASSERT_EQUAL(0, yuid.GetCas());

            UNIT_ASSERT_EQUAL(1, yuid.GetAttributes().size());
            UNIT_ASSERT_EQUAL(SYNT_ATTRIBUTE, yuid.GetAttributes()[0].GetName());
            UNIT_ASSERT_EQUAL(SYNT_FALSE_STR, yuid.GetAttributes()[0].GetValue());
        }
        {
            const auto& icookie = ids[1];
            UNIT_ASSERT_STRINGS_EQUAL("icookie", icookie.GetId().GetType());
            UNIT_ASSERT_STRINGS_EQUAL("2340000001500000001", icookie.GetId().GetValue());
            UNIT_ASSERT_EQUAL(1500000001, icookie.GetMatchTs());
            UNIT_ASSERT_EQUAL(1, icookie.GetCas());

            UNIT_ASSERT_EQUAL(2, icookie.GetAttributes().size());
            UNIT_ASSERT_EQUAL(REALTIME_ATTRIBUTE, icookie.GetAttributes()[0].GetName());
            UNIT_ASSERT_EQUAL(REALTIME_TRUE_STR, icookie.GetAttributes()[0].GetValue());
            UNIT_ASSERT_EQUAL(SYNT_ATTRIBUTE, icookie.GetAttributes()[1].GetName());
            UNIT_ASSERT_EQUAL(SYNT_TRUE_STR, icookie.GetAttributes()[1].GetValue());
        }
    }

    Y_UNIT_TEST(DeserializeSerializeDeserialize) {
        auto ids = NMatchedIdsProtoJsonSerializer::Deserialize(IDS_JSON);
        auto json2ndOrder = NMatchedIdsProtoJsonSerializer::Serialize(ids);
        auto ids2ndOrder = NMatchedIdsProtoJsonSerializer::Deserialize(json2ndOrder);

        UNIT_ASSERT(std::equal(ids.begin(), ids.end(), ids2ndOrder.begin()));
    }

    const TString EMPTY_ATTRIBUTES_IDS_JSON = R"JSON(
    [
        {
            "type": "yandexuid",
            "value": "2340000001500000000",
            "attributes": {},
            "cas": 0
        },
        {
            "type": "icookie",
            "value": "2340000001500000001",
            "attributes": {},
            "cas": 1
        }
    ]
    )JSON";

    Y_UNIT_TEST(EmptyAttributesDeserializeSerializeDeserialize) {
        auto ids = NMatchedIdsProtoJsonSerializer::Deserialize(EMPTY_ATTRIBUTES_IDS_JSON);
        auto json2ndOrder = NMatchedIdsProtoJsonSerializer::Serialize(ids);
        auto ids2ndOrder = NMatchedIdsProtoJsonSerializer::Deserialize(json2ndOrder);

        UNIT_ASSERT(std::equal(ids.begin(), ids.end(), ids2ndOrder.begin()));
    }
}
