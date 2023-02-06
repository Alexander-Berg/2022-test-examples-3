#include <crypta/lib/native/cgiparam/cgiparam.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta::NCgiParam;

Y_UNIT_TEST_SUITE(GetNonEmptyString) {
    Y_UNIT_TEST(Positive) {
        const TString field = "field";
        const TString value = "value";
        TCgiParameters cgi = {{field, value}};
        UNIT_ASSERT_EQUAL(value, GetNonEmptyString(cgi, field));
    }

    Y_UNIT_TEST(Negative) {
        const TString field = "field";
        TCgiParameters cgi = {{field, ""}};
        UNIT_ASSERT_EXCEPTION_CONTAINS(GetNonEmptyString(cgi, field), yexception, "Param 'field' is empty");
    }
}

Y_UNIT_TEST_SUITE(GetOptional) {
    Y_UNIT_TEST(FieldExists) {
        const TString field = "field";
        TCgiParameters cgi = {{field, "42"}};
        UNIT_ASSERT_EQUAL(42u, GetOptional<ui64>(cgi, field));
    }

    Y_UNIT_TEST(FieldNotExists) {
        TCgiParameters cgi = {{"field", "value"}};
        UNIT_ASSERT_EQUAL(Nothing(), GetOptional<ui64>(cgi, "xxx"));
    }
}

Y_UNIT_TEST_SUITE(Get) {
    Y_UNIT_TEST(Positive) {
        const TString field = "field";
        TCgiParameters cgi = {{field, "42"}};
        UNIT_ASSERT_EQUAL(42, Get<ui64>(cgi, field));
    }

    Y_UNIT_TEST(FieldNotExists) {
        TCgiParameters cgi = {{"field", "value"}};
        UNIT_ASSERT_EXCEPTION_CONTAINS(Get<ui64>(cgi, "xxx"), yexception, "Param 'xxx' is not specified");
    }

    Y_UNIT_TEST(Negative) {
        const TString field = "field";
        TCgiParameters cgi = {{field, "x"}};
        UNIT_ASSERT_EXCEPTION_CONTAINS(Get<ui64>(cgi, field), yexception, "Param 'field' has invalid type");
    }
}
