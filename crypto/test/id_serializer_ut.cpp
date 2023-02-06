#include <crypta/idserv/data/id_serializer.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TIdSerializer) {
    using namespace NCrypta::NIS;

    Y_UNIT_TEST(Serialize) {
        UNIT_ASSERT_STRINGS_EQUAL("crypta_id:1234567890", TIdSerializer::Serialize(TId("crypta_id", "1234567890")));
        UNIT_ASSERT_STRINGS_EQUAL("crypta_id:1:2:3:4:5:6:7:8:9:0", TIdSerializer::Serialize(TId("crypta_id", "1:2:3:4:5:6:7:8:9:0")));
        UNIT_ASSERT_STRINGS_EQUAL("crypta_id:", TIdSerializer::Serialize(TId("crypta_id", "")));
    }

    Y_UNIT_TEST(SerializeTemplate) {
        UNIT_ASSERT_STRINGS_EQUAL("crypta_id:1234567890", TIdSerializer::Serialize("crypta_id", "1234567890"));
        UNIT_ASSERT_STRINGS_EQUAL("crypta_id:1234567890", TIdSerializer::Serialize("crypta_id", 1234567890));
    }

    Y_UNIT_TEST(Deserialize) {
        UNIT_ASSERT_EQUAL(TId("crypta_id", "1234567890"), TIdSerializer::Deserialize("crypta_id:1234567890"));
        UNIT_ASSERT_EQUAL(TId("crypta_id", "1:2:3:4:5:6:7:8:9:0"), TIdSerializer::Deserialize("crypta_id:1:2:3:4:5:6:7:8:9:0"));
        UNIT_ASSERT_EQUAL(TId("crypta_id", ""), TIdSerializer::Deserialize("crypta_id:"));
    }

    Y_UNIT_TEST(DeserializeNoDelimiter) {
        UNIT_ASSERT_EXCEPTION(TIdSerializer::Deserialize("crypta_id1234567890"), yexception);
    }
}
