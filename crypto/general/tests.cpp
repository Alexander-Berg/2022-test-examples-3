#include <library/cpp/testing/unittest/registar.h>
#include <util/charset/wide.h>
#include <crypta/graph/engine/score/native/lib/utils/lcs/lcs.h>

namespace NCrypta {
    namespace NGraphEngine {
        Y_UNIT_TEST_SUITE(TLongestCommonSubstringTestSuite) {
            Y_UNIT_TEST(TestLcs) {
                TString a = "qwe_1945";
                TString b = "1945_1945";

                TString lcs = LongestCommonSubstring(a, b);

                UNIT_ASSERT_EQUAL(lcs, "_1945");
            }

            Y_UNIT_TEST(TestLcsSize) {
                TString a = "qwe_1945";
                TString b = "1945_1945";

                size_t size = LongestCommonSubstringLength(a, b);

                UNIT_ASSERT_EQUAL(size, 5);
            }
        }

    }
}
