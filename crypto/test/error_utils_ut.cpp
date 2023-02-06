#include <crypta/lib/native/yt/utils/error_utils.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(NErrorUtils) {
    using namespace NCrypta;

    Y_UNIT_TEST(ToString) {
        UNIT_ASSERT_STRINGS_EQUAL("transaction_lock_conflict", NErrorUtils::ToString(1700));
        UNIT_ASSERT_STRINGS_EQUAL("-1", NErrorUtils::ToString(-1));
    }
}
