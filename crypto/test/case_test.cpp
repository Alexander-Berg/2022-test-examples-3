#include <crypta/lib/native/string/case.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(Case) {
    using namespace NCrypta;

    Y_UNIT_TEST(ToUpperCamelCase) {
        UNIT_ASSERT_STRINGS_EQUAL("FooBar", ToUpperCamelCase("foo_bar"));
        UNIT_ASSERT_STRINGS_EQUAL("FooBar", ToUpperCamelCase("foo-bar"));
        UNIT_ASSERT_STRINGS_EQUAL("FooBar", ToUpperCamelCase("fooBar"));
        UNIT_ASSERT_STRINGS_EQUAL("FooBar", ToUpperCamelCase("FooBar"));
        UNIT_ASSERT_STRINGS_EQUAL("FooBar", ToUpperCamelCase("foo__bar"));
        UNIT_ASSERT_STRINGS_EQUAL("FooBar", ToUpperCamelCase("--foo-bar"));
        UNIT_ASSERT_STRINGS_EQUAL("", ToUpperCamelCase(""));
    }
}
