#include <crypta/cm/services/common/data/id_utils.h>
#include <crypta/cm/services/common/serializers/matched_ids/json/matched_ids_json_serializer.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TMatchedIdsJsonSerializer) {
    using namespace NCrypta::NCm;

    const TString IDS_JSON_1 = R"JSON(
    [
        {
            "type": "yandexuid",
            "value": "2340000001500000000",
            "attributes": {
                "synt": "0"
            },
            "match_ts": 1500000000,
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
        auto ids = NMatchedIdsJsonSerializer::Deserialize(IDS_JSON_1);

        UNIT_ASSERT_EQUAL_C(2, ids.size(), ids.size());
        {
            const auto& yuid = ids[0];
            UNIT_ASSERT_STRINGS_EQUAL("yandexuid", yuid.GetId().Type);
            UNIT_ASSERT_STRINGS_EQUAL("2340000001500000000", yuid.GetId().Value);
            UNIT_ASSERT_EQUAL(TInstant::Seconds(1500000000), yuid.GetMatchTs());
            UNIT_ASSERT_EQUAL(0, yuid.GetCas());

            UNIT_ASSERT_EQUAL(1, yuid.GetAttributes().size());
            UNIT_ASSERT_EQUAL(SYNT_FALSE_STR, yuid.GetAttributes().at(SYNT_ATTRIBUTE));
        }
        {
            const auto& icookie = ids[1];
            UNIT_ASSERT_STRINGS_EQUAL("icookie", icookie.GetId().Type);
            UNIT_ASSERT_STRINGS_EQUAL("2340000001500000001", icookie.GetId().Value);
            UNIT_ASSERT_EQUAL(TInstant::Seconds(1500000001), icookie.GetMatchTs());
            UNIT_ASSERT_EQUAL(1, icookie.GetCas());

            UNIT_ASSERT_EQUAL(2, icookie.GetAttributes().size());
            UNIT_ASSERT_EQUAL(SYNT_TRUE_STR, icookie.GetAttributes().at(SYNT_ATTRIBUTE));
            UNIT_ASSERT_EQUAL(REALTIME_TRUE_STR, icookie.GetAttributes().at(REALTIME_ATTRIBUTE));
        }
    }

    Y_UNIT_TEST(DeserializeSerializeDeserialize) {
        auto ids = NMatchedIdsJsonSerializer::Deserialize(IDS_JSON_1);
        auto json2ndOrder = NMatchedIdsJsonSerializer::Serialize(ids);
        auto ids2ndOrder = NMatchedIdsJsonSerializer::Deserialize(json2ndOrder);

        UNIT_ASSERT_EQUAL(ids, ids2ndOrder);
    }
    //TODO(r-andrey): тест на невалидный, неполный, пустой json, json с лишними данными
}
