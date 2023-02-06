#include "common.h"

#include <crypta/lookalike/lib/native/segment_id_validator.h>

#include <library/cpp/testing/unittest/registar.h>
#include <util/generic/vector.h>

using namespace NCrypta::NLookalike;

Y_UNIT_TEST_SUITE(NSegmentIdValidator) {
        Y_UNIT_TEST(IsLookalike) {
            UNIT_ASSERT(NSegmentIdValidator::IsLookalike(1500000000));
            UNIT_ASSERT(NSegmentIdValidator::IsLookalike(1777777777));
            UNIT_ASSERT(NSegmentIdValidator::IsLookalike(1999999999));

            UNIT_ASSERT(!NSegmentIdValidator::IsLookalike(0));
            UNIT_ASSERT(!NSegmentIdValidator::IsLookalike(1499999999));
            UNIT_ASSERT(!NSegmentIdValidator::IsLookalike(2000000000));
        }
}
