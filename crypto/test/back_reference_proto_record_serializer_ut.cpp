#include <crypta/cm/services/common/data/id_utils.h>
#include <crypta/cm/services/common/serializers/back_reference_proto/record/back_reference_proto_record_serializer.h>
#include <crypta/cm/services/common/serializers/id/string/id_string_serializer.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(NBackReferenceRecordSerializer) {
    using namespace NCrypta::NCm;

    const TString YANDEXUID_VALUE = "12345678901500000000";
    const TString EXT_ID_TYPE = "ext_ns";
    const TString EXT_ID_VALUE = "XXXYYYZZZ";

    Y_UNIT_TEST(SerializeDeserialize) {
        TBackRefProto backRefProto;

        backRefProto.MutableId()->SetType(YANDEXUID_TYPE);
        backRefProto.MutableId()->SetValue(YANDEXUID_VALUE);

        auto* extId = backRefProto.AddRefs();
        extId->SetType(EXT_ID_TYPE);
        extId->SetValue(EXT_ID_VALUE);

        NCrypta::TRecord record;
        NBackReferenceProtoSerializer::ToRecord(backRefProto, record);

        UNIT_ASSERT_STRINGS_EQUAL(NIdSerializer::ToString(TId(YANDEXUID_TYPE, YANDEXUID_VALUE)), record.Key);

        TBackRefProto deserializedProto;
        NBackReferenceProtoSerializer::FromRecord(record, deserializedProto);

        UNIT_ASSERT_EQUAL(backRefProto, deserializedProto);
    }
}
