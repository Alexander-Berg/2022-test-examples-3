#include "config_helpers.h"

#include <library/cpp/testing/gtest/gtest.h>

#include <util/generic/vector.h>

namespace NPlutonium {

void CheckRange(TStringBuf rangeStr, const TVector<ui64>& expectedNumbers) {
    const TVector<ui64> actualNumbers = ParseNumericRange(rangeStr);
    EXPECT_EQ(actualNumbers, expectedNumbers);
}

TEST(ConfigHelpers, ParseNumericRange) {
    CheckRange("1", {1});
    CheckRange("1-1", {1});
    CheckRange("2-4", {2, 3, 4});

    CheckRange("1,4,8", {1, 4, 8});

    CheckRange("1-3,6", {1, 2, 3, 6});

    CheckRange(",1-3,,6", {1, 2, 3, 6});

    EXPECT_ANY_THROW(ParseNumericRange(" "));
    EXPECT_ANY_THROW(ParseNumericRange("1x"));
    EXPECT_ANY_THROW(ParseNumericRange("1-"));
    EXPECT_ANY_THROW(ParseNumericRange("1-2-3"));
    EXPECT_ANY_THROW(ParseNumericRange("3-2"));
}

}
