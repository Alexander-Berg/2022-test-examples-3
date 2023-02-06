#pragma once

#include <library/cpp/testing/unittest/registar.h>

namespace NCrypta {
    template <typename T>
    void AssertEqual(const T& left, const T& right) {
        UNIT_ASSERT_EQUAL(left, right);
        UNIT_ASSERT_EQUAL(right, left);
        UNIT_ASSERT(!(left != right));
        UNIT_ASSERT(!(right != left));
    }

    template <typename T>
    void AssertUnequal(const T& left, const T& right) {
        UNIT_ASSERT_UNEQUAL(left, right);
        UNIT_ASSERT_UNEQUAL(right, left);
        UNIT_ASSERT(!(left == right));
        UNIT_ASSERT(!(right == left));
    }
}
