#include <crypta/siberia/bin/core/lib/logic/common/helpers/cgiparam.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta::NSiberia::NCgiParamUtils;

Y_UNIT_TEST_SUITE(GetUserSetId) {
    Y_UNIT_TEST(Positive) {
        UNIT_ASSERT_EQUAL(42, GetUserSetId({{"user_set_id", "42"}}));
    }

    Y_UNIT_TEST(Negative) {
        UNIT_ASSERT_EXCEPTION(GetUserSetId({{"user_set_id", "x"}}), yexception);
    }
}

Y_UNIT_TEST_SUITE(GetSegmentId) {
    Y_UNIT_TEST(Positive) {
        UNIT_ASSERT_EQUAL(42, GetSegmentId({{"segment_id", "42"}}));
    }

    Y_UNIT_TEST(Negative) {
        UNIT_ASSERT_EXCEPTION(GetSegmentId({{"segment_id", "x"}}), yexception);
    }
}

Y_UNIT_TEST_SUITE(GetLimit) {
    Y_UNIT_TEST(Positive) {
        UNIT_ASSERT_EQUAL(MakeMaybe<ui64>(42), GetLimit({{"limit", "42"}}));
    }

    Y_UNIT_TEST(NotExists) {
        UNIT_ASSERT_EQUAL(Nothing(), GetLimit({{"x", "z"}}));
    }

    Y_UNIT_TEST(Zero) {
        UNIT_ASSERT_EXCEPTION_CONTAINS(GetLimit({{"limit", "0"}}), yexception, "Param 'limit' is equal to 0");
    }

    Y_UNIT_TEST(Negative) {
        UNIT_ASSERT_EXCEPTION(GetLimit({{"limit", "x"}}), yexception);
    }
}
