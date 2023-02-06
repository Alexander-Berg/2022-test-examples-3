#include "common.h"

#include <crypta/lookalike/lib/native/normalize.h>

#include <library/cpp/testing/unittest/registar.h>
#include <util/generic/vector.h>

using namespace NCrypta::NLookalike;

Y_UNIT_TEST_SUITE(Normalize) {
    Y_UNIT_TEST(Normalize) {
        TVector<float> vector(SITE2VEC_SIZE, 1);
        Normalize(vector);
        for (const auto& elem : vector) {
            UNIT_ASSERT_DOUBLES_EQUAL(elem, 0.0441942, EPS);
        }
    }
}
