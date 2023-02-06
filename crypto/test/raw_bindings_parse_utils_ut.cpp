#include <crypta/dmp/adobe/bin/parse_raw_bindings/lib/raw_bindings_parse_utils.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta::NDmp;
using namespace NAdobe;

const static ui64 TIMESTAMP = 1500000000;

Y_UNIT_TEST_SUITE(ParseRawBindingsDiff) {
    Y_UNIT_TEST(Valid) {
        UNIT_ASSERT_EQUAL(ParseRawBindingsDiff("xxx 1,2 3,4", TIMESTAMP), TBindingsDiff("xxx", TIMESTAMP, {1, 2}, {3, 4}));
        UNIT_ASSERT_EQUAL(ParseRawBindingsDiff("xxx 1,2 ", TIMESTAMP), TBindingsDiff("xxx", TIMESTAMP, {1, 2}, {}));
        UNIT_ASSERT_EQUAL(ParseRawBindingsDiff("xxx  3,4", TIMESTAMP), TBindingsDiff("xxx", TIMESTAMP, {}, {3, 4}));
    }

    Y_UNIT_TEST(Invalid) {
        UNIT_ASSERT_EXCEPTION(ParseRawBindingsDiff("", TIMESTAMP), yexception);
        UNIT_ASSERT_EXCEPTION(ParseRawBindingsDiff("xxx", TIMESTAMP), yexception);
        UNIT_ASSERT_EXCEPTION(ParseRawBindingsDiff("xxx 1,2", TIMESTAMP), yexception);
        UNIT_ASSERT_EXCEPTION(ParseRawBindingsDiff("xxx aa", TIMESTAMP), yexception);
        UNIT_ASSERT_EXCEPTION(ParseRawBindingsDiff("xxx -1", TIMESTAMP), yexception);
        UNIT_ASSERT_EXCEPTION(ParseRawBindingsDiff("xxx aa bb", TIMESTAMP), yexception);
        UNIT_ASSERT_EXCEPTION(ParseRawBindingsDiff("xxx 1,2 c,d", TIMESTAMP), yexception);
        UNIT_ASSERT_EXCEPTION(ParseRawBindingsDiff("xxx  1,2 3,4", TIMESTAMP), yexception);
    }
}


Y_UNIT_TEST_SUITE(ParseRawBindings) {
    Y_UNIT_TEST(Valid) {
        UNIT_ASSERT_EQUAL(ParseRawBindings("xxx 1 ", TIMESTAMP), TBindings("xxx", TIMESTAMP, {1}));
        UNIT_ASSERT_EQUAL(ParseRawBindings("xxx 1,2 ", TIMESTAMP), TBindings("xxx", TIMESTAMP, {1, 2}));
        UNIT_ASSERT_EQUAL(ParseRawBindings("xxx 1,2 3,4", TIMESTAMP), TBindings("xxx", TIMESTAMP, {1, 2}));
    }

    Y_UNIT_TEST(Invalid) {
        UNIT_ASSERT_EXCEPTION(ParseRawBindings("", TIMESTAMP), yexception);
        UNIT_ASSERT_EXCEPTION(ParseRawBindings("xxx", TIMESTAMP), yexception);
        UNIT_ASSERT_EXCEPTION(ParseRawBindings("xxx 1", TIMESTAMP), yexception);
        UNIT_ASSERT_EXCEPTION(ParseRawBindings("xxx 1 2 ", TIMESTAMP), yexception);
        UNIT_ASSERT_EXCEPTION(ParseRawBindings("xxx aa", TIMESTAMP), yexception);
        UNIT_ASSERT_EXCEPTION(ParseRawBindings("xxx 1,2 c,d", TIMESTAMP), yexception);
    }
}
