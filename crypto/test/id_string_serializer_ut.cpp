#include <crypta/cm/services/common/serializers/id/string/id_string_serializer.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(NIdSerializer) {
    using namespace NCrypta::NCm;

    Y_UNIT_TEST(Serialize) {
        UNIT_ASSERT_STRINGS_EQUAL("yandexuid:1234567890", NIdSerializer::ToString(TId("yandexuid", "1234567890")));
        UNIT_ASSERT_STRINGS_EQUAL("yandexuid:1:2:3:4:5:6:7:8:9:0", NIdSerializer::ToString(TId("yandexuid", "1:2:3:4:5:6:7:8:9:0")));
        UNIT_ASSERT_STRINGS_EQUAL("yandexuid:", NIdSerializer::ToString(TId("yandexuid", "")));
    }

    Y_UNIT_TEST(SerializeTemplate) {
        UNIT_ASSERT_STRINGS_EQUAL("yandexuid:1234567890", NIdSerializer::JoinId("yandexuid", "1234567890"));
        UNIT_ASSERT_STRINGS_EQUAL("yandexuid:1234567890", NIdSerializer::JoinId("yandexuid", 1234567890));
    }

    Y_UNIT_TEST(Deserialize) {
        UNIT_ASSERT_EQUAL(TId("yandexuid", "1234567890"), NIdSerializer::FromString("yandexuid:1234567890"));
        UNIT_ASSERT_EQUAL(TId("yandexuid", "1:2:3:4:5:6:7:8:9:0"), NIdSerializer::FromString("yandexuid:1:2:3:4:5:6:7:8:9:0"));
        UNIT_ASSERT_EQUAL(TId("yandexuid", ""), NIdSerializer::FromString("yandexuid:"));
    }

    Y_UNIT_TEST(DeserializeNoDelimiter) {
        UNIT_ASSERT_EXCEPTION_CONTAINS(NIdSerializer::FromString("yandexuid1234567890"), yexception, "Could not split id 'yandexuid1234567890' by delimiter ':'");
    }
}
