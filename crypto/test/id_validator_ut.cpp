#include <crypta/cm/services/common/data/id_validator.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(NIdValidator) {
    using namespace NCrypta::NCm;

    Y_UNIT_TEST(Valid) {
        UNIT_ASSERT(NIdValidator::IsValid(TId("test_id_md5", "!\" #$%&'()*+,-./:;<=>?@[\\]^_`{|}")));
        UNIT_ASSERT(NIdValidator::IsValid(TId("12345678901234567890", "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890")));
    }

    Y_UNIT_TEST(InvalidType) {
        UNIT_ASSERT(!NIdValidator::IsValid(TId("test.id", "123")));
        UNIT_ASSERT(!NIdValidator::IsValid(TId("test id", "123")));
        UNIT_ASSERT(!NIdValidator::IsValid(TId("test:id", "123")));
        UNIT_ASSERT(!NIdValidator::IsValid(TId("test\nid", "123")));
        UNIT_ASSERT(!NIdValidator::IsValid(TId("", "123")));
    }

    Y_UNIT_TEST(InvalidValue) {
        UNIT_ASSERT(!NIdValidator::IsValid(TId("test_id", "1\t3")));
        UNIT_ASSERT(!NIdValidator::IsValid(TId("test_id", "1\n3")));
        UNIT_ASSERT(!NIdValidator::IsValid(TId("test_id", "1я3")));
        UNIT_ASSERT(!NIdValidator::IsValid(TId("test_id", "")));
        UNIT_ASSERT(!NIdValidator::IsValid(TId("test_id", "ok:�lĭ\u001Bj�,�")));
        UNIT_ASSERT(!NIdValidator::IsValid(TId("123456789012345678901", "123")));
        UNIT_ASSERT(!NIdValidator::IsValid(TId("test_id", "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901")));
    }
}
