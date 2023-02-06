#include <crypta/dmp/adobe/bin/update_bindings/lib/bindings_update_utils.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace NCrypta::NDmp;
using namespace NAdobe;
using namespace NBindingsUpdateUtils;

Y_UNIT_TEST_SUITE(AddSegments) {
    namespace {
        void TestAddSegments(const TSegmentIds& initialState, const TSegmentIds& segmentsToAdd, const TSegmentIds& stateRef, const TVector<TSegmentId>& errorsRef) {
            TSegmentIds state(initialState);
            const auto& errors = AddSegments(state, segmentsToAdd);
            UNIT_ASSERT_EQUAL(stateRef, state);
            UNIT_ASSERT_EQUAL(errorsRef, errors);
        }
    }

    Y_UNIT_TEST(Common) {
        TestAddSegments({1, 2}, {3, 4}, {1, 2, 3, 4}, {});
    }

    Y_UNIT_TEST(EmptyState) {
        TestAddSegments({}, {1, 2}, {1, 2}, {});
    }

    Y_UNIT_TEST(EmptySegmentsToAdd) {
        TestAddSegments({1, 2}, {}, {1, 2}, {});
    }

    Y_UNIT_TEST(Errors) {
        TestAddSegments({1, 2}, {1, 2, 3, 4}, {1, 2, 3, 4}, {1, 2});
    }
}

Y_UNIT_TEST_SUITE(RemoveSegments) {
    namespace {
        void TestRemoveSegments(const TSegmentIds& initialState, const TSegmentIds& segmentsToRemove, const TSegmentIds& stateRef, const TVector<TSegmentId>& errorsRef) {
            TSegmentIds state(initialState);
            const auto& errors = RemoveSegments(state, segmentsToRemove);
            UNIT_ASSERT_EQUAL(stateRef, state);
            UNIT_ASSERT_EQUAL(errorsRef, errors);
        }
    }

    Y_UNIT_TEST(Common) {
        TestRemoveSegments({1, 2, 3, 4}, {3, 4}, {1, 2}, {});
    }

    Y_UNIT_TEST(EmptyState) {
        TestRemoveSegments({}, {1, 2}, {}, {1, 2});
    }

    Y_UNIT_TEST(EmptySegmentsToAdd) {
        TestRemoveSegments({1, 2}, {}, {1, 2}, {});
    }

    Y_UNIT_TEST(Errors) {
        TestRemoveSegments({1, 2, 3}, {1, 2, 4, 5}, {3}, {4, 5});
    }
}
